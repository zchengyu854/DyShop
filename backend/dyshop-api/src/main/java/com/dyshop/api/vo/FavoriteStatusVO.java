package com.dyshop.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 收藏状态 VO。
 */
@Data
public class FavoriteStatusVO implements Serializable {

    private boolean favorited;
}
