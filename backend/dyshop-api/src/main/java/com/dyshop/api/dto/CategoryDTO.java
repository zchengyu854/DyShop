package com.dyshop.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 后台分类新增/编辑 DTO。
 */
@Data
public class CategoryDTO {

    /** 父分类 ID（本期固定顶级 0，预留字段） */
    private Long parentId;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称最多 50 字")
    private String name;

    @NotNull(message = "排序值不能为空")
    private Integer sort;
}