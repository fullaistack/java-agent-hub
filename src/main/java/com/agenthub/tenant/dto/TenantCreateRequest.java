package com.agenthub.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建租户请求。
 */
@Data
public class TenantCreateRequest {

    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;

    @NotBlank(message = "租户名称不能为空")
    private String tenantName;

    @NotBlank(message = "管理员用户名不能为空")
    private String adminUsername;

    @NotBlank(message = "管理员密码不能为空")
    private String adminPassword;
}
