package com.tinglans.backend.service;

import com.tinglans.backend.domain.Activity;
import com.tinglans.backend.domain.Day;
import com.tinglans.backend.domain.Trip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * BudgetService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private ExpenseService expenseService;

    @InjectMocks
    private BudgetService budgetService;

    private Trip testTrip;
    private String testTripId;

    @BeforeEach
    void setUp() {
        testTripId = "trip-123";
        
        // 创建测试数据
        Activity activity1 = Activity.builder()
                .id("activity-1")
                .type("transport")
                .title("乘坐地铁")
                .estimatedCost(500L)  // 5元
                .build();

        Activity activity2 = Activity.builder()
                .id("activity-2")
                .type("food")
                .title("午餐")
                .estimatedCost(3000L)  // 30元
                .build();

        Activity activity3 = Activity.builder()
                .id("activity-3")
                .type("sight")
                .title("参观景点")
                .estimatedCost(8000L)  // 80元
                .build();

        Activity activity4 = Activity.builder()
                .id("activity-4")
                .type("hotel")
                .title("住宿")
                .estimatedCost(25000L)  // 250元
                .build();

        Day day1 = Day.builder()
                .dayIndex(1)
                .date(LocalDate.of(2024, 11, 1))
                .activities(Arrays.asList(activity1, activity2))
                .build();

        Day day2 = Day.builder()
                .dayIndex(2)
                .date(LocalDate.of(2024, 11, 2))
                .activities(Arrays.asList(activity3, activity4))
                .build();

        testTrip = Trip.builder()
                .id(testTripId)
                .userId("user-123")
                .title("测试行程")
                .destination("东京")
                .startDate(LocalDate.of(2024, 11, 1))
                .endDate(LocalDate.of(2024, 11, 2))
                .days(Arrays.asList(day1, day2))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void testCalculatePlannedBudget_withValidTrip() {
        // When
        Map<String, Long> budget = budgetService.calculatePlannedBudget(testTrip);

        // Then
        assertNotNull(budget);
        assertEquals(500L, budget.get("transport"));
        assertEquals(3000L, budget.get("food"));
        assertEquals(8000L, budget.get("sight"));
        assertEquals(25000L, budget.get("hotel"));
        assertEquals(0L, budget.get("other"));
    }

    @Test
    void testCalculatePlannedBudget_withNullDays() {
        // Given
        Trip tripWithNullDays = Trip.builder()
                .id("trip-456")
                .title("空行程")
                .days(null)
                .build();

        // When
        Map<String, Long> budget = budgetService.calculatePlannedBudget(tripWithNullDays);

        // Then
        assertNotNull(budget);
        assertEquals(0L, budget.get("transport"));
        assertEquals(0L, budget.get("hotel"));
        assertEquals(0L, budget.get("sight"));
        assertEquals(0L, budget.get("food"));
        assertEquals(0L, budget.get("other"));
    }

    @Test
    void testCalculatePlannedBudget_withEmptyDays() {
        // Given
        Trip tripWithEmptyDays = Trip.builder()
                .id("trip-789")
                .title("空行程")
                .days(new ArrayList<>())
                .build();

        // When
        Map<String, Long> budget = budgetService.calculatePlannedBudget(tripWithEmptyDays);

        // Then
        assertNotNull(budget);
        assertEquals(0L, budget.get("transport"));
        assertEquals(0L, budget.get("hotel"));
        assertEquals(0L, budget.get("sight"));
        assertEquals(0L, budget.get("food"));
        assertEquals(0L, budget.get("other"));
    }

    @Test
    void testCalculatePlannedBudget_withNullActivities() {
        // Given
        Day dayWithNullActivities = Day.builder()
                .dayIndex(1)
                .activities(null)
                .build();

        Trip trip = Trip.builder()
                .id("trip-null-activities")
                .days(Collections.singletonList(dayWithNullActivities))
                .build();

        // When
        Map<String, Long> budget = budgetService.calculatePlannedBudget(trip);

        // Then
        assertNotNull(budget);
        assertEquals(0L, budget.get("transport"));
    }

    @Test
    void testCalculatePlannedBudget_withNullEstimatedCost() {
        // Given
        Activity activityWithNullCost = Activity.builder()
                .id("activity-null-cost")
                .type("food")
                .estimatedCost(null)
                .build();

        Day day = Day.builder()
                .dayIndex(1)
                .activities(Collections.singletonList(activityWithNullCost))
                .build();

        Trip trip = Trip.builder()
                .id("trip-null-cost")
                .days(Collections.singletonList(day))
                .build();

        // When
        Map<String, Long> budget = budgetService.calculatePlannedBudget(trip);

        // Then
        assertNotNull(budget);
        assertEquals(0L, budget.get("food"));
    }

    @Test
    void testCalculatePlannedBudget_withMultipleSameTypeActivities() {
        // Given - 多个相同类型的活动
        Activity food1 = Activity.builder()
                .type("food")
                .estimatedCost(2000L)
                .build();

        Activity food2 = Activity.builder()
                .type("food")
                .estimatedCost(3500L)
                .build();

        Day day = Day.builder()
                .dayIndex(1)
                .activities(Arrays.asList(food1, food2))
                .build();

        Trip trip = Trip.builder()
                .id("trip-same-type")
                .days(Collections.singletonList(day))
                .build();

        // When
        Map<String, Long> budget = budgetService.calculatePlannedBudget(trip);

        // Then
        assertEquals(5500L, budget.get("food"));  // 2000 + 3500
    }

    @Test
    void testGetBudgetSummary_success() throws ExecutionException, InterruptedException {
        // Given
        Map<String, Long> actualExpense = new HashMap<>();
        actualExpense.put("transport", 600L);
        actualExpense.put("food", 2800L);
        actualExpense.put("sight", 8500L);
        actualExpense.put("hotel", 24000L);
        actualExpense.put("other", 1000L);

        when(expenseService.calculateExpenseByCategory(testTripId)).thenReturn(actualExpense);

        // When
        BudgetService.BudgetSummary summary = budgetService.getBudgetSummary(testTrip);

        // Then
        assertNotNull(summary);
        assertEquals(testTripId, summary.getTripId());
        
        // 验证计划预算
        assertNotNull(summary.getPlannedBudget());
        assertEquals(500L, summary.getPlannedBudget().get("transport"));
        assertEquals(3000L, summary.getPlannedBudget().get("food"));
        assertEquals(8000L, summary.getPlannedBudget().get("sight"));
        assertEquals(25000L, summary.getPlannedBudget().get("hotel"));
        
        // 验证实际支出
        assertNotNull(summary.getActualExpense());
        assertEquals(600L, summary.getActualExpense().get("transport"));
        assertEquals(2800L, summary.getActualExpense().get("food"));
        
        // 验证总额
        assertEquals(36500L, summary.getTotalPlanned());  // 500 + 3000 + 8000 + 25000
        assertEquals(36900L, summary.getTotalActual());   // 600 + 2800 + 8500 + 24000 + 1000

        verify(expenseService, times(1)).calculateExpenseByCategory(testTripId);
    }

    @Test
    void testGetBudgetSummary_withEmptyExpense() throws ExecutionException, InterruptedException {
        // Given
        when(expenseService.calculateExpenseByCategory(testTripId)).thenReturn(new HashMap<>());

        // When
        BudgetService.BudgetSummary summary = budgetService.getBudgetSummary(testTrip);

        // Then
        assertNotNull(summary);
        assertEquals(0L, summary.getTotalActual());
        assertTrue(summary.getTotalPlanned() > 0);
    }

    @Test
    void testGetBudgetSummary_expenseServiceThrowsException() throws ExecutionException, InterruptedException {
        // Given
        when(expenseService.calculateExpenseByCategory(anyString()))
                .thenThrow(new ExecutionException(new RuntimeException("Database error")));

        // When & Then
        assertThrows(ExecutionException.class, () -> {
            budgetService.getBudgetSummary(testTrip);
        });

        verify(expenseService, times(1)).calculateExpenseByCategory(testTripId);
    }

    @Test
    void testGetBudgetSummary_withNullTrip() throws ExecutionException, InterruptedException {
        // Given
        Trip nullDaysTrip = Trip.builder()
                .id(testTripId)
                .days(null)
                .build();

        when(expenseService.calculateExpenseByCategory(testTripId)).thenReturn(new HashMap<>());

        // When
        BudgetService.BudgetSummary summary = budgetService.getBudgetSummary(nullDaysTrip);

        // Then
        assertNotNull(summary);
        assertEquals(0L, summary.getTotalPlanned());
        assertEquals(0L, summary.getTotalActual());
    }

    @Test
    void testGetBudgetInfo_success() {
        // When
        BudgetService.BudgetInfo budgetInfo = budgetService.getBudgetInfo(testTrip);

        // Then
        assertNotNull(budgetInfo);
        assertEquals(testTripId, budgetInfo.getTripId());
        
        // 验证总预算
        assertEquals(36500L, budgetInfo.getTotalBudget());  // 500 + 3000 + 8000 + 25000
        
        // 验证分类预算
        assertNotNull(budgetInfo.getCategoryBudget());
        assertEquals(500L, budgetInfo.getCategoryBudget().get("transport"));
        assertEquals(3000L, budgetInfo.getCategoryBudget().get("food"));
        assertEquals(8000L, budgetInfo.getCategoryBudget().get("sight"));
        assertEquals(25000L, budgetInfo.getCategoryBudget().get("hotel"));
        assertEquals(0L, budgetInfo.getCategoryBudget().get("other"));
    }

    @Test
    void testGetBudgetInfo_withEmptyTrip() {
        // Given
        Trip emptyTrip = Trip.builder()
                .id("empty-trip")
                .days(null)
                .build();

        // When
        BudgetService.BudgetInfo budgetInfo = budgetService.getBudgetInfo(emptyTrip);

        // Then
        assertNotNull(budgetInfo);
        assertEquals("empty-trip", budgetInfo.getTripId());
        assertEquals(0L, budgetInfo.getTotalBudget());
        
        // 验证所有分类都是0
        assertEquals(0L, budgetInfo.getCategoryBudget().get("transport"));
        assertEquals(0L, budgetInfo.getCategoryBudget().get("hotel"));
        assertEquals(0L, budgetInfo.getCategoryBudget().get("sight"));
        assertEquals(0L, budgetInfo.getCategoryBudget().get("food"));
        assertEquals(0L, budgetInfo.getCategoryBudget().get("other"));
    }

    @Test
    void testGetBudgetInfo_withNullCosts() {
        // Given - 创建一个活动成本为null的行程
        Activity activityWithNullCost = Activity.builder()
                .id("activity-null")
                .type("food")
                .title("免费餐")
                .estimatedCost(null)  // null成本
                .build();

        Day day = Day.builder()
                .dayIndex(1)
                .date(LocalDate.of(2024, 11, 1))
                .activities(Collections.singletonList(activityWithNullCost))
                .build();

        Trip tripWithNullCosts = Trip.builder()
                .id("trip-null-costs")
                .days(Collections.singletonList(day))
                .build();

        // When
        BudgetService.BudgetInfo budgetInfo = budgetService.getBudgetInfo(tripWithNullCosts);

        // Then
        assertNotNull(budgetInfo);
        assertEquals(0L, budgetInfo.getTotalBudget());
        assertEquals(0L, budgetInfo.getCategoryBudget().get("food"));
    }

    @Test
    void testBudgetInfo_dataTransferObject() {
        // Given
        Map<String, Long> categoryBudget = new HashMap<>();
        categoryBudget.put("food", 5000L);
        categoryBudget.put("transport", 1000L);

        // When
        BudgetService.BudgetInfo budgetInfo = new BudgetService.BudgetInfo();
        budgetInfo.setTripId("trip-info-test");
        budgetInfo.setTotalBudget(6000L);
        budgetInfo.setCategoryBudget(categoryBudget);

        // Then
        assertEquals("trip-info-test", budgetInfo.getTripId());
        assertEquals(6000L, budgetInfo.getTotalBudget());
        assertEquals(5000L, budgetInfo.getCategoryBudget().get("food"));
        assertEquals(1000L, budgetInfo.getCategoryBudget().get("transport"));
    }

    @Test
    void testBudgetSummary_dataTransferObject() {
        // Given
        Map<String, Long> planned = new HashMap<>();
        planned.put("food", 5000L);
        
        Map<String, Long> actual = new HashMap<>();
        actual.put("food", 4800L);

        // When
        BudgetService.BudgetSummary summary = new BudgetService.BudgetSummary();
        summary.setTripId("trip-dto-test");
        summary.setPlannedBudget(planned);
        summary.setActualExpense(actual);
        summary.setTotalPlanned(5000L);
        summary.setTotalActual(4800L);

        // Then
        assertEquals("trip-dto-test", summary.getTripId());
        assertEquals(5000L, summary.getPlannedBudget().get("food"));
        assertEquals(4800L, summary.getActualExpense().get("food"));
        assertEquals(5000L, summary.getTotalPlanned());
        assertEquals(4800L, summary.getTotalActual());
    }
}
