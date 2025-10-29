package com.tinglans.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinglans.backend.common.BusinessException;
import com.tinglans.backend.common.ResponseCode;
import com.tinglans.backend.domain.Expense;
import com.tinglans.backend.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ExpenseController 测试类
 */
@WebMvcTest(ExpenseController.class)
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExpenseService expenseService;

    private Expense testExpense;
    private String testTripId;

    @BeforeEach
    void setUp() {
        testTripId = "trip-123";

        testExpense = Expense.builder()
                .id("expense-123")
                .tripId(testTripId)
                .category("food")
                .amountCents(5000L)
                .note("午餐")
                .happenedAt(Instant.now())
                .build();
    }

    @Test
    void testCreateExpenseFromText_success() throws Exception {
        // Given
        ExpenseController.CreateExpenseRequest request = new ExpenseController.CreateExpenseRequest();
        request.setTextInput("午餐花了50元");

        when(expenseService.createExpenseFromText(eq(testTripId), eq("午餐花了50元")))
                .thenReturn(testExpense);

        // When & Then
        mockMvc.perform(post("/api/trips/{tripId}/expenses", testTripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("记账成功"))
                .andExpect(jsonPath("$.data.id").value("expense-123"))
                .andExpect(jsonPath("$.data.category").value("food"))
                .andExpect(jsonPath("$.data.amountCents").value(5000))
                .andExpect(jsonPath("$.data.note").value("午餐"));

        verify(expenseService, times(1)).createExpenseFromText(eq(testTripId), eq("午餐花了50元"));
    }

    @Test
    void testCreateExpenseFromText_emptyInput() throws Exception {
        // Given
        ExpenseController.CreateExpenseRequest request = new ExpenseController.CreateExpenseRequest();
        request.setTextInput("");

        when(expenseService.createExpenseFromText(eq(testTripId), eq("")))
                .thenThrow(new BusinessException(ResponseCode.INVALID_PARAM, "输入不能为空"));

        // When & Then
        mockMvc.perform(post("/api/trips/{tripId}/expenses", testTripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(expenseService, times(1)).createExpenseFromText(eq(testTripId), eq(""));
    }

    @Test
    void testCreateExpenseFromText_tripNotFound() throws Exception {
        // Given
        ExpenseController.CreateExpenseRequest request = new ExpenseController.CreateExpenseRequest();
        request.setTextInput("午餐花了50元");

        when(expenseService.createExpenseFromText(eq("non-existent-trip"), eq("午餐花了50元")))
                .thenThrow(new BusinessException(ResponseCode.TRIP_NOT_FOUND));

        // When & Then
        mockMvc.perform(post("/api/trips/{tripId}/expenses", "non-existent-trip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(expenseService, times(1)).createExpenseFromText(eq("non-existent-trip"), eq("午餐花了50元"));
    }

    @Test
    void testGetExpenses_success() throws Exception {
        // Given
        Expense expense2 = Expense.builder()
                .id("expense-456")
                .tripId(testTripId)
                .category("transport")
                .amountCents(3000L)
                .note("地铁")
                .happenedAt(Instant.now())
                .build();

        List<Expense> expenses = Arrays.asList(testExpense, expense2);
        when(expenseService.getExpensesByTripId(testTripId)).thenReturn(expenses);

        // When & Then
        mockMvc.perform(get("/api/trips/{tripId}/expenses", testTripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value("expense-123"))
                .andExpect(jsonPath("$.data[0].category").value("food"))
                .andExpect(jsonPath("$.data[1].id").value("expense-456"))
                .andExpect(jsonPath("$.data[1].category").value("transport"));

        verify(expenseService, times(1)).getExpensesByTripId(testTripId);
    }

    @Test
    void testGetExpenses_emptyList() throws Exception {
        // Given
        when(expenseService.getExpensesByTripId(testTripId)).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/trips/{tripId}/expenses", testTripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(expenseService, times(1)).getExpensesByTripId(testTripId);
    }

    @Test
    void testGetExpenses_tripNotFound() throws Exception {
        // Given
        when(expenseService.getExpensesByTripId("non-existent-trip"))
                .thenThrow(new BusinessException(ResponseCode.TRIP_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/trips/{tripId}/expenses", "non-existent-trip"))
                .andExpect(status().isNotFound());

        verify(expenseService, times(1)).getExpensesByTripId("non-existent-trip");
    }

    @Test
    void testCreateExpenseFromText_invalidJson() throws Exception {
        // Given
        ExpenseController.CreateExpenseRequest request = new ExpenseController.CreateExpenseRequest();
        request.setTextInput("无效输入");

        when(expenseService.createExpenseFromText(eq(testTripId), eq("无效输入")))
                .thenThrow(new BusinessException(ResponseCode.BAD_REQUEST, "Invalid JSON format for expense data"));

        // When & Then
        mockMvc.perform(post("/api/trips/{tripId}/expenses", testTripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(expenseService, times(1)).createExpenseFromText(eq(testTripId), eq("无效输入"));
    }
}
