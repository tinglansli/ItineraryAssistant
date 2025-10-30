package com.tinglans.backend.repository;

import com.tinglans.backend.domain.Activity;
import com.tinglans.backend.domain.Day;
import com.tinglans.backend.domain.Trip;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TripRepository Redis 缓存测试
 */
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TripRepositoryRedisTest {

    @Autowired
    private TripRepository tripRepository;

    private static final String TEST_TRIP_ID_1 = "test-redis-trip-001";
    private static final String TEST_TRIP_ID_2 = "test-redis-trip-002";
    private static final String TEST_TRIP_ID_3 = "test-redis-trip-003";
    private static final String TEST_USER_ID = "test-redis-user-001";

    private Trip createTestTrip(String tripId, String title) {
        Activity activity1 = Activity.builder()
                .type("sight")
                .title("参观伏见稻荷大社")
                .locationName("伏见稻荷大社")
                .startTime("09:00")
                .endTime("11:00")
                .estimatedCost(0L)
                .build();

        Activity activity2 = Activity.builder()
                .type("food")
                .title("一兰拉面午餐")
                .locationName("一兰拉面 京都河原町店")
                .startTime("12:00")
                .endTime("13:00")
                .estimatedCost(8000L)
                .build();

        Day day1 = Day.builder()
                .dayIndex(1)
                .date(LocalDate.of(2025, 11, 1))
                .activities(Arrays.asList(activity1, activity2))
                .build();

        return Trip.builder()
                .id(tripId)
                .userId(TEST_USER_ID)
                .title(title)
                .destination("日本 京都")
                .startDate(LocalDate.of(2025, 11, 1))
                .endDate(LocalDate.of(2025, 11, 3))
                .days(Arrays.asList(day1))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @Order(1)
    @DisplayName("1. 保存行程到 Redis 缓存")
    void testSaveTripToCache() {
        log.info("=== 测试: 保存行程到 Redis 缓存 ===");
        
        Trip trip = createTestTrip(TEST_TRIP_ID_1, "京都三日游");
        tripRepository.saveToCache(trip);
        
        log.info("✅ 已保存行程到 Redis: id={}, title={}", TEST_TRIP_ID_1, trip.getTitle());
    }

    @Test
    @Order(2)
    @DisplayName("2. 从 Redis 缓存获取行程")
    void testGetTripFromCache() {
        log.info("=== 测试: 从 Redis 缓存获取行程 ===");
        
        // 先保存
        Trip trip = createTestTrip(TEST_TRIP_ID_2, "大阪五日游");
        tripRepository.saveToCache(trip);
        
        // 再查询
        Optional<Trip> cached = tripRepository.getFromCache(TEST_TRIP_ID_2);
        
        assertTrue(cached.isPresent(), "应该能从 Redis 获取到行程");
        assertEquals(TEST_TRIP_ID_2, cached.get().getId());
        assertEquals("大阪五日游", cached.get().getTitle());
        assertEquals(TEST_USER_ID, cached.get().getUserId());
        assertEquals("日本 京都", cached.get().getDestination());
        assertEquals(1, cached.get().getDays().size());
        assertEquals(2, cached.get().getDays().get(0).getActivities().size());
        
        log.info("✅ Redis 查询成功: {}", cached.get().getTitle());
    }

    @Test
    @Order(3)
    @DisplayName("3. 查询不存在的行程")
    void testGetNonExistentTrip() {
        log.info("=== 测试: 查询不存在的行程 ===");
        
        Optional<Trip> result = tripRepository.getFromCache("non-existent-id");
        
        assertFalse(result.isPresent(), "不存在的行程应该返回 empty");
        log.info("✅ 不存在的行程正确返回 empty");
    }

    @Test
    @Order(4)
    @DisplayName("4. 更新缓存中的行程")
    void testUpdateCachedTrip() {
        log.info("=== 测试: 更新缓存中的行程 ===");
        
        // 保存初始版本
        Trip trip = createTestTrip(TEST_TRIP_ID_3, "东京七日游");
        tripRepository.saveToCache(trip);
        
        // 修改并重新保存
        trip.setTitle("东京七日游（已修改）");
        trip.setDestination("日本 东京");
        tripRepository.saveToCache(trip);
        
        // 验证更新
        Optional<Trip> updated = tripRepository.getFromCache(TEST_TRIP_ID_3);
        
        assertTrue(updated.isPresent());
        assertEquals("东京七日游（已修改）", updated.get().getTitle());
        assertEquals("日本 东京", updated.get().getDestination());
        
        log.info("✅ Redis 更新成功: {}", updated.get().getTitle());
    }

    @Test
    @Order(5)
    @DisplayName("5. 删除 Redis 缓存中的行程")
    void testDeleteTripFromCache() {
        log.info("=== 测试: 删除 Redis 缓存中的行程 ===");
        
        // 保存一个行程
        Trip trip = createTestTrip("test-redis-trip-delete", "待删除的行程");
        tripRepository.saveToCache(trip);
        
        // 验证存在
        Optional<Trip> beforeDelete = tripRepository.getFromCache("test-redis-trip-delete");
        assertTrue(beforeDelete.isPresent(), "删除前应该存在");
        log.info("✅ 删除前验证通过: 行程存在");
        
        // 删除
        tripRepository.deleteFromCache("test-redis-trip-delete");
        
        // 验证已删除
        Optional<Trip> afterDelete = tripRepository.getFromCache("test-redis-trip-delete");
        assertFalse(afterDelete.isPresent(), "删除后应该不存在");
        
        log.info("✅ Redis 删除成功: 行程已被移除");
    }

    @AfterEach
    void cleanupAfterEach() {
        // 每个测试后清理测试数据
        try {
            tripRepository.deleteFromCache(TEST_TRIP_ID_1);
            tripRepository.deleteFromCache(TEST_TRIP_ID_2);
            tripRepository.deleteFromCache(TEST_TRIP_ID_3);
            tripRepository.deleteFromCache("test-redis-trip-delete");
            log.debug("测试数据清理完成");
        } catch (Exception e) {
            log.warn("清理测试数据时出错: {}", e.getMessage());
        }
    }

    @AfterAll
    static void cleanup() {
        log.info("=== Redis 缓存测试完成 ===");
        log.info("✅ 所有测试数据已自动清理");
    }
}
