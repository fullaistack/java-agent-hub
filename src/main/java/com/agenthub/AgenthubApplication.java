package com.agenthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * java-agent-hub 启动入口。
 *
 * <p>平台定位：企业级多租户 RAG + Harness Agent + MCP 工具平台。
 * 分层架构：接入层(gateway) → 业务服务层(tenant/knowledge) → Agent核心层(agent/rag/llm/mcp) → 插件层(parser/task) → 存储层(MySQL/PGVector/MinIO)。</p>
 */
@EnableAsync
@SpringBootApplication
@ConfigurationPropertiesScan
public class AgenthubApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgenthubApplication.class, args);
    }
}
