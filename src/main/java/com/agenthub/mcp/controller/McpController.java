package com.agenthub.mcp.controller;

import com.agenthub.common.api.ApiResponse;
import com.agenthub.mcp.dto.McpServerRequest;
import com.agenthub.mcp.entity.McpServerConfig;
import com.agenthub.mcp.service.McpServerService;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * MCP Server 管理接口：配置 CRUD、连接管理、工具发现与直接调用。
 */
@RestController
@RequestMapping("/api/v1/mcp/servers")
@RequiredArgsConstructor
public class McpController {

    private final McpServerService mcpServerService;

    /** 新增 MCP Server 配置 */
    @PostMapping
    public ApiResponse<McpServerConfig> create(@RequestBody McpServerRequest request) {
        return ApiResponse.ok(mcpServerService.create(request));
    }

    /** 更新配置 */
    @PutMapping("/{id}")
    public ApiResponse<McpServerConfig> update(@PathVariable Long id, @RequestBody McpServerRequest request) {
        return ApiResponse.ok(mcpServerService.update(id, request));
    }

    /** 删除配置 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        mcpServerService.delete(id);
        return ApiResponse.ok();
    }

    /** 配置列表 */
    @GetMapping
    public ApiResponse<List<McpServerConfig>> list() {
        return ApiResponse.ok(mcpServerService.list());
    }

    /** 建立连接并注册工具到 Agent */
    @PostMapping("/{id}/connect")
    public ApiResponse<List<McpSchema.Tool>> connect(@PathVariable Long id) {
        return ApiResponse.ok(mcpServerService.connect(id));
    }

    /** 断开连接并注销工具 */
    @PostMapping("/{id}/disconnect")
    public ApiResponse<Void> disconnect(@PathVariable Long id) {
        mcpServerService.disconnect(id);
        return ApiResponse.ok();
    }

    /** 列出已连接 Server 的工具 */
    @GetMapping("/{id}/tools")
    public ApiResponse<List<McpSchema.Tool>> listTools(@PathVariable Long id) {
        return ApiResponse.ok(mcpServerService.listTools(id));
    }

    /** 直接调用 MCP 工具（调试/演示用） */
    @PostMapping("/{id}/tools/{toolName}/call")
    public ApiResponse<String> callTool(@PathVariable Long id,
                                        @PathVariable String toolName,
                                        @RequestBody(required = false) Map<String, Object> arguments) {
        return ApiResponse.ok(mcpServerService.callTool(id, toolName,
                arguments == null ? Map.of() : arguments));
    }
}
