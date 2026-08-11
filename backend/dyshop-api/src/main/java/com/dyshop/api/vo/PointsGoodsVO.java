package com.dyshop.api.vo;

import lombok.Data;

/**
 * 积分商城商品 VO（ch13，C 端在售列表项）。
 */
@Data
public class PointsGoodsVO {

    private Long id;

    private String name;

    private String coverImage;

    private String description;

    /** COUPON=发券 / CODE=兑换码 */
    private String goodsType;

    private Integer pointCost;

    /** 库存 -1=不限 */
    private Integer stock;

    /** 每人限兑次数 0=不限 */
    private Integer limitPerUser;

    /** 本人已兑次数 */
    private Integer exchangedCount;

    /** 已上架标识（前端透传） */
    private Integer status;
}