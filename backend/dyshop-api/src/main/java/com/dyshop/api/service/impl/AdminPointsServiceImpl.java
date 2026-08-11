package com.dyshop.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dyshop.api.dto.PointsGoodsDTO;
import com.dyshop.api.mapper.CouponTemplateMapper;
import com.dyshop.api.mapper.PointsExchangeMapper;
import com.dyshop.api.mapper.PointsGoodsMapper;
import com.dyshop.api.mapper.UserMapper;
import com.dyshop.api.vo.PointsExchangeAdminVO;
import com.dyshop.api.vo.PointsGoodsAdminVO;
import com.dyshop.common.entity.CouponTemplate;
import com.dyshop.common.entity.PointsExchange;
import com.dyshop.common.entity.PointsGoods;
import com.dyshop.common.entity.User;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台积分商城实现（ch13）。
 */
@Service
@RequiredArgsConstructor
public class AdminPointsServiceImpl {

    private final PointsGoodsMapper pointsGoodsMapper;
    private final PointsExchangeMapper pointsExchangeMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final UserMapper userMapper;

    public IPage<PointsGoodsAdminVO> pageGoods(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<PointsGoods> qw = new LambdaQueryWrapper<PointsGoods>()
                .like(StringUtils.hasText(keyword), PointsGoods::getName, keyword == null ? null : keyword.trim())
                .eq(status != null, PointsGoods::getStatus, status)
                .orderByDesc(PointsGoods::getCreateTime);
        IPage<PointsGoods> p = pointsGoodsMapper.selectPage(new Page<>(page, size), qw);
        Page<PointsGoodsAdminVO> result = new Page<>(page, size, p.getTotal());

        Map<Long, CouponTemplate> tpls = loadTemplates(p.getRecords());
        result.setRecords(p.getRecords().stream().map(g -> toAdminVo(g, tpls)).toList());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public PointsGoodsAdminVO create(PointsGoodsDTO dto) {
        validateGoods(dto, null);
        PointsGoods goods = new PointsGoods();
        applyDto(goods, dto);
        pointsGoodsMapper.insert(goods);
        return toAdminVo(goods, loadTemplates(List.of(goods)));
    }

    @Transactional(rollbackFor = Exception.class)
    public PointsGoodsAdminVO update(Long id, PointsGoodsDTO dto) {
        PointsGoods goods = pointsGoodsMapper.selectById(id);
        if (goods == null) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在");
        }
        validateGoods(dto, goods);
        applyDto(goods, dto);
        pointsGoodsMapper.updateById(goods);
        return toAdminVo(goods, loadTemplates(List.of(goods)));
    }

    public void updateStatus(Long id, Integer status) {
        PointsGoods goods = pointsGoodsMapper.selectById(id);
        if (goods == null) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态参数错误");
        }
        goods.setStatus(status);
        pointsGoodsMapper.updateById(goods);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PointsGoods goods = pointsGoodsMapper.selectById(id);
        if (goods == null) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在");
        }
        if (goods.getStatus() != null && goods.getStatus() == 1) {
            throw new BizException(ResultCode.PARAM_ERROR, "请先下架再删除商品");
        }
        pointsGoodsMapper.deleteById(id);
    }

    public IPage<PointsExchangeAdminVO> pageExchanges(long page, long size, Long goodsId, String keyword) {
        LambdaQueryWrapper<PointsExchange> qw = new LambdaQueryWrapper<PointsExchange>()
                .eq(goodsId != null, PointsExchange::getGoodsId, goodsId)
                .orderByDesc(PointsExchange::getCreateTime);
        boolean hasKeyword = StringUtils.hasText(keyword);
        if (hasKeyword) {
            List<Long> matchedUids = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .and(w -> w.like(User::getUsername, keyword.trim())
                                    .or().like(User::getNickname, keyword.trim())))
                    .stream().map(User::getId).toList();
            if (matchedUids.isEmpty()) {
                // 无可匹配用户 → 空结果
                Page<PointsExchangeAdminVO> empty = new Page<>(page, size, 0);
                empty.setRecords(List.of());
                return empty;
            }
            qw.in(PointsExchange::getUserId, matchedUids);
        }
        IPage<PointsExchange> p = pointsExchangeMapper.selectPage(new Page<>(page, size), qw);
        Page<PointsExchangeAdminVO> result = new Page<>(page, size, p.getTotal());

        if (!p.getRecords().isEmpty()) {
            Map<Long, User> users = userMapper.selectBatchIds(
                            p.getRecords().stream().map(PointsExchange::getUserId).distinct().toList())
                    .stream().collect(Collectors.toMap(User::getId, Function.identity()));
            result.setRecords(p.getRecords().stream().map(e -> {
                PointsExchangeAdminVO vo = new PointsExchangeAdminVO();
                vo.setId(e.getId());
                vo.setExchangeNo(e.getExchangeNo());
                vo.setGoodsId(e.getGoodsId());
                vo.setGoodsName(e.getGoodsName());
                vo.setGoodsType(e.getGoodsType());
                vo.setPointCost(e.getPointCost());
                vo.setCode(e.getCode());
                vo.setCouponId(e.getCouponId());
                vo.setCreateTime(e.getCreateTime());
                User u = users.get(e.getUserId());
                vo.setUsername(u == null ? null : u.getUsername());
                vo.setNickname(u == null ? null : u.getNickname());
                return vo;
            }).toList());
        } else {
            result.setRecords(List.of());
        }
        return result;
    }

    // ---------- 私有 ----------

    private void validateGoods(PointsGoodsDTO dto, PointsGoods existing) {
        if (!"COUPON".equals(dto.getGoodsType()) && !"CODE".equals(dto.getGoodsType())) {
            throw new BizException(ResultCode.PARAM_ERROR, "商品类型不合法");
        }
        Integer stock = dto.getStock();
        if (stock != null && stock == 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "库存不能为 0（不限请用 -1）");
        }
        Integer limit = dto.getLimitPerUser();
        if (limit != null && limit < 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "限购次数不能为负数");
        }
        if ("COUPON".equals(dto.getGoodsType())) {
            Long tplId = dto.getCouponTemplateId();
            if (tplId == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "COUPON 商品必须关联券模板");
            }
            CouponTemplate tpl = couponTemplateMapper.selectById(tplId);
            if (tpl == null || tpl.getStatus() == null || tpl.getStatus() != 1) {
                throw new BizException(ResultCode.PARAM_ERROR, "关联券模板不存在或未启用");
            }
        } else {
            // CODE 商品不允许关联模板
            if (dto.getCouponTemplateId() != null) {
                throw new BizException(ResultCode.PARAM_ERROR, "CODE 商品不能关联券模板");
            }
        }
        // 已产生兑换记录的 COUPON 商品不可改关联模板
        if (existing != null && "COUPON".equals(existing.getGoodsType())) {
            Long exchanged = pointsExchangeMapper.selectCount(new LambdaQueryWrapper<PointsExchange>()
                    .eq(PointsExchange::getGoodsId, existing.getId()));
            if (exchanged != null && exchanged > 0) {
                Long oldTpl = existing.getCouponTemplateId();
                Long newTpl = dto.getCouponTemplateId();
                if ((newTpl == null && oldTpl != null)
                        || (newTpl != null && !newTpl.equals(oldTpl))) {
                    throw new BizException(ResultCode.PARAM_ERROR, "已产生兑换记录，关联券模板不可修改");
                }
            }
        }
    }

    private void applyDto(PointsGoods g, PointsGoodsDTO dto) {
        g.setName(dto.getName());
        g.setCoverImage(dto.getCoverImage());
        g.setDescription(dto.getDescription());
        g.setGoodsType(dto.getGoodsType());
        g.setPointCost(dto.getPointCost());
        g.setStock(dto.getStock() == null ? -1 : dto.getStock());
        g.setLimitPerUser(dto.getLimitPerUser() == null ? 0 : dto.getLimitPerUser());
        g.setCouponTemplateId("COUPON".equals(dto.getGoodsType()) ? dto.getCouponTemplateId() : null);
        g.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        g.setSort(dto.getSort() == null ? 0 : dto.getSort());
    }

    private Map<Long, CouponTemplate> loadTemplates(List<PointsGoods> goodsList) {
        List<Long> tplIds = goodsList.stream()
                .map(PointsGoods::getCouponTemplateId)
                .filter(java.util.Objects::nonNull)
                .distinct().toList();
        if (tplIds.isEmpty()) {
            return Map.of();
        }
        return couponTemplateMapper.selectBatchIds(tplIds).stream()
                .collect(Collectors.toMap(CouponTemplate::getId, Function.identity()));
    }

    private PointsGoodsAdminVO toAdminVo(PointsGoods g, Map<Long, CouponTemplate> tpls) {
        PointsGoodsAdminVO vo = new PointsGoodsAdminVO();
        vo.setId(g.getId());
        vo.setName(g.getName());
        vo.setCoverImage(g.getCoverImage());
        vo.setDescription(g.getDescription());
        vo.setGoodsType(g.getGoodsType());
        vo.setPointCost(g.getPointCost());
        vo.setStock(g.getStock());
        vo.setLimitPerUser(g.getLimitPerUser());
        vo.setCouponTemplateId(g.getCouponTemplateId());
        CouponTemplate tpl = g.getCouponTemplateId() == null ? null : tpls.get(g.getCouponTemplateId());
        vo.setCouponTemplateName(tpl == null ? null : tpl.getName());
        vo.setStatus(g.getStatus());
        vo.setSort(g.getSort());
        vo.setCreateTime(g.getCreateTime());
        vo.setUpdateTime(g.getUpdateTime());
        return vo;
    }
}