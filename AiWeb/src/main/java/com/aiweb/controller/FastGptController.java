package com.aiweb.controller;

import com.aiweb.common.Result;
import com.aiweb.dto.ChatResponse;
import com.aiweb.dto.request.ChatRequest;
import com.aiweb.entity.FastGptApiData;
import com.aiweb.entity.FastGptApiResponse;
import com.aiweb.entity.UserDocument;
import com.aiweb.service.FastGptService;
import com.aiweb.service.UserDocumentService;
import com.aiweb.service.UserRoleKnowledgeBaseService;
import com.aiweb.mapper.KnowledgeBaseFileMapper;
import com.aiweb.utils.JwtUtil;
import com.aiweb.utils.FilenameEncodingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@Slf4j
@RequestMapping("/api/fastgpt")
public class FastGptController {
    
    @Autowired
    private FastGptService fastGptService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserDocumentService userDocumentService;
    
    @Autowired
    private UserRoleKnowledgeBaseService userRoleKnowledgeBaseService;
    
    @Autowired
    private KnowledgeBaseFileMapper knowledgeBaseFileMapper;
    
    @Autowired
    private com.aiweb.service.UserService userService;

    
    /**
     * 文件上传（支持单个和批量）
     * 单个文件：files参数传入一个文件
     * 批量文件：files参数传入多个文件
     */
    @PostMapping("/upload")
    public Result uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "parentId", required = false) String parentId,
            @RequestParam(value = "chunkSize", required = false) Integer chunkSize,
            Authentication authentication,
            HttpServletRequest request
    ) {
        if (files == null || files.isEmpty()) {
            return Result.error("上传文件不能为空");
        }
        
        try {
            String username = getUsername(authentication, request);
            if (username == null) {
                return Result.error("未认证，请先登录");
            }
            
            // 确保用户数据集存在
            String datasetId = fastGptService.ensureUserDataset(username);
            
            List<Map<String, Object>> results = new java.util.ArrayList<>();
            int successCount = 0;
            int failCount = 0;
            
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                
                try {
                    // 处理文件名编码
                    String originalFilename = FilenameEncodingUtils.getCorrectFilename(file);
                    
                    // 检查重复文件名
                    List<UserDocument> existingDocuments = userDocumentService.getUserDocuments(username);
                    List<String> existingFilenames = existingDocuments.stream()
                            .map(UserDocument::getOriginalFilename)
                            .collect(java.util.stream.Collectors.toList());
                    
                    String finalFilename = FilenameEncodingUtils.handleDuplicateFilename(originalFilename, existingFilenames);
                    
                    // 创建文档记录
                    UserDocument userDocument = new UserDocument();
                    userDocument.setUsername(username);
                    userDocument.setDatasetId(datasetId);
                    userDocument.setOriginalFilename(finalFilename);
                    userDocument.setFileSize(file.getSize());
                    userDocument.setFileType(getFileExtension(finalFilename));
                    userDocument.setStatus("uploading");
                    
                    Long documentId = userDocumentService.saveUserDocument(userDocument);
                    
                    // 上传到FastGPT
                    FastGptApiData data = new FastGptApiData(
                            datasetId, "chunk", parentId, null, null, null, null,
                            chunkSize != null ? "custom" : "auto", null, chunkSize,
                            null, null, null, null, null
                    );
                    
                    FastGptApiResponse resp = fastGptService.CreateListFromFiles(file, data);
                    
                    // 更新文档状态
                    userDocument.setId(documentId);
                    userDocument.setStatus("completed");
                    if (resp != null && resp.getData() != null && resp.getData().getCollectionId() != null) {
                        userDocument.setFastgptDocumentId(resp.getData().getCollectionId());
                    }
                    userDocumentService.updateById(userDocument);
                    
                    Map<String, Object> fileResult = new HashMap<>();
                    fileResult.put("documentId", documentId);
                    fileResult.put("filename", finalFilename);
                    fileResult.put("status", "success");
                    results.add(fileResult);
                    successCount++;
                    
                } catch (Exception e) {
                    log.error("文件 {} 上传失败", file.getOriginalFilename(), e);
                    Map<String, Object> fileResult = new HashMap<>();
                    fileResult.put("filename", file.getOriginalFilename());
                    fileResult.put("status", "failed");
                    fileResult.put("error", e.getMessage());
                    results.add(fileResult);
                    failCount++;
                }
            }
            
            Map<String, Object> batchResult = new HashMap<>();
            batchResult.put("totalFiles", files.size());
            batchResult.put("successCount", successCount);
            batchResult.put("failCount", failCount);
            batchResult.put("datasetId", datasetId);
            batchResult.put("results", results);
            
            return Result.success(batchResult);
            
        } catch (Exception e) {
            log.error("批量上传失败", e);
            return Result.error("批量上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 支持角色选择的文件上传接口
     */
    @PostMapping("/upload-with-role")
    public Result uploadFilesWithRole(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("roleName") String roleName,
            @RequestParam(value = "parentId", required = false) String parentId,
            @RequestParam(value = "chunkSize", required = false) Integer chunkSize,
            Authentication authentication,
            HttpServletRequest request
    ) {
        if (files == null || files.isEmpty()) {
            return Result.error("上传文件不能为空");
        }
        
        if (roleName == null || roleName.trim().isEmpty()) {
            return Result.error("角色名称不能为空");
        }
        
        try {
            String username = getUsername(authentication, request);
            if (username == null) {
                return Result.error("未认证，请先登录");
            }
            
            // 获取用户ID（这里需要根据你的用户系统来获取）
            Long userId = getUserIdFromUsername(username);
            
            // 检查或创建角色知识库
            log.info("开始确保角色知识库存在 - 用户ID: {}, 角色: {}", userId, roleName);
            String knowledgeBaseId = ensureRoleKnowledgeBase(userId, roleName);
            log.info("获取到的知识库ID: '{}'", knowledgeBaseId);
            
            if (knowledgeBaseId == null || knowledgeBaseId.trim().isEmpty()) {
                log.error("知识库ID为空，无法上传文件");
                return Result.error("角色知识库创建失败，知识库ID为空");
            }
            
            List<Map<String, Object>> results = new java.util.ArrayList<>();
            int successCount = 0;
            int failCount = 0;
            
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                
                try {
                    // 处理文件名编码
                    String originalFilename = FilenameEncodingUtils.getCorrectFilename(file);
                    
                    // 检查重复文件名（在角色知识库中）
                    List<UserDocument> existingDocuments = userDocumentService.getUserDocumentsByDatasetId(knowledgeBaseId);
                    List<String> existingFilenames = existingDocuments.stream()
                            .map(UserDocument::getOriginalFilename)
                            .collect(java.util.stream.Collectors.toList());
                    
                    String finalFilename = FilenameEncodingUtils.handleDuplicateFilename(originalFilename, existingFilenames);
                    
                    // 创建文档记录
                    UserDocument userDocument = new UserDocument();
                    userDocument.setUsername(username);
                    userDocument.setDatasetId(knowledgeBaseId);
                    userDocument.setOriginalFilename(finalFilename);
                    userDocument.setFileSize(file.getSize());
                    userDocument.setFileType(getFileExtension(finalFilename));
                    userDocument.setStatus("uploading");
                    
                    Long documentId = userDocumentService.saveUserDocument(userDocument);
                    
                    // 上传到FastGPT
                    FastGptApiData data = new FastGptApiData(
                            knowledgeBaseId, "chunk", parentId, null, null, null, null,
                            chunkSize != null ? "custom" : "auto", null, chunkSize,
                            null, null, null, null, null
                    );
                    
                    FastGptApiResponse resp = fastGptService.CreateListFromFiles(file, data);
                    
                    // 更新文档状态
                    userDocument.setId(documentId);
                    userDocument.setStatus("completed");
                    if (resp != null && resp.getData() != null && resp.getData().getCollectionId() != null) {
                        userDocument.setFastgptDocumentId(resp.getData().getCollectionId());
                    }
                    userDocumentService.updateById(userDocument);
                    
                    // 记录到知识库文件表
                    saveKnowledgeBaseFile(knowledgeBaseId, finalFilename, file);
                    
                    Map<String, Object> fileResult = new HashMap<>();
                    fileResult.put("documentId", documentId);
                    fileResult.put("filename", finalFilename);
                    fileResult.put("status", "success");
                    fileResult.put("roleName", roleName);
                    fileResult.put("knowledgeBaseId", knowledgeBaseId);
                    results.add(fileResult);
                    successCount++;
                    
                } catch (Exception e) {
                    log.error("文件 {} 上传失败", file.getOriginalFilename(), e);
                    Map<String, Object> fileResult = new HashMap<>();
                    fileResult.put("filename", file.getOriginalFilename());
                    fileResult.put("status", "failed");
                    fileResult.put("error", e.getMessage());
                    results.add(fileResult);
                    failCount++;
                }
            }
            
            Map<String, Object> batchResult = new HashMap<>();
            batchResult.put("totalFiles", files.size());
            batchResult.put("successCount", successCount);
            batchResult.put("failCount", failCount);
            batchResult.put("roleName", roleName);
            batchResult.put("knowledgeBaseId", knowledgeBaseId);
            batchResult.put("results", results);
            
            return Result.success(batchResult);
            
        } catch (Exception e) {
            log.error("角色文件上传失败", e);
            return Result.error("角色文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户知识库列表（数据集列表）
     */
    @GetMapping("/knowledge-bases")
    public Result getUserKnowledgeBases(
            Authentication authentication,
            HttpServletRequest request
    ) {
        try {
            String username = getUsername(authentication, request);
            if (username == null) {
                return Result.error("未认证，请先登录");
            }
            
            // 获取用户的数据集ID
            String datasetId = fastGptService.ensureUserDataset(username);
            
            // 获取用户的所有文档
            List<UserDocument> documents = userDocumentService.getUserDocuments(username);
            
            Map<String, Object> knowledgeBase = new HashMap<>();
            knowledgeBase.put("datasetId", datasetId);
            knowledgeBase.put("username", username);
            knowledgeBase.put("documentCount", documents.size());
            knowledgeBase.put("documents", documents);
            
            return Result.success(knowledgeBase);
            
        } catch (Exception e) {
            log.error("获取知识库列表失败", e);
            return Result.error("获取知识库列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定知识库中的文件列表
     */
    @GetMapping("/files")
    public Result getKnowledgeBaseFiles(
            @RequestParam(value = "datasetId", required = false) String datasetId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        try {
            String username = getUsername(authentication, request);
            if (username == null) {
                return Result.error("未认证，请先登录");
            }
            
            // 如果没有指定datasetId，使用用户默认的数据集
            final String finalDatasetId;
            if (datasetId == null || datasetId.isEmpty()) {
                finalDatasetId = fastGptService.ensureUserDataset(username);
            } else {
                finalDatasetId = datasetId;
            }
            
            List<UserDocument> documents = userDocumentService.getUserDocuments(username);
            
            // 如果指定了datasetId，只返回该数据集的文件
            if (finalDatasetId != null && !finalDatasetId.isEmpty()) {
                documents = documents.stream()
                        .filter(doc -> finalDatasetId.equals(doc.getDatasetId()))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            return Result.success(documents);
            
        } catch (Exception e) {
            log.error("获取文件列表失败", e);
            return Result.error("获取文件列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除文件
     */
    @DeleteMapping("/files/{documentId}")
    public Result deleteFile(
            @PathVariable Long documentId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        try {
            String username = getUsername(authentication, request);
            if (username == null) {
                return Result.error("未认证，请先登录");
            }
            
            boolean success = userDocumentService.deleteUserDocument(documentId, username);
            if (success) {
                return Result.success("文件删除成功");
            } else {
                return Result.error("文件删除失败或文件不存在");
            }
            
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return Result.error("删除文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取文件详情
     */
    @GetMapping("/files/{documentId}")
    public Result getFileDetail(
            @PathVariable Long documentId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        try {
            String username = getUsername(authentication, request);
            if (username == null) {
                return Result.error("未认证，请先登录");
            }
            
            UserDocument document = userDocumentService.getUserDocumentById(documentId, username);
            if (document == null) {
                return Result.error("文件不存在或无权限访问");
            }
            
            return Result.success(document);
            
        } catch (Exception e) {
            log.error("获取文件详情失败", e);
            return Result.error("获取文件详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 从认证信息中获取用户名
     */
    private String getUsername(Authentication authentication, HttpServletRequest request) {
        String username = null;
        
        if (authentication != null) {
            username = authentication.getName();
        }
        
        if (username == null || username.isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring("Bearer ".length());
                username = jwtUtil.extractUsername(token);
            }
        }
        
        return username;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
    
    /**
     * 从用户名获取用户ID
     * 这里需要根据你的用户系统来实现
     */
    private Long getUserIdFromUsername(String username) {
        try {
            // 从用户服务中获取用户信息
            com.aiweb.entity.User user = userService.findByUsername(username);
            if (user != null) {
                log.info("找到用户: {}, ID: {}", username, user.getId());
                return user.getId();
            } else {
                log.error("用户不存在: {}", username);
                // 如果用户不存在，创建一个临时用户ID（基于用户名哈希）
                long tempUserId = Math.abs(username.hashCode());
                log.warn("用户不存在，使用临时用户ID: {}", tempUserId);
                return tempUserId;
            }
        } catch (Exception e) {
            log.error("获取用户ID失败: {}", e.getMessage(), e);
            // 如果出现异常，使用基于用户名哈希的临时ID
            long tempUserId = Math.abs(username.hashCode());
            log.warn("获取用户ID异常，使用临时用户ID: {}", tempUserId);
            return tempUserId;
        }
    }
    
    /**
     * 确保角色知识库存在
     */
    private String ensureRoleKnowledgeBase(Long userId, String roleName) {
        try {
            log.info("确保角色知识库存在 - 用户ID: {}, 角色: {}", userId, roleName);
            
            // 检查是否已存在该角色的知识库
            boolean hasExistingKb = userRoleKnowledgeBaseService.hasRoleKnowledgeBase(userId, roleName);
            log.info("检查角色知识库是否存在: {}", hasExistingKb);
            
            if (hasExistingKb) {
                log.info("角色知识库已存在，获取现有知识库ID");
                // 如果存在，返回知识库ID
                var existingKb = userRoleKnowledgeBaseService.getUserRoleKnowledgeBase(userId, roleName);
                log.info("查询到的现有知识库: {}", existingKb);
                if (existingKb != null) {
                    String knowledgeBaseId = existingKb.getKnowledgeBaseId();
                    log.info("现有知识库ID: '{}'", knowledgeBaseId);
                    if (knowledgeBaseId == null || knowledgeBaseId.trim().isEmpty()) {
                        log.error("现有知识库ID为空或空字符串");
                        return null;
                    }
                    return knowledgeBaseId;
                } else {
                    log.error("获取现有知识库失败，返回null");
                    return null;
                }
            } else {
                log.info("角色知识库不存在，创建新的知识库");
                // 如果不存在，创建新的角色知识库
                var selectRoleRequest = new com.aiweb.dto.request.SelectRoleRequest();
                selectRoleRequest.setRoleName(roleName);
                log.info("准备调用selectRoleAndCreateKnowledgeBase方法");
                var newKb = userRoleKnowledgeBaseService.selectRoleAndCreateKnowledgeBase(userId, selectRoleRequest);
                log.info("创建的知识库结果: {}", newKb);
                if (newKb != null) {
                    String knowledgeBaseId = newKb.getKnowledgeBaseId();
                    log.info("新创建的知识库ID: '{}'", knowledgeBaseId);
                    if (knowledgeBaseId == null || knowledgeBaseId.trim().isEmpty()) {
                        log.error("新创建的知识库ID为空或空字符串");
                        return null;
                    }
                    return knowledgeBaseId;
                } else {
                    log.error("创建新知识库失败，返回null");
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("确保角色知识库存在失败", e);
            throw new RuntimeException("创建角色知识库失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存知识库文件记录
     */
    private void saveKnowledgeBaseFile(String knowledgeBaseId, String fileName, MultipartFile file) {
        try {
            com.aiweb.entity.KnowledgeBaseFile kbFile = new com.aiweb.entity.KnowledgeBaseFile();
            kbFile.setKnowledgeBaseId(knowledgeBaseId);
            kbFile.setFileName(fileName);
            kbFile.setFilePath(""); // 这里可以设置实际的文件路径
            kbFile.setFileSize(file.getSize());
            kbFile.setFileType(getFileExtension(fileName));
            kbFile.setUploadTime(java.time.LocalDateTime.now());
            kbFile.setStatus(1);
            
            knowledgeBaseFileMapper.insert(kbFile);
            log.info("保存知识库文件记录: {}", fileName);
        } catch (Exception e) {
            log.error("保存知识库文件记录失败", e);
        }
    }
    
    /**
     * 与FastGPT进行聊天对话，使用知识库
     */
    @PostMapping("/chat")
    public Result chatWithKnowledgeBase(
            @RequestBody ChatRequest chatRequest,
            Authentication authentication,
            HttpServletRequest request
    ) {
        try {
            String username = getUsername(authentication, request);
            if (username == null) {
                return Result.error("未认证，请先登录");
            }
            
            // 验证请求参数
            if (chatRequest.getMessage() == null || chatRequest.getMessage().trim().isEmpty()) {
                return Result.error("消息内容不能为空");
            }
            
            if (chatRequest.getDatasetId() == null || chatRequest.getDatasetId().trim().isEmpty()) {
                return Result.error("数据集ID不能为空");
            }
            
            // 验证用户是否有权限访问该数据集
            String userDatasetId = fastGptService.ensureUserDataset(username);
            if (!userDatasetId.equals(chatRequest.getDatasetId())) {
                return Result.error("无权限访问指定的数据集");
            }
            
            // 调用聊天服务
            ChatResponse chatResponse = fastGptService.chatWithKnowledgeBase(chatRequest);
            
            return Result.success(chatResponse);
            
        } catch (Exception e) {
            log.error("聊天失败", e);
            return Result.error("聊天失败: " + e.getMessage());
        }
    }
    
    /**
     * 简化版聊天接口，自动使用用户默认数据集
     */
    @PostMapping("/chat/simple")
    public Result simpleChat(
            @RequestParam("message") String message,
            @RequestParam(value = "systemPrompt", required = false) String systemPrompt,
            @RequestParam(value = "temperature", required = false) Double temperature,
            @RequestParam(value = "maxTokens", required = false) Integer maxTokens,
            Authentication authentication,
            HttpServletRequest request
    ) {
        try {
            String username = getUsername(authentication, request);
            if (username == null) {
                return Result.error("未认证，请先登录");
            }
            
            if (message == null || message.trim().isEmpty()) {
                return Result.error("消息内容不能为空");
            }
            
            // 获取用户默认数据集
            String datasetId = fastGptService.ensureUserDataset(username);
            
            // 构建聊天请求
            ChatRequest chatRequest = new ChatRequest();
            chatRequest.setMessage(message);
            chatRequest.setDatasetId(datasetId);
            chatRequest.setSystemPrompt(systemPrompt);
            chatRequest.setTemperature(temperature);
            chatRequest.setMaxTokens(maxTokens);
            
            // 调用聊天服务
            ChatResponse chatResponse = fastGptService.chatWithKnowledgeBase(chatRequest);
            
            return Result.success(chatResponse);
            
        } catch (Exception e) {
            log.error("简化聊天失败", e);
            return Result.error("聊天失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查FastGPT服务状态
     */
    @GetMapping("/health")
    public Result checkFastGptHealth() {
        try {
            boolean isHealthy = fastGptService.checkFastGptHealth();
            if (isHealthy) {
                return Result.success("FastGPT服务正常运行");
            } else {
                return Result.error("FastGPT服务不可用");
            }
        } catch (Exception e) {
            log.error("检查FastGPT服务状态失败", e);
            return Result.error("检查FastGPT服务状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户的所有角色知识库
     */
    @GetMapping("/role-knowledge-bases")
    public Result getUserRoleKnowledgeBases(
            Authentication authentication,
            HttpServletRequest request
    ) {
        try {
            String username = getUsername(authentication, request);
            if (username == null) {
                return Result.error("未认证，请先登录");
            }
            
            Long userId = getUserIdFromUsername(username);
            var result = userRoleKnowledgeBaseService.getUserRoleKnowledgeBases(userId);
            
            log.info("获取用户 {} 的角色知识库列表成功，共 {} 个", username, result.size());
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("获取角色知识库列表失败", e);
            return Result.error("获取角色知识库列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定角色的知识库详情
     */
    @GetMapping("/role-knowledge-bases/{roleName}")
    public Result getUserRoleKnowledgeBase(
            @PathVariable String roleName,
            Authentication authentication,
            HttpServletRequest request
    ) {
        try {
            String username = getUsername(authentication, request);
            if (username == null) {
                return Result.error("未认证，请先登录");
            }
            
            Long userId = getUserIdFromUsername(username);
            var result = userRoleKnowledgeBaseService.getUserRoleKnowledgeBase(userId, roleName);
            
            if (result == null) {
                return Result.error("该角色知识库不存在");
            }
            
            log.info("获取用户 {} 角色 {} 的知识库成功", username, roleName);
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("获取角色知识库失败", e);
            return Result.error("获取角色知识库失败: " + e.getMessage());
        }
    }
    
    /**
     * 选择角色并创建知识库
     */
    @PostMapping("/select-role")
    public Result selectRole(
            @RequestBody com.aiweb.dto.request.SelectRoleRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        try {
            String username = getUsername(authentication, httpRequest);
            if (username == null) {
                return Result.error("未认证，请先登录");
            }
            
            Long userId = getUserIdFromUsername(username);
            var result = userRoleKnowledgeBaseService.selectRoleAndCreateKnowledgeBase(userId, request);
            
            log.info("用户 {} 选择角色 {} 成功", username, request.getRoleName());
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("选择角色失败", e);
            return Result.error("选择角色失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除角色知识库
     */
    @DeleteMapping("/role-knowledge-bases/{roleName}")
    public Result deleteRoleKnowledgeBase(
            @PathVariable String roleName,
            Authentication authentication,
            HttpServletRequest request
    ) {
        try {
            String username = getUsername(authentication, request);
            if (username == null) {
                return Result.error("未认证，请先登录");
            }
            
            Long userId = getUserIdFromUsername(username);
            boolean result = userRoleKnowledgeBaseService.deleteRoleKnowledgeBase(userId, roleName);
            
            if (!result) {
                return Result.error("删除角色知识库失败，该角色知识库不存在");
            }
            
            log.info("删除用户 {} 角色 {} 的知识库成功", username, roleName);
            return Result.success();
            
        } catch (Exception e) {
            log.error("删除角色知识库失败", e);
            return Result.error("删除角色知识库失败: " + e.getMessage());
        }
    }
    
    /**
     * 使用角色知识库进行聊天
     */
    @PostMapping("/chat-with-role")
    public Result chatWithRoleKnowledgeBase(
            @RequestBody Map<String, Object> request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        try {
            String username = getUsername(authentication, httpRequest);
            if (username == null) {
                return Result.error("未认证，请先登录");
            }
            
            String message = (String) request.get("message");
            String roleName = (String) request.get("roleName");
            String systemPrompt = (String) request.get("systemPrompt");
            
            if (message == null || message.trim().isEmpty()) {
                return Result.error("消息内容不能为空");
            }
            
            if (roleName == null || roleName.trim().isEmpty()) {
                return Result.error("角色名称不能为空");
            }
            
            // 获取用户ID
            Long userId = getUserIdFromUsername(username);
            
            // 检查角色知识库是否存在
            boolean hasRoleKb = userRoleKnowledgeBaseService.hasRoleKnowledgeBase(userId, roleName);
            if (!hasRoleKb) {
                return Result.error("该角色知识库不存在，请先创建角色知识库");
            }
            
            // 获取角色知识库信息
            var roleKb = userRoleKnowledgeBaseService.getUserRoleKnowledgeBase(userId, roleName);
            if (roleKb == null) {
                return Result.error("获取角色知识库信息失败");
            }
            
            String knowledgeBaseId = roleKb.getKnowledgeBaseId();
            log.info("使用角色知识库聊天 - 用户: {}, 角色: {}, 知识库ID: {}, 消息: {}", 
                    username, roleName, knowledgeBaseId, message);
            
            // 构建聊天请求
            ChatRequest chatRequest = new ChatRequest();
            chatRequest.setMessage(message);
            chatRequest.setDatasetId(knowledgeBaseId);
            chatRequest.setSystemPrompt(systemPrompt);
            
            // 调用FastGPT聊天服务
            ChatResponse chatResponse = fastGptService.chatWithKnowledgeBase(chatRequest);
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("reply", chatResponse.getReply());
            response.put("roleName", roleName);
            response.put("knowledgeBaseId", knowledgeBaseId);
            response.put("references", chatResponse.getReferences());
            
            log.info("角色知识库聊天成功 - 用户: {}, 角色: {}, 回复长度: {}", 
                    username, roleName, chatResponse.getReply().length());
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("角色知识库聊天失败", e);
            return Result.error("聊天失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户的所有角色列表（用于聊天时选择角色）
     */
    @GetMapping("/user-roles")
    public Result getUserRoles(Authentication authentication, HttpServletRequest request) {
        try {
            String username = getUsername(authentication, request);
            if (username == null) {
                return Result.error("未认证，请先登录");
            }
            
            // 获取用户ID
            Long userId = getUserIdFromUsername(username);
            
            // 获取用户的所有角色知识库
            var roleKnowledgeBases = userRoleKnowledgeBaseService.getUserRoleKnowledgeBases(userId);
            
            // 提取角色名称列表
            List<String> roles = roleKnowledgeBases.stream()
                    .map(roleKb -> roleKb.getRoleName())
                    .collect(java.util.stream.Collectors.toList());
            
            log.info("获取用户 {} 的角色列表: {}", username, roles);
            
            Map<String, Object> response = new HashMap<>();
            response.put("roles", roles);
            response.put("count", roles.size());
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("获取用户角色列表失败", e);
            return Result.error("获取角色列表失败: " + e.getMessage());
        }
    }
}