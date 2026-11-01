package com.agenthub.rag.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检索结果（对外返回）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResult {

    private Long chunkId;

    /** 片段内容 */
    private String content;

    /** 来源文档名（用于引用标注） */
    private String docName;

    private Integer chunkIndex;

    /** 最终排序分数 */
    private double score;
}
