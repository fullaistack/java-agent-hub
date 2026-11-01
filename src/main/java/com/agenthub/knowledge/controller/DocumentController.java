package com.agenthub.knowledge.controller;

import com.agenthub.common.api.ApiResponse;
import com.agenthub.knowledge.entity.Document;
import com.agenthub.knowledge.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档管理接口：上传即返回，处理进度通过 GET 状态接口轮询。
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /** 上传文档到知识库（multipart/form-data，字段名 file） */
    @PostMapping("/kb/{kbId}/documents")
    public ApiResponse<Document> upload(@PathVariable Long kbId, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(documentService.upload(kbId, file));
    }

    /** 知识库文档列表 */
    @GetMapping("/kb/{kbId}/documents")
    public ApiResponse<List<Document>> list(@PathVariable Long kbId) {
        return ApiResponse.ok(documentService.list(kbId));
    }

    /** 文档状态查询 */
    @GetMapping("/documents/{docId}")
    public ApiResponse<Document> detail(@PathVariable Long docId) {
        return ApiResponse.ok(documentService.require(docId));
    }

    /** 失败重试 */
    @PostMapping("/documents/{docId}/retry")
    public ApiResponse<Void> retry(@PathVariable Long docId) {
        documentService.retry(docId);
        return ApiResponse.ok();
    }
}
