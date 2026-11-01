package com.agenthub.task.producer;

import com.agenthub.task.event.TaskEvent;
import com.agenthub.task.model.DocumentTaskMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 本地任务生产者：mq.type=local 时生效，通过 Spring 事件在进程内异步投递，
 * 使无 RocketMQ 环境下也能跑通「上传 → 解析 → 向量化」全链路。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agenthub.mq.type", havingValue = "local", matchIfMissing = true)
public class LocalTaskProducer implements TaskProducer {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void sendParseTask(Long tenantId, Long kbId, Long docId) {
        send(new DocumentTaskMessage(UUID.randomUUID().toString(), tenantId, kbId, docId, "parse"));
    }

    @Override
    public void sendEmbedTask(Long tenantId, Long kbId, Long docId) {
        send(new DocumentTaskMessage(UUID.randomUUID().toString(), tenantId, kbId, docId, "embed"));
    }

    @Override
    public void send(DocumentTaskMessage message) {
        eventPublisher.publishEvent(new TaskEvent(message));
    }
}
