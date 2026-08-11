package com.dyshop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板表 coupon_template（ch11）。
 * type: REDUCE=立减型(满减/无门槛)   scope: ALL=全场 / LIMITED=有限定
 * issue_type: CENTER=可领取 / MANUAL_ONLY=仅后台发放
 * valid_type: FIXED=固定起止 / AFTER_DAYS=领取后N天
 */
@Data
@TableName("coupon_template")
public class CouponTemplate implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板名（展示） */
    private String name;

    /** 券类型：REDUCE=立减型 */
    private String type;

    /** 满减门槛，0=无门槛立减 */
    private BigDecimal minAmount;

    /** 立减金额（>0） */
    private BigDecimal discountAmount;

    /** 适用范围：ALL / LIMITED */
    private String scope;

    /** 指定分类 id 数组（JSON 数组文本；LIMITED 时与 productIds 并集生效） */
    private String categoryIds;

    /** 指定商品 id 数组（JSON 数组文本） */
    private String productIds;

    /** 是否可与会员折扣叠加：1 是 / 0 否 */
    private Integer allowStack;

    /** 发放渠道：CENTER / MANUAL_ONLY */
    private String issueType;

    /** 有效期类型：FIXED / AFTER_DAYS */
    private String validType;

    /** FIXED 生效开始（可空=长期） */
    private LocalDateTime startAt;

    /** FIXED 生效结束（可空=长期） */
    private LocalDateTime endAt;

    /** AFTER_DAYS 领取后有效天数（0=长期） */
    private Integer validDays;

    /** 可发放总量，-1=不限 */
    private Integer totalQuantity;

    /** 每人限领张数 */
    private Integer perUser;

    /** 已发放量（领取+发放） */
    private Integer issuedCount;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createAt;

    private LocalDateTime updateAt;
}
