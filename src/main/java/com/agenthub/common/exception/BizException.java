package com.agenthub.common.exception;

import lombok.Getter;

/**
 * 业务异常：携带 {@link ErrorCode}，由 {@link GlobalExceptionHandler} 统一转换为 API 响应。
 */
@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
