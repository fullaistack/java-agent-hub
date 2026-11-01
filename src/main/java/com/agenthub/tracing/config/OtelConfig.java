package com.agenthub.tracing.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry 配置。
 *
 * <p>链路与指标上报由 Spring Boot Actuator 的 Micrometer Tracing Bridge
 * 自动完成（见 application.yml 的 management.otlp 配置），无需手工初始化 SDK：
 * - Traces：micrometer-tracing-bridge-otel → OTLP → Collector → Jaeger/Tempo
 * - Metrics：micrometer-registry-otlp 自动导出
 *
 * <p>这里额外暴露 OTel 原生 Tracer/Meter，供业务代码在需要细粒度
 * Span/指标时直接使用 OTel API（与 Micrometer 共享同一 SDK 实例）。</p>
 */
@Configuration
public class OtelConfig {

    /** OTel 原生 OpenTelemetry 实例（由 Micrometer bridge 初始化的全局实例） */
    @Bean
    public OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get();
    }

    /** OTel 原生 Tracer */
    @Bean
    public Tracer otelTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("com.agenthub", "1.0.0");
    }

    /** OTel 原生 Meter */
    @Bean
    public Meter otelMeter(OpenTelemetry openTelemetry) {
        return openTelemetry.getMeter("com.agenthub");
    }
}
