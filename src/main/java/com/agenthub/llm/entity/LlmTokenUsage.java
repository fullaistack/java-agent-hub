package com.agenthub.llm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * LLM token 消耗记录：每次模型调用落一条，支撑用量统计与成本核算。
 */
@Data
@TableName("llm_token_usage")
public class LlmTokenUsage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 会话ID（Agent 场景），REST 直接调用可为空 */
    private Long conversationId;

    /** 引擎模型名 */
    private String model;

    /** provider 类型：openai / ollama / mock */
    private String provider;

    private Integer promptTokens;

    private Integer completionTokens;

    /** 调用耗时（毫秒） */
    private Long durationMs;

    /** SUCCESS / ERROR */
    private String status;

    private String errorMsg;

    private LocalDateTime createdAt;
}
