package com.agenthub.agent.service;

import com.agenthub.agent.config.AgentProperties;
import com.agenthub.agent.dto.ChatRequestDTO;
import com.agenthub.agent.dto.ChatResponseDTO;
import com.agenthub.agent.entity.Conversation;
import com.agenthub.agent.harness.ChatContext;
import com.agenthub.agent.harness.HarnessAgentEngine;
import com.agenthub.agent.memory.MemoryService;
import com.agenthub.agent.prompt.Prompts;
import com.agenthub.common.exception.BizException;
import com.agenthub.common.exception.ErrorCode;
import com.agenthub.llm.model.ChatMessage;
import com.agenthub.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent 服务：多轮对话入口，负责会话管理、记忆装载与引擎分发。
 *
 * <p>引擎分发：
 * - builtin：{@link HarnessAgentEngine}（内置 ReAct 循环，工具来自 ToolRegistry）；
 * - agentscope：{@link com.agenthub.agent.agentscope.AgentScopeAdapter}（AgentScope 2.0 HarnessAgent）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentProperties agentProperties;
    private final MemoryService memoryService;
    private final HarnessAgentEngine harnessAgentEngine;
    private final com.agenthub.agent.agentscope.AgentScopeAdapter agentScopeAdapter;
    private final com.agenthub.mcp.service.McpServerService mcpServerService;

    /** 多轮对话入口 */
    public ChatResponseDTO chat(ChatRequestDTO request) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.getUserId();

        // 0. 懒连接：确保当前租户启用的 MCP Server 已连接、工具已注册
        mcpServerService.ensureConnectedForTenant(tenantId);

        // 1. 会话管理：无会话ID则创建新会话
        Conversation conversation;
        if (request.getConversationId() == null) {
            conversation = memoryService.createConversation(
                    tenantId, userId, request.getKbId(), request.getModel(), agentProperties.getEngine());
        } else {
            conversation = memoryService.requireConversation(request.getConversationId());
        }

        // 2. 装载窗口记忆
        List<ChatMessage> history = memoryService.loadWindow(conversation.getId());
        if (request.getKbId() != null && conversation.getKbId() == null) {
            conversation.setKbId(request.getKbId());
        }

        // 3. 引擎分发执行
        ChatContext context = new ChatContext();
        context.setTenantId(tenantId);
        context.setUserId(userId);
        context.setConversationId(conversation.getId());
        context.setModel(request.getModel() == null ? conversation.getModel() : request.getModel());

        String answer = switch (agentProperties.getEngine()) {
            case "builtin" -> harnessAgentEngine.run(
                    context, Prompts.agentSystemPrompt(), history, request.getMessage());
            case "agentscope" -> agentScopeAdapter.run(
                    context, Prompts.agentSystemPrompt(), request.getMessage());
            default -> throw new BizException(ErrorCode.BUSINESS_ERROR,
                    "未知 Agent 引擎: " + agentProperties.getEngine());
        };

        // 4. 记忆追加
        memoryService.append(tenantId, conversation.getId(), request.getMessage(), answer, null);

        return new ChatResponseDTO(conversation.getId(), answer,
                agentProperties.getEngine(), context.getTrace(), context.getToolCallCount());
    }
}
