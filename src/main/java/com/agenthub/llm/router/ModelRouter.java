package com.agenthub.llm.router;

import com.agenthub.common.exception.BizException;
import com.agenthub.common.exception.ErrorCode;
import com.agenthub.llm.config.LLMProperties;
import com.agenthub.llm.model.ChatRequest;
import com.agenthub.llm.model.ChatResponse;
import com.agenthub.llm.provider.LLMProvider;
import com.agenthub.llm.provider.MockLLMProvider;
import com.agenthub.llm.provider.OllamaProvider;
import com.agenthub.llm.provider.OpenAiCompatibleProvider;
import com.agenthub.llm.service.TokenUsageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 模型路由：按模型路由键选择 Provider 与引擎模型，统一完成 token 统计与耗时埋点。
 *
 * <p>路由表结构：provider → models[] → {路由键 name, 引擎模型 model}。
 * 请求方只感知路由键，底层 provider 可随时切换（例如从 Ollama 迁到 vLLM 无需改业务代码）。</p>
 */
@Slf4j
@Service
public class ModelRouter {

    /** 路由键 → (provider, 模型配置) */
    private final Map<String, RouteEntry> routes = new HashMap<>();
    private final String defaultModel;
    private final TokenUsageService tokenUsageService;

    public ModelRouter(LLMProperties properties, TokenUsageService tokenUsageService) {
        this.defaultModel = properties.getDefaultModel();
        this.tokenUsageService = tokenUsageService;
        for (LLMProperties.ProviderConfig providerConfig : properties.getProviders()) {
            LLMProvider provider = buildProvider(providerConfig);
            for (LLMProperties.ModelConfig model : providerConfig.getModels()) {
                routes.put(model.getName(), new RouteEntry(provider, model));
            }
        }
    }

    /** 对话（模型路由键缺省时使用默认模型） */
    public ChatResponse chat(ChatRequest request) {
        RouteEntry entry = resolve(request.getModel());
        // 路由键替换为引擎模型名
        request.setModel(entry.model().getModel());
        if (request.getTemperature() == null) {
            request.setTemperature(entry.model().getTemperature());
        }
        if (request.getMaxTokens() == null) {
            request.setMaxTokens(entry.model().getMaxTokens());
        }

        long start = System.currentTimeMillis();
        ChatResponse response;
        try {
            response = entry.provider().chat(request);
        } catch (BizException e) {
            // 失败也记录统计（token 为 0，异常标记）
            tokenUsageService.recordFailure(request.getModel(), entry.provider().type(),
                    System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
        long duration = System.currentTimeMillis() - start;
        // 每次调用都记录 token 消耗与耗时（供统计接口与 OTel 指标使用）
        tokenUsageService.record(request.getModel(), entry.provider().type(), response.getUsage(), duration);
        log.debug("LLM调用完成: model={}, duration={}ms, tokens={}", request.getModel(), duration,
                response.getUsage() == null ? 0 : response.getUsage().total());
        return response;
    }

    /** 纯文本生成（无工具） */
    public String generate(String model, String systemPrompt, String userPrompt) {
        ChatRequest request = ChatRequest.builder()
                .model(model)
                .messages(java.util.List.of(
                        com.agenthub.llm.model.ChatMessage.system(systemPrompt),
                        com.agenthub.llm.model.ChatMessage.user(userPrompt)))
                .build();
        return chat(request).getContent();
    }

    private RouteEntry resolve(String modelName) {
        String key = (modelName == null || modelName.isBlank()) ? defaultModel : modelName;
        RouteEntry entry = routes.get(key);
        if (entry == null) {
            throw new BizException(ErrorCode.LLM_ERROR, "未配置的模型路由键: " + key);
        }
        return entry;
    }

    private LLMProvider buildProvider(LLMProperties.ProviderConfig config) {
        return switch (config.getType()) {
            case OpenAiCompatibleProvider.TYPE ->
                    new OpenAiCompatibleProvider(config.getBaseUrl(), config.getApiKey());
            case OllamaProvider.TYPE -> new OllamaProvider(config.getBaseUrl());
            case MockLLMProvider.TYPE -> new MockLLMProvider();
            default -> throw new IllegalArgumentException("未知 LLM Provider 类型: " + config.getType());
        };
    }

    /** 路由表项 */
    private record RouteEntry(LLMProvider provider, LLMProperties.ModelConfig model) {
    }
}
