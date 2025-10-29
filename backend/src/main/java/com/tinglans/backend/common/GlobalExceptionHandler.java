package com.tinglans.backend.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.concurrent.ExecutionException;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getDisplayMessage());
        ApiResponse<Void> response = ApiResponse.error(e.getResponseCode(), e.getDisplayMessage());
        return ResponseEntity.status(e.getResponseCode().getHttpStatus()).body(response);
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        ApiResponse<Void> response = ApiResponse.error(ResponseCode.INVALID_PARAM, e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("上传文件过大: {}", e.getMessage());
        ApiResponse<Void> response = ApiResponse.error(ResponseCode.BAD_REQUEST, "上传文件过大");
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理异步执行异常
     */
    @ExceptionHandler({ExecutionException.class, InterruptedException.class})
    public ResponseEntity<ApiResponse<Void>> handleExecutionException(Exception e) {
        log.error("异步执行异常", e);
        ApiResponse<Void> response = ApiResponse.error(ResponseCode.INTERNAL_ERROR);
        return ResponseEntity.status(ResponseCode.INTERNAL_ERROR.getHttpStatus()).body(response);
    }

    /**
     * 处理所有未捕获异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("未知异常", e);
        ApiResponse<Void> response = ApiResponse.error(ResponseCode.INTERNAL_ERROR, "系统错误，请稍后重试");
        return ResponseEntity.status(ResponseCode.INTERNAL_ERROR.getHttpStatus()).body(response);
    }
}
