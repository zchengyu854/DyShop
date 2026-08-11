package com.dyshop.common.result;

import lombok.Getter;

/**
 * 统一错误码。
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未认证或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    ERROR(500, "服务器内部错误"),

    // ----- 优惠券（ch11，409 系） -----
    COUPON_USED(409, "优惠券已被使用"),
    COUPON_EXPIRED(409, "优惠券已过期"),
    COUPON_NOT_REACH(409, "未满足使用门槛"),
    COUPON_SOLD_OUT(409, "优惠券已被抢光"),
    COUPON_ALREADY_CLAIMED(409, "已领取过该优惠券"),
    COUPON_ALREADY_GRANTED(409, "该用户已发放过此券"),
    COUPON_STACK_FORBIDDEN(409, "该券不可与会员折扣同用"),
    COUPON_INVALID(409, "优惠券不可用"),

    // ----- 售后（ch12，409 系） -----
    AFTER_SALE_DUPLICATED(409, "该商品已申请售后"),

    // ----- 积分商城（ch13，409 系） -----
    POINTS_GOODS_NOT_FOUND(404, "商品不存在或已下架"),
    POINTS_GOODS_SOLD_OUT(409, "商品已兑完"),
    POINTS_NOT_ENOUGH(409, "积分余额不足"),
    POINTS_EXCHANGE_FROZEN(409, "该商品已兑换过"),
    POINTS_EXCHANGE_FAIL(500, "兑换失败，请重试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
