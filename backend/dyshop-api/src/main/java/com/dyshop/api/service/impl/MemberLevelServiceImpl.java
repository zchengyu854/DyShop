package com.dyshop.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dyshop.api.mapper.MemberLevelMapper;
import com.dyshop.api.mapper.OrderMapper;
import com.dyshop.api.mapper.PointBatchMapper;
import com.dyshop.api.mapper.PointLogMapper;
import com.dyshop.api.mapper.ProductMapper;
import com.dyshop.api.mapper.UserMapper;
import com.dyshop.api.mapper.AfterSaleMapper;
import com.dyshop.api.util.SkuJsonUtils;
import com.dyshop.api.vo.MemberLevelVO;
import com.dyshop.api.vo.MemberOverviewVO;
import com.dyshop.api.vo.MemberPricePreviewVO;
import com.dyshop.api.vo.PointLogVO;
import com.dyshop.api.vo.SkuVO;
import com.dyshop.common.entity.MemberLevel;
import com.dyshop.common.entity.Order;
import com.dyshop.common.entity.PointBatch;
import com.dyshop.common.entity.PointLog;
import com.dyshop.common.entity.Product;
import com.dyshop.common.entity.User;
import com.dyshop.common.entity.AfterSale;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.PageResult;
import com.dyshop.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 会员等级与积分实现（ch09）。
 */
@Service
@RequiredArgsConstructor
public class MemberLevelServiceImpl {

    private final MemberLevelMapper memberLevelMapper;
    private final OrderMapper orderMapper;
    private final PointBatchMapper pointBatchMapper;
    private final PointLogMapper pointLogMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final AfterSaleMapper afterSaleMapper;

    public MemberLevel getCurrentLevel(Long userId) {
        List<MemberLevel> levels = memberLevelMapper.selectList(
                new LambdaQueryWrapper<MemberLevel>().orderByAsc(MemberLevel::getSort));
        if (levels.isEmpty()) {
            return null;
        }
        BigDecimal annual = annualConsumption(userId);
        // 取满足门槛的最高等级（只升不降由实时计算天然保证：消费回落则等级回落）
        MemberLevel current = levels.get(0);
        for (MemberLevel level : levels) {
            BigDecimal threshold = level.getThreshold() == null ? BigDecimal.ZERO : level.getThreshold();
            if (annual.compareTo(threshold) >= 0) {
                current = level;
            }
        }
        return current;
    }

    public MemberPricePreviewVO previewPrices(Long userId, List<MemberPricePreviewVO.PriceRow> rows) {
        MemberLevel level = getCurrentLevel(userId);
        List<Long> productIds = rows.stream().map(MemberPricePreviewVO.PriceRow::getProductId).distinct().toList();
        List<Product> products = productIds.isEmpty() ? List.of()
                : productMapper.selectBatchIds(productIds);

        MemberPricePreviewVO vo = new MemberPricePreviewVO();
        vo.setLevel(toLevelVo(level));
        vo.setRows(rows.stream().map(r -> {
            Product product = products.stream()
                    .filter(p -> p.getId().equals(r.getProductId()))
                    .findFirst().orElse(null);
            MemberPricePreviewVO.PriceRow out = new MemberPricePreviewVO.PriceRow();
            out.setProductId(r.getProductId());
            out.setSkuId(r.getSkuId());
            BigDecimal base = r.getSkuId() != null && r.getSkuId() > 0 ? skuPrice(product, r.getSkuId()) : null;
            BigDecimal original = base != null ? base : (product == null ? null : product.getPrice());
            out.setOriginalPrice(original);
            out.setMemberPrice(original == null ? null : resolvePrice(level, product, original));
            return out;
        }).toList());
        return vo;
    }

    /** SKU 号价，找不到返回 null（回退商品价） */
    private BigDecimal skuPrice(Product product, Long skuId) {
        if (product == null || skuId == null || skuId <= 0) {
            return null;
        }
        for (SkuVO sku : SkuJsonUtils.parseSkus(product.getSkus())) {
            if (sku.getId().equals(skuId)) {
                return sku.getPrice();
            }
        }
        return null;
    }

    public MemberOverviewVO overview(Long userId) {
        MemberLevel current = getCurrentLevel(userId);
        List<MemberLevel> levels = memberLevelMapper.selectList(
                new LambdaQueryWrapper<MemberLevel>().orderByAsc(MemberLevel::getSort));

        BigDecimal annual = annualConsumption(userId);
        BigDecimal total = totalConsumption(userId);
        User user = userMapper.selectById(userId);

        MemberOverviewVO vo = new MemberOverviewVO();
        vo.setLevel(toLevelVo(current));
        vo.setTotalConsumption(total);
        vo.setAnnualConsumption(annual);
        vo.setPoints(user == null || user.getPoints() == null ? 0 : user.getPoints());

        // 下一级 = 门槛高于当前年消费的最低档
        MemberLevel next = null;
        for (MemberLevel level : levels) {
            BigDecimal threshold = level.getThreshold() == null ? BigDecimal.ZERO : level.getThreshold();
            if (annual.compareTo(threshold) < 0) {
                next = level;
                break;
            }
        }
        vo.setNextLevel(next == null ? null : toLevelVo(next));
        if (next != null) {
            BigDecimal need = next.getThreshold().subtract(annual);
            vo.setNeedAmount(need.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : need);
            int pct = next.getThreshold().compareTo(BigDecimal.ZERO) == 0 ? 0
                    : annual.multiply(BigDecimal.valueOf(100)).divide(next.getThreshold(), 0, RoundingMode.DOWN)
                    .min(BigDecimal.valueOf(100)).intValue();
            vo.setProgressPct(pct);
        } else {
            vo.setProgressPct(100);
        }
        return vo;
    }

    public BigDecimal resolvePrice(MemberLevel level, Product product, BigDecimal basePrice) {
        if (product == null || basePrice == null) {
            return basePrice;
        }
        // 非会员（普通等级）不享受专享价/折扣
        if (level == null || level.getDiscountRate() == null
                || level.getDiscountRate().compareTo(BigDecimal.ONE) >= 0) {
            return basePrice;
        }
        BigDecimal discount = level.getDiscountRate();
        BigDecimal price = basePrice;
        // 专享价仅对无规格商品生效，且优先于折扣
        boolean hasSpec = product.getSpecs() != null && !product.getSpecs().isBlank()
                && !"null".equalsIgnoreCase(product.getSpecs().trim());
        if (!hasSpec && product.getVipPrice() != null) {
            return product.getVipPrice();
        }
        return price.multiply(discount).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(rollbackFor = Exception.class)
    public void grantPoints(Order order) {
        if (order == null || order.getPayAmount() == null) {
            return;
        }
        Long existing = pointLogMapper.selectCount(new LambdaQueryWrapper<PointLog>()
                .eq(PointLog::getOrderId, order.getId()));
        if (existing != null && existing > 0) {
            return;
        }
        MemberLevel level = getCurrentLevel(order.getUserId());
        BigDecimal rate = level == null || level.getPointRate() == null
                ? BigDecimal.ONE : level.getPointRate();
        int points = order.getPayAmount().setScale(0, RoundingMode.DOWN)
                .multiply(rate).setScale(0, RoundingMode.DOWN).intValue();

        User user = userMapper.selectById(order.getUserId());
        if (user == null) {
            return;
        }
        int balance = (user.getPoints() == null ? 0 : user.getPoints()) + points;
        user.setPoints(balance);
        userMapper.updateById(user);

        PointLog log = new PointLog();
        log.setUserId(order.getUserId());
        log.setOrderId(order.getId());
        log.setPoints(points);
        log.setBalance(balance);
        log.setRemark("订单完成赠送积分");
        try {
            pointLogMapper.insert(log);
        } catch (DuplicateKeyException e) {
            // 并发重复发放兜底：order_id 唯一键冲突，回滚积分累加
            user.setPoints(balance - points);
            userMapper.updateById(user);
            throw e;
        }

        // ch13：同事务写积分批次（12 个月有效期，兑换按 FIFO 扣减）
        PointBatch batch = new PointBatch();
        batch.setUserId(order.getUserId());
        batch.setSourceType("ORDER");
        batch.setSourceId(order.getId());
        batch.setPoints(points);
        batch.setRemaining(points);
        batch.setExpireAt(LocalDateTime.now().plusMonths(12));
        pointBatchMapper.insert(batch);
    }

    public PageResult<PointLogVO> pointPage(Long userId, long page, long size) {
        IPage<PointLog> p = pointLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PointLog>()
                        .eq(PointLog::getUserId, userId)
                        .orderByDesc(PointLog::getCreateTime));
        List<PointLogVO> vos = p.getRecords().stream().map(log -> {
            PointLogVO vo = new PointLogVO();
            vo.setPoints(log.getPoints());
            vo.setBalance(log.getBalance());
            vo.setRemark(log.getRemark());
            vo.setCreateTime(log.getCreateTime());
            return vo;
        }).toList();
        return PageResult.of(vos, p.getTotal(), page, size);
    }

    public List<MemberLevelVO> listLevels() {
        return memberLevelMapper.selectList(
                        new LambdaQueryWrapper<MemberLevel>().orderByAsc(MemberLevel::getSort))
                .stream().map(this::toLevelVo).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateLevel(Long id, BigDecimal threshold, BigDecimal discountRate, BigDecimal pointRate) {
        MemberLevel level = memberLevelMapper.selectById(id);
        if (level == null) {
            throw new BizException(ResultCode.NOT_FOUND, "等级不存在");
        }
        level.setThreshold(threshold == null ? level.getThreshold() : threshold);
        level.setDiscountRate(discountRate == null ? level.getDiscountRate() : discountRate);
        level.setPointRate(pointRate == null ? level.getPointRate() : pointRate);
        memberLevelMapper.updateById(level);
    }

    // ---------- 私有 ----------

    /** 近12个月已支付订单 pay_amount 之和（等级判定口径：支付即计入；退款单从消费中扣减） */
    private BigDecimal annualConsumption(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusMonths(12);
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .isNotNull(Order::getPayTime)
                .ge(Order::getPayTime, since));
        return sumPayAmount(orders).subtract(refundedAmountSince(userId, since));
    }

    /** 全部已支付订单 pay_amount 之和（累计消费口径：支付即计入消费；退款单从消费中扣减） */
    private BigDecimal totalConsumption(Long userId) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .isNotNull(Order::getPayTime));
        return sumPayAmount(orders).subtract(refundedAmountSince(userId, null));
    }

    /**
     * 已退款售后金额合计（ch12：退款完成单 status=2；since 为空=全部）。
     * 退款后累计消费/等级判定同步扣减，防止退款用户消费虚高。
     */
    private BigDecimal refundedAmountSince(Long userId, LocalDateTime since) {
        LambdaQueryWrapper<AfterSale> qw = new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getUserId, userId)
                .eq(AfterSale::getStatus, 2)
                .ge(since != null, AfterSale::getHandleTime, since);
        List<AfterSale> list = afterSaleMapper.selectList(qw);
        return list.stream()
                .map(AfterSale::getRefundAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumPayAmount(List<Order> orders) {
        return orders.stream()
                .map(Order::getPayAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private MemberLevelVO toLevelVo(MemberLevel level) {
        if (level == null) {
            return null;
        }
        MemberLevelVO vo = new MemberLevelVO();
        vo.setId(level.getId());
        vo.setCode(level.getCode());
        vo.setName(level.getName());
        vo.setThreshold(level.getThreshold());
        vo.setDiscountRate(level.getDiscountRate());
        vo.setPointRate(level.getPointRate());
        return vo;
    }
}