package com.dyshop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分兑换记录表 points_exchange（ch13）。
 * 商品名/积分价快照；COUPON 类记 coupon_id，CODE 类记 code(唯一)。
 */
@Data
@TableName("points_exchange")
public class PointsExchange implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 兑换单号(业务唯一) */
    private String exchangeNo;

    private Long userId;

    private Long goodsId;

    /** 商品名快照 */
    private String goodsName;

    /** 快照类型 COUPON/CODE */
    private String goodsType;

    /** 快照消耗积分 */
    private Integer pointCost;

    /** CODE类型兑换码(唯一) */
    private String code;

    /** COUPON类型发放的 user_coupon.id */
    private Long couponId;

    private LocalDateTime createTime;
}