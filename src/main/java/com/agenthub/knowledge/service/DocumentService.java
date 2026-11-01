package com.agenthub.knowledge.service;

import com.agenthub.common.exception.BizException;
import com.agenthub.common.exception.ErrorCode;
import com.agenthub.knowledge.entity.Document;
import com.agenthub.knowledge.entity.KnowledgeBase;
import com.agenthub.knowledge.enums.DocumentStatus;
import com.agenthub.knowledge.mapper.DocumentMapper;
import com.agenthub.knowledge.storage.StorageService;
import com.agenthub.task.producer.TaskProducer;
import com.agenthub.tenant.context.TenantContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 文档服务：上传即返回（异步解析+向量化），避免长请求阻塞。
 *
 * <p>上传流程：存储原始文件 → 落库 document（UPLOADED）→ 提交 MQ 解析任务。
 * 后续解析/切分/向量化由 task 包消费者异步完成。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    /** 允许上传的文件类型（Tika 实际支持更多，这里做基本约束） */
    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "docx", "doc", "txt", "md", "pptx", "xlsx", "html");

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseService kbService;
    private final StorageService storageService;
    private final TaskProducer taskProducer;

    /** 上传文档：返回文档ID，处理进度通过状态字段轮询 */
    public Document upload(Long kbId, MultipartFile file) {
        KnowledgeBase kb = kbService.require(kbId);
        Long tenantId = TenantContext.requireTenantId();
        String fileName = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
        String ext = extensionOf(fileName);
        if (!ALLOWED_TYPES.contains(ext)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不支持的文件类型: ." + ext);
        }

        // 1. 存储原始文件：路径天然携带租户ID，物理隔离
        String objectKey = tenantId + "/" + kbId + "/" + UUID.randomUUID() + "." + ext;
        try {
            storageService.store(objectKey, file.getInputStream(), file.getContentType(), file.getSize());
        } catch (Exception e) {
            throw new BizException(ErrorCode.FILE_ERROR, "文件存储失败: " + e.getMessage());
        }

        // 2. 落库文档记录
        Document doc = new Document();
        doc.setKbId(kbId);
        doc.setTenantId(tenantId);
        doc.setFileName(fileName);
        doc.setFileType(ext);
        doc.setFileSize(file.getSize());
        doc.setStoragePath(objectKey);
        doc.setStatus(DocumentStatus.UPLOADED.name());
        doc.setChunkCount(0);
        doc.setCreatedBy(TenantContext.getUserId());
        documentMapper.insert(doc);

        // 3. 提交异步解析任务
        taskProducer.sendParseTask(tenantId, kbId, doc.getId());
        log.info("文档已上传: docId={}, kbId={}, file={}", doc.getId(), kbId, fileName);
        return doc;
    }

    /** 知识库下的文档列表 */
    public List<Document> list(Long kbId) {
        kbService.require(kbId);
        return documentMapper.selectList(new LambdaQueryWrapper<Document>()
                .eq(Document::getKbId, kbId)
                .orderByDesc(Document::getId));
    }

    /** 文档详情 */
    public Document require(Long docId) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "文档不存在: " + docId);
        }
        return doc;
    }

    /** 重新提交解析任务（FAILED 状态重试） */
    public void retry(Long docId) {
        Document doc = require(docId);
        doc.setStatus(DocumentStatus.UPLOADED.name());
        doc.setErrorMsg(null);
        documentMapper.updateById(doc);
        taskProducer.sendParseTask(doc.getTenantId(), doc.getKbId(), doc.getId());
    }

    /** 更新文档状态（供任务消费者调用） */
    public void updateStatus(Long docId, DocumentStatus status, String errorMsg) {
        Document doc = new Document();
        doc.setId(docId);
        doc.setStatus(status.name());
        doc.setErrorMsg(errorMsg);
        documentMapper.updateById(doc);
    }

    /** 保存解析结果并更新状态（供解析任务消费者调用） */
    public void updateParsed(Long docId, String parsedText, String parseProvider, DocumentStatus status) {
        Document doc = new Document();
        doc.setId(docId);
        doc.setParsedText(parsedText);
        doc.setParseProvider(parseProvider);
        doc.setStatus(status.name());
        documentMapper.updateById(doc);
    }

    /** 保存分块数量并更新状态（供向量化任务消费者调用） */
    public void updateEmbedded(Long docId, int chunkCount, DocumentStatus status, String errorMsg) {
        Document doc = new Document();
        doc.setId(docId);
        doc.setChunkCount(chunkCount);
        doc.setStatus(status.name());
        doc.setErrorMsg(errorMsg);
        documentMapper.updateById(doc);
    }

    private String extensionOf(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx < 0 ? "" : fileName.substring(idx + 1).toLowerCase();
    }
}
