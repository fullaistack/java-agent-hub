package com.agenthub.agent.memory;

import com.agenthub.agent.entity.Conversation;
import com.agenthub.agent.entity.ConversationMessage;
import com.agenthub.agent.mapper.ConversationMapper;
import com.agenthub.agent.mapper.ConversationMessageMapper;
import com.agenthub.common.exception.BizException;
import com.agenthub.common.exception.ErrorCode;
import com.agenthub.common.util.JsonUtils;
import com.agenthub.llm.model.ChatMessage;
import com.agenthub.llm.model.ToolCall;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 记忆管理：多轮对话的窗口记忆。
 *
 * <p>当前实现为"最近 N 条消息窗口截断"（agenthub.agent.memory-window），
 * 生产可扩展：LLM 摘要压缩、长期记忆（对接 AgentScope 的 Memory 扩展）等。</p>
 */
@Service
@RequiredArgsConstructor
public class MemoryService {

    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper messageMapper;

    @Value("${agenthub.agent.memory-window:10}")
    private int memoryWindow;

    /** 创建会话 */
    public Conversation createConversation(Long tenantId, Long userId, Long kbId, String model, String engine) {
        Conversation conversation = new Conversation();
        conversation.setTenantId(tenantId);
        conversation.setUserId(userId);
        conversation.setKbId(kbId);
        conversation.setModel(model);
        conversation.setEngine(engine);
        conversation.setTitle("新会话");
        conversationMapper.insert(conversation);
        return conversation;
    }

    /** 获取会话（租户隔离由 MyBatis 插件保证） */
    public Conversation requireConversation(Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "会话不存在: " + conversationId);
        }
        return conversation;
    }

    /** 记录一轮对话（用户消息 + 最终回答） */
    public void append(Long tenantId, Long conversationId, String userInput, String answer, String toolCallsJson) {
        ConversationMessage userMsg = new ConversationMessage();
        userMsg.setConversationId(conversationId);
        userMsg.setTenantId(tenantId);
        userMsg.setRole("user");
        userMsg.setContent(userInput);
        messageMapper.insert(userMsg);

        ConversationMessage assistantMsg = new ConversationMessage();
        assistantMsg.setConversationId(conversationId);
        assistantMsg.setTenantId(tenantId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(answer);
        assistantMsg.setToolCalls(toolCallsJson);
        messageMapper.insert(assistantMsg);
    }

    /** 读取最近 N 条历史消息（窗口记忆） */
    public List<ChatMessage> loadWindow(Long conversationId) {
        List<ConversationMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getConversationId, conversationId)
                        .orderByDesc(ConversationMessage::getId)
                        .last("LIMIT " + memoryWindow));
        return messages.stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(this::toChatMessage)
                .toList();
    }

    private ChatMessage toChatMessage(ConversationMessage m) {
        ChatMessage chatMessage = new ChatMessage(m.getRole(), m.getContent());
        if (m.getToolCalls() != null && !m.getToolCalls().isBlank()) {
            try {
                chatMessage.setToolCalls(JsonUtils.fromJson(m.getToolCalls(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<ToolCall>>() {
                        }));
            } catch (Exception ignored) {
                // 工具调用明细解析失败不影响对话主流程
            }
        }
        return chatMessage;
    }
}
