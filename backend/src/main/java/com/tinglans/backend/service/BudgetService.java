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
     * @param trip 行程对象
     * @return 预算汇总数据
     */
    public BudgetSummary getBudgetSummary(Trip trip) throws ExecutionException, InterruptedException {
        log.info("生成预算汇总: tripId={}", trip.getId());

        // 1. 统计预算
        Map<String, Long> plannedBudget = calculatePlannedBudget(trip);

        // 2. 统计开销（需要通过tripId查询，因为开销是独立存储的）
        Map<String, Long> actualExpense = expenseService.calculateExpenseByCategory(trip.getId());

        // 3. 构建汇总对象
        BudgetSummary summary = new BudgetSummary();
        summary.setTripId(trip.getId());
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
     * 获取预算信息（仅预算，不含实际开销）
     *
     * @param trip 行程对象
     * @return 预算信息
     */
    public BudgetInfo getBudgetInfo(Trip trip) {
        log.info("获取预算信息: tripId={}", trip.getId());

        // 1. 计算各分类预算
        Map<String, Long> categoryBudget = calculatePlannedBudget(trip);

        // 2. 计算总预算
        long totalBudget = calculateTotal(categoryBudget);

        // 3. 构建响应对象
        BudgetInfo budgetInfo = new BudgetInfo();
        budgetInfo.setTripId(trip.getId());
        budgetInfo.setTotalBudget(totalBudget);
        budgetInfo.setCategoryBudget(categoryBudget);

        log.info("预算信息获取完成: 总预算={}, 分类预算={}", totalBudget, categoryBudget);
        return budgetInfo;
    }

    /**
     * 预算信息数据传输对象
     * 仅包含预算信息，不包含实际开销
     */
    @lombok.Data
    public static class BudgetInfo {
        private String tripId;
        private long totalBudget;                 // 总预算（分）
        private Map<String, Long> categoryBudget; // 各分类预算（分），key为活动类型的value值（如"transport"）
    }

    /**
     * 预算汇总数据传输对象
     * 包含预算和实际开销的对比分析
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
