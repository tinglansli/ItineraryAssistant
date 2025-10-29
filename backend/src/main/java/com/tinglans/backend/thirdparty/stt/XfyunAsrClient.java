package com.tinglans.backend.thirdparty.stt;

import com.alibaba.fastjson.JSON;
import com.tinglans.backend.config.XfyunConfig;
import com.tinglans.backend.thirdparty.stt.dto.*;
import com.tinglans.backend.thirdparty.stt.util.FileUtil;
import com.tinglans.backend.thirdparty.stt.util.SignUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 科大讯飞语音识别客户端
 */
@Slf4j
@Component
public class XfyunAsrClient {

    // API端点
    private static final String UPLOAD_HOST = "upload-ost-api.xfyun.cn";
    private static final String TASK_HOST = "ost-api.xfyun.cn";

    private static final String SMALL_FILE_UPLOAD_URL = "https://" + UPLOAD_HOST + "/file/upload";
    private static final String MULTIPART_INIT_URL = "https://" + UPLOAD_HOST + "/file/mpupload/init";
    private static final String MULTIPART_UPLOAD_URL = "https://" + UPLOAD_HOST + "/file/mpupload/upload";
    private static final String MULTIPART_COMPLETE_URL = "https://" + UPLOAD_HOST + "/file/mpupload/complete";
    private static final String TASK_CREATE_URL = "https://" + TASK_HOST + "/v2/ost/pro_create";
    private static final String TASK_QUERY_URL = "https://" + TASK_HOST + "/v2/ost/query";

    private final XfyunConfig config;
    private final RestTemplate restTemplate;

    public XfyunAsrClient(XfyunConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 转写音频文件
     *
     * @param audioFile 音频文件
     * @return 识别结果文本
     */
    public String transcribe(File audioFile) throws IOException, InterruptedException {
        log.info("开始转写音频文件: {}, 大小: {} bytes", audioFile.getName(), audioFile.length());

        // 1. 上传音频文件
        String audioUrl = uploadFile(audioFile);
        log.info("音频文件上传成功，URL: {}", audioUrl);

        // 2. 创建转写任务
        String taskId = createTask(audioUrl, audioFile.getName());
        log.info("转写任务创建成功，任务ID: {}", taskId);

        // 3. 轮询查询任务状态
        String result = pollTaskResult(taskId);
        log.info("转写完成，文本长度: {}", result.length());

        return result;
    }

    /**
     * 上传音频文件
     */
    private String uploadFile(File audioFile) throws IOException {
        if (FileUtil.isSmallFile(audioFile)) {
            return uploadSmallFile(audioFile);
        } else {
            return uploadLargeFile(audioFile);
        }
    }

    /**
     * 上传小文件（小于30MB）
     */
    private String uploadSmallFile(File audioFile) throws IOException {
        String requestId = generateRequestId();

        // 生成签名
        Map<String, String> authHeaders = SignUtil.generateHeaders(
                "POST", "/file/upload", UPLOAD_HOST,
                config.getApiKey(), config.getApiSecret()
        );

        // 构建multipart请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        authHeaders.forEach(headers::add);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("request_id", requestId);
        body.add("app_id", config.getAppId());
        body.add("data", new ByteArrayResource(FileUtil.readFileBytes(audioFile)) {
            @Override
            public String getFilename() {
                return audioFile.getName();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // 发送请求
        ResponseEntity<String> response = restTemplate.exchange(
                SMALL_FILE_UPLOAD_URL,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // 解析响应
        XfyunUploadResponse.SmallFile uploadResponse = JSON.parseObject(
                response.getBody(),
                XfyunUploadResponse.SmallFile.class
        );

        if (uploadResponse.getCode() != 0) {
            throw new RuntimeException("文件上传失败: " + uploadResponse.getMessage());
        }

        return uploadResponse.getData().getUrl();
    }

    /**
     * 上传大文件（大于30MB，使用分块上传）
     */
    private String uploadLargeFile(File audioFile) throws IOException {
        String requestId = generateRequestId();

        // 1. 初始化分块上传
        String uploadId = initMultipartUpload(requestId);
        log.info("分块上传初始化成功，uploadId: {}", uploadId);

        // 2. 分块上传文件
        List<FileUtil.FileChunk> chunks = FileUtil.splitFile(audioFile);
        for (FileUtil.FileChunk chunk : chunks) {
            uploadFilePart(requestId, uploadId, chunk, audioFile.getName());
            log.debug("上传分块 {}/{}", chunk.getIndex() + 1, chunks.size());
        }

        // 3. 完成分块上传
        String audioUrl = completeMultipartUpload(requestId, uploadId);
        log.info("分块上传完成，URL: {}", audioUrl);

        return audioUrl;
    }

    /**
     * 初始化分块上传
     */
    private String initMultipartUpload(String requestId) {
        // 生成签名
        Map<String, String> authHeaders = SignUtil.generateHeaders(
                "POST", "/file/mpupload/init", UPLOAD_HOST,
                config.getApiKey(), config.getApiSecret()
        );

        // 构建请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.forEach(headers::add);

        XfyunUploadRequest.Init initRequest = XfyunUploadRequest.Init.builder()
                .requestId(requestId)
                .appId(config.getAppId())
                .cloudId("0")
                .build();

        HttpEntity<String> requestEntity = new HttpEntity<>(JSON.toJSONString(initRequest), headers);

        // 发送请求
        ResponseEntity<String> response = restTemplate.exchange(
                MULTIPART_INIT_URL,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // 解析响应
        XfyunUploadResponse.Init initResponse = JSON.parseObject(
                response.getBody(),
                XfyunUploadResponse.Init.class
        );

        if (initResponse.getCode() != 0) {
            throw new RuntimeException("初始化分块上传失败: " + initResponse.getMessage());
        }

        return initResponse.getData().getUploadId();
    }

    /**
     * 上传文件分块
     */
    private void uploadFilePart(String requestId, String uploadId, FileUtil.FileChunk chunk, String fileName) {
        // 生成签名
        Map<String, String> authHeaders = SignUtil.generateHeaders(
                "POST", "/file/mpupload/upload", UPLOAD_HOST,
                config.getApiKey(), config.getApiSecret()
        );

        // 构建multipart请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        authHeaders.forEach(headers::add);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("request_id", requestId);
        body.add("app_id", config.getAppId());
        body.add("upload_id", uploadId);
        body.add("slice_id", String.valueOf(chunk.getIndex()));
        body.add("data", new ByteArrayResource(chunk.getData()) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // 发送请求
        ResponseEntity<String> response = restTemplate.exchange(
                MULTIPART_UPLOAD_URL,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // 解析响应
        XfyunUploadResponse.Part partResponse = JSON.parseObject(
                response.getBody(),
                XfyunUploadResponse.Part.class
        );

        if (partResponse.getCode() != 0) {
            throw new RuntimeException("上传分块失败: " + partResponse.getMessage());
        }
    }

    /**
     * 完成分块上传
     */
    private String completeMultipartUpload(String requestId, String uploadId) {
        // 生成签名
        Map<String, String> authHeaders = SignUtil.generateHeaders(
                "POST", "/file/mpupload/complete", UPLOAD_HOST,
                config.getApiKey(), config.getApiSecret()
        );

        // 构建请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.forEach(headers::add);

        XfyunUploadRequest.Complete completeRequest = XfyunUploadRequest.Complete.builder()
                .requestId(requestId)
                .appId(config.getAppId())
                .uploadId(uploadId)
                .build();

        HttpEntity<String> requestEntity = new HttpEntity<>(JSON.toJSONString(completeRequest), headers);

        // 发送请求
        ResponseEntity<String> response = restTemplate.exchange(
                MULTIPART_COMPLETE_URL,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // 解析响应
        XfyunUploadResponse.Complete completeResponse = JSON.parseObject(
                response.getBody(),
                XfyunUploadResponse.Complete.class
        );

        if (completeResponse.getCode() != 0) {
            throw new RuntimeException("完成分块上传失败: " + completeResponse.getMessage());
        }

        return completeResponse.getData().getUrl();
    }

    /**
     * 创建转写任务
     */
    private String createTask(String audioUrl, String fileName) {
        // 生成签名
        Map<String, String> authHeaders = SignUtil.generateHeaders(
                "POST", "/v2/ost/pro_create", TASK_HOST,
                config.getApiKey(), config.getApiSecret()
        );

        // 构建请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.forEach(headers::add);

        XfyunTaskCreateRequest createRequest = XfyunTaskCreateRequest.builder()
                .common(XfyunTaskCreateRequest.Common.builder()
                        .appId(config.getAppId())
                        .build())
                .business(XfyunTaskCreateRequest.Business.builder()
                        .requestId(generateRequestId())
                        .language("zh_cn")
                        .domain("pro_ost_ed")
                        .accent("mandarin")
                        .postprocOn(1)
                        .build())
                .data(XfyunTaskCreateRequest.AudioData.builder()
                        .audioUrl(audioUrl)
                        .audioSrc("http")
                        .format(FileUtil.getFormat(fileName))
                        .encoding(FileUtil.getEncoding(fileName))
                        .build())
                .build();

        HttpEntity<String> requestEntity = new HttpEntity<>(JSON.toJSONString(createRequest), headers);

        // 发送请求
        ResponseEntity<String> response = restTemplate.exchange(
                TASK_CREATE_URL,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // 解析响应
        XfyunTaskResponse.Create createResponse = JSON.parseObject(
                response.getBody(),
                XfyunTaskResponse.Create.class
        );

        if (createResponse.getCode() != 0) {
            throw new RuntimeException("创建转写任务失败: " + createResponse.getMessage());
        }

        return createResponse.getData().getTaskId();
    }

    /**
     * 轮询查询任务结果
     */
    private String pollTaskResult(String taskId) throws InterruptedException {
        int pollCount = 0;

        while (pollCount < config.getMaxPollCount()) {
            XfyunTaskResponse.Query queryResponse = queryTask(taskId);

            String taskStatus = queryResponse.getData().getTaskStatus();
            log.debug("任务状态: {}, 轮询次数: {}/{}", taskStatus, pollCount + 1, config.getMaxPollCount());

            // 任务状态：1-待处理 2-处理中 3-处理完成 4-回调完成
            if ("3".equals(taskStatus) || "4".equals(taskStatus)) {
                return extractText(queryResponse);
            }

            // 等待后继续轮询
            Thread.sleep(config.getPollInterval());
            pollCount++;
        }

        throw new RuntimeException("转写任务超时，任务ID: " + taskId);
    }

    /**
     * 查询任务状态
     */
    private XfyunTaskResponse.Query queryTask(String taskId) {
        // 生成签名
        Map<String, String> authHeaders = SignUtil.generateHeaders(
                "POST", "/v2/ost/query", TASK_HOST,
                config.getApiKey(), config.getApiSecret()
        );

        // 构建请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.forEach(headers::add);

        XfyunTaskQueryRequest queryRequest = XfyunTaskQueryRequest.builder()
                .common(XfyunTaskQueryRequest.Common.builder()
                        .appId(config.getAppId())
                        .build())
                .business(XfyunTaskQueryRequest.Business.builder()
                        .taskId(taskId)
                        .build())
                .build();

        HttpEntity<String> requestEntity = new HttpEntity<>(JSON.toJSONString(queryRequest), headers);

        // 发送请求
        ResponseEntity<String> response = restTemplate.exchange(
                TASK_QUERY_URL,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // 解析响应
        XfyunTaskResponse.Query queryResponse = JSON.parseObject(
                response.getBody(),
                XfyunTaskResponse.Query.class
        );

        if (queryResponse.getCode() != 0) {
            throw new RuntimeException("查询任务失败: " + queryResponse.getMessage());
        }

        return queryResponse;
    }

    /**
     * 从查询结果中提取文本
     */
    private String extractText(XfyunTaskResponse.Query queryResponse) {
        StringBuilder text = new StringBuilder();

        XfyunTaskResponse.Query.Result result = queryResponse.getData().getResult();
        if (result == null || result.getLattice() == null) {
            return "";
        }

        // 使用lattice（经过后处理的识别结果）
        for (XfyunTaskResponse.Query.Lattice lattice : result.getLattice()) {
            if (lattice.getJson1best() != null && lattice.getJson1best().getSt() != null) {
                XfyunTaskResponse.Query.St st = lattice.getJson1best().getSt();

                if (st.getRt() != null) {
                    for (XfyunTaskResponse.Query.Rt rt : st.getRt()) {
                        if (rt.getWs() != null) {
                            for (XfyunTaskResponse.Query.Ws ws : rt.getWs()) {
                                if (ws.getCw() != null && !ws.getCw().isEmpty()) {
                                    // 获取第一个候选词
                                    XfyunTaskResponse.Query.Cw cw = ws.getCw().get(0);
                                    text.append(cw.getW());
                                }
                            }
                        }
                    }
                }
            }
        }

        return text.toString();
    }

    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
