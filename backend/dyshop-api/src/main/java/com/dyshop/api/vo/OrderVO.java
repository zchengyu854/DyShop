package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 VO（C 端）。
 */
@Data
public class OrderVO implements Serializable {

    private Long id;

    private String orderNo;

    private Integer status;

    /** 状态文字：待支付/待发货/待收货/已完成/已取消 */
    private String statusText;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    /** 券优惠总额（ch11；无券为 0） */
    private BigDecimal discountAmount;

    /** 订单券快照（ch11；无券为 null） */
    private OrderCouponVO coupon;

    private String remark;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddr;

    private LocalDateTime payTime;

    private LocalDateTime shipTime;

    private LocalDateTime finishTime;

    private LocalDateTime cancelTime;

    private LocalDateTime createTime;

    /** 支付截止时间（仅待支付返回：createTime + 15 分钟，前端倒计时的权威基准） */
    private LocalDateTime payDeadline;

    private List<OrderItemVO> items;
}
