package com.tinglans.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinglans.backend.common.BusinessException;
import com.tinglans.backend.common.ResponseCode;
import com.tinglans.backend.domain.Day;
import com.tinglans.backend.domain.Trip;
import com.tinglans.backend.service.TripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TripController 测试类
 */
@WebMvcTest(TripController.class)
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TripService tripService;

    private Trip testTrip;
    private String testUserId;
    private String testTripId;

    @BeforeEach
    void setUp() {
        testUserId = "user-123";
        testTripId = "trip-123";

        // 创建测试用的 Trip 对象
        Day day = Day.builder()
                .date(LocalDate.now())
                .activities(new ArrayList<>())
                .build();

        testTrip = Trip.builder()
                .id(testTripId)
                .userId(testUserId)
                .title("北京三日游")
                .destination("北京")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(2))
                .days(Arrays.asList(day))
                .totalBudget(100000L)
                .build();
    }

    @Test
    void testCreateTripFromText_success() throws Exception {
        // Given
        TripController.CreateTripRequest request = new TripController.CreateTripRequest();
        request.setUserInput("去北京玩三天");
        request.setUserId(testUserId);

        when(tripService.createTripFromText(eq("去北京玩三天"), eq(testUserId)))
                .thenReturn(testTrip);

        // When & Then
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("行程生成成功"))
                .andExpect(jsonPath("$.data.id").value(testTripId))
                .andExpect(jsonPath("$.data.title").value("北京三日游"))
                .andExpect(jsonPath("$.data.destination").value("北京"));

        verify(tripService, times(1)).createTripFromText(eq("去北京玩三天"), eq(testUserId));
    }

    @Test
    void testCreateTripFromText_invalidInput() throws Exception {
        // Given
        TripController.CreateTripRequest request = new TripController.CreateTripRequest();
        request.setUserInput("");
        request.setUserId(testUserId);

        when(tripService.createTripFromText(eq(""), eq(testUserId)))
                .thenThrow(new BusinessException(ResponseCode.INVALID_PARAM, "用户输入不能为空"));

        // When & Then
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(tripService, times(1)).createTripFromText(eq(""), eq(testUserId));
    }

    @Test
    void testGetTripItinerary_success() throws Exception {
        // Given
        when(tripService.validateAndGetTrip(testTripId)).thenReturn(testTrip);

        // When & Then
        mockMvc.perform(get("/api/trips/{tripId}/itinerary", testTripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(testTripId))
                .andExpect(jsonPath("$.data.title").value("北京三日游"));

        verify(tripService, times(1)).validateAndGetTrip(testTripId);
    }

    @Test
    void testGetTripItinerary_tripNotFound() throws Exception {
        // Given
        when(tripService.validateAndGetTrip("non-existent-trip"))
                .thenThrow(new BusinessException(ResponseCode.TRIP_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/trips/{tripId}/itinerary", "non-existent-trip"))
                .andExpect(status().isNotFound());

        verify(tripService, times(1)).validateAndGetTrip("non-existent-trip");
    }

    @Test
    void testConfirmTrip_success() throws Exception {
        // Given
        TripController.ConfirmTripRequest request = new TripController.ConfirmTripRequest();
        request.setUserId(testUserId);

        when(tripService.confirmTrip(testTripId, testUserId)).thenReturn(testTrip);

        // When & Then
        mockMvc.perform(post("/api/trips/{tripId}/confirm", testTripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("行程确认成功"))
                .andExpect(jsonPath("$.data.id").value(testTripId));

        verify(tripService, times(1)).confirmTrip(testTripId, testUserId);
    }

    @Test
    void testConfirmTrip_permissionDenied() throws Exception {
        // Given
        TripController.ConfirmTripRequest request = new TripController.ConfirmTripRequest();
        request.setUserId("other-user");

        when(tripService.confirmTrip(testTripId, "other-user"))
                .thenThrow(new BusinessException(ResponseCode.PERMISSION_DENIED));

        // When & Then
        mockMvc.perform(post("/api/trips/{tripId}/confirm", testTripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(tripService, times(1)).confirmTrip(testTripId, "other-user");
    }

    @Test
    void testGetConfirmedTrips_success() throws Exception {
        // Given
        Trip trip2 = Trip.builder()
                .id("trip-456")
                .userId(testUserId)
                .title("上海两日游")
                .destination("上海")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .days(new ArrayList<>())
                .totalBudget(80000L)
                .build();

        List<Trip> trips = Arrays.asList(testTrip, trip2);
        when(tripService.getConfirmedTripsByUserId(testUserId)).thenReturn(trips);
        doNothing().when(tripService).validateUserId(testUserId);

        // When & Then
        mockMvc.perform(get("/api/trips")
                        .param("userId", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(testTripId))
                .andExpect(jsonPath("$.data[1].id").value("trip-456"));

        verify(tripService, times(1)).validateUserId(testUserId);
        verify(tripService, times(1)).getConfirmedTripsByUserId(testUserId);
    }

    @Test
    void testGetConfirmedTrips_userNotFound() throws Exception {
        // Given
        doThrow(new BusinessException(ResponseCode.USER_NOT_FOUND))
                .when(tripService).validateUserId("non-existent-user");

        // When & Then
        mockMvc.perform(get("/api/trips")
                        .param("userId", "non-existent-user"))
                .andExpect(status().isNotFound());

        verify(tripService, times(1)).validateUserId("non-existent-user");
        verify(tripService, never()).getConfirmedTripsByUserId(any());
    }

    @Test
    void testGetConfirmedTrips_emptyList() throws Exception {
        // Given
        when(tripService.getConfirmedTripsByUserId(testUserId)).thenReturn(new ArrayList<>());
        doNothing().when(tripService).validateUserId(testUserId);

        // When & Then
        mockMvc.perform(get("/api/trips")
                        .param("userId", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(tripService, times(1)).validateUserId(testUserId);
        verify(tripService, times(1)).getConfirmedTripsByUserId(testUserId);
    }
}
