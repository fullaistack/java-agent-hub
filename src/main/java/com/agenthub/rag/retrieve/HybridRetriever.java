package com.agenthub.rag.retrieve;

import com.agenthub.rag.model.ScoredChunk;
import com.agenthub.rag.store.VectorStore;
import com.agenthub.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 混合检索器：向量检索（余弦相似度）+ 关键字检索（词命中计数），RRF 融合排序。
 *
 * <p>RRF（Reciprocal Rank Fusion）：{@code score = Σ 1/(k + rank)}。
 * 不依赖两路分数的绝对量纲，能稳定融合语义相关性与关键词精确匹配。
 * 融合后按 chunk id 去重合并。</p>
 */
@Service
@RequiredArgsConstructor
public class HybridRetriever {

    private final VectorStore vectorStore;

    @Value("${agenthub.rag.vector-top-k:5}")
    private int vectorTopK;

    @Value("${agenthub.rag.keyword-top-k:5}")
    private int keywordTopK;

    @Value("${agenthub.rag.rrf-k:60}")
    private int rrfK;

    @Value("${agenthub.rag.hybrid:true}")
    private boolean hybrid;

    /**
     * 混合检索。
     *
     * @param kbIds     知识库ID集合（租户内）
     * @param query     查询文本
     * @param embedding 查询向量（由调用方预先向量化）
     */
    public List<ScoredChunk> retrieve(Long tenantId, List<Long> kbIds, String query, float[] embedding) {
        // 第一路：向量检索
        List<ScoredChunk> vectorHits = vectorStore.vectorSearch(tenantId, kbIds, embedding, vectorTopK);
        if (!hybrid) {
            return vectorHits;
        }
        // 第二路：关键字检索
        List<ScoredChunk> keywordHits = vectorStore.keywordSearch(tenantId, kbIds, query, keywordTopK);

        // RRF 融合
        Map<Long, Double> rrfScores = new HashMap<>();
        Map<Long, ScoredChunk> chunksById = new HashMap<>();
        accumulateRrf(vectorHits, rrfScores, chunksById);
        accumulateRrf(keywordHits, rrfScores, chunksById);

        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(e -> {
                    ScoredChunk chunk = chunksById.get(e.getKey());
                    chunk.setScore(e.getValue());
                    return chunk;
                })
                .toList();
    }

    private void accumulateRrf(List<ScoredChunk> hits, Map<Long, Double> scores, Map<Long, ScoredChunk> chunksById) {
        for (int i = 0; i < hits.size(); i++) {
            ScoredChunk chunk = hits.get(i);
            scores.merge(chunk.getId(), 1.0 / (rrfK + i + 1), Double::sum);
            chunksById.putIfAbsent(chunk.getId(), chunk);
        }
    }
}
