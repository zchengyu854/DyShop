package com.dyshop.api.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员全景 VO（ch09，GET /api/user/member/overview）。
 */
@Data
public class MemberOverviewVO {

    /** 当前等级 */
    private MemberLevelVO level;

    /** 累计消费（全部已支付订单，口径同 ch08.4 overview；退款扣减） */
    private BigDecimal totalConsumption;

    /** 近12个月消费（等级判定口径） */
    private BigDecimal annualConsumption;

    /** 下一级（null=已达最高等级） */
    private MemberLevelVO nextLevel;

    /** 距下一级还差金额（满级为 null） */
    private BigDecimal needAmount;

    /** 升级进度（0-100，满级 100） */
    private Integer progressPct;

    /** 积分余额 */
    private Integer points;
}