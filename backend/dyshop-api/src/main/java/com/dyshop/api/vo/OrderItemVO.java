package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细 VO（商品快照）。
 */
@Data
public class OrderItemVO implements Serializable {

    /** order_item 主键（ch12 售后申请入参） */
    private Long id;

    private Long productId;

    /** 规格快照；无规格商品为 NULL */
    private String specText;

    private String productName;

    private String productImage;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal subtotal;
}
