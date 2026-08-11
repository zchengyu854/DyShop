package com.dyshop.api.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分流水条目（ch09，GET /api/user/member/points）。
 */
@Data
public class PointLogVO {

    private Integer points;

    private Integer balance;

    private String remark;

    private LocalDateTime createTime;
}