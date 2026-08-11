package com.dyshop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细表 order_item（商品信息快照，改价/改名不影响历史订单）。
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long productId;

    /** 规格SKU ID: 0=无规格商品（取消订单按此回补 SKU 显示库存） */
    private Long skuId;

    /** 规格快照，如「型号:MacBook Air, 颜色:深空灰」；无规格商品为 NULL */
    private String specText;

    /** 商品名称（快照） */
    private String productName;

    /** 商品主图（快照） */
    private String productImage;

    /** 成交单价（快照） */
    private BigDecimal price;

    private Integer quantity;

    /** 小计金额 */
    private BigDecimal subtotal;

    private LocalDateTime createTime;
}
