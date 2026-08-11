package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后台分类管理 VO（含 status，供后台启停展示）。
 */
@Data
public class AdminCategoryVO implements Serializable {

    private Long id;

    private Long parentId;

    private String name;

    private Integer sort;

    /** 0=禁用 1=启用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}