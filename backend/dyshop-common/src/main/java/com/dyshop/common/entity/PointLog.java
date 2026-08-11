package com.dyshop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分流水表 point_log（ch09）。
 * order_id 唯一索引兜底幂等，同订单只记一条。
 */
@Data
@TableName("point_log")
public class PointLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long orderId;

    /** 变动积分(正=获得) */
    private Integer points;

    /** 变动后余额 */
    private Integer balance;

    private String remark;

    private LocalDateTime createTime;
}