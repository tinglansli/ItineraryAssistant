package com.tinglans.backend.service;

import com.tinglans.backend.thirdparty.stt.XfyunAsrClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

/**
 * 语音服务层
 * 职责：提供通用的语音转文字功能，与具体业务逻辑无关
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceService {

    private final XfyunAsrClient xfyunAsrClient;

    /**
     * 语音转文本
     * 通用接口，适用于所有需要语音识别的场景（行程创建、记账等）
     *
     * @param audioFile 音频文件
     * @return 识别的文本
     * @throws IOException            文件读取异常
     * @throws InterruptedException   STT 调用中断异常
     */
    public String transcribeVoice(File audioFile) throws IOException, InterruptedException {
        log.info("开始语音识别，文件: {}", audioFile.getName());

        // 调用 STT 服务进行语音识别
        String transcript = xfyunAsrClient.transcribe(audioFile);
        
        log.info("语音识别完成，文本长度: {}, 内容: {}", 
                transcript != null ? transcript.length() : 0, transcript);
        return transcript;
    }
}
