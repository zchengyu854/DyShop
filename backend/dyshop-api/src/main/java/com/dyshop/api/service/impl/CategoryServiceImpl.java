package com.dyshop.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dyshop.api.mapper.CategoryMapper;
import com.dyshop.api.vo.CategoryVO;
import com.dyshop.common.entity.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl {

    private final CategoryMapper categoryMapper;

    public List<CategoryVO> listEnabled() {
        LambdaQueryWrapper<Category> qw = new LambdaQueryWrapper<>();
        qw.eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort);
        return categoryMapper.selectList(qw).stream()
                .map(c -> {
                    CategoryVO vo = new CategoryVO();
                    vo.setId(c.getId());
                    vo.setName(c.getName());
                    vo.setSort(c.getSort());
                    return vo;
                })
                .toList();
    }
}
