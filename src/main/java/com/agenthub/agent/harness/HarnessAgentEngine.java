package com.agenthub.agent.harness;

import com.agenthub.agent.tool.ToolRegistry;
import com.agenthub.common.exception.BizException;
import com.agenthub.common.exception.ErrorCode;
import com.agenthub.common.util.JsonUtils;
import com.agenthub.llm.model.ChatMessage;
import com.agenthub.llm.model.ChatRequest;
import com.agenthub.llm.model.ChatResponse;
import com.agenthub.llm.model.ToolCall;
import com.agenthub.llm.router.ModelRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Harness 编排引擎（内置 ReAct 实现）。
 *
 * <p>执行循环：组装上下文 → 调 LLM → 有工具调用则执行工具并回填结果 → 继续，
 * 直到模型给出最终回答或超过最大迭代次数。
 * 生命周期钩子（{@link AgentHook}）在 LLM 调用与工具执行前后插入，
 * 与 AgentScope HarnessAgent 的 Hook 机制对齐，方便两种引擎无缝切换。</p>
 *
 * <p>注意：Agent 循环中的"工具调用"由 LLM 发起（function calling），
 * 与"多轮对话记忆"解耦——记忆由 {@code MemoryService} 维护，
 * 每次循环前把窗口内的历史消息拼入上下文。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HarnessAgentEngine {

    private final ModelRouter modelRouter;
    private final ToolRegistry toolRegistry;
    private final List<AgentHook> hooks;

    @Value("${agenthub.agent.max-iterations:5}")
    private int maxIterations;

    /**
     * 执行一次对话（多轮工具调用循环）。
     *
     * @param systemPrompt 系统提示词
     * @param history      历史消息（不含本次输入）
     * @param userInput    本次用户输入
     */
    public String run(ChatContext context, String systemPrompt, List<ChatMessage> history, String userInput) {
        // 1. 组装消息：系统提示词 + 历史 + 本次输入
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(systemPrompt));
        messages.addAll(history);
        messages.add(ChatMessage.user(userInput));
        context.setUserInput(userInput);
        context.appendTrace("start");

        // 2. ReAct 循环
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            ChatRequest request = ChatRequest.builder()
                    .model(context.getModel())
                    .messages(messages)
                    .tools(toolRegistry.toolDefinitions().isEmpty() ? null : toolRegistry.toolDefinitions())
                    .build();

            // hook: LLM 调用前
            request = applyBeforeLLM(request, context);
            ChatResponse response = modelRouter.chat(request);
            applyAfterLLM(request, response, context);
            context.appendTrace("llm[" + iteration + "]");

            // 3. 无工具调用 → 最终回答
            if (response.getToolCalls() == null || response.getToolCalls().isEmpty()) {
                String answer = response.getContent();
                applyOnFinish(context, answer);
                return answer;
            }

            // 4. 有工具调用 → 逐个执行并回填结果
            ChatMessage assistantMsg = new ChatMessage("assistant", "");
            assistantMsg.setToolCalls(response.getToolCalls());
            messages.add(assistantMsg);
            for (ToolCall toolCall : response.getToolCalls()) {
                Map<String, Object> arguments = parseArguments(toolCall.getArguments());
                // hook: 工具调用前（权限/参数改写）
                arguments = applyBeforeTool(toolCall, arguments, context);
                String result;
                try {
                    result = toolRegistry.call(toolCall.getName(), arguments);
                } catch (Exception e) {
                    result = "工具调用失败: " + e.getMessage();
                    log.warn("工具调用失败: tool={}, err={}", toolCall.getName(), e.getMessage());
                }
                applyAfterTool(toolCall, result, context);
                context.setToolCallCount(context.getToolCallCount() + 1);
                context.appendTrace("tool:" + toolCall.getName());
                ChatMessage toolMsg = new ChatMessage("tool", result);
                toolMsg.setToolCallId(toolCall.getId());
                messages.add(toolMsg);
            }
        }
        throw new BizException(ErrorCode.BUSINESS_ERROR, "Agent 超过最大迭代次数(" + maxIterations + ")仍未收敛");
    }

    // ---------- Hook 编排 ----------

    private ChatRequest applyBeforeLLM(ChatRequest request, ChatContext context) {
        for (AgentHook hook : hooks) {
            request = hook.beforeLLM(request, context);
        }
        return request;
    }

    private void applyAfterLLM(ChatRequest request, ChatResponse response, ChatContext context) {
        hooks.forEach(h -> h.afterLLM(request, response, context));
    }

    private Map<String, Object> applyBeforeTool(ToolCall toolCall, Map<String, Object> arguments, ChatContext context) {
        for (AgentHook hook : hooks) {
            arguments = hook.beforeTool(toolCall, arguments, context);
        }
        return arguments;
    }

    private void applyAfterTool(ToolCall toolCall, String result, ChatContext context) {
        hooks.forEach(h -> h.afterTool(toolCall, result, context));
    }

    private void applyOnFinish(ChatContext context, String answer) {
        hooks.forEach(h -> h.onFinish(context, answer));
    }

    // ---------- 消息组装 ----------

    private Map<String, Object> parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return Map.of();
        }
        try {
            return JsonUtils.parseMap(argumentsJson);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
