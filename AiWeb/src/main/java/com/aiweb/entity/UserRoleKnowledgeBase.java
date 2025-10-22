package com.aiweb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户角色知识库绑定关系实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user_role_knowledge_base")
public class UserRoleKnowledgeBase {
    
    @TableId(value = "id", type = IdType.AUTO)
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
     * 更新时间
     */
    private LocalDateTime updatedTime;
    
    /**
     * 状态：1-正常，0-删除
     */
    private Integer status;
}
