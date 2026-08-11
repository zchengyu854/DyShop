package com.dyshop.api.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员等级信息（ch09）。
 */
@Data
public class MemberLevelVO {

    /** 等级 ID（后台保存接口 /levels/{id} 依赖，ch11 修复缺失） */
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
}