package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 后台仪表盘概览 VO。
 */
@Data
public class OverviewVO implements Serializable {

    /** 今日订单数（今日创建，含待支付/已取消） */
    private Long todayOrderCount;

    /** 今日交易额（pay_time 今日且已支付成功 status∈1/2/3，Σ pay_amount） */
    private BigDecimal todayPaidAmount;

    /** 待支付订单数 */
    private Long waitPayCount;

    /** 待发货订单数 */
    private Long waitShipCount;

    /** 商品总数（未删除） */
    private Long productCount;

    /** 用户总数（未删除） */
    private Long userCount;
}