package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * SKU 单元 VO（product.skus JSON 元素）。
 * 例：{ "id": 1101, "specs": { "型号": "MacBook Air", "颜色": "深空灰" },
 *      "price": 7999.00, "originalPrice": 8999.00, "stock": 12, "image": null }
 */
@Data
public class SkuVO implements Serializable {

    /** SKU ID（product 内唯一，购物车/订单以此引用） */
    private Long id;

    /** 规格名 -> 规格值 */
    private Map<String, String> specs;

    private BigDecimal price;

    private BigDecimal originalPrice;

    /** 库存（显示口径；下单并发安全扣减以 product.stock 合计为准） */
    private Integer stock;

    /** 规格图 URL（覆盖商品主图，如颜色维度）；无则为 null */
    private String image;
}
