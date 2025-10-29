package com.tinglans.backend.controller;

import com.tinglans.backend.common.ApiResponse;
import com.tinglans.backend.domain.Trip;
import com.tinglans.backend.service.BudgetService;
import com.tinglans.backend.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 预算控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trips")
public class BudgetController {

    private final BudgetService budgetService;
    private final TripService tripService;

    /**
     * 获取行程的预算信息
     * 返回总预算和各分类预算
     */
    @GetMapping("/{tripId}/budget/info")
    public ResponseEntity<ApiResponse<BudgetService.BudgetInfo>> getBudgetInfo(@PathVariable String tripId) 
            throws Exception {
        Trip trip = tripService.validateAndGetTrip(tripId);
        BudgetService.BudgetInfo budgetInfo = budgetService.getBudgetInfo(trip);
        return ResponseEntity.ok(ApiResponse.success(budgetInfo));
    }

    /**
     * 获取行程的预算分析信息
     * 返回预算与实际开销
     */
    @GetMapping("/{tripId}/budget")
    public ResponseEntity<ApiResponse<BudgetService.BudgetSummary>> getBudgetSummary(@PathVariable String tripId) 
            throws Exception {
        Trip trip = tripService.validateAndGetTrip(tripId);
        BudgetService.BudgetSummary budgetSummary = budgetService.getBudgetSummary(trip);
        return ResponseEntity.ok(ApiResponse.success(budgetSummary));
    }
}
