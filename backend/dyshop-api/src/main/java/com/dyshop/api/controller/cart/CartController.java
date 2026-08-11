package com.dyshop.api.controller.cart;

import com.dyshop.api.dto.CartAddDTO;
import com.dyshop.api.dto.CartCheckedDTO;
import com.dyshop.api.dto.CartQuantityDTO;
import com.dyshop.api.service.impl.CartServiceImpl;
import com.dyshop.api.vo.CartItemVO;
import com.dyshop.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车接口（全部需认证，principal=userId）。
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartServiceImpl cartService;

    @GetMapping
    public Result<List<CartItemVO>> list() {
        return Result.success(cartService.listCart(currentUserId()));
    }

    @PostMapping("/items")
    public Result<Void> addItem(@Valid @RequestBody CartAddDTO dto) {
        cartService.addItem(currentUserId(), dto.getProductId(), dto.getSkuId(), dto.getQuantity());
        return Result.success();
    }

    @PutMapping("/items/{cartItemId}")
    public Result<Void> updateQuantity(@PathVariable Long cartItemId, @Valid @RequestBody CartQuantityDTO dto) {
        cartService.updateQuantity(currentUserId(), cartItemId, dto.getQuantity());
        return Result.success();
    }

    @PutMapping("/items/{cartItemId}/checked")
    public Result<Void> updateChecked(@PathVariable Long cartItemId, @Valid @RequestBody CartCheckedDTO dto) {
        cartService.updateChecked(currentUserId(), cartItemId, dto.getChecked());
        return Result.success();
    }

    @DeleteMapping("/items/{cartItemId}")
    public Result<Void> removeItem(@PathVariable Long cartItemId) {
        cartService.removeItem(currentUserId(), cartItemId);
        return Result.success();
    }

    @DeleteMapping
    public Result<Void> clear() {
        cartService.clear(currentUserId());
        return Result.success();
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
