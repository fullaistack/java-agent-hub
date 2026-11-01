package com.agenthub.parser.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 解析结果：统一的数据模型，屏蔽 MinerU / Tika 的实现差异。
 */
@Data
@AllArgsConstructor
public class ParsedDocument {

    /** 原始文件名 */
    private String fileName;

    /** 解析出的纯文本（Markdown 文本） */
    private String text;

    /** 实际使用的解析引擎 */
    private String parseProvider;
}
