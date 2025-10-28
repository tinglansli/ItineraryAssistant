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
        
        Expense expense = createTestExpense(TEST_EXPENSE_ID_1, "food", 65000L, "午餐：一兰拉面");
        
        expenseRepository.save(TEST_TRIP_ID, expense);
        
        Optional<Expense> saved = expenseRepository.findById(TEST_TRIP_ID, TEST_EXPENSE_ID_1);
        assertTrue(saved.isPresent(), "应该能找到保存的支出");
        assertEquals("food", saved.get().getCategory());
        assertEquals(65000L, saved.get().getAmountCents());
        
        log.info("✅ 单个支出保存成功");
    }

    @Test
    @Order(2)
    @DisplayName("2. 测试批量保存支出")
    void testSaveAll() throws ExecutionException, InterruptedException {
        log.info("=== 测试2: 批量保存支出 ===");
        
        Expense expense2 = createTestExpense(TEST_EXPENSE_ID_2, "ticket", 15000L, "门票：清水寺");
        Expense expense3 = createTestExpense(TEST_EXPENSE_ID_3, "transport", 5000L, "地铁");
        
        expenseRepository.saveAll(TEST_TRIP_ID, Arrays.asList(expense2, expense3));
        
        List<Expense> all = expenseRepository.findByTripId(TEST_TRIP_ID);
        assertEquals(3, all.size(), "应该有3条支出记录");
        
        log.info("✅ 批量保存成功，共 {} 条支出", all.size());
    }

    @Test
    @Order(3)
    @DisplayName("3. 测试按行程ID查询所有支出")
    void testFindByTripId() throws ExecutionException, InterruptedException {
        log.info("=== 测试3: 按行程ID查询所有支出 ===");
        
        List<Expense> expenses = expenseRepository.findByTripId(TEST_TRIP_ID);
        
        assertNotNull(expenses);
        assertEquals(3, expenses.size());
        
        log.info("✅ 查询到 {} 条支出记录", expenses.size());
    }

    @Test
    @Order(4)
    @DisplayName("4. 测试按类别查询支出")
    void testFindByTripIdAndCategory() throws ExecutionException, InterruptedException {
        log.info("=== 测试4: 按类别查询支出 ===");
        
        List<Expense> foodExpenses = expenseRepository.findByTripIdAndCategory(TEST_TRIP_ID, "food");
        List<Expense> ticketExpenses = expenseRepository.findByTripIdAndCategory(TEST_TRIP_ID, "ticket");
        
        assertEquals(1, foodExpenses.size(), "应该有1条餐饮支出");
        assertEquals(1, ticketExpenses.size(), "应该有1条门票支出");
        assertEquals(65000L, foodExpenses.get(0).getAmountCents());
        
        log.info("✅ 餐饮支出: {} 条, 门票支出: {} 条", foodExpenses.size(), ticketExpenses.size());
    }

    @Test
    @Order(5)
    @DisplayName("5. 测试计算总支出")
    void testCalculateTotalExpense() throws ExecutionException, InterruptedException {
        log.info("=== 测试5: 计算总支出 ===");
        
        long total = expenseRepository.calculateTotalExpense(TEST_TRIP_ID);
        
        assertEquals(85000L, total, "总支出应该是 85000 分");
        
        log.info("✅ 总支出: {} 分 ({}元)", total, total / 100.0);
    }

    @Test
    @Order(6)
    @DisplayName("6. 测试按类别统计支出")
    void testCalculateExpenseByCategory() throws ExecutionException, InterruptedException {
        log.info("=== 测试6: 按类别统计支出 ===");
        
        Map<String, Long> categoryTotals = expenseRepository.calculateExpenseByCategory(TEST_TRIP_ID);
        
        assertEquals(3, categoryTotals.size(), "应该有3个类别");
        assertEquals(65000L, categoryTotals.get("food"));
        assertEquals(15000L, categoryTotals.get("ticket"));
        assertEquals(5000L, categoryTotals.get("transport"));
        
        log.info("✅ 类别统计:");
        categoryTotals.forEach((category, amount) -> 
            log.info("   - {}: {} 分", category, amount));
    }

    @Test
    @Order(7)
    @DisplayName("7. 测试删除单个支出")
    void testDelete() throws ExecutionException, InterruptedException {
        log.info("=== 测试7: 删除单个支出 ===");
        
        expenseRepository.delete(TEST_TRIP_ID, TEST_EXPENSE_ID_1);
        
        Optional<Expense> deleted = expenseRepository.findById(TEST_TRIP_ID, TEST_EXPENSE_ID_1);
        assertFalse(deleted.isPresent(), "支出应该已被删除");
        
        List<Expense> remaining = expenseRepository.findByTripId(TEST_TRIP_ID);
        assertEquals(2, remaining.size(), "应该剩余2条支出");
        
        long newTotal = expenseRepository.calculateTotalExpense(TEST_TRIP_ID);
        assertEquals(20000L, newTotal, "新的总支出应该是 20000 分");
        
        log.info("✅ 删除成功，剩余 {} 条支出，总计: {} 分", remaining.size(), newTotal);
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
