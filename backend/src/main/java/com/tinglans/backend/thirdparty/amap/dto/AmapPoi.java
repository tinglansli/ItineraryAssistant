package com.tinglans.backend.thirdparty.amap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 高德地图 POI 信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapPoi {
    
    /**
     * POI 唯一标识
     */
    private String id;
    
    /**
     * POI 名称
     */
    private String name;
    
    /**
     * POI 所属类型
     */
    private String type;
    
    /**
     * POI 分类编码
     */
    private String typecode;
    
    /**
     * POI 详细地址
     */
    private String address;
    
    /**
     * POI 经纬度（格式：经度,纬度）
     */
    private String location;
    
    /**
     * POI 所属省份
     */
    private String pname;
    
    /**
     * POI 所属城市
     */
    private String cityname;
    
    /**
     * POI 所属区县
     */
    private String adname;
    
    /**
     * POI 所属省份编码
     */
    private String pcode;
    
    /**
     * POI 所属城市编码
     */
    private String citycode;
    
    /**
     * POI 所属区域编码
     */
    private String adcode;
    
    /**
     * POI 的联系电话
     */
    private String tel;
    
    /**
     * 从 location 解析出纬度
     * 官方文档：location格式为"经度,纬度"
     */
    public Double getLat() {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }
        String[] parts = location.split(",");
        if (parts.length >= 2) {
            try {
                return Double.parseDouble(parts[1].trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * 从 location 解析出经度
     * 官方文档：location格式为"经度,纬度"
     */
    public Double getLng() {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }
        String[] parts = location.split(",");
        if (parts.length >= 1) {
            try {
                return Double.parseDouble(parts[0].trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
