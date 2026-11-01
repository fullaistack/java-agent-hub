package com.agenthub.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户：数据隔离的顶层边界。
 * 注意：本表没有 tenant_id 字段，已在租户插件 ignoreTable 中排除，避免被自动加租户条件。
 */
@Data
@TableName("tenant")
public class Tenant {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户编码（唯一，登录时使用） */
    private String tenantCode;

    /** 租户名称 */
    private String tenantName;

    /** 状态：1启用 0停用 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
