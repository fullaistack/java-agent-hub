package com.agenthub.knowledge.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * MinIO 对象存储：生产环境推荐（agenthub.storage.type=minio）。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "agenthub.storage.type", havingValue = "minio")
public class MinioStorageService implements StorageService {

    private final MinioClient client;
    private final String bucket;

    public MinioStorageService(StorageProperties properties) {
        StorageProperties.MinioConfig minio = properties.getMinio();
        this.bucket = minio.getBucket();
        this.client = MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
        ensureBucket();
    }

    private void ensureBucket() {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket 已创建: {}", bucket);
            }
        } catch (Exception e) {
            throw new IllegalStateException("MinIO bucket 初始化失败: " + bucket, e);
        }
    }

    @Override
    public String store(String objectKey, InputStream in, String contentType, long size) {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(in, size, -1)
                    .contentType(contentType)
                    .build());
            return objectKey;
        } catch (Exception e) {
            throw new IllegalStateException("MinIO 上传失败: " + objectKey, e);
        }
    }

    @Override
    public InputStream load(String objectKey) {
        try {
            return client.getObject(io.minio.GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("MinIO 下载失败: " + objectKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception e) {
            log.warn("MinIO 删除失败: {}", objectKey, e);
        }
    }
}
