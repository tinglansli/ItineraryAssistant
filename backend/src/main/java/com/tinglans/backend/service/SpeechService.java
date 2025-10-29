package com.tinglans.backend.service;

import com.tinglans.backend.common.BusinessException;
import com.tinglans.backend.common.ResponseCode;
import com.tinglans.backend.thirdparty.stt.XfyunAsrClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 语音服务层
 * 职责：提供通用的语音转文字功能，与具体业务逻辑无关
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechService {

    private final XfyunAsrClient xfyunAsrClient;

    /**
     * 语音转文本（从 MultipartFile）
     * 处理文件上传、校验、临时文件管理等业务逻辑
     *
     * @param audioFile 音频文件
     * @return 识别的文本
     * @throws IOException            文件读取异常
     * @throws InterruptedException   STT 调用中断异常
     */
    public String transcribeVoice(MultipartFile audioFile) throws IOException, InterruptedException {
        // 1. 校验文件
        validateAudioFile(audioFile);
        
        log.info("收到语音转文字请求，文件名: {}, 大小: {} bytes", 
                audioFile.getOriginalFilename(), audioFile.getSize());

        // 2. 创建临时文件并进行识别
        File tempFile = null;
        try {
            tempFile = createTempFile(audioFile);
            log.info("临时文件已创建: {}", tempFile.getAbsolutePath());

            // 3. 调用 STT 服务进行语音识别
            String transcript = transcribeFromFile(tempFile);
            
            log.info("语音识别成功，返回文本长度: {}", transcript.length());
            return transcript;

        } finally {
            // 4. 清理临时文件
            cleanupTempFile(tempFile);
        }
    }

    /**
     * 语音转文本
     * 直接处理文件的语音识别
     *
     * @param audioFile 音频文件
     * @return 识别的文本
     * @throws IOException            文件读取异常
     * @throws InterruptedException   STT 调用中断异常
     */
    private String transcribeFromFile(File audioFile) throws IOException, InterruptedException {
        if (audioFile == null || !audioFile.exists()) {
            throw new BusinessException(ResponseCode.INVALID_PARAM, "音频文件不存在");
        }
        
        log.info("开始语音识别，文件: {}", audioFile.getName());

        // 调用 STT 服务进行语音识别
        String transcript = xfyunAsrClient.transcribe(audioFile);
        
        if (!StringUtils.hasText(transcript)) {
            throw new BusinessException(ResponseCode.VOICE_RECOGNITION_FAILED, "语音识别失败，未能识别出文本");
        }
        
        log.info("语音识别完成，文本长度: {}, 内容: {}", 
                transcript.length(), transcript);
        return transcript;
    }

    /**
     * 校验音频文件
     */
    private void validateAudioFile(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(ResponseCode.INVALID_PARAM, "音频文件不能为空");
        }
    }

    /**
     * 创建临时文件
     */
    private File createTempFile(MultipartFile audioFile) throws IOException {
        // 获取原始文件名和扩展名
        String originalFilename = audioFile.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".tmp";
        
        // 创建临时文件
        File tempFile = File.createTempFile("audio_", extension);
        
        // 将上传的文件保存到临时文件
        Path tempPath = tempFile.toPath();
        Files.copy(audioFile.getInputStream(), tempPath, StandardCopyOption.REPLACE_EXISTING);
        
        return tempFile;
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            boolean deleted = tempFile.delete();
            if (deleted) {
                log.info("临时文件已删除: {}", tempFile.getAbsolutePath());
            } else {
                log.warn("临时文件删除失败: {}", tempFile.getAbsolutePath());
            }
        }
    }
}
