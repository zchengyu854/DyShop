package com.dyshop.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dyshop.api.dto.CouponGrantDTO;
import com.dyshop.api.dto.CouponTemplateDTO;
import com.dyshop.api.mapper.CouponTemplateMapper;
import com.dyshop.api.mapper.UserCouponMapper;
import com.dyshop.api.mapper.UserMapper;
import com.dyshop.api.util.CouponUtils;
import com.dyshop.api.vo.CouponTemplateVO;
import com.dyshop.api.vo.GrantResultVO;
import com.dyshop.api.vo.UserCouponAdminVO;
import com.dyshop.common.entity.CouponTemplate;
import com.dyshop.common.entity.User;
import com.dyshop.common.entity.UserCoupon;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台优惠券管理实现（ch11）。
 */
@Service
@RequiredArgsConstructor
public class AdminCouponServiceImpl {

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final UserMapper userMapper;

    public IPage<CouponTemplateVO> pageTemplates(long page, long size, String keyword, Integer status) {
        boolean hasKeyword = StringUtils.hasText(keyword);
        LambdaQueryWrapper<CouponTemplate> qw = new LambdaQueryWrapper<CouponTemplate>()
                .eq(status != null, CouponTemplate::getStatus, status)
                .like(hasKeyword, CouponTemplate::getName, keyword == null ? null : keyword.trim())
                .orderByDesc(CouponTemplate::getCreateAt);
        IPage<CouponTemplate> p = couponTemplateMapper.selectPage(new Page<>(page, size), qw);
        Page<CouponTemplateVO> result = new Page<>(page, size, p.getTotal());
        result.setRecords(p.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public CouponTemplateVO createTemplate(CouponTemplateDTO dto) {
        validateTemplate(dto, true);
        CouponTemplate tpl = new CouponTemplate();
        applyEditable(dto, tpl);
        tpl.setIssuedCount(0);
        tpl.setStatus(1);
        tpl.setCreateAt(LocalDateTime.now());
        couponTemplateMapper.insert(tpl);
        return toVO(tpl);
    }

    @Transactional(rollbackFor = Exception.class)
    public CouponTemplateVO updateTemplate(Long id, CouponTemplateDTO dto) {
        CouponTemplate tpl = requireTemplate(id);
        boolean issued = hasIssued(tpl.getId());
        validateTemplate(dto, issued);

        if (issued) {
            // 已发放模板：只允许改名称（金额/范围/有效期等仅对新券生效）
            if (!Objects.equals(trim(dto.getName()), trim(tpl.getName()))) {
                tpl.setName(dto.getName().trim());
                couponTemplateMapper.updateById(tpl);
            }
            return toVO(tpl);
        }
        applyEditable(dto, tpl);
        tpl.setUpdateAt(LocalDateTime.now());
        couponTemplateMapper.updateById(tpl);
        return toVO(tpl);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态参数错误");
        }
        CouponTemplate tpl = requireTemplate(id);
        if (Objects.equals(tpl.getStatus(), status)) {
            return;
        }
        tpl.setStatus(status);
        tpl.setUpdateAt(LocalDateTime.now());
        couponTemplateMapper.updateById(tpl);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        CouponTemplate tpl = requireTemplate(id);
        if (Objects.equals(tpl.getStatus(), 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "请先停用再删除模板");
        }
        if (hasIssued(id)) {
            throw new BizException(ResultCode.PARAM_ERROR, "该券已发放，无法删除（可保持停用）");
        }
        // @TableLogic 逻辑删除
        couponTemplateMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public GrantResultVO grant(CouponGrantDTO dto) {
        CouponTemplate tpl = requireTemplate(dto.getTemplateId());

        List<Long> userIds;
        if ("all".equals(dto.getTarget())) {
            userIds = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getStatus, 0))
                    .stream().map(User::getId).toList();
        } else {
            if (dto.getUserIds() == null || dto.getUserIds().isEmpty()) {
                throw new BizException(ResultCode.PARAM_ERROR, "请选择要发放的用户");
            }
            userIds = dto.getUserIds();
        }
        if (userIds.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "没有可发放的用户");
        }

        long granted = 0;
        long skipped = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Long uid : userIds) {
            Long existing = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getUserId, uid)
                    .eq(UserCoupon::getTemplateId, tpl.getId())
                    .eq(UserCoupon::getSource, "MANUAL"));
            if (existing != null && existing > 0) {
                skipped++;
                continue;
            }
            // 总量乐观锁：total_quantity=-1 不限，否则 issued_count+1 <= total_quantity
            int updated = couponTemplateMapper.update(null, new LambdaUpdateWrapper<CouponTemplate>()
                    .eq(CouponTemplate::getId, tpl.getId())
                    .apply("(total_quantity = -1 OR issued_count + 1 <= total_quantity)")
                    .setSql("issued_count = issued_count + 1"));
            if (updated == 0) {
                throw new BizException(ResultCode.COUPON_SOLD_OUT, "优惠券库存不足，已停止发放");
            }
            UserCoupon uc = new UserCoupon();
            uc.setUserId(uid);
            uc.setTemplateId(tpl.getId());
            uc.setStatus(0);
            uc.setSource("MANUAL");
            uc.setReceivedAt(now);
            uc.setExpireAt(CouponUtils.computeExpireAt(tpl, now));
            try {
                userCouponMapper.insert(uc);
                granted++;
            } catch (DuplicateKeyException e) {
                // 并发发放同源兜底：回滚 issued_count，计入跳过
                couponTemplateMapper.update(null, new LambdaUpdateWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getId, tpl.getId())
                        .setSql("issued_count = issued_count - 1"));
                skipped++;
            }
        }
        if (granted == 0 && skipped > 0) {
            throw new BizException(ResultCode.COUPON_ALREADY_GRANTED);
        }
        return new GrantResultVO(granted, skipped);
    }

    public IPage<UserCouponAdminVO> pageUserCoupons(long page, long size, String keyword,
                                                    Long templateId, Integer status, String source) {
        boolean hasKeyword = StringUtils.hasText(keyword);
        List<Long> kwUserIds = null;
        if (hasKeyword) {
            kwUserIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .like(User::getUsername, keyword.trim())
                            .or().like(User::getPhone, keyword.trim()))
                    .stream().map(User::getId).toList();
            if (kwUserIds.isEmpty()) {
                return new Page<>(page, size);
            }
        }

        LambdaQueryWrapper<UserCoupon> qw = new LambdaQueryWrapper<UserCoupon>()
                .in(hasKeyword, UserCoupon::getUserId, kwUserIds)
                .eq(templateId != null, UserCoupon::getTemplateId, templateId)
                .eq(status != null, UserCoupon::getStatus, status)
                .eq(StringUtils.hasText(source), UserCoupon::getSource, source)
                .orderByDesc(UserCoupon::getReceivedAt);
        IPage<UserCoupon> p = userCouponMapper.selectPage(new Page<>(page, size), qw);
        if (p.getRecords().isEmpty()) {
            return new Page<>(page, size);
        }

        Map<Long, User> users = userMapper.selectBatchIds(
                        p.getRecords().stream().map(UserCoupon::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, CouponTemplate> templates = couponTemplateMapper.selectBatchIds(
                        p.getRecords().stream().map(UserCoupon::getTemplateId).distinct().toList())
                .stream().collect(Collectors.toMap(CouponTemplate::getId, Function.identity()));

        Page<UserCouponAdminVO> result = new Page<>(page, size, p.getTotal());
        result.setRecords(p.getRecords().stream().map(uc -> {
            UserCouponAdminVO vo = new UserCouponAdminVO();
            vo.setId(uc.getId());
            vo.setUserId(uc.getUserId());
            User u = users.get(uc.getUserId());
            vo.setUsername(u == null ? null : u.getUsername());
            vo.setPhone(u == null ? null : u.getPhone());
            vo.setTemplateId(uc.getTemplateId());
            CouponTemplate tpl = templates.get(uc.getTemplateId());
            vo.setTemplateName(tpl == null ? null : tpl.getName());
            vo.setStatus(uc.getStatus());
            vo.setSource(uc.getSource());
            vo.setUsedOrderId(uc.getUsedOrderId());
            vo.setReceivedAt(uc.getReceivedAt());
            vo.setExpireAt(uc.getExpireAt());
            vo.setUsedAt(uc.getUsedAt());
            return vo;
        }).toList());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void voidUserCoupon(Long id) {
        UserCoupon uc = userCouponMapper.selectById(id);
        if (uc == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户券不存在");
        }
        if (!Objects.equals(uc.getStatus(), 0)) {
            throw new BizException(ResultCode.PARAM_ERROR, "仅未使用的优惠券可作废");
        }
        uc.setStatus(2);
        userCouponMapper.updateById(uc);
    }

    // ---------- 私有 ----------

    /**
     * 模板业务校验。
     *
     * @param dto      提交数据
     * @param isIssued 是否已发放（已发放时锁定业务字段，仅允许改名称）
     */
    private void validateTemplate(CouponTemplateDTO dto, boolean isIssued) {
        BigDecimal min = dto.getMinAmount() == null ? BigDecimal.ZERO : dto.getMinAmount();
        if (min.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "满减门槛不能为负数");
        }
        if (dto.getDiscountAmount() == null || dto.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "立减金额必须大于 0");
        }
        if (isIssued) {
            // 已发放模板：名称外字段保持原值（此处只校验名称），其余按现有值覆盖
            if (!StringUtils.hasText(dto.getName())) {
                throw new BizException(ResultCode.PARAM_ERROR, "模板名称不能为空");
            }
            return;
        }
        if ("LIMITED".equals(dto.getScope())) {
            boolean hasCategory = !CouponUtils.parseLongs(dto.getCategoryIds()).isEmpty();
            boolean hasProduct = !CouponUtils.parseLongs(dto.getProductIds()).isEmpty();
            if (!hasCategory && !hasProduct) {
                throw new BizException(ResultCode.PARAM_ERROR, "限定范围券至少需指定分类或商品一项");
            }
        }
        if ("FIXED".equals(dto.getValidType()) && dto.getEndAt() != null
                && dto.getStartAt() != null && dto.getEndAt().isBefore(dto.getStartAt())) {
            throw new BizException(ResultCode.PARAM_ERROR, "有效期结束时间不能早于开始时间");
        }
        if ("AFTER_DAYS".equals(dto.getValidType())
                && (dto.getValidDays() == null || dto.getValidDays() < 0)) {
            throw new BizException(ResultCode.PARAM_ERROR, "有效天数不能为负数");
        }
    }

    /** 将可编辑字段写入实体（新券/未发放模板） */
    private void applyEditable(CouponTemplateDTO dto, CouponTemplate tpl) {
        tpl.setName(dto.getName().trim());
        tpl.setType(dto.getType() == null ? "REDUCE" : dto.getType());
        tpl.setMinAmount(dto.getMinAmount() == null ? BigDecimal.ZERO : dto.getMinAmount());
        tpl.setDiscountAmount(dto.getDiscountAmount());
        tpl.setScope(dto.getScope());
        tpl.setCategoryIds(dto.getCategoryIds());
        tpl.setProductIds(dto.getProductIds());
        tpl.setAllowStack(dto.getAllowStack() == null ? 0 : dto.getAllowStack());
        tpl.setIssueType(dto.getIssueType());
        tpl.setValidType(dto.getValidType());
        tpl.setStartAt(dto.getStartAt());
        tpl.setEndAt(dto.getEndAt());
        tpl.setValidDays(dto.getValidDays() == null ? 0 : dto.getValidDays());
        tpl.setTotalQuantity(dto.getTotalQuantity() == null ? -1 : dto.getTotalQuantity());
        tpl.setPerUser(dto.getPerUser() == null ? 1 : dto.getPerUser());
    }

    private boolean hasIssued(Long templateId) {
        Long count = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getTemplateId, templateId));
        return count != null && count > 0;
    }

    private CouponTemplate requireTemplate(Long id) {
        CouponTemplate tpl = couponTemplateMapper.selectById(id);
        if (tpl == null) {
            throw new BizException(ResultCode.NOT_FOUND, "优惠券模板不存在");
        }
        return tpl;
    }

    private CouponTemplateVO toVO(CouponTemplate tpl) {
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

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
