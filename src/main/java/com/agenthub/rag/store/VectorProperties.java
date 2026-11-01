package com.agenthub.rag.store;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PGVector 配置：独立于 MySQL 主库的向量数据源。
 */
@Data
@ConfigurationProperties(prefix = "agenthub.vector")
public class VectorProperties {

    private DataSourceConfig datasource = new DataSourceConfig();

    /** 向量维度：建表与写入校验使用 */
    private int dimension = 1024;

    @Data
    public static class DataSourceConfig {
        private String jdbcUrl = "jdbc:postgresql://localhost:5432/agenthub_vector";
        private String username = "agenthub";
        private String password = "agenthub123";
    }
}
