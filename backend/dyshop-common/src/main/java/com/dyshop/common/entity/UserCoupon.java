package com.dyshop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户持有券表 user_coupon（ch11）。
 * status: 0=未使用 1=已使用 2=已过期  source: CENTER领取 / MANUAL发放
 */
@Data
@TableName("user_coupon")
public class UserCoupon implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long templateId;

    /** 0 未使用 / 1 已使用 / 2 已过期 */
    private Integer status;

    /** CENTER 领取 / MANUAL 发放 */
    private String source;

    /** 占用订单 ID（下单写入；回退置空） */
    private Long usedOrderId;

    private LocalDateTime receivedAt;

    /** 有效期到期（FIXED 复制 end_at / AFTER_DAYS 领取+valid_days） */
    private LocalDateTime expireAt;

    private LocalDateTime usedAt;
}
