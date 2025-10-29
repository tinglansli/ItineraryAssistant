package com.tinglans.backend.controller;

import com.tinglans.backend.common.BusinessException;
import com.tinglans.backend.common.ResponseCode;
import com.tinglans.backend.service.SpeechService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SpeechController 测试类
 */
@WebMvcTest(SpeechController.class)
class SpeechControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpeechService speechService;

    private MockMultipartFile testAudioFile;

    @BeforeEach
    void setUp() {
        // 创建模拟的音频文件
        testAudioFile = new MockMultipartFile(
                "audio",
                "test.wav",
                "audio/wav",
                "test audio content".getBytes()
        );
    }

    @Test
    void testTranscribeAudio_success() throws Exception {
        // Given
        String expectedTranscript = "去北京玩三天";
        when(speechService.transcribeVoice(any())).thenReturn(expectedTranscript);

        // When & Then
        mockMvc.perform(multipart("/api/speech/transcribe")
                        .file(testAudioFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("语音识别成功"))
                .andExpect(jsonPath("$.data").value(expectedTranscript));

        verify(speechService, times(1)).transcribeVoice(any());
    }

    @Test
    void testTranscribeAudio_emptyFile() throws Exception {
        // Given
        when(speechService.transcribeVoice(any()))
                .thenThrow(new BusinessException(ResponseCode.FILE_EMPTY));

        // When & Then
        mockMvc.perform(multipart("/api/speech/transcribe")
                        .file(testAudioFile))
                .andExpect(status().isBadRequest());

        verify(speechService, times(1)).transcribeVoice(any());
    }

    @Test
    void testTranscribeAudio_invalidFormat() throws Exception {
        // Given
        MockMultipartFile invalidFile = new MockMultipartFile(
                "audio",
                "test.txt",
                "text/plain",
                "not audio content".getBytes()
        );

        when(speechService.transcribeVoice(any()))
                .thenThrow(new BusinessException(ResponseCode.INVALID_AUDIO_FORMAT));

        // When & Then
        mockMvc.perform(multipart("/api/speech/transcribe")
                        .file(invalidFile))
                .andExpect(status().isBadRequest());

        verify(speechService, times(1)).transcribeVoice(any());
    }

    @Test
    void testTranscribeAudio_recognitionFailed() throws Exception {
        // Given
        when(speechService.transcribeVoice(any()))
                .thenThrow(new BusinessException(ResponseCode.VOICE_RECOGNITION_FAILED));

        // When & Then
        mockMvc.perform(multipart("/api/speech/transcribe")
                        .file(testAudioFile))
                .andExpect(status().isBadRequest());

        verify(speechService, times(1)).transcribeVoice(any());
    }

    @Test
    void testTranscribeAudio_serviceError() throws Exception {
        // Given
        when(speechService.transcribeVoice(any()))
                .thenThrow(new BusinessException(ResponseCode.INTERNAL_ERROR));

        // When & Then
        mockMvc.perform(multipart("/api/speech/transcribe")
                        .file(testAudioFile))
                .andExpect(status().isInternalServerError());

        verify(speechService, times(1)).transcribeVoice(any());
    }

    @Test
    void testTranscribeAudio_largeFile() throws Exception {
        // Given
        byte[] largeContent = new byte[1024 * 1024]; // 1MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "audio",
                "large.wav",
                "audio/wav",
                largeContent
        );

        String expectedTranscript = "这是一段很长的语音识别结果";
        when(speechService.transcribeVoice(any())).thenReturn(expectedTranscript);

        // When & Then
        mockMvc.perform(multipart("/api/speech/transcribe")
                        .file(largeFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(expectedTranscript));

        verify(speechService, times(1)).transcribeVoice(any());
    }
}
