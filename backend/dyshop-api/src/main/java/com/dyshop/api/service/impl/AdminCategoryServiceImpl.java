package com.dyshop.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dyshop.api.dto.CategoryDTO;
import com.dyshop.api.mapper.CategoryMapper;
import com.dyshop.api.mapper.ProductMapper;
import com.dyshop.api.vo.AdminCategoryVO;
import com.dyshop.common.entity.Category;
import com.dyshop.common.entity.Product;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 后台分类管理实现。
 */
@Service
@RequiredArgsConstructor
public class AdminCategoryServiceImpl {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    public List<AdminCategoryVO> list() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSort).orderByAsc(Category::getId))
                .stream().map(this::toVo).toList();
    }

    public void create(CategoryDTO dto) {
        Category category = new Category();
        category.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        category.setName(dto.getName().trim());
        category.setSort(dto.getSort());
        category.setStatus(1);
        categoryMapper.insert(category);
    }

    public void update(Long id, CategoryDTO dto) {
        Category category = requireCategory(id);
        category.setName(dto.getName().trim());
        category.setSort(dto.getSort());
        categoryMapper.updateById(category);
    }

    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态参数错误");
        }
        Category category = requireCategory(id);
        category.setStatus(status);
        categoryMapper.updateById(category);
    }

    public void remove(Long id) {
        requireCategory(id);
        // 引用检查：本分类 + 子分类下存在未删除商品 → 拒绝删除
        List<Long> ids = List.of(id);
        List<Long> childIds = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .eq(Category::getParentId, id))
                .stream().map(Category::getId).toList();
        if (!childIds.isEmpty()) {
            ids = java.util.stream.Stream.concat(ids.stream(), childIds.stream()).toList();
        }
        long ref = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .in(Product::getCategoryId, ids));
        if (ref > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "分类下存在商品，无法删除");
        }
        // @TableLogic：逻辑删除
        categoryMapper.deleteById(id);
    }

    // ---------- 私有 ----------

    private Category requireCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ResultCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private AdminCategoryVO toVo(Category c) {
        AdminCategoryVO vo = new AdminCategoryVO();
        vo.setId(c.getId());
        vo.setParentId(c.getParentId());
        vo.setName(c.getName());
        vo.setSort(c.getSort());
        vo.setStatus(c.getStatus());
        vo.setCreateTime(c.getCreateTime());
        vo.setUpdateTime(c.getUpdateTime());
        return vo;
    }
}