package com.agenthub.tracing.hook;

import com.agenthub.agent.harness.AgentHook;
import com.agenthub.agent.harness.ChatContext;
import com.agenthub.llm.model.ChatRequest;
import com.agenthub.llm.model.ChatResponse;
import com.agenthub.llm.model.ToolCall;
import com.agenthub.tracing.entity.CallStat;
import com.agenthub.tracing.mapper.CallStatMapper;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Agent 埋点 Hook：在编排引擎的关键节点记录 LLM 调用与工具调用的
 * 耗时、token 消耗、异常信息，异步写入 call_stat 表。
 *
 * <p>同时把关键事件上报 OTel Span（供 Jaeger/Tempo 按租户、按工具检索）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TraceHook implements AgentHook {

    private final CallStatMapper callStatMapper;
    private final Tracer tracer;

    /** LLM 调用开始时间（ThreadLocal：Agent 循环同步执行，保证并发安全） */
    private final ThreadLocal<Long> llmStart = new ThreadLocal<>();
    /** 工具调用开始时间 */
    private final ThreadLocal<Long> toolStart = new ThreadLocal<>();

    @Override
    public ChatRequest beforeLLM(ChatRequest request, ChatContext context) {
        llmStart.set(System.currentTimeMillis());
        Span span = tracer.nextSpan().name("agent.llm").start();
        if (span != null) {
            span.tag("model", request.getModel());
            span.tag("tenant.id", String.valueOf(context.getTenantId()));
        }
        return request;
    }

    @Override
    public void afterLLM(ChatRequest request, ChatResponse response, ChatContext context) {
        Long start = llmStart.get();
        llmStart.remove();
        long cost = start == null ? 0 : System.currentTimeMillis() - start;
        Span span = tracer.currentSpan();
        if (span != null) {
            span.end();
        }
        record(context, "llm", request.getModel(), true, cost,
                response.getUsage(), null);
    }

    @Override
    public Map<String, Object> beforeTool(ToolCall toolCall, Map<String, Object> arguments, ChatContext context) {
        toolStart.set(System.currentTimeMillis());
        return arguments;
    }

    @Override
    public void afterTool(ToolCall toolCall, String result, ChatContext context) {
        Long start = toolStart.get();
        toolStart.remove();
        long cost = start == null ? 0 : System.currentTimeMillis() - start;
        record(context, "tool", toolCall.getName(), true, cost, null, null);
    }

    @Override
    public void onFinish(ChatContext context, String finalAnswer) {
        // 整轮 Agent 对话汇总：耗时与工具调用次数
        Span span = tracer.currentSpan();
        if (span != null) {
            span.tag("agent.toolCalls", String.valueOf(context.getToolCallCount()));
        }
    }

    /** 异步落库（同步 insert 足够轻量，避免 @Async 自调用失效问题） */
    public void record(ChatContext context, String bizType, String target,
                       boolean success, long costMs,
                       com.agenthub.llm.model.TokenUsage usage, String errMsg) {
        CallStat stat = new CallStat();
        stat.setTenantId(context.getTenantId());
        stat.setUserId(context.getUserId());
        stat.setBizType(bizType);
        stat.setTarget(target);
        stat.setSuccess(success);
        stat.setCostMs(costMs);
        if (usage != null) {
            stat.setPromptTokens(usage.getPromptTokens());
            stat.setCompletionTokens(usage.getCompletionTokens());
        }
        stat.setErrMsg(errMsg);
        Span current = tracer.currentSpan();
        if (current != null && current.context() != null && current.context().traceId() != null) {
            stat.setTraceId(current.context().traceId());
        }
        try {
            callStatMapper.insert(stat);
        } catch (Exception e) {
            log.warn("调用统计落库失败: bizType={}, err={}", bizType, e.getMessage());
        }
    }
}
