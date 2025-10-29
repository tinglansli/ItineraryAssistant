package com.tinglans.backend.controller;

import com.tinglans.backend.common.ApiResponse;
import com.tinglans.backend.domain.Trip;
import com.tinglans.backend.service.TripService;
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
    public ResponseEntity<ApiResponse<Trip>> createTripFromText(@RequestBody CreateTripRequest request) 
            throws Exception {
        Trip trip = tripService.createTripFromText(request.getUserInput(), request.getUserId());
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
    public ResponseEntity<ApiResponse<Trip>> confirmTrip(
            @PathVariable String tripId,
            @RequestBody ConfirmTripRequest request) throws Exception {
        Trip trip = tripService.confirmTrip(tripId, request.getUserId());
        return ResponseEntity.ok(ApiResponse.success("行程确认成功", trip));
    }

    /**
     * 获取行程列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Trip>>> getConfirmedTrips(@RequestParam String userId) throws Exception {
        tripService.validateUserId(userId);
        List<Trip> trips = tripService.getConfirmedTripsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(trips));
    }

    @Data
    public static class CreateTripRequest {
        private String userInput;
        private String userId;
    }

    @Data
    public static class ConfirmTripRequest {
        private String userId;
    }
}
