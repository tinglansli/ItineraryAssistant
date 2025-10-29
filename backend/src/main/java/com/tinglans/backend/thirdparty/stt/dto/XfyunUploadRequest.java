package com.tinglans.backend.thirdparty.stt.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 科大讯飞上传相关请求DTO
 */
public class XfyunUploadRequest {

    /**
     * 小文件上传请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmallFile {
        @JSONField(name = "request_id")
        private String requestId;

        @JSONField(name = "app_id")
        private String appId;

        // 文件数据通过multipart传输，不在JSON中
    }

    /**
     * 分块上传初始化请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Init {
        @JSONField(name = "request_id")
        private String requestId;

        @JSONField(name = "app_id")
        private String appId;

        @Builder.Default
        @JSONField(name = "cloud_id")
        private String cloudId = "0"; // 0代表公有云
    }

    /**
     * 分块上传请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        @JSONField(name = "request_id")
        private String requestId;

        @JSONField(name = "app_id")
        private String appId;

        @JSONField(name = "upload_id")
        private String uploadId;

        @JSONField(name = "slice_id")
        private Integer sliceId;

        // 文件数据通过multipart传输
    }

    /**
     * 分块上传完成请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Complete {
        @JSONField(name = "request_id")
        private String requestId;

        @JSONField(name = "app_id")
        private String appId;

        @JSONField(name = "upload_id")
        private String uploadId;
    }
}
