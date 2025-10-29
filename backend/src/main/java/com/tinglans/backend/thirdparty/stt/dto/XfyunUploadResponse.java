package com.tinglans.backend.thirdparty.stt.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 科大讯飞上传响应DTO
 */
public class XfyunUploadResponse {

    /**
     * 小文件上传响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmallFile {
        private Integer code;
        private String message;
        private String sid;
        private FileData data;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class FileData {
            private String url;
        }
    }

    /**
     * 分块上传初始化响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Init {
        private Integer code;
        private String message;
        private String sid;
        private UploadData data;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class UploadData {
            @JSONField(name = "upload_id")
            private String uploadId;
        }
    }

    /**
     * 分块上传响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private Integer code;
        private String message;
        private String sid;
    }

    /**
     * 分块上传完成响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Complete {
        private Integer code;
        private String message;
        private String sid;
        private FileData data;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class FileData {
            private String url;
        }
    }
}
