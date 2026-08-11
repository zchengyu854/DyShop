package com.dyshop.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板新建/编辑 DTO（ch11）。
 * categoryIds / productIds 为 JSON 数组文本（如 "[1,2]"），与既有 specs/skus 风格一致。
 */
@Data
public class CouponTemplateDTO {

    @NotBlank(message = "模板名称不能为空")
    private String name;

    /** 券类型：本期仅 REDUCE（立减型） */
    @Pattern(regexp = "REDUCE", message = "本期仅支持立减券")
    private String type = "REDUCE";

    /** 满减门槛，0=无门槛 */
    private BigDecimal minAmount = BigDecimal.ZERO;

    @NotNull(message = "立减金额不能为空")
    @DecimalMin(value = "0.01", message = "立减金额必须大于 0")
    private BigDecimal discountAmount;

    @NotBlank(message = "适用范围不能为空")
    @Pattern(regexp = "ALL|LIMITED", message = "适用范围不合法")
    private String scope = "ALL";

    /** 指定分类 id 数组（JSON 数组文本；LIMITED 时与 productIds 并集生效） */
    private String categoryIds;

    /** 指定商品 id 数组（JSON 数组文本） */
    private String productIds;

    /** 是否可与会员折扣叠加：1 是 / 0 否 */
    private Integer allowStack = 0;

    @NotBlank(message = "发放渠道不能为空")
    @Pattern(regexp = "CENTER|MANUAL_ONLY", message = "发放渠道不合法")
    private String issueType = "CENTER";

    @NotBlank(message = "有效期类型不能为空")
    @Pattern(regexp = "FIXED|AFTER_DAYS", message = "有效期类型不合法")
    private String validType = "FIXED";

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    /** AFTER_DAYS 领取后有效天数（0=长期） */
    private Integer validDays = 0;

    /** 可发放总量，-1=不限 */
    private Integer totalQuantity = -1;

    /** 每人限领张数（本期固定 1，unique 索引兜底） */
    private Integer perUser = 1;
}
