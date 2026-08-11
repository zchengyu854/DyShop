package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户信息 VO（不暴露密码）。
 */
@Data
public class UserVO implements Serializable {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String phone;

    private String email;

    /** 0=买家 1=管理员 */
    private Integer role;
}
