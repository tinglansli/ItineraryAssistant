package com.tinglans.backend.thirdparty.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QwenClient 测试
 * 测试通用 LLM 调用接口
 */
@SpringBootTest
class QwenClientTest {

    @Autowired
    private QwenClient qwenClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 测试对话
     */
    @Test
    void testChatWithJsonOutput() throws Exception {
        String systemPrompt = """
            你是一个数据分析助手。
            请以 JSON 格式返回城市信息，格式如下：
            {
              "city": "城市名称",
              "country": "国家",
              "population": 人口数量,
              "famous_for": ["特色1", "特色2", "特色3"]
            }
            """;
        
        String userMessage = "请提供东京的城市信息";
        
        String response = qwenClient.chat(systemPrompt, userMessage);
        
        System.out.println("=== 测试 JSON 输出 ===");
        System.out.println("回复: " + response);
        
        // 验证是否为有效的 JSON
        JsonNode jsonNode = objectMapper.readTree(response);
        
        assertNotNull(jsonNode);
        assertTrue(jsonNode.has("city"));
        assertTrue(jsonNode.has("country"));
        
        System.out.println("✅ JSON 格式验证通过");
        System.out.println("城市: " + jsonNode.get("city").asText());
        System.out.println("国家: " + jsonNode.get("country").asText());
    }
}
