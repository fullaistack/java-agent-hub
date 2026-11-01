package com.agenthub.rag.store;

import com.agenthub.common.exception.BizException;
import com.agenthub.common.exception.ErrorCode;
import com.agenthub.rag.model.Chunk;
import com.agenthub.rag.model.ScoredChunk;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * PGVector 存储实现：JDBC 直连 PostgreSQL + pgvector 扩展。
 *
 * <p>与 MySQL 业务库分离：MySQL 走 MyBatis-Plus（自动租户过滤），
 * 向量检索走本类原生 SQL，tenant_id 条件必须显式拼入——两条链路都强制租户隔离。</p>
 *
 * <p>索引说明：HNSW 索引在 init SQL 中创建（embedding vector_cosine_ops），
 * 检索使用 {@code <=>} 余弦距离算子，相似度 = 1 - 距离。</p>
 */
@Slf4j
@Component
public class PgVectorStore implements VectorStore {

    private final JdbcTemplate jdbc;

    public PgVectorStore(VectorProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getDatasource().getJdbcUrl());
        config.setUsername(properties.getDatasource().getUsername());
        config.setPassword(properties.getDatasource().getPassword());
        config.setMaximumPoolSize(10);
        config.setPoolName("pgvector-pool");
        this.jdbc = new JdbcTemplate(new HikariDataSource(config));
        ensureSchema(properties.getDimension());
    }

    /** 幂等建表（docker init 脚本已建表，这里兜底） */
    private void ensureSchema(int dimension) {
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rag_chunk (
                    id BIGSERIAL PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    kb_id BIGINT NOT NULL,
                    doc_id BIGINT NOT NULL,
                    doc_name VARCHAR(255),
                    chunk_index INT NOT NULL DEFAULT 0,
                    content TEXT NOT NULL,
                    embedding vector(%d),
                    metadata JSONB DEFAULT '{}',
                    created_at TIMESTAMP DEFAULT now()
                )
                """.formatted(dimension));
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunk_tenant_kb ON rag_chunk (tenant_id, kb_id)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunk_embedding ON rag_chunk USING hnsw (embedding vector_cosine_ops)");
    }

    @Override
    public void batchInsert(List<Chunk> chunks) {
        if (chunks.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO rag_chunk (tenant_id, kb_id, doc_id, doc_name, chunk_index, content, embedding, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?::vector, ?::jsonb)
                """;
        jdbc.batchUpdate(sql, chunks, chunks.size(), (PreparedStatement ps, Chunk c) -> {
            ps.setLong(1, c.getTenantId());
            ps.setLong(2, c.getKbId());
            ps.setLong(3, c.getDocId());
            ps.setString(4, c.getDocName());
            ps.setInt(5, c.getChunkIndex());
            ps.setString(6, c.getContent());
            ps.setString(7, toVectorString(c.getEmbedding()));
            ps.setString(8, metadataToJson(c.getMetadata()));
        });
    }

    @Override
    public List<ScoredChunk> vectorSearch(Long tenantId, List<Long> kbIds, float[] embedding, int topK) {
        String kbCondition = inClause("kb_id", kbIds.size());
        String sql = """
                SELECT id, content, doc_name, chunk_index, metadata,
                       1 - (embedding <=> ?::vector) AS score
                FROM rag_chunk
                WHERE tenant_id = ? AND %s
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """.formatted(kbCondition);
        List<Object> args = new ArrayList<>();
        args.add(toVectorString(embedding));
        args.add(tenantId);
        args.addAll(kbIds);
        args.add(toVectorString(embedding));
        args.add(topK);
        return jdbc.query(sql, this::mapScoredChunk, args.toArray());
    }

    @Override
    public List<ScoredChunk> keywordSearch(Long tenantId, List<Long> kbIds, String query, int topK) {
        // 简单分词：英文按空白/标点切词，中文保留原词整体匹配，最多取 5 个词
        List<String> terms = tokenize(query);
        if (terms.isEmpty()) {
            return List.of();
        }
        String kbCondition = inClause("kb_id", kbIds.size());
        // 每个词命中一次计 1 分，按总分排序
        StringJoiner scoreCase = new StringJoiner(" + ", "(", ") AS score");
        StringJoiner whereLike = new StringJoiner(" OR ");
        // 参数顺序与 SQL 一致：score 参数 → tenant_id → kb_ids → where 参数 → limit
        List<Object> args = new ArrayList<>();
        for (String term : terms) {
            scoreCase.add("CASE WHEN content ILIKE ? THEN 1 ELSE 0 END");
            args.add("%" + term + "%");
        }
        for (String term : terms) {
            whereLike.add("content ILIKE ?");
        }
        args.add(tenantId);
        args.addAll(kbIds);
        for (String term : terms) {
            args.add("%" + term + "%");
        }
        String sql = """
                SELECT id, content, doc_name, chunk_index, metadata, %s
                FROM rag_chunk
                WHERE tenant_id = ? AND %s AND (%s)
                ORDER BY score DESC
                LIMIT ?
                """.formatted(scoreCase, kbCondition, whereLike);
        args.add(topK);
        return jdbc.query(sql, this::mapScoredChunk, args.toArray());
    }

    @Override
    public void deleteByDoc(Long tenantId, Long docId) {
        jdbc.update("DELETE FROM rag_chunk WHERE tenant_id = ? AND doc_id = ?", tenantId, docId);
    }

    @Override
    public void deleteByKb(Long tenantId, Long kbId) {
        jdbc.update("DELETE FROM rag_chunk WHERE tenant_id = ? AND kb_id = ?", tenantId, kbId);
    }

    // ---------- 内部工具 ----------

    private ScoredChunk mapScoredChunk(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        ScoredChunk chunk = new ScoredChunk();
        chunk.setId(rs.getLong("id"));
        chunk.setContent(rs.getString("content"));
        chunk.setDocName(rs.getString("doc_name"));
        chunk.setChunkIndex(rs.getInt("chunk_index"));
        chunk.setScore(rs.getDouble("score"));
        String metadata = rs.getString("metadata");
        chunk.setMetadata(metadata == null ? Map.of() : com.agenthub.common.util.JsonUtils.parseMap(metadata));
        return chunk;
    }

    /** float[] → PGVector 字符串 "[0.1,0.2,...]" */
    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }

    private String metadataToJson(Map<String, Object> metadata) {
        return metadata == null || metadata.isEmpty()
                ? "{}"
                : com.agenthub.common.util.JsonUtils.toJson(metadata);
    }

    /** 生成 IN (?, ?, ...) 占位符 */
    private String inClause(String column, int count) {
        return column + " IN (" + java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> "?").collect(Collectors.joining(",")) + ")";
    }

    /** 关键字分词：英文词 + 中文连续串 */
    private List<String> tokenize(String query) {
        List<String> terms = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (char c : query.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                buf.append(c);
            } else {
                addTerm(terms, buf);
            }
        }
        addTerm(terms, buf);
        return terms.stream().filter(t -> t.length() >= 2).limit(5).collect(Collectors.toList());
    }

    private void addTerm(List<String> terms, StringBuilder buf) {
        if (buf.isEmpty()) {
            return;
        }
        String term = buf.toString();
        // 中文串长度超过 4 时切分为 2-gram，提升命中率
        if (term.length() > 4 && term.chars().allMatch(c -> c > 127)) {
            for (int i = 0; i + 2 <= term.length(); i++) {
                terms.add(term.substring(i, i + 2));
            }
        } else {
            terms.add(term);
        }
        buf.setLength(0);
    }
}
