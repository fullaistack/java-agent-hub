package com.agenthub.task.consumer;

import com.agenthub.task.executor.DocumentTaskExecutor;
import com.agenthub.task.model.DocumentTaskMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 向量化任务消费者：mq.type=rocketmq 时生效。
 *
 * <p>消费 chunk-embed Topic。与解析消费者分离，便于向量化成为瓶颈时独立扩容。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agenthub.mq.type", havingValue = "rocketmq")
@RocketMQMessageListener(topic = "${agenthub.mq.topics.chunk-embed}",
        consumerGroup = "agenthub-chunk-embed-consumer")
public class RocketMqEmbedConsumer implements RocketMQListener<DocumentTaskMessage> {

    private final DocumentTaskExecutor executor;

    @Override
    public void onMessage(DocumentTaskMessage message) {
        log.info("收到向量化任务: taskId={}, docId={}", message.getTaskId(), message.getDocId());
        executor.execute(message);
    }
}
