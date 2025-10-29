package com.tinglans.backend.repository;

import com.google.cloud.firestore.Firestore;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TripRepository Firestore 持久化测试
 */
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TripRepositoryFirestoreTest {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private Firestore firestore;

    private static final String TEST_TRIP_ID = "test-trip-firestore-001";
    private static final String TEST_USER_ID = "test-user-001";

    private Trip createTestTrip(String tripId, String title) {
        Activity activity1 = Activity.builder()
                .id("activity-001")
                .dayIndex(1)
                .type("sight")
                .title("参观伏见稻荷大社")
                .locationName("伏见稻荷大社")
                .address("京都市伏见区深草藪之内町68")
                .lat(34.9671)
                .lng(135.7727)
                .poiId("B000A7BD6C")
                .startTime("09:00")
                .endTime("11:00")
                .estimatedCost(0L)
                .build();

        Activity activity2 = Activity.builder()
                .id("activity-002")
                .dayIndex(1)
                .type("food")
                .title("一兰拉面午餐")
                .locationName("一兰拉面 京都河原町店")
                .address("京都市下京区河原町通四条下ル市之町251-2")
                .lat(34.9856)
                .lng(135.7580)
                .poiId("B000A8HHGF")
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
    @DisplayName("1. 插入一个 Trip 到 Firestore")
    void test1_InsertTrip() throws ExecutionException, InterruptedException {
        log.info("=== 测试1: 插入 Trip ===");
        
        Trip trip = createTestTrip(TEST_TRIP_ID, "日本京都3日游");
        trip.setConfirmed(true);
        
        tripRepository.saveToFirestore(trip);
        
        log.info("✅ 已插入 Trip: id={}, title={}", TEST_TRIP_ID, trip.getTitle());
    }

    @Test
    @Order(2)
    @DisplayName("2. 从 Firestore 获取指定 ID 的 Trip 并验证字段")
    void test2_GetTripAndVerifyField() throws ExecutionException, InterruptedException {
        log.info("=== 测试2: 获取 Trip 并验证字段 ===");
        
        Optional<Trip> result = tripRepository.getFromFirestore(TEST_TRIP_ID);
        
        assertTrue(result.isPresent(), "应该能从 Firestore 获取到 Trip");
        
        Trip trip = result.get();
        assertEquals(TEST_TRIP_ID, trip.getId(), "Trip ID 应该匹配");
        assertEquals("日本京都3日游", trip.getTitle(), "Trip title 应该是 '日本京都3日游'");
        assertTrue(trip.getConfirmed(), "Trip confirmed 应该是 true");
        assertEquals("日本 京都", trip.getDestination(), "Trip destination 应该匹配");
        
        log.info("✅ 验证成功: title={}, destination={}, confirmed={}", 
                trip.getTitle(), trip.getDestination(), trip.getConfirmed());
        log.info("✅ Firestore 数据库验证通过！数据已成功持久化并可正确读取");
    }

    @AfterAll
    static void cleanup(@Autowired Firestore firestore) throws ExecutionException, InterruptedException {
        log.info("=== 清理测试数据 ===");
        
        try {
            firestore.collection("trips").document(TEST_TRIP_ID).delete().get();
            log.info("✅ 已清理 Firestore 测试数据: {}", TEST_TRIP_ID);
        } catch (Exception e) {
            log.warn("⚠️ 清理 Firestore 数据失败: {}", e.getMessage());
        }
        
        log.info("✅ 清理完成");
    }
}
