package com.aiweb.service.impl;

import com.aiweb.dto.KnowledgeBaseFileDto;
import com.aiweb.dto.UserRoleKnowledgeBaseDto;
import com.aiweb.dto.request.SelectRoleRequest;
import com.aiweb.entity.KnowledgeBaseFile;
import com.aiweb.entity.UserRoleKnowledgeBase;
import com.aiweb.mapper.KnowledgeBaseFileMapper;
import com.aiweb.mapper.UserRoleKnowledgeBaseMapper;
import com.aiweb.service.FastGptService;
import com.aiweb.service.UserRoleKnowledgeBaseService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户角色知识库服务实现类
 */
@Slf4j
@Service
public class UserRoleKnowledgeBaseServiceImpl implements UserRoleKnowledgeBaseService {
    
    @Autowired
    private UserRoleKnowledgeBaseMapper userRoleKnowledgeBaseMapper;
    
    @Autowired
    private KnowledgeBaseFileMapper knowledgeBaseFileMapper;
    
    @Autowired
    private FastGptService fastGptService;
    
    @Autowired
    private com.aiweb.mapper.UserMapper userMapper;
    
    @Override
    @Transactional
    public UserRoleKnowledgeBaseDto selectRoleAndCreateKnowledgeBase(Long userId, SelectRoleRequest request) {
        String roleName = request.getRoleName();
        log.info("开始为用户 {} 创建角色 {} 的知识库", userId, roleName);
        
        // 检查是否已存在该角色的知识库
        if (hasRoleKnowledgeBase(userId, roleName)) {
            log.warn("用户 {} 的角色 {} 知识库已存在", userId, roleName);
            throw new RuntimeException("该角色已存在知识库，请选择其他角色");
        }
        
        // 从userId获取username（这里需要根据你的用户系统来实现）
        String username = getUsernameFromUserId(userId);
        log.info("获取到用户名: {}", username);
        
        // 生成知识库名称
        String knowledgeBaseName = generateKnowledgeBaseName(username, roleName);
        log.info("生成知识库名称: {}", knowledgeBaseName);
        
        // 调用FastGPT API创建数据集
        String fastGptDatasetId;
        try {
            log.info("开始调用FastGPT API创建数据集: {}", knowledgeBaseName);
            fastGptDatasetId = fastGptService.createDataset(knowledgeBaseName);
            log.info("FastGPT API返回的数据集ID: '{}'", fastGptDatasetId);
            
            if (fastGptDatasetId == null || fastGptDatasetId.trim().isEmpty()) {
                log.error("FastGPT API返回的数据集ID为空或空字符串");
                throw new RuntimeException("FastGPT API返回的数据集ID为空");
            }
            
            log.info("成功在FastGPT创建数据集: {}，ID: {}", knowledgeBaseName, fastGptDatasetId);
        } catch (Exception e) {
            log.error("在FastGPT创建数据集失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建FastGPT数据集失败: " + e.getMessage());
        }
        
        // 创建用户角色知识库绑定关系
        UserRoleKnowledgeBase userRoleKnowledgeBase = new UserRoleKnowledgeBase();
        userRoleKnowledgeBase.setUserId(userId);
        userRoleKnowledgeBase.setRoleName(roleName);
        userRoleKnowledgeBase.setKnowledgeBaseId(fastGptDatasetId); // 使用FastGPT返回的数据集ID
        userRoleKnowledgeBase.setKnowledgeBaseName(knowledgeBaseName);
        userRoleKnowledgeBase.setCreatedTime(LocalDateTime.now());
        userRoleKnowledgeBase.setUpdatedTime(LocalDateTime.now());
        userRoleKnowledgeBase.setStatus(1);
        
        log.info("准备保存用户角色知识库绑定关系到数据库: {}", userRoleKnowledgeBase);
        
        int result = userRoleKnowledgeBaseMapper.insert(userRoleKnowledgeBase);
        if (result <= 0) {
            log.error("数据库插入失败，影响行数: {}", result);
            throw new RuntimeException("创建角色知识库失败");
        }
        
        log.info("为用户 {} 创建角色 {} 的知识库成功，FastGPT数据集ID: {}", userId, roleName, fastGptDatasetId);
        
        // 转换为DTO并返回
        UserRoleKnowledgeBaseDto dto = convertToDto(userRoleKnowledgeBase);
        log.info("转换后的DTO: {}", dto);
        return dto;
    }
    
    @Override
    public List<UserRoleKnowledgeBaseDto> getUserRoleKnowledgeBases(Long userId) {
        QueryWrapper<UserRoleKnowledgeBase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("status", 1)
                   .orderByDesc("created_time");
        
        List<UserRoleKnowledgeBase> userRoleKnowledgeBases = userRoleKnowledgeBaseMapper.selectList(queryWrapper);
        
        return userRoleKnowledgeBases.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public UserRoleKnowledgeBaseDto getUserRoleKnowledgeBase(Long userId, String roleName) {
        QueryWrapper<UserRoleKnowledgeBase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("role_name", roleName)
                   .eq("status", 1);
        
        UserRoleKnowledgeBase userRoleKnowledgeBase = userRoleKnowledgeBaseMapper.selectOne(queryWrapper);
        if (userRoleKnowledgeBase == null) {
            return null;
        }
        
        return convertToDto(userRoleKnowledgeBase);
    }
    
    @Override
    public boolean hasRoleKnowledgeBase(Long userId, String roleName) {
        QueryWrapper<UserRoleKnowledgeBase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("role_name", roleName)
                   .eq("status", 1);
        
        Long count = userRoleKnowledgeBaseMapper.selectCount(queryWrapper);
        log.info("检查用户 {} 角色 {} 的知识库是否存在，查询结果数量: {}", userId, roleName, count);
        return count > 0;
    }
    
    @Override
    @Transactional
    public boolean deleteRoleKnowledgeBase(Long userId, String roleName) {
        QueryWrapper<UserRoleKnowledgeBase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("role_name", roleName)
                   .eq("status", 1);
        
        UserRoleKnowledgeBase userRoleKnowledgeBase = userRoleKnowledgeBaseMapper.selectOne(queryWrapper);
        if (userRoleKnowledgeBase == null) {
            return false;
        }
        
        // 删除FastGPT中的数据集
        try {
            fastGptService.deleteDataset(userRoleKnowledgeBase.getKnowledgeBaseId());
            log.info("成功从FastGPT删除数据集: {}", userRoleKnowledgeBase.getKnowledgeBaseId());
        } catch (Exception e) {
            log.error("从FastGPT删除数据集失败: {}，错误: {}", userRoleKnowledgeBase.getKnowledgeBaseId(), e.getMessage());
            // 即使FastGPT删除失败，我们仍然删除本地记录
        }
        
        // 软删除用户角色知识库绑定关系
        userRoleKnowledgeBase.setStatus(0);
        userRoleKnowledgeBase.setUpdatedTime(LocalDateTime.now());
        int result = userRoleKnowledgeBaseMapper.updateById(userRoleKnowledgeBase);
        
        // 软删除知识库中的所有文件
        QueryWrapper<KnowledgeBaseFile> fileQueryWrapper = new QueryWrapper<>();
        fileQueryWrapper.eq("knowledge_base_id", userRoleKnowledgeBase.getKnowledgeBaseId())
                       .eq("status", 1);
        
        List<KnowledgeBaseFile> files = knowledgeBaseFileMapper.selectList(fileQueryWrapper);
        for (KnowledgeBaseFile file : files) {
            file.setStatus(0);
            file.setUploadTime(LocalDateTime.now());
            knowledgeBaseFileMapper.updateById(file);
        }
        
        log.info("删除用户 {} 角色 {} 的知识库成功", userId, roleName);
        return result > 0;
    }
    
    
    /**
     * 生成知识库名称
     */
    private String generateKnowledgeBaseName(String username, String roleName) {
        return username + "_" + roleName + "知识库";
    }
    
    /**
     * 转换为DTO
     */
    private UserRoleKnowledgeBaseDto convertToDto(UserRoleKnowledgeBase userRoleKnowledgeBase) {
        UserRoleKnowledgeBaseDto dto = new UserRoleKnowledgeBaseDto();
        BeanUtils.copyProperties(userRoleKnowledgeBase, dto);
        
        // 查询知识库中的文件
        QueryWrapper<KnowledgeBaseFile> fileQueryWrapper = new QueryWrapper<>();
        fileQueryWrapper.eq("knowledge_base_id", userRoleKnowledgeBase.getKnowledgeBaseId())
                       .eq("status", 1)
                       .orderByDesc("upload_time");
        
        List<KnowledgeBaseFile> files = knowledgeBaseFileMapper.selectList(fileQueryWrapper);
        List<KnowledgeBaseFileDto> fileDtos = files.stream()
                .map(this::convertFileToDto)
                .collect(Collectors.toList());
        
        dto.setFiles(fileDtos);
        dto.setFileCount(fileDtos.size());
        
        return dto;
    }
    
    /**
     * 转换文件为DTO
     */
    private KnowledgeBaseFileDto convertFileToDto(KnowledgeBaseFile file) {
        KnowledgeBaseFileDto dto = new KnowledgeBaseFileDto();
        BeanUtils.copyProperties(file, dto);
        
        // 格式化文件大小
        dto.setFileSizeFormatted(formatFileSize(file.getFileSize()));
        
        return dto;
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long fileSize) {
        if (fileSize == null || fileSize == 0) {
            return "0 B";
        }
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = fileSize.doubleValue();
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }
    
    /**
     * 从用户ID获取用户名
     */
    private String getUsernameFromUserId(Long userId) {
        try {
            // 从数据库获取用户信息
            com.aiweb.entity.User user = userMapper.selectById(userId);
            if (user != null) {
                log.info("找到用户ID: {}, 用户名: {}", userId, user.getUsername());
                return user.getUsername();
            } else {
                log.error("用户不存在: {}", userId);
                // 如果用户不存在，使用默认格式
                return "用户" + userId;
            }
        } catch (Exception e) {
            log.error("获取用户名失败: {}", e.getMessage(), e);
            // 如果出现异常，使用默认格式
            return "用户" + userId;
        }
    }
}
