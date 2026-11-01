package com.agenthub.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库成员：租户内用户对知识库的访问权限。
 */
@Data
@TableName("kb_member")
public class KbMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long kbId;

    private Long tenantId;

    private Long userId;

    /** 角色：OWNER / EDITOR / VIEWER */
    private String role;

    private LocalDateTime createdAt;
}
