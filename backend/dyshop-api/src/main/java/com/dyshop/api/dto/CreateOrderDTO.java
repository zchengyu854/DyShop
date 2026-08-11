package com.dyshop.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateOrderDTO {

    /** 结算来源: cart=购物车勾选结算  buyNow=立即购买 */
    @NotBlank(message = "结算来源不能为空")
    @Pattern(regexp = "cart|buyNow", message = "结算来源不合法")
    private String source;

    @NotNull(message = "请选择收货地址")
    private Long addressId;

    @Size(max = 200, message = "订单备注最多 200 字")
    private String remark;

    /** buyNow 模式下必填 */
    private Long productId;

    /** buyNow 模式下必填，1~99 */
    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 99, message = "单件商品最多 99 件")
    private Integer quantity;

    /** buyNow 模式下规格商品必传；0=无规格商品 */
    private Long skuId = 0L;

    /** 优惠券 ID（可选，ch11；一单一券，下单事务内乐观扣券） */
    private Long couponId;
}
