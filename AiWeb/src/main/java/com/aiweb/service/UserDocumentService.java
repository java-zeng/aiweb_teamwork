package com.aiweb.service;

import com.aiweb.entity.UserDocument;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 用户文档管理服务接口
 */
public interface UserDocumentService extends IService<UserDocument> {
    
    /**
     * 保存用户上传的文档信息
     * @param userDocument 文档信息
     * @return 保存后的文档ID
     */
    Long saveUserDocument(UserDocument userDocument);
    
    /**
     * 根据用户名获取用户上传的所有文档
     * @param username 用户名
     * @return 文档列表
     */
    List<UserDocument> getUserDocuments(String username);
    
    /**
     * 根据文档ID删除文档（包括FastGPT中的对应文档）
     * @param documentId 文档ID
     * @param username 用户名（用于权限验证）
     * @return 是否删除成功
     */
    boolean deleteUserDocument(Long documentId, String username);
    
    /**
     * 根据文档ID获取文档信息
     * @param documentId 文档ID
     * @param username 用户名（用于权限验证）
     * @return 文档信息
     */
    UserDocument getUserDocumentById(Long documentId, String username);
    
    /**
     * 根据数据集ID获取文档列表
     * @param datasetId 数据集ID
     * @return 文档列表
     */
    List<UserDocument> getUserDocumentsByDatasetId(String datasetId);
}
