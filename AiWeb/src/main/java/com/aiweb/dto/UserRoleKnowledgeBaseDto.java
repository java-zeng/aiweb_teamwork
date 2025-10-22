package com.aiweb.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户角色知识库DTO
 */
@Data
public class UserRoleKnowledgeBaseDto {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 角色名称
     */
    private String roleName;
    
    /**
     * 知识库ID
     */
    private String knowledgeBaseId;
    
    /**
     * 知识库名称
     */
    private String knowledgeBaseName;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 文件列表
     */
    private List<KnowledgeBaseFileDto> files;
    
    /**
     * 文件数量
     */
    private Integer fileCount;
}
