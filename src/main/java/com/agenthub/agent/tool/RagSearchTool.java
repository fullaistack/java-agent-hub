package com.agenthub.agent.tool;

import com.agenthub.common.util.JsonUtils;
import com.agenthub.rag.model.RetrievalResult;
import com.agenthub.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * RAG 检索工具：供 Agent 按需调用，检索知识库并把结果交给 LLM 整理。
 *
 * <p>输入：query（检索问题）、kbId（知识库ID，可空表示全部）、topK（返回条数）；
 * 输出：JSON 数组（片段 + 来源文档名），由 Agent 系统提示词引导 LLM 标注来源。</p>
 */
@Component
@RequiredArgsConstructor
public class RagSearchTool implements AgentTool {

    public static final String NAME = "rag_search";

    private final RagService ragService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "检索企业知识库。当用户问题需要基于内部文档/知识库内容回答时调用。" +
                "参数：query(必填，检索问题)；kbId(可选，知识库ID)；topK(可选，返回条数，默认5)。";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of("type", "string", "description", "检索问题"),
                        "kbId", Map.of("type", "integer", "description", "知识库ID，不传则检索全部知识库"),
                        "topK", Map.of("type", "integer", "description", "返回条数，默认5")),
                "required", List.of("query"));
    }

    @Override
    public String call(Map<String, Object> arguments) {
        String query = String.valueOf(arguments.getOrDefault("query", ""));
        Long kbId = toLong(arguments.get("kbId"));
        int topK = arguments.get("topK") instanceof Number n ? n.intValue() : 5;

        List<Long> kbIds = kbId == null ? null : List.of(kbId);
        List<RetrievalResult> results = ragService.search(kbIds, query, topK, null, Map.of());
        if (results.isEmpty()) {
            return "知识库中没有检索到相关内容。";
        }
        List<Map<String, Object>> items = results.stream()
                .map(r -> Map.<String, Object>of(
                        "content", r.getContent(),
                        "source", r.getDocName()))
                .toList();
        return JsonUtils.toJson(items);
    }

    private Long toLong(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }
}
