package com.agenthub.agent.tool;

import com.agenthub.common.exception.BizException;
import com.agenthub.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册表：内存注册 Agent 可用工具（内置 RAG 工具 + MCP 远程工具）。
 *
 * <p>内置引擎（builtin）直接从这里取工具执行；
 * AgentScope 引擎（agentscope）在适配层把注册表中的工具桥接为 AgentScope ToolBase。</p>
 */
@Component
public class ToolRegistry {

    private final Map<String, AgentTool> tools = new ConcurrentHashMap<>();

    /** 注册工具（同名覆盖） */
    public void register(AgentTool tool) {
        tools.put(tool.name(), tool);
    }

    /** 注销工具 */
    public void unregister(String name) {
        tools.remove(name);
    }

    /** 按名获取工具 */
    public AgentTool require(String name) {
        AgentTool tool = tools.get(name);
        if (tool == null) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "未注册的工具: " + name);
        }
        return tool;
    }

    /** 全部工具定义（LLM function calling） */
    public List<Map<String, Object>> toolDefinitions() {
        return tools.values().stream().map(AgentTool::toToolDefinition).toList();
    }

    /** 执行工具 */
    public String call(String name, Map<String, Object> arguments) {
        return require(name).call(arguments);
    }

    public Map<String, AgentTool> snapshot() {
        return Map.copyOf(tools);
    }
}
