package com.dyshop.api.controller.order;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dyshop.api.dto.CouponGrantDTO;
import com.dyshop.api.dto.CouponTemplateDTO;
import com.dyshop.api.service.impl.AdminCouponServiceImpl;
import com.dyshop.api.vo.CouponTemplateVO;
import com.dyshop.api.vo.GrantResultVO;
import com.dyshop.api.vo.UserCouponAdminVO;
import com.dyshop.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台优惠券接口（ch11，需 ROLE_ADMIN，路径 /api/admin/** 已由 SecurityConfig 保护）。
 */
@RestController
@RequestMapping("/api/admin/coupon")
@RequiredArgsConstructor
public class AdminCouponController {

    private final AdminCouponServiceImpl adminCouponService;

    /** 模板分页/搜索 */
    @GetMapping("/templates")
    public Result<IPage<CouponTemplateVO>> templates(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Integer status) {
        return Result.success(adminCouponService.pageTemplates(page, size, keyword, status));
    }

    /** 新建模板 */
    @PostMapping("/templates")
    public Result<CouponTemplateVO> create(@Valid @RequestBody CouponTemplateDTO dto) {
        return Result.success(adminCouponService.createTemplate(dto));
    }

    /** 编辑模板（已发放模板仅允许改名称） */
    @PutMapping("/templates/{id}")
    public Result<CouponTemplateVO> update(@PathVariable Long id, @Valid @RequestBody CouponTemplateDTO dto) {
        return Result.success(adminCouponService.updateTemplate(id, dto));
    }

    /** 启用/停用 */
    @PatchMapping("/templates/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminCouponService.updateStatus(id, status);
        return Result.success();
    }

    /** 逻辑删除（仅停用模板） */
    @DeleteMapping("/templates/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminCouponService.deleteTemplate(id);
        return Result.success();
    }

    /** 发放：all=全员 / manual=指定用户 */
    @PostMapping("/grants")
    public Result<GrantResultVO> grant(@Valid @RequestBody CouponGrantDTO dto) {
        return Result.success(adminCouponService.grant(dto));
    }

    /** 用户券分页/搜索 */
    @GetMapping("/user-coupons")
    public Result<IPage<UserCouponAdminVO>> userCoupons(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) Long templateId,
                                                        @RequestParam(required = false) Integer status,
                                                        @RequestParam(required = false) String source) {
        return Result.success(adminCouponService.pageUserCoupons(page, size, keyword, templateId, status, source));
    }

    /** 作废用户券（仅未使用） */
    @PatchMapping("/user-coupons/{id}/void")
    public Result<Void> voidCoupon(@PathVariable Long id) {
        adminCouponService.voidUserCoupon(id);
        return Result.success();
    }
}
