package com.agenthub.mcp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP Server 连接配置：一个租户可接入多个 MCP Server，
 * 连接成功后其工具以 mcp__{server}__{tool} 命名注册进 Agent 工具注册表。
 */
@Data
@TableName("mcp_server_config")
public class McpServerConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 配置名（唯一标识，工具名前缀） */
    private String name;

    private String description;

    /** 传输类型：stdio | sse | streamable-http */
    private String transportType;

    /** SSE / Streamable HTTP 服务地址 */
    private String baseUrl;

    /** Streamable HTTP 的 endpoint（默认 /mcp） */
    private String endpoint;

    /** STDIO 模式启动命令，如 npx / uvx / java -jar */
    private String command;

    /** STDIO 模式启动参数（JSON 数组） */
    private String argsJson;

    /** 环境变量（JSON 对象，如 API Key） */
    private String envJson;

    /** 是否启用：启用状态在应用启动时自动建立连接 */
    private Boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
