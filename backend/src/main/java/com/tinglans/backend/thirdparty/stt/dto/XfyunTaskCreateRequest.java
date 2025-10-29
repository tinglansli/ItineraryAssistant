package com.tinglans.backend.thirdparty.stt.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 科大讯飞任务创建请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XfyunTaskCreateRequest {

    private Common common;
    private Business business;
    private AudioData data;

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
        @JSONField(name = "request_id")
        private String requestId;

        private String language;  // zh_cn
        private String domain;    // pro_ost_ed
        private String accent;    // mandarin

        @JSONField(name = "callback_url")
        private String callbackUrl;

        @Builder.Default
        @JSONField(name = "vspp_on")
        private Integer vsppOn = 0;  // 是否开启说话人分离

        @Builder.Default
        @JSONField(name = "speaker_num")
        private Integer speakerNum = 0;  // 说话人个数

        @Builder.Default
        @JSONField(name = "postproc_on")
        private Integer postprocOn = 1;  // 后处理开关

        private Integer duration;  // 音频时长（秒）
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioData {
        @JSONField(name = "audio_url")
        private String audioUrl;

        @Builder.Default
        @JSONField(name = "audio_src")
        private String audioSrc = "http";  // 音频来源类型

        private String format;  // 例如：audio/L16;rate=16000

        private String encoding;  // raw/lame
    }
}
