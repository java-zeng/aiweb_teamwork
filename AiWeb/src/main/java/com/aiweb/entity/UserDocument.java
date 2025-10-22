package com.aiweb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户上传文档实体类
 * 用于记录用户上传到FastGPT的文档信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_documents")
public class UserDocument {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 用户名
     */
    @TableField("username")
    private String username;
    
    /**
     * FastGPT数据集ID
     */
    @TableField("dataset_id")
    private String datasetId;
    
    /**
     * FastGPT文档ID
     */
    @TableField("fastgpt_document_id")
    private String fastgptDocumentId;
    
    /**
     * 原始文件名（正确编码的中英文名称）
     */
    @TableField("original_filename")
    private String originalFilename;
    
    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;
    
    /**
     * 文件类型
     */
    @TableField("file_type")
    private String fileType;
    
    /**
     * 上传时间
     */
    @TableField("upload_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime uploadTime;
    
    /**
     * 文档状态：uploading-上传中, processing-处理中, completed-已完成, failed-失败
     */
    @TableField("status")
    private String status;
    
    /**
     * 错误信息（如果状态为failed）
     */
    @TableField("error_message")
    private String errorMessage;
}
