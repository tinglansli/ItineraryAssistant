package com.tinglans.backend.service;

import com.tinglans.backend.common.BusinessException;
import com.tinglans.backend.common.ResponseCode;
import com.tinglans.backend.thirdparty.stt.XfyunAsrClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SpeechService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SpeechServiceTest {

    @Mock
    private XfyunAsrClient xfyunAsrClient;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private SpeechService speechService;

    private byte[] mockAudioData;
    private String expectedTranscript;

    @BeforeEach
    void setUp() {
        mockAudioData = "mock audio data".getBytes();
        expectedTranscript = "今天去东京塔玩";
    }

    @Test
    void testTranscribeVoice_success() throws IOException, InterruptedException {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("test.wav");
        when(multipartFile.getSize()).thenReturn((long) mockAudioData.length);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(mockAudioData));
        when(xfyunAsrClient.transcribe(any())).thenReturn(expectedTranscript);

        // When
        String result = speechService.transcribeVoice(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(expectedTranscript, result);
        verify(xfyunAsrClient, times(1)).transcribe(any());
        verify(multipartFile, times(1)).getInputStream();
    }

    @Test
    void testTranscribeVoice_withNullFile() throws Exception {
        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            speechService.transcribeVoice(null);
        });

        assertEquals(ResponseCode.INVALID_PARAM, exception.getResponseCode());
        assertEquals("音频文件不能为空", exception.getMessage());
        verify(xfyunAsrClient, never()).transcribe(any());
    }

    @Test
    void testTranscribeVoice_withEmptyFile() throws Exception {
        // Given
        when(multipartFile.isEmpty()).thenReturn(true);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            speechService.transcribeVoice(multipartFile);
        });

        assertEquals(ResponseCode.INVALID_PARAM, exception.getResponseCode());
        assertEquals("音频文件不能为空", exception.getMessage());
        verify(xfyunAsrClient, never()).transcribe(any());
    }

    @Test
    void testTranscribeVoice_recognitionReturnsEmpty() throws IOException, InterruptedException {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("test.wav");
        when(multipartFile.getSize()).thenReturn((long) mockAudioData.length);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(mockAudioData));
        when(xfyunAsrClient.transcribe(any())).thenReturn("");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            speechService.transcribeVoice(multipartFile);
        });

        assertEquals(ResponseCode.VOICE_RECOGNITION_FAILED, exception.getResponseCode());
        assertEquals("语音识别失败，未能识别出文本", exception.getMessage());
    }

    @Test
    void testTranscribeVoice_recognitionReturnsNull() throws IOException, InterruptedException {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("test.wav");
        when(multipartFile.getSize()).thenReturn((long) mockAudioData.length);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(mockAudioData));
        when(xfyunAsrClient.transcribe(any())).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            speechService.transcribeVoice(multipartFile);
        });

        assertEquals(ResponseCode.VOICE_RECOGNITION_FAILED, exception.getResponseCode());
    }

    @Test
    void testTranscribeVoice_withDifferentFileExtensions() throws IOException, InterruptedException {
        // Test with .mp3 extension
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("audio.mp3");
        when(multipartFile.getSize()).thenReturn((long) mockAudioData.length);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(mockAudioData));
        when(xfyunAsrClient.transcribe(any())).thenReturn(expectedTranscript);

        String result = speechService.transcribeVoice(multipartFile);

        assertNotNull(result);
        assertEquals(expectedTranscript, result);
    }

    @Test
    void testTranscribeVoice_withNoExtension() throws IOException, InterruptedException {
        // Given - 文件名没有扩展名
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("audiofile");
        when(multipartFile.getSize()).thenReturn((long) mockAudioData.length);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(mockAudioData));
        when(xfyunAsrClient.transcribe(any())).thenReturn(expectedTranscript);

        // When
        String result = speechService.transcribeVoice(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(expectedTranscript, result);
    }

    @Test
    void testTranscribeVoice_ioException() throws Exception {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("test.wav");
        when(multipartFile.getSize()).thenReturn((long) mockAudioData.length);
        when(multipartFile.getInputStream()).thenThrow(new IOException("Failed to read file"));

        // When & Then
        assertThrows(IOException.class, () -> {
            speechService.transcribeVoice(multipartFile);
        });

        verify(xfyunAsrClient, never()).transcribe(any());
    }

    @Test
    void testTranscribeVoice_sttServiceThrowsException() throws Exception {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("test.wav");
        when(multipartFile.getSize()).thenReturn((long) mockAudioData.length);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(mockAudioData));
        when(xfyunAsrClient.transcribe(any())).thenThrow(new InterruptedException("STT service interrupted"));

        // When & Then
        assertThrows(InterruptedException.class, () -> {
            speechService.transcribeVoice(multipartFile);
        });
    }

    @Test
    void testTranscribeVoice_longTranscript() throws IOException, InterruptedException {
        // Given - 测试长文本识别
        String longTranscript = "今天的行程安排是早上八点从酒店出发，先去浅草寺参观，然后去天空树，中午在附近吃拉面，下午去秋叶原购物，晚上回酒店休息。";
        
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("long_audio.wav");
        when(multipartFile.getSize()).thenReturn(102400L);  // 100KB
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(mockAudioData));
        when(xfyunAsrClient.transcribe(any())).thenReturn(longTranscript);

        // When
        String result = speechService.transcribeVoice(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(longTranscript, result);
        assertTrue(result.length() > 50);
    }

    @Test
    void testTranscribeVoice_chineseAndEnglishMixed() throws IOException, InterruptedException {
        // Given - 测试中英文混合识别
        String mixedTranscript = "明天去Tokyo Tower和Shibuya";
        
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("mixed.wav");
        when(multipartFile.getSize()).thenReturn((long) mockAudioData.length);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(mockAudioData));
        when(xfyunAsrClient.transcribe(any())).thenReturn(mixedTranscript);

        // When
        String result = speechService.transcribeVoice(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(mixedTranscript, result);
        assertTrue(result.contains("Tokyo"));
        assertTrue(result.contains("明天"));
    }
}
