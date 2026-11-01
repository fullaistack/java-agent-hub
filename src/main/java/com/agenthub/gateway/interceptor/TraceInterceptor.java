package com.agenthub.gateway.interceptor;

import com.agenthub.common.constant.HeaderConstants;
import com.agenthub.tenant.context.TenantContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 链路追踪拦截器：为每个请求生成 traceId 写入 MDC 与响应头，
 * 并把 tenant_id 作为 Span 属性上链，便于在 Jaeger/Tempo 中按租户过滤。
 */
@Component
@RequiredArgsConstructor
public class TraceInterceptor implements HandlerInterceptor {

    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_TENANT_ID = "tenantId";

    private final Tracer tracer;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = extractTraceId();
        MDC.put(MDC_TRACE_ID, traceId);
        response.setHeader(HeaderConstants.TRACE_ID, traceId);
        // 当前 Span 补充业务属性
        Span current = tracer.currentSpan();
        if (current != null) {
            current.tag("http.path", request.getRequestURI());
            Long tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                current.tag("tenant.id", String.valueOf(tenantId));
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_TENANT_ID);
    }

    /** 优先透传上游 traceId，否则生成新的（OTel 桥接会自动处理跨服务传播，这里兜底） */
    private String extractTraceId() {
        Span current = tracer.currentSpan();
        if (current != null && current.context().traceId() != null) {
            return current.context().traceId();
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
