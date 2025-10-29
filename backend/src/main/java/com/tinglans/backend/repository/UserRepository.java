package com.tinglans.backend.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.tinglans.backend.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * User 数据访问层
 * 管理 users 集合
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepository {

    private static final String COLLECTION_USERS = "users";

    private final Firestore firestore;

    /**
     * 保存或更新用户
     */
    public void save(User user) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_USERS).document(user.getId());
        
        Map<String, Object> data = convertUserToMap(user);
        ApiFuture<WriteResult> result = docRef.set(data);
        result.get();
        
        log.info("保存用户到 Firestore: userId={}", user.getId());
    }

    /**
     * 根据ID获取用户
     */
    public Optional<User> findById(String userId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_USERS).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();

        if (!document.exists()) {
            return Optional.empty();
        }

        log.debug("从 Firestore 获取用户: userId={}", userId);
        return Optional.of(convertDocumentToUser(document));
    }

    /**
     * 根据邮箱查找用户
     */
    public Optional<User> findByEmail(String email) throws ExecutionException, InterruptedException {
        CollectionReference usersRef = firestore.collection(COLLECTION_USERS);
        Query query = usersRef.whereEqualTo("email", email).limit(1);

        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        if (documents.isEmpty()) {
            return Optional.empty();
        }

        log.debug("通过邮箱查找用户: email={}", email);
        return Optional.of(convertDocumentToUser(documents.get(0)));
    }

    /**
     * 根据用户名查找用户
     */
    public Optional<User> findByUsername(String username) throws ExecutionException, InterruptedException {
        CollectionReference usersRef = firestore.collection(COLLECTION_USERS);
        Query query = usersRef.whereEqualTo("username", username).limit(1);

        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        if (documents.isEmpty()) {
            return Optional.empty();
        }

        log.debug("通过用户名查找用户: username={}", username);
        return Optional.of(convertDocumentToUser(documents.get(0)));
    }

    /**
     * 更新用户偏好
     */
    public void updatePreferences(String userId, List<String> preferences) 
            throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_USERS).document(userId);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("preferences", preferences);
        
        ApiFuture<WriteResult> result = docRef.update(updates);
        result.get();
        
        log.info("更新用户偏好: userId={}", userId);
    }

    /**
     * 更新最后登录时间
     */
    public void updateLastLoginAt(String userId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_USERS).document(userId);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastLoginAt", Instant.now());
        
        ApiFuture<WriteResult> result = docRef.update(updates);
        result.get();
        
        log.debug("更新最后登录时间: userId={}", userId);
    }

    /**
     * 删除用户
     */
    public void delete(String userId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_USERS).document(userId);
        
        ApiFuture<WriteResult> result = docRef.delete();
        result.get();
        
        log.info("删除用户: userId={}", userId);
    }

    /**
     * 检查邮箱是否已存在
     */
    public boolean existsByEmail(String email) throws ExecutionException, InterruptedException {
        return findByEmail(email).isPresent();
    }

    /**
     * 检查用户名是否已存在
     */
    public boolean existsByUsername(String username) throws ExecutionException, InterruptedException {
        return findByUsername(username).isPresent();
    }

    // ========== 辅助转换方法 ==========

    private Map<String, Object> convertUserToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("passwordHash", user.getPasswordHash());
        map.put("preferences", user.getPreferences());
        map.put("defaultCurrency", user.getDefaultCurrency());
        map.put("createdAt", user.getCreatedAt());
        map.put("lastLoginAt", user.getLastLoginAt());
        
        return map;
    }

    @SuppressWarnings("unchecked")
    private User convertDocumentToUser(DocumentSnapshot doc) {
        return User.builder()
                .id(doc.getString("id"))
                .username(doc.getString("username"))
                .email(doc.getString("email"))
                .passwordHash(doc.getString("passwordHash"))
                .preferences((List<String>) doc.get("preferences"))
                .defaultCurrency(doc.getString("defaultCurrency"))
                .createdAt(doc.getDate("createdAt") != null ? doc.getDate("createdAt").toInstant() : null)
                .lastLoginAt(doc.getDate("lastLoginAt") != null ? doc.getDate("lastLoginAt").toInstant() : null)
                .build();
    }
}
