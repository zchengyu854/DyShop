package com.dyshop.api.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 发放结果 VO（ch11）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrantResultVO implements Serializable {

    /** 本次成功发放张数 */
    private long granted;

    /** 因已存在而跳过的张数 */
    private long skipped;
}
