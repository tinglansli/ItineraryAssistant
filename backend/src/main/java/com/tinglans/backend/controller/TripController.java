package com.tinglans.backend.controller;

import com.tinglans.backend.common.ApiResponse;
import com.tinglans.backend.domain.Trip;
import com.tinglans.backend.dto.TripSummary;
import com.tinglans.backend.service.TripService;
import com.tinglans.backend.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行程控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    /**
     * 从文本创建行程预览
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Trip>> createTripFromText(
            @RequestBody CreateTripRequest request,
            HttpServletRequest httpRequest) throws Exception {
        String userId = AuthUtil.getCurrentUserId(httpRequest);
        Trip trip = tripService.createTripFromText(request.getUserInput(), userId);
        return ResponseEntity.ok(ApiResponse.success("行程生成成功", trip));
    }

    /**
     * 获取行程详情
     */
    @GetMapping("/{tripId}/itinerary")
    public ResponseEntity<ApiResponse<Trip>> getTripItinerary(@PathVariable String tripId) throws Exception {
        Trip trip = tripService.validateAndGetTrip(tripId);
        return ResponseEntity.ok(ApiResponse.success(trip));
    }

    /**
     * 确认行程
     */
    @PostMapping("/{tripId}/confirm")
    public ResponseEntity<ApiResponse<TripSummary>> confirmTrip(
            @PathVariable String tripId,
            HttpServletRequest httpRequest) throws Exception {
        String userId = AuthUtil.getCurrentUserId(httpRequest);
        Trip trip = tripService.confirmTrip(tripId, userId);
        TripSummary summary = tripService.convertToSummary(trip);
        return ResponseEntity.ok(ApiResponse.success("行程确认成功", summary));
    }

    /**
     * 获取行程列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TripSummary>>> getConfirmedTrips(HttpServletRequest httpRequest) throws Exception {
        String userId = AuthUtil.getCurrentUserId(httpRequest);
        tripService.validateUserId(userId);
        List<Trip> trips = tripService.getConfirmedTripsByUserId(userId);
        List<TripSummary> summaries = tripService.convertToSummaryList(trips);
        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    @Data
    public static class CreateTripRequest {
        private String userInput;
    }
}
