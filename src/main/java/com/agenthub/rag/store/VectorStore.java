package com.agenthub.rag.store;

import com.agenthub.rag.model.Chunk;
import com.agenthub.rag.model.ScoredChunk;

import java.util.List;

/**
 * 向量存储抽象：当前实现为 PGVector，后续可扩展 Milvus/Elasticsearch 等。
 * 所有方法强制要求 tenantId 参数——租户隔离在存储层不可绕过。
 */
public interface VectorStore {

    /** 批量写入向量块 */
    void batchInsert(List<Chunk> chunks);

    /** 向量检索（余弦相似度 TopK） */
    List<ScoredChunk> vectorSearch(Long tenantId, List<Long> kbIds, float[] embedding, int topK);

    /** 关键字检索（ILIKE 匹配计数打分） */
    List<ScoredChunk> keywordSearch(Long tenantId, List<Long> kbIds, String query, int topK);

    /** 删除文档的全部向量（重试前清理） */
    void deleteByDoc(Long tenantId, Long docId);

    /** 删除知识库的全部向量 */
    void deleteByKb(Long tenantId, Long kbId);
}
