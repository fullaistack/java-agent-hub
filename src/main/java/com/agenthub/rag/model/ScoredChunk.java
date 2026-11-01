package com.agenthub.rag.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 带分数的检索块：向量检索（余弦相似度）、关键字检索（匹配计数）、RRF 融合分统一用 score 表达。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoredChunk {

    /** rag_chunk 主键 */
    private Long id;

    private String content;

    /** 来源文档名 */
    private String docName;

    /** 块序号 */
    private Integer chunkIndex;

    /** 分数（不同来源含义不同，仅用于排序/融合） */
    private double score;

    private Map<String, Object> metadata;
}
