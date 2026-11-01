package com.agenthub.llm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模型调用响应（统一结构）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /** 生成内容（无工具调用时为最终回答） */
    private String content;

    /** 工具调用列表（ReAct 循环据此执行工具） */
    private List<ToolCall> toolCalls;

    /** token 消耗 */
    private TokenUsage usage;

    /** 实际使用的引擎模型名 */
    private String engineModel;
}
