package com.aiweb.dto.request;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 聊天请求DTO
 */
@Data
public class ChatRequest {
    
    /**
     * 用户消息内容
     */
    private String message;
    
    /**
     * 数据集ID，用于指定使用哪个知识库
     */
    private String datasetId;
    
    /**
     * 聊天历史记录（可选）
     * 用于支持多轮对话
     */
    private List<Map<String, String>> history;
    
    /**
     * 系统提示词（可选）
     * 用于自定义AI的行为
     */
    private String systemPrompt;
    
    /**
     * 温度参数（可选）
     * 控制AI回答的随机性，范围0-1
     */
    private Double temperature;
    
    /**
     * 最大token数（可选）
     * 限制AI回答的长度
     */
    private Integer maxTokens;
}
