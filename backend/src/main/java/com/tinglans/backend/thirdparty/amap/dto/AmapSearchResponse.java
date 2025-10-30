package com.tinglans.backend.thirdparty.amap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 高德地图搜索响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapSearchResponse {
    
    /**
     * 状态码：成功返回"1"，失败返回"0"
     */
    private String status;
    
    /**
     * 单次请求返回的实际poi点的个数
     */
    private String count;
    
    /**
     * 访问状态值的说明，成功返回"ok"
     */
    private String info;
    
    /**
     * 返回状态说明，10000代表正确
     */
    private String infocode;
    
    /**
     * POI 列表
     */
    private AmapPoi[] pois;
    
    /**
     * 是否成功（根据官方文档，成功时status为字符串"1"）
     */
    public boolean isSuccess() {
        return "1".equals(status);
    }
    
    /**
     * 获取第一个 POI
     */
    public AmapPoi getFirstPoi() {
        if (pois != null && pois.length > 0) {
            return pois[0];
        }
        return null;
    }
}
