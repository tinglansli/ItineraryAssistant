package com.tinglans.backend.thirdparty.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QwenClient 测试
 * 测试真实的行程生成场景
 */
@SpringBootTest
class QwenClientTest {

    @Autowired
    private QwenClient qwenClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 测试真实的行程生成 - 大阪7日游
     */
    @Test
    void testRealTripGeneration() throws Exception {
        String systemPrompt = """
            你是一个专业的旅行规划助手。根据用户提供的目的地、天数、人数和预算信息，生成详细的旅行行程规划。
            
            【重要】你必须只返回纯JSON格式的数据，不要包含任何其他文字、解释或markdown代码块标记。
            直接从 { 开始，到 } 结束，确保是可以被JSON解析器直接解析的有效JSON。
            
            ===== 必须输出的完整 JSON 格式 =====
            {
              "tripName": "行程名称",
              "destination": "目的地",
              "startDate": "开始日期(yyyy-MM-dd格式)",
              "endDate": "结束日期(yyyy-MM-dd格式)",
              "days": [
                {
                  "dayIndex": 1,
                  "activities": [
                    {
                      "type": "transport/hotel/sight/food/other",
                      "title": "活动描述（如：参观伏见稻荷大社）",
                      "locationName": "地点名称（如：伏见稻荷大社）",
                      "startTime": "HH:mm",
                      "endTime": "HH:mm",
                      "estimatedCost": 预估费用（单位：分，100分=1元）
                    }
                  ]
                },
                {
                  "dayIndex": 2,
                  "activities": [...]
                }
              ]
            }
            
            ===== 关键字段说明 =====
            - tripName: 行程名称，建议反映主题和目的地
            - dayIndex: 从 1 开始的连续整数，不能跳过或重复
            - type: 只能使用这 5 个值: transport、hotel、sight、food、other
            - locationName: **能够在地图上找到的真实地点名称（真实餐厅名称、酒店名称、景点名称等）**
            - startTime/endTime: 24小时制, 格式为 HH:mm (如 09:00、14:30)
            - estimatedCost: 整数, 单位是分(1元=100分)
            
            ===== 生成行程的逻辑规则 =====
            1. 每天安排 3-5 个活动，确保时间分配合理且地理位置相近
            2. 第一天应该包含交通（从出发地到目的地）和酒店入住
            3. 最后一天应该包含交通返回（从目的地回到出发地）

            ===== 生成行程的格式规则 =====
            1. 必须生成指定天数的完整行程。如果用户说"7天", days 数组必须包含 1-7 天的所有数据
            2. 所有数值字段(dayIndex、estimatedCost)必须是数字类型，不能是字符串
            3. 返回的必须是可被标准 JSON 解析器解析的完整 JSON 对象，从 { 开始到 } 结束
            4. 不要添加注释

            **重要：生成完成后，检查输出是否符合格式规则。如果不符合则重新生成。**
            """;
        
        String userMessage = "\"我想今年国庆假期去福建厦门玩3天，2个大人1个小孩，预算15000块\"\n\n我的旅行偏好：美食、购物";

        System.out.println("用户输入: " + userMessage);
        
        String response = qwenClient.chat(systemPrompt, userMessage);

        System.out.println("大模型原始响应:");
        System.out.println(response);
        System.out.println("响应长度: " + response.length() + " 字符");
        
        // 验证是否为有效的 JSON
        JsonNode jsonNode = objectMapper.readTree(response);
        
        assertNotNull(jsonNode);
        assertTrue(jsonNode.has("tripName"), "缺少 tripName 字段");
        assertTrue(jsonNode.has("destination"), "缺少 destination 字段");
        assertTrue(jsonNode.has("startDate"), "缺少 startDate 字段");
        assertTrue(jsonNode.has("endDate"), "缺少 endDate 字段");
        assertTrue(jsonNode.has("days"), "缺少 days 字段");
        assertTrue(jsonNode.get("days").isArray(), "days 必须是数组");
        
        JsonNode daysArray = jsonNode.get("days");
        System.out.println("JSON 解析结果:");
        System.out.println("行程名称: " + jsonNode.get("tripName").asText());
        System.out.println("目的地: " + jsonNode.get("destination").asText());
        System.out.println("开始日期: " + jsonNode.get("startDate").asText());
        System.out.println("结束日期: " + jsonNode.get("endDate").asText());
        System.out.println("天数数组长度: " + daysArray.size());
        
        for (int i = 0; i < daysArray.size(); i++) {
            JsonNode day = daysArray.get(i);
            int dayIndex = day.has("dayIndex") ? day.get("dayIndex").asInt() : -1;
            int activitiesCount = day.has("activities") && day.get("activities").isArray() 
                ? day.get("activities").size() 
                : 0;
            
            System.out.println("\n第 " + (i+1) + " 天:");
            System.out.println("  dayIndex: " + dayIndex);
            System.out.println("  活动数量: " + activitiesCount);
            
            if (day.has("activities") && day.get("activities").isArray()) {
                JsonNode activities = day.get("activities");
                for (int j = 0; j < activities.size(); j++) {
                    JsonNode activity = activities.get(j);
                    System.out.println("    活动 " + (j+1) + ": " + 
                        activity.get("title").asText() + " (" + 
                        activity.get("type").asText() + ")");
                }
            }
        }
        
        // 验证天数是否正确（用户要求7天）
        assertEquals(3, daysArray.size(),
            "用户要求3天行程，但 days 数组只有 " + daysArray.size() + " 个元素！");
        
        System.out.println("\n✅ 测试通过！");
    }
}
