<template>
  <div class="home-container">
    <!-- 顶部导航 -->
    <div class="home-header">
      <h1 class="home-title">🤖 AI行程助手</h1>
      <button @click="handleLogout" class="logout-button">退出登录</button>
    </div>

    <!-- 主内容区域 -->
    <div class="home-content">
      <!-- 欢迎卡片 -->
      <div class="welcome-card">
        <div class="welcome-icon">✈️</div>
        <h2>欢迎回来！</h2>
        <p v-if="userInfo" class="user-greeting">{{ userInfo.username }}</p>
        <p class="welcome-subtitle">让我们开始策划您的下一段精彩旅程</p>
      </div>

      <!-- 操作按钮区域 -->
      <div class="action-buttons">
        <div class="action-card create-trip">
          <div class="action-icon">📝</div>
          <h3>创建行程</h3>
          <p>开启您的旅行冒险</p>
          <button class="action-button" @click="goToCreateTrip">开始规划</button>
        </div>

        <div class="action-card view-trip">
          <div class="action-icon">📅</div>
          <h3>查看行程</h3>
          <p>查看您的旅行记录</p>
          <button class="action-button" @click="goToTripList">查看详情</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentUser } from '@/api/auth'
import { logout } from '@/router' // 导入路由的logout函数

export default {
  name: 'HomeView',
  setup() {
    const router = useRouter()
    const userInfo = ref(null)

    // 退出登录 - 使用路由提供的logout函数,清除缓存和token验证
    const handleLogout = () => {
      logout()
    }

    // 跳转到创建行程页面
    const goToCreateTrip = () => {
      router.push('/create-trip')
    }

    // 跳转到查看行程列表
    const goToTripList = () => {
      router.push('/trips')
    }

    // 获取用户信息
    const fetchUserInfo = async () => {
      try {
        const response = await getCurrentUser()
        if (response.success) {
          userInfo.value = response.data
        }
      } catch (error) {
        console.error('获取用户信息失败:', error)
      }
    }

    onMounted(() => {
      fetchUserInfo()
    })

    return {
      userInfo,
      handleLogout,
      goToCreateTrip,
      goToTripList
    }
  }
}
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 2vw;
}

/* 顶部导航 */
.home-header {
  max-width: 90vw;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5vw 0;
}

.home-title {
  color: white;
  font-size: 2.5vw;
  font-weight: 700;
  margin: 0;
  text-shadow: 0 0.15vw 0.5vw rgba(0, 0, 0, 0.2);
}

.logout-button {
  padding: 0.75vw 1.875vw;
  background: rgba(255, 255, 255, 0.15);
  color: white;
  border: 0.125vw solid rgba(255, 255, 255, 0.4);
  border-radius: 1.25vw;
  font-size: 1vw;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.logout-button:hover {
  background: rgba(255, 255, 255, 0.25);
  border-color: white;
  transform: translateY(-0.1vw);
  box-shadow: 0 0.3vw 0.8vw rgba(0, 0, 0, 0.15);
}

/* 主内容区域 */
.home-content {
  max-width: 80vw;
  margin: 2vw auto;
}

/* 欢迎卡片 */
.welcome-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 1.5vw;
  padding: 2vw;
  margin-bottom: 2vw;
  box-shadow: 0 0.75vw 2.25vw rgba(0, 0, 0, 0.15);
  text-align: center;
}

.welcome-icon {
  font-size: 3.5vw;
  margin-bottom: 0.5vw;
  display: inline-block;
  animation: float-large 4s ease-in-out infinite;
}

@keyframes float-large {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-1vw);
  }
}

.welcome-card h2 {
  font-size: 2vw;
  color: #333;
  margin: 0.3vw 0 0 0;
  font-weight: 800;
}

.user-greeting {
  font-size: 1.25vw;
  color: #667eea;
  font-weight: 700;
  margin: 0.3vw 0 0 0;
}

.welcome-subtitle {
  font-size: 1vw;
  color: #999;
  margin: 0.5vw 0 0 0;
}

/* 操作按钮区域 */
.action-buttons {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.5vw;
}

/* 操作卡片 */
.action-card {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 1.25vw;
  padding: 2vw;
  text-align: center;
  transition: all 0.4s ease;
  cursor: pointer;
  box-shadow: 0 0.5vw 1.5vw rgba(0, 0, 0, 0.12);
  border: 0.15vw solid rgba(255, 255, 255, 0.3);
}

.action-card:hover {
  transform: translateY(-0.5vw);
  box-shadow: 0 1.25vw 3.5vw rgba(0, 0, 0, 0.2);
}

.action-icon {
  font-size: 3.5vw;
  margin-bottom: 0.75vw;
  display: inline-block;
  animation: float 3s ease-in-out infinite;
}

.action-card:nth-child(2) .action-icon {
  animation-delay: 0.5s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-0.8vw);
  }
}

.action-card h3 {
  font-size: 1.5vw;
  color: #333;
  margin: 0.3vw 0 0.5vw 0;
  font-weight: 800;
}

.action-card p {
  font-size: 1vw;
  color: #999;
  margin: 0.3vw 0 1vw 0;
}

/* 操作按钮 */
.action-button {
  padding: 0.75vw 2vw;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 1vw;
  font-weight: 600;
  border: none;
  border-radius: 0.75vw;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-button:hover {
  transform: translateY(-0.15vw);
  box-shadow: 0 0.5vw 1.25vw rgba(102, 126, 234, 0.4);
}

.action-button:active {
  transform: translateY(0);
}

/* 渐变背景区分两个卡片 */
.create-trip {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(102, 126, 234, 0.05) 100%);
}

.view-trip {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(118, 75, 162, 0.05) 100%);
}
</style>