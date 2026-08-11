package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 收货地址 VO（fullAddress 服务端拼接省市区+详细地址）。
 */
@Data
public class AddressVO implements Serializable {

    private Long id;

    private String receiverName;

    private String receiverPhone;

    private String province;

    private String city;

    private String district;

    private String detail;

    private Integer isDefault;

    /** 省+市+区+详细地址（区可为空，自动省略） */
    private String fullAddress;
}