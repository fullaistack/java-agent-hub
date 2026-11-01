package com.agenthub.agent.agentscope;

import com.agenthub.agent.tool.AgentTool;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.tool.ToolCallParam;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 工具桥接器：把平台统一抽象 {@link AgentTool} 包装为 AgentScope 的
 * {@link io.agentscope.core.tool.AgentTool}，使 HarnessAgent 能直接调用
 * 平台注册表里的 RAG 检索工具与 MCP 远程工具。
 *
 * <p>执行链路：HarnessAgent 发起工具调用 → callAsync 收到参数
 * → 委托给平台 ToolRegistry 中的同名工具 → 结果封装为 ToolResultBlock 返回。</p>
 */
public class AgentScopeToolBridge implements io.agentscope.core.tool.AgentTool {

    private final AgentTool delegate;

    public AgentScopeToolBridge(AgentTool delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.name();
    }

    @Override
    public String getDescription() {
        return delegate.description();
    }

    @Override
    public Map<String, Object> getParameters() {
        return delegate.inputSchema();
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        return Mono.fromCallable(() -> {
            String result = delegate.call(param.getInput());
            ToolUseBlock use = param.getToolUseBlock();
            String id = use == null ? null : use.getId();
            return new ToolResultBlock(id, delegate.name(),
                    List.of(TextBlock.builder().text(result).build()));
        });
    }
}
