package com.dyshop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分批次表 point_batch（ch13）。
 * 一次积分入账一个批次，expire_at=到账+12个月，兑换按 FIFO 扣减 remaining。
 */
@Data
@TableName("point_batch")
public class PointBatch implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 来源: ORDER=订单发放 */
    private String sourceType;

    /** 来源单号(订单ID) */
    private Long sourceId;

    /** 本批次积分 */
    private Integer points;

    /** 剩余可用积分 */
    private Integer remaining;

    /** 到期时间(到账+12个月) */
    private LocalDateTime expireAt;

    private LocalDateTime createTime;
}