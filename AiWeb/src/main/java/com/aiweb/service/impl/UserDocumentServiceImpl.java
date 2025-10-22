package com.aiweb.service.impl;

import com.aiweb.entity.UserDocument;
import com.aiweb.mapper.UserDocumentMapper;
import com.aiweb.service.UserDocumentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户文档管理服务实现类
 */
@Slf4j
@Service
public class UserDocumentServiceImpl extends ServiceImpl<UserDocumentMapper, UserDocument> implements UserDocumentService {
    
    @Autowired
    private UserDocumentMapper userDocumentMapper;
    
    @Override
    public Long saveUserDocument(UserDocument userDocument) {
        userDocument.setUploadTime(LocalDateTime.now());
        userDocument.setStatus("uploading");
        userDocumentMapper.insert(userDocument);
        log.info("保存用户文档信息: 用户={}, 文件名={}, 文档ID={}", 
                userDocument.getUsername(), userDocument.getOriginalFilename(), userDocument.getId());
        return userDocument.getId();
    }
    
    @Override
    public List<UserDocument> getUserDocuments(String username) {
        QueryWrapper<UserDocument> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username)
                   .orderByDesc("upload_time");
        return userDocumentMapper.selectList(queryWrapper);
    }
    
    @Override
    public boolean deleteUserDocument(Long documentId, String username) {
        // 先查询文档是否存在且属于该用户
        QueryWrapper<UserDocument> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", documentId)
                   .eq("username", username);
        UserDocument document = userDocumentMapper.selectOne(queryWrapper);
        
        if (document == null) {
            log.warn("文档不存在或不属于用户: documentId={}, username={}", documentId, username);
            return false;
        }
        
        try {
            // 如果FastGPT文档ID存在，尝试从FastGPT中删除
            if (document.getFastgptDocumentId() != null && !document.getFastgptDocumentId().isEmpty()) {
                // 这里需要调用FastGPT的删除文档API
                // 由于FastGPT的删除文档API可能需要不同的实现，这里先记录日志
                log.info("尝试从FastGPT删除文档: datasetId={}, documentId={}", 
                        document.getDatasetId(), document.getFastgptDocumentId());
                // TODO: 实现FastGPT删除文档的API调用
            }
            
            // 从数据库中删除记录
            int result = userDocumentMapper.deleteById(documentId);
            if (result > 0) {
                log.info("成功删除用户文档: documentId={}, username={}, filename={}", 
                        documentId, username, document.getOriginalFilename());
                return true;
            } else {
                log.error("删除文档失败: documentId={}, username={}", documentId, username);
                return false;
            }
        } catch (Exception e) {
            log.error("删除文档时发生异常: documentId={}, username={}, error={}", 
                    documentId, username, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public UserDocument getUserDocumentById(Long documentId, String username) {
        QueryWrapper<UserDocument> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", documentId)
                   .eq("username", username);
        return userDocumentMapper.selectOne(queryWrapper);
    }
    
    @Override
    public List<UserDocument> getUserDocumentsByDatasetId(String datasetId) {
        QueryWrapper<UserDocument> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dataset_id", datasetId)
                   .orderByDesc("upload_time");
        return userDocumentMapper.selectList(queryWrapper);
    }
}
