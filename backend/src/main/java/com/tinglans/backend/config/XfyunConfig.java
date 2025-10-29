package com.tinglans.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 科大讯飞语音识别配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "xfyun.asr")
public class XfyunConfig {

    /**
     * 应用ID
     */
    private String appId;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API密钥Secret
     */
    private String apiSecret;

    /**
     * 请求超时时间（毫秒）
     */
    private Integer timeout = 30000;

    /**
     * 轮询查询任务状态的间隔时间（毫秒）
     */
    private Integer pollInterval = 5000;

    /**
     * 轮询最大次数
     */
    private Integer maxPollCount = 60;
}
