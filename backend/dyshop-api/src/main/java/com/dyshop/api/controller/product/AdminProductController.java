package com.dyshop.api.controller.product;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dyshop.api.dto.ProductDTO;
import com.dyshop.api.service.impl.AdminProductServiceImpl;
import com.dyshop.api.vo.AdminProductVO;
import com.dyshop.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台商品管理接口（需 ROLE_ADMIN）。
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductServiceImpl adminProductService;

    @GetMapping
    public Result<IPage<AdminProductVO>> list(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size) {
        return Result.success(adminProductService.list(keyword, categoryId, status, page, size));
    }

    @GetMapping("/{id}")
    public Result<AdminProductVO> detail(@PathVariable Long id) {
        return Result.success(adminProductService.get(id));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody ProductDTO dto) {
        adminProductService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductDTO dto) {
        adminProductService.update(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminProductService.changeStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        adminProductService.remove(id);
        return Result.success();
    }
}