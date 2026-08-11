package com.dyshop.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartCheckedDTO {

    @NotNull(message = "勾选状态不能为空")
    private Integer checked;
}
