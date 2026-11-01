package com.agenthub.task.producer;

import com.agenthub.task.model.DocumentTaskMessage;

/**
 * 任务生产者抽象：rocketmq 模式发 MQ，local 模式发进程内事件。
 */
public interface TaskProducer {

    /** 提交文档解析任务 */
    void sendParseTask(Long tenantId, Long kbId, Long docId);

    /** 提交分块向量化任务 */
    void sendEmbedTask(Long tenantId, Long kbId, Long docId);

    /** 直接提交指定任务 */
    void send(DocumentTaskMessage message);
}
