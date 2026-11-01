package com.agenthub.llm.provider;

import com.agenthub.common.exception.BizException;
import com.agenthub.common.exception.ErrorCode;
import com.agenthub.common.util.JsonUtils;
import com.agenthub.llm.model.ChatMessage;
import com.agenthub.llm.model.ChatRequest;
import com.agenthub.llm.model.ChatResponse;
import com.agenthub.llm.model.TokenUsage;
import com.agenthub.llm.model.ToolCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ollama Provider：POST {baseUrl}/api/chat。
 * Ollama 原生支持 tools 参数（function calling）。
 */
@Slf4j
public class OllamaProvider implements LLMProvider {

    public static final String TYPE = "ollama";

    private final WebClient webClient;

    public OllamaProvider(String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());
        body.put("messages", buildMessages(request.getMessages()));
        body.put("stream", false);
        if (request.getTemperature() != null) {
            body.put("options", Map.of("temperature", request.getTemperature()));
        }
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", request.getTools());
        }

        String resp;
        try {
            resp = webClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(JsonUtils.toJson(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            throw new BizException(ErrorCode.LLM_ERROR, "Ollama 调用失败: " + e.getMessage());
        }
        return parse(request.getModel(), resp);
    }

    private List<Map<String, Object>> buildMessages(List<ChatMessage> messages) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage msg : messages) {
            Map<String, Object> m = new HashMap<>();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent() == null ? "" : msg.getContent());
            if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                m.put("tool_calls", msg.getToolCalls().stream().map(tc -> Map.<String, Object>of(
                        "function", Map.of("name", tc.getName(), "arguments", tc.getArguments()))).toList());
            }
            result.add(m);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private ChatResponse parse(String model, String resp) {
        Map<String, Object> root = JsonUtils.parseMap(resp);
        Map<String, Object> message = (Map<String, Object>) root.get("message");
        if (message == null) {
            throw new BizException(ErrorCode.LLM_ERROR, "Ollama 响应缺少 message 字段");
        }

        ChatResponse response = new ChatResponse();
        response.setContent(message.get("content") == null ? "" : String.valueOf(message.get("content")));
        // Ollama 的工具调用在 message.tool_calls: [{function: {name, arguments}}]
        Object toolCalls = message.get("tool_calls");
        if (toolCalls instanceof List<?> calls && !calls.isEmpty()) {
            List<ToolCall> parsed = new ArrayList<>();
            for (Object call : calls) {
                Map<String, Object> fn = (Map<String, Object>) ((Map<String, Object>) call).get("function");
                parsed.add(new ToolCall("ollama-" + parsed.size(), String.valueOf(fn.get("name")),
                        JsonUtils.toJson(fn.get("arguments"))));
            }
            response.setToolCalls(parsed);
        }
        // Ollama token 统计：prompt_eval_count / eval_count
        TokenUsage usage = new TokenUsage();
        usage.setPromptTokens(toInt(root.get("prompt_eval_count")));
        usage.setCompletionTokens(toInt(root.get("eval_count")));
        response.setUsage(usage);
        response.setEngineModel(model);
        return response;
    }

    private int toInt(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }
}
