package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后台用户管理 VO（不暴露密码）。
 */
@Data
public class AdminUserVO implements Serializable {

    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private String email;

    /** 0=买家 1=管理员 */
    private Integer role;

    /** 0=正常 1=禁用 */
    private Integer status;

    private LocalDateTime createTime;
}