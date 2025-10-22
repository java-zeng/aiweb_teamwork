package com.aiweb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 知识库文件实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("knowledge_base_files")
public class KnowledgeBaseFile {
    
    @TableId(value = "id", type = IdType.AUTO)
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
     * 状态：1-正常，0-删除
     */
    private Integer status;
}
