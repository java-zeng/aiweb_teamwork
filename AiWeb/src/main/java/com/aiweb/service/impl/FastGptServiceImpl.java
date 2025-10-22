package com.aiweb.service.impl;

import com.aiweb.dto.ChatResponse;
import com.aiweb.dto.request.ChatRequest;
import com.aiweb.entity.FastGptApiData;
import com.aiweb.entity.FastGptApiResponse;
import com.aiweb.service.FastGptService;
import com.aiweb.utils.FilenameEncodingUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.aiweb.mapper.UserMapper;
import com.aiweb.entity.User;


@Slf4j
@Service
public class FastGptServiceImpl implements FastGptService {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;


    @Value("${fastgpt.api.baseUrl}")
    private String FastGptBaseUrl;

    @Value("${fastgpt.api.key}")
    private String fastGptApiKey;
    
    @Value("${fastgpt.api.appId}")
    private String fastGptAppId;

    @Value("${fastgpt.app.key}")
    private String fastGptAppKey;

    @Autowired
    private UserMapper userMapper;

    @Value("${fastgpt.api.dataset.create:/api/core/dataset/create}")
    private String datasetCreatePath;

    @Value("${fastgpt.api.dataset.delete:/api/core/dataset/delete}")
    private String datasetDeletePath;

    public FastGptApiResponse CreateListFromFiles(MultipartFile multipartFile, FastGptApiData fastGptApiData) throws JsonProcessingException {
        //1.构造目标URl
        String curlUrl=FastGptBaseUrl+"/api/core/dataset/collection/create/localFile";
        //2.设置http请求头
        HttpHeaders httpHeaders = new HttpHeaders();
        //指定传输的类型：file+data(文件传输模式)
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        //添加需要的Bearer Token认证头部
        httpHeaders.setBearerAuth(fastGptApiKey);
        
        // 获取正确的文件名（解决编码问题）
        String correctFilename = FilenameEncodingUtils.getCorrectFilename(multipartFile);
        log.info("FastGPT上传 - 原始文件名: {}, 修复后文件名: {}", 
                multipartFile.getOriginalFilename(), correctFilename);
        
        // 确保传递给FastGPT的文件名编码正确
        String fastgptFilename = ensureFastGptFilenameEncoding(correctFilename);
        log.info("FastGPT最终文件名: {}", fastgptFilename);
        
        //3.构建 multipart/form-data 请求体
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        //3.1添加file字段 - 使用自定义的Resource包装器确保文件名编码正确
        body.add("file", createCustomFileResource(multipartFile, fastgptFilename));
        //3.2添加data字段
        String jsonData = objectMapper.writeValueAsString(fastGptApiData);
        body.add("data",jsonData);
        //4.将header和body组成一个HttpEntity
        HttpEntity<LinkedMultiValueMap<String, Object>> httpEntity = new HttpEntity<>(body, httpHeaders);
        log.info("准备向Fastgpt发送请求");
        log.info("请求地址为:{}",curlUrl);
        log.info("请求数据部分为:{}",jsonData);
        try {
            ResponseEntity<FastGptApiResponse> response = restTemplate.exchange(curlUrl, HttpMethod.POST, httpEntity, FastGptApiResponse.class);
            //处理响应
            FastGptApiResponse responseBody = response.getBody();
            if(responseBody==null||responseBody.getCode()!=200)
            {
                log.error("FASTGPT调用失败，失败响应为:{}",responseBody);
                throw new RuntimeException("FASTGPT API调用失败");
            }
            log.info("成功上传文件到FASTGPT APi并创建了文件集合,CollectionId:{}",responseBody.getData().getCollectionId());
            return responseBody;
        }catch (HttpClientErrorException e)
        {
            log.error("调用FASTGPT API时发生HTTP客户端请求错误:{}",e.getStatusCode());
            throw new RuntimeException("请求FASTGPT APi失败，请检查参数或者APi KEY");
        }
    }

    @Override
    public String ensureUserDataset(String username) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            throw new RuntimeException("用户不存在: " + username);
        }
        
        // 如果用户已有数据集ID，直接复用（若 FastGPT 已被手动删除，将在调用上传时自动重试并重建）
        if (user.getFastgptDatasetId() != null && !user.getFastgptDatasetId().isEmpty()) {
            return user.getFastgptDatasetId();
        }
        // 调用 FastGPT 创建数据集
        String url = FastGptBaseUrl + datasetCreatePath;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(fastGptApiKey);
        // 简化：用 username 作为数据集名称
        String payload = "{\"name\":\"" + username + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            String body = resp.getBody();
            if (body == null) {
                throw new RuntimeException("创建数据集失败：响应为空");
            }
            // 兼容多种返回结构
            var root = objectMapper.readTree(body);
            String datasetId = null;
            if (root.has("data")) {
                var dataNode = root.get("data");
                if (dataNode.isTextual()) {
                    datasetId = dataNode.asText();
                } else {
                    datasetId = dataNode.path("datasetId").asText(null);
                    if (datasetId == null || datasetId.isEmpty()) datasetId = dataNode.path("id").asText(null);
                    if (datasetId == null || datasetId.isEmpty()) datasetId = dataNode.path("_id").asText(null);
                }
            } else {
                // 有些实现可能直接返回 {id: "..."}
                datasetId = root.path("datasetId").asText(null);
                if (datasetId == null || datasetId.isEmpty()) datasetId = root.path("id").asText(null);
                if (datasetId == null || datasetId.isEmpty()) datasetId = root.path("_id").asText(null);
            }
            if (datasetId == null || datasetId.isEmpty()) {
                log.error("创建数据集返回原文: {}", body);
                throw new RuntimeException("创建数据集失败，未返回 datasetId");
            }
            user.setFastgptDatasetId(datasetId);
            userMapper.updateById(user);
            return datasetId;
        } catch (Exception e) {
            log.error("创建 FastGPT 数据集失败", e);
            throw new RuntimeException("创建 FastGPT 数据集失败: " + e.getMessage());
        }
    }


    @Override
    public String createDataset(String datasetName) {
        try {
            log.info("尝试创建FastGPT数据集: {}", datasetName);

            if (!checkFastGptHealth()) {
                throw new RuntimeException("FastGPT服务不可用，无法创建数据集");
            }

            String url = FastGptBaseUrl + datasetCreatePath;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(fastGptApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("name", datasetName);
            requestBody.put("avatar", "/icon/dataset.png");
            requestBody.put("vectorModel", "text-embedding-ada-002"); // 默认向量模型
            requestBody.put("agentModel", "gpt-3.5-turbo"); // 默认Agent模型

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            String responseBody = response.getBody();

            if (responseBody == null) {
                throw new RuntimeException("创建数据集响应为空");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            log.info("FastGPT API响应内容: {}", responseBody);
            log.info("解析后的JSON根节点: {}", root);
            
            if (root.path("code").asInt() != 200) {
                String errorMsg = root.path("message").asText("未知错误");
                log.error("创建FastGPT数据集失败: {}", errorMsg);
                throw new RuntimeException("创建FastGPT数据集失败: " + errorMsg);
            }

            // 检查data节点是否存在
            JsonNode dataNode = root.path("data");
            log.info("data节点: {}", dataNode);
            
            // FastGPT API返回的data字段直接包含数据集ID
            String datasetId = null;
            if (dataNode.isTextual()) {
                // data字段是字符串，直接使用
                datasetId = dataNode.asText();
                log.info("从data字段直接获取ID: {}", datasetId);
            } else if (dataNode.isObject()) {
                // data字段是对象，尝试不同的字段名
                if (dataNode.has("datasetId")) {
                    datasetId = dataNode.path("datasetId").asText();
                    log.info("从data.datasetId字段获取ID: {}", datasetId);
                } else if (dataNode.has("id")) {
                    datasetId = dataNode.path("id").asText();
                    log.info("从data.id字段获取ID: {}", datasetId);
                } else if (dataNode.has("dataset_id")) {
                    datasetId = dataNode.path("dataset_id").asText();
                    log.info("从data.dataset_id字段获取ID: {}", datasetId);
                } else {
                    log.error("无法找到数据集ID字段，data节点内容: {}", dataNode);
                    // 打印所有字段名
                    dataNode.fieldNames().forEachRemaining(fieldName -> 
                        log.info("data节点中的字段: {} = {}", fieldName, dataNode.path(fieldName))
                    );
                }
            } else {
                log.error("data节点既不是字符串也不是对象: {}", dataNode);
            }
            
            log.info("成功创建FastGPT数据集: {}，ID: {}", datasetName, datasetId);
            return datasetId;

        } catch (HttpClientErrorException e) {
            log.error("调用FastGPT创建数据集API时发生HTTP错误: {}", e.getStatusCode(), e);
            log.error("错误响应体: {}", e.getResponseBodyAsString());
            throw new RuntimeException("创建FastGPT数据集失败: HTTP " + e.getStatusCode().value() + " - " + e.getMessage());
        } catch (Exception e) {
            log.error("创建FastGPT数据集过程中发生异常", e);
            throw new RuntimeException("创建FastGPT数据集失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteDataset(String datasetId) {
        String url = FastGptBaseUrl + datasetDeletePath;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(fastGptApiKey);
        String payload = "{\"datasetId\":\"" + datasetId + "\"}";
        try {
            restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
        } catch (Exception e) {
            log.warn("删除 FastGPT 数据集失败: {}", e.getMessage());
        }
    }
    
    /**
     * 确保传递给FastGPT的文件名编码正确
     * 专门处理FastGPT可能存在的编码问题
     */
    private String ensureFastGptFilenameEncoding(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "document_" + System.currentTimeMillis();
        }
        
        try {
            // 方法1: 尝试UTF-8编码，确保FastGPT能正确识别
            byte[] utf8Bytes = filename.getBytes(StandardCharsets.UTF_8);
            String utf8Filename = new String(utf8Bytes, StandardCharsets.UTF_8);
            
            // 检查是否包含中文字符且没有乱码
            if (containsChineseCharacters(utf8Filename) && !containsGarbledCharacters(utf8Filename)) {
                log.info("UTF-8编码文件名: {} -> {}", filename, utf8Filename);
                return utf8Filename;
            }
            
            // 方法2: 尝试GBK编码（FastGPT可能偏好GBK）
            try {
                byte[] gbkBytes = filename.getBytes("GBK");
                String gbkFilename = new String(gbkBytes, "GBK");
                
                if (containsChineseCharacters(gbkFilename) && !containsGarbledCharacters(gbkFilename)) {
                    log.info("GBK编码文件名: {} -> {}", filename, gbkFilename);
                    return gbkFilename;
                }
            } catch (Exception e) {
                log.warn("GBK编码失败: {}", e.getMessage());
            }
            
            // 方法3: 如果包含中文字符，尝试URL编码
            if (containsChineseCharacters(filename)) {
                try {
                    String urlEncoded = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8);
                    log.info("URL编码文件名: {} -> {}", filename, urlEncoded);
                    return urlEncoded;
                } catch (Exception e) {
                    log.warn("URL编码失败: {}", e.getMessage());
                }
            }
            
            // 方法4: 如果都失败，使用安全的ASCII文件名
            String safeFilename = generateSafeAsciiFilename(filename);
            log.warn("使用安全ASCII文件名: {} -> {}", filename, safeFilename);
            return safeFilename;
            
        } catch (Exception e) {
            log.error("FastGPT文件名编码处理失败: {}", e.getMessage(), e);
            return filename;
        }
    }
    
    /**
     * 检查是否包含中文字符
     */
    private boolean containsChineseCharacters(String text) {
        if (text == null) return false;
        
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) { // 中文字符范围
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否包含乱码字符
     */
    private boolean containsGarbledCharacters(String text) {
        if (text == null) return true;
        
        return text.contains("?") || 
               text.contains("") || 
               text.contains("æ") || 
               text.contains("å") || 
               text.contains("è") ||
               text.contains("é") ||
               text.contains("®") ||
               text.contains("¡") ||
               text.contains("½");
    }
    
    /**
     * 生成安全的ASCII文件名
     */
    private String generateSafeAsciiFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "document_" + System.currentTimeMillis();
        }
        
        // 提取文件扩展名
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
            extension = originalFilename.substring(lastDotIndex);
        }
        
        // 生成安全的ASCII文件名
        String safeName = "document_" + System.currentTimeMillis() + extension;
        log.info("生成安全ASCII文件名: {} -> {}", originalFilename, safeName);
        return safeName;
    }
    
    /**
     * 创建自定义文件资源，确保文件名编码正确
     */
    private Resource createCustomFileResource(MultipartFile multipartFile, String correctFilename) {
        try {
            log.info("创建自定义文件资源 - 原始文件名: {}, 正确文件名: {}", 
                    multipartFile.getOriginalFilename(), correctFilename);
            
            // 创建一个自定义的InputStreamResource，使用修复后的文件名
            return new InputStreamResource(multipartFile.getInputStream()) {
                @Override
                public String getFilename() {
                    // 返回修复后的正确文件名
                    return correctFilename;
                }
                
                @Override
                public long contentLength() throws IOException {
                    return multipartFile.getSize();
                }
            };
        } catch (IOException e) {
            log.error("创建自定义文件资源失败: {}", e.getMessage(), e);
            // 如果失败，回退到原始方式
            return multipartFile.getResource();
        }
    }
    
    @Override
    public ChatResponse chatWithKnowledgeBase(ChatRequest chatRequest) throws JsonProcessingException {
        try {
            log.info("开始与FastGPT聊天，数据集ID: {}, 消息: {}", chatRequest.getDatasetId(), chatRequest.getMessage());
            
            // 首先检查FastGPT服务是否可用
            if (!checkFastGptHealth()) {
                throw new RuntimeException("FastGPT服务不可用，请检查服务是否正常运行在 " + FastGptBaseUrl);
            }
            
            // 构建聊天请求URL - 尝试使用应用ID的API路径
            String chatUrl = FastGptBaseUrl + "/api/v1/chat/completions";
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(fastGptAppKey);
            
            // 构建请求体 - 根据您的应用配置调整
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chatId", generateChatId());
            requestBody.put("stream", false);
            requestBody.put("detail", true);
            requestBody.put("responseChatItemId", generateChatId() + "_response");
            
            // 构建变量
            Map<String, String> variables = new HashMap<>();
            variables.put("uid", "user_" + System.currentTimeMillis());
            variables.put("name", "用户");
            requestBody.put("variables", variables);
            
            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统提示词（如果有）
            if (chatRequest.getSystemPrompt() != null && !chatRequest.getSystemPrompt().trim().isEmpty()) {
                Map<String, String> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", chatRequest.getSystemPrompt());
                messages.add(systemMessage);
            }
            
            // 添加用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", chatRequest.getMessage());
            messages.add(userMessage);
            
            requestBody.put("messages", messages);

            // 动态知识库选择 (保持开启)
            if (chatRequest.getDatasetId() != null && !chatRequest.getDatasetId().trim().isEmpty()) {
                Map<String, Object> datasetConfig = new HashMap<>();
                datasetConfig.put("datasets", Arrays.asList(chatRequest.getDatasetId()));
                datasetConfig.put("similarity", 0.4);
                datasetConfig.put("limit", 3000);
                datasetConfig.put("searchMode", "embedding");
                datasetConfig.put("usingReRank", true);
                datasetConfig.put("rerankWeight", 0.5);
                datasetConfig.put("datasetSearchUsingExtensionQuery", true);
                requestBody.put("datasetConfig", datasetConfig);
                log.info("已动态指定知识库: {}", chatRequest.getDatasetId());
            }
            
            // 发送请求
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("发送聊天请求到FastGPT: {}", chatUrl);
            log.info("请求体: {}", objectMapper.writeValueAsString(requestBody));
            
            ResponseEntity<String> response = restTemplate.postForEntity(chatUrl, entity, String.class);
            String responseBody = response.getBody();
            
            log.info("FastGPT聊天响应: {}", responseBody);
            
            if (responseBody == null) {
                throw new RuntimeException("FastGPT聊天响应为空");
            }
            
            // 解析响应
            JsonNode root = objectMapper.readTree(responseBody);

            if (root.has("error")) {
                String errorMsg = root.path("error").path("message").asText("未知API错误");
                log.error("FastGPT聊天失败: {}", errorMsg);
                throw new RuntimeException("FastGPT聊天失败: " + errorMsg);
            }

            // 检查 choices 数组
            if (!root.has("choices") || !root.path("choices").isArray() || root.path("choices").isEmpty()) {
                log.error("FastGPT响应格式错误，缺少 'choices' 数组: {}", responseBody);
                throw new RuntimeException("FastGPT响应格式错误: " + responseBody);
            }

            JsonNode firstChoice = root.path("choices").get(0);
            JsonNode contentNode = firstChoice.path("message").path("content");

            String reply;
            if (contentNode.isTextual()) {
                // 兼容没有工具的旧格式
                reply = contentNode.asText();
            } else if (contentNode.isArray()) {
                // 遍历包含工具和文本的新格式
                StringBuilder replyBuilder = new StringBuilder();
                for (JsonNode part : contentNode) {
                    if ("text".equals(part.path("type").asText())) {
                        replyBuilder.append(part.path("text").path("content").asText());
                    }
                }
                reply = replyBuilder.toString();
            } else {
                reply = "无法解析的回复格式";
                log.warn("未知的回复 content 格式: {}", contentNode.toString());
            }

            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setChatId(root.path("id").asText(generateChatId()));
            chatResponse.setDatasetId(chatRequest.getDatasetId());
            chatResponse.setTimestamp(LocalDateTime.now());
            chatResponse.setReply(reply);

            // --- 新的引用解析逻辑 ---
            List<ChatResponse.Reference> references = new ArrayList<>();
            if (root.has("responseData") && root.path("responseData").isArray()) {
                for (JsonNode node : root.path("responseData")) {
                    // 找到知识库搜索模块
                    if ("datasetSearchNode".equals(node.path("moduleType").asText())) {
                        if (node.has("quoteList") && node.path("quoteList").isArray()) {
                            for (JsonNode quote : node.path("quoteList")) {
                                ChatResponse.Reference reference = new ChatResponse.Reference();
                                reference.setContent(quote.path("q").asText());
                                reference.setScore(quote.path("score").asDouble());
                                reference.setDocumentId(quote.path("id").asText());
                                reference.setDocumentName(quote.path("source").asText());
                                references.add(reference);
                            }
                        }
                        break; // 已经找到知识库模块，跳出循环
                    }
                }
            }
            chatResponse.setReferences(references);
            // --- 引用解析结束 ---
            
            log.info("聊天完成，回复: {}", chatResponse.getReply());
            return chatResponse;
            
        } catch (HttpClientErrorException e) {
            log.error("调用FastGPT聊天API时发生HTTP错误: {}", e.getStatusCode(), e);
            log.error("错误响应体: {}", e.getResponseBodyAsString());
            
            // 检查是否是404错误，提供更详细的错误信息
            if (e.getStatusCode().value() == 404) {
                throw new RuntimeException("FastGPT API路径不存在，请检查FastGPT服务是否正常运行在 " + FastGptBaseUrl + " 端口，以及API路径是否正确");
            } else if (e.getStatusCode().value() == 401) {
                throw new RuntimeException("FastGPT API密钥无效，请检查配置中的API密钥是否正确");
            } else if (e.getStatusCode().value() == 500) {
                throw new RuntimeException("FastGPT服务器内部错误，请检查FastGPT服务状态");
            } else {
                throw new RuntimeException("调用FastGPT聊天API失败: HTTP " + e.getStatusCode().value() + " - " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("聊天过程中发生异常", e);
            throw new RuntimeException("聊天失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成聊天ID
     */
    private String generateChatId() {
        return "chat_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 检查FastGPT服务是否可用
     */
    public boolean checkFastGptHealth() {
        try {
            // 尝试访问FastGPT的根路径来检查服务是否可用
            String healthUrl = FastGptBaseUrl + "/";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(fastGptApiKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(healthUrl, HttpMethod.GET, entity, String.class);
            
            log.info("FastGPT健康检查响应: {}", response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            log.error("FastGPT健康检查失败: {}", e.getMessage());
            return false;
        }
    }
}
