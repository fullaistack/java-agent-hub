package com.agenthub.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 对话响应：回答 + 会话ID + 执行轨迹（观测用）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDTO {

    private Long conversationId;

    private String answer;

    /** 引擎类型：builtin / agentscope */
    private String engine;

    /** 本轮工具调用轨迹 */
    private List<String> trace;

    /** 工具调用次数 */
    private int toolCallCount;
}
