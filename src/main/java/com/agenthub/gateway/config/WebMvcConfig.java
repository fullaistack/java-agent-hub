package com.agenthub.gateway.config;

import com.agenthub.gateway.interceptor.TenantInterceptor;
import com.agenthub.gateway.interceptor.TraceInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 接入层配置：注册租户认证与链路追踪拦截器。
 *
 * <p>拦截器顺序：Tenant → Trace。Tenant 先解析并校验租户身份，
 * Trace 后执行即可把 tenant.id 作为 Span 属性上链。</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;
    private final TraceInterceptor traceInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**")
                .order(1);
        registry.addInterceptor(traceInterceptor)
                .addPathPatterns("/api/**")
                .order(2);
    }
}
