package com.agenthub.mcp.client;

import com.agenthub.agent.tool.AgentTool;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

/**
 * MCP 远程工具适配器：把 MCP Server 暴露的 Tool 包装为平台统一的 {@link AgentTool}，
 * 注册进 {@link com.agenthub.agent.tool.ToolRegistry} 后即可被 LLM 通过 function calling 调用。
 *
 * <p>命名规则：mcp__{serverName}__{toolName}，与内置工具命名空间隔离，避免冲突。</p>
 */
public class McpToolAdapter implements AgentTool {

    /** 工具命名前缀 */
    public static final String PREFIX = "mcp__";

    private final Long configId;
    private final String serverName;
    private final McpSchema.Tool tool;
    private final McpClientManager clientManager;

    public McpToolAdapter(Long configId, String serverName, McpSchema.Tool tool, McpClientManager clientManager) {
        this.configId = configId;
        this.serverName = serverName;
        this.tool = tool;
        this.clientManager = clientManager;
    }

    @Override
    public String name() {
        return PREFIX + serverName + "__" + tool.name();
    }

    @Override
    public String description() {
        String desc = tool.description();
        return (desc == null || desc.isBlank() ? "MCP 远程工具" : desc) + "（来自 MCP Server: " + serverName + "）";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> schema = tool.inputSchema();
        return schema == null || schema.isEmpty()
                ? Map.of("type", "object", "properties", Map.of())
                : schema;
    }

    @Override
    public String call(Map<String, Object> arguments) {
        return clientManager.callTool(configId, tool.name(), arguments);
    }
}
