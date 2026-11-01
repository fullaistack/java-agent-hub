package com.agenthub.agent.harness;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 单次对话上下文：贯穿一次 Agent 调用的整个生命周期。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatContext {

    /** 租户ID */
    private Long tenantId;

    /** 用户ID（可空） */
    private Long userId;

    /** 会话ID */
    private Long conversationId;

    /** 用户输入 */
    private String userInput;

    /** 模型路由键 */
    private String model;

    /** 本轮累计工具调用次数 */
    private int toolCallCount;

    /** 执行轨迹（用于观测与调试） */
    private final List<String> trace = new ArrayList<>();

    public void appendTrace(String event) {
        trace.add(event);
    }
}
