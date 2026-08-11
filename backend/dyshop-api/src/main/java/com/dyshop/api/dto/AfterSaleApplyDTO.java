package com.dyshop.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 售后申请 DTO（ch12）。
 */
@Data
public class AfterSaleApplyDTO {

    /** 订单商品行 ID（订单列表/详情返回的 OrderItemVO.id） */
    @NotNull(message = "请选择要申请售后的商品")
    private Long orderItemId;

    @Size(max = 200, message = "申请原因最多 200 字")
    private String reason;
}
