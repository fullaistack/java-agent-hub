package com.agenthub.parser;

import com.agenthub.parser.model.ParsedDocument;

import java.io.InputStream;

/**
 * 文档解析抽象：支持多引擎（MinerU / Tika），通过 agenthub.parser.provider 切换。
 */
public interface ParserClient {

    /** 引擎名称 */
    String provider();

    /**
     * 解析文档为纯文本。
     *
     * @param fileName 原始文件名（用于类型识别）
     * @param in       文件流（调用方负责关闭）
     */
    ParsedDocument parse(String fileName, InputStream in);
}
