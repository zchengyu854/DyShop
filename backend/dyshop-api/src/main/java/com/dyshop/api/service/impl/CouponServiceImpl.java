package com.dyshop.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dyshop.api.mapper.CouponTemplateMapper;
import com.dyshop.api.mapper.UserCouponMapper;
import com.dyshop.api.util.CouponUtils;
import com.dyshop.api.vo.CouponCenterVO;
import com.dyshop.api.vo.CouponTemplateVO;
import com.dyshop.api.vo.MyCouponVO;
import com.dyshop.common.entity.CouponTemplate;
import com.dyshop.common.entity.UserCoupon;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.PageResult;
import com.dyshop.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * C 端优惠券实现（ch11）。
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl {

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;

    public List<CouponCenterVO> center(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<CouponTemplate> templates = couponTemplateMapper.selectList(
                new LambdaQueryWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getStatus, 1)
                        .eq(CouponTemplate::getIssueType, "CENTER")
                        .and(w -> w.isNull(CouponTemplate::getStartAt)
                                .or().le(CouponTemplate::getStartAt, now))
                        .and(w -> w.isNull(CouponTemplate::getEndAt)
                                .or().ge(CouponTemplate::getEndAt, now))
                        .orderByDesc(CouponTemplate::getCreateAt));

        if (templates.isEmpty()) {
            return List.of();
        }
        // 当前用户已领取的模板集合（CENTER 源）
        List<Long> claimedIds = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getSource, "CENTER")
                        .in(UserCoupon::getTemplateId,
                                templates.stream().map(CouponTemplate::getId).toList()))
                .stream().map(UserCoupon::getTemplateId).collect(Collectors.toSet()).stream().toList();

        return templates.stream().map(tpl -> {
            CouponCenterVO vo = new CouponCenterVO();
            vo.setTemplate(toTemplateVO(tpl));
            vo.setClaimed(claimedIds.contains(tpl.getId()));
            vo.setRemaining(computeRemaining(tpl));
            return vo;
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void claim(Long userId, Long templateId) {
        CouponTemplate tpl = couponTemplateMapper.selectById(templateId);
        if (tpl == null) {
            throw new BizException(ResultCode.NOT_FOUND, "优惠券模板不存在");
        }
        if (tpl.getStatus() == null || tpl.getStatus() != 1) {
            throw new BizException(ResultCode.PARAM_ERROR, "该优惠券已停发");
        }
        if (!"CENTER".equals(tpl.getIssueType())) {
            throw new BizException(ResultCode.PARAM_ERROR, "该优惠券不支持领取");
        }
        LocalDateTime now = LocalDateTime.now();
        if (tpl.getStartAt() != null && now.isBefore(tpl.getStartAt())) {
            throw new BizException(ResultCode.PARAM_ERROR, "活动尚未开始");
        }
        if (tpl.getEndAt() != null && now.isAfter(tpl.getEndAt())) {
            throw new BizException(ResultCode.PARAM_ERROR, "活动已结束");
        }

        // 每人限领（unique 索引兜底）：同「用户+模板+CENTER」最多 1 张
        Long exists = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getTemplateId, templateId)
                .eq(UserCoupon::getSource, "CENTER"));
        if (exists != null && exists > 0) {
            throw new BizException(ResultCode.COUPON_ALREADY_CLAIMED);
        }

        // 总量乐观锁：total_quantity=-1 不限，否则 issued_count+1 <= total_quantity（防超领）
        int updated = couponTemplateMapper.update(null, new LambdaUpdateWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, templateId)
                .apply("(total_quantity = -1 OR issued_count + 1 <= total_quantity)")
                .setSql("issued_count = issued_count + 1"));
        if (updated == 0) {
            throw new BizException(ResultCode.COUPON_SOLD_OUT);
        }

        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setTemplateId(templateId);
        uc.setStatus(0);
        uc.setSource("CENTER");
        uc.setReceivedAt(now);
        uc.setExpireAt(CouponUtils.computeExpireAt(tpl, now));
        try {
            userCouponMapper.insert(uc);
        } catch (DuplicateKeyException e) {
            // 并发重复领取兜底：回滚 issued_count
            couponTemplateMapper.update(null, new LambdaUpdateWrapper<CouponTemplate>()
                    .eq(CouponTemplate::getId, templateId)
                    .setSql("issued_count = issued_count - 1"));
            throw new BizException(ResultCode.COUPON_ALREADY_CLAIMED);
        }
    }

    public PageResult<MyCouponVO> mine(Long userId, Integer status, long page, long size) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<UserCoupon> qw = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId);
        if (status != null) {
            if (status == 0) {
                // 未使用：库里 status=0 且未过期
                qw.eq(UserCoupon::getStatus, 0)
                        .and(w -> w.isNull(UserCoupon::getExpireAt)
                                .or().gt(UserCoupon::getExpireAt, now));
            } else if (status == 1) {
                qw.eq(UserCoupon::getStatus, 1);
            } else if (status == 2) {
                // 已过期：库里已标记 2，或未使用但已过有效期（惰性判定）
                qw.and(w -> w.eq(UserCoupon::getStatus, 2)
                        .or(o -> o.eq(UserCoupon::getStatus, 0)
                                .isNotNull(UserCoupon::getExpireAt)
                                .le(UserCoupon::getExpireAt, now)));
            } else {
                throw new BizException(ResultCode.PARAM_ERROR, "状态参数错误");
            }
        }
        qw.orderByDesc(UserCoupon::getReceivedAt);

        IPage<UserCoupon> p = userCouponMapper.selectPage(new Page<>(page, size), qw);
        if (p.getRecords().isEmpty()) {
            return PageResult.of(List.of(), p.getTotal(), page, size);
        }
        Map<Long, CouponTemplate> templates = couponTemplateMapper.selectBatchIds(
                        p.getRecords().stream().map(UserCoupon::getTemplateId).distinct().toList())
                .stream().collect(Collectors.toMap(CouponTemplate::getId, Function.identity()));

        List<MyCouponVO> vos = p.getRecords().stream().map(uc -> {
            CouponTemplate tpl = templates.get(uc.getTemplateId());
            MyCouponVO vo = new MyCouponVO();
            vo.setId(uc.getId());
            vo.setTemplateId(uc.getTemplateId());
            vo.setName(tpl == null ? "已删除的券" : tpl.getName());
            vo.setMinAmount(tpl == null ? null : tpl.getMinAmount());
            vo.setDiscountAmount(tpl == null ? null : tpl.getDiscountAmount());
            vo.setScope(tpl == null ? null : tpl.getScope());
            vo.setCategoryIds(tpl == null ? null : tpl.getCategoryIds());
            vo.setProductIds(tpl == null ? null : tpl.getProductIds());
            vo.setAllowStack(tpl == null ? null : tpl.getAllowStack());
            vo.setStatus(resolveStatus(uc, now));
            vo.setSource(uc.getSource());
            vo.setUsedOrderId(uc.getUsedOrderId());
            vo.setReceivedAt(uc.getReceivedAt());
            vo.setExpireAt(uc.getExpireAt());
            return vo;
        }).toList();
        return PageResult.of(vos, p.getTotal(), page, size);
    }

    // ---------- 私有 ----------

    /** 展示层状态：未使用但已过期 → 2（惰性判定，不落库） */
    private int resolveStatus(UserCoupon uc, LocalDateTime now) {
        if (uc.getStatus() != null && uc.getStatus() != 0) {
            return uc.getStatus();
        }
        if (uc.getExpireAt() != null && now.isAfter(uc.getExpireAt())) {
            return 2;
        }
        return 0;
    }

    /** 剩余量：-1 不限 → -1；否则 total - issued */
    private long computeRemaining(CouponTemplate tpl) {
        int total = tpl.getTotalQuantity() == null ? -1 : tpl.getTotalQuantity();
        if (total == -1) {
            return -1;
        }
        int issued = tpl.getIssuedCount() == null ? 0 : tpl.getIssuedCount();
        return Math.max(0, total - issued);
    }

    private CouponTemplateVO toTemplateVO(CouponTemplate tpl) {
        CouponTemplateVO vo = new CouponTemplateVO();
        vo.setId(tpl.getId());
        vo.setName(tpl.getName());
        vo.setType(tpl.getType());
        vo.setMinAmount(tpl.getMinAmount());
        vo.setDiscountAmount(tpl.getDiscountAmount());
        vo.setScope(tpl.getScope());
        vo.setCategoryIds(tpl.getCategoryIds());
        vo.setProductIds(tpl.getProductIds());
        vo.setAllowStack(tpl.getAllowStack());
        vo.setIssueType(tpl.getIssueType());
        vo.setValidType(tpl.getValidType());
        vo.setStartAt(tpl.getStartAt());
        vo.setEndAt(tpl.getEndAt());
        vo.setValidDays(tpl.getValidDays());
        vo.setTotalQuantity(tpl.getTotalQuantity());
        vo.setPerUser(tpl.getPerUser());
        vo.setIssuedCount(tpl.getIssuedCount());
        vo.setStatus(tpl.getStatus());
        vo.setCreateAt(tpl.getCreateAt());
        vo.setUpdateAt(tpl.getUpdateAt());
        return vo;
    }
}
