package com.agenthub.agent.agentscope;

import com.agenthub.agent.config.AgentProperties;
import com.agenthub.agent.harness.ChatContext;
import com.agenthub.agent.tool.ToolRegistry;
import com.agenthub.common.exception.BizException;
import com.agenthub.common.exception.ErrorCode;
import io.agentscope.core.message.Msg;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AgentScope 2.0 HarnessAgent 适配层。
 *
 * <p>把 AgentScope 官方 Harness 编排引擎接入平台：
 * 1. 按配置的模型 URI（{@code {provider}:{model}}）构建 HarnessAgent；
 * 2. 把 {@link ToolRegistry} 中的平台工具（RAG / MCP）桥接为 AgentScope Toolkit；
 * 3. 通过 {@code agent.call(userInput)} 执行对话，阻塞等待结果。</p>
 *
 * <p>与内置引擎（builtin）的差异：AgentScope Harness 自带工程化中间件体系
 * （记忆维护、Plan Mode、Skill 等），适合进阶编排场景；builtin 引擎零外部
 * 依赖即可跑通全链路，两者通过 {@code agenthub.agent.engine} 切换。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentScopeAdapter {

    private final AgentProperties agentProperties;
    private final ToolRegistry toolRegistry;

    /** HarnessAgent 单例（懒加载：首次 agentscope 引擎调用时构建） */
    private volatile HarnessAgent agent;

    /**
     * 执行对话。
     *
     * @param context      本次对话上下文
     * @param systemPrompt 系统提示词
     * @param userInput    用户输入
     */
    public String run(ChatContext context, String systemPrompt, String userInput) {
        try {
            HarnessAgent harnessAgent = agent();
            Msg reply = harnessAgent.call(userInput)
                    .block(Duration.ofSeconds(120));
            if (reply == null) {
                throw new BizException(ErrorCode.BUSINESS_ERROR, "AgentScope 引擎执行超时");
            }
            String answer = reply.getTextContent();
            context.appendTrace("agentscope-harness");
            return answer;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("AgentScope 引擎执行失败", e);
            throw new BizException(ErrorCode.BUSINESS_ERROR, "AgentScope 引擎执行失败: " + e.getMessage());
        }
    }

    /**
     * 构建（或复用）HarnessAgent。
     * 构建时把 ToolRegistry 快照中的全部工具桥接注册进 Toolkit；
     * MCP 工具在运行期增删后，由 {@link #refresh()} 重建 Agent 生效。
     */
    private HarnessAgent agent() {
        HarnessAgent current = agent;
        if (current == null) {
            synchronized (this) {
                if (agent == null) {
                    agent = build();
                }
                current = agent;
            }
        }
        return current;
    }

    /**
     * 工具变更后重建 Agent（MCP 动态注册/注销后调用）。
     */
    public void refresh() {
        synchronized (this) {
            HarnessAgent old = agent;
            if (old != null) {
                old.close();
            }
            agent = build();
            log.info("HarnessAgent 已重建，工具数={}", toolRegistry.snapshot().size());
        }
    }

    private HarnessAgent build() {
        String modelUri = agentProperties.getAgentscope().getDefaultModel();
        // 工具桥接：RAG 检索工具 + MCP 远程工具统一注册
        Toolkit toolkit = new Toolkit();
        toolRegistry.snapshot().values()
                .forEach(t -> toolkit.registerAgentTool(new AgentScopeToolBridge(t)));
        log.info("构建 AgentScope HarnessAgent: model={}, tools={}", modelUri, toolkit.getToolNames());
        return HarnessAgent.builder()
                .name("agenthub")
                .description("企业级多租户 RAG + MCP 工具智能助手")
                .model(modelUri)
                .toolkit(toolkit)
                .maxIters(agentProperties.getMaxIterations())
                .build();
    }

    @PreDestroy
    public void destroy() {
        HarnessAgent current = agent;
        if (current != null) {
            current.close();
        }
    }
}
