package com.dyshop.api.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台订单 VO：C 端字段 + 下单人信息。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminOrderVO extends OrderVO {

    private Long userId;

    private String userName;
}
