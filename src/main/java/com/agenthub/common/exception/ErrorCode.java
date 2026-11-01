package com.agenthub.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 平台统一错误码。
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    UNAUTHORIZED(401, "未认证或API Key无效"),
    FORBIDDEN(403, "无权限访问该资源"),
    NOT_FOUND(404, "资源不存在"),
    PARAM_ERROR(400, "参数错误"),
    BUSINESS_ERROR(500, "业务处理失败"),
    TENANT_ERROR(500, "租户信息缺失，请检查请求头 X-Tenant-Id"),
    LLM_ERROR(502, "模型调用失败"),
    MCP_ERROR(503, "MCP服务调用失败"),
    TASK_ERROR(500, "异步任务处理失败"),
    FILE_ERROR(500, "文件处理失败");

    private final int code;
    private final String message;
}
