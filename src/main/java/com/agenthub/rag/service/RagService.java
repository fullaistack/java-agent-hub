package com.agenthub.rag.service;

import com.agenthub.rag.embedding.EmbeddingRouter;
import com.agenthub.rag.model.RetrievalResult;
import com.agenthub.rag.model.ScoredChunk;
import com.agenthub.rag.rerank.Reranker;
import com.agenthub.rag.retrieve.HybridRetriever;
import com.agenthub.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 核心服务：检索、过滤、重排的编排入口。
 *
 * <p>被两类上层调用：
 * 1. REST 检索接口（直接返回片段）；
 * 2. Agent 的 RAG 工具（把片段拼入上下文交给 LLM）。</p>
 */
@Service
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingRouter embeddingRouter;
    private final HybridRetriever hybridRetriever;
    private final Reranker reranker;

    /**
     * 检索：向量化 → 混合检索（RRF）→ 重排。
     *
     * @param kbIds      知识库ID列表（当前租户内，可为 null 表示全部知识库——生产建议显式传）
     * @param query      查询文本
     * @param topK       返回条数
     * @param embeddingModel 向量化模型路由键（null 用默认）
     * @param filters    过滤条件（预留：doc_id / 元数据等）
     */
    public List<RetrievalResult> search(List<Long> kbIds, String query, int topK, String embeddingModel, java.util.Map<String, Object> filters) {
        Long tenantId = TenantContext.requireTenantId();
        float[] embedding = embeddingRouter.route(embeddingModel).embed(query);
        List<ScoredChunk> chunks = hybridRetriever.retrieve(tenantId, kbIds, query, embedding);
        List<ScoredChunk> reranked = reranker.rerank(query, chunks);

        List<RetrievalResult> results = new ArrayList<>();
        for (ScoredChunk chunk : reranked) {
            if (results.size() >= topK) {
                break;
            }
            results.add(new RetrievalResult(
                    chunk.getId(), chunk.getContent(), chunk.getDocName(), chunk.getChunkIndex(), chunk.getScore()));
        }
        return results;
    }

    /**
     * 把检索结果拼装为 LLM 上下文（带来源标注）。
     */
    public String buildContext(List<RetrievalResult> results) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            RetrievalResult r = results.get(i);
            sb.append("【文档片段 ").append(i + 1).append("】")
                    .append(" 来源文档: ").append(r.getDocName())
                    .append("（第").append(r.getChunkIndex() + 1).append("块）\n")
                    .append(r.getContent()).append('\n');
        }
        return sb.toString();
    }
}
