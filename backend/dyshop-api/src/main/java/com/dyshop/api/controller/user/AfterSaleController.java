package com.dyshop.api.controller.user;

import com.dyshop.api.dto.AfterSaleApplyDTO;
import com.dyshop.api.service.impl.AfterSaleServiceImpl;
import com.dyshop.api.vo.AfterSaleVO;
import com.dyshop.common.result.PageResult;
import com.dyshop.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端售后接口（ch12，需登录）。
 */
@RestController
@RequestMapping("/api/user/after-sales")
@RequiredArgsConstructor
public class AfterSaleController {

    private final AfterSaleServiceImpl afterSaleService;

    /** 申请售后（仅已完成订单；同一商品行仅一次） */
    @PostMapping
    public Result<AfterSaleVO> apply(@Valid @RequestBody AfterSaleApplyDTO dto) {
        return Result.success(afterSaleService.apply(currentUserId(), dto));
    }

    /** 我的售后列表（status 筛选 + 分页） */
    @GetMapping
    public Result<PageResult<AfterSaleVO>> list(@RequestParam(required = false) Integer status,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size) {
        return Result.success(afterSaleService.mine(currentUserId(), status, page, size));
    }

    /** 售后详情（越权 404） */
    @GetMapping("/{id}")
    public Result<AfterSaleVO> detail(@PathVariable Long id) {
        return Result.success(afterSaleService.detail(currentUserId(), id));
    }

    /** 取消申请（仅待处理） */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        afterSaleService.cancel(currentUserId(), id);
        return Result.success();
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
