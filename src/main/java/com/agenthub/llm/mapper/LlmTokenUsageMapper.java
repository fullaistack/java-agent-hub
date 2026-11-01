package com.agenthub.llm.mapper;

import com.agenthub.llm.entity.LlmTokenUsage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * token 用量 Mapper（tenant_id 自动隔离）。
 */
@Mapper
public interface LlmTokenUsageMapper extends BaseMapper<LlmTokenUsage> {
}
