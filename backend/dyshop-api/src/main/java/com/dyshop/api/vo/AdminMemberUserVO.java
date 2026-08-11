package com.dyshop.api.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 后台会员列表条目（ch09）。
 */
@Data
public class AdminMemberUserVO {

    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private Integer role;

    private Integer status;

    /** 当前等级 */
    private MemberLevelVO level;

    /** 近12个月消费 */
    private BigDecimal annualConsumption;

    /** 累计消费 */
    private BigDecimal totalConsumption;

    private Integer points;

    private LocalDateTime createTime;
}