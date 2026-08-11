package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 后台商品列表/详情 VO（详情含 specs/skus 原文）。
 */
@Data
public class AdminProductVO implements Serializable {

    private Long id;

    private Long categoryId;

    private String categoryName;

    private String name;

    private String subtitle;

    private String mainImage;

    /** 轮播图 URL，逗号分隔 */
    private String images;

    private String detail;

    /** 规格维度定义 JSON 原文（null = 无规格） */
    private String specs;

    /** SKU 列表 JSON 原文（null = 无规格） */
    private String skus;

    private BigDecimal price;

    private BigDecimal originalPrice;

    /** 会员专享价(ch09) */
    private BigDecimal vipPrice;

    private Integer stock;

    private Integer sales;

    /** 0=下架 1=上架 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}