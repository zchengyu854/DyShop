package com.dyshop.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台兑换记录 VO（ch13）。
 */
@Data
public class PointsExchangeAdminVO {

    private Long id;

    private String exchangeNo;

    private String username;

    private String nickname;

    private Long goodsId;

    private String goodsName;

    private String goodsType;

    private Integer pointCost;

    private String code;

    private Long couponId;

    private LocalDateTime createTime;
}