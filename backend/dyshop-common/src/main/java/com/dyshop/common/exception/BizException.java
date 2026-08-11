package com.dyshop.common.exception;

import com.dyshop.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：携带错误码，由全局异常处理器统一转换为 Result。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
