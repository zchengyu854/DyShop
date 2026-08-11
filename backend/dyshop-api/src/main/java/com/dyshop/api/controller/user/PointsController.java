package com.dyshop.api.controller.user;

import com.dyshop.api.service.impl.PointsServiceImpl;
import com.dyshop.api.vo.PointsExchangeResultVO;
import com.dyshop.api.vo.PointsExchangeVO;
import com.dyshop.api.vo.PointsMallVO;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.PageResult;
import com.dyshop.common.result.Result;
import com.dyshop.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端积分商城接口（ch13，需登录）。
 */
@RestController
@RequestMapping("/api/user/points")
@RequiredArgsConstructor
public class PointsController {

    private static final int MAX_PAGE_SIZE = 50;

    private final PointsServiceImpl pointsService;

    /** 积分商城：在售商品 + 我的可用积分余额 */
    @GetMapping("/goods")
    public Result<PointsMallVO> mall() {
        return Result.success(pointsService.mall(currentUserId()));
    }

    /** 兑换虚拟商品 */
    @PostMapping("/exchange")
    public Result<PointsExchangeResultVO> exchange(@RequestParam Long goodsId) {
        return Result.success(pointsService.exchange(currentUserId(), goodsId));
    }

    /** 我的兑换记录分页 */
    @GetMapping("/exchanges")
    public Result<PageResult<PointsExchangeVO>> exchanges(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BizException(ResultCode.PARAM_ERROR, "size 必须在 1~" + MAX_PAGE_SIZE + " 之间");
        }
        return Result.success(pointsService.myExchanges(currentUserId(), page, size));
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}