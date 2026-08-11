package com.dyshop.api.controller.order;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dyshop.api.service.impl.AfterSaleServiceImpl;
import com.dyshop.api.vo.AdminAfterSaleVO;
import com.dyshop.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台售后接口（ch12，需 ROLE_ADMIN）。
 */
@RestController
@RequestMapping("/api/admin/after-sales")
@RequiredArgsConstructor
public class AdminAfterSaleController {

    private final AfterSaleServiceImpl afterSaleService;

    /** 售后分页（status/keyword=订单号/用户名/商品名） */
    @GetMapping
    public Result<IPage<AdminAfterSaleVO>> list(@RequestParam(required = false) Integer status,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size) {
        return Result.success(afterSaleService.adminList(status, keyword, page, size));
    }

    /** 同意：模拟退款 → 已退款完成 */
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        afterSaleService.approve(id);
        return Result.success();
    }

    /** 拒绝：必填理由 */
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        afterSaleService.reject(id, body.get("reason"));
        return Result.success();
    }
}
