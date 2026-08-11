package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后单 VO（C 端我的售后 / 详情）。
 */
@Data
public class AfterSaleVO implements Serializable {

    private Long id;

    private String afterSaleNo;

    private Long orderId;

    private Long orderItemId;

    private Long productId;

    private String productName;

    private String productImage;

    private String specText;

    private Integer quantity;

    /** 退款金额（后端自动计算） */
    private BigDecimal refundAmount;

    private String reason;

    private String type;

    /** 0 待处理 / 1 退款中 / 2 已退款 / 3 已拒绝 / 4 已取消 */
    private Integer status;

    private String statusText;

    private String rejectReason;

    private LocalDateTime handleTime;

    private LocalDateTime cancelTime;

    private LocalDateTime createTime;
}
