package com.tinglans.backend.controller;

import com.tinglans.backend.common.BusinessException;
import com.tinglans.backend.common.ResponseCode;
import com.tinglans.backend.domain.Day;
import com.tinglans.backend.domain.Trip;
import com.tinglans.backend.service.BudgetService;
import com.tinglans.backend.service.TripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BudgetController 测试类
 */
@WebMvcTest(BudgetController.class)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BudgetService budgetService;

    @MockBean
    private TripService tripService;

    private Trip testTrip;
    private String testTripId;

    @BeforeEach
    void setUp() {
        testTripId = "trip-123";

        Day day = Day.builder()
                .date(LocalDate.now())
                .activities(new ArrayList<>())
                .build();

        testTrip = Trip.builder()
                .id(testTripId)
                .userId("user-123")
                .title("北京三日游")
                .destination("北京")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(2))
                .days(Arrays.asList(day))
                .totalBudget(100000L)
                .build();
    }

    private Map<String, Long> createCategoryBudgets() {
        Map<String, Long> budgets = new HashMap<>();
        budgets.put("transport", 30000L);
        budgets.put("food", 40000L);
        budgets.put("accommodation", 20000L);
        budgets.put("other", 10000L);
        return budgets;
    }

    @Test
    void testGetBudgetInfo_success() throws Exception {
        // Given
        Map<String, Long> categoryBudgets = createCategoryBudgets();
        BudgetService.BudgetInfo budgetInfo = new BudgetService.BudgetInfo();
        budgetInfo.setTripId(testTripId);
        budgetInfo.setTotalBudget(100000L);
        budgetInfo.setCategoryBudget(categoryBudgets);

        when(tripService.validateAndGetTrip(testTripId)).thenReturn(testTrip);
        when(budgetService.getBudgetInfo(testTrip)).thenReturn(budgetInfo);

        // When & Then
        mockMvc.perform(get("/api/trips/{tripId}/budget/info", testTripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalBudget").value(100000))
                .andExpect(jsonPath("$.data.categoryBudget.transport").value(30000))
                .andExpect(jsonPath("$.data.categoryBudget.food").value(40000))
                .andExpect(jsonPath("$.data.categoryBudget.accommodation").value(20000))
                .andExpect(jsonPath("$.data.categoryBudget.other").value(10000));

        verify(tripService, times(1)).validateAndGetTrip(testTripId);
        verify(budgetService, times(1)).getBudgetInfo(testTrip);
    }

    @Test
    void testGetBudgetInfo_tripNotFound() throws Exception {
        // Given
        when(tripService.validateAndGetTrip("non-existent-trip"))
                .thenThrow(new BusinessException(ResponseCode.TRIP_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/trips/{tripId}/budget/info", "non-existent-trip"))
                .andExpect(status().isNotFound());

        verify(tripService, times(1)).validateAndGetTrip("non-existent-trip");
        verify(budgetService, never()).getBudgetInfo(any());
    }

    @Test
    void testGetBudgetSummary_success() throws Exception {
        // Given
        Map<String, Long> categoryBudgets = createCategoryBudgets();
        Map<String, Long> categoryExpenses = new HashMap<>();
        categoryExpenses.put("transport", 25000L);
        categoryExpenses.put("food", 35000L);
        categoryExpenses.put("accommodation", 18000L);
        categoryExpenses.put("other", 5000L);

        BudgetService.BudgetSummary budgetSummary = new BudgetService.BudgetSummary();
        budgetSummary.setTripId(testTripId);
        budgetSummary.setTotalPlanned(100000L);
        budgetSummary.setTotalActual(83000L);
        budgetSummary.setPlannedBudget(categoryBudgets);
        budgetSummary.setActualExpense(categoryExpenses);

        when(tripService.validateAndGetTrip(testTripId)).thenReturn(testTrip);
        when(budgetService.getBudgetSummary(testTrip)).thenReturn(budgetSummary);

        // When & Then
        mockMvc.perform(get("/api/trips/{tripId}/budget", testTripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalPlanned").value(100000))
                .andExpect(jsonPath("$.data.totalActual").value(83000))
                .andExpect(jsonPath("$.data.plannedBudget.transport").value(30000))
                .andExpect(jsonPath("$.data.actualExpense.transport").value(25000));

        verify(tripService, times(1)).validateAndGetTrip(testTripId);
        verify(budgetService, times(1)).getBudgetSummary(testTrip);
    }

    @Test
    void testGetBudgetSummary_noExpenses() throws Exception {
        // Given
        Map<String, Long> categoryBudgets = createCategoryBudgets();
        Map<String, Long> categoryExpenses = new HashMap<>();

        BudgetService.BudgetSummary budgetSummary = new BudgetService.BudgetSummary();
        budgetSummary.setTripId(testTripId);
        budgetSummary.setTotalPlanned(100000L);
        budgetSummary.setTotalActual(0L);
        budgetSummary.setPlannedBudget(categoryBudgets);
        budgetSummary.setActualExpense(categoryExpenses);

        when(tripService.validateAndGetTrip(testTripId)).thenReturn(testTrip);
        when(budgetService.getBudgetSummary(testTrip)).thenReturn(budgetSummary);

        // When & Then
        mockMvc.perform(get("/api/trips/{tripId}/budget", testTripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalPlanned").value(100000))
                .andExpect(jsonPath("$.data.totalActual").value(0));

        verify(tripService, times(1)).validateAndGetTrip(testTripId);
        verify(budgetService, times(1)).getBudgetSummary(testTrip);
    }

    @Test
    void testGetBudgetSummary_overBudget() throws Exception {
        // Given
        Map<String, Long> categoryBudgets = createCategoryBudgets();
        Map<String, Long> categoryExpenses = new HashMap<>();
        categoryExpenses.put("transport", 35000L);  // 超出预算
        categoryExpenses.put("food", 50000L);       // 超出预算
        categoryExpenses.put("accommodation", 25000L); // 超出预算
        categoryExpenses.put("other", 10000L);

        BudgetService.BudgetSummary budgetSummary = new BudgetService.BudgetSummary();
        budgetSummary.setTripId(testTripId);
        budgetSummary.setTotalPlanned(100000L);
        budgetSummary.setTotalActual(120000L);
        budgetSummary.setPlannedBudget(categoryBudgets);
        budgetSummary.setActualExpense(categoryExpenses);

        when(tripService.validateAndGetTrip(testTripId)).thenReturn(testTrip);
        when(budgetService.getBudgetSummary(testTrip)).thenReturn(budgetSummary);

        // When & Then
        mockMvc.perform(get("/api/trips/{tripId}/budget", testTripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalPlanned").value(100000))
                .andExpect(jsonPath("$.data.totalActual").value(120000));

        verify(tripService, times(1)).validateAndGetTrip(testTripId);
        verify(budgetService, times(1)).getBudgetSummary(testTrip);
    }

    @Test
    void testGetBudgetSummary_tripNotFound() throws Exception {
        // Given
        when(tripService.validateAndGetTrip("non-existent-trip"))
                .thenThrow(new BusinessException(ResponseCode.TRIP_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/trips/{tripId}/budget", "non-existent-trip"))
                .andExpect(status().isNotFound());

        verify(tripService, times(1)).validateAndGetTrip("non-existent-trip");
        verify(budgetService, never()).getBudgetSummary(any());
    }

    @Test
    void testGetBudgetInfo_noCategoryBudgets() throws Exception {
        // Given
        BudgetService.BudgetInfo budgetInfo = new BudgetService.BudgetInfo();
        budgetInfo.setTripId(testTripId);
        budgetInfo.setTotalBudget(100000L);
        budgetInfo.setCategoryBudget(new HashMap<>());

        when(tripService.validateAndGetTrip(testTripId)).thenReturn(testTrip);
        when(budgetService.getBudgetInfo(testTrip)).thenReturn(budgetInfo);

        // When & Then
        mockMvc.perform(get("/api/trips/{tripId}/budget/info", testTripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalBudget").value(100000))
                .andExpect(jsonPath("$.data.categoryBudget").isEmpty());

        verify(tripService, times(1)).validateAndGetTrip(testTripId);
        verify(budgetService, times(1)).getBudgetInfo(testTrip);
    }
}
