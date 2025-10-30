<template>
  <div class="auth-container">
    <!-- 背景装饰圆圈 -->
    <div class="bg-circles">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
      <div class="circle circle-4"></div>
    </div>

    <!-- 左侧内容区 -->
    <div class="auth-left-section">
      <div class="brand-section">
        <div class="brand-icon">🌍</div>
        <h1 class="brand-title">AI行程助手</h1>
        <p class="brand-slogan">智能规划 · 轻松出行</p>
      </div>

      <div class="feature-list">
        <div class="feature-item">
          <div class="feature-icon">🎯</div>
          <div class="feature-content">
            <h3>智能推荐</h3>
            <p>AI 助力定制专属行程</p>
          </div>
        </div>
        <div class="feature-item">
          <div class="feature-icon">💰</div>
          <div class="feature-content">
            <h3>预算管理</h3>
            <p>实时跟踪开支情况</p>
          </div>
        </div>
        <div class="feature-item">
          <div class="feature-icon">📊</div>
          <div class="feature-content">
            <h3>数据分析</h3>
            <p>可视化你的旅行数据</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧登录卡片 -->
    <div class="auth-right-section">
      <div class="auth-card">
        <transition name="fade" mode="out-in">
          <div :key="isLogin" class="auth-content">
            <!-- 顶部图标 -->
            <div class="auth-header-icon">✈️</div>
            
            <!-- 标题 -->
            <h1 class="auth-title">{{ isLogin ? '欢迎回来' : '创建账户' }}</h1>
            <p class="auth-subtitle">{{ isLogin ? '登录您的账户继续旅程' : '注册账户开启精彩旅程' }}</p>

            <!-- 用户名输入框 -->
            <div class="input-group">
              <div class="input-icon">👤</div>
              <input
                v-model="username"
                type="text"
                placeholder="请输入用户名"
                class="auth-input"
                @keyup.enter="handleSubmit"
              />
            </div>

            <!-- 密码输入框 -->
            <div class="input-group">
              <div class="input-icon">🔒</div>
              <input
                v-model="password"
                type="password"
                placeholder="请输入密码"
                class="auth-input"
                @keyup.enter="handleSubmit"
              />
            </div>

            <!-- 错误提示 -->
            <transition name="slide-fade">
              <div v-if="errorMessage" class="error-message">
                {{ errorMessage }}
              </div>
            </transition>

            <!-- 主按钮 -->
            <button
              class="auth-button"
              :disabled="isLoading"
              @click="handleSubmit"
            >
              <span v-if="!isLoading">{{ isLogin ? '立即登录' : '立即注册' }}</span>
              <span v-else class="loading-text">处理中...</span>
            </button>

            <!-- 切换模式提示 -->
            <div class="toggle-mode">
              <span class="toggle-text">
                {{ isLogin ? '还没有账号？' : '已有账号？' }}
                <a href="#" @click.prevent="toggleMode" class="toggle-link">
                  {{ isLogin ? '立即注册' : '立即登录' }}
                </a>
              </span>
            </div>
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login, register } from '@/api/auth'

export default {
  name: 'AuthView',
  setup() {
    const router = useRouter()
    
    // 响应式数据
    const isLogin = ref(true)
    const username = ref('')
    const password = ref('')
    const errorMessage = ref('')
    const isLoading = ref(false)

    // 切换登录/注册模式
    const toggleMode = () => {
      isLogin.value = !isLogin.value
      errorMessage.value = ''
      username.value = ''
      password.value = ''
    }

    // 处理提交
    const handleSubmit = async () => {
      // 清除错误提示
      errorMessage.value = ''

      // 验证输入
      if (!username.value.trim()) {
        errorMessage.value = '请输入用户名'
        return
      }

      if (!password.value.trim()) {
        errorMessage.value = '请输入密码'
        return
      }

      if (password.value.length < 6) {
        errorMessage.value = '密码长度至少为6位'
        return
      }

      isLoading.value = true

      try {
        if (isLogin.value) {
          // 登录逻辑
          const response = await login(username.value, password.value)
          
          if (response.success) {
            // 存储 Token
            localStorage.setItem('token', response.data.token)
            
            // 跳转到主页
            router.push('/home')
          } else {
            errorMessage.value = response.message || '登录失败'
          }
        } else {
          // 注册逻辑
          const response = await register(username.value, password.value)
          
          if (response.success) {
            // 注册成功，自动切换到登录模式
            errorMessage.value = ''
            isLogin.value = true
            password.value = ''
            
            // 显示成功提示（可选）
            alert('注册成功！请登录')
          } else {
            errorMessage.value = response.message || '注册失败'
          }
        }
      } catch (error) {
        console.error('请求失败:', error)
        
        if (error.response) {
          const { data } = error.response
          errorMessage.value = data.message || (isLogin.value ? '用户名或密码错误' : '注册失败，用户名可能已存在')
        } else if (error.request) {
          errorMessage.value = '网络连接失败，请检查后端服务是否启动'
        } else {
          errorMessage.value = '请求失败，请稍后重试'
        }
      } finally {
        isLoading.value = false
      }
    }

    return {
      isLogin,
      username,
      password,
      errorMessage,
      isLoading,
      toggleMode,
      handleSubmit
    }
  }
}
</script>

<style scoped>
/* 整体容器 - 满屏展示 */
.auth-container {
  min-height: 100vh;
  display: flex;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

/* 背景装饰圆圈 */
.bg-circles {
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  animation: float-circle 20s ease-in-out infinite;
}

.circle-1 {
  width: 30vw;
  height: 30vw;
  top: -10vw;
  left: -10vw;
}

.circle-2 {
  width: 20vw;
  height: 20vw;
  bottom: -5vw;
  right: 10vw;
  animation-delay: 2s;
}

.circle-3 {
  width: 25vw;
  height: 25vw;
  top: 40vh;
  left: 5vw;
  animation-delay: 4s;
}

.circle-4 {
  width: 15vw;
  height: 15vw;
  top: 10vh;
  right: 15vw;
  animation-delay: 6s;
}

@keyframes float-circle {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(2vw, 2vw) scale(1.1);
  }
}

/* 左侧内容区 */
.auth-left-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 4vw 6vw;
  z-index: 1;
}

/* 品牌区域 */
.brand-section {
  margin-bottom: 4vw;
}

.brand-icon {
  font-size: 5vw;
  margin-bottom: 1vw;
  animation: rotate-globe 10s linear infinite;
}

@keyframes rotate-globe {
  0%, 100% {
    transform: rotate(0deg) scale(1);
  }
  50% {
    transform: rotate(180deg) scale(1.1);
  }
}

.brand-title {
  font-size: 3.5vw;
  font-weight: 800;
  color: white;
  margin: 0 0 0.5vw 0;
  text-shadow: 0 0.3vw 1vw rgba(0, 0, 0, 0.2);
  letter-spacing: 0.15vw;
}

.brand-slogan {
  font-size: 1.25vw;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
  font-weight: 500;
  text-shadow: 0 0.15vw 0.5vw rgba(0, 0, 0, 0.1);
}

/* 功能列表 */
.feature-list {
  display: flex;
  flex-direction: column;
  gap: 1.5vw;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 1.25vw;
  padding: 1.25vw;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 1vw;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
  animation: slide-in-left 0.6s ease-out backwards;
}

.feature-item:nth-child(1) {
  animation-delay: 0.2s;
}

.feature-item:nth-child(2) {
  animation-delay: 0.4s;
}

.feature-item:nth-child(3) {
  animation-delay: 0.6s;
}

@keyframes slide-in-left {
  from {
    opacity: 0;
    transform: translateX(-3vw);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateX(0.5vw);
}

.feature-icon {
  font-size: 2.5vw;
  flex-shrink: 0;
}

.feature-content h3 {
  font-size: 1.25vw;
  color: white;
  margin: 0 0 0.25vw 0;
  font-weight: 700;
}

.feature-content p {
  font-size: 0.9vw;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

/* 右侧登录区 */
.auth-right-section {
  flex: 0 0 40vw;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 3vw;
  z-index: 1;
}

/* 半透明卡片窗口 */
.auth-card {
  width: 100%;
  max-width: 28vw;
  background: rgba(255, 255, 255, 0.75);
  border-radius: 1.5vw;
  padding: 2.5vw;
  box-shadow: 0 1.5vw 4vw rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  animation: fade-in 0.6s ease-out;
}

@keyframes fade-in {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 内容区域 */
.auth-content {
  width: 100%;
}

/* 顶部图标 */
.auth-header-icon {
  font-size: 3.5vw;
  text-align: center;
  margin-bottom: 1vw;
}

/* 标题 */
.auth-title {
  font-size: 2vw;
  font-weight: 800;
  color: #333;
  margin: 0 0 0.5vw 0;
  text-align: center;
}

/* 副标题 */
.auth-subtitle {
  font-size: 0.9vw;
  color: #999;
  margin: 0 0 2vw 0;
  text-align: center;
}

/* 输入框组 */
.input-group {
  position: relative;
  margin-bottom: 1.25vw;
}

/* 输入框图标 */
.input-icon {
  position: absolute;
  left: 1vw;
  top: 50%;
  transform: translateY(-50%);
  font-size: 1.25vw;
  opacity: 0.6;
}

/* 输入框 */
.auth-input {
  width: 100%;
  height: 3.125vw;
  padding: 0 1.125vw 0 3vw;
  font-size: 1vw;
  border: 0.125vw solid #e0e0e0;
  border-radius: 0.75vw;
  outline: none;
  transition: all 0.3s ease;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.8);
}

.auth-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 0.1875vw rgba(102, 126, 234, 0.1);
}

.auth-input::placeholder {
  color: #aaa;
}

/* 错误提示 */
.error-message {
  color: #e74c3c;
  font-size: 0.875vw;
  margin: -0.625vw 0 1.25vw 0;
  text-align: left;
  padding-left: 0.3125vw;
}

/* 主按钮 */
.auth-button {
  width: 100%;
  height: 3.125vw;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 1.125vw;
  font-weight: 600;
  border: none;
  border-radius: 0.75vw;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 0.625vw;
}

.auth-button:hover:not(:disabled) {
  transform: translateY(-0.125vw);
  box-shadow: 0 0.625vw 1.5625vw rgba(102, 126, 234, 0.4);
}

.auth-button:active:not(:disabled) {
  transform: translateY(0);
}

.auth-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-text {
  display: inline-block;
}

/* 切换模式提示 */
.toggle-mode {
  margin-top: 1.5625vw;
  text-align: center;
}

.toggle-text {
  font-size: 0.875vw;
  color: #666;
}

.toggle-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
  border-bottom: 1px solid transparent;
  transition: border-color 0.3s ease;
}

.toggle-link:hover {
  border-bottom-color: #667eea;
}

/* 淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 错误提示滑入动画 */
.slide-fade-enter-active {
  transition: all 0.3s ease;
}

.slide-fade-leave-active {
  transition: all 0.2s ease;
}

.slide-fade-enter-from {
  transform: translateY(-0.625vw);
  opacity: 0;
}

.slide-fade-leave-to {
  opacity: 0;
}
</style>
