package com.agenthub.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Agent 配置。
 */
@Data
@ConfigurationProperties(prefix = "agenthub.agent")
public class AgentProperties {

    /** builtin（内置 ReAct 引擎）| agentscope（AgentScope HarnessAgent） */
    private String engine = "builtin";

    /** 单轮最大工具调用迭代次数 */
    private int maxIterations = 5;

    /** 多轮记忆窗口（最近 N 条消息） */
    private int memoryWindow = 10;

    private AgentScopeConfig agentscope = new AgentScopeConfig();

    @Data
    public static class AgentScopeConfig {
        /** AgentScope 模型 URI：{provider}:{model} */
        private String defaultModel = "ollama:qwen2.5:7b";
        /** Workspace 目录（Harness 的工程化资产根目录） */
        private String workspace = ".agentscope/workspace";
    }
}
