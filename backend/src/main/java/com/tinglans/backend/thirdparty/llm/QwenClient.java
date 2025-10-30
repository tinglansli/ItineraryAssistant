package com.tinglans.backend.thirdparty.llm;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.tinglans.backend.config.LlmConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 千问 LLM 客户端
 * 职责：封装阿里云百炼 API，提供通用的 LLM 调用接口
 */
@Slf4j
@Component
public class QwenClient {

    private final LlmConfig config;
    private final Generation generation;

    public QwenClient(LlmConfig config) {
        this.config = config;
        this.generation = new Generation();
    }

    /**
     * 对话接口
     *
     * @param systemPrompt     系统提示词
     * @param userMessage      用户消息
     * @return LLM 回复内容
     */
    public String chat(String systemPrompt, String userMessage) {
        log.info("发送 LLM 请求 - 模型: {}", config.getModel());

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
            
            response = response.trim();
            
            log.info("LLM 响应成功 - Token: {}/{}", 
                     result.getUsage().getInputTokens(),
                     result.getUsage().getTotalTokens());

            return response;

        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            log.error("LLM API 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("LLM 调用失败: " + e.getMessage(), e);
        }
    }
}
