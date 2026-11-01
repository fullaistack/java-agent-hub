package com.agenthub.task.consumer;

import com.agenthub.task.event.TaskEvent;
import com.agenthub.task.executor.DocumentTaskExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 本地任务消费者：mq.type=local 时生效。
 * 监听进程内 {@link TaskEvent}，在任务线程池异步执行（@Async 与 @EventListener 组合），
 * 上传接口立即返回，异步线程透传租户上下文。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agenthub.mq.type", havingValue = "local", matchIfMissing = true)
public class LocalTaskConsumer {

    private final DocumentTaskExecutor executor;

    @Async("taskExecutor")
    @EventListener
    public void onTaskEvent(TaskEvent event) {
        log.info("本地任务事件: taskId={}, type={}, docId={}",
                event.getMessage().getTaskId(), event.getMessage().getType(), event.getMessage().getDocId());
        executor.execute(event.getMessage());
    }
}
