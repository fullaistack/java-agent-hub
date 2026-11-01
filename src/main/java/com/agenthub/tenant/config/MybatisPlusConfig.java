package com.agenthub.tenant.config;

import com.agenthub.tenant.context.TenantContext;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * MyBatis-Plus 多租户配置。
 *
 * <p>核心机制：{@link TenantLineInnerInterceptor} 在 SQL 执行前自动为所有 DML 追加
 * {@code tenant_id = 当前租户} 条件，业务代码无需手写租户过滤，从根源上杜绝跨租户查询。</p>
 *
 * <p>注意：向量检索走 PGVector（JDBC 直连），不经过该插件，由 PgVectorStore 显式拼 tenant_id 条件。</p>
 */
@Configuration
public class MybatisPlusConfig {

    /** 不含 tenant_id 字段、不需要租户隔离的表 */
    private static final Set<String> IGNORE_TABLES = Set.of("tenant");

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1. 租户隔离插件（必须放在分页插件之前）
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContext.getTenantId();
                // 上下文为空（如登录接口、定时任务）时不追加租户条件
                return tenantId == null ? null : new LongValue(tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return IGNORE_TABLES.contains(tableName);
            }
        }));
        // 2. 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
