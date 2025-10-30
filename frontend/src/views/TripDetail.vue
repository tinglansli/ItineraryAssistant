<template>
  <div class="trip-detail-container">
    <!-- 顶部导航栏 -->
    <div class="detail-header">
      <button @click="goBack" class="back-button">
        <span class="back-icon">←</span>
        <span>返回</span>
      </button>
      <h1 class="page-title">🗺️ 行程详情</h1>
      <div class="header-spacer"></div>
    </div>

    <!-- 主内容区 -->
    <div class="detail-content">
      <div class="detail-card">
        <h2>行程详情页面</h2>
        <p>这是行程详情页面，将在后续开发中完善</p>
        <pre>{{ tripData }}</pre>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

export default {
  name: 'TripDetailView',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const tripData = ref(null)

    const goBack = () => {
      router.push('/home')
    }

    onMounted(() => {
      // 从路由状态获取行程数据
      tripData.value = history.state.tripData || route.params
    })

    return {
      tripData,
      goBack
    }
  }
}
</script>

<style scoped>
.trip-detail-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 2vw;
}

.detail-header {
  max-width: 90vw;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5vw 0;
}

.page-title {
  color: white;
  font-size: 2.5vw;
  font-weight: 700;
  margin: 0;
  text-shadow: 0 0.15vw 0.5vw rgba(0, 0, 0, 0.2);
}

.back-button {
  display: flex;
  align-items: center;
  gap: 0.5vw;
  padding: 0.75vw 1.5vw;
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

.back-button:hover {
  background: rgba(255, 255, 255, 0.25);
  border-color: white;
  transform: translateY(-0.1vw);
  box-shadow: 0 0.3vw 0.8vw rgba(0, 0, 0, 0.15);
}

.back-icon {
  font-size: 1.25vw;
}

.header-spacer {
  width: 8vw;
}

.detail-content {
  max-width: 80vw;
  margin: 2vw auto;
}

.detail-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 1.5vw;
  padding: 2vw;
  box-shadow: 0 0.75vw 2vw rgba(0, 0, 0, 0.15);
}

.detail-card h2 {
  font-size: 1.75vw;
  color: #333;
  margin: 0 0 1vw 0;
}

.detail-card p {
  font-size: 1vw;
  color: #666;
  margin: 0 0 1vw 0;
}

.detail-card pre {
  background: #f5f5f5;
  padding: 1vw;
  border-radius: 0.5vw;
  overflow-x: auto;
  font-size: 0.85vw;
}
</style>
