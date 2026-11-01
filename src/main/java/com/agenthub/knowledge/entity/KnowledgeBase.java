package com.agenthub.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库：租户下的向量数据集合边界，检索时以 kb_id 为过滤条件。
 */
@Data
@TableName("knowledge_base")
public class KnowledgeBase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String name;

    private String description;

    /** 向量化模型（路由键，对应 agenthub.embedding.providers[].name） */
    private String embeddingModel;

    /** 分块大小（字符数） */
    private Integer chunkSize;

    /** 分块重叠（字符数） */
    private Integer chunkOverlap;

    /** 状态：1启用 0停用 */
    private Integer status;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
