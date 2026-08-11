package com.dyshop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分商城商品表 points_goods（ch13）。
 * goods_type: COUPON=发券 / CODE=兑换码；stock=-1 不限；status=1 上架。
 */
@Data
@TableName("points_goods")
public class PointsGoods implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String coverImage;

    private String description;

    /** COUPON=发券 / CODE=兑换码 */
    private String goodsType;

    /** 兑换所需积分(>0) */
    private Integer pointCost;

    /** 库存, -1=不限 */
    private Integer stock;

    /** 每人限兑次数, 0=不限 */
    private Integer limitPerUser;

    /** COUPON类关联优惠券模板 */
    private Long couponTemplateId;

    /** 1上架 0下架 */
    private Integer status;

    private Integer sort;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}