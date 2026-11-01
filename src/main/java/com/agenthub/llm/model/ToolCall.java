package com.agenthub.llm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 模型工具调用（function calling）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolCall {

    private String id;

    /** 工具名 */
    private String name;

    /** 工具参数 JSON */
    private String arguments;
}
