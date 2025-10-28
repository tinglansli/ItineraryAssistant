package com.tinglans.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

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
    
    @Value("${firebase.credentials-path:classpath:firebase-service-account.json}")
    private Resource credentialsPath;

    @Bean
    public Firestore firestore() {
        log.info("初始化 Firestore 连接");
        log.info("项目ID: {}", projectId);
        log.info("Firestore Host: {}", emulatorHost);
        
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    credentialsPath.getInputStream()
            );
            
            FirestoreOptions options = FirestoreOptions.newBuilder()
                    .setProjectId(projectId)
                    .setCredentials(credentials)
                    .setEmulatorHost(emulatorHost)
                    .build();
            
            log.info("✅ Firestore 连接成功");
            return options.getService();
        } catch (Exception e) {
            log.error("❌ Firestore 初始化失败", e);
            throw new RuntimeException("Failed to initialize Firestore", e);
        }
    }
}
