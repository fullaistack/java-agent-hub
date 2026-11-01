package com.agenthub.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户 API Key：对外提供程序化访问凭证，服务端只保存哈希。
 */
@Data
@TableName("tenant_api_key")
public class TenantApiKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** API Key 的 SHA-256 哈希 */
    private String apiKeyHash;

    /** API Key 前缀（用于展示与快速定位） */
    private String apiKeyPrefix;

    /** Key 名称 */
    private String name;

    /** 是否启用 */
    private Integer enabled;

    /** 过期时间，null 表示永不过期 */
    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;
}
