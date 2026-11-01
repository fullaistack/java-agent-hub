package com.agenthub.agent.controller;

import com.agenthub.agent.dto.ChatRequestDTO;
import com.agenthub.agent.dto.ChatResponseDTO;
import com.agenthub.agent.service.AgentService;
import com.agenthub.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 对话接口：Harness 编排（内置 ReAct / AgentScope）的多轮对话入口。
 */
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /** 对话：首次不传 conversationId 自动创建会话，后续传入同一 ID 实现多轮记忆 */
    @PostMapping("/chat")
    public ApiResponse<ChatResponseDTO> chat(@Valid @RequestBody ChatRequestDTO request) {
        return ApiResponse.ok(agentService.chat(request));
    }
}
