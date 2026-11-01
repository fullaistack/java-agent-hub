package com.agenthub.llm.provider;

import com.agenthub.common.util.JsonUtils;
import com.agenthub.llm.model.ChatRequest;
import com.agenthub.llm.model.ChatResponse;
import com.agenthub.llm.model.TokenUsage;
import com.agenthub.llm.model.ToolCall;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mock LLM：无外部模型时返回确定性回答，用于演示与单测。
 *
 * <p>行为：
 * 1. 若请求携带工具定义，模拟一次工具调用（返回第一个工具名）；
 * 2. 纯对话则回显最后一条用户消息摘要。</p>
 */
public class MockLLMProvider implements LLMProvider {

    public static final String TYPE = "mock";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        ChatResponse response = new ChatResponse();
        response.setEngineModel(request.getModel());
        response.setUsage(new TokenUsage(10, 10));

        if (request.getTools() != null && !request.getTools().isEmpty()) {
            // 模拟工具调用：取第一个工具，参数置空对象
            Map<String, Object> toolDef = request.getTools().get(0);
            Map<String, Object> fn = (Map<String, Object>) toolDef.get("function");
            ToolCall call = new ToolCall("mock-call-1", String.valueOf(fn.get("name")), "{}");
            response.setToolCalls(new ArrayList<>(List.of(call)));
            response.setContent("");
        } else {
            String lastUser = request.getMessages().stream()
                    .filter(m -> "user".equals(m.getRole()))
                    .reduce((a, b) -> b)
                    .map(m -> m.getContent())
                    .orElse("");
            response.setContent("【Mock回答】已收到您的问题：" + lastUser + "。"
                    + "当前为演示模式（agenthub.llm.default-model=mock-chat），"
                    + "配置真实模型（Ollama/OpenAI 兼容接口）后即可获得真实回答。");
        }
        return response;
    }
}
