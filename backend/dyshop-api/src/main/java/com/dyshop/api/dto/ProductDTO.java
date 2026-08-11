package com.dyshop.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 后台商品新增/编辑 DTO。
 * <p>
 * 规格约定（与 docs/ch08/spec.md D4 一致）：
 * - specs/skus 均非空 → 有规格商品：总库存自动 = Σ sku.stock，表单 stock 忽略
 * - 均为空 → 无规格商品：直接使用 stock 字段
 * - specs/skus 必须同时存在（一个有值另一个为空 → 400）
 */
@Data
public class ProductDTO {

    @NotNull(message = "请选择分类")
    private Long categoryId;

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称最多 100 字")
    private String name;

    @Size(max = 200, message = "副标题最多 200 字")
    private String subtitle;

    @NotBlank(message = "主图不能为空")
    @Size(max = 500, message = "主图地址过长")
    private String mainImage;

    @Size(max = 2000, message = "轮播图地址过长")
    private String images;

    private String detail;

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.01", message = "售价必须大于 0")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "原价必须大于 0")
    private BigDecimal originalPrice;

    /** 会员专享价(ch09)，可空 */
    private BigDecimal vipPrice;

    private Integer stock;

    @NotNull(message = "请选择上架状态")
    private Integer status;

    /** 规格维度定义 JSON；与 skus 同时为空 = 无规格商品 */
    private String specs;

    /** SKU 列表 JSON；与 specs 同时为空 = 无规格商品 */
    private String skus;
}