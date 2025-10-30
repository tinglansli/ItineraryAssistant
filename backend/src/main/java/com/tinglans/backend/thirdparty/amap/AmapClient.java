package com.tinglans.backend.thirdparty.amap;

import com.tinglans.backend.config.AmapConfig;
import com.tinglans.backend.thirdparty.amap.dto.AmapPoi;
import com.tinglans.backend.thirdparty.amap.dto.AmapSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * 高德地图客户端
 * 职责：封装高德地图 API，提供地点搜索功能
 */
@Slf4j
@Component
public class AmapClient {
    
    private final AmapConfig config;
    private final WebClient webClient;
    
    /**
     * 上一次请求的时间戳（毫秒）
     * 用于控制请求频率，避免超过高德 API 的 QPS 限制（3次/秒）
     */
    private long lastRequestTime = 0;
    
    /**
     * 请求最小间隔（毫秒）
     * 设置为 340ms，确保不超过 3次/秒的限制（1000 / 3 ≈ 333ms）
     */
    private static final long MIN_REQUEST_INTERVAL = 340;
    
    public AmapClient(AmapConfig config, WebClient.Builder webClientBuilder) {
        this.config = config;
        this.webClient = webClientBuilder
                .baseUrl(config.getBaseUrl())
                .build();
    }
    
    /**
     * 搜索地点
     *
     * @param keywords 搜索关键词（地点名称）
     * @return 首个匹配的 POI
     */
    public AmapPoi searchLocation(String keywords) {
        return searchLocation(keywords, null);
    }

    /**
     * 指定区划内搜索地点
     *
     * @param keywords 搜索关键词
     * @param region   搜索区划
     * @return 首个匹配的 POI
     */
    public AmapPoi searchLocation(String keywords, String region) {
        // 执行频率限制
        throttleRequest();
        
        if (keywords == null || keywords.trim().isEmpty()) {
            log.warn("搜索关键词为空");
            return null;
        }

        // 检查关键词长度
        String finalKeywords = keywords;
        if (keywords.length() > 80) {
            log.warn("搜索关键词过长: keywords={}, length={}", keywords, keywords.length());
            finalKeywords = keywords.substring(0, 80);
        }

        try {
            log.debug("开始搜索地点: keywords={}, region={}", finalKeywords, region);

            final String fk = finalKeywords;
            final String finalRegion = region;
            
            AmapSearchResponse response = webClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path(config.getSearchPath())
                                .queryParam("key", config.getApiKey())
                                .queryParam("keywords", fk)
                                .queryParam("page_size", config.getPageSize());
                        
                        // 如果提供了 region，添加区域限制参数
                        if (finalRegion != null && !finalRegion.isBlank()) {
                            builder = builder.queryParam("region", finalRegion)
                                    .queryParam("city_limit", "true");
                        }
                        
                        return builder.build();
                    })
                    .retrieve()
                    .bodyToMono(AmapSearchResponse.class)
                    .timeout(Duration.ofSeconds(config.getTimeout()))
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
                    .block();

            if (response == null) {
                log.warn("高德地图 API 返回空响应: keywords={}", finalKeywords);
                return null;
            }

            if (!response.isSuccess()) {
                log.warn("高德地图 API 返回失败状态: keywords={}, status={}, info={}, infocode={}",
                        finalKeywords, response.getStatus(), response.getInfo(), response.getInfocode());
                return null;
            }

            AmapPoi poi = response.getFirstPoi();
            if (poi == null) {
                log.debug("未找到匹配的地点: keywords={}, count={}", finalKeywords, response.getCount());
                return null;
            }

            log.debug("地点搜索成功: keywords={}, name={}, address={}, location={}, id={}",
                    finalKeywords, poi.getName(), poi.getAddress(), poi.getLocation(), poi.getId());

            return poi;

        } catch (WebClientResponseException e) {
            log.error("高德地图 API 调用失败 (HTTP错误): keywords={}, status={}, body={}",
                    finalKeywords, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return null;
        } catch (Exception e) {
            log.error("高德地图 API 调用失败: keywords={}", finalKeywords, e);
            return null;
        }
    }

    /**
     * 频率限制：确保相邻请求间隔不小于 MIN_REQUEST_INTERVAL
     * 高德 Web API 基础搜索服务限制为 3次/秒
     */
    private synchronized void throttleRequest() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTime;
        
        if (elapsed < MIN_REQUEST_INTERVAL) {
            long sleepTime = MIN_REQUEST_INTERVAL - elapsed;
            log.debug("请求频率限制触发，等待 {}ms", sleepTime);
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("频率限制等待被中断", e);
            }
        }
        
        lastRequestTime = System.currentTimeMillis();
    }

}
