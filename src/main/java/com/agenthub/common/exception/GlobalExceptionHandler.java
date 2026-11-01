package com.agenthub.common.exception;

import com.agenthub.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理：业务异常转统一响应，未知异常记录日志并返回通用错误。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBiz(BizException e, HttpServletRequest request) {
        log.warn("[BIZ] {} -> {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return ApiResponse.fail(e.getErrorCode().getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResponse<Void> handleValidation(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .orElse(ErrorCode.PARAM_ERROR.getMessage());
        return ApiResponse.fail(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleUploadSize(MaxUploadSizeExceededException e) {
        return ApiResponse.fail(ErrorCode.FILE_ERROR.getCode(), "上传文件超出大小限制");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnknown(Exception e, HttpServletRequest request) {
        log.error("[ERROR] {} -> {}", request.getMethod(), request.getRequestURI(), e);
        return ApiResponse.fail(ErrorCode.BUSINESS_ERROR);
    }
}
