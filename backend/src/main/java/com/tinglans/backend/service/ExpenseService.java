package com.tinglans.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinglans.backend.domain.Expense;
import com.tinglans.backend.repository.ExpenseRepository;
import com.tinglans.backend.thirdparty.llm.QwenClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * 开销业务逻辑层
 * 负责：记账、查询、分类统计等业务流程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final QwenClient qwenClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从文本创建开销记录
     *
     * @param tripId      行程ID
     * @param textInput   用户输入的文本（如："今天吃拉面花了50块"）
     * @return 创建的开销对象
     */
    public Expense createExpenseFromText(String tripId, String textInput) 
            throws ExecutionException, InterruptedException {
        log.info("从文本创建开销记录: tripId={}, input={}", tripId, textInput);

        // 1. LLM 解析文本中的金额和类别（业务逻辑）
        String systemPrompt = buildExpenseParsePrompt();
        String parsedJson = qwenClient.chat(systemPrompt, textInput);

        // 2. 解析 JSON 为 Expense 对象
        Expense expense = parseExpenseJson(parsedJson);
        expense.setId(UUID.randomUUID().toString());
        expense.setTripId(tripId);
        expense.setHappenedAt(Instant.now());
        expense.setCreatedAt(Instant.now());

        // 3. 保存到数据库
        expenseRepository.save(tripId, expense);

        log.info("文本记账成功: expenseId={}, category={}, amount={}", 
                expense.getId(), expense.getCategory(), expense.getAmountCents());
        return expense;
    }

    /**
     * 获取行程的所有开销记录
     *
     * @param tripId 行程ID
     * @return 开销列表
     */
    public List<Expense> getExpensesByTripId(String tripId) throws ExecutionException, InterruptedException {
        log.debug("获取行程开销列表: tripId={}", tripId);
        return expenseRepository.findByTripId(tripId);
    }

    /**
     * 按类别统计开销
     *
     * @param tripId 行程ID
     * @return 类别 -> 金额的映射
     */
    public Map<String, Long> calculateExpenseByCategory(String tripId) 
            throws ExecutionException, InterruptedException {
        log.debug("按类别统计开销: tripId={}", tripId);

        List<Expense> expenses = expenseRepository.findByTripId(tripId);
        Map<String, Long> categoryTotals = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.summingLong(Expense::getAmountCents)
                ));
        
        log.debug("类别统计完成: tripId={}, categories={}", tripId, categoryTotals.keySet());
        return categoryTotals;
    }

    private String buildExpenseParsePrompt() {
        return """
            你是一个智能记账助手。
            从用户口述中提取支出信息，返回 JSON 格式：
            {
              "category": "分类（transport/hotel/sight/food/other）",
              "amountCents": 金额（单位：分，1元=100分）,
              "note": "备注说明"
            }
            
            规则：
            1. category 字段固定使用以下值之一：transport（交通）、hotel（住宿）、sight（景点）、food（餐厅）、other（其他）
            2. 如果金额不清楚，设为 0
            3. 如果类别模糊，优先选择最接近的，实在无法判断选 other
            4. note 应该简洁明了地描述支出内容
            """;
    }

    /**
     * 解析 LLM 返回的开销 JSON
     */
    private Expense parseExpenseJson(String json) {
        try {
            log.debug("开始解析开销 JSON: {}", json);
            
            JsonNode root = objectMapper.readTree(json);
            
            // 解析字段
            String category = root.has("category") ? root.get("category").asText() : "other";
            long amountCents = root.has("amountCents") ? root.get("amountCents").asLong() : 0L;
            String note = root.has("note") ? root.get("note").asText() : "";
            
            Expense expense = Expense.builder()
                    .category(category)
                    .amountCents(amountCents)
                    .note(note)
                    .build();
            
            log.debug("开销 JSON 解析成功: category={}, amount={}", category, amountCents);
            return expense;
            
        } catch (Exception e) {
            log.error("开销 JSON 解析失败: {}", json, e);
            throw new RuntimeException("开销 JSON 解析失败: " + e.getMessage(), e);
        }
    }
}
