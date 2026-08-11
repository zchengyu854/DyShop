package com.dyshop.api.controller.user;

import com.dyshop.api.service.impl.CouponServiceImpl;
import com.dyshop.api.vo.CouponCenterVO;
import com.dyshop.api.vo.MyCouponVO;
import com.dyshop.common.result.PageResult;
import com.dyshop.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端优惠券接口（ch11，需登录）。
 */
@RestController
@RequestMapping("/api/user/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponServiceImpl couponService;

    /** 领券中心模板列表（含每人领取状态与剩余量） */
    @GetMapping("/center")
    public Result<List<CouponCenterVO>> center() {
        return Result.success(couponService.center(currentUserId()));
    }

    /** 领取（幂等：重复领取 409） */
    @PostMapping("/center/claim")
    public Result<Void> claim(@RequestParam Long templateId) {
        couponService.claim(currentUserId(), templateId);
        return Result.success();
    }

    /** 我的优惠券（status=0/1/2 分态，page 分页） */
    @GetMapping("/mine")
    public Result<PageResult<MyCouponVO>> mine(@RequestParam(required = false) Integer status,
                                               @RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size) {
        return Result.success(couponService.mine(currentUserId(), status, page, size));
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
