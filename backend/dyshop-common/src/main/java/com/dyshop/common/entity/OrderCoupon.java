package com.dyshop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单券快照表 order_coupon（ch11；order_id 唯一=一单一券）。
 */
@Data
@TableName("order_coupon")
public class OrderCoupon implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 ID（唯一约束=一单一券） */
    private Long orderId;

    /** 消费的持有券实例 */
    private Long userCouponId;

    /** 模板冗余 */
    private Long templateId;

    /** 名称快照（模板后改不影响历史） */
    private String templateName;

    /** 范围快照：ALL / LIMITED */
    private String scope;

    /** 分类范围快照（JSON 数组文本） */
    private String categoryIds;

    /** 商品范围快照（JSON 数组文本） */
    private String productIds;

    /** 实际抵扣额 */
    private BigDecimal discountAmount;

    private LocalDateTime usedAt;
}
