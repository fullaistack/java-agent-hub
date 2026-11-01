package com.agenthub.llm.provider;

import com.agenthub.llm.model.ChatRequest;
import com.agenthub.llm.model.ChatResponse;

/**
 * LLM Provider 抽象：openai（兼容接口）/ ollama / mock。
 */
public interface LLMProvider {

    /** provider 类型 */
    String type();

    /** 发起对话（含 function calling） */
    ChatResponse chat(ChatRequest request);
}
