package com.dyshop.api.util;

import com.dyshop.api.vo.SkuVO;
import com.dyshop.api.vo.SpecVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * product.specs / product.skus JSON 列解析工具。
 * 演示级约定：解析失败一律兜底空列表，不让脏数据击穿业务。
 */
public final class SkuJsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SkuJsonUtils() {
    }

    /** 解析规格维度定义；空/损坏 -> 空列表 */
    public static List<SpecVO> parseSpecs(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<SpecVO>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** 解析 SKU 列表；空/损坏 -> 空列表 */
    public static List<SkuVO> parseSkus(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<SkuVO>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static SkuVO findSku(List<SkuVO> skus, Long skuId) {
        if (skus == null || skuId == null) {
            return null;
        }
        return skus.stream()
                .filter(s -> Objects.equals(s.getId(), skuId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 按规格维度顺序生成展示快照文本，如「型号:MacBook Air, 颜色:深空灰」。
     * 服务端生成，不信任前端直传。
     */
    public static String buildSpecText(SkuVO sku, List<SpecVO> specs) {
        if (sku == null || sku.getSpecs() == null || specs == null || specs.isEmpty()) {
            return null;
        }
        return specs.stream()
                .map(SpecVO::getName)
                .map(name -> name + ":" + (sku.getSpecs().get(name) == null ? "" : sku.getSpecs().get(name)))
                .collect(Collectors.joining(", "));
    }

    /** SKU 列表写回 JSON（下单扣减 / 取消回补 SKU 显示库存用） */
    public static String writeSkus(List<SkuVO> skus) {
        try {
            return MAPPER.writeValueAsString(skus);
        } catch (Exception e) {
            return null;
        }
    }
}
