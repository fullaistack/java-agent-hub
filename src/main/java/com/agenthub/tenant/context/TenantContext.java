package com.agenthub.tenant.context;

import com.agenthub.common.exception.BizException;
import com.agenthub.common.exception.ErrorCode;

/**
 * 租户上下文：基于 ThreadLocal 保存当前请求的租户身份。
 *
 * <p>由 {@code TenantInterceptor} 在请求进入时设置、请求结束时清理；
 * 异步线程通过 {@code AsyncConfig} 的 TaskDecorator 自动透传。</p>
 */
public final class TenantContext {

    private static final ThreadLocal<TenantIdentity> HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    /** 设置当前租户身份 */
    public static void set(Long tenantId, Long userId, String userName) {
        HOLDER.set(new TenantIdentity(tenantId, userId, userName));
    }

    /** 获取当前租户ID，缺失时抛出业务异常（保证所有查询强制带租户） */
    public static Long requireTenantId() {
        TenantIdentity identity = HOLDER.get();
        if (identity == null || identity.tenantId() == null) {
            throw new BizException(ErrorCode.TENANT_ERROR);
        }
        return identity.tenantId();
    }

    /** 获取当前租户ID，可能为 null（如登录接口、MQ 消费者手动设置前） */
    public static Long getTenantId() {
        TenantIdentity identity = HOLDER.get();
        return identity == null ? null : identity.tenantId();
    }

    /** 获取当前用户ID */
    public static Long getUserId() {
        TenantIdentity identity = HOLDER.get();
        return identity == null ? null : identity.userId();
    }

    /** 获取当前用户名 */
    public static String getUserName() {
        TenantIdentity identity = HOLDER.get();
        return identity == null ? null : identity.userName();
    }

    /** 清理上下文（必须配合 try/finally 使用，防止线程池复用导致租户串号） */
    public static void clear() {
        HOLDER.remove();
    }

    /** 租户身份快照 */
    public record TenantIdentity(Long tenantId, Long userId, String userName) {
    }
}
