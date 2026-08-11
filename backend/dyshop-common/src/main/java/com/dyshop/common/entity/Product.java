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
 * 商品表 product。
 * status: 0=下架  1=上架
 */
@Data
@TableName("product")
public class Product implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String name;

    private String subtitle;

    private String mainImage;

    /** 轮播图 URL，逗号分隔 */
    private String images;

    /** 商品详情（富文本/HTML） */
    private String detail;

    /** 规格维度定义(JSON数组)，如 [{"name":"型号","values":["Air","Pro"]}]；NULL=无规格商品 */
    private String specs;

    /** SKU 列表(JSON数组)，结构见 docs/ch04/spec-sku-selector.md */
    private String skus;

    private BigDecimal price;

    private BigDecimal originalPrice;

    /** 会员专享价(ch09)；NULL=未设置，会员下单走等级折扣 */
    private BigDecimal vipPrice;

    private Integer stock;

    private Integer sales;

    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
