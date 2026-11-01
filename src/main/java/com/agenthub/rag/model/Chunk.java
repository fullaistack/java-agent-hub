package com.agenthub.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 向量块：待写入 PGVector 的最小检索单元。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {

    /** 租户ID（强制隔离维度） */
    private Long tenantId;

    /** 知识库ID */
    private Long kbId;

    /** 文档ID */
    private Long docId;

    /** 来源文档名（检索后用于引用标注） */
    private String docName;

    /** 块在文档内的序号 */
    private Integer chunkIndex;

    /** 块内容 */
    private String content;

    /** 向量 */
    private float[] embedding;

    /** 附加元数据（JSONB） */
    private Map<String, Object> metadata;
}
