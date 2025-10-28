package com.tinglans.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 活动实体
 * 代表行程中的一个活动（景点、餐饮、酒店、交通等）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity {
    
    /**
     * 活动ID
     */
    private String id;
    
    /**
     * 第几天（从1开始）
     */
    private Integer dayIndex;
    
    /**
     * 活动类型：sight（景点）、food（餐饮）、hotel（酒店）、transport（交通）
     */
    private String type;
    
    /**
     * 活动名称
     */
    private String title;
    
    /**
     * 地址
     */
    private String address;
    
    /**
     * 纬度
     */
    private Double lat;
    
    /**
     * 经度
     */
    private Double lng;
    
    /**
     * 开始时间（HH:mm 格式）
     */
    private String startTime;
    
    /**
     * 结束时间（HH:mm 格式）
     */
    private String endTime;
    
    /**
     * 预估费用（分）
     */
    private Long estimatedCost;
}
