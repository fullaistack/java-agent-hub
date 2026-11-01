package com.agenthub.llm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 模型调用请求（平台内部统一结构，屏蔽 OpenAI/Ollama 差异）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /** 模型路由键（providers[].models[].name） */
    private String model;

    private List<ChatMessage> messages;

    /** 采样温度 */
    private Double temperature;

    /** 最大生成 token 数 */
    private Integer maxTokens;

    /** 工具定义（OpenAI function calling 格式），null 表示纯对话 */
    private List<Map<String, Object>> tools;
}
