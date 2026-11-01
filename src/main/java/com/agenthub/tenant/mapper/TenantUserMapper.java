package com.agenthub.tenant.mapper;

import com.agenthub.tenant.entity.TenantUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户用户 Mapper（查询自动携带 tenant_id 条件）。
 */
@Mapper
public interface TenantUserMapper extends BaseMapper<TenantUser> {
}
