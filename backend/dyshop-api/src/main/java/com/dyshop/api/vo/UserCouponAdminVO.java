package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后台用户券管理 VO（ch11）。
 */
@Data
public class UserCouponAdminVO implements Serializable {

    private Long id;

    private Long userId;

    private String username;

    private String phone;

    private Long templateId;

    private String templateName;

    /** 0 未使用 / 1 已使用 / 2 已过期 */
    private Integer status;

    /** CENTER / MANUAL */
    private String source;

    private Long usedOrderId;

    private LocalDateTime receivedAt;

    private LocalDateTime expireAt;

    private LocalDateTime usedAt;
}
