package com.tinglans.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 行程概要DTO
 * 用于列表展示，不包含Days详细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSummary {
    
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
     * 总预算（分）
     */
    private Long totalBudget;
    
    /**
     * 同行人数
     */
    private Headcount headcount;
    
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
