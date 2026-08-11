package com.dyshop.api.controller.order;

import com.dyshop.api.dto.CreateOrderDTO;
import com.dyshop.api.service.impl.OrderServiceImpl;
import com.dyshop.api.vo.OrderPreviewVO;
import com.dyshop.api.vo.OrderVO;
import com.dyshop.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单接口（C 端，全部需认证，principal=userId）。
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderServiceImpl orderService;

    @PostMapping
    public Result<OrderVO> create(@Valid @RequestBody CreateOrderDTO dto) {
        return Result.success(orderService.createOrder(currentUserId(), dto));
    }

    /**
     * 结算价预览（ch11）：明细 + 会员价 + 券抵扣 + 应付 + 可用券清单。
     * source=cart 读勾选购物车；source=buyNow 需 productId/skuId/quantity。
     */
    @GetMapping("/preview")
    public Result<OrderPreviewVO> preview(@RequestParam(required = false, defaultValue = "cart") String source,
                                          @RequestParam(required = false) Long couponId,
                                          @RequestParam(required = false) Long productId,
                                          @RequestParam(required = false) Long skuId,
                                          @RequestParam(required = false) Integer quantity) {
        return Result.success(orderService.preview(currentUserId(), source, couponId, productId, skuId, quantity));
    }

    @GetMapping
    public Result<List<OrderVO>> list(@RequestParam(required = false) Integer status) {
        return Result.success(orderService.listOrders(currentUserId(), status));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.getOrder(currentUserId(), id));
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(currentUserId(), id);
        return Result.success();
    }

    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.pay(currentUserId(), id);
        return Result.success();
    }

    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        orderService.confirm(currentUserId(), id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        orderService.remove(currentUserId(), id);
        return Result.success();
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
