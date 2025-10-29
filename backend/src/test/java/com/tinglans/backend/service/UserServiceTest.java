package com.tinglans.backend.service;

import com.tinglans.backend.common.BusinessException;
import com.tinglans.backend.domain.User;
import com.tinglans.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private String testUserId;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUserId = "user-123";

        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .preferences(Arrays.asList("美食", "历史文化", "寺庙"))
                .defaultCurrency("CNY")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void testGetUserById_success() throws ExecutionException, InterruptedException {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUserById(testUserId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUserId, result.get().getId());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("test@example.com", result.get().getEmail());

        verify(userRepository, times(1)).findById(testUserId);
    }

    @Test
    void testGetUserById_notFound() throws ExecutionException, InterruptedException {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUserById(testUserId);

        // Then
        assertFalse(result.isPresent());

        verify(userRepository, times(1)).findById(testUserId);
    }

    @Test
    void testGetUserById_repositoryThrowsException() throws ExecutionException, InterruptedException {
        // Given
        when(userRepository.findById(testUserId))
                .thenThrow(new ExecutionException(new RuntimeException("Database error")));

        // When & Then
        assertThrows(ExecutionException.class, () -> {
            userService.getUserById(testUserId);
        });
    }

    @Test
    void testUpdatePreferences_success() throws ExecutionException, InterruptedException {
        // Given
        String preferencesStr = "购物;美食;温泉";
        List<String> expectedPreferences = Arrays.asList("购物", "美食", "温泉");

        doNothing().when(userRepository).updatePreferences(eq(testUserId), anyList());

        // When
        userService.updatePreferences(testUserId, preferencesStr);

        // Then
        verify(userRepository, times(1)).updatePreferences(eq(testUserId), eq(expectedPreferences));
    }

    @Test
    void testUpdatePreferences_withEmptyString() throws ExecutionException, InterruptedException {
        // Given
        String preferencesStr = "";

        doNothing().when(userRepository).updatePreferences(eq(testUserId), anyList());

        // When
        userService.updatePreferences(testUserId, preferencesStr);

        // Then
        verify(userRepository, times(1)).updatePreferences(eq(testUserId), eq(new ArrayList<>()));
    }

    @Test
    void testUpdatePreferences_withNullString() throws ExecutionException, InterruptedException {
        // Given
        String preferencesStr = null;

        doNothing().when(userRepository).updatePreferences(eq(testUserId), anyList());

        // When
        userService.updatePreferences(testUserId, preferencesStr);

        // Then
        verify(userRepository, times(1)).updatePreferences(eq(testUserId), eq(new ArrayList<>()));
    }

    @Test
    void testUpdatePreferences_withWhitespace() throws ExecutionException, InterruptedException {
        // Given
        String preferencesStr = "  美食  ;  购物  ;  温泉  ";
        List<String> expectedPreferences = Arrays.asList("美食", "购物", "温泉");

        doNothing().when(userRepository).updatePreferences(eq(testUserId), anyList());

        // When
        userService.updatePreferences(testUserId, preferencesStr);

        // Then
        verify(userRepository, times(1)).updatePreferences(eq(testUserId), eq(expectedPreferences));
    }

    @Test
    void testUpdatePreferences_withDuplicates() throws ExecutionException, InterruptedException {
        // Given
        String preferencesStr = "美食;购物;美食;温泉;购物";
        
        doNothing().when(userRepository).updatePreferences(eq(testUserId), anyList());

        // When
        userService.updatePreferences(testUserId, preferencesStr);

        // Then
        verify(userRepository, times(1)).updatePreferences(eq(testUserId), argThat(list -> {
            // 验证去重后只有3个不同的偏好
            return list.size() == 3 && 
                   list.contains("美食") && 
                   list.contains("购物") && 
                   list.contains("温泉");
        }));
    }

    @Test
    void testUpdatePreferences_withEmptySegments() throws ExecutionException, InterruptedException {
        // Given - 包含空分段
        String preferencesStr = "美食;;购物;;;温泉";
        List<String> expectedPreferences = Arrays.asList("美食", "购物", "温泉");

        doNothing().when(userRepository).updatePreferences(eq(testUserId), anyList());

        // When
        userService.updatePreferences(testUserId, preferencesStr);

        // Then
        verify(userRepository, times(1)).updatePreferences(eq(testUserId), eq(expectedPreferences));
    }

    @Test
    void testUpdatePreferences_singlePreference() throws ExecutionException, InterruptedException {
        // Given
        String preferencesStr = "美食";
        List<String> expectedPreferences = Collections.singletonList("美食");

        doNothing().when(userRepository).updatePreferences(eq(testUserId), anyList());

        // When
        userService.updatePreferences(testUserId, preferencesStr);

        // Then
        verify(userRepository, times(1)).updatePreferences(eq(testUserId), eq(expectedPreferences));
    }

    @Test
    void testUpdatePreferences_repositoryThrowsException() throws ExecutionException, InterruptedException {
        // Given
        String preferencesStr = "美食;购物";
        doThrow(new RuntimeException("Database error"))
                .when(userRepository).updatePreferences(anyString(), anyList());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            userService.updatePreferences(testUserId, preferencesStr);
        });
    }

    @Test
    void testGetPreferences_success() throws ExecutionException, InterruptedException {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        String result = userService.getPreferences(testUserId);

        // Then
        assertNotNull(result);
        assertEquals("美食;历史文化;寺庙", result);

        verify(userRepository, times(1)).findById(testUserId);
    }

    @Test
    void testGetPreferences_userNotFound() throws ExecutionException, InterruptedException {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.getPreferences(testUserId);
        });

        assertTrue(exception.getMessage().contains("用户不存在"));

        verify(userRepository, times(1)).findById(testUserId);
    }

    @Test
    void testGetPreferences_emptyPreferences() throws ExecutionException, InterruptedException {
        // Given
        User userWithEmptyPreferences = User.builder()
                .id(testUserId)
                .username("testuser")
                .preferences(new ArrayList<>())
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(userWithEmptyPreferences));

        // When
        String result = userService.getPreferences(testUserId);

        // Then
        assertEquals("", result);
    }

    @Test
    void testGetPreferences_nullPreferences() throws ExecutionException, InterruptedException {
        // Given
        User userWithNullPreferences = User.builder()
                .id(testUserId)
                .username("testuser")
                .preferences(null)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(userWithNullPreferences));

        // When
        String result = userService.getPreferences(testUserId);

        // Then
        assertEquals("", result);
    }

    @Test
    void testGetPreferences_singlePreference() throws ExecutionException, InterruptedException {
        // Given
        User userWithSinglePreference = User.builder()
                .id(testUserId)
                .username("testuser")
                .preferences(Collections.singletonList("美食"))
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(userWithSinglePreference));

        // When
        String result = userService.getPreferences(testUserId);

        // Then
        assertEquals("美食", result);
    }

    @Test
    void testGetPreferencesList_success() throws ExecutionException, InterruptedException {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        List<String> result = userService.getPreferencesList(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("美食"));
        assertTrue(result.contains("历史文化"));
        assertTrue(result.contains("寺庙"));

        verify(userRepository, times(1)).findById(testUserId);
    }

    @Test
    void testGetPreferencesList_userNotFound() throws ExecutionException, InterruptedException {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When
        List<String> result = userService.getPreferencesList(testUserId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPreferencesList_nullPreferences() throws ExecutionException, InterruptedException {
        // Given
        User userWithNullPreferences = User.builder()
                .id(testUserId)
                .preferences(null)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(userWithNullPreferences));

        // When
        List<String> result = userService.getPreferencesList(testUserId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPreferencesList_emptyPreferences() throws ExecutionException, InterruptedException {
        // Given
        User userWithEmptyPreferences = User.builder()
                .id(testUserId)
                .preferences(new ArrayList<>())
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(userWithEmptyPreferences));

        // When
        List<String> result = userService.getPreferencesList(testUserId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testParsePreferencesString_variousFormats() throws ExecutionException, InterruptedException {
        // 通过updatePreferences方法间接测试parsePreferencesString
        
        // Test case 1: 标准格式
        doNothing().when(userRepository).updatePreferences(anyString(), anyList());
        userService.updatePreferences(testUserId, "美食;购物;温泉");
        verify(userRepository).updatePreferences(eq(testUserId), eq(Arrays.asList("美食", "购物", "温泉")));

        // Test case 2: 带空格
        reset(userRepository);
        doNothing().when(userRepository).updatePreferences(anyString(), anyList());
        userService.updatePreferences(testUserId, " 美食 ; 购物 ; 温泉 ");
        verify(userRepository).updatePreferences(eq(testUserId), eq(Arrays.asList("美食", "购物", "温泉")));

        // Test case 3: 空字符串
        reset(userRepository);
        doNothing().when(userRepository).updatePreferences(anyString(), anyList());
        userService.updatePreferences(testUserId, "   ");
        verify(userRepository).updatePreferences(eq(testUserId), eq(new ArrayList<>()));
    }

    @Test
    void testCompleteUserWorkflow() throws ExecutionException, InterruptedException {
        // 完整的用户偏好更新和查询流程
        
        // 1. 更新偏好
        String newPreferences = "动漫;科技;摄影";
        doNothing().when(userRepository).updatePreferences(eq(testUserId), anyList());
        userService.updatePreferences(testUserId, newPreferences);

        // 2. 更新后的用户对象
        User updatedUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .preferences(Arrays.asList("动漫", "科技", "摄影"))
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(updatedUser));

        // 3. 查询偏好
        String result = userService.getPreferences(testUserId);
        assertEquals("动漫;科技;摄影", result);

        // 4. 获取偏好列表
        List<String> preferencesList = userService.getPreferencesList(testUserId);
        assertEquals(3, preferencesList.size());
        assertTrue(preferencesList.contains("动漫"));
        assertTrue(preferencesList.contains("科技"));
        assertTrue(preferencesList.contains("摄影"));
    }
}
