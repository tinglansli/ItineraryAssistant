package com.tinglans.backend.repository;

import com.google.cloud.firestore.Firestore;
import com.tinglans.backend.domain.Expense;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExpenseRepository 测试
 */
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExpenseRepositoryTest {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private Firestore firestore;

    private static final String TEST_TRIP_ID = "test-trip-expense-001";
    private static final String TEST_EXPENSE_ID_1 = "test-expense-001";
    private static final String TEST_EXPENSE_ID_2 = "test-expense-002";
    private static final String TEST_EXPENSE_ID_3 = "test-expense-003";

    private Expense createTestExpense(String expenseId, String category, long amountCents, String note) {
        return Expense.builder()
                .id(expenseId)
                .tripId(TEST_TRIP_ID)
                .category(category)
                .amountCents(amountCents)
                .note(note)
                .happenedAt(Instant.now())
                .build();
    }

    @Test
    @Order(1)
    @DisplayName("1. 测试保存单个支出")
    void testSave() throws ExecutionException, InterruptedException {
        log.info("=== 测试1: 保存单个支出 ===");
        
        // 保存第一条支出
        Expense expense1 = createTestExpense(TEST_EXPENSE_ID_1, "food", 65000L, "午餐：一兰拉面");
        expenseRepository.save(TEST_TRIP_ID, expense1);
        
        // 保存第二条支出
        Expense expense2 = createTestExpense(TEST_EXPENSE_ID_2, "ticket", 8000L, "门票：浅草寺");
        expenseRepository.save(TEST_TRIP_ID, expense2);
        
        // 保存第三条支出
        Expense expense3 = createTestExpense(TEST_EXPENSE_ID_3, "transport", 1200L, "地铁");
        expenseRepository.save(TEST_TRIP_ID, expense3);
        
        Optional<Expense> saved = expenseRepository.findById(TEST_TRIP_ID, TEST_EXPENSE_ID_1);
        assertTrue(saved.isPresent(), "应该能找到保存的支出");
        assertEquals("food", saved.get().getCategory());
        assertEquals(65000L, saved.get().getAmountCents());
        
        log.info("✅ 已保存3条测试支出");
    }

    @Test
    @Order(2)
    @DisplayName("2. 测试按行程ID查询所有支出")
    void testFindByTripId() throws ExecutionException, InterruptedException {
        log.info("=== 测试2: 按行程ID查询所有支出 ===");
        
        List<Expense> expenses = expenseRepository.findByTripId(TEST_TRIP_ID);
        
        assertNotNull(expenses);
        assertEquals(3, expenses.size());
        
        log.info("✅ 查询到 {} 条支出记录", expenses.size());
    }

    @Test
    @Order(3)
    @DisplayName("3. 测试按类别查询支出")
    void testFindByTripIdAndCategory() throws ExecutionException, InterruptedException {
        log.info("=== 测试3: 按类别查询支出 ===");
        
        List<Expense> foodExpenses = expenseRepository.findByTripIdAndCategory(TEST_TRIP_ID, "food");
        List<Expense> ticketExpenses = expenseRepository.findByTripIdAndCategory(TEST_TRIP_ID, "ticket");
        
        assertEquals(1, foodExpenses.size(), "应该有1条餐饮支出");
        assertEquals(1, ticketExpenses.size(), "应该有1条门票支出");
        assertEquals(65000L, foodExpenses.get(0).getAmountCents());
        
        log.info("✅ 餐饮支出: {} 条, 门票支出: {} 条", foodExpenses.size(), ticketExpenses.size());
    }

    @Test
    @Order(4)
    @DisplayName("4. 测试删除单个支出")
    void testDelete() throws ExecutionException, InterruptedException {
        log.info("=== 测试4: 删除单个支出 ===");
        
        expenseRepository.delete(TEST_TRIP_ID, TEST_EXPENSE_ID_1);
        
        Optional<Expense> deleted = expenseRepository.findById(TEST_TRIP_ID, TEST_EXPENSE_ID_1);
        assertFalse(deleted.isPresent(), "支出应该已被删除");
        
        List<Expense> remaining = expenseRepository.findByTripId(TEST_TRIP_ID);
        assertEquals(2, remaining.size(), "应该剩余2条支出");
        
        log.info("✅ 删除成功，剩余 {} 条支出", remaining.size());
    }

    @AfterAll
    static void cleanup(@Autowired Firestore firestore, @Autowired ExpenseRepository expenseRepository) 
            throws ExecutionException, InterruptedException {
        log.info("=== 清理测试数据 ===");
        
        try {
            expenseRepository.deleteByTripId(TEST_TRIP_ID);
            // 同时清理行程文档
            firestore.collection("trips").document(TEST_TRIP_ID).delete().get();
            log.info("✅ 已清理所有测试数据");
        } catch (Exception e) {
            log.warn("⚠️ 清理数据失败: {}", e.getMessage());
        }
        
        log.info("✅ 清理完成");
    }
}
