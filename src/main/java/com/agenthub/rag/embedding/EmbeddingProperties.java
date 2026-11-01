package com.agenthub.rag.embedding;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Embedding 配置：多 provider 列表 + 默认模型路由键。
 */
@Data
@ConfigurationProperties(prefix = "agenthub.embedding")
public class EmbeddingProperties {

    /** 默认 provider 路由键 */
    private String defaultModel = "mock-embed";

    private List<ProviderConfig> providers = new ArrayList<>();

    @Data
    public static class ProviderConfig {
        /** 路由键（知识库 embeddingModel 引用该值） */
        private String name;
        /** openai | ollama | mock */
        private String type;
        private String baseUrl;
        private String apiKey;
        /** 引擎模型名（如 bge-m3 / text-embedding-3-small） */
        private String model;
    }
}
