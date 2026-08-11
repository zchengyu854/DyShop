package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 分类 VO。
 */
@Data
public class CategoryVO implements Serializable {

    private Long id;

    private String name;

    private Integer sort;
}
