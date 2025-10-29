package com.tinglans.backend.thirdparty.stt;

import com.tinglans.backend.config.XfyunConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 科大讯飞语音识别客户端测试
 */
@Slf4j
@SpringBootTest
class XfyunAsrClientTest {

    @Autowired
    private XfyunAsrClient xfyunAsrClient;

    @Autowired
    private XfyunConfig xfyunConfig;

    @BeforeEach
    void setUp() {
        log.info("XFyun配置 - AppId: {}, ApiKey: {}", 
                xfyunConfig.getAppId(), 
                xfyunConfig.getApiKey() != null ? "已配置" : "未配置");
    }

    /**
     * 测试语音转写
     */
    @Test
    @Disabled
    void testTranscribeSmallFile() throws Exception {
        // 替换为实际的测试音频文件路径
        File audioFile = new File("src/test/resources/test-audio.wav");
        
        assertTrue(audioFile.exists(), "测试音频文件不存在");
        
        // 执行转写
        String result = xfyunAsrClient.transcribe(audioFile);
        
        // 验证结果
        assertNotNull(result, "转写结果不应为空");
        assertFalse(result.isEmpty(), "转写结果不应为空字符串");
        
        log.info("转写结果: {}", result);
    }
}
