package com.agenthub.llm.service;

import com.agenthub.llm.entity.LlmTokenUsage;
import com.agenthub.llm.mapper.LlmTokenUsageMapper;
import com.agenthub.llm.model.TokenUsage;
import com.agenthub.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Token 统计服务：模型路由每次调用后落库，并提供聚合查询。
 *
 * <p>落库失败仅告警不阻断主流程（监控数据不允许影响业务可用性）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenUsageService {

    private final LlmTokenUsageMapper usageMapper;

    /** 记录成功调用 */
    public void record(String model, String provider, TokenUsage usage, long durationMs) {
        try {
            LlmTokenUsage record = new LlmTokenUsage();
            record.setTenantId(TenantContext.getTenantId());
            record.setModel(model);
            record.setProvider(provider);
            record.setPromptTokens(usage == null ? 0 : usage.getPromptTokens());
            record.setCompletionTokens(usage == null ? 0 : usage.getCompletionTokens());
            record.setDurationMs(durationMs);
            record.setStatus("SUCCESS");
            usageMapper.insert(record);
        } catch (Exception e) {
            log.warn("token 统计落库失败: {}", e.getMessage());
        }
    }

    /** 记录失败调用 */
    public void recordFailure(String model, String provider, long durationMs, String errorMsg) {
        try {
            LlmTokenUsage record = new LlmTokenUsage();
            record.setTenantId(TenantContext.getTenantId());
            record.setModel(model);
            record.setProvider(provider);
            record.setPromptTokens(0);
            record.setCompletionTokens(0);
            record.setDurationMs(durationMs);
            record.setStatus("ERROR");
            record.setErrorMsg(errorMsg == null ? null : errorMsg.substring(0, Math.min(errorMsg.length(), 480)));
            usageMapper.insert(record);
        } catch (Exception e) {
            log.warn("token 失败记录落库失败: {}", e.getMessage());
        }
    }

    /** 近 N 天用量聚合（按模型） */
    public java.util.List<Map<String, Object>> aggregateByModel(int days) {
        return usageMapper.selectMaps(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LlmTokenUsage>()
                .select("model", "SUM(prompt_tokens) AS prompt_tokens",
                        "SUM(completion_tokens) AS completion_tokens",
                        "COUNT(*) AS calls", "AVG(duration_ms) AS avg_duration_ms")
                .ge("created_at", java.time.LocalDateTime.now().minusDays(days))
                .groupBy("model"));
    }
}
