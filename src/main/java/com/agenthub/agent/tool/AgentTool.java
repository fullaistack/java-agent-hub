package com.agenthub.agent.tool;

import java.util.Map;

/**
 * Agent 工具统一抽象：RAG 检索工具与 MCP 远程工具都实现该接口，
 * 由 {@link ToolRegistry} 注册后暴露给 LLM（function calling）。
 */
public interface AgentTool {

    /** 工具名（全局唯一，MCP 工具命名 mcp__{server}__{tool}） */
    String name();

    /** 工具描述（LLM 依据它判断何时调用） */
    String description();

    /** 参数 JSON Schema（OpenAI function calling 格式的 parameters 部分） */
    Map<String, Object> inputSchema();

    /** 执行工具，返回结果文本 */
    String call(Map<String, Object> arguments);

    /** 转 OpenAI function calling 工具定义 */
    default Map<String, Object> toToolDefinition() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name(),
                        "description", description(),
                        "parameters", inputSchema()));
    }
}
