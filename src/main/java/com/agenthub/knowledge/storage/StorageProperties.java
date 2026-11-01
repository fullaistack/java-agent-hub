package com.agenthub.knowledge.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 存储配置：本地/对象存储两种模式。
 */
@Data
@ConfigurationProperties(prefix = "agenthub.storage")
public class StorageProperties {

    /** local | minio */
    private String type = "local";

    private LocalConfig local = new LocalConfig();

    private MinioConfig minio = new MinioConfig();

    @Data
    public static class LocalConfig {
        /** 本地存储根目录 */
        private String baseDir = "./data/files";
    }

    @Data
    public static class MinioConfig {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";
        private String bucket = "agenthub";
    }
}
