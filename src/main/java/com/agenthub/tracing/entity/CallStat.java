package com.agenthub.tracing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 全链路调用统计：LLM 调用、工具调用、RAG 检索的耗时与 token 消耗记录。
 *
 * <p>写入路径：
 * - Agent 编排循环 → {@link com.agenthub.tracing.hook.TraceHook}（LLM/工具）
 * - 检索/解析链路 → {@link com.agenthub.tracing.aspect.TraceAspect}（RAG/解析）</p>
 */
@Data
@TableName("call_stat")
public class CallStat {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long userId;

    /** 业务类型：llm / tool / rag / parse / agent */
    private String bizType;

    /** 目标对象：模型名 / 工具名 / 知识库ID等 */
    private String target;

    /** 是否成功 */
    private Boolean success;

    /** 耗时（毫秒） */
    private Long costMs;

    /** 提示词 token */
    private Integer promptTokens;

    /** 生成 token */
    private Integer completionTokens;

    private String errMsg;

    /** 链路 traceId（与 OTel 关联） */
    private String traceId;

    private LocalDateTime createdAt;
}
