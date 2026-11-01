package com.agenthub.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话消息：多轮记忆的持久化载体，检索最近 N 条拼入 LLM 上下文。
 */
@Data
@TableName("conversation_message")
public class ConversationMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private Long tenantId;

    /** user / assistant / tool */
    private String role;

    private String content;

    /** 工具调用明细（assistant 消息，JSON） */
    private String toolCalls;

    private LocalDateTime createdAt;
}
