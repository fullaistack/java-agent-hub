package com.agenthub.tracing.aspect;

import com.agenthub.tracing.entity.CallStat;
import com.agenthub.tracing.mapper.CallStatMapper;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 检索/解析链路 AOP 埋点：对 RAG 检索与文档解析做统一耗时统计，
 * 创建 OTel Span 上链，并异步写 call_stat 表。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TraceAspect {

    private final Tracer tracer;
    private final CallStatMapper callStatMapper;

    /** RAG 检索埋点 */
    @Around("execution(* com.agenthub.rag.service.RagService.search(..))")
    public Object aroundRagSearch(ProceedingJoinPoint pjp) throws Throwable {
        return trace(pjp, "rag", span -> span.name("rag.search"));
    }

    /** 文档解析埋点 */
    @Around("execution(* com.agenthub.parser.ParserService.parse(..))")
    public Object aroundParse(ProceedingJoinPoint pjp) throws Throwable {
        return trace(pjp, "parse", span -> span.name("doc.parse"));
    }

    /** 通用埋点模板：Span + 耗时 + 统计落库 */
    private Object trace(ProceedingJoinPoint pjp, String bizType, java.util.function.Consumer<Span> spanBuilder) throws Throwable {
        long start = System.currentTimeMillis();
        Span span = tracer.nextSpan().start();
        if (span != null) {
            spanBuilder.accept(span);
            Long tenantId = com.agenthub.tenant.context.TenantContext.getTenantId();
            if (tenantId != null) {
                span.tag("tenant.id", String.valueOf(tenantId));
            }
        }
        Throwable error = null;
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (span != null) {
                span.tag("costMs", String.valueOf(cost));
                if (error != null) {
                    span.error(error);
                }
                span.end();
            }
            record(bizType, targetOf(pjp), error, cost);
        }
    }

    /** 统计目标：RAG 检索取查询词摘要（第2个参数），解析取文件名（第1个参数） */
    private String targetOf(ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();
        // RAG: search(List kbIds, String query, ...) → 取 query
        if (args.length > 1 && args[1] instanceof String s) {
            return s.length() > 50 ? s.substring(0, 50) : s;
        }
        if (args.length > 0 && args[0] instanceof String s) {
            return s.length() > 50 ? s.substring(0, 50) : s;
        }
        return pjp.getSignature().getName();
    }

    private void record(String bizType, String target, Throwable error, long costMs) {
        try {
            CallStat stat = new CallStat();
            Long tenantId = com.agenthub.tenant.context.TenantContext.getTenantId();
            if (tenantId == null) {
                // 无租户上下文（如本地事件消费前未设置），忽略统计
                return;
            }
            stat.setTenantId(tenantId);
            stat.setUserId(com.agenthub.tenant.context.TenantContext.getUserId());
            stat.setBizType(bizType);
            stat.setTarget(target);
            stat.setSuccess(error == null);
            stat.setCostMs(costMs);
            stat.setErrMsg(error == null ? null : error.getMessage());
            Span current = tracer.currentSpan();
            if (current != null && current.context() != null && current.context().traceId() != null) {
                stat.setTraceId(current.context().traceId());
            }
            callStatMapper.insert(stat);
        } catch (Exception e) {
            log.warn("埋点统计落库失败: bizType={}, err={}", bizType, e.getMessage());
        }
    }
}
