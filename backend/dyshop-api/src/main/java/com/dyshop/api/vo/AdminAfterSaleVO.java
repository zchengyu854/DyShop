package com.dyshop.api.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台售后单 VO：C 端字段 + 订单号/申请人。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminAfterSaleVO extends AfterSaleVO {

    private String orderNo;

    private String username;
}
