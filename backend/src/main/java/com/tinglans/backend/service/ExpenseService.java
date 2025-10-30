package com.tinglans.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinglans.backend.common.BusinessException;
import com.tinglans.backend.common.ResponseCode;
import com.tinglans.backend.domain.Expense;
import com.tinglans.backend.repository.ExpenseRepository;
import com.tinglans.backend.thirdparty.llm.QwenClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    // ==================== 校验方法 ====================

    /**
     * 校验文本输入
     */
    public void validateTextInput(String textInput) {
        if (!StringUtils.hasText(textInput)) {
            throw new BusinessException(ResponseCode.INVALID_PARAM, "文本输入不能为空");
        }
    }

    // ==================== 业务方法 ====================

    /**
     * 从文本创建开销记录
     *
     * @param tripId      行程ID
     * @param textInput   用户输入的文本（如："今天吃拉面花了50块"）
     * @return 创建的开销对象
     */
    public Expense createExpenseFromText(String tripId, String textInput) 
            throws ExecutionException, InterruptedException {
        validateTextInput(textInput);
        
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
            你是一个智能记账助手。从用户口述中提取支出信息，必须返回有效的 JSON 格式。
            
            【重要】你必须只返回纯JSON格式的数据，不要包含任何其他文字、解释或markdown代码块标记。
            直接从 { 开始，到 } 结束，确保是可以被JSON解析器直接解析的有效JSON。
            
            ===== 必须输出的完整 JSON 格式 =====
            {
              "category": "分类（transport/hotel/sight/food/other）",
              "amountCents": 金额（单位：分，1元=100分）,
              "note": "备注说明"
            }
            
            ===== 关键字段说明 =====
            - category: 必须是以下 5 个值之一：transport（交通）、hotel（住宿）、sight（景点）、food（餐厅）、other（其他）
            - amountCents: 必须是整数，单位是分。例如 50 元应该填 5000，100 元应该填 10000。如果用户未明确说明金额，设为 0
            - note: 字符串类型，对支出内容的简洁说明

            ===== 解析行程的逻辑规则 =====
            1. 如果类别模糊不清，优先选择最接近的类别，实在无法判断则选择 other
            2. 金额必须转换为分（整数），不能是浮点数或字符串
            3. 返回必须是有效的、可被标准 JSON 解析器解析的 JSON 对象

            ===== 解析行程的格式规则 =====
            1. 返回的必须是有效的、可被标准 JSON 解析器解析的完整 JSON 对象，从 { 开始到 } 结束
            2. 所有数值字段(amountCents)必须是数字类型，不能是字符串
            3. 不允许在 JSON 中添加任何注释、说明文字或非结构化的内容
            4. 不要在 JSON 中添加 // 这样的注释
            5. 不要混入任何非 JSON 的文本说明

            **重要：解析完成后，检查输出是否符合格式规则。如果不符合则重新生成。**
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
            
        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            log.error("开销 JSON 解析失败: {}", json);
            throw new BusinessException(ResponseCode.BAD_REQUEST, "Invalid JSON format for expense data");
        } catch (Exception e) {
            log.error("开销 JSON 解析过程中发生未知错误: {}", json, e);
            throw new BusinessException(ResponseCode.INTERNAL_ERROR, e);
        }
    }
}
