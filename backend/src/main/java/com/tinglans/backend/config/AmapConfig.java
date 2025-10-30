package com.tinglans.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 高德地图配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "amap")
public class AmapConfig {
    
    /**
     * 高德地图 API Key
     */
    private String apiKey;
    
    /**
     * 高德地图 API 基础 URL
     */
    private String baseUrl = "https://restapi.amap.com";
    
    /**
     * 搜索 API 路径
     */
    private String searchPath = "/v5/place/text";
    
    /**
     * 搜索超时时间（秒）
     */
    private Integer timeout = 10;
    
    /**
     * 搜索返回结果数量
     */
    private Integer pageSize = 1;
}
