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
 * Ollama Embedding 客户端：POST {baseUrl}/api/embed。
 */
@Slf4j
public class OllamaEmbeddingClient implements EmbeddingClient {

    private final String name;
    private final String model;
    private final WebClient webClient;

    public OllamaEmbeddingClient(String name, String baseUrl, String model) {
        this.name = name;
        this.model = model;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> result = new ArrayList<>();
        for (String text : texts) {
            result.add(embedSingle(text));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private float[] embedSingle(String text) {
        Map<String, Object> body = Map.of("model", model, "input", text);
        try {
            String resp = webClient.post()
                    .uri("/api/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(JsonUtils.toJson(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            // 响应结构：{"embeddings": [[0.1, 0.2, ...]]}
            Map<String, Object> root = JsonUtils.parseMap(resp);
            Object embeddings = root.get("embeddings");
            if (!(embeddings instanceof List<?> outer) || outer.isEmpty()
                    || !(outer.get(0) instanceof List<?> values)) {
                throw new BizException(ErrorCode.LLM_ERROR, "Ollama embedding 响应格式异常");
            }
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = ((Number) values.get(i)).floatValue();
            }
            return vector;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.LLM_ERROR, "Ollama embedding 调用失败: " + e.getMessage());
        }
    }
}
