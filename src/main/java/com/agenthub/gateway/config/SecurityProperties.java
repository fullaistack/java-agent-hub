package com.agenthub.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 安全配置：认证开关与免认证路径。
 */
@Data
@ConfigurationProperties(prefix = "agenthub.security")
public class SecurityProperties {

    /** 是否开启租户认证 */
    private boolean enabled = true;

    /** 免认证路径（Ant 风格） */
    private List<String> skipPaths = List.of();
}
