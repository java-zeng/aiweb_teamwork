package com.aiweb.controller;

import com.aiweb.common.Result;
import com.aiweb.dto.UserRoleKnowledgeBaseDto;
import com.aiweb.dto.request.SelectRoleRequest;
import com.aiweb.service.UserRoleKnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 用户角色知识库管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user-role-kb")
public class UserRoleKnowledgeBaseController {
    
    @Autowired
    private UserRoleKnowledgeBaseService userRoleKnowledgeBaseService;
    
    /**
     * 选择角色并创建知识库
     */
    @PostMapping("/select-role")
    public Result selectRole(@RequestHeader("Authorization") String token,
                            @Valid @RequestBody SelectRoleRequest request) {
        try {
            // 从token中获取用户ID（这里需要根据你的JWT实现来解析）
            Long userId = getUserIdFromToken(token);
            
            UserRoleKnowledgeBaseDto result = userRoleKnowledgeBaseService.selectRoleAndCreateKnowledgeBase(userId, request);
            
            log.info("用户 {} 选择角色 {} 成功", userId, request.getRoleName());
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("选择角色失败", e);
            return Result.error("选择角色失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户的所有角色知识库
     */
    @GetMapping("/list")
    public Result getUserRoleKnowledgeBases(@RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            
            List<UserRoleKnowledgeBaseDto> result = userRoleKnowledgeBaseService.getUserRoleKnowledgeBases(userId);
            
            log.info("获取用户 {} 的角色知识库列表成功，共 {} 个", userId, result.size());
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("获取角色知识库列表失败", e);
            return Result.error("获取角色知识库列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定角色的知识库
     */
    @GetMapping("/{roleName}")
    public Result getUserRoleKnowledgeBase(@RequestHeader("Authorization") String token,
                                          @PathVariable String roleName) {
        try {
            Long userId = getUserIdFromToken(token);
            
            UserRoleKnowledgeBaseDto result = userRoleKnowledgeBaseService.getUserRoleKnowledgeBase(userId, roleName);
            
            if (result == null) {
                return Result.error("该角色知识库不存在");
            }
            
            log.info("获取用户 {} 角色 {} 的知识库成功", userId, roleName);
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("获取角色知识库失败", e);
            return Result.error("获取角色知识库失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除角色知识库
     */
    @DeleteMapping("/{roleName}")
    public Result deleteRoleKnowledgeBase(@RequestHeader("Authorization") String token,
                                         @PathVariable String roleName) {
        try {
            Long userId = getUserIdFromToken(token);
            
            boolean result = userRoleKnowledgeBaseService.deleteRoleKnowledgeBase(userId, roleName);
            
            if (!result) {
                return Result.error("删除角色知识库失败，该角色知识库不存在");
            }
            
            log.info("删除用户 {} 角色 {} 的知识库成功", userId, roleName);
            return Result.success();
            
        } catch (Exception e) {
            log.error("删除角色知识库失败", e);
            return Result.error("删除角色知识库失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查用户是否已有该角色的知识库
     */
    @GetMapping("/check/{roleName}")
    public Result checkRoleKnowledgeBase(@RequestHeader("Authorization") String token,
                                        @PathVariable String roleName) {
        try {
            Long userId = getUserIdFromToken(token);
            
            boolean exists = userRoleKnowledgeBaseService.hasRoleKnowledgeBase(userId, roleName);
            
            log.info("检查用户 {} 角色 {} 的知识库存在性: {}", userId, roleName, exists);
            return Result.success(exists);
            
        } catch (Exception e) {
            log.error("检查角色知识库失败", e);
            return Result.error("检查角色知识库失败: " + e.getMessage());
        }
    }
    
    /**
     * 从token中获取用户ID
     * 这里需要根据你的JWT实现来解析token
     */
    private Long getUserIdFromToken(String token) {
        // TODO: 实现从JWT token中解析用户ID的逻辑
        // 这里暂时返回一个模拟的用户ID，你需要根据实际的JWT实现来修改
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("无效的token");
        }
        
        // 这里应该解析JWT token获取用户ID
        // 暂时返回模拟值，请根据实际实现修改
        return 1L;
    }
}
