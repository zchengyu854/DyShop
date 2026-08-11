package com.dyshop.api.vo;

import lombok.Data;

import java.util.List;

/**
 * 积分商城首页 VO（ch13，GET /api/user/points/goods）。
 */
@Data
public class PointsMallVO {

    /** 我的可用积分余额 */
    private Integer myPoints;

    /** 在售商品（排序升序） */
    private List<PointsGoodsVO> goods;
}