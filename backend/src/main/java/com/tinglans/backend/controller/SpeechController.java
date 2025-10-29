package com.tinglans.backend.controller;

import com.tinglans.backend.thirdparty.stt.XfyunAsrClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 语音识别控制器（示例）
 */
@Slf4j
@RestController
@RequestMapping("/api/speech")
public class SpeechController {

    private final XfyunAsrClient xfyunAsrClient;

    public SpeechController(XfyunAsrClient xfyunAsrClient) {
        this.xfyunAsrClient = xfyunAsrClient;
    }

    /**
     * 上传音频文件并转写为文本
     *
     * @param file 音频文件（wav/pcm/mp3格式，16k采样率，16bit，单声道）
     * @return 转写结果
     */
    @PostMapping("/transcribe")
    public ResponseEntity<Map<String, Object>> transcribe(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        File tempFile = null;

        try {
            // 验证文件
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "上传的文件为空");
                return ResponseEntity.badRequest().body(response);
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !isValidAudioFile(originalFilename)) {
                response.put("success", false);
                response.put("message", "不支持的文件格式，仅支持 wav、pcm、mp3");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("收到音频文件: {}, 大小: {} bytes", originalFilename, file.getSize());

            // 创建临时文件
            String suffix = getFileSuffix(originalFilename);
            tempFile = Files.createTempFile("audio_", suffix).toFile();
            file.transferTo(tempFile);

            // 调用科大讯飞API转写
            String text = xfyunAsrClient.transcribe(tempFile);

            // 返回结果
            response.put("success", true);
            response.put("text", text);
            response.put("filename", originalFilename);
            response.put("fileSize", file.getSize());

            log.info("转写成功: {} -> {}", originalFilename, text);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("文件处理失败", e);
            response.put("success", false);
            response.put("message", "文件处理失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (InterruptedException e) {
            log.error("转写任务被中断", e);
            Thread.currentThread().interrupt();
            response.put("success", false);
            response.put("message", "转写任务被中断");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception e) {
            log.error("转写失败", e);
            response.put("success", false);
            response.put("message", "转写失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                    log.debug("已删除临时文件: {}", tempFile.getPath());
                } catch (IOException e) {
                    log.warn("删除临时文件失败: {}", tempFile.getPath(), e);
                }
            }
        }
    }

    /**
     * 验证是否为支持的音频文件格式
     */
    private boolean isValidAudioFile(String filename) {
        String lowerCase = filename.toLowerCase();
        return lowerCase.endsWith(".wav") || 
               lowerCase.endsWith(".pcm") || 
               lowerCase.endsWith(".mp3");
    }

    /**
     * 获取文件后缀
     */
    private String getFileSuffix(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex);
        }
        return ".tmp";
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "speech-recognition");
        return ResponseEntity.ok(response);
    }
}
