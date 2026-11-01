package com.agenthub.gateway.config;

import com.agenthub.tenant.context.TenantContext;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 异步线程配置：自定义 TaskDecorator 把主线程的租户上下文与 MDC 透传到异步线程，
 * 保证 MQ 消费、@Async 场景下的租户隔离与日志链路不丢失。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("agenthub-task-");
        executor.setTaskDecorator(new TenantTaskDecorator());
        executor.initialize();
        return executor;
    }

    /** 租户上下文透传装饰器 */
    static class TenantTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // 提交任务时快照主线程上下文
            TenantContext.TenantIdentity identity = snapshotIdentity();
            Map<String, String> mdc = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (identity != null) {
                        TenantContext.set(identity.tenantId(), identity.userId(), identity.userName());
                    }
                    if (mdc != null) {
                        MDC.setContextMap(mdc);
                    }
                    runnable.run();
                } finally {
                    TenantContext.clear();
                    MDC.clear();
                }
            };
        }

        private TenantContext.TenantIdentity snapshotIdentity() {
            Long tenantId = TenantContext.getTenantId();
            return tenantId == null ? null
                    : new TenantContext.TenantIdentity(tenantId, TenantContext.getUserId(), TenantContext.getUserName());
        }
    }
}
