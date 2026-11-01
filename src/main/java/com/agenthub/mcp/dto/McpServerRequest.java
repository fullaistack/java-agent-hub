package com.agenthub.mcp.dto;

import lombok.Data;

/**
 * MCP Server 配置请求（创建/更新共用）。
 */
@Data
public class McpServerRequest {

    /** 配置名（唯一标识，工具名前缀） */
    private String name;

    private String description;

    /** 传输类型：stdio | sse | streamable-http */
    private String transportType;

    /** SSE / Streamable HTTP 服务地址 */
    private String baseUrl;

    /** Streamable HTTP 的 endpoint（默认 /mcp） */
    private String endpoint;

    /** STDIO 模式启动命令 */
    private String command;

    /** STDIO 模式启动参数（JSON 数组字符串） */
    private String argsJson;

    /** 环境变量（JSON 对象字符串） */
    private String envJson;

    /** 是否启用 */
    private Boolean enabled;
}
