package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收藏列表 VO：收藏 ID + 商品快照 + 收藏时间。
 */
@Data
public class FavoriteVO implements Serializable {

    private Long favoriteId;

    private Long productId;

    private String name;

    private String subtitle;

    private String mainImage;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer sales;

    private LocalDateTime createTime;
}