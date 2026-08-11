package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 购物车条目 VO（含商品信息）。
 */
@Data
public class CartItemVO implements Serializable {

    private Long cartItemId;

    private Long productId;

    /** 规格SKU ID: 0=无规格商品 */
    private Long skuId;

    /** 规格展示快照 */
    private String specText;

    private String name;

    private String subtitle;

    private String mainImage;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    /** 商品上架状态: 0=下架 1=上架（下架商品在购物车中保留展示、不可购买） */
    private Integer productStatus;

    private Integer sales;

    private Integer quantity;

    /** 是否勾选: 0否 1是 */
    private Integer checked;
}
