package com.agenthub.gateway.interceptor;

import com.agenthub.common.api.ApiResponse;
import com.agenthub.common.constant.HeaderConstants;
import com.agenthub.common.exception.ErrorCode;
import com.agenthub.common.util.JsonUtils;
import com.agenthub.gateway.config.SecurityProperties;
import com.agenthub.tenant.context.TenantContext;
import com.agenthub.tenant.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户认证拦截器：解析 X-Tenant-Id / X-Api-Key 并校验 API Key，
 * 通过后把租户身份写入 {@link TenantContext}，为下游 MyBatis 租户插件提供隔离依据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final SecurityProperties securityProperties;
    private final TenantService tenantService;
    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 认证关闭或命中白名单：直接放行
        if (!securityProperties.isEnabled() || isSkipPath(request.getRequestURI())) {
            return true;
        }

        Long tenantId = parseTenantId(request.getHeader(HeaderConstants.TENANT_ID));
        String apiKey = request.getHeader(HeaderConstants.API_KEY);
        if (tenantId == null) {
            return reject(response, ErrorCode.UNAUTHORIZED, "缺少请求头 X-Tenant-Id");
        }
        Long authedTenant = tenantService.verifyApiKey(apiKey);
        if (authedTenant == null || !authedTenant.equals(tenantId)) {
            return reject(response, ErrorCode.UNAUTHORIZED, "API Key 无效或与租户不匹配");
        }

        TenantContext.set(tenantId, null, apiKey.substring(0, Math.min(12, apiKey.length())));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 防止线程池复用导致租户信息残留
        TenantContext.clear();
    }

    private boolean isSkipPath(String uri) {
        return securityProperties.getSkipPaths().stream().anyMatch(p -> matcher.match(p, uri));
    }

    private Long parseTenantId(String value) {
        try {
            return value == null ? null : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean reject(HttpServletResponse response, ErrorCode code, String message) throws Exception {
        response.setStatus(code.getCode());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JsonUtils.toJson(ApiResponse.fail(code.getCode(), message)));
        return false;
    }
}
