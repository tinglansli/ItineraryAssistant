package com.tinglans.backend.config;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Firebase/Firestore 配置类
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.project-id}")
    private String projectId;
    
    @Value("${firebase.emulator.host}")
    private String emulatorHost;
    
    @Bean
    public Firestore firestore() {
        log.info("初始化 Firestore Emulator 连接");
        log.info("项目ID: {}", projectId);
        log.info("Emulator Host: {}", emulatorHost);
        
        try {
            // 演示项目：仅使用 Firestore Emulator，不需要 credentials
            FirestoreOptions options = FirestoreOptions.newBuilder()
                    .setProjectId(projectId)
                    .setEmulatorHost(emulatorHost)
                    .build();
            
            Firestore firestore = options.getService();
            
            log.info("✅ Firestore Emulator 连接成功");
            return firestore;
        } catch (Exception e) {
            log.error("❌ Firestore Emulator 初始化失败", e);
            log.error("配置信息 - 项目ID: {}, Emulator Host: {}", projectId, emulatorHost);
            throw new RuntimeException("Failed to initialize Firestore Emulator", e);
        }
    }
}
