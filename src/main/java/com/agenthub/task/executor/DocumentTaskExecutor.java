package com.agenthub.task.executor;

import com.agenthub.knowledge.entity.Document;
import com.agenthub.knowledge.enums.DocumentStatus;
import com.agenthub.knowledge.service.DocumentService;
import com.agenthub.knowledge.storage.StorageService;
import com.agenthub.parser.ParserService;
import com.agenthub.parser.model.ParsedDocument;
import com.agenthub.rag.embedding.EmbeddingRouter;
import com.agenthub.rag.model.Chunk;
import com.agenthub.rag.splitter.TextSplitter;
import com.agenthub.rag.store.VectorStore;
import com.agenthub.task.model.DocumentTaskMessage;
import com.agenthub.task.producer.TaskProducer;
import com.agenthub.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文档任务执行器：解析与向量化的核心流水线，供 MQ 消费者（RocketMQ / 本地事件）调用。
 *
 * <pre>
 * parse 任务：PARSING → 下载原始文件 → 解析（MinerU/Tika）→ 保存文本 → PARSED → 投递 embed 任务
 * embed 任务：EMBEDDING → 读取文本 → 分块 → 向量化 → 清旧向量 → 批量入库 → READY
 * </pre>
 *
 * <p>幂等与失败处理：执行前清空该文档旧向量；任何异常置 FAILED 并记录错误信息，
 * 可通过重新提交任务重试。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentTaskExecutor {

    private final DocumentService documentService;
    private final StorageService storageService;
    private final ParserService parserService;
    private final TextSplitter textSplitter;
    private final EmbeddingRouter embeddingRouter;
    private final VectorStore vectorStore;
    private final TaskProducer taskProducer;

    /** 任务分发：按消息类型执行 parse 或 embed */
    public void execute(DocumentTaskMessage message) {
        // 恢复租户上下文：MQ 消息携带 tenantId，消费线程据此建立隔离边界
        TenantContext.set(message.getTenantId(), null, null);
        try {
            switch (message.getType()) {
                case "parse" -> doParse(message);
                case "embed" -> doEmbed(message);
                default -> log.warn("未知任务类型: {}", message.getType());
            }
        } finally {
            TenantContext.clear();
        }
    }

    /** 解析任务：下载 → 解析 → 存文本 → 触发向量化 */
    private void doParse(DocumentTaskMessage message) {
        Document doc = documentService.require(message.getDocId());
        documentService.updateStatus(doc.getId(), DocumentStatus.PARSING, null);
        try {
            InputStream in = storageService.load(doc.getStoragePath());
            ParsedDocument parsed = parserService.parse(doc.getFileName(), in);
            // 保存解析文本 + 解析引擎 + 状态
            documentService.updateParsed(doc.getId(), parsed.getText(), parsed.getParseProvider(), DocumentStatus.PARSED);
            // 触发下一阶段：分块向量化（异步）
            taskProducer.sendEmbedTask(message.getTenantId(), message.getKbId(), doc.getId());
            log.info("文档解析完成: docId={}, provider={}, chars={}",
                    doc.getId(), parsed.getParseProvider(), parsed.getText().length());
        } catch (Exception e) {
            log.error("文档解析失败: docId={}", doc.getId(), e);
            documentService.updateStatus(doc.getId(), DocumentStatus.FAILED, e.getMessage());
        }
    }

    /** 向量化任务：分块 → 向量化 → 批量入库 */
    private void doEmbed(DocumentTaskMessage message) {
        Document doc = documentService.require(message.getDocId());
        documentService.updateStatus(doc.getId(), DocumentStatus.EMBEDDING, null);
        try {
            if (doc.getParsedText() == null || doc.getParsedText().isBlank()) {
                throw new IllegalStateException("文档解析文本为空");
            }
            // 1. 分块
            List<String> texts = textSplitter.split(doc.getParsedText());
            if (texts.isEmpty()) {
                throw new IllegalStateException("分块结果为空");
            }
            // 2. 幂等清理旧向量
            vectorStore.deleteByDoc(message.getTenantId(), doc.getId());
            // 3. 批量向量化并组装块
            List<float[]> embeddings = embeddingRouter.route(null).embed(texts);
            List<Chunk> chunks = new ArrayList<>(texts.size());
            for (int i = 0; i < texts.size(); i++) {
                chunks.add(Chunk.builder()
                        .tenantId(message.getTenantId())
                        .kbId(message.getKbId())
                        .docId(doc.getId())
                        .docName(doc.getFileName())
                        .chunkIndex(i)
                        .content(texts.get(i))
                        .embedding(embeddings.get(i))
                        .metadata(Map.of("kbId", message.getKbId()))
                        .build());
            }
            // 4. 入库
            vectorStore.batchInsert(chunks);
            documentService.updateEmbedded(doc.getId(), chunks.size(), DocumentStatus.READY, null);
            log.info("文档向量化完成: docId={}, chunks={}", doc.getId(), chunks.size());
        } catch (Exception e) {
            log.error("文档向量化失败: docId={}", doc.getId(), e);
            documentService.updateStatus(doc.getId(), DocumentStatus.FAILED, e.getMessage());
        }
    }
}
