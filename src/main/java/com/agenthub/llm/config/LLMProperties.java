package com.agenthub.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM 配置：多 provider 与模型注册表。
 */
@Data
@ConfigurationProperties(prefix = "agenthub.llm")
public class LLMProperties {

    /** 默认模型路由键 */
    private String defaultModel = "mock-chat";

    private List<ProviderConfig> providers = new ArrayList<>();

    @Data
    public static class ProviderConfig {
        /** provider 实例名 */
        private String name;
        /** openai | ollama | mock */
        private String type;
        private String baseUrl;
        private String apiKey;
        /** 该 provider 下可路由的模型列表 */
        private List<ModelConfig> models = new ArrayList<>();
    }

    @Data
    public static class ModelConfig {
        /** 路由键（业务侧引用） */
        private String name;
        /** 引擎模型名 */
        private String model;
        private Double temperature = 0.3;
        private Integer maxTokens = 2048;
    }
}
