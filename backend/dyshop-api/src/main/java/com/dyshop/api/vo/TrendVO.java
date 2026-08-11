package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 后台仪表盘趋势 VO（按日聚合）。
 */
@Data
public class TrendVO implements Serializable {

    /** 日期序列（yyyy-MM-dd），与下面两数组一一对应 */
    private List<String> dates;

    /** 订单数序列（按日 create_time 聚合） */
    private List<Long> orderCounts;

    /** 交易金额序列（按日 pay_time 聚合，pay_amount Σ） */
    private List<BigDecimal> paidAmounts;
}