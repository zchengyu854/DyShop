package com.dyshop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后单表 after_sale（ch12）。
 * status: 0=待处理 1=退款中 2=已退款完成 3=已拒绝 4=已取消
 */
@Data
@TableName("after_sale")
public class AfterSale implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 售后单号（业务唯一） */
    private String afterSaleNo;

    private Long orderId;

    /** 商品行 ID（唯一索引：同一行仅可申请一次） */
    private Long orderItemId;

    private Long userId;

    private Long productId;

    private String productName;

    private String productImage;

    private String specText;

    /** 退款数量（=行数量，本期不支持部分退） */
    private Integer quantity;

    /** 退款金额 = 成交单价 × 数量（后端自动计算） */
    private BigDecimal refundAmount;

    private String reason;

    /** 售后类型：本期仅 ONLY_REFUND */
    private String type;

    /** 0 待处理 / 1 退款中 / 2 已退款 / 3 已拒绝 / 4 已取消 */
    private Integer status;

    private String rejectReason;

    private LocalDateTime handleTime;

    private LocalDateTime cancelTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
