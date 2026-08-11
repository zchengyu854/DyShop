package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 规格维度 VO（product.specs JSON 元素）。
 * 例：{ "name": "型号", "values": ["MacBook Air", "MacBook Pro"] }
 */
@Data
public class SpecVO implements Serializable {

    /** 规格名（维度），如「型号」「显存」「颜色」 */
    private String name;

    /** 可选值列表（展示顺序即后端顺序） */
    private List<String> values;
}
