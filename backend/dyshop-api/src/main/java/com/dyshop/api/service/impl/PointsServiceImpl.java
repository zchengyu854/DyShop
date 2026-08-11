package com.dyshop.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dyshop.api.mapper.CouponTemplateMapper;
import com.dyshop.api.mapper.PointBatchMapper;
import com.dyshop.api.mapper.PointLogMapper;
import com.dyshop.api.mapper.PointsExchangeMapper;
import com.dyshop.api.mapper.PointsGoodsMapper;
import com.dyshop.api.mapper.UserCouponMapper;
import com.dyshop.api.mapper.UserMapper;
import com.dyshop.api.util.CouponUtils;
import com.dyshop.api.vo.PointsExchangeResultVO;
import com.dyshop.api.vo.PointsExchangeVO;
import com.dyshop.api.vo.PointsGoodsVO;
import com.dyshop.api.vo.PointsMallVO;
import com.dyshop.common.entity.CouponTemplate;
import com.dyshop.common.entity.PointBatch;
import com.dyshop.common.entity.PointLog;
import com.dyshop.common.entity.PointsExchange;
import com.dyshop.common.entity.PointsGoods;
import com.dyshop.common.entity.User;
import com.dyshop.common.entity.UserCoupon;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.PageResult;
import com.dyshop.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * C 端积分商城实现（ch13）。
 * 兑换并发：用户行 FOR UPDATE 串行化余额与批次扣减；uk_code 兜底重复兑换码；
 * uk_exchange_no 兜底重复提交。过期任务幂等：仅 remaining>0 的过期批次清零。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsServiceImpl {

    private static final SecureRandom RANDOM = new SecureRandom();
    /** 兑换码字符集（去掉易混淆 0/O/1/I） */
    private static final char[] CODE_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LEN = 16;
    private static final DateTimeFormatter NO_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final PointsGoodsMapper pointsGoodsMapper;
    private final PointsExchangeMapper pointsExchangeMapper;
    private final PointBatchMapper pointBatchMapper;
    private final PointLogMapper pointLogMapper;
    private final UserMapper userMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;

    public PointsMallVO mall(Long userId) {
        List<PointsGoods> goodsList = pointsGoodsMapper.selectList(
                new LambdaQueryWrapper<PointsGoods>()
                        .eq(PointsGoods::getStatus, 1)
                        .orderByAsc(PointsGoods::getSort)
                        .orderByAsc(PointsGoods::getId));
        // 本人已兑次数（一次查全量分组，避免 N+1）
        List<PointsExchange> mine = pointsExchangeMapper.selectList(
                new LambdaQueryWrapper<PointsExchange>().eq(PointsExchange::getUserId, userId));
        Map<Long, Long> countByGoods = mine.stream()
                .collect(Collectors.groupingBy(PointsExchange::getGoodsId, Collectors.counting()));

        PointsMallVO vo = new PointsMallVO();
        vo.setMyPoints(currentPoints(userId));
        vo.setGoods(goodsList.stream().map(g -> toGoodsVo(g, countByGoods)).toList());
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public PointsExchangeResultVO exchange(Long userId, Long goodsId) {
        PointsGoods goods = pointsGoodsMapper.selectById(goodsId);
        if (goods == null || goods.getStatus() == null || goods.getStatus() != 1) {
            throw new BizException(ResultCode.POINTS_GOODS_NOT_FOUND);
        }
        Integer cost = goods.getPointCost();
        if (cost == null || cost <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "积分价配置错误");
        }

        // 已兑换过该商品 → 409（幂等/防重复点击）
        if (countExchange(userId, goodsId) > 0) {
            throw new BizException(ResultCode.POINTS_EXCHANGE_FROZEN);
        }
        // 每人限兑次数（limit_per_user>0 时）
        Integer limit = goods.getLimitPerUser() == null ? 0 : goods.getLimitPerUser();
        if (limit > 0 && countExchange(userId, goodsId) >= limit) {
            throw new BizException(ResultCode.POINTS_EXCHANGE_FROZEN, "超出该商品兑换次数上限");
        }

        // 锁用户行：串行化同用户并发兑换/过期，避免超扣
        User user = lockUser(userId);
        int balance = user.getPoints() == null ? 0 : user.getPoints();
        if (balance < cost) {
            throw new BizException(ResultCode.POINTS_NOT_ENOUGH);
        }

        // 库存乐观扣减（stock=-1 不限）
        Integer stock = goods.getStock();
        if (stock != null && stock >= 0) {
            int stockRows = pointsGoodsMapper.update(null, new LambdaUpdateWrapper<PointsGoods>()
                    .eq(PointsGoods::getId, goodsId)
                    .gt(PointsGoods::getStock, 0)
                    .setSql("stock = stock - 1"));
            if (stockRows == 0) {
                throw new BizException(ResultCode.POINTS_GOODS_SOLD_OUT);
            }
        }

        // FIFO 扣减批次：先到期先扣
        int need = cost;
        for (PointBatch batch : usableBatches(userId)) {
            if (need <= 0) {
                break;
            }
            int take = Math.min(need, batch.getRemaining());
            int rows = pointBatchMapper.update(null, new LambdaUpdateWrapper<PointBatch>()
                    .eq(PointBatch::getId, batch.getId())
                    .gt(PointBatch::getRemaining, 0)
                    .setSql("remaining = remaining - " + take));
            if (rows > 0) {
                need -= take;
            }
        }
        if (need > 0) {
            throw new BizException(ResultCode.POINTS_NOT_ENOUGH);
        }

        int newBalance = balance - cost;
        user.setPoints(newBalance);
        userMapper.updateById(user);

        PointLog plog = new PointLog();
        plog.setUserId(userId);
        plog.setPoints(-cost);
        plog.setBalance(newBalance);
        plog.setRemark("积分商城兑换：" + goods.getName());
        pointLogMapper.insert(plog);

        // 发券或生成兑换码
        String code = null;
        Long couponId = null;
        if ("COUPON".equals(goods.getGoodsType())) {
            couponId = grantCoupon(userId, goods.getCouponTemplateId());
        } else if ("CODE".equals(goods.getGoodsType())) {
            code = newUniqueCode();
        } else {
            throw new BizException(ResultCode.PARAM_ERROR, "商品类型不合法");
        }

        PointsExchange exchange = new PointsExchange();
        exchange.setExchangeNo(newExchangeNo());
        exchange.setUserId(userId);
        exchange.setGoodsId(goodsId);
        exchange.setGoodsName(goods.getName());
        exchange.setGoodsType(goods.getGoodsType());
        exchange.setPointCost(cost);
        exchange.setCode(code);
        exchange.setCouponId(couponId);
        try {
            pointsExchangeMapper.insert(exchange);
        } catch (DuplicateKeyException e) {
            throw new BizException(ResultCode.POINTS_EXCHANGE_FAIL);
        }

        PointsExchangeResultVO result = new PointsExchangeResultVO();
        result.setExchangeNo(exchange.getExchangeNo());
        result.setGoodsType(goods.getGoodsType());
        result.setPointCost(cost);
        result.setCode(code);
        result.setCouponId(couponId);
        return result;
    }

    public PageResult<PointsExchangeVO> myExchanges(Long userId, long page, long size) {
        IPage<PointsExchange> p = pointsExchangeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PointsExchange>()
                        .eq(PointsExchange::getUserId, userId)
                        .orderByDesc(PointsExchange::getCreateTime));
        List<PointsExchangeVO> vos = p.getRecords().stream().map(e -> {
            PointsExchangeVO vo = new PointsExchangeVO();
            vo.setId(e.getId());
            vo.setExchangeNo(e.getExchangeNo());
            vo.setGoodsId(e.getGoodsId());
            vo.setGoodsName(e.getGoodsName());
            vo.setGoodsType(e.getGoodsType());
            vo.setPointCost(e.getPointCost());
            vo.setCode(e.getCode());
            vo.setCouponId(e.getCouponId());
            vo.setCreateTime(e.getCreateTime());
            return vo;
        }).toList();
        return PageResult.of(vos, p.getTotal(), page, size);
    }

    @Transactional(rollbackFor = Exception.class)
    public int expireOverdueBatches() {
        LocalDateTime now = LocalDateTime.now();
        // 需要过期的批次（remaining>0 且已到期）
        List<PointBatch> overdue = pointBatchMapper.selectList(
                new LambdaQueryWrapper<PointBatch>()
                        .gt(PointBatch::getRemaining, 0)
                        .lt(PointBatch::getExpireAt, now));
        if (overdue.isEmpty()) {
            return 0;
        }
        Map<Long, List<PointBatch>> byUser = overdue.stream()
                .collect(Collectors.groupingBy(PointBatch::getUserId));
        int processedUsers = 0;
        for (Map.Entry<Long, List<PointBatch>> entry : byUser.entrySet()) {
            if (expireOneUser(entry.getKey(), entry.getValue(), now) > 0) {
                processedUsers++;
            }
        }
        return processedUsers;
    }

    /** 对单个用户执行过期清零；返回清零积分总数（>0 表示有处理） */
    private int expireOneUser(Long userId, List<PointBatch> expired, LocalDateTime now) {
        int overdue = expired.stream()
                .filter(b -> b.getRemaining() != null && b.getRemaining() > 0)
                .mapToInt(PointBatch::getRemaining).sum();
        if (overdue <= 0) {
            return 0;
        }
        User user = lockUser(userId);
        int balance = user.getPoints() == null ? 0 : user.getPoints();
        int newBalance = Math.max(0, balance - overdue);
        user.setPoints(newBalance);
        userMapper.updateById(user);

        for (PointBatch batch : expired) {
            pointBatchMapper.update(null, new LambdaUpdateWrapper<PointBatch>()
                    .eq(PointBatch::getId, batch.getId())
                    .gt(PointBatch::getRemaining, 0)
                    .set(PointBatch::getRemaining, 0));
        }

        PointLog log = new PointLog();
        log.setUserId(userId);
        log.setPoints(-overdue);
        log.setBalance(newBalance);
        log.setRemark("积分过期清零");
        try {
            pointLogMapper.insert(log);
        } catch (DuplicateKeyException e) {
            // 忽略：幂等运行不重复记账
        }
        return overdue;
    }

    // ---------- 私有 ----------

    private PointsGoodsVO toGoodsVo(PointsGoods g, Map<Long, Long> countByGoods) {
        PointsGoodsVO vo = new PointsGoodsVO();
        vo.setId(g.getId());
        vo.setName(g.getName());
        vo.setCoverImage(g.getCoverImage());
        vo.setDescription(g.getDescription());
        vo.setGoodsType(g.getGoodsType());
        vo.setPointCost(g.getPointCost());
        vo.setStock(g.getStock());
        vo.setLimitPerUser(g.getLimitPerUser());
        vo.setStatus(g.getStatus());
        Long c = countByGoods.get(g.getId());
        vo.setExchangedCount(c == null ? 0 : c.intValue());
        return vo;
    }

    private User lockUser(Long userId) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, userId)
                .last("FOR UPDATE"));
    }

    private int currentPoints(Long userId) {
        User user = userMapper.selectById(userId);
        return user.getPoints() == null ? 0 : user.getPoints();
    }

    private long countExchange(Long userId, Long goodsId) {
        Long c = pointsExchangeMapper.selectCount(new LambdaQueryWrapper<PointsExchange>()
                .eq(PointsExchange::getUserId, userId)
                .eq(PointsExchange::getGoodsId, goodsId));
        return c == null ? 0 : c;
    }

    /** 可用批次：remaining>0 且未过期，按到期时间升序（FIFO） */
    private List<PointBatch> usableBatches(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return pointBatchMapper.selectList(new LambdaQueryWrapper<PointBatch>()
                .eq(PointBatch::getUserId, userId)
                .gt(PointBatch::getRemaining, 0)
                .gt(PointBatch::getExpireAt, now)
                .orderByAsc(PointBatch::getExpireAt)
                .orderByAsc(PointBatch::getId));
    }

    /** COUPON 类：发券到 user_coupon（source=POINTS）；唯一键 (user,template,source) 兜底限 1 */
    private Long grantCoupon(Long userId, Long templateId) {
        CouponTemplate tpl = couponTemplateMapper.selectById(templateId);
        if (tpl == null || tpl.getStatus() == null || tpl.getStatus() == 0) {
            throw new BizException(ResultCode.POINTS_GOODS_SOLD_OUT, "关联优惠券已停用");
        }
        LocalDateTime now = LocalDateTime.now();
        int updated = couponTemplateMapper.update(null, new LambdaUpdateWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, templateId)
                .apply("(total_quantity = -1 OR issued_count + 1 <= total_quantity)")
                .setSql("issued_count = issued_count + 1"));
        if (updated == 0) {
            throw new BizException(ResultCode.POINTS_GOODS_SOLD_OUT, "关联优惠券已发完");
        }
        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setTemplateId(templateId);
        uc.setStatus(0);
        uc.setSource("POINTS");
        uc.setReceivedAt(now);
        uc.setExpireAt(CouponUtils.computeExpireAt(tpl, now));
        try {
            userCouponMapper.insert(uc);
            return uc.getId();
        } catch (DuplicateKeyException e) {
            couponTemplateMapper.update(null, new LambdaUpdateWrapper<CouponTemplate>()
                    .eq(CouponTemplate::getId, templateId)
                    .setSql("issued_count = issued_count - 1"));
            throw new BizException(ResultCode.POINTS_EXCHANGE_FROZEN, "该优惠券已兑换过");
        }
    }

    private String newExchangeNo() {
        return "PX" + NO_FORMAT.format(LocalDateTime.now()) + randomDigits(3);
    }

    private String newUniqueCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = randomCode();
            Long c = pointsExchangeMapper.selectCount(new QueryWrapper<PointsExchange>()
                    .eq("code", candidate));
            if (c == null || c == 0) {
                return candidate;
            }
        }
        throw new BizException(ResultCode.POINTS_EXCHANGE_FAIL);
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LEN);
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(CODE_CHARS[RANDOM.nextInt(CODE_CHARS.length)]);
        }
        return sb.toString();
    }

    private String randomDigits(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append((char) ('0' + RANDOM.nextInt(10)));
        }
        return sb.toString();
    }
}