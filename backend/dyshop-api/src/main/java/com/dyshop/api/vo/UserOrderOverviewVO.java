package com.dyshop.api.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 个人中心账户数据概览（ch08.3：累计消费 / 订单总数动态统计）。
 */
@Data
public class UserOrderOverviewVO {

    /** 累计消费：已完成订单应付金额合计 */
    private BigDecimal totalConsumption;

    /** 订单总数（本人全部订单，含已取消） */
    private Long totalOrders;

    /** 待发货订单数 */
    private Long waitShip;

    /** 待收货订单数 */
    private Long waitReceive;
}