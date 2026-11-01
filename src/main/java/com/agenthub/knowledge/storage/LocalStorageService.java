package com.agenthub.knowledge.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 本地磁盘存储：开发环境与单机部署使用（agenthub.storage.type=local）。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "agenthub.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path baseDir;

    public LocalStorageService(com.agenthub.knowledge.storage.StorageProperties properties) {
        this.baseDir = Path.of(properties.getLocal().getBaseDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建本地存储目录: " + baseDir, e);
        }
    }

    @Override
    public String store(String objectKey, InputStream in, String contentType, long size) {
        Path target = baseDir.resolve(objectKey).normalize();
        // 路径穿越防护：目标必须位于 baseDir 之内
        if (!target.startsWith(baseDir)) {
            throw new SecurityException("非法存储路径: " + objectKey);
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return objectKey;
        } catch (IOException e) {
            throw new IllegalStateException("本地文件写入失败: " + objectKey, e);
        }
    }

    @Override
    public InputStream load(String objectKey) {
        try {
            return Files.newInputStream(baseDir.resolve(objectKey));
        } catch (IOException e) {
            throw new IllegalStateException("本地文件读取失败: " + objectKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(baseDir.resolve(objectKey));
        } catch (IOException e) {
            log.warn("本地文件删除失败: {}", objectKey, e);
        }
    }
}
