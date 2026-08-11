package com.dyshop.api.controller.product;

import com.dyshop.api.service.impl.ProductServiceImpl;
import com.dyshop.api.vo.ProductDetailVO;
import com.dyshop.api.vo.ProductVO;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.PageResult;
import com.dyshop.common.result.Result;
import com.dyshop.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端商品接口（公开，免认证）。
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ProductServiceImpl productService;

    /**
     * 商品分页列表（仅上架），支持分类/关键词筛选。
     */
    @GetMapping
    public Result<PageResult<ProductVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        if (page < 1) {
            throw new BizException(ResultCode.PARAM_ERROR, "page 必须大于等于 1");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BizException(ResultCode.PARAM_ERROR, "size 必须在 1~" + MAX_PAGE_SIZE + " 之间");
        }
        return Result.success(productService.pageProducts(page, size, categoryId, keyword));
    }

    /**
     * 商品详情（仅上架）。
     */
    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.success(productService.getProductDetail(id));
    }
}
