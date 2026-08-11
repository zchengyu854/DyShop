package com.dyshop.api.controller.order;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dyshop.api.service.impl.OrderServiceImpl;
import com.dyshop.api.vo.AdminOrderVO;
import com.dyshop.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台订单接口（需 ROLE_ADMIN，SecurityConfig 已配置 hasRole("ADMIN")）。
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderServiceImpl orderService;

    @GetMapping
    public Result<IPage<AdminOrderVO>> list(@RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.success(orderService.adminList(status, keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<AdminOrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.adminGet(id));
    }

    @PostMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id) {
        orderService.ship(id);
        return Result.success();
    }
}