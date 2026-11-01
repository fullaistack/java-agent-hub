package com.agenthub.knowledge.mapper;

import com.agenthub.knowledge.entity.Document;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档 Mapper。
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}
