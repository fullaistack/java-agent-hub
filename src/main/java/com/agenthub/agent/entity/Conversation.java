package com.agenthub.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话：Agent 多轮对话的容器，属于租户。
 */
@Data
@TableName("conversation")
public class Conversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long userId;

    /** 绑定的知识库ID（可空） */
    private Long kbId;

    private String title;

    /** 引擎：builtin / agentscope */
    private String engine;

    /** 模型路由键 */
    private String model;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
