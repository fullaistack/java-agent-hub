package com.agenthub.parser.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 解析模块配置。
 */
@Data
@ConfigurationProperties(prefix = "agenthub.parser")
public class ParserProperties {

    /** tika | mineru */
    private String provider = "tika";

    private MineruConfig mineru = new MineruConfig();

    @Data
    public static class MineruConfig {
        /** MinerU WebAPI 地址（MinerU 1.x 内置 FastAPI 服务） */
        private String url = "http://localhost:30000";
    }
}
