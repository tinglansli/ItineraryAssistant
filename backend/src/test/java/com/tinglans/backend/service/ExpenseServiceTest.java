package com.tinglans.backend.service;

import com.tinglans.backend.domain.Expense;
import com.tinglans.backend.repository.ExpenseRepository;
import com.tinglans.backend.thirdparty.llm.QwenClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExpenseService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private QwenClient qwenClient;

    @InjectMocks
    private ExpenseService expenseService;

    private String testTripId;
    private List<Expense> testExpenses;

    @BeforeEach
    void setUp() {
        testTripId = "trip-123";

        // 准备测试数据
        Expense expense1 = Expense.builder()
                .id("expense-1")
                .tripId(testTripId)
                .category("food")
                .amountCents(5000L)
                .note("午餐")
                .happenedAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        Expense expense2 = Expense.builder()
                .id("expense-2")
                .tripId(testTripId)
                .category("transport")
                .amountCents(1200L)
                .note("地铁")
                .happenedAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        Expense expense3 = Expense.builder()
                .id("expense-3")
                .tripId(testTripId)
                .category("food")
                .amountCents(3000L)
                .note("晚餐")
                .happenedAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        testExpenses = Arrays.asList(expense1, expense2, expense3);
    }

    @Test
    void testCreateExpenseFromText_success() throws ExecutionException, InterruptedException {
        // Given
        String userInput = "今天吃拉面花了50块";
        String llmResponse = """
                {
                  "category": "food",
                  "amountCents": 5000,
                  "note": "拉面"
                }
                """;

        when(qwenClient.chat(anyString(), eq(userInput))).thenReturn(llmResponse);
        doNothing().when(expenseRepository).save(eq(testTripId), any(Expense.class));

        // When
        Expense result = expenseService.createExpenseFromText(testTripId, userInput);

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(testTripId, result.getTripId());
        assertEquals("food", result.getCategory());
        assertEquals(5000L, result.getAmountCents());
        assertEquals("拉面", result.getNote());
        assertNotNull(result.getHappenedAt());
        assertNotNull(result.getCreatedAt());

        verify(qwenClient, times(1)).chat(anyString(), eq(userInput));
        verify(expenseRepository, times(1)).save(eq(testTripId), any(Expense.class));
    }

    @Test
    void testCreateExpenseFromText_transportCategory() throws ExecutionException, InterruptedException {
        // Given
        String userInput = "打车去机场花了120";
        String llmResponse = """
                {
                  "category": "transport",
                  "amountCents": 12000,
                  "note": "打车去机场"
                }
                """;

        when(qwenClient.chat(anyString(), eq(userInput))).thenReturn(llmResponse);
        doNothing().when(expenseRepository).save(anyString(), any(Expense.class));

        // When
        Expense result = expenseService.createExpenseFromText(testTripId, userInput);

        // Then
        assertEquals("transport", result.getCategory());
        assertEquals(12000L, result.getAmountCents());
        assertEquals("打车去机场", result.getNote());
    }

    @Test
    void testCreateExpenseFromText_hotelCategory() throws ExecutionException, InterruptedException {
        // Given
        String userInput = "酒店住宿500元";
        String llmResponse = """
                {
                  "category": "hotel",
                  "amountCents": 50000,
                  "note": "酒店住宿"
                }
                """;

        when(qwenClient.chat(anyString(), anyString())).thenReturn(llmResponse);
        doNothing().when(expenseRepository).save(anyString(), any(Expense.class));

        // When
        Expense result = expenseService.createExpenseFromText(testTripId, userInput);

        // Then
        assertEquals("hotel", result.getCategory());
        assertEquals(50000L, result.getAmountCents());
    }

    @Test
    void testCreateExpenseFromText_withMissingFields() throws ExecutionException, InterruptedException {
        // Given - LLM返回的JSON缺少某些字段
        String userInput = "买了点东西";
        String llmResponse = """
                {
                  "category": "other"
                }
                """;

        when(qwenClient.chat(anyString(), eq(userInput))).thenReturn(llmResponse);
        doNothing().when(expenseRepository).save(anyString(), any(Expense.class));

        // When
        Expense result = expenseService.createExpenseFromText(testTripId, userInput);

        // Then
        assertEquals("other", result.getCategory());
        assertEquals(0L, result.getAmountCents());  // 默认为0
        assertEquals("", result.getNote());  // 默认为空字符串
    }

    @Test
    void testCreateExpenseFromText_withInvalidJson() throws ExecutionException, InterruptedException {
        // Given
        String userInput = "测试输入";
        String invalidJson = "{ invalid json";

        when(qwenClient.chat(anyString(), eq(userInput))).thenReturn(invalidJson);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            expenseService.createExpenseFromText(testTripId, userInput);
        });

        verify(qwenClient, times(1)).chat(anyString(), eq(userInput));
        verify(expenseRepository, never()).save(anyString(), any(Expense.class));
    }

    @Test
    void testCreateExpenseFromText_llmThrowsException() throws ExecutionException, InterruptedException {
        // Given
        String userInput = "测试输入";
        when(qwenClient.chat(anyString(), eq(userInput)))
                .thenThrow(new RuntimeException("LLM service error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            expenseService.createExpenseFromText(testTripId, userInput);
        });

        verify(expenseRepository, never()).save(anyString(), any(Expense.class));
    }

    @Test
    void testCreateExpenseFromText_repositoryThrowsException() throws ExecutionException, InterruptedException {
        // Given
        String userInput = "花了100元";
        String llmResponse = """
                {
                  "category": "other",
                  "amountCents": 10000,
                  "note": "其他支出"
                }
                """;

        when(qwenClient.chat(anyString(), eq(userInput))).thenReturn(llmResponse);
        doThrow(new RuntimeException("Database error"))
                .when(expenseRepository).save(anyString(), any(Expense.class));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            expenseService.createExpenseFromText(testTripId, userInput);
        });
    }

    @Test
    void testGetExpensesByTripId_success() throws ExecutionException, InterruptedException {
        // Given
        when(expenseRepository.findByTripId(testTripId)).thenReturn(testExpenses);

        // When
        List<Expense> result = expenseService.getExpensesByTripId(testTripId);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("expense-1", result.get(0).getId());
        assertEquals("expense-2", result.get(1).getId());
        assertEquals("expense-3", result.get(2).getId());

        verify(expenseRepository, times(1)).findByTripId(testTripId);
    }

    @Test
    void testGetExpensesByTripId_emptyList() throws ExecutionException, InterruptedException {
        // Given
        when(expenseRepository.findByTripId(testTripId)).thenReturn(new ArrayList<>());

        // When
        List<Expense> result = expenseService.getExpensesByTripId(testTripId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetExpensesByTripId_repositoryThrowsException() throws ExecutionException, InterruptedException {
        // Given
        when(expenseRepository.findByTripId(testTripId))
                .thenThrow(new ExecutionException(new RuntimeException("Database error")));

        // When & Then
        assertThrows(ExecutionException.class, () -> {
            expenseService.getExpensesByTripId(testTripId);
        });
    }

    @Test
    void testCalculateExpenseByCategory_success() throws ExecutionException, InterruptedException {
        // Given
        when(expenseRepository.findByTripId(testTripId)).thenReturn(testExpenses);

        // When
        Map<String, Long> result = expenseService.calculateExpenseByCategory(testTripId);

        // Then
        assertNotNull(result);
        assertEquals(8000L, result.get("food"));  // 5000 + 3000
        assertEquals(1200L, result.get("transport"));
        assertNull(result.get("hotel"));  // 没有hotel类别的支出

        verify(expenseRepository, times(1)).findByTripId(testTripId);
    }

    @Test
    void testCalculateExpenseByCategory_emptyExpenses() throws ExecutionException, InterruptedException {
        // Given
        when(expenseRepository.findByTripId(testTripId)).thenReturn(new ArrayList<>());

        // When
        Map<String, Long> result = expenseService.calculateExpenseByCategory(testTripId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCalculateExpenseByCategory_multipleCategories() throws ExecutionException, InterruptedException {
        // Given
        List<Expense> diverseExpenses = Arrays.asList(
                Expense.builder().category("food").amountCents(2000L).build(),
                Expense.builder().category("transport").amountCents(1000L).build(),
                Expense.builder().category("hotel").amountCents(30000L).build(),
                Expense.builder().category("sight").amountCents(5000L).build(),
                Expense.builder().category("other").amountCents(500L).build(),
                Expense.builder().category("food").amountCents(1500L).build()
        );

        when(expenseRepository.findByTripId(testTripId)).thenReturn(diverseExpenses);

        // When
        Map<String, Long> result = expenseService.calculateExpenseByCategory(testTripId);

        // Then
        assertEquals(3500L, result.get("food"));  // 2000 + 1500
        assertEquals(1000L, result.get("transport"));
        assertEquals(30000L, result.get("hotel"));
        assertEquals(5000L, result.get("sight"));
        assertEquals(500L, result.get("other"));
    }

    @Test
    void testCalculateExpenseByCategory_singleCategory() throws ExecutionException, InterruptedException {
        // Given
        List<Expense> singleCategoryExpenses = Arrays.asList(
                Expense.builder().category("food").amountCents(1000L).build(),
                Expense.builder().category("food").amountCents(2000L).build(),
                Expense.builder().category("food").amountCents(3000L).build()
        );

        when(expenseRepository.findByTripId(testTripId)).thenReturn(singleCategoryExpenses);

        // When
        Map<String, Long> result = expenseService.calculateExpenseByCategory(testTripId);

        // Then
        assertEquals(1, result.size());
        assertEquals(6000L, result.get("food"));
    }

    @Test
    void testParseExpenseJson_allCategories() throws ExecutionException, InterruptedException {
        // Test all category types
        String[] categories = {"transport", "hotel", "sight", "food", "other"};
        
        for (String category : categories) {
            // Given
            String userInput = "test";
            String llmResponse = String.format("""
                    {
                      "category": "%s",
                      "amountCents": 10000,
                      "note": "test note"
                    }
                    """, category);

            when(qwenClient.chat(anyString(), eq(userInput))).thenReturn(llmResponse);
            doNothing().when(expenseRepository).save(anyString(), any(Expense.class));

            // When
            Expense result = expenseService.createExpenseFromText(testTripId, userInput);

            // Then
            assertEquals(category, result.getCategory());
        }
    }
}
