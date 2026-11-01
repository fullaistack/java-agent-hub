package com.agenthub.agent.mapper;

import com.agenthub.agent.entity.ConversationMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话消息 Mapper。
 */
@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessage> {
}
