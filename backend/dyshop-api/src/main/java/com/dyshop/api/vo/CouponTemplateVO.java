package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板 VO（后台/领券中心共用展示）。
 */
@Data
public class CouponTemplateVO implements Serializable {

    private Long id;

    private String name;

    private String type;

    private BigDecimal minAmount;

    private BigDecimal discountAmount;

    private String scope;

    /** JSON 数组文本 */
    private String categoryIds;

    /** JSON 数组文本 */
    private String productIds;

    private Integer allowStack;

    private String issueType;

    private String validType;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Integer validDays;

    private Integer totalQuantity;

    private Integer perUser;

    private Integer issuedCount;

    private Integer status;

    private LocalDateTime createAt;

    private LocalDateTime updateAt;
}
