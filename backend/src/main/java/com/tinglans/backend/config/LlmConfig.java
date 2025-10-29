package com.tinglans.backend.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LLM 配置类
 */
@Getter
@Configuration
@ConfigurationProperties(prefix = "aliyun.llm")
public class LlmConfig {

    private String apiKey;
    private String model;
    private Integer maxTokens;
    private Double temperature;
    private Double topP;
    private Boolean enableSearch;

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public void setEnableSearch(Boolean enableSearch) {
        this.enableSearch = enableSearch;
    }
}
