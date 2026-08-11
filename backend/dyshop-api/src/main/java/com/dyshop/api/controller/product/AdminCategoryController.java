package com.dyshop.api.controller.product;

import com.dyshop.api.dto.CategoryDTO;
import com.dyshop.api.service.impl.AdminCategoryServiceImpl;
import com.dyshop.api.vo.AdminCategoryVO;
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

import java.util.List;

/**
 * 后台分类管理接口（需 ROLE_ADMIN）。
 */
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryServiceImpl adminCategoryService;

    @GetMapping
    public Result<List<AdminCategoryVO>> list() {
        return Result.success(adminCategoryService.list());
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody CategoryDTO dto) {
        adminCategoryService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        adminCategoryService.update(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminCategoryService.changeStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        adminCategoryService.remove(id);
        return Result.success();
    }
}