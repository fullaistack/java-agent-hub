package com.agenthub.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户用户。
 */
@Data
@TableName("tenant_user")
public class TenantUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String username;

    /** 密码哈希（演示用 SHA-256，生产环境请换 BCrypt） */
    private String passwordHash;

    private String displayName;

    /** 角色：OWNER / ADMIN / MEMBER */
    private String role;

    /** 状态：1启用 0停用 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
