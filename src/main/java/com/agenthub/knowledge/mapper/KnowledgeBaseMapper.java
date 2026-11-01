package com.agenthub.knowledge.mapper;

import com.agenthub.knowledge.entity.KnowledgeBase;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库 Mapper（租户插件自动追加 tenant_id 过滤）。
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {
}
