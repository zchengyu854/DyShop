package com.dyshop.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的兑换记录 VO（ch13）。
 */
@Data
public class PointsExchangeVO {

    private Long id;

    private String exchangeNo;

    private Long goodsId;

    /** 商品名快照 */
    private String goodsName;

    /** 快照类型 COUPON/CODE */
    private String goodsType;

    /** 快照消耗积分 */
    private Integer pointCost;

    /** CODE 类型兑换码 */
    private String code;

    /** COUPON 类型发放的 user_coupon.id */
    private Long couponId;

    private LocalDateTime createTime;
}