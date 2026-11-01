package com.agenthub.mcp.service;

import com.agenthub.agent.agentscope.AgentScopeAdapter;
import com.agenthub.agent.tool.ToolRegistry;
import com.agenthub.common.exception.BizException;
import com.agenthub.common.exception.ErrorCode;
import com.agenthub.mcp.client.McpClientManager;
import com.agenthub.mcp.client.McpToolAdapter;
import com.agenthub.mcp.dto.McpServerRequest;
import com.agenthub.mcp.entity.McpServerConfig;
import com.agenthub.mcp.mapper.McpServerConfigMapper;
import com.agenthub.tenant.context.TenantContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MCP Server 配置服务：配置 CRUD、连接管理、工具注册。
 *
 * <p>工具注册链路：连接成功 → 发现工具 → 以 mcp__{server}__{tool} 命名
 * 注册进 {@link ToolRegistry} → 内置引擎直接可用，AgentScope 引擎通过
 * {@link AgentScopeAdapter#refresh()} 重建生效。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpServerService {

    private final McpServerConfigMapper configMapper;
    private final McpClientManager clientManager;
    private final ToolRegistry toolRegistry;
    private final AgentScopeAdapter agentScopeAdapter;

    /** 创建配置 */
    public McpServerConfig create(McpServerRequest request) {
        validate(request);
        McpServerConfig config = new McpServerConfig();
        BeanUtils.copyProperties(request, config);
        config.setTenantId(TenantContext.requireTenantId());
        config.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        configMapper.insert(config);
        return config;
    }

    /** 更新配置（已连接的先断开，改完需重新连接） */
    public McpServerConfig update(Long id, McpServerRequest request) {
        McpServerConfig config = require(id);
        validate(request);
        disconnect(id);
        BeanUtils.copyProperties(request, config);
        config.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        configMapper.updateById(config);
        return config;
    }

    /** 删除配置并断开连接 */
    public void delete(Long id) {
        require(id);
        disconnect(id);
        configMapper.deleteById(id);
    }

    /** 配置列表（租户隔离由 MyBatis-Plus 插件完成） */
    public List<McpServerConfig> list() {
        return configMapper.selectList(new LambdaQueryWrapper<McpServerConfig>()
                .orderByDesc(McpServerConfig::getId));
    }

    /** 建立连接并把 Server 工具注册进 Agent 工具注册表 */
    public List<McpSchema.Tool> connect(Long id) {
        McpServerConfig config = require(id);
        List<McpSchema.Tool> tools = clientManager.connect(config);
        for (McpSchema.Tool tool : tools) {
            toolRegistry.register(new McpToolAdapter(config.getId(), config.getName(), tool, clientManager));
        }
        agentScopeAdapter.refresh();
        return tools;
    }

    /** 断开连接并注销该 Server 的全部工具 */
    public void disconnect(Long id) {
        McpServerConfig config = configMapper.selectById(id);
        if (config == null) {
            return;
        }
        clientManager.disconnect(id);
        toolRegistry.snapshot().keySet().stream()
                .filter(name -> name.startsWith(McpToolAdapter.PREFIX + config.getName() + "__"))
                .forEach(toolRegistry::unregister);
        agentScopeAdapter.refresh();
    }

    /** 列出已连接 Server 的工具 */
    public List<McpSchema.Tool> listTools(Long id) {
        require(id);
        return clientManager.listTools(id);
    }

    /** 直接调用 MCP 工具（调试/演示用，Agent 场景走 ToolRegistry） */
    public String callTool(Long id, String toolName, Map<String, Object> arguments) {
        require(id);
        return clientManager.callTool(id, toolName, arguments);
    }

    /**
     * 确保当前租户的全部启用配置已建立连接（懒连接）。
     *
     * <p>由 Agent 入口调用：连接是全局资源，但每个租户只连接自己的配置，
     * 避免启动阶段无租户上下文时跨租户重连。已连接的配置跳过。</p>
     */
    public void ensureConnectedForTenant(Long tenantId) {
        List<McpServerConfig> enabled = configMapper.selectList(
                new LambdaQueryWrapper<McpServerConfig>().eq(McpServerConfig::getEnabled, true));
        for (McpServerConfig config : enabled) {
            if (clientManager.isConnected(config.getId())) {
                continue;
            }
            try {
                connect(config.getId());
            } catch (Exception e) {
                log.error("租户 MCP Server 懒连接失败: tenant={}, name={}, err={}",
                        tenantId, config.getName(), e.getMessage());
            }
        }
    }

    private McpServerConfig require(Long id) {
        McpServerConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "MCP Server 配置不存在: " + id);
        }
        return config;
    }

    private void validate(McpServerRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "配置名不能为空");
        }
        if (request.getTransportType() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "传输类型不能为空（stdio/sse/streamable-http）");
        }
        if (!"stdio".equals(request.getTransportType())
                && (request.getBaseUrl() == null || request.getBaseUrl().isBlank())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "SSE/Streamable HTTP 模式必须配置 baseUrl");
        }
        if ("stdio".equals(request.getTransportType())
                && (request.getCommand() == null || request.getCommand().isBlank())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "STDIO 模式必须配置启动命令");
        }
    }
}
