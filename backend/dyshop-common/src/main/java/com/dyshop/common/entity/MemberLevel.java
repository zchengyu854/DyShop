package com.dyshop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员等级配置表 member_level（ch09）。
 * 等级实时计算不落库：近12个月已完成订单 pay_amount 之和匹配 threshold。
 */
@Data
@TableName("member_level")
public class MemberLevel implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 等级标识: NORMAL/SILVER/GOLD/DIAMOND */
    private String code;

    private String name;

    /** 近12个月消费门槛 */
    private BigDecimal threshold;

    /** 订单折扣率: 0.98=98折 */
    private BigDecimal discountRate;

    /** 积分倍率 */
    private BigDecimal pointRate;

    /** 排序(升序=等级从低到高) */
    private Integer sort;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}