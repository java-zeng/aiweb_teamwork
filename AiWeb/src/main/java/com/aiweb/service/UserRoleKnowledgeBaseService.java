package com.aiweb.service;

import com.aiweb.dto.UserRoleKnowledgeBaseDto;
import com.aiweb.dto.request.SelectRoleRequest;
import com.aiweb.entity.UserRoleKnowledgeBase;

import java.util.List;

/**
 * 用户角色知识库服务接口
 */
public interface UserRoleKnowledgeBaseService {
    
    /**
     * 为用户选择角色并创建知识库
     */
    UserRoleKnowledgeBaseDto selectRoleAndCreateKnowledgeBase(Long userId, SelectRoleRequest request);
    
    /**
     * 获取用户的所有角色知识库
     */
    List<UserRoleKnowledgeBaseDto> getUserRoleKnowledgeBases(Long userId);
    
    /**
     * 根据用户ID和角色名称获取知识库
     */
    UserRoleKnowledgeBaseDto getUserRoleKnowledgeBase(Long userId, String roleName);
    
    /**
     * 检查用户是否已有该角色的知识库
     */
    boolean hasRoleKnowledgeBase(Long userId, String roleName);
    
    /**
     * 删除角色知识库
     */
    boolean deleteRoleKnowledgeBase(Long userId, String roleName);
}
