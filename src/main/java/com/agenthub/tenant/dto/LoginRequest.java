package com.agenthub.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求（免认证白名单接口）。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
