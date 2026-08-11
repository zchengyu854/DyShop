package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单券快照 VO（ch11，订单列表/详情展示优惠条目）。
 */
@Data
public class OrderCouponVO implements Serializable {

    private Long id;

    private String templateName;

    private BigDecimal discountAmount;
}
