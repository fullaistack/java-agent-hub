package com.agenthub.tenant.mapper;

import com.agenthub.tenant.entity.Tenant;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户表 Mapper。
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
