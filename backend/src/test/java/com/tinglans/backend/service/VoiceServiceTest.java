package com.tinglans.backend.service;

import com.tinglans.backend.thirdparty.stt.XfyunAsrClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * VoiceService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class VoiceServiceTest {

    @Mock
    private XfyunAsrClient xfyunAsrClient;

    @InjectMocks
    private VoiceService voiceService;

    @TempDir
    Path tempDir;

    private File testAudioFile;

    @BeforeEach
    void setUp() throws IOException {
        // 创建临时测试音频文件
        testAudioFile = tempDir.resolve("test-audio.wav").toFile();
        try (FileWriter writer = new FileWriter(testAudioFile)) {
            writer.write("fake audio content");
        }
    }

    @Test
    void testTranscribeVoice_success() throws IOException, InterruptedException {
        // Given
        String expectedTranscript = "我想去东京玩三天";
        when(xfyunAsrClient.transcribe(testAudioFile)).thenReturn(expectedTranscript);

        // When
        String result = voiceService.transcribeVoice(testAudioFile);

        // Then
        assertNotNull(result);
        assertEquals(expectedTranscript, result);

        verify(xfyunAsrClient, times(1)).transcribe(testAudioFile);
    }

    @Test
    void testTranscribeVoice_emptyTranscript() throws IOException, InterruptedException {
        // Given
        String emptyTranscript = "";
        when(xfyunAsrClient.transcribe(testAudioFile)).thenReturn(emptyTranscript);

        // When
        String result = voiceService.transcribeVoice(testAudioFile);

        // Then
        assertNotNull(result);
        assertEquals("", result);

        verify(xfyunAsrClient, times(1)).transcribe(testAudioFile);
    }

    @Test
    void testTranscribeVoice_longTranscript() throws IOException, InterruptedException {
        // Given
        String longTranscript = "我想在11月1号到11月5号去日本东京旅游，希望能体验当地的美食文化，" +
                "参观一些著名的寺庙和景点，预算大概在一万元左右，希望能住得舒适一些";
        when(xfyunAsrClient.transcribe(testAudioFile)).thenReturn(longTranscript);

        // When
        String result = voiceService.transcribeVoice(testAudioFile);

        // Then
        assertNotNull(result);
        assertEquals(longTranscript, result);
        assertTrue(result.length() > 50);
    }

    @Test
    void testTranscribeVoice_chineseCharacters() throws IOException, InterruptedException {
        // Given
        String chineseText = "今天吃拉面花了五十块钱";
        when(xfyunAsrClient.transcribe(testAudioFile)).thenReturn(chineseText);

        // When
        String result = voiceService.transcribeVoice(testAudioFile);

        // Then
        assertEquals(chineseText, result);
    }

    @Test
    void testTranscribeVoice_withPunctuation() throws IOException, InterruptedException {
        // Given
        String textWithPunctuation = "我想去京都，大概三天时间。预算不限！";
        when(xfyunAsrClient.transcribe(testAudioFile)).thenReturn(textWithPunctuation);

        // When
        String result = voiceService.transcribeVoice(testAudioFile);

        // Then
        assertEquals(textWithPunctuation, result);
    }

    @Test
    void testTranscribeVoice_fileNotFound() throws IOException, InterruptedException {
        // Given
        File nonExistentFile = new File("non-existent-file.wav");
        when(xfyunAsrClient.transcribe(nonExistentFile))
                .thenThrow(new IOException("File not found"));

        // When & Then
        assertThrows(IOException.class, () -> {
            voiceService.transcribeVoice(nonExistentFile);
        });

        verify(xfyunAsrClient, times(1)).transcribe(nonExistentFile);
    }

    @Test
    void testTranscribeVoice_xfyunThrowsIOException() throws IOException, InterruptedException {
        // Given
        when(xfyunAsrClient.transcribe(testAudioFile))
                .thenThrow(new IOException("Network error"));

        // When & Then
        IOException exception = assertThrows(IOException.class, () -> {
            voiceService.transcribeVoice(testAudioFile);
        });

        assertTrue(exception.getMessage().contains("Network error"));

        verify(xfyunAsrClient, times(1)).transcribe(testAudioFile);
    }

    @Test
    void testTranscribeVoice_xfyunThrowsInterruptedException() throws IOException, InterruptedException {
        // Given
        when(xfyunAsrClient.transcribe(testAudioFile))
                .thenThrow(new InterruptedException("Service interrupted"));

        // When & Then
        InterruptedException exception = assertThrows(InterruptedException.class, () -> {
            voiceService.transcribeVoice(testAudioFile);
        });

        assertTrue(exception.getMessage().contains("Service interrupted"));

        verify(xfyunAsrClient, times(1)).transcribe(testAudioFile);
    }

    @Test
    void testTranscribeVoice_xfyunReturnsNull() throws IOException, InterruptedException {
        // Given
        when(xfyunAsrClient.transcribe(testAudioFile)).thenReturn(null);

        // When
        String result = voiceService.transcribeVoice(testAudioFile);

        // Then
        assertNull(result);

        verify(xfyunAsrClient, times(1)).transcribe(testAudioFile);
    }

    @Test
    void testTranscribeVoice_multipleFiles() throws IOException, InterruptedException {
        // Given
        File audioFile1 = tempDir.resolve("audio1.wav").toFile();
        File audioFile2 = tempDir.resolve("audio2.wav").toFile();
        audioFile1.createNewFile();
        audioFile2.createNewFile();

        when(xfyunAsrClient.transcribe(audioFile1)).thenReturn("第一段语音");
        when(xfyunAsrClient.transcribe(audioFile2)).thenReturn("第二段语音");

        // When
        String result1 = voiceService.transcribeVoice(audioFile1);
        String result2 = voiceService.transcribeVoice(audioFile2);

        // Then
        assertEquals("第一段语音", result1);
        assertEquals("第二段语音", result2);

        verify(xfyunAsrClient, times(1)).transcribe(audioFile1);
        verify(xfyunAsrClient, times(1)).transcribe(audioFile2);
    }

    @Test
    void testTranscribeVoice_differentFileFormats() throws IOException, InterruptedException {
        // Given - 测试不同的音频文件格式
        File wavFile = tempDir.resolve("audio.wav").toFile();
        File mp3File = tempDir.resolve("audio.mp3").toFile();
        File pcmFile = tempDir.resolve("audio.pcm").toFile();
        
        wavFile.createNewFile();
        mp3File.createNewFile();
        pcmFile.createNewFile();

        when(xfyunAsrClient.transcribe(any(File.class))).thenReturn("转录文本");

        // When
        String resultWav = voiceService.transcribeVoice(wavFile);
        String resultMp3 = voiceService.transcribeVoice(mp3File);
        String resultPcm = voiceService.transcribeVoice(pcmFile);

        // Then
        assertEquals("转录文本", resultWav);
        assertEquals("转录文本", resultMp3);
        assertEquals("转录文本", resultPcm);

        verify(xfyunAsrClient, times(3)).transcribe(any(File.class));
    }

    @Test
    void testTranscribeVoice_largeFile() throws IOException, InterruptedException {
        // Given - 模拟大文件
        File largeFile = tempDir.resolve("large-audio.wav").toFile();
        try (FileWriter writer = new FileWriter(largeFile)) {
            for (int i = 0; i < 10000; i++) {
                writer.write("audio data ");
            }
        }

        String longTranscript = "这是一段很长的语音转录结果，包含了大量的信息内容";
        when(xfyunAsrClient.transcribe(largeFile)).thenReturn(longTranscript);

        // When
        String result = voiceService.transcribeVoice(largeFile);

        // Then
        assertEquals(longTranscript, result);
        assertTrue(largeFile.length() > 100000);  // 验证文件确实比较大
    }

    @Test
    void testTranscribeVoice_specialCharacters() throws IOException, InterruptedException {
        // Given
        String textWithSpecialChars = "价格是￥100，时间是09:00-18:00，地址在东京都";
        when(xfyunAsrClient.transcribe(testAudioFile)).thenReturn(textWithSpecialChars);

        // When
        String result = voiceService.transcribeVoice(testAudioFile);

        // Then
        assertEquals(textWithSpecialChars, result);
    }

    @Test
    void testTranscribeVoice_numbersAndText() throws IOException, InterruptedException {
        // Given
        String mixedContent = "我想预订2024年11月1日到11月5日的行程，大概3到5天";
        when(xfyunAsrClient.transcribe(testAudioFile)).thenReturn(mixedContent);

        // When
        String result = voiceService.transcribeVoice(testAudioFile);

        // Then
        assertEquals(mixedContent, result);
    }

    @Test
    void testTranscribeVoice_verifyLogging() throws IOException, InterruptedException {
        // Given
        String transcript = "测试日志记录";
        when(xfyunAsrClient.transcribe(testAudioFile)).thenReturn(transcript);

        // When
        String result = voiceService.transcribeVoice(testAudioFile);

        // Then
        // 验证服务正常工作（日志验证需要日志框架支持，这里验证功能性）
        assertNotNull(result);
        assertEquals(transcript, result);
    }

    @Test
    void testTranscribeVoice_sequentialCalls() throws IOException, InterruptedException {
        // Given - 连续调用
        when(xfyunAsrClient.transcribe(testAudioFile))
                .thenReturn("第一次识别")
                .thenReturn("第二次识别")
                .thenReturn("第三次识别");

        // When
        String result1 = voiceService.transcribeVoice(testAudioFile);
        String result2 = voiceService.transcribeVoice(testAudioFile);
        String result3 = voiceService.transcribeVoice(testAudioFile);

        // Then
        assertEquals("第一次识别", result1);
        assertEquals("第二次识别", result2);
        assertEquals("第三次识别", result3);

        verify(xfyunAsrClient, times(3)).transcribe(testAudioFile);
    }

    @Test
    void testTranscribeVoice_fileNameWithSpaces() throws IOException, InterruptedException {
        // Given
        File fileWithSpaces = tempDir.resolve("audio file with spaces.wav").toFile();
        fileWithSpaces.createNewFile();

        String transcript = "文件名包含空格";
        when(xfyunAsrClient.transcribe(fileWithSpaces)).thenReturn(transcript);

        // When
        String result = voiceService.transcribeVoice(fileWithSpaces);

        // Then
        assertEquals(transcript, result);
    }

    @Test
    void testTranscribeVoice_emptyFile() throws IOException, InterruptedException {
        // Given - 空文件
        File emptyFile = tempDir.resolve("empty.wav").toFile();
        emptyFile.createNewFile();

        when(xfyunAsrClient.transcribe(emptyFile)).thenReturn("");

        // When
        String result = voiceService.transcribeVoice(emptyFile);

        // Then
        assertEquals("", result);
        assertEquals(0L, emptyFile.length());
    }
}
