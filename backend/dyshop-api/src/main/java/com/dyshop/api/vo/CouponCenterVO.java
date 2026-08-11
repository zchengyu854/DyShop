package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 领券中心条目 VO（ch11）。
 */
@Data
public class CouponCenterVO implements Serializable {

    private CouponTemplateVO template;

    /** 当前用户是否已领取（CENTER 源） */
    private boolean claimed;

    /** 剩余可领量；-1=不限 */
    private long remaining;
}
