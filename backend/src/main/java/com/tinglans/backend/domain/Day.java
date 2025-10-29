package com.tinglans.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 行程天实体
 * 代表行程中的某一天
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Day {
    
    /**
     * 第几天（从1开始）
     */
    private Integer dayIndex;
    
    /**
     * 日期
     */
    private LocalDate date;
    
    /**
     * 该天的活动列表
     */
    private List<Activity> activities;
}
