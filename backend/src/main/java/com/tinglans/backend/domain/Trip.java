package com.tinglans.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 行程实体
 * 代表一次完整的旅行规划
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip {
    
    /**
     * 行程ID
     */
    private String id;
    
    /**
     * 所属用户ID
     */
    private String userId;
    
    /**
     * 行程标题
     */
    private String title;
    
    /**
     * 目的地
     */
    private String destination;
    
    /**
     * 开始日期
     */
    private LocalDate startDate;
    
    /**
     * 结束日期
     */
    private LocalDate endDate;
    
    /**
     * 货币类型（如 CNY、USD）
     */
    private String currency;
    
    /**
     * 总预算（分）
     */
    private Long totalBudget;
    
    /**
     * 同行人数
     */
    private Headcount headcount;
    
    /**
     * 用户偏好（如：美食、动漫、亲子）
     */
    private List<String> preferences;
    
    /**
     * 行程天数列表
     */
    private List<Day> days;
    
    /**
     * 是否已确认（确认后会存入数据库）
     */
    private Boolean confirmed;
    
    /**
     * 创建时间
     */
    private Instant createdAt;
    
    /**
     * 更新时间
     */
    private Instant updatedAt;
    
    /**
     * 同行人数信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Headcount {
        /**
         * 成人数量
         */
        private Integer adults;
        
        /**
         * 儿童数量
         */
        private Integer children;
    }
}
