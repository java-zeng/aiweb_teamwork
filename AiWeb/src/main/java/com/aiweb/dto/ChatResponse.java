package com.aiweb.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天响应DTO
 */
@Data
public class ChatResponse {
    
    /**
     * AI回复内容
     */
    private String reply;
    
    /**
     * 聊天会话ID
     */
    private String chatId;
    
    /**
     * 使用的数据集ID
     */
    private String datasetId;
    
    /**
     * 响应时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 引用的文档片段（可选）
     * 显示AI回答时参考了哪些知识库内容
     */
    private List<Reference> references;
    
    /**
     * 引用的文档片段信息
     */
    @Data
    public static class Reference {
        /**
         * 文档名称
         */
        private String documentName;
        
        /**
         * 文档片段内容
         */
        private String content;
        
        /**
         * 相似度分数
         */
        private Double score;
        
        /**
         * 文档ID
         */
        private String documentId;
    }
}
