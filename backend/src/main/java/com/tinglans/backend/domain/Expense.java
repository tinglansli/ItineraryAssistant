package com.tinglans.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 开销实体
 * 记录旅行中的实际支出
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense {
    
    /**
     * 支出ID
     */
    private String id;
    
    /**
     * 所属行程ID
     */
    private String tripId;
    
    /**
     * 支出类别：
     * - transport（交通）
     * - hotel（住宿）
     * - sight（景点）
     * - food（餐厅）
     * - other（其他）
     */
    private String category;
    
    /**
     * 金额（分）
     */
    private Long amountCents;
    
    /**
     * 备注
     */
    private String note;
    
    /**
     * 发生时间
     */
    private Instant happenedAt;
    
    /**
     * 创建时间
     */
    private Instant createdAt;
}
