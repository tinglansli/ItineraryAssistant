package com.tinglans.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinglans.backend.domain.Activity;
import com.tinglans.backend.domain.Day;
import com.tinglans.backend.domain.Trip;
import com.tinglans.backend.repository.TripRepository;
import com.tinglans.backend.thirdparty.llm.QwenClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * 行程业务逻辑层
 * 负责：行程创建、查询、确认等业务流程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final UserService userService;
    private final QwenClient qwenClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从文本创建行程预览
     *
     * @param userInput 用户输入的文本描述
     * @param userId    用户ID
     * @return 生成的行程对象
     */
    public Trip createTripFromText(String userInput, String userId) throws ExecutionException, InterruptedException {
        log.info("开始从文本创建行程: userId={}", userId);

        // 1. 构建 LLM Prompt
        String systemPrompt = buildTripGenerationSystemPrompt();
        String enhancedUserMessage = buildTripGenerationUserMessage(userInput, userId);

        // 2. 调用 LLM 生成行程 JSON
        String llmJsonResponse = qwenClient.chat(systemPrompt, enhancedUserMessage);

        // 3. 解析 LLM 响应为 Trip 对象
        Trip trip = parseLlmResponseToTrip(llmJsonResponse);
        trip.setUserId(userId);

        // 4. 存入 Redis 缓存
        tripRepository.saveToCache(trip);

        log.info("行程创建成功: tripId={}", trip.getId());
        return trip;
    }

    /**
     * 获取行程详情（优先从缓存，缓存未命中则查数据库）
     *
     * @param tripId 行程ID
     * @return 行程对象
     */
    public Optional<Trip> getTripById(String tripId) throws ExecutionException, InterruptedException {
        log.debug("获取行程: tripId={}", tripId);

        // 1. 优先从 Redis 获取
        Optional<Trip> cachedTrip = tripRepository.getFromCache(tripId);
        if (cachedTrip.isPresent()) {
            log.debug("从缓存获取行程成功: tripId={}", tripId);
            return cachedTrip;
        }

        // 2. 缓存未命中，从 Firestore 获取
        Optional<Trip> trip = tripRepository.getFromFirestore(tripId);
        if (trip.isPresent()) {
            // 3. 回写缓存
            tripRepository.saveToCache(trip.get());
            log.debug("从数据库获取行程并回写缓存: tripId={}", tripId);
        }

        return trip;
    }

    /**
     * 确认行程（持久化到 Firestore）
     *
     * @param tripId 行程ID
     * @param userId 用户ID
     * @return 确认后的行程对象
     */
    public Trip confirmTrip(String tripId, String userId) throws ExecutionException, InterruptedException {
        log.info("开始确认行程: tripId={}, userId={}", tripId, userId);

        // 1. 从 Redis 获取行程
        Optional<Trip> tripOpt = tripRepository.getFromCache(tripId);
        if (tripOpt.isEmpty()) {
            throw new IllegalStateException("行程不存在或已过期: " + tripId);
        }

        Trip trip = tripOpt.get();

        // 2. 权限校验（业务逻辑）
        if (!trip.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权确认该行程");
        }

        // 3. 持久化到 Firestore
        tripRepository.saveToFirestore(trip);

        // 4. 更新 Redis 缓存（延长 TTL 为 6 小时）
        tripRepository.saveToCache(trip);

        log.info("行程确认成功: tripId={}", tripId);
        return trip;
    }

    /**
     * 获取用户的已确认行程列表
     *
     * @param userId 用户ID
     * @return 已确认的行程列表
     */
    public List<Trip> getConfirmedTripsByUserId(String userId) throws ExecutionException, InterruptedException {
        log.debug("获取用户已确认行程列表: userId={}", userId);
        return tripRepository.findConfirmedTripsByUserId(userId);
    }

    /**
     * 构建行程生成的系统 Prompt
     */
    private String buildTripGenerationSystemPrompt() {
        return """
            你是一个专业的旅行规划助手。
            你需要根据用户提供的目的地、天数和偏好，生成详细的旅行行程规划。
            请以 JSON 格式返回行程，包含以下结构：
            {
              "tripName": "行程名称",
              "destination": "目的地",
              "startDate": "开始日期(yyyy-MM-dd格式，如：2024-11-01)",
              "endDate": "结束日期(yyyy-MM-dd格式)",
              "days": [
                {
                  "dayIndex": 1,
                  "activities": [
                    {
                      "type": "活动类型(transport/hotel/sight/food/other)",
                      "title": "活动描述（如：参观伏见稻荷大社）",
                      "locationName": "地点关键词（如：伏见稻荷大社）",
                      "startTime": "开始时间(HH:mm)",
                      "endTime": "结束时间(HH:mm)",
                      "estimatedCost": 预估费用（单位：分，100分=1元）
                    },
                    {
                      ......
                    }
                  ]
                },
                {
                  "dayIndex": 2,
                  "activities": [
                    {
                      ......
                    }
                  ]
                },
                ......
              ]
            }
            
            重要说明：
            1. type 字段固定使用以下值之一：transport（交通）、hotel（住宿）、sight（景点）、food（餐厅）、other（其他）
            2. locationName 是用于地图搜索的关键词，必须准确（如景点名、餐厅名、酒店名）
            3. title 是对活动的描述，可以更详细生动
            4. 不要生成 address、lat、lng、poiId 等字段，后端会自动通过地图API补全
            5. 费用单位是"分"（1元 = 100分）
            6. 每天安排3-5个活动，时间分配合理
            7. startDate 和 endDate 必须是有效的日期格式（yyyy-MM-dd）
            8. 生成旅程的时候要考虑到日期对应的季节（比如冬天可以滑雪、泡温泉）
            """;
    }

    /**
     * 构建增强的用户消息（加入用户偏好）
     */
    private String buildTripGenerationUserMessage(String userInput, String userId) throws ExecutionException, InterruptedException {
        // 获取用户偏好
        List<String> preferences = userService.getPreferencesList(userId);
        
        if (preferences.isEmpty()) {
            return userInput;
        }
        
        String preferencesText = String.join("、", preferences);
        
        // 将用户偏好加入到输入中
        return userInput + "\n\n我的旅行偏好：" + preferencesText;
    }

    /**
     * 解析 LLM 响应为 Trip 对象
     */
    private Trip parseLlmResponseToTrip(String llmJsonResponse) {
        try {
            log.debug("开始解析行程 JSON，长度: {}", llmJsonResponse.length());
            
            JsonNode root = objectMapper.readTree(llmJsonResponse);
            
            // 解析基本信息
            String tripName = root.has("tripName") ? root.get("tripName").asText() : "未命名行程";
            String destination = root.has("destination") ? root.get("destination").asText() : "";
            
            // 解析日期
            LocalDate startDate = null;
            LocalDate endDate = null;
            if (root.has("startDate")) {
                startDate = LocalDate.parse(root.get("startDate").asText());
            }
            if (root.has("endDate")) {
                endDate = LocalDate.parse(root.get("endDate").asText());
            }
            
            // 解析天数列表
            List<Day> days = new ArrayList<>();
            if (root.has("days") && root.get("days").isArray()) {
                JsonNode daysArray = root.get("days");
                for (JsonNode dayNode : daysArray) {
                    Day day = parseDay(dayNode, startDate);
                    days.add(day);
                }
            }
            
            // 构建 Trip 对象
            Trip trip = Trip.builder()
                    .id(UUID.randomUUID().toString())
                    .title(tripName)
                    .destination(destination)
                    .startDate(startDate)
                    .endDate(endDate)
                    .days(days)
                    .createdAt(Instant.now())
                    .build();
            
            log.debug("行程 JSON 解析成功: tripName={}, days={}, startDate={}, endDate={}", 
                    tripName, days.size(), startDate, endDate);
            return trip;
            
        } catch (Exception e) {
            log.error("行程 JSON 解析失败: {}", llmJsonResponse, e);
            throw new RuntimeException("行程 JSON 解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析单个 Day 对象
     * 
     * @param dayNode JSON 节点
     * @param startDate 行程开始日期（用于计算当天日期）
     */
    private Day parseDay(JsonNode dayNode, LocalDate startDate) throws Exception {
        int dayIndex = dayNode.has("dayIndex") ? dayNode.get("dayIndex").asInt() : 1;
        
        // 根据开始日期和 dayIndex 计算当天日期
        LocalDate date = null;
        if (startDate != null) {
            date = startDate.plusDays(dayIndex - 1);
        }
        
        List<Activity> activities = new ArrayList<>();
        if (dayNode.has("activities") && dayNode.get("activities").isArray()) {
            JsonNode activitiesArray = dayNode.get("activities");
            for (JsonNode activityNode : activitiesArray) {
                Activity activity = parseActivity(activityNode, dayIndex);
                activities.add(activity);
            }
        }
        
        return Day.builder()
                .dayIndex(dayIndex)
                .date(date)
                .activities(activities)
                .build();
    }
    
    /**
     * 解析单个 Activity 对象
     */
    private Activity parseActivity(JsonNode activityNode, int dayIndex) throws Exception {
        String id = UUID.randomUUID().toString();
        String type = activityNode.has("type") ? activityNode.get("type").asText() : "other";
        String title = activityNode.has("title") ? activityNode.get("title").asText() : "";
        String locationName = activityNode.has("locationName") ? activityNode.get("locationName").asText() : "";
        String startTime = activityNode.has("startTime") ? activityNode.get("startTime").asText() : "";
        String endTime = activityNode.has("endTime") ? activityNode.get("endTime").asText() : "";
        Long estimatedCost = activityNode.has("estimatedCost") ? activityNode.get("estimatedCost").asLong() : 0L;
        
        return Activity.builder()
                .id(id)
                .dayIndex(dayIndex)
                .type(type)
                .title(title)
                .locationName(locationName)
                .startTime(startTime)
                .endTime(endTime)
                .estimatedCost(estimatedCost)
                .build();
    }
}
