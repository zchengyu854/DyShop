package com.dyshop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 购物车表 cart_item（user_id + product_id 唯一）。
 */
@Data
@TableName("cart_item")
public class CartItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long productId;

    /** 规格SKU ID: 0=无规格商品 */
    private Long skuId;

    /** 规格展示快照（服务端按 SKU 生成），如「颜色:黑色, 版本:标准版」 */
    private String specText;

    private Integer quantity;

    /** 是否勾选: 0否 1是（本期未启用，结算模块使用） */
    private Integer checked;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
