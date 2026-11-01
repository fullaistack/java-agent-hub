package com.agenthub.tenant.controller;

import com.agenthub.common.api.ApiResponse;
import com.agenthub.tenant.context.TenantContext;
import com.agenthub.tenant.dto.ApiKeyResponse;
import com.agenthub.tenant.dto.LoginRequest;
import com.agenthub.tenant.dto.TenantCreateRequest;
import com.agenthub.tenant.entity.Tenant;
import com.agenthub.tenant.entity.TenantUser;
import com.agenthub.tenant.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 租户管理接口。
 */
@RestController
@RequestMapping("/api/v1/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    /** 创建租户（自助注册，生产环境应加邀请码/审批） */
    @PostMapping("/register")
    public ApiResponse<Tenant> register(@Valid @RequestBody TenantCreateRequest request) {
        return ApiResponse.ok(tenantService.createTenant(request));
    }

    /** 账号密码登录 */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        TenantUser user = tenantService.login(request);
        return ApiResponse.ok(Map.of(
                "tenantId", user.getTenantId(),
                "userId", user.getId(),
                "username", user.getUsername(),
                "displayName", user.getDisplayName(),
                "role", user.getRole()));
    }

    /** 为当前租户签发 API Key（明文仅返回一次） */
    @PostMapping("/api-keys")
    public ApiResponse<ApiKeyResponse> createApiKey(@RequestBody Map<String, String> body) {
        String raw = tenantService.createApiKey(TenantContext.requireTenantId(), body.get("name"));
        return ApiResponse.ok(new ApiKeyResponse(body.get("name"), raw, raw.substring(0, 12)));
    }
}
