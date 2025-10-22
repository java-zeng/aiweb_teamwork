package com.aiweb;

import com.aiweb.dto.ChatResponse;
import com.aiweb.dto.request.ChatRequest;
import com.aiweb.service.FastGptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class FastGptChatTest {

    @Autowired
    private FastGptService fastGptService;

    @Test
    public void testChatWithKnowledgeBase() {
        try {
            // 创建聊天请求
            ChatRequest chatRequest = new ChatRequest();
            chatRequest.setMessage("你好，请介绍一下你自己");
            chatRequest.setDatasetId("test-dataset-id");
            chatRequest.setSystemPrompt("你是一个智能助手，请用中文回答用户的问题。");
            chatRequest.setTemperature(0.7);
            chatRequest.setMaxTokens(1000);

            // 调用聊天服务
            ChatResponse response = fastGptService.chatWithKnowledgeBase(chatRequest);

            // 验证响应
            assertNotNull(response);
            assertNotNull(response.getReply());
            assertNotNull(response.getChatId());
            assertNotNull(response.getDatasetId());
            assertNotNull(response.getTimestamp());

            System.out.println("聊天测试成功！");
            System.out.println("回复内容: " + response.getReply());
            System.out.println("聊天ID: " + response.getChatId());
            System.out.println("数据集ID: " + response.getDatasetId());

        } catch (Exception e) {
            System.err.println("聊天测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
