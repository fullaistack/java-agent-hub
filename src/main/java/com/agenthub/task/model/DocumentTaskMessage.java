package com.agenthub.task.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 异步任务消息体：MQ 传输与本地事件共用。
 * tenantId 随消息传递，消费者据此恢复租户上下文。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTaskMessage {

    /** 任务唯一ID（幂等键） */
    private String taskId;

    /** 租户ID */
    private Long tenantId;

    /** 知识库ID */
    private Long kbId;

    /** 文档ID */
    private Long docId;

    /** 任务类型：parse / embed */
    private String type;
}
