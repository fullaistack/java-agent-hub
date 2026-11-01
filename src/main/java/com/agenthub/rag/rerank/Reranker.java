package com.agenthub.rag.rerank;

import com.agenthub.rag.model.ScoredChunk;

import java.util.List;

/**
 * 重排抽象：对混合检索结果做精排。
 */
public interface Reranker {

    /** 重排检索块，返回精排后的结果 */
    List<ScoredChunk> rerank(String query, List<ScoredChunk> chunks);
}
