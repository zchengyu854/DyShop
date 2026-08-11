package com.dyshop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户表 user。
 * role: 0=普通用户(买家)  1=管理员
 * status: 0=正常  1=禁用
 */
@Data
@TableName("user")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 密码(BCrypt) */
    private String password;

    private String nickname;

    private String avatar;

    private String phone;

    private String email;

    private Integer role;

    private Integer status;

    /** 积分余额(ch09) */
    private Integer points;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
