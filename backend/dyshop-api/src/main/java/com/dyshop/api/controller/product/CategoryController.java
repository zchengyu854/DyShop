package com.dyshop.api.controller.product;

import com.dyshop.api.service.impl.CategoryServiceImpl;
import com.dyshop.api.vo.CategoryVO;
import com.dyshop.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户端分类接口（公开，免认证）。
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryServiceImpl categoryService;

    @GetMapping
    public Result<List<CategoryVO>> list() {
        return Result.success(categoryService.listEnabled());
    }
}
