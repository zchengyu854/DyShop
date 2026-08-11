package com.dyshop.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dyshop.api.mapper.CategoryMapper;
import com.dyshop.api.mapper.ProductMapper;
import com.dyshop.api.util.SkuJsonUtils;
import com.dyshop.api.vo.ProductDetailVO;
import com.dyshop.api.vo.ProductVO;
import com.dyshop.common.entity.Category;
import com.dyshop.common.entity.Product;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.PageResult;
import com.dyshop.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    public PageResult<ProductVO> pageProducts(int page, int size, Long categoryId, String keyword) {
        Page<Product> p = new Page<>(page, size);
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        qw.eq(Product::getStatus, 1);
        // 分类启用过滤（ch08）：停用分类下的商品对 C 端隐藏
        List<Long> enabledCategoryIds = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1))
                .stream().map(Category::getId).toList();
        if (enabledCategoryIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, page, size);
        }
        qw.in(Product::getCategoryId, enabledCategoryIds);
        if (categoryId != null) {
            qw.eq(Product::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            qw.like(Product::getName, keyword.trim());
        }
        qw.orderByDesc(Product::getCreateTime);

        productMapper.selectPage(p, qw);
        List<ProductVO> records = p.getRecords().stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    public ProductDetailVO getProductDetail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || !Objects.equals(product.getStatus(), 1)) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在");
        }
        // 分类停用（ch08）：详情同样对 C 端隐藏
        Category category = categoryMapper.selectById(product.getCategoryId());
        if (category == null || !Objects.equals(category.getStatus(), 1)) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在");
        }
        return toDetailVO(product);
    }

    private ProductVO toVO(Product p) {
        ProductVO vo = new ProductVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setSubtitle(p.getSubtitle());
        vo.setMainImage(p.getMainImage());
        vo.setPrice(p.getPrice());
        vo.setOriginalPrice(p.getOriginalPrice());
        vo.setVipPrice(p.getVipPrice());
        vo.setSales(p.getSales());
        return vo;
    }

    private ProductDetailVO toDetailVO(Product p) {
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(p.getId());
        vo.setCategoryId(p.getCategoryId());
        vo.setName(p.getName());
        vo.setSubtitle(p.getSubtitle());
        vo.setMainImage(p.getMainImage());
        vo.setImages(StringUtils.hasText(p.getImages())
                ? Arrays.stream(p.getImages().split(",")).map(String::trim).toList()
                : Collections.emptyList());
        vo.setDetail(p.getDetail());
        vo.setSpecs(SkuJsonUtils.parseSpecs(p.getSpecs()));
        vo.setSkus(SkuJsonUtils.parseSkus(p.getSkus()));
        vo.setPrice(p.getPrice());
        vo.setOriginalPrice(p.getOriginalPrice());
        vo.setVipPrice(p.getVipPrice());
        vo.setStock(p.getStock());
        vo.setSales(p.getSales());
        return vo;
    }
}
