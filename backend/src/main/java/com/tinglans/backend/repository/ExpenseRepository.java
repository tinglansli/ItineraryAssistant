package com.tinglans.backend.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.tinglans.backend.domain.Expense;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Expense 数据访问层
 * 管理 trips/{tripId}/expenses 子集合
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ExpenseRepository {

    private static final String COLLECTION_TRIPS = "trips";
    private static final String COLLECTION_EXPENSES = "expenses";

    private final Firestore firestore;

    /**
     * 保存单个支出记录
     */
    public void save(String tripId, Expense expense) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore
                .collection(COLLECTION_TRIPS)
                .document(tripId)
                .collection(COLLECTION_EXPENSES)
                .document(expense.getId());

        Map<String, Object> data = convertExpenseToMap(expense);
        ApiFuture<WriteResult> result = docRef.set(data);
        result.get();
        
        log.debug("保存支出到 Firestore: tripId={}, expenseId={}", tripId, expense.getId());
    }

    /**
     * 批量保存支出记录
     */
    public void saveAll(String tripId, List<Expense> expenses) throws ExecutionException, InterruptedException {
        if (expenses == null || expenses.isEmpty()) {
            return;
        }

        CollectionReference expensesRef = firestore
                .collection(COLLECTION_TRIPS)
                .document(tripId)
                .collection(COLLECTION_EXPENSES);

        WriteBatch batch = firestore.batch();
        for (Expense expense : expenses) {
            DocumentReference docRef = expensesRef.document(expense.getId());
            batch.set(docRef, convertExpenseToMap(expense));
        }
        
        ApiFuture<List<WriteResult>> result = batch.commit();
        result.get();
        
        log.info("批量保存支出到 Firestore: tripId={}, count={}", tripId, expenses.size());
    }

    /**
     * 根据ID获取单个支出记录
     */
    public Optional<Expense> findById(String tripId, String expenseId) 
            throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore
                .collection(COLLECTION_TRIPS)
                .document(tripId)
                .collection(COLLECTION_EXPENSES)
                .document(expenseId);

        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();

        if (!document.exists()) {
            return Optional.empty();
        }

        log.debug("从 Firestore 获取支出: tripId={}, expenseId={}", tripId, expenseId);
        return Optional.of(convertDocumentToExpense(document));
    }

    /**
     * 获取某个行程的所有支出记录
     */
    public List<Expense> findByTripId(String tripId) throws ExecutionException, InterruptedException {
        CollectionReference expensesRef = firestore
                .collection(COLLECTION_TRIPS)
                .document(tripId)
                .collection(COLLECTION_EXPENSES);

        Query query = expensesRef.orderBy("happenedAt", Query.Direction.DESCENDING);
        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        log.debug("从 Firestore 获取行程支出列表: tripId={}, count={}", tripId, documents.size());
        
        return documents.stream()
                .map(this::convertDocumentToExpense)
                .collect(Collectors.toList());
    }

    /**
     * 按类别获取支出记录
     */
    public List<Expense> findByTripIdAndCategory(String tripId, String category) 
            throws ExecutionException, InterruptedException {
        CollectionReference expensesRef = firestore
                .collection(COLLECTION_TRIPS)
                .document(tripId)
                .collection(COLLECTION_EXPENSES);

        Query query = expensesRef
                .whereEqualTo("category", category)
                .orderBy("happenedAt", Query.Direction.DESCENDING);
        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        log.debug("从 Firestore 获取指定类别支出: tripId={}, category={}, count={}", 
                tripId, category, documents.size());
        
        return documents.stream()
                .map(this::convertDocumentToExpense)
                .collect(Collectors.toList());
    }

    /**
     * 计算某个行程的总支出
     */
    public long calculateTotalExpense(String tripId) throws ExecutionException, InterruptedException {
        List<Expense> expenses = findByTripId(tripId);
        long total = expenses.stream()
                .mapToLong(Expense::getAmountCents)
                .sum();
        
        log.debug("计算行程总支出: tripId={}, total={}", tripId, total);
        return total;
    }

    /**
     * 按类别统计支出
     */
    public Map<String, Long> calculateExpenseByCategory(String tripId) 
            throws ExecutionException, InterruptedException {
        List<Expense> expenses = findByTripId(tripId);
        
        Map<String, Long> categoryTotals = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.summingLong(Expense::getAmountCents)
                ));
        
        log.debug("按类别统计支出: tripId={}, categories={}", tripId, categoryTotals.keySet());
        return categoryTotals;
    }

    /**
     * 删除单个支出记录
     */
    public void delete(String tripId, String expenseId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore
                .collection(COLLECTION_TRIPS)
                .document(tripId)
                .collection(COLLECTION_EXPENSES)
                .document(expenseId);

        ApiFuture<WriteResult> result = docRef.delete();
        result.get();
        
        log.debug("从 Firestore 删除支出: tripId={}, expenseId={}", tripId, expenseId);
    }

    /**
     * 删除某个行程的所有支出记录
     */
    public void deleteByTripId(String tripId) throws ExecutionException, InterruptedException {
        CollectionReference expensesRef = firestore
                .collection(COLLECTION_TRIPS)
                .document(tripId)
                .collection(COLLECTION_EXPENSES);

        ApiFuture<QuerySnapshot> future = expensesRef.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        WriteBatch batch = firestore.batch();
        for (DocumentSnapshot doc : documents) {
            batch.delete(doc.getReference());
        }
        
        ApiFuture<List<WriteResult>> result = batch.commit();
        result.get();
        
        log.info("删除行程所有支出: tripId={}, count={}", tripId, documents.size());
    }

    // ========== 辅助转换方法 ==========

    private Map<String, Object> convertExpenseToMap(Expense expense) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", expense.getId());
        map.put("tripId", expense.getTripId());
        map.put("category", expense.getCategory());
        map.put("amountCents", expense.getAmountCents());
        map.put("note", expense.getNote());
        map.put("happenedAt", expense.getHappenedAt());
        return map;
    }

    private Expense convertDocumentToExpense(DocumentSnapshot doc) {
        return Expense.builder()
                .id(doc.getString("id"))
                .tripId(doc.getString("tripId"))
                .category(doc.getString("category"))
                .amountCents(doc.getLong("amountCents"))
                .note(doc.getString("note"))
                .happenedAt(doc.getDate("happenedAt").toInstant())
                .build();
    }
}
