package com.agenthub.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * API Key 创建响应：明文仅在创建时返回一次。
 */
@Data
@AllArgsConstructor
public class ApiKeyResponse {

    private String name;
    private String apiKey;
    private String prefix;
}
