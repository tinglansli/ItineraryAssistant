package com.tinglans.backend.controller;

import com.tinglans.backend.common.ApiResponse;
import com.tinglans.backend.domain.Expense;
import com.tinglans.backend.service.ExpenseService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 开销控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trips")
public class ExpenseController {

    private final ExpenseService expenseService;

    /**
     * 从文本创建开销记录
     */
    @PostMapping("/{tripId}/expenses")
    public ResponseEntity<ApiResponse<Expense>> createExpenseFromText(
            @PathVariable String tripId,
            @RequestBody CreateExpenseRequest request) throws Exception {
        Expense expense = expenseService.createExpenseFromText(tripId, request.getTextInput());
        return ResponseEntity.ok(ApiResponse.success("记账成功", expense));
    }

    /**
     * 获取行程的所有开销记录
     */
    @GetMapping("/{tripId}/expenses")
    public ResponseEntity<ApiResponse<List<Expense>>> getExpenses(@PathVariable String tripId) throws Exception {
        List<Expense> expenses = expenseService.getExpensesByTripId(tripId);
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @Data
    public static class CreateExpenseRequest {
        private String textInput;
    }
}
