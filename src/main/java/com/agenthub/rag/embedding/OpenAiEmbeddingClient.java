package com.agenthub.rag.embedding;

import com.agenthub.common.exception.BizException;
import com.agenthub.common.exception.ErrorCode;
import com.agenthub.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 Embedding 客户端：POST {baseUrl}/v1/embeddings。
 * 兼容 OpenAI / vLLM / OneAPI 等任何兼容接口。
 */
@Slf4j
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private final String name;
    private final String model;
    private final WebClient webClient;

    public OpenAiEmbeddingClient(String name, String baseUrl, String apiKey, String model) {
        this.name = name;
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        Map<String, Object> body = Map.of("model", model, "input", texts);
        try {
            String resp = webClient.post()
                    .uri("/v1/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(JsonUtils.toJson(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return parse(resp);
        } catch (Exception e) {
            throw new BizException(ErrorCode.LLM_ERROR, "Embedding 调用失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<float[]> parse(String resp) {
        // {"data": [{"embedding": [0.1, 0.2, ...]}, ...]}
        Map<String, Object> root = JsonUtils.parseMap(resp);
        Object data = root.get("data");
        if (!(data instanceof List<?> list)) {
            throw new BizException(ErrorCode.LLM_ERROR, "Embedding 响应格式异常: 缺少 data");
        }
        List<float[]> result = new ArrayList<>();
        for (Object item : list) {
            Object emb = ((Map<String, Object>) item).get("embedding");
            if (!(emb instanceof List<?> values)) {
                throw new BizException(ErrorCode.LLM_ERROR, "Embedding 响应格式异常: 缺少 embedding");
            }
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = ((Number) values.get(i)).floatValue();
            }
            result.add(vector);
        }
        return result;
    }
}
