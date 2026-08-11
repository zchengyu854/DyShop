package com.dyshop.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台积分商城商品 VO（ch13）。
 */
@Data
public class PointsGoodsAdminVO {

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

    private Long couponTemplateId;

    /** 关联券模板名（冗余展示） */
    private String couponTemplateName;

    /** 1上架 0下架 */
    private Integer status;

    private Integer sort;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}