package com.tinglans.backend.service;

import com.tinglans.backend.common.BusinessException;
import com.tinglans.backend.domain.Activity;
import com.tinglans.backend.domain.Day;
import com.tinglans.backend.domain.Trip;
import com.tinglans.backend.repository.TripRepository;
import com.tinglans.backend.thirdparty.llm.QwenClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TripService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private UserService userService;

    @Mock
    private QwenClient qwenClient;

    @InjectMocks
    private TripService tripService;

    private String testUserId;
    private String testTripId;
    private Trip testTrip;

    @BeforeEach
    void setUp() {
        testUserId = "user-123";
        testTripId = "trip-456";

        testTrip = Trip.builder()
                .id(testTripId)
                .userId(testUserId)
                .title("东京三日游")
                .destination("东京")
                .startDate(LocalDate.of(2024, 11, 1))
                .endDate(LocalDate.of(2024, 11, 3))
                .days(new ArrayList<>())
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void testCreateTripFromText_success() throws ExecutionException, InterruptedException {
        // Given
        String userInput = "我想去东京玩三天，11月1号出发";
        List<String> preferences = Arrays.asList("美食", "动漫");

        String llmResponse = """
                {
                  "tripName": "东京三日游",
                  "destination": "东京",
                  "startDate": "2024-11-01",
                  "endDate": "2024-11-03",
                  "days": [
                    {
                      "dayIndex": 1,
                      "activities": [
                        {
                          "type": "transport",
                          "title": "乘坐地铁",
                          "locationName": "东京站",
                          "startTime": "09:00",
                          "endTime": "10:00",
                          "estimatedCost": 500
                        },
                        {
                          "type": "food",
                          "title": "午餐拉面",
                          "locationName": "一兰拉面",
                          "startTime": "12:00",
                          "endTime": "13:00",
                          "estimatedCost": 3000
                        }
                      ]
                    }
                  ]
                }
                """;

        when(userService.getPreferencesList(testUserId)).thenReturn(preferences);
        when(qwenClient.chat(anyString(), anyString())).thenReturn(llmResponse);
        doNothing().when(tripRepository).saveToCache(any(Trip.class));

        // When
        Trip result = tripService.createTripFromText(userInput, testUserId);

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(testUserId, result.getUserId());
        assertEquals("东京三日游", result.getTitle());
        assertEquals("东京", result.getDestination());
        assertEquals(LocalDate.of(2024, 11, 1), result.getStartDate());
        assertEquals(LocalDate.of(2024, 11, 3), result.getEndDate());
        assertEquals(1, result.getDays().size());
        assertEquals(2, result.getDays().get(0).getActivities().size());

        // 验证第一个活动
        Activity activity1 = result.getDays().get(0).getActivities().get(0);
        assertEquals("transport", activity1.getType());
        assertEquals("乘坐地铁", activity1.getTitle());
        assertEquals("东京站", activity1.getLocationName());
        assertEquals("09:00", activity1.getStartTime());
        assertEquals("10:00", activity1.getEndTime());
        assertEquals(500L, activity1.getEstimatedCost());

        verify(userService, times(1)).getPreferencesList(testUserId);
        verify(qwenClient, times(1)).chat(anyString(), anyString());
        verify(tripRepository, times(1)).saveToCache(any(Trip.class));
    }

    @Test
    void testCreateTripFromText_withoutUserPreferences() throws ExecutionException, InterruptedException {
        // Given
        String userInput = "我想去京都";
        when(userService.getPreferencesList(testUserId)).thenReturn(new ArrayList<>());

        String llmResponse = """
                {
                  "tripName": "京都一日游",
                  "destination": "京都",
                  "startDate": "2024-11-05",
                  "endDate": "2024-11-05",
                  "days": []
                }
                """;

        when(qwenClient.chat(anyString(), eq(userInput))).thenReturn(llmResponse);
        doNothing().when(tripRepository).saveToCache(any(Trip.class));

        // When
        Trip result = tripService.createTripFromText(userInput, testUserId);

        // Then
        assertNotNull(result);
        assertEquals("京都一日游", result.getTitle());
        assertEquals("京都", result.getDestination());

        // 验证传递给LLM的消息就是原始输入（没有偏好）
        verify(qwenClient, times(1)).chat(anyString(), eq(userInput));
    }

    @Test
    void testCreateTripFromText_withUserPreferences() throws ExecutionException, InterruptedException {
        // Given
        String userInput = "我想去大阪";
        List<String> preferences = Arrays.asList("美食", "购物");

        String llmResponse = """
                {
                  "tripName": "大阪美食购物游",
                  "destination": "大阪",
                  "startDate": "2024-12-01",
                  "endDate": "2024-12-02",
                  "days": []
                }
                """;

        when(userService.getPreferencesList(testUserId)).thenReturn(preferences);
        when(qwenClient.chat(anyString(), anyString())).thenReturn(llmResponse);
        doNothing().when(tripRepository).saveToCache(any(Trip.class));

        // When
        Trip result = tripService.createTripFromText(userInput, testUserId);

        // Then
        assertNotNull(result);
        
        // 验证传递给LLM的消息包含用户偏好
        verify(qwenClient, times(1)).chat(anyString(), contains("我的旅行偏好"));
    }

    @Test
    void testCreateTripFromText_invalidJson() throws ExecutionException, InterruptedException {
        // Given
        String userInput = "去北京";
        when(userService.getPreferencesList(testUserId)).thenReturn(new ArrayList<>());
        when(qwenClient.chat(anyString(), anyString())).thenReturn("{ invalid json");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            tripService.createTripFromText(userInput, testUserId);
        });
        assertEquals("Invalid JSON format for trip data", exception.getMessage());

        verify(tripRepository, never()).saveToCache(any(Trip.class));
    }

    @Test
    void testCreateTripFromText_llmThrowsException() throws ExecutionException, InterruptedException {
        // Given
        String userInput = "去上海";
        when(userService.getPreferencesList(testUserId)).thenReturn(new ArrayList<>());
        when(qwenClient.chat(anyString(), anyString()))
                .thenThrow(new RuntimeException("LLM service error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            tripService.createTripFromText(userInput, testUserId);
        });

        verify(tripRepository, never()).saveToCache(any(Trip.class));
    }

    @Test
    void testCreateTripFromText_multipleDaysWithActivities() throws ExecutionException, InterruptedException {
        // Given
        String userInput = "三天东京游";
        String llmResponse = """
                {
                  "tripName": "东京三日游",
                  "destination": "东京",
                  "startDate": "2024-11-01",
                  "endDate": "2024-11-03",
                  "days": [
                    {
                      "dayIndex": 1,
                      "activities": [
                        {
                          "type": "hotel",
                          "title": "入住酒店",
                          "locationName": "东京希尔顿酒店",
                          "startTime": "15:00",
                          "endTime": "16:00",
                          "estimatedCost": 50000
                        }
                      ]
                    },
                    {
                      "dayIndex": 2,
                      "activities": [
                        {
                          "type": "sight",
                          "title": "参观浅草寺",
                          "locationName": "浅草寺",
                          "startTime": "10:00",
                          "endTime": "12:00",
                          "estimatedCost": 0
                        }
                      ]
                    },
                    {
                      "dayIndex": 3,
                      "activities": []
                    }
                  ]
                }
                """;

        when(userService.getPreferencesList(testUserId)).thenReturn(new ArrayList<>());
        when(qwenClient.chat(anyString(), anyString())).thenReturn(llmResponse);
        doNothing().when(tripRepository).saveToCache(any(Trip.class));

        // When
        Trip result = tripService.createTripFromText(userInput, testUserId);

        // Then
        assertEquals(3, result.getDays().size());
        assertEquals(1, result.getDays().get(0).getActivities().size());
        assertEquals(1, result.getDays().get(1).getActivities().size());
        assertEquals(0, result.getDays().get(2).getActivities().size());
        
        // 验证日期计算
        assertEquals(LocalDate.of(2024, 11, 1), result.getDays().get(0).getDate());
        assertEquals(LocalDate.of(2024, 11, 2), result.getDays().get(1).getDate());
        assertEquals(LocalDate.of(2024, 11, 3), result.getDays().get(2).getDate());
    }

    @Test
    void testGetTripById_fromCache() throws ExecutionException, InterruptedException {
        // Given
        when(tripRepository.getFromCache(testTripId)).thenReturn(Optional.of(testTrip));

        // When
        Optional<Trip> result = tripService.getTripById(testTripId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testTripId, result.get().getId());
        assertEquals(testUserId, result.get().getUserId());

        verify(tripRepository, times(1)).getFromCache(testTripId);
        verify(tripRepository, never()).getFromFirestore(anyString());
    }

    @Test
    void testGetTripById_fromFirestore() throws ExecutionException, InterruptedException {
        // Given
        when(tripRepository.getFromCache(testTripId)).thenReturn(Optional.empty());
        when(tripRepository.getFromFirestore(testTripId)).thenReturn(Optional.of(testTrip));
        doNothing().when(tripRepository).saveToCache(testTrip);

        // When
        Optional<Trip> result = tripService.getTripById(testTripId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testTripId, result.get().getId());

        verify(tripRepository, times(1)).getFromCache(testTripId);
        verify(tripRepository, times(1)).getFromFirestore(testTripId);
        verify(tripRepository, times(1)).saveToCache(testTrip);  // 验证回写缓存
    }

    @Test
    void testGetTripById_notFound() throws ExecutionException, InterruptedException {
        // Given
        when(tripRepository.getFromCache(testTripId)).thenReturn(Optional.empty());
        when(tripRepository.getFromFirestore(testTripId)).thenReturn(Optional.empty());

        // When
        Optional<Trip> result = tripService.getTripById(testTripId);

        // Then
        assertFalse(result.isPresent());

        verify(tripRepository, times(1)).getFromCache(testTripId);
        verify(tripRepository, times(1)).getFromFirestore(testTripId);
        verify(tripRepository, never()).saveToCache(any(Trip.class));
    }

    @Test
    void testConfirmTrip_success() throws ExecutionException, InterruptedException {
        // Given
        when(tripRepository.getFromCache(testTripId)).thenReturn(Optional.of(testTrip));
        doNothing().when(tripRepository).saveToFirestore(testTrip);
        doNothing().when(tripRepository).saveToCache(testTrip);

        // When
        Trip result = tripService.confirmTrip(testTripId, testUserId);

        // Then
        assertNotNull(result);
        assertEquals(testTripId, result.getId());
        assertEquals(testUserId, result.getUserId());

        verify(tripRepository, times(1)).getFromCache(testTripId);
        verify(tripRepository, times(1)).saveToFirestore(testTrip);
        verify(tripRepository, times(1)).saveToCache(testTrip);
    }

    @Test
    void testConfirmTrip_tripNotFound() throws ExecutionException, InterruptedException {
        // Given
        when(tripRepository.getFromCache(testTripId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            tripService.confirmTrip(testTripId, testUserId);
        });

        assertTrue(exception.getMessage().contains("行程") && exception.getMessage().contains("过期"));

        verify(tripRepository, times(1)).getFromCache(testTripId);
        verify(tripRepository, never()).saveToFirestore(any(Trip.class));
    }

    @Test
    void testConfirmTrip_unauthorizedUser() throws ExecutionException, InterruptedException {
        // Given
        String differentUserId = "user-999";
        when(tripRepository.getFromCache(testTripId)).thenReturn(Optional.of(testTrip));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            tripService.confirmTrip(testTripId, differentUserId);
        });

        assertTrue(exception.getMessage().contains("无权"));

        verify(tripRepository, times(1)).getFromCache(testTripId);
        verify(tripRepository, never()).saveToFirestore(any(Trip.class));
    }

    @Test
    void testConfirmTrip_firestoreSaveThrowsException() throws ExecutionException, InterruptedException {
        // Given
        when(tripRepository.getFromCache(testTripId)).thenReturn(Optional.of(testTrip));
        doThrow(new RuntimeException("Firestore error"))
                .when(tripRepository).saveToFirestore(testTrip);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            tripService.confirmTrip(testTripId, testUserId);
        });

        verify(tripRepository, never()).saveToCache(any(Trip.class));
    }

    @Test
    void testGetConfirmedTripsByUserId_success() throws ExecutionException, InterruptedException {
        // Given
        List<Trip> confirmedTrips = Arrays.asList(
                Trip.builder().id("trip-1").userId(testUserId).title("行程1").build(),
                Trip.builder().id("trip-2").userId(testUserId).title("行程2").build(),
                Trip.builder().id("trip-3").userId(testUserId).title("行程3").build()
        );

        when(tripRepository.findConfirmedTripsByUserId(testUserId)).thenReturn(confirmedTrips);

        // When
        List<Trip> result = tripService.getConfirmedTripsByUserId(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("trip-1", result.get(0).getId());
        assertEquals("trip-2", result.get(1).getId());
        assertEquals("trip-3", result.get(2).getId());

        verify(tripRepository, times(1)).findConfirmedTripsByUserId(testUserId);
    }

    @Test
    void testGetConfirmedTripsByUserId_emptyList() throws ExecutionException, InterruptedException {
        // Given
        when(tripRepository.findConfirmedTripsByUserId(testUserId)).thenReturn(new ArrayList<>());

        // When
        List<Trip> result = tripService.getConfirmedTripsByUserId(testUserId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetConfirmedTripsByUserId_repositoryThrowsException() throws ExecutionException, InterruptedException {
        // Given
        when(tripRepository.findConfirmedTripsByUserId(testUserId))
                .thenThrow(new ExecutionException(new RuntimeException("Database error")));

        // When & Then
        assertThrows(ExecutionException.class, () -> {
            tripService.getConfirmedTripsByUserId(testUserId);
        });
    }

    @Test
    void testParseLlmResponse_withMissingOptionalFields() throws ExecutionException, InterruptedException {
        // Given - JSON中缺少某些可选字段
        String userInput = "简单行程";
        String minimalResponse = """
                {
                  "days": []
                }
                """;

        when(userService.getPreferencesList(testUserId)).thenReturn(new ArrayList<>());
        when(qwenClient.chat(anyString(), anyString())).thenReturn(minimalResponse);
        doNothing().when(tripRepository).saveToCache(any(Trip.class));

        // When
        Trip result = tripService.createTripFromText(userInput, testUserId);

        // Then
        assertNotNull(result);
        assertEquals("未命名行程", result.getTitle());  // 默认值
        assertEquals("", result.getDestination());
        assertNull(result.getStartDate());
        assertNull(result.getEndDate());
        assertEquals(0, result.getDays().size());
    }

    @Test
    void testParseLlmResponse_withAllActivityTypes() throws ExecutionException, InterruptedException {
        // Given - 测试所有活动类型
        String userInput = "完整行程";
        String llmResponse = """
                {
                  "tripName": "完整测试",
                  "destination": "测试地",
                  "startDate": "2024-12-01",
                  "endDate": "2024-12-01",
                  "days": [
                    {
                      "dayIndex": 1,
                      "activities": [
                        {"type": "transport", "title": "交通", "locationName": "车站", "startTime": "09:00", "endTime": "10:00", "estimatedCost": 1000},
                        {"type": "hotel", "title": "住宿", "locationName": "酒店", "startTime": "15:00", "endTime": "16:00", "estimatedCost": 20000},
                        {"type": "sight", "title": "景点", "locationName": "景区", "startTime": "10:00", "endTime": "12:00", "estimatedCost": 5000},
                        {"type": "food", "title": "餐饮", "locationName": "餐厅", "startTime": "12:00", "endTime": "13:00", "estimatedCost": 3000},
                        {"type": "other", "title": "其他", "locationName": "商店", "startTime": "14:00", "endTime": "15:00", "estimatedCost": 2000}
                      ]
                    }
                  ]
                }
                """;

        when(userService.getPreferencesList(testUserId)).thenReturn(new ArrayList<>());
        when(qwenClient.chat(anyString(), anyString())).thenReturn(llmResponse);
        doNothing().when(tripRepository).saveToCache(any(Trip.class));

        // When
        Trip result = tripService.createTripFromText(userInput, testUserId);

        // Then
        assertEquals(1, result.getDays().size());
        assertEquals(5, result.getDays().get(0).getActivities().size());
        
        List<Activity> activities = result.getDays().get(0).getActivities();
        assertEquals("transport", activities.get(0).getType());
        assertEquals("hotel", activities.get(1).getType());
        assertEquals("sight", activities.get(2).getType());
        assertEquals("food", activities.get(3).getType());
        assertEquals("other", activities.get(4).getType());
    }
}
