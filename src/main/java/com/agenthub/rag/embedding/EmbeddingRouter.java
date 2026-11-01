package com.agenthub.rag.embedding;

import com.agenthub.common.exception.BizException;
import com.agenthub.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Embedding 路由：按知识库配置的模型路由键选择 Embedding 客户端。
 */
@Service
public class EmbeddingRouter {

    private final EmbeddingProperties properties;
    private final Map<String, EmbeddingClient> clients;

    public EmbeddingRouter(EmbeddingProperties properties) {
        this.properties = properties;
        this.clients = properties.getProviders().stream()
                .map(this::buildClient)
                .collect(Collectors.toMap(EmbeddingClient::name, Function.identity()));
    }

    /** 按路由键获取客户端；路由键为空时使用默认模型 */
    public EmbeddingClient route(String modelName) {
        String key = (modelName == null || modelName.isBlank()) ? properties.getDefaultModel() : modelName;
        EmbeddingClient client = clients.get(key);
        if (client == null) {
            throw new BizException(ErrorCode.LLM_ERROR, "未配置的 Embedding 模型: " + key);
        }
        return client;
    }

    private EmbeddingClient buildClient(EmbeddingProperties.ProviderConfig config) {
        return switch (config.getType()) {
            case "openai" -> new OpenAiEmbeddingClient(config.getName(), config.getBaseUrl(), config.getApiKey(), config.getModel());
            case "ollama" -> new OllamaEmbeddingClient(config.getName(), config.getBaseUrl(), config.getModel());
            case "mock" -> new MockEmbeddingClient(1024);
            default -> throw new IllegalArgumentException("未知 Embedding 类型: " + config.getType());
        };
    }
}
