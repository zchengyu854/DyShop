package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品列表卡片 VO。
 */
@Data
public class ProductVO implements Serializable {

    private Long id;

    private String name;

    private String subtitle;

    private String mainImage;

    private BigDecimal price;

    private BigDecimal originalPrice;

    /** 会员专享价(ch09)，NULL=未设置 */
    private BigDecimal vipPrice;

    private Integer sales;
}
