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
 * RocketMQ 文档解析任务消费者：mq.type=rocketmq 时生效。
 *
 * <p>消费 doc-parse Topic，失败消息进入重试队列（RocketMQ 默认 16 次重试，
 * 最终进入 DLQ），配合任务幂等设计（重试前清理旧数据）保证最终一致。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agenthub.mq.type", havingValue = "rocketmq")
@RocketMQMessageListener(topic = "${agenthub.mq.topics.doc-parse}",
        consumerGroup = "agenthub-doc-parse-consumer")
public class RocketMqParseConsumer implements RocketMQListener<DocumentTaskMessage> {

    private final DocumentTaskExecutor executor;

    @Override
    public void onMessage(DocumentTaskMessage message) {
        log.info("收到解析任务: taskId={}, docId={}", message.getTaskId(), message.getDocId());
        executor.execute(message);
    }
}
