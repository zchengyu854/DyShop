package com.dyshop.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 积分商城商品 DTO（ch13，后台新建/编辑）。
 */
@Data
public class PointsGoodsDTO {

    @NotBlank(message = "商品名不能为空")
    private String name;

    private String coverImage;

    private String description;

    /** COUPON=发券 / CODE=兑换码 */
    @NotBlank(message = "商品类型不能为空")
    @Pattern(regexp = "COUPON|CODE", message = "商品类型不合法")
    private String goodsType;

    @NotNull(message = "积分价不能为空")
    @Min(value = 1, message = "积分价必须大于 0")
    private Integer pointCost;

    /** 库存 -1=不限 */
    private Integer stock;

    /** 每人限兑次数 0=不限 */
    private Integer limitPerUser;

    /** COUPON 类必填：关联优惠券模板 */
    private Long couponTemplateId;

    /** 1上架 0下架 */
    private Integer status;

    private Integer sort;
}