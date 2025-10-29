package com.tinglans.backend.service;

import com.tinglans.backend.domain.Activity;
import com.tinglans.backend.domain.Day;
import com.tinglans.backend.domain.Trip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 预算业务逻辑层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final ExpenseService expenseService;

    /**
     * 计算行程的计划预算
     *
     * @param trip 行程对象
     * @return 按类别汇总的计划预算（分）
     */
    public Map<String, Long> calculatePlannedBudget(Trip trip) {
        log.debug("计算行程计划预算: tripId={}", trip.getId());

        Map<String, Long> budget = new HashMap<>();
        budget.put("transport", 0L);
        budget.put("hotel", 0L);
        budget.put("sight", 0L);
        budget.put("food", 0L);
        budget.put("other", 0L);

        if (trip.getDays() == null) {
            return budget;
        }

        // 遍历所有活动，按类型汇总费用
        for (Day day : trip.getDays()) {
            if (day.getActivities() == null) continue;

            for (Activity activity : day.getActivities()) {
                String type = activity.getType();
                long currentAmount = budget.getOrDefault(type, 0L);
                budget.put(type, currentAmount + (activity.getEstimatedCost() != null ? activity.getEstimatedCost() : 0L));
            }
        }

        log.debug("计划预算计算完成: {}", budget);
        return budget;
    }

    /**
     * 计算预算汇总
     *
     * @param tripId 行程ID
     * @param trip 行程对象
     * @return 预算汇总数据
     */
    public BudgetSummary getBudgetSummary(String tripId, Trip trip) throws ExecutionException, InterruptedException {
        log.info("生成预算汇总: tripId={}", tripId);

        // 1. 统计预算
        Map<String, Long> plannedBudget = calculatePlannedBudget(trip);

        // 2. 统计开销
        Map<String, Long> actualExpense = expenseService.calculateExpenseByCategory(tripId);

        // 3. 构建汇总对象
        BudgetSummary summary = new BudgetSummary();
        summary.setTripId(tripId);
        summary.setPlannedBudget(plannedBudget);
        summary.setActualExpense(actualExpense);
        summary.setTotalPlanned(calculateTotal(plannedBudget));
        summary.setTotalActual(calculateTotal(actualExpense));

        log.info("预算汇总完成: 计划={}, 实际={}", summary.getTotalPlanned(), summary.getTotalActual());
        return summary;
    }

    /**
     * 计算总额
     */
    private long calculateTotal(Map<String, Long> budget) {
        return budget.values().stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    /**
     * 预算汇总数据传输对象
     */
    @lombok.Data
    public static class BudgetSummary {
        private String tripId;
        private Map<String, Long> plannedBudget;  // 计划预算
        private Map<String, Long> actualExpense;  // 实际开销
        private long totalPlanned;                // 计划总额
        private long totalActual;                 // 实际总额
    }
}
