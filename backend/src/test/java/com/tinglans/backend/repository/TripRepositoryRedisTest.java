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

    private static final String TEST_TRIP_ID_1 = "test-trip-001";
    private static final String TEST_TRIP_ID_2 = "test-trip-002";
    private static final String TEST_TRIP_ID_3 = "test-trip-003";
    private static final String TEST_USER_ID = "test-user-001";

    private Trip createTestTrip(String tripId, String title) {
        Activity activity1 = Activity.builder()
                .id("activity-001")
                .dayIndex(1)
                .type("sight")
                .title("伏见稻荷大社")
                .address("京都市伏见区")
                .lat(34.9671)
                .lng(135.7727)
                .startTime("09:00")
                .endTime("11:00")
                .estimatedCost(0L)
                .build();

        Activity activity2 = Activity.builder()
                .id("activity-002")
                .dayIndex(1)
                .type("food")
                .title("一兰拉面")
                .address("京都市下京区")
                .lat(34.9856)
                .lng(135.7580)
                .startTime("12:00")
                .endTime("13:00")
                .estimatedCost(8000L)
                .build();

        Day day1 = Day.builder()
                .dayIndex(1)
                .activities(Arrays.asList(activity1, activity2))
                .build();

        return Trip.builder()
                .id(tripId)
                .userId(TEST_USER_ID)
                .title(title)
                .destination("日本 京都")
                .startDate(LocalDate.of(2025, 11, 1))
                .endDate(LocalDate.of(2025, 11, 3))
                .currency("CNY")
                .totalBudget(1000000L)
                .headcount(Trip.Headcount.builder()
                        .adults(2)
                        .children(1)
                        .build())
                .preferences(Arrays.asList("美食", "文化", "亲子"))
                .days(Arrays.asList(day1))
                .confirmed(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @Order(1)
    @DisplayName("1. 保存多个行程到 Redis 并验证")
    void testSaveMultipleTripsToCache() {
        log.info("=== 测试: 保存多个行程到 Redis ===");
        
        // 创建并保存 3 个行程
        Trip trip1 = createTestTrip(TEST_TRIP_ID_1, "日本京都3日游");
        Trip trip2 = createTestTrip(TEST_TRIP_ID_2, "日本大阪5日游");
        Trip trip3 = createTestTrip(TEST_TRIP_ID_3, "日本东京7日游");
        
        tripRepository.saveToCache(trip1);
        tripRepository.saveToCache(trip2);
        tripRepository.saveToCache(trip3);
        
        log.info("✅ 已保存 3 个行程到 Redis:");
        log.info("   - {}: {}", TEST_TRIP_ID_1, trip1.getTitle());
        log.info("   - {}: {}", TEST_TRIP_ID_2, trip2.getTitle());
        log.info("   - {}: {}", TEST_TRIP_ID_3, trip3.getTitle());
        log.info("");
        log.info("📌 请手动检查 Redis:");
        log.info("   docker exec redis-dev redis-cli KEYS \"trip:*\"");
        log.info("   docker exec redis-dev redis-cli GET \"trip:test-trip-001\"");
        log.info("");
        log.info("📌 数据将在 30 分钟 (1800秒) 后自动过期删除");
        
        // 验证可以读取
        Optional<Trip> cached1 = tripRepository.getFromCache(TEST_TRIP_ID_1);
        Optional<Trip> cached2 = tripRepository.getFromCache(TEST_TRIP_ID_2);
        Optional<Trip> cached3 = tripRepository.getFromCache(TEST_TRIP_ID_3);
        
        assertTrue(cached1.isPresent(), "应该能从 Redis 获取到行程1");
        assertTrue(cached2.isPresent(), "应该能从 Redis 获取到行程2");
        assertTrue(cached3.isPresent(), "应该能从 Redis 获取到行程3");
        
        assertEquals("日本京都3日游", cached1.get().getTitle());
        assertEquals("日本大阪5日游", cached2.get().getTitle());
        assertEquals("日本东京7日游", cached3.get().getTitle());
        
        log.info("✅ Redis 读取验证通过 - 所有数据完整");
    }

    @AfterAll
    static void cleanup() {
        log.info("=== 测试完成 ===");
        log.info("📌 Redis 中的数据将在 30 分钟后自动过期");
        log.info("📌 如需手动清理: docker exec redis-dev redis-cli FLUSHDB");
    }
}
