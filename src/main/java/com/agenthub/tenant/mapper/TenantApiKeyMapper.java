package com.agenthub.tenant.mapper;

import com.agenthub.tenant.entity.TenantApiKey;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户 API Key Mapper。
 */
@Mapper
public interface TenantApiKeyMapper extends BaseMapper<TenantApiKey> {
}
