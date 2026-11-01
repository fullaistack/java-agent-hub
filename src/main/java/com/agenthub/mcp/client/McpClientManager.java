package com.agenthub.mcp.client;

import com.agenthub.common.exception.BizException;
import com.agenthub.common.exception.ErrorCode;
import com.agenthub.mcp.entity.McpServerConfig;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Client 管理器：按配置建立与 MCP Server 的连接，
 * 提供工具发现与调用能力，供 Agent 编排层使用。
 *
 * <p>三种传输模式（与官方 MCP Java SDK 2.0 对齐）：
 * - streamable-http：主流远程 HTTP 模式；
 * - sse：SSE 长连接模式；
 * - stdio：本地进程模式（npx/uvx/java -jar 拉起子进程）。</p>
 */
@Slf4j
@Component
public class McpClientManager {

    /** 已连接的客户端：key = 配置ID */
    private final Map<Long, McpSyncClient> clients = new ConcurrentHashMap<>();

    /** 建立连接并初始化握手，返回 Server 暴露的工具列表 */
    public List<McpSchema.Tool> connect(McpServerConfig config) {
        disconnect(config.getId());
        try {
            McpClientTransport transport = buildTransport(config);
            McpSyncClient client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(30))
                    .build();
            client.initialize();
            clients.put(config.getId(), client);
            List<McpSchema.Tool> tools = client.listTools().tools();
            log.info("MCP Server 连接成功: name={}, transport={}, tools={}",
                    config.getName(), config.getTransportType(), tools.size());
            return tools;
        } catch (Exception e) {
            log.error("MCP Server 连接失败: name={}, err={}", config.getName(), e.getMessage());
            throw new BizException(ErrorCode.MCP_ERROR, "MCP Server 连接失败: " + e.getMessage());
        }
    }

    /** 断开并释放连接 */
    public void disconnect(Long configId) {
        McpSyncClient client = clients.remove(configId);
        if (client != null) {
            try {
                client.closeGracefully();
            } catch (Exception e) {
                log.warn("MCP 连接关闭异常: configId={}, err={}", configId, e.getMessage());
            }
        }
    }

    /** 是否已连接 */
    public boolean isConnected(Long configId) {
        return clients.containsKey(configId);
    }

    /** 列出已连接 Server 的工具 */
    public List<McpSchema.Tool> listTools(Long configId) {
        return require(configId).listTools().tools();
    }

    /** 调用工具：提取文本内容返回 */
    public String callTool(Long configId, String toolName, Map<String, Object> arguments) {
        try {
            McpSchema.CallToolResult result = require(configId).callTool(
                    McpSchema.CallToolRequest.builder(toolName).arguments(arguments).build());
            if (Boolean.TRUE.equals(result.isError())) {
                return "MCP 工具执行失败: " + extractText(result);
            }
            return extractText(result);
        } catch (Exception e) {
            log.warn("MCP 工具调用失败: configId={}, tool={}, err={}", configId, toolName, e.getMessage());
            throw new BizException(ErrorCode.MCP_ERROR, "MCP 工具调用失败: " + e.getMessage());
        }
    }

    /** 提取 CallToolResult 的文本内容（TextContent 拼接） */
    private String extractText(McpSchema.CallToolResult result) {
        StringBuilder sb = new StringBuilder();
        for (McpSchema.Content content : result.content()) {
            if ("text".equals(content.type()) && content instanceof McpSchema.TextContent text) {
                sb.append(text.text());
            }
        }
        return sb.toString();
    }

    private McpSyncClient require(Long configId) {
        McpSyncClient client = clients.get(configId);
        if (client == null) {
            throw new BizException(ErrorCode.MCP_ERROR, "MCP Server 未连接: " + configId);
        }
        return client;
    }

    /** 按传输类型构建 Transport */
    private McpClientTransport buildTransport(McpServerConfig config) {
        return switch (config.getTransportType()) {
            case "streamable-http" -> HttpClientStreamableHttpTransport.builder(config.getBaseUrl())
                    .endpoint(config.getEndpoint() == null || config.getEndpoint().isBlank() ? "/mcp" : config.getEndpoint())
                    .build();
            case "sse" -> HttpClientSseClientTransport.builder(config.getBaseUrl()).build();
            case "stdio" -> new StdioClientTransport(buildServerParameters(config), McpJsonDefaults.getMapper());
            default -> throw new BizException(ErrorCode.PARAM_ERROR,
                    "不支持的传输类型: " + config.getTransportType());
        };
    }

    /** STDIO 模式启动参数 */
    private ServerParameters buildServerParameters(McpServerConfig config) {
        ServerParameters.Builder builder = ServerParameters.builder(config.getCommand());
        List<String> args = com.agenthub.common.util.JsonUtils.fromJson(
                config.getArgsJson() == null || config.getArgsJson().isBlank() ? "[]" : config.getArgsJson(),
                new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                });
        builder.args(args);
        Map<String, String> env = com.agenthub.common.util.JsonUtils.fromJson(
                config.getEnvJson() == null || config.getEnvJson().isBlank() ? "{}" : config.getEnvJson(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                });
        builder.env(env);
        return builder.build();
    }

    @PreDestroy
    public void closeAll() {
        clients.keySet().forEach(this::disconnect);
    }
}
