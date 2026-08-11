package com.dyshop.api.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结算价格预览 VO（ch09，POST /api/user/member/price-preview）。
 * 与下单共用 resolvePrice 规则，保证展示价 = 实际成交价。
 */
@Data
public class MemberPricePreviewVO {

    /** 当前等级（普通=无优惠，前端据此隐藏折扣行） */
    private MemberLevelVO level;

    /** 每行价格明细：原价 / 会员成交价 */
    private List<PriceRow> rows;

    @Data
    public static class PriceRow {
        private Long productId;
        private Long skuId;
        /** 原价（商品价或 SKU 价） */
        private BigDecimal originalPrice;
        /** 会员成交价（专享价优先，否则原价 × 折扣率） */
        private BigDecimal memberPrice;
    }
}