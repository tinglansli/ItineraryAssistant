package com.tinglans.backend.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.tinglans.backend.domain.Activity;
import com.tinglans.backend.domain.Day;
import com.tinglans.backend.domain.Trip;
import com.tinglans.backend.thirdparty.amap.dto.AmapPoi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Trip 数据访问层
 * 处理 Firestore 和 Redis 的数据操作
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TripRepository {

    private static final String COLLECTION_TRIPS = "trips";
    private static final String REDIS_KEY_PREFIX = "trip:";

    private final Firestore firestore;
    private final RedisTemplate<String, Object> redisTemplate;
    private final long cacheDefaultTtl;

    /**
     * 保存行程到 Redis 缓存
     */
    public void saveToCache(Trip trip) {
        String key = REDIS_KEY_PREFIX + trip.getId();
        redisTemplate.opsForValue().set(key, trip, cacheDefaultTtl, TimeUnit.SECONDS);
        log.debug("保存行程到 Redis: {}", trip.getId());
    }

    /**
     * 从 Redis 缓存获取行程
     */
    public Optional<Trip> getFromCache(String tripId) {
        String key = REDIS_KEY_PREFIX + tripId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof Trip) {
            log.debug("从 Redis 获取行程: {}", tripId);
            return Optional.of((Trip) cached);
        }
        return Optional.empty();
    }

    /**
     * 从 Redis 删除缓存
     */
    public void deleteFromCache(String tripId) {
        String key = REDIS_KEY_PREFIX + tripId;
        redisTemplate.delete(key);
        log.debug("从 Redis 删除行程: {}", tripId);
    }

    /**
     * 保存行程到 Firestore
     */
    public void saveToFirestore(Trip trip) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_TRIPS).document(trip.getId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("id", trip.getId());
        data.put("userId", trip.getUserId());
        data.put("title", trip.getTitle());
        data.put("destination", trip.getDestination());
        data.put("startDate", trip.getStartDate().toString());
        data.put("endDate", trip.getEndDate().toString());
        data.put("totalBudget", trip.getTotalBudget());
        data.put("headcount", convertHeadcountToMap(trip.getHeadcount()));
        data.put("createdAt", trip.getCreatedAt());
        data.put("updatedAt", trip.getUpdatedAt());
        
        // 将 days 作为嵌入式数组保存
        if (trip.getDays() != null) {
            data.put("days", convertDaysToList(trip.getDays()));
        }

        ApiFuture<WriteResult> result = docRef.set(data);
        result.get();
        
        log.info("保存行程到 Firestore: {}", trip.getId());
    }

    /**
     * 从 Firestore 获取行程
     */
    public Optional<Trip> getFromFirestore(String tripId) 
            throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_TRIPS).document(tripId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();

        if (!document.exists()) {
            return Optional.empty();
        }

        Trip trip = convertDocumentToTrip(document);

        log.debug("从 Firestore 获取行程: {}", tripId);
        return Optional.of(trip);
    }

    /**
     * 根据用户ID查询行程列表
     */
    public List<Trip> findConfirmedTripsByUserId(String userId) 
            throws ExecutionException, InterruptedException {
        CollectionReference tripsRef = firestore.collection(COLLECTION_TRIPS);
        Query query = tripsRef
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING);

        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        return documents.stream()
                .map(this::convertDocumentToTrip)
                .collect(Collectors.toList());
    }

    /**
     * 从 Firestore 删除行程
     */
    public void deleteFromFirestore(String tripId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_TRIPS).document(tripId);
        ApiFuture<WriteResult> result = docRef.delete();
        result.get();
        
        // 同时删除 Redis 缓存
        deleteFromCache(tripId);
        
        log.info("从 Firestore 删除行程: {}", tripId);
    }

    // ========== 辅助转换方法 ==========

    private Map<String, Object> convertHeadcountToMap(Trip.Headcount headcount) {
        Map<String, Object> map = new HashMap<>();
        if (headcount != null) {
            map.put("adults", headcount.getAdults());
            map.put("children", headcount.getChildren());
        }
        return map;
    }

    private List<Map<String, Object>> convertDaysToList(List<Day> days) {
        List<Map<String, Object>> daysList = new ArrayList<>();
        for (Day day : days) {
            Map<String, Object> dayMap = new HashMap<>();
            dayMap.put("dayIndex", day.getDayIndex());
            
            if (day.getActivities() != null) {
                List<Map<String, Object>> activitiesList = new ArrayList<>();
                for (Activity activity : day.getActivities()) {
                    activitiesList.add(convertActivityToMap(activity));
                }
                dayMap.put("activities", activitiesList);
            }
            
            daysList.add(dayMap);
        }
        return daysList;
    }

    private Map<String, Object> convertActivityToMap(Activity activity) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", activity.getId());
        map.put("dayIndex", activity.getDayIndex());
        map.put("type", activity.getType());
        map.put("title", activity.getTitle());
        map.put("locationName", activity.getLocationName());
        map.put("poi", activity.getPoi());
        map.put("startTime", activity.getStartTime());
        map.put("endTime", activity.getEndTime());
        map.put("estimatedCost", activity.getEstimatedCost());
        return map;
    }

    @SuppressWarnings("unchecked")
    private Trip convertDocumentToTrip(DocumentSnapshot doc) {
        Map<String, Object> headcountMap = (Map<String, Object>) doc.get("headcount");
        Trip.Headcount headcount = null;
        if (headcountMap != null) {
            headcount = Trip.Headcount.builder()
                    .adults(((Long) headcountMap.get("adults")).intValue())
                    .children(((Long) headcountMap.get("children")).intValue())
                    .build();
        }

        // 解析 days 数组
        List<Day> days = null;
        List<Map<String, Object>> daysList = (List<Map<String, Object>>) doc.get("days");
        if (daysList != null) {
            days = new ArrayList<>();
            for (Map<String, Object> dayMap : daysList) {
                Day day = convertMapToDay(dayMap);
                days.add(day);
            }
        }

        // 处理可能为 null 的日期字段
        Instant createdAt = null;
        Date createdAtDate = doc.getDate("createdAt");
        if (createdAtDate != null) {
            createdAt = createdAtDate.toInstant();
        }

        Instant updatedAt = null;
        Date updatedAtDate = doc.getDate("updatedAt");
        if (updatedAtDate != null) {
            updatedAt = updatedAtDate.toInstant();
        }

        return Trip.builder()
                .id(doc.getString("id"))
                .userId(doc.getString("userId"))
                .title(doc.getString("title"))
                .destination(doc.getString("destination"))
                .startDate(LocalDate.parse(doc.getString("startDate")))
                .endDate(LocalDate.parse(doc.getString("endDate")))
                .totalBudget(doc.getLong("totalBudget"))
                .headcount(headcount)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .days(days)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Day convertMapToDay(Map<String, Object> dayMap) {
        Integer dayIndex = ((Long) dayMap.get("dayIndex")).intValue();
        
        List<Activity> activities = null;
        List<Map<String, Object>> activitiesList = (List<Map<String, Object>>) dayMap.get("activities");
        if (activitiesList != null) {
            activities = new ArrayList<>();
            for (Map<String, Object> activityMap : activitiesList) {
                activities.add(convertMapToActivity(activityMap));
            }
        }
        
        return Day.builder()
                .dayIndex(dayIndex)
                .activities(activities)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Activity convertMapToActivity(Map<String, Object> map) {
        // 解析 POI 对象
        AmapPoi poi = null;
        Object poiObj = map.get("poi");
        if (poiObj instanceof Map) {
            Map<String, Object> poiMap = (Map<String, Object>) poiObj;
            poi = AmapPoi.builder()
                    .id((String) poiMap.get("id"))
                    .name((String) poiMap.get("name"))
                    .type((String) poiMap.get("type"))
                    .typecode((String) poiMap.get("typecode"))
                    .address((String) poiMap.get("address"))
                    .location((String) poiMap.get("location"))
                    .pname((String) poiMap.get("pname"))
                    .cityname((String) poiMap.get("cityname"))
                    .adname((String) poiMap.get("adname"))
                    .pcode((String) poiMap.get("pcode"))
                    .citycode((String) poiMap.get("citycode"))
                    .adcode((String) poiMap.get("adcode"))
                    .tel((String) poiMap.get("tel"))
                    .build();
        }
        
        return Activity.builder()
                .id((String) map.get("id"))
                .dayIndex(((Long) map.get("dayIndex")).intValue())
                .type((String) map.get("type"))
                .title((String) map.get("title"))
                .locationName((String) map.get("locationName"))
                .poi(poi)
                .startTime((String) map.get("startTime"))
                .endTime((String) map.get("endTime"))
                .estimatedCost((Long) map.get("estimatedCost"))
                .build();
    }
}
