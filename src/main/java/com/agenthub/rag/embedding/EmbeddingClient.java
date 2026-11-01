package com.agenthub.rag.embedding;

import java.util.List;

/**
 * 向量化抽象：支持 OpenAI 兼容接口 / Ollama / Mock 实现。
 */
public interface EmbeddingClient {

    /** 实现标识（配置 providers[].name 路由键） */
    String name();

    /** 批量向量化 */
    List<float[]> embed(List<String> texts);

    default float[] embed(String text) {
        return embed(List.of(text)).get(0);
    }
}
