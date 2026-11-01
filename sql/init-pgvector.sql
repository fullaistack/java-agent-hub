-- =====================================================================
-- java-agent-hub PostgreSQL(PGVector) 初始化脚本
-- 用途：向量库建表 + HNSW 索引（docker-compose 首次启动自动执行）
-- 说明：
--   1. rag_chunk 表与 PgVectorStore.ensureSchema 一致（幂等兜底）；
--   2. tenant_id 为向量数据的租户隔离维度，检索 SQL 显式带 tenant_id 条件；
--   3. 生产环境可在此基础上启用 PostgreSQL Row-Level-Security（RLS）双保险：
--      ALTER TABLE rag_chunk ENABLE ROW LEVEL SECURITY;
--      CREATE POLICY tenant_isolation ON rag_chunk
--          USING (tenant_id = current_setting('app.tenant_id')::bigint);
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS rag_chunk (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT      NOT NULL COMMENT '租户ID',
    kb_id       BIGINT      NOT NULL COMMENT '知识库ID',
    doc_id      BIGINT      NOT NULL COMMENT '文档ID',
    doc_name    VARCHAR(255) COMMENT '来源文档名',
    chunk_index INT         NOT NULL DEFAULT 0 COMMENT '块序号',
    content     TEXT        NOT NULL COMMENT '块内容',
    embedding   vector(1024) COMMENT '向量（维度与 embedding 模型一致）',
    metadata    JSONB       DEFAULT '{}' COMMENT '附加元数据',
    created_at  TIMESTAMP   DEFAULT now()
);

-- 租户+知识库联合索引：过滤性检索前置
CREATE INDEX IF NOT EXISTS idx_rag_chunk_tenant_kb ON rag_chunk (tenant_id, kb_id);
-- HNSW 向量索引：余弦相似度检索
CREATE INDEX IF NOT EXISTS idx_rag_chunk_embedding ON rag_chunk USING hnsw (embedding vector_cosine_ops);
