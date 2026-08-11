package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 结算页可用券选项 VO（ch11）。
 */
@Data
public class CouponOptionVO implements Serializable {

    private Long userCouponId;

    private String name;

    private BigDecimal minAmount;

    private BigDecimal discountAmount;

    private String scope;

    /** 是否可用 */
    private boolean applicable;

    /** 不可用原因（可用时为 null）；如「还差 ¥X 可用」「已过期」「该券不可与会员折扣同用」 */
    private String reason;

    /** 可用时的预计抵扣额 */
    private BigDecimal discount;
}
