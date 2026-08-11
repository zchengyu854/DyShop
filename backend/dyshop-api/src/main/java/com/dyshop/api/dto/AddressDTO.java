package com.dyshop.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 收货地址新增/编辑共用 DTO。
 */
@Data
public class AddressDTO {

    @NotBlank(message = "收货人姓名不能为空")
    @Size(min = 2, max = 50, message = "收货人姓名长度需在 2~50 字符")
    private String receiverName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    @NotBlank(message = "省份不能为空")
    @Size(max = 50, message = "省份长度超出限制")
    private String province;

    @NotBlank(message = "城市不能为空")
    @Size(max = 50, message = "城市长度超出限制")
    private String city;

    @Size(max = 50, message = "区县长度超出限制")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Size(min = 5, max = 200, message = "详细地址长度需在 5~200 字符")
    private String detail;

    /** 是否默认: 0否 1是（默认 0） */
    private Integer isDefault;
}