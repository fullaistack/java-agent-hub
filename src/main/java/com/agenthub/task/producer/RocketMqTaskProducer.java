package com.agenthub.task.producer;

import com.agenthub.task.model.DocumentTaskMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * RocketMQ 任务生产者：mq.type=rocketmq 时生效。
 * 解析任务与向量化任务分别投递到独立 Topic，支持按 Topic 扩容消费。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agenthub.mq.type", havingValue = "rocketmq")
public class RocketMqTaskProducer implements TaskProducer {

    private final RocketMQTemplate rocketMQTemplate;

    @Value("${agenthub.mq.topics.doc-parse}")
    private String parseTopic;

    @Value("${agenthub.mq.topics.chunk-embed}")
    private String embedTopic;

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
        String topic = "parse".equals(message.getType()) ? parseTopic : embedTopic;
        // taskId 作为消息 Key，保证 RocketMQ 幂等与顺序消费
        rocketMQTemplate.syncSend(topic, message, 5000);
        log.info("任务已投递: topic={}, taskId={}, type={}, docId={}",
                topic, message.getTaskId(), message.getType(), message.getDocId());
    }
}
