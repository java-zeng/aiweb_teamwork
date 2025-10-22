package com.aiweb.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文件DTO
 */
@Data
public class KnowledgeBaseFileDto {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 知识库ID
     */
    private String knowledgeBaseId;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
    
    /**
     * 文件大小格式化显示
     */
    private String fileSizeFormatted;
}
