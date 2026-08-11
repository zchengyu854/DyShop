package com.dyshop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水表 payment（模拟支付通道 MOCK）。
 * status: 0=处理中 1=成功 2=失败/关闭
 */
@Data
@TableName("payment")
public class Payment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 支付流水号（业务唯一） */
    private String paymentNo;

    private Long orderId;

    private Long userId;

    private BigDecimal amount;

    /** 支付渠道（当前为模拟） */
    private String channel;

    private Integer status;

    /** 支付成功时间 */
    private LocalDateTime paidAt;

    private LocalDateTime createTime;
}
