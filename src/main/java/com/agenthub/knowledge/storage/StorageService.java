package com.agenthub.knowledge.storage;

import java.io.InputStream;

/**
 * 文件存储抽象：支持本地磁盘与 MinIO 两种实现，通过 agenthub.storage.type 切换。
 */
public interface StorageService {

    /** 存储文件，返回可再次读取的存储路径 */
    String store(String objectKey, InputStream in, String contentType, long size);

    /** 按存储路径读取文件内容 */
    InputStream load(String objectKey);

    /** 删除文件 */
    void delete(String objectKey);
}
