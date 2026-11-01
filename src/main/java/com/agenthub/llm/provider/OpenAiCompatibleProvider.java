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
 * OpenAI 兼容 Provider：POST {baseUrl}/v1/chat/completions。
 * 兼容 OpenAI / DeepSeek / Kimi / vLLM / OneAPI 等所有 OpenAI 格式接口。
 */
@Slf4j
public class OpenAiCompatibleProvider implements LLMProvider {

    public static final String TYPE = "openai";

    private final WebClient webClient;

    public OpenAiCompatibleProvider(String baseUrl, String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
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
        body.put("temperature", request.getTemperature() == null ? 0.3 : request.getTemperature());
        body.put("max_tokens", request.getMaxTokens() == null ? 2048 : request.getMaxTokens());
        body.put("stream", false);
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", request.getTools());
        }

        String resp;
        try {
            resp = webClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(JsonUtils.toJson(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            throw new BizException(ErrorCode.LLM_ERROR, "OpenAI 兼容接口调用失败: " + e.getMessage());
        }
        return parse(request.getModel(), resp);
    }

    private List<Map<String, Object>> buildMessages(List<ChatMessage> messages) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage msg : messages) {
            Map<String, Object> m = new HashMap<>();
            m.put("role", msg.getRole());
            if ("tool".equals(msg.getRole())) {
                m.put("tool_call_id", msg.getToolCallId());
                m.put("content", msg.getContent());
            } else {
                m.put("content", msg.getContent() == null ? "" : msg.getContent());
                // assistant 消息携带 tool_calls（function calling 回填）
                if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                    m.put("tool_calls", msg.getToolCalls().stream().map(tc -> Map.<String, Object>of(
                            "id", tc.getId(),
                            "type", "function",
                            "function", Map.of("name", tc.getName(), "arguments", tc.getArguments()))).toList());
                }
            }
            result.add(m);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private ChatResponse parse(String model, String resp) {
        Map<String, Object> root = JsonUtils.parseMap(resp);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) root.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new BizException(ErrorCode.LLM_ERROR, "OpenAI 响应缺少 choices");
        }
        Map<String, Object> choice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) choice.get("message");

        ChatResponse response = new ChatResponse();
        response.setContent(message.get("content") == null ? "" : String.valueOf(message.get("content")));
        // function calling 结果
        Object toolCalls = message.get("tool_calls");
        if (toolCalls instanceof List<?> calls && !calls.isEmpty()) {
            List<ToolCall> parsed = new ArrayList<>();
            for (Object call : calls) {
                Map<String, Object> callMap = (Map<String, Object>) call;
                Map<String, Object> fn = (Map<String, Object>) callMap.get("function");
                parsed.add(new ToolCall(
                        String.valueOf(callMap.get("id")),
                        String.valueOf(fn.get("name")),
                        String.valueOf(fn.get("arguments"))));
            }
            response.setToolCalls(parsed);
        }
        // token 统计
        Map<String, Object> usage = (Map<String, Object>) root.get("usage");
        TokenUsage tokenUsage = new TokenUsage();
        if (usage != null) {
            tokenUsage.setPromptTokens(((Number) usage.get("prompt_tokens")).intValue());
            tokenUsage.setCompletionTokens(((Number) usage.get("completion_tokens")).intValue());
        }
        response.setUsage(tokenUsage);
        response.setEngineModel(model);
        return response;
    }
}
