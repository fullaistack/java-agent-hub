package com.agenthub.task.event;

import com.agenthub.task.model.DocumentTaskMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 本地任务事件：mq.type=local 模式下替代 RocketMQ 消息的进程内载体。
 */
@Getter
@AllArgsConstructor
public class TaskEvent {

    private final DocumentTaskMessage message;
}
