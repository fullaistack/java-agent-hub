package com.agenthub.knowledge.enums;

/**
 * 文档处理流水线状态机。
 *
 * <pre>
 * UPLOADED →(MQ: 解析任务)→ PARSING → PARSED →(MQ: 向量化任务)→ EMBEDDING → READY
 * 任一步骤异常 → FAILED（可通过重新提交任务重试）
 * </pre>
 */
public enum DocumentStatus {

    /** 已上传，等待解析 */
    UPLOADED,
    /** 解析中 */
    PARSING,
    /** 解析完成，等待向量化 */
    PARSED,
    /** 向量化入库中 */
    EMBEDDING,
    /** 全流程完成，可检索 */
    READY,
    /** 处理失败 */
    FAILED
}
