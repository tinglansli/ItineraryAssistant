package com.tinglans.backend.thirdparty.llm;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinglans.backend.config.LlmConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 千问 LLM 客户端
 */
@Slf4j
@Component
public class QwenClient {

    private final LlmConfig config;
    private final Generation generation;
    private final ObjectMapper objectMapper;

    public QwenClient(LlmConfig config) {
        this.config = config;
        this.generation = new Generation();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 简单对话
     *
     * @param userMessage 用户消息
     * @return LLM 回复内容
     */
    public String chat(String userMessage) {
        return chat(null, userMessage, false);
    }

    /**
     * 带系统提示的对话
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @return LLM 回复内容
     */
    public String chat(String systemPrompt, String userMessage) {
        return chat(systemPrompt, userMessage, false);
    }

    /**
     * 对话（支持结构化输出）
     *
     * @param systemPrompt     系统提示词
     * @param userMessage      用户消息
     * @param enableJsonOutput 是否启用 JSON 输出
     * @return LLM 回复内容
     */
    public String chat(String systemPrompt, String userMessage, boolean enableJsonOutput) {
        log.info("发送 LLM 请求 - 模型: {}, JSON输出: {}", config.getModel(), enableJsonOutput);

        try {
            List<Message> messages = new ArrayList<>();

            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Message.builder()
                        .role(Role.SYSTEM.getValue())
                        .content(systemPrompt)
                        .build());
            }

            messages.add(Message.builder()
                    .role(Role.USER.getValue())
                    .content(userMessage)
                    .build());

            GenerationParam.GenerationParamBuilder<?, ?> paramBuilder = GenerationParam.builder()
                    .apiKey(config.getApiKey())
                    .model(config.getModel())
                    .messages(messages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE);

            if (config.getMaxTokens() != null) {
                paramBuilder.maxTokens(config.getMaxTokens());
            }
            if (config.getTemperature() != null) {
                paramBuilder.temperature(config.getTemperature().floatValue());
            }
            if (config.getTopP() != null) {
                paramBuilder.topP(config.getTopP());
            }
            if (config.getEnableSearch() != null && config.getEnableSearch()) {
                paramBuilder.enableSearch(true);
            }

            GenerationParam param = paramBuilder.build();
            GenerationResult result = generation.call(param);
            String response = result.getOutput().getChoices().get(0).getMessage().getContent();
            
            if (enableJsonOutput) {
                response = cleanJsonResponse(response);
            }
            
            log.info("LLM 响应成功 - Token: {}/{}", 
                     result.getUsage().getInputTokens(),
                     result.getUsage().getTotalTokens());

            return response;

        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            log.error("LLM API 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("LLM 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 多轮对话
     *
     * @param messages 消息历史
     * @return LLM 回复内容
     */
    public String chatWithHistory(List<Message> messages) {
        return chatWithHistory(messages, false);
    }

    /**
     * 多轮对话（支持结构化输出）
     *
     * @param messages         消息历史
     * @param enableJsonOutput 是否启用 JSON 输出
     * @return LLM 回复内容
     */
    public String chatWithHistory(List<Message> messages, boolean enableJsonOutput) {
        log.info("发送多轮对话请求 - 模型: {}, 消息数: {}", config.getModel(), messages.size());

        try {
            GenerationParam.GenerationParamBuilder<?, ?> paramBuilder = GenerationParam.builder()
                    .apiKey(config.getApiKey())
                    .model(config.getModel())
                    .messages(messages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE);

            if (config.getMaxTokens() != null) {
                paramBuilder.maxTokens(config.getMaxTokens());
            }
            if (config.getTemperature() != null) {
                paramBuilder.temperature(config.getTemperature().floatValue());
            }
            if (config.getTopP() != null) {
                paramBuilder.topP(config.getTopP());
            }

            GenerationParam param = paramBuilder.build();
            GenerationResult result = generation.call(param);
            String response = result.getOutput().getChoices().get(0).getMessage().getContent();
            
            log.info("多轮对话响应成功");

            return response;

        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            log.error("多轮对话 API 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("LLM 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成旅行行程规划
     *
     * @param destination 目的地
     * @param days        天数
     * @param preferences 用户偏好
     * @return JSON 格式的行程规划
     */
    public String generateItinerary(String destination, int days, String preferences) {
        String systemPrompt = """
            你是一个专业的旅行规划助手。
            你需要根据用户提供的目的地、天数和偏好，生成详细的旅行行程规划。
            请以 JSON 格式返回行程，包含以下结构：
            {
              "tripName": "行程名称",
              "destination": "目的地",
              "days": [
                {
                  "dayIndex": 1,
                  "activities": [
                    {
                      "type": "活动类型(sight/food/hotel/transport)",
                      "title": "活动描述（如：参观伏见稻荷大社）",
                      "locationName": "地点关键词（如：伏见稻荷大社）",
                      "startTime": "开始时间(HH:mm)",
                      "endTime": "结束时间(HH:mm)",
                      "estimatedCost": 预估费用（单位：分，100分=1元）
                    }
                  ]
                }
              ]
            }
            
            重要说明：
            1. locationName 是用于地图搜索的关键词，必须准确（如景点名、餐厅名、酒店名）
            2. title 是对活动的描述，可以更详细生动
            3. 费用单位是"分"（1元 = 100分）
            4. 每天安排3-5个活动，时间分配合理
            """;

        String userMessage = String.format(
                "请为我规划一个 %s 的 %d 天旅行行程。%s",
                destination,
                days,
                preferences != null ? "我的偏好：" + preferences : ""
        );

        return chat(systemPrompt, userMessage, true);
    }

    /**
     * 清理 JSON 响应中的 Markdown 代码块标记
     *
     * @param response 原始响应
     * @return 清理后的 JSON 字符串
     */
    private String cleanJsonResponse(String response) {
        if (response == null) {
            return null;
        }
        
        // 移除 Markdown 代码块标记
        response = response.trim();
        
        // 移除 ```json 开头
        if (response.startsWith("```json")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }
        
        // 移除 ``` 结尾
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        
        return response.trim();
    }
}
