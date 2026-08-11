package com.dyshop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单表 orders。
 * status: 0=待支付 1=待发货 2=待收货 3=已完成 4=已取消
 */
@Data
@TableName("orders")
public class Order implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号（业务唯一） */
    private String orderNo;

    private Long userId;

    /** 订单总额 */
    private BigDecimal totalAmount;

    /** 应付金额 */
    private BigDecimal payAmount;

    /** 优惠总额（会员/券自动取优后；无优惠为 0，ch11） */
    private BigDecimal discountAmount;

    private Integer status;

    /** 收货信息快照（下单时固化） */
    private String receiverName;

    private String receiverPhone;

    private String receiverAddr;

    /** 买家备注 */
    private String remark;

    private LocalDateTime payTime;

    private LocalDateTime shipTime;

    private LocalDateTime finishTime;

    private LocalDateTime cancelTime;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
