package com.tinglans.backend.common;

import lombok.Data;

/**
 * 统一 API 响应封装
 */
@Data
public class ApiResponse<T> {
    private boolean success;
    private int code;
    private String message;
    private T data;

    private ApiResponse(boolean success, int code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, ResponseCode.SUCCESS.getCode(), 
                ResponseCode.SUCCESS.getMessage(), null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, ResponseCode.SUCCESS.getCode(), 
                ResponseCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, ResponseCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败响应（使用 ResponseCode）
     */
    public static <T> ApiResponse<T> error(ResponseCode responseCode) {
        return new ApiResponse<>(false, responseCode.getCode(), 
                responseCode.getMessage(), null);
    }

    /**
     * 失败响应（自定义消息）
     */
    public static <T> ApiResponse<T> error(ResponseCode responseCode, String message) {
        return new ApiResponse<>(false, responseCode.getCode(), message, null);
    }

    /**
     * 失败响应（完全自定义）
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
