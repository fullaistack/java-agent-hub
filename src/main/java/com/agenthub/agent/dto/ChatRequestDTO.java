package com.agenthub.agent.dto;

import lombok.Data;

/**
 * 对话请求。
 */
@Data
public class ChatRequestDTO {

    /** 会话ID（可空：自动创建新会话） */
    private Long conversationId;

    /** 绑定的知识库ID（可空） */
    private Long kbId;

    /** 模型路由键（可空：使用默认模型） */
    private String model;

    /** 用户消息 */
    private String message;
}
