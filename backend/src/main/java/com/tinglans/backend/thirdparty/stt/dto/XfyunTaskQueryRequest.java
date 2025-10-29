package com.tinglans.backend.thirdparty.stt.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 科大讯飞任务查询请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XfyunTaskQueryRequest {

    private Common common;
    private Business business;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Common {
        @JSONField(name = "app_id")
        private String appId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Business {
        @JSONField(name = "task_id")
        private String taskId;
    }
}
