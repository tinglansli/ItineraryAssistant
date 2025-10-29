package com.tinglans.backend.thirdparty.llm;

import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QwenClient测试
 */
@SpringBootTest
class QwenClientTest {

    @Autowired
    private QwenClient qwenClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 测试简单对话
     */
    @Test
    void testSimpleChat() {
        String response = qwenClient.chat("你好，请用一句话介绍一下你自己。");
        
        System.out.println("=== 测试简单对话 ===");
        System.out.println("回复: " + response);
        
        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    /**
     * 测试带系统提示的对话
     */
    @Test
    @Disabled("仅在需要时运行此测试")
    void testChatWithSystemPrompt() {
        String systemPrompt = "你是一个旅行规划专家，请用专业且友好的语气回答问题。";
        String userMessage = "推荐一个适合周末去的城市。";
        
        String response = qwenClient.chat(systemPrompt, userMessage);
        
        System.out.println("=== 测试带系统提示的对话 ===");
        System.out.println("系统提示: " + systemPrompt);
        System.out.println("用户消息: " + userMessage);
        System.out.println("回复: " + response);
        
        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    /**
     * 测试结构化 JSON 输出
     */
    @Test
    @Disabled("仅在需要时运行此测试")
    void testJsonOutput() throws Exception {
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
        
        String response = qwenClient.chat(systemPrompt, userMessage, true);
        
        System.out.println("=== 测试结构化 JSON 输出 ===");
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

    /**
     * 测试多轮对话
     */
    @Disabled("仅在需要时运行此测试")
    @Test
    void testMultiTurnChat() {
        List<Message> messages = new ArrayList<>();
        
        // 第一轮：系统消息
        messages.add(Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("你是一个旅行规划助手。")
                .build());
        
        // 第一轮：用户提问
        messages.add(Message.builder()
                .role(Role.USER.getValue())
                .content("我想去日本旅游，有什么推荐的城市吗？")
                .build());
        
        // 第一轮：助手回复（模拟）
        messages.add(Message.builder()
                .role(Role.ASSISTANT.getValue())
                .content("推荐东京、京都和大阪。东京现代繁华，京都古色古香，大阪美食丰富。")
                .build());
        
        // 第二轮：用户追问
        messages.add(Message.builder()
                .role(Role.USER.getValue())
                .content("京都有哪些必去的景点？")
                .build());
        
        String response = qwenClient.chatWithHistory(messages);
        
        System.out.println("=== 测试多轮对话 ===");
        System.out.println("回复: " + response);
        
        assertNotNull(response);
        assertFalse(response.isBlank());
        assertTrue(response.contains("京都") || response.contains("寺") || response.contains("神社"));
    }

    /**
     * 测试生成旅行行程规划 (核心功能)
     */
    @Test
    @Disabled("仅在需要时运行此测试")
    void testGenerateItinerary() throws Exception {
        String destination = "日本京都";
        int days = 3;
        String preferences = "喜欢历史文化，对寺庙和传统建筑感兴趣";
        
        String response = qwenClient.generateItinerary(destination, days, preferences);
        
        System.out.println("=== 测试生成旅行行程规划 ===");
        System.out.println("目的地: " + destination);
        System.out.println("天数: " + days);
        System.out.println("偏好: " + preferences);
        System.out.println("行程规划 JSON:");
        System.out.println(response);
        
        // 验证 JSON 结构
        JsonNode jsonNode = objectMapper.readTree(response);
        
        assertNotNull(jsonNode);
        assertTrue(jsonNode.has("tripName") || jsonNode.has("destination"));
        assertTrue(jsonNode.has("days"));
        
        JsonNode daysArray = jsonNode.get("days");
        assertTrue(daysArray.isArray());
        assertTrue(daysArray.size() > 0);
        
        // 验证第一天的活动
        JsonNode firstDay = daysArray.get(0);
        assertTrue(firstDay.has("dayIndex") || firstDay.has("day"));
        assertTrue(firstDay.has("activities"));
        
        JsonNode activities = firstDay.get("activities");
        assertTrue(activities.isArray());
        assertTrue(activities.size() > 0);
        
        // 验证第一个活动的字段（根据新的Activity结构）
        JsonNode firstActivity = activities.get(0);
        assertTrue(firstActivity.has("type"), "活动应该有 type 字段");
        assertTrue(firstActivity.has("title"), "活动应该有 title 字段");
        assertTrue(firstActivity.has("locationName"), "活动应该有 locationName 字段");
        assertTrue(firstActivity.has("address") || firstActivity.has("location"), "活动应该有 address 字段");
        assertTrue(firstActivity.has("lat"), "活动应该有 lat 字段");
        assertTrue(firstActivity.has("lng"), "活动应该有 lng 字段");
        assertTrue(firstActivity.has("estimatedCost"), "活动应该有 estimatedCost 字段");
        
        System.out.println("✅ 行程规划 JSON 结构验证通过");
        System.out.println("天数: " + daysArray.size());
        System.out.println("第一天活动数: " + activities.size());
        System.out.println("第一个活动类型: " + firstActivity.get("type").asText());
        System.out.println("第一个活动地点: " + firstActivity.get("locationName").asText());
    }

    /**
     * 测试结构化输出 - 支出分类建议
     */
    @Test
    @Disabled("仅在需要时运行此测试")
    void testExpenseCategorySuggestion() throws Exception {
        String systemPrompt = """
            你是一个智能支出分类助手。
            根据用户输入的消费描述，返回 JSON 格式的分类建议：
            {
              "category": "分类名称 (FOOD/TRANSPORT/ACCOMMODATION/ENTERTAINMENT/SHOPPING/OTHER)",
              "confidence": 置信度 (0-1),
              "reason": "分类理由"
            }
            """;
        
        String userMessage = "在拉面店吃了一碗拉面";
        
        String response = qwenClient.chat(systemPrompt, userMessage, true);
        
        System.out.println("=== 测试支出分类建议 ===");
        System.out.println("消费描述: " + userMessage);
        System.out.println("分类建议: " + response);
        
        // 验证 JSON 结构
        JsonNode jsonNode = objectMapper.readTree(response);
        
        assertNotNull(jsonNode);
        assertTrue(jsonNode.has("category"));
        assertTrue(jsonNode.has("confidence") || jsonNode.has("reason"));
        
        String category = jsonNode.get("category").asText();
        assertEquals("FOOD", category);
        
        System.out.println("✅ 支出分类验证通过");
        System.out.println("分类: " + category);
    }
}
