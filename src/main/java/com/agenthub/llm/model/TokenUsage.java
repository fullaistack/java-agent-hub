package com.agenthub.llm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * token 消耗统计。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsage {

    private int promptTokens;
    private int completionTokens;

    public int total() {
        return promptTokens + completionTokens;
    }
}
