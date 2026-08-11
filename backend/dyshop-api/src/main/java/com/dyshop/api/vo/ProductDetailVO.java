package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品详情 VO。
 */
@Data
public class ProductDetailVO implements Serializable {

    private Long id;

    private Long categoryId;

    private String name;

    private String subtitle;

    private String mainImage;

    private List<String> images;

    private String detail;

    /** 规格维度定义；无规格商品为空列表 */
    private List<SpecVO> specs;

    /** SKU 列表；无规格商品为空列表 */
    private List<SkuVO> skus;

    private BigDecimal price;

    private BigDecimal originalPrice;

    /** 会员专享价(ch09)，NULL=未设置 */
    private BigDecimal vipPrice;

    private Integer stock;

    private Integer sales;
}
