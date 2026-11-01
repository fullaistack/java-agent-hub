package com.agenthub.agent.harness;

import com.agenthub.llm.model.ChatRequest;
import com.agenthub.llm.model.ChatResponse;
import com.agenthub.llm.model.ToolCall;

import java.util.List;
import java.util.Map;

/**
 * Agent Hook 生命周期：在编排引擎的关键节点插入扩展逻辑，
 * 对应 AgentScope Harness 的 Hook 机制（中间件体系）。
 */
public interface AgentHook {

    /** LLM 调用前：可改写请求（如注入上下文、压缩记忆） */
    default ChatRequest beforeLLM(ChatRequest request, ChatContext context) {
        return request;
    }

    /** LLM 调用后：可观测响应、记录埋点 */
    default void afterLLM(ChatRequest request, ChatResponse response, ChatContext context) {
    }

    /** 工具调用前：可拦截、改写参数（如权限校验、租户过滤） */
    default Map<String, Object> beforeTool(ToolCall toolCall, Map<String, Object> arguments, ChatContext context) {
        return arguments;
    }

    /** 工具调用后：可观测结果 */
    default void afterTool(ToolCall toolCall, String result, ChatContext context) {
    }

    /** 对话结束：统一收尾 */
    default void onFinish(ChatContext context, String finalAnswer) {
    }
}
