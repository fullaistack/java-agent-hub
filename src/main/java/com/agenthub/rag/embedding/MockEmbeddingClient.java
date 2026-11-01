package com.agenthub.rag.embedding;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mock Embedding：基于内容哈希生成确定性向量，维度可配置（默认 1024）。
 *
 * <p>用途：无外部 Embedding 服务时跑通全链路（上传→解析→切块→入库→检索）。
 * 同一文本始终生成同一向量，相似文本生成部分重叠的向量，
 * 因此关键字检索仍能命中；生产环境请替换为真实 Embedding 模型。</p>
 */
public class MockEmbeddingClient implements EmbeddingClient {

    public static final String NAME = "mock-embed";

    private final int dimension;

    public MockEmbeddingClient(int dimension) {
        this.dimension = dimension;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> result = new ArrayList<>(texts.size());
        for (String text : texts) {
            result.add(embedSingle(text));
        }
        return result;
    }

    private float[] embedSingle(String text) {
        float[] vector = new float[dimension];
        // 简单特征哈希：中文按单字、英文按空格分词，映射到向量维度
        for (String token : tokenize(text)) {
            int idx = Math.floorMod(token.hashCode(), dimension);
            vector[idx] += 1.0f;
        }
        // L2 归一化，保证余弦相似度语义一致
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }

    /** 简单分词：中文逐字 + 英文按空格/标点切词 */
    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (char c : text.toLowerCase().toCharArray()) {
            if (Character.isLetterOrDigit(c) && c < 128) {
                word.append(c);
            } else {
                if (!word.isEmpty()) {
                    tokens.add(word.toString());
                    word.setLength(0);
                }
                if (c >= 0x4e00 && c <= 0x9fff) {
                    tokens.add(String.valueOf(c));
                }
            }
        }
        if (!word.isEmpty()) {
            tokens.add(word.toString());
        }
        return tokens;
    }
}
