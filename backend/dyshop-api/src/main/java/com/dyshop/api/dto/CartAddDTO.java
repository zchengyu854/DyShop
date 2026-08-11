package com.dyshop.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartAddDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** 规格SKU ID: 0=无规格商品（规格商品必传，服务端校验存在性与库存） */
    private Long skuId = 0L;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 99, message = "单件商品最多 99 件")
    private Integer quantity;
}
