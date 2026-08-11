package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 结算价预览 VO（ch11：GET /api/orders/preview）。
 * 金额口径与下单一致（会员价已含，券在订单级立减）。
 */
@Data
public class OrderPreviewVO implements Serializable {

    /** 结算行明细（会员价口径） */
    private List<OrderItemVO> lines;

    /** 原价合计（商品总额，优惠前基准；ch11 起 total_amount 使用该口径） */
    private BigDecimal totalAmount;

    /** 会员优惠额（未用券/券未生效时生效；用券后为 0） */
    private BigDecimal memberBenefit;

    /** 券抵扣额（券方案生效时为实际抵扣，否则 0） */
    private BigDecimal couponDiscount;

    /** 应付金额 = totalAmount − 总优惠（自动取优后） */
    private BigDecimal payAmount;

    /** 选中的券（couponId 参数时返回；未选为 null） */
    private OrderCouponVO coupon;

    /**
     * 选中的券是否实际生效（ch11 二选一自动取优）：
     * false = 会员价更优惠，已自动采用会员价方案（券未生效）。
     */
    private boolean couponApplied;

    /** 全部未使用未过期券的可用性（供结算页渲染选券清单） */
    private List<CouponOptionVO> couponOptions;
}
