package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录/注册成功返回：JWT + 用户信息。
 */
@Data
public class LoginVO implements Serializable {

    private String token;

    private UserVO user;
}
