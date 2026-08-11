package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 我的优惠券 VO（ch11，含模板快照字段）。
 */
@Data
public class MyCouponVO implements Serializable {

    private Long id;

    private Long templateId;

    private String name;

    private BigDecimal minAmount;

    private BigDecimal discountAmount;

    private String scope;

    private String categoryIds;

    private String productIds;

    private Integer allowStack;

    /** 展示层状态：0 未使用 / 1 已使用 / 2 已过期（过期惰性判定） */
    private Integer status;

    /** CENTER / MANUAL */
    private String source;

    private Long usedOrderId;

    private LocalDateTime receivedAt;

    private LocalDateTime expireAt;
}
