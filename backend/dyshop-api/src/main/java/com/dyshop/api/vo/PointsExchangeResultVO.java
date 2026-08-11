package com.dyshop.api.vo;

import lombok.Data;

/**
 * 兑换结果 VO（ch13，POST /api/user/points/exchange）。
 */
@Data
public class PointsExchangeResultVO {

    /** 兑换单号 */
    private String exchangeNo;

    /** COUPON=发券 / CODE=兑换码 */
    private String goodsType;

    /** 快照消耗积分 */
    private Integer pointCost;

    /** CODE 类型：兑换码 */
    private String code;

    /** COUPON 类型：发放的 user_coupon.id */
    private Long couponId;
}