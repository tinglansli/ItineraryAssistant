package com.tinglans.backend.controller;

import com.tinglans.backend.common.ApiResponse;
import com.tinglans.backend.service.SpeechService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 语音控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/speech")
public class SpeechController {

    private final SpeechService speechService;

    /**
     * 语音转文字
     */
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> transcribeAudio(
            @RequestPart("audio") MultipartFile audioFile,
            HttpServletRequest httpRequest) throws Exception {
        String transcript = speechService.transcribeVoice(audioFile);
        return ResponseEntity.ok(ApiResponse.success("语音识别成功", transcript));
    }
}
