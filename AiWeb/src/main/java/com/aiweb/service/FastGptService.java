package com.aiweb.service;

import com.aiweb.dto.ChatResponse;
import com.aiweb.dto.request.ChatRequest;
import com.aiweb.entity.FastGptApiData;
import com.aiweb.entity.FastGptApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.multipart.MultipartFile;

public interface FastGptService {
    public FastGptApiResponse CreateListFromFiles(MultipartFile multipartFile, FastGptApiData fastGptApiData) throws JsonProcessingException;

    /**
     * 确保为指定用户名存在一个数据集，不存在则在 FastGPT 端创建并返回 datasetId。
     */
    String ensureUserDataset(String username);

    /**
     * 创建数据集
     * @param datasetName 数据集名称
     * @return 数据集ID
     */
    String createDataset(String datasetName);

    /**
     * 删除指定数据集（用于用户注销时清理）。
     */
    void deleteDataset(String datasetId);
    
    /**
     * 与FastGPT进行聊天对话，使用指定的知识库
     * @param chatRequest 聊天请求
     * @return 聊天响应
     */
    ChatResponse chatWithKnowledgeBase(ChatRequest chatRequest) throws JsonProcessingException;
    
    /**
     * 检查FastGPT服务是否可用
     * @return true如果服务可用，false否则
     */
    boolean checkFastGptHealth();
}
