package com.dyshop.api.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 个人中心首页聚合统计（ch12，GET /api/user/dashboard-stats）。
 * 一次请求合并订单概览 + 会员全景，避免前端并发多次请求。
 */
@Data
public class DashboardStatsVO {

    /** 累计消费：已完成订单应付金额合计（退款扣减） */
    private BigDecimal totalSpent;

    /** 订单总数（本人全部订单，含已取消） */
    private Long totalOrders;

    /** 待发货订单数 */
    private Long pendingShipment;

    /** 待收货订单数 */
    private Long pendingReceive;

    /** 积分余额 */
    private Integer points;

    /** 当前等级标识: NORMAL/SILVER/GOLD/DIAMOND */
    private String levelCode;

    /** 当前等级名 */
    private String levelName;

    /** 下一级门槛（null=已达最高等级） */
    private BigDecimal nextLevelThreshold;

    /** 距下一级还差金额（满级为 null） */
    private BigDecimal needAmount;

    /** 升级进度（0-100，满级 100） */
    private Integer progressPct;
}