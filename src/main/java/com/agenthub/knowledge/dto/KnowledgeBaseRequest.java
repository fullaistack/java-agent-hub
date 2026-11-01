package com.agenthub.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 知识库创建/更新请求。
 */
@Data
public class KnowledgeBaseRequest {

    @NotBlank(message = "知识库名称不能为空")
    private String name;

    private String description;

    /** 向量化模型路由键，缺省使用全局默认 */
    private String embeddingModel;

    /** 分块大小，缺省 800 */
    private Integer chunkSize = 800;

    /** 分块重叠，缺省 100 */
    private Integer chunkOverlap = 100;
}
