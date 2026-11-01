package com.agenthub.common.constant;

/**
 * 平台通用常量。
 */
public final class HeaderConstants {

    /** 租户ID请求头 */
    public static final String TENANT_ID = "X-Tenant-Id";

    /** API Key请求头 */
    public static final String API_KEY = "X-Api-Key";

    /** 链路追踪ID */
    public static final String TRACE_ID = "X-Trace-Id";

    private HeaderConstants() {
    }
}
