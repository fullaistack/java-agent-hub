package com.agenthub.agent.prompt;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 默认提示词：从 classpath 加载，可在不改代码的情况下替换提示词文件。
 * 也保留内置兜底文本，确保文件缺失时应用仍可启动。
 */
@Component
public class Prompts {

    /** RAG 问答系统提示词模板（%s 依次为：参考文档上下文、用户问题） */
    public static final String RAG_SYSTEM_PROMPT_TEMPLATE = """
            你是专业知识库助手，基于检索到的文档内容回答用户问题。
            1. 只使用参考文档中的信息回答，如果文档没有相关内容，直接说明不知道，不要编造；
            2. 回答时标注引用来源文档名称；
            3. 语言简洁，条理清晰；
            4. 禁止幻觉，不要自己编造事实。

            参考文档内容：
            %s

            用户问题：%s
            """;

    /** Agent（Harness）系统提示词 */
    public static final String AGENT_SYSTEM_PROMPT = """
            你是可以调用工具的智能助手，拥有检索知识库、调用MCP工具的能力。
            1. 先判断用户问题是否需要检索知识库，需要则调用RAG检索工具；
            2. 判断是否需要调用MCP工具，按需调用；
            3. 拿到工具返回结果后再整理回答；
            4. 不要编造工具返回结果，不知道就如实告知用户；
            5. 回答尽量贴合企业内部业务场景，简洁专业。
            """;

    /** 从文件加载提示词（文件缺失时返回内置默认值） */
    public static String load(String path, String fallback) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (resource.exists()) {
                try (var in = resource.getInputStream()) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException ignored) {
        }
        return fallback;
    }

    /** 从文件加载 RAG 提示词模板 */
    public static String ragTemplate() {
        return load("prompts/rag-system-prompt.txt", RAG_SYSTEM_PROMPT_TEMPLATE);
    }

    /** 从文件加载 Agent 系统提示词 */
    public static String agentSystemPrompt() {
        return load("prompts/agent-system-prompt.txt", AGENT_SYSTEM_PROMPT);
    }
}
