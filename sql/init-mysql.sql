-- =====================================================================
-- java-agent-hub MySQL 初始化脚本
-- 用途：建库建表 + 演示租户种子数据（docker-compose 首次启动自动执行）
-- 说明：所有业务表均含 tenant_id，由 MyBatis-Plus 租户插件自动追加过滤条件
-- =====================================================================

CREATE DATABASE IF NOT EXISTS agenthub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE agenthub;

-- ---------- 租户域 ----------

CREATE TABLE IF NOT EXISTS tenant (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_code VARCHAR(64)  NOT NULL UNIQUE COMMENT '租户编码',
    tenant_name VARCHAR(128) NOT NULL COMMENT '租户名称',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '租户';

CREATE TABLE IF NOT EXISTS tenant_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL COMMENT '租户ID',
    username      VARCHAR(64)  NOT NULL COMMENT '用户名',
    password_hash VARCHAR(128) NOT NULL COMMENT 'SHA-256密码哈希',
    display_name  VARCHAR(128) COMMENT '显示名',
    role          VARCHAR(32)  NOT NULL DEFAULT 'MEMBER' COMMENT 'OWNER/ADMIN/MEMBER',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user (tenant_id, username),
    KEY idx_user_tenant (tenant_id)
) COMMENT '租户用户';

CREATE TABLE IF NOT EXISTS tenant_api_key (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL COMMENT '租户ID',
    api_key_hash  VARCHAR(128) NOT NULL COMMENT 'API Key SHA-256哈希',
    api_key_prefix VARCHAR(16) COMMENT '明文前缀（用于识别）',
    name          VARCHAR(64)  COMMENT 'Key名称',
    enabled       TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    expires_at    DATETIME     COMMENT '过期时间',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_apikey_hash (api_key_hash)
) COMMENT '租户API Key';

-- ---------- 知识库域 ----------

CREATE TABLE IF NOT EXISTS knowledge_base (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL COMMENT '租户ID',
    name            VARCHAR(128) NOT NULL COMMENT '知识库名称',
    description     VARCHAR(512) COMMENT '描述',
    embedding_model VARCHAR(64)  COMMENT '向量化模型路由键',
    chunk_size      INT          NOT NULL DEFAULT 800 COMMENT '分块大小（字符）',
    chunk_overlap   INT          NOT NULL DEFAULT 100 COMMENT '分块重叠（字符）',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    created_by      BIGINT       COMMENT '创建人',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_kb_tenant (tenant_id)
) COMMENT '知识库';

CREATE TABLE IF NOT EXISTS kb_member (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    kb_id      BIGINT      NOT NULL COMMENT '知识库ID',
    tenant_id  BIGINT      NOT NULL COMMENT '租户ID',
    user_id    BIGINT      NOT NULL COMMENT '用户ID',
    role       VARCHAR(32) NOT NULL DEFAULT 'READER' COMMENT 'OWNER/EDITOR/READER',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_kb_member (kb_id, user_id)
) COMMENT '知识库成员（权限隔离）';

CREATE TABLE IF NOT EXISTS document (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    kb_id          BIGINT       NOT NULL COMMENT '知识库ID',
    tenant_id      BIGINT       NOT NULL COMMENT '租户ID',
    file_name      VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_type      VARCHAR(16)  COMMENT '扩展名',
    file_size      BIGINT       COMMENT '字节数',
    storage_path   VARCHAR(512) NOT NULL COMMENT '对象存储路径',
    status         VARCHAR(16)  NOT NULL DEFAULT 'UPLOADED' COMMENT 'UPLOADED/PARSING/PARSED/EMBEDDING/READY/FAILED',
    parse_provider VARCHAR(16)  COMMENT '解析引擎 tika/mineru',
    parsed_text    MEDIUMTEXT   COMMENT '解析后的纯文本',
    chunk_count    INT          NOT NULL DEFAULT 0 COMMENT '向量块数量',
    error_msg      VARCHAR(1024) COMMENT '失败原因',
    created_by     BIGINT       COMMENT '上传人',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_doc_kb (kb_id),
    KEY idx_doc_tenant (tenant_id)
) COMMENT '文档';

-- ---------- Agent 会话域 ----------

CREATE TABLE IF NOT EXISTS conversation (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL COMMENT '租户ID',
    user_id    BIGINT       COMMENT '用户ID',
    kb_id      BIGINT       COMMENT '绑定知识库',
    title      VARCHAR(255) COMMENT '会话标题',
    engine     VARCHAR(32)  NOT NULL DEFAULT 'builtin' COMMENT 'builtin/agentscope',
    model      VARCHAR(64)  COMMENT '模型路由键',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_conv_tenant (tenant_id),
    KEY idx_conv_user (user_id)
) COMMENT '会话';

CREATE TABLE IF NOT EXISTS conversation_message (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT      NOT NULL COMMENT '会话ID',
    tenant_id       BIGINT      NOT NULL COMMENT '租户ID',
    role            VARCHAR(16) NOT NULL COMMENT 'user/assistant/tool',
    content         MEDIUMTEXT  COMMENT '消息内容',
    tool_calls      TEXT        COMMENT '工具调用JSON',
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_msg_conv (conversation_id)
) COMMENT '会话消息';

-- ---------- 统计域 ----------

CREATE TABLE IF NOT EXISTS llm_token_usage (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id         BIGINT      NOT NULL COMMENT '租户ID',
    conversation_id   BIGINT      COMMENT '会话ID',
    model             VARCHAR(64) COMMENT '模型名',
    provider          VARCHAR(32) COMMENT '提供方 openai/ollama/mock',
    prompt_tokens     INT         NOT NULL DEFAULT 0,
    completion_tokens INT         NOT NULL DEFAULT 0,
    duration_ms       BIGINT      COMMENT '耗时毫秒',
    status            VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
    error_msg         VARCHAR(512),
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_token_tenant (tenant_id),
    KEY idx_token_conv (conversation_id)
) COMMENT 'LLM token消耗';

CREATE TABLE IF NOT EXISTS call_stat (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id         BIGINT      NOT NULL COMMENT '租户ID',
    user_id           BIGINT      COMMENT '用户ID',
    biz_type          VARCHAR(16) NOT NULL COMMENT 'llm/tool/rag/parse/agent',
    target            VARCHAR(255) COMMENT '模型名/工具名/查询词',
    success           TINYINT     NOT NULL DEFAULT 1,
    cost_ms           BIGINT      COMMENT '耗时毫秒',
    prompt_tokens     INT         DEFAULT 0,
    completion_tokens INT         DEFAULT 0,
    error_msg         VARCHAR(512),
    trace_id          VARCHAR(64) COMMENT 'OTel traceId',
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_stat_tenant (tenant_id),
    KEY idx_stat_type (biz_type),
    KEY idx_stat_time (created_at)
) COMMENT '全链路调用统计';

-- ---------- MCP 域 ----------

CREATE TABLE IF NOT EXISTS mcp_server_config (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id      BIGINT       NOT NULL COMMENT '租户ID',
    name           VARCHAR(64)  NOT NULL COMMENT '配置名（工具名前缀）',
    description    VARCHAR(512),
    transport_type VARCHAR(32)  NOT NULL COMMENT 'stdio/sse/streamable-http',
    base_url       VARCHAR(512) COMMENT 'SSE/HTTP地址',
    endpoint       VARCHAR(64)  DEFAULT '/mcp' COMMENT 'Streamable HTTP endpoint',
    command        VARCHAR(255) COMMENT 'STDIO启动命令',
    args_json      VARCHAR(1024) COMMENT 'STDIO参数JSON数组',
    env_json       VARCHAR(1024) COMMENT '环境变量JSON',
    enabled        TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_mcp_name (tenant_id, name)
) COMMENT 'MCP Server连接配置';

-- =====================================================================
-- 种子数据：演示租户 demo（租户ID=1）
-- 登录：tenantCode=demo, username=admin, password=admin123
-- API Key：ahk_demo1234567890abcdef（演示用，生产环境务必更换）
-- =====================================================================

INSERT INTO tenant (id, tenant_code, tenant_name, status) VALUES
(1, 'demo', '演示租户', 1);

-- admin123 的 SHA-256
INSERT INTO tenant_user (tenant_id, username, password_hash, display_name, role, status) VALUES
(1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '演示管理员', 'OWNER', 1);

-- ahk_demo1234567890abcdef 的 SHA-256（前缀 ahk_demo1234）
INSERT INTO tenant_api_key (tenant_id, api_key_hash, api_key_prefix, name, enabled) VALUES
(1, 'c4c6fa7a07369058cfe44af405eb67af0d4c0d72728d96d9ed0cdca3b19d0596', 'ahk_demo1234', 'demo默认Key', 1);

-- 演示知识库
INSERT INTO knowledge_base (id, tenant_id, name, description, chunk_size, chunk_overlap, status) VALUES
(1, 1, '企业产品手册', '演示知识库：产品介绍、使用手册等文档', 800, 100, 1);
