package com.dyshop.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 后台发放 DTO（ch11）。
 */
@Data
public class CouponGrantDTO {

    @NotNull(message = "请选择优惠券模板")
    private Long templateId;

    /** all=全员发放 / manual=指定用户 */
    @NotBlank(message = "发放范围不能为空")
    @Pattern(regexp = "all|manual", message = "发放范围不合法")
    private String target;

    /** manual 模式下必填：目标用户 id 列表 */
    private List<Long> userIds;
}
