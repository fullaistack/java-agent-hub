package com.agenthub.knowledge.controller;

import com.agenthub.common.api.ApiResponse;
import com.agenthub.knowledge.dto.KnowledgeBaseRequest;
import com.agenthub.knowledge.entity.KnowledgeBase;
import com.agenthub.knowledge.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识库管理接口。
 */
@RestController
@RequestMapping("/api/v1/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService kbService;

    @PostMapping
    public ApiResponse<KnowledgeBase> create(@Valid @RequestBody KnowledgeBaseRequest request) {
        return ApiResponse.ok(kbService.create(request));
    }

    @GetMapping
    public ApiResponse<List<KnowledgeBase>> list() {
        return ApiResponse.ok(kbService.list());
    }

    @DeleteMapping("/{kbId}")
    public ApiResponse<Void> delete(@PathVariable Long kbId) {
        kbService.delete(kbId);
        return ApiResponse.ok();
    }
}
