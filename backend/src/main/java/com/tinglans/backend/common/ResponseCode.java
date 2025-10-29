package com.tinglans.backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 统一响应状态码枚举
 */
@Getter
public enum ResponseCode {
    // 成功
    SUCCESS(200, "操作成功", HttpStatus.OK),
    
    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误", HttpStatus.BAD_REQUEST),
    INVALID_PARAM(400, "参数校验失败", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(401, "未授权", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "无权访问", HttpStatus.FORBIDDEN),
    NOT_FOUND(404, "资源不存在", HttpStatus.NOT_FOUND),
    
    // 业务错误
    TRIP_NOT_FOUND(404, "行程不存在或已过期", HttpStatus.NOT_FOUND),
    TRIP_EXPIRED(404, "行程已过期", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(404, "用户不存在", HttpStatus.NOT_FOUND),
    PERMISSION_DENIED(403, "无权操作该资源", HttpStatus.FORBIDDEN),
    VOICE_RECOGNITION_FAILED(400, "语音识别失败", HttpStatus.BAD_REQUEST),
    FILE_EMPTY(400, "上传文件为空", HttpStatus.BAD_REQUEST),
    INVALID_AUDIO_FORMAT(400, "不支持的音频格式", HttpStatus.BAD_REQUEST),
    
    // 服务器错误 5xx
    INTERNAL_ERROR(500, "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ResponseCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
