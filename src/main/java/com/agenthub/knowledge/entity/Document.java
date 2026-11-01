package com.agenthub.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档：一次上传对应一条记录，异步流水线状态机见 {@link com.agenthub.knowledge.enums.DocumentStatus}。
 */
@Data
@TableName("document")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long kbId;

    private Long tenantId;

    private String fileName;

    /** 文件扩展名，如 pdf/docx/txt/md */
    private String fileType;

    private Long fileSize;

    /** 对象存储路径（MinIO objectKey 或本地相对路径） */
    private String storagePath;

    /** 流水线状态：UPLOADED/PARSING/PARSED/EMBEDDING/READY/FAILED */
    private String status;

    /** 解析引擎：tika/mineru */
    private String parseProvider;

    /** 解析后的纯文本（分块前暂存） */
    private String parsedText;

    /** 生成的向量块数量 */
    private Integer chunkCount;

    private String errorMsg;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
