package com.agenthub.rag.rerank;

import com.agenthub.rag.model.ScoredChunk;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 相似度重排（默认实现）：直接沿用 RRF 融合分排序。
 *
 * <p>生产环境可替换为 BGE-Reranker 等交叉编码模型（如通过 Xinference/TEI 的 /rerank 接口），
 * 只需新增一个 {@link Reranker} 实现并按需装配。</p>
 */
@Component
public class SimilarityReranker implements Reranker {

    @Override
    public List<ScoredChunk> rerank(String query, List<ScoredChunk> chunks) {
        return chunks.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .toList();
    }
}
