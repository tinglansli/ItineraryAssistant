package com.tinglans.backend.thirdparty.stt.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 科大讯飞任务响应DTO
 */
public class XfyunTaskResponse {

    /**
     * 任务创建响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        private Integer code;
        private String message;
        private String sid;
        private TaskData data;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TaskData {
            @JSONField(name = "task_id")
            private String taskId;
        }
    }

    /**
     * 任务查询响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Query {
        private Integer code;
        private String message;
        private String sid;
        private QueryData data;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class QueryData {
            @JSONField(name = "task_id")
            private String taskId;

            @JSONField(name = "task_status")
            private String taskStatus;  // 1:待处理 2:处理中 3:处理完成 4:回调完成

            @JSONField(name = "task_type")
            private String taskType;

            @JSONField(name = "force_refresh")
            private String forceRefresh;

            private Result result;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Result {
            @JSONField(name = "file_length")
            private Integer fileLength;

            private List<Lattice> lattice;
            private List<Lattice> lattice2;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Lattice {
            private String begin;
            private String end;

            @JSONField(name = "json_1best")
            private Json1Best json1best;

            private String lid;
            private String spk;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Json1Best {
            private St st;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class St {
            private String bg;  // 句子开始位置(ms)
            private String ed;  // 句子结束位置(ms)
            private String pa;  // 段落
            private String pt;  // 保留字段
            private String rl;  // 说话人角色
            private String sc;  // 句子置信度
            private String si;  // vad的ID号
            private List<Rt> rt;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Rt {
            private String nb;
            private String nc;
            private List<Ws> ws;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Ws {
            private List<Cw> cw;
            private Integer wb;  // 词语开始帧数
            private Integer we;  // 词语结束帧数
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Cw {
            private String w;   // 识别结果
            private String wc;  // 词语置信度
            private String wp;  // 词语属性: n-正常词 s-顺滑 p-标点 g-分段
        }
    }
}
