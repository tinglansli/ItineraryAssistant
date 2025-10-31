package com.tinglans.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParseException;
import com.tinglans.backend.common.BusinessException;
import com.tinglans.backend.common.ResponseCode;
import com.tinglans.backend.domain.Activity;
import com.tinglans.backend.domain.Day;
import com.tinglans.backend.domain.Trip;
import com.tinglans.backend.dto.TripSummary;
import com.tinglans.backend.repository.TripRepository;
import com.tinglans.backend.thirdparty.amap.AmapClient;
import com.tinglans.backend.thirdparty.amap.dto.AmapPoi;
import com.tinglans.backend.thirdparty.llm.QwenClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private final AmapClient amapClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 校验方法 ====================

    /**
     * 校验用户输入
     */
    public void validateUserInput(String userInput) {
        if (!StringUtils.hasText(userInput)) {
            throw new BusinessException(ResponseCode.INVALID_PARAM, "用户输入不能为空");
        }
    }

    /**
     * 校验用户ID
     */
    public void validateUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(ResponseCode.INVALID_PARAM, "用户ID不能为空");
        }
    }

    /**
     * 校验并获取行程
     */
    public Trip validateAndGetTrip(String tripId) throws ExecutionException, InterruptedException {
        Optional<Trip> tripOpt = getTripById(tripId);
        if (tripOpt.isEmpty()) {
            throw new BusinessException(ResponseCode.TRIP_NOT_FOUND);
        }
        return tripOpt.get();
    }

    /**
     * 校验行程权限
     */
    public void validateTripPermission(Trip trip, String userId) {
        if (!trip.getUserId().equals(userId)) {
            throw new BusinessException(ResponseCode.PERMISSION_DENIED, "无权操作该行程");
        }
    }

    // ==================== 业务方法 ====================

    /**
     * 从文本创建行程预览
     *
     * @param userInput 用户输入的文本描述
     * @param userId    用户ID
     * @return 生成的行程对象
     */
    public Trip createTripFromText(String userInput, String userId) throws ExecutionException, InterruptedException {
        validateUserInput(userInput);
        validateUserId(userId);
        
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
        validateUserId(userId);
        
        log.info("开始确认行程: tripId={}, userId={}", tripId, userId);

        // 1. 从 Redis 获取行程
        Optional<Trip> tripOpt = tripRepository.getFromCache(tripId);
        if (tripOpt.isEmpty()) {
            throw new BusinessException(ResponseCode.TRIP_EXPIRED);
        }

        Trip trip = tripOpt.get();

        // 2. 权限校验
        validateTripPermission(trip, userId);

        // 3. 设置 updatedAt 时间戳
        trip.setUpdatedAt(Instant.now());

        // 4. 持久化到 Firestore
        tripRepository.saveToFirestore(trip);

        // 5. 更新 Redis 缓存
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
            你是一个专业的旅行规划助手。根据用户提供的目的地、天数、人数和预算信息，生成详细的旅行行程规划。
            
            【重要】你必须只返回纯JSON格式的数据，不要包含任何其他文字、解释或markdown代码块标记。
            直接从 { 开始，到 } 结束，确保是可以被JSON解析器直接解析的有效JSON。
            
            ===== 必须输出的完整 JSON 格式 =====
            {
              "tripName": "行程名称（10个字以内）",
              "destination": "目的地（城市名，如厦门/东京）",
              "startDate": "开始日期(yyyy-MM-dd格式)",
              "endDate": "结束日期(yyyy-MM-dd格式)",
              "headcount": {
                "adults": 成人数量（整数）,
                "children": 儿童数量（整数）
              },
              "days": [
                {
                  "dayIndex": 1,
                  "activities": [
                    {
                      "type": "transport/hotel/sight/food/other",
                      "title": "活动描述（10个字以内，如：参观伏见稻荷大社）",
                      "locationName": "地点名称（如：伏见稻荷大社）",
                      "startTime": "HH:mm",
                      "endTime": "HH:mm",
                      "estimatedCost": 预估费用（单位：分，100分=1元）
                    }
                  ]
                },
                {
                  "dayIndex": 2,
                  "activities": [...]
                }
              ]
            }
            
            ===== 关键字段说明 =====
            - tripName: 行程名称，建议反映主题和目的地
            - destination: 目的地名称
            - startDate/endDate: 必须是 yyyy-MM-dd 格式的有效日期
            - headcount: 同行人数信息，adults和children都必须是整数（不能为null），从用户输入中推断（无明确信息时默认adults=1, children=0）
            - days: 数组, 长度必须等于用户指定的旅游天数
            - dayIndex: 从 1 开始的连续整数，不能跳过或重复
            - type: 只能使用这 5 个值: transport、hotel、sight、food、other
            - locationName: 能够在地图上找到的真实地点名称（真实餐厅名称、酒店名称、景点名称等）
            - startTime/endTime: 24小时制, 格式为 HH:mm (如 09:00、14:30)
            - estimatedCost: 整数, 单位是分(1元=100分)
            
            ===== 生成行程的逻辑规则 =====
            0. 所有活动的预估费用加起来应该在总预算附近（允许有10%的浮动）
            1. 每天安排 3-5 个活动，确保时间分配合理且地理位置相近
            2. 第一天应该包含交通（从出发地到目的地）和酒店入住
            3. 最后一天应该包含交通返回（从目的地回到出发地）
            4. 根据用户提供的人数和预算信息合理分配每个活动的费用
            5. 根据目的地的季节特点和天气条件推荐活动
            6. 考虑用户的旅行偏好（已在用户消息中提供），融入到行程推荐中

            ===== 生成行程的格式规则 =====
            1. 必须生成指定天数的完整行程。如果用户说"7天", days 数组必须包含 1-7 天的所有数据
            2. 所有数值字段(dayIndex、estimatedCost、adults、children)必须是数字类型，不能是字符串
            3. 返回的必须是可被标准 JSON 解析器解析的完整 JSON 对象，从 { 开始到 } 结束
            4. headcount字段必须存在，不能为null或空
            5. 不要添加注释

            **重要：生成完成后，检查输出是否符合格式规则。如果不符合则重新生成。**
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

            // 解析同行人数
            Trip.Headcount headcount = null;
            if (root.has("headcount") && root.get("headcount").isObject()) {
                JsonNode headcountNode = root.get("headcount");
                int adults = headcountNode.has("adults") ? headcountNode.get("adults").asInt() : 1;
                int children = headcountNode.has("children") ? headcountNode.get("children").asInt() : 0;
                headcount = Trip.Headcount.builder()
                        .adults(adults)
                        .children(children)
                        .build();
            }

            // 解析天数列表
            List<Day> days = new ArrayList<>();
            if (root.has("days") && root.get("days").isArray()) {
                JsonNode daysArray = root.get("days");
                for (JsonNode dayNode : daysArray) {
                    Day day = parseDay(dayNode, startDate, destination);
                    days.add(day);
                }
            }
            
            // 计算总预算（所有 Activity 的 estimatedCost 之和）
            Long totalBudget = calculateTotalBudget(days);
            
            // 构建 Trip 对象
            Trip trip = Trip.builder()
                    .id(UUID.randomUUID().toString())
                    .title(tripName)
                    .destination(destination)
                    .startDate(startDate)
                    .endDate(endDate)
                    .totalBudget(totalBudget)
                    .headcount(headcount)
                    .days(days)
                    .createdAt(Instant.now())
                    .build();
            
            log.debug("行程 JSON 解析成功: tripName={}, days={}, startDate={}, endDate={}, adults={}, children={}, totalBudget={}", 
                    tripName, days.size(), startDate, endDate, 
                    headcount != null ? headcount.getAdults() : 0, 
                    headcount != null ? headcount.getChildren() : 0,
                    totalBudget);
            return trip;
            
        } catch (JsonParseException e) {
            log.error("行程 JSON 解析失败: {}", llmJsonResponse);
            throw new BusinessException(ResponseCode.BAD_REQUEST, "Invalid JSON format for trip data");
        } catch (Exception e) {
            log.error("行程 JSON 解析过程中发生未知错误: {}", llmJsonResponse, e);
            throw new BusinessException(ResponseCode.INTERNAL_ERROR, e);
        }
    }
    
    /**
     * 计算行程总预算
     * 
     * @param days 行程天数列表
     * @return 总预算（分）
     */
    private Long calculateTotalBudget(List<Day> days) {
        long totalBudget = 0;
        
        if (days == null) {
            return totalBudget;
        }
        
        for (Day day : days) {
            if (day.getActivities() == null) {
                continue;
            }
            
            for (Activity activity : day.getActivities()) {
                if (activity.getEstimatedCost() != null) {
                    totalBudget += activity.getEstimatedCost();
                }
            }
        }
        
        log.debug("行程总预算计算完成: totalBudget={}", totalBudget);
        return totalBudget;
    }
    
    /**
     * 解析单个 Day 对象
     * 
     * @param dayNode JSON 节点
     * @param startDate 行程开始日期（用于计算当天日期）
     */
    private Day parseDay(JsonNode dayNode, LocalDate startDate, String destination) throws Exception {
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
                Activity activity = parseActivity(activityNode, dayIndex, destination);
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
     * 根据 locationName 调用高德地图 API 获取地理位置信息
     */
    private Activity parseActivity(JsonNode activityNode, int dayIndex, String destination) throws Exception {
        String id = UUID.randomUUID().toString();
        String type = activityNode.has("type") ? activityNode.get("type").asText() : "other";
        String title = activityNode.has("title") ? activityNode.get("title").asText() : "";
        String locationName = activityNode.has("locationName") ? activityNode.get("locationName").asText() : "";
        String startTime = activityNode.has("startTime") ? activityNode.get("startTime").asText() : "";
        String endTime = activityNode.has("endTime") ? activityNode.get("endTime").asText() : "";
        Long estimatedCost = activityNode.has("estimatedCost") ? activityNode.get("estimatedCost").asLong() : 0L;
        
        // 根据 locationName 调用高德地图 API 获取完整 POI 信息
        AmapPoi poi = null;
        if (StringUtils.hasText(locationName)) {
            // 使用 Trip 的 destination 作为 region 限定搜索范围
            poi = amapClient.searchLocation(locationName, destination);
            if (poi != null) {
                log.debug("Activity POI 信息已获取: locationName={}, poiName={}, address={}, location={}", 
                        locationName, poi.getName(), poi.getAddress(), poi.getLocation());
            } else {
                log.warn("未能从高德地图获取位置信息: locationName={}, region={}", locationName, destination);
            }
        }
        
        Activity activity = Activity.builder()
                .id(id)
                .dayIndex(dayIndex)
                .type(type)
                .title(title)
                .locationName(locationName)
                .poi(poi)
                .startTime(startTime)
                .endTime(endTime)
                .estimatedCost(estimatedCost)
                .build();
        
        log.debug("Activity 解析完成: title={}, locationName={}, type={}", title, locationName, type);
        return activity;
    }

    // ==================== 转换方法 ====================

    /**
     * 将Trip转换为TripSummary（不包含Days详细信息）
     *
     * @param trip 完整的Trip对象
     * @return TripSummary概要对象
     */
    public TripSummary convertToSummary(Trip trip) {
        if (trip == null) {
            return null;
        }

        TripSummary.Headcount headcount = null;
        if (trip.getHeadcount() != null) {
            headcount = TripSummary.Headcount.builder()
                    .adults(trip.getHeadcount().getAdults())
                    .children(trip.getHeadcount().getChildren())
                    .build();
        }

        return TripSummary.builder()
                .id(trip.getId())
                .userId(trip.getUserId())
                .title(trip.getTitle())
                .destination(trip.getDestination())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .totalBudget(trip.getTotalBudget())
                .headcount(headcount)
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .build();
    }

    /**
     * 将Trip列表转换为TripSummary列表
     *
     * @param trips Trip列表
     * @return TripSummary列表
     */
    public List<TripSummary> convertToSummaryList(List<Trip> trips) {
        if (trips == null) {
            return new ArrayList<>();
        }

        return trips.stream()
                .map(this::convertToSummary)
                .toList();
    }
}

