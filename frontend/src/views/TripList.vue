<template>
  <div class="trip-list-container">
    <!-- Toast 通知组件 -->
    <Toast ref="toastRef" />

    <!-- 顶部导航栏 -->
    <div class="list-header">
      <button @click="goBack" class="back-button">
        <span class="back-icon">←</span>
        <span>返回</span>
      </button>
      <h1 class="page-title">🗺️ 我的行程</h1>
      <button @click="goToCreateTrip" class="create-button">
        <span class="create-icon">+</span>
        <span>创建新行程</span>
      </button>
    </div>

    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading-container">
      <div class="loading-spinner"></div>
      <p class="loading-text">加载中...</p>
    </div>

    <!-- 行程列表 -->
    <div v-else-if="trips.length > 0" class="trips-content">
      <div class="trips-grid">
        <div
          v-for="trip in trips"
          :key="trip.id"
          class="trip-card"
          @click="goToTripDetail(trip.id)"
        >
          <!-- 渐变背景装饰 -->
          <div class="card-gradient"></div>
          
          <!-- 卡片内容 -->
          <div class="card-header">
            <h3 class="trip-title">{{ trip.title }}</h3>
            <div class="trip-destination">
              <span class="destination-icon">📍</span>
              <span>{{ trip.destination }}</span>
            </div>
          </div>

          <div class="card-body">
            <!-- 日期信息 -->
            <div class="info-row">
              <div class="info-item">
                <span class="info-icon">📅</span>
                <span class="info-label">出发日期</span>
                <span class="info-value">{{ formatDate(trip.startDate) }}</span>
              </div>
              <div class="info-item">
                <span class="info-icon">🏁</span>
                <span class="info-label">返回日期</span>
                <span class="info-value">{{ formatDate(trip.endDate) }}</span>
              </div>
            </div>

            <!-- 行程天数 -->
            <div class="info-row">
              <div class="info-item">
                <span class="info-icon">⏱️</span>
                <span class="info-label">行程天数</span>
                <span class="info-value">{{ calculateDays(trip.startDate, trip.endDate) }} 天</span>
              </div>
              <div class="info-item">
                <span class="info-icon">👥</span>
                <span class="info-label">出行人数</span>
                <span class="info-value">{{ trip.headcount.adults }} 大 {{ trip.headcount.children }} 小</span>
              </div>
            </div>

            <!-- 预算信息 -->
            <div class="budget-bar">
              <div class="budget-label">
                <span class="budget-icon">💰</span>
                <span>预算</span>
              </div>
              <div class="budget-amount">¥{{ (trip.totalBudget / 100).toFixed(0) }}</div>
            </div>
          </div>

          <!-- 卡片底部 -->
          <div class="card-footer">
            <span class="created-time">创建于 {{ formatDateTime(trip.createdAt) }}</span>
            <div class="footer-actions">
              <button 
                @click.stop="showDeleteConfirmation(trip)"
                class="delete-btn"
                title="删除行程"
              >
                <span class="delete-icon">🗑️</span>
              </button>
              <span class="view-arrow">→</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <div class="empty-illustration">
        <div class="empty-circle">
          <span class="empty-icon">✈️</span>
        </div>
        <div class="empty-waves">
          <div class="wave wave-1"></div>
          <div class="wave wave-2"></div>
          <div class="wave wave-3"></div>
        </div>
      </div>
      <h2 class="empty-title">还没有任何行程</h2>
      <p class="empty-subtitle">开始创建您的第一个旅行计划吧！</p>
      <button @click="goToCreateTrip" class="empty-action-button">
        <span class="button-icon">✨</span>
        <span>创建新行程</span>
      </button>
    </div>

    <!-- 确认删除弹窗 -->
    <transition name="modal-fade">
      <div v-if="showDeleteModal" class="modal-overlay" @click.self="showDeleteModal = false">
        <div class="modal-container">
          <div class="modal-header">
            <h2>⚠️ 确认删除</h2>
            <button @click="showDeleteModal = false" class="modal-close">✕</button>
          </div>
          <div class="modal-body">
            <p class="modal-warning">您确定要删除这个行程吗？</p>
            <p class="modal-trip-name">{{ tripToDelete?.title }}</p>
            <p class="modal-hint">此操作无法撤销，删除后将无法恢复该行程的所有信息。</p>
          </div>
          <div class="modal-footer">
            <button @click="showDeleteModal = false" class="modal-button cancel">取消</button>
            <button @click="confirmDelete" class="modal-button delete" :disabled="isDeleting">
              {{ isDeleting ? '删除中...' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import apiClient from '@/api/auth'
import Toast from '@/components/Toast.vue'

export default {
  name: 'TripListView',
  components: {
    Toast
  },
  setup() {
    const router = useRouter()
    const toastRef = ref(null)
    const trips = ref([])
    const isLoading = ref(true)
    const showDeleteModal = ref(false)
    const tripToDelete = ref(null)
    const isDeleting = ref(false)

    // 显示Toast通知
    const showToast = (message, type = 'success') => {
      if (toastRef.value) {
        toastRef.value.show(message, type)
      }
    }

    // 返回首页
    const goBack = () => {
      router.push('/home')
    }

    // 跳转到创建行程
    const goToCreateTrip = () => {
      router.push('/create-trip')
    }

    // 跳转到行程详情
    const goToTripDetail = (tripId) => {
      router.push({
        name: 'TripDetail',
        params: { tripId },
        state: {
          fromPage: 'list'  // 标记来源页面
        }
      })
    }

    // 格式化日期（YYYY-MM-DD）
    const formatDate = (dateString) => {
      if (!dateString) return ''
      const date = new Date(dateString)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    }

    // 格式化日期时间（用于显示创建时间）
    const formatDateTime = (dateString) => {
      if (!dateString) return ''
      const date = new Date(dateString)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}/${month}/${day}`
    }

    // 计算行程天数
    const calculateDays = (startDate, endDate) => {
      if (!startDate || !endDate) return 0
      const start = new Date(startDate)
      const end = new Date(endDate)
      const diffTime = Math.abs(end - start)
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
      return diffDays + 1 // 包含首尾两天
    }

    // 获取行程列表
    const fetchTrips = async () => {
      try {
        isLoading.value = true
        const response = await apiClient.get('/trips')
        
        console.log('行程列表响应:', response)

        if (response.success) {
          trips.value = response.data || []
        } else {
          showToast(response.message || '获取行程列表失败', 'error')
        }
      } catch (error) {
        console.error('获取行程列表失败:', error)
        showToast('获取行程列表失败，请稍后重试', 'error')
      } finally {
        isLoading.value = false
      }
    }

    // 显示删除确认弹窗
    const showDeleteConfirmation = (trip) => {
      tripToDelete.value = trip
      showDeleteModal.value = true
    }

    // 确认删除行程
    const confirmDelete = async () => {
      if (!tripToDelete.value) return

      try {
        isDeleting.value = true
        const response = await apiClient.delete(`/trips/${tripToDelete.value.id}`)
        
        console.log('删除行程响应:', response)

        if (response.success) {
          // 从列表中移除该行程
          trips.value = trips.value.filter(trip => trip.id !== tripToDelete.value.id)
          showToast('行程删除成功', 'success')
          showDeleteModal.value = false
          tripToDelete.value = null
        } else {
          showToast(response.message || '删除失败', 'error')
        }
      } catch (error) {
        console.error('删除行程失败:', error)
        showToast(error.response?.data?.message || '删除失败，请稍后重试', 'error')
      } finally {
        isDeleting.value = false
      }
    }

    onMounted(() => {
      fetchTrips()
    })

    return {
      trips,
      isLoading,
      toastRef,
      showDeleteModal,
      tripToDelete,
      isDeleting,
      goBack,
      goToCreateTrip,
      goToTripDetail,
      formatDate,
      formatDateTime,
      calculateDays,
      showDeleteConfirmation,
      confirmDelete
    }
  }
}
</script>

<style scoped>
.trip-list-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 2vw;
}

/* 顶部导航栏 */
.list-header {
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

.back-button,
.create-button {
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

.back-button:hover,
.create-button:hover {
  background: rgba(255, 255, 255, 0.25);
  border-color: white;
  transform: translateY(-0.1vw);
  box-shadow: 0 0.3vw 0.8vw rgba(0, 0, 0, 0.15);
}

.back-icon,
.create-icon {
  font-size: 1.25vw;
}

/* 加载状态 */
.loading-container {
  max-width: 90vw;
  margin: 5vw auto;
  text-align: center;
}

.loading-spinner {
  width: 4vw;
  height: 4vw;
  margin: 0 auto 1vw;
  border: 0.4vw solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  color: white;
  font-size: 1.2vw;
  font-weight: 600;
}

/* 行程列表网格 */
.trips-content {
  max-width: 90vw;
  margin: 2vw auto;
}

.trips-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(28vw, 1fr));
  gap: 2vw;
}

/* 行程卡片 */
.trip-card {
  position: relative;
  background: white;
  border-radius: 1.5vw;
  padding: 2vw;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 0.5vw 2vw rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.trip-card:hover {
  transform: translateY(-0.8vw) scale(1.02);
  box-shadow: 0 1.5vw 4vw rgba(0, 0, 0, 0.25);
}

/* 渐变背景装饰 */
.card-gradient {
  position: absolute;
  top: 0;
  right: 0;
  width: 12vw;
  height: 12vw;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  border-radius: 0 0 0 100%;
  pointer-events: none;
}

/* 卡片头部 */
.card-header {
  position: relative;
  margin-bottom: 1.5vw;
}

.trip-title {
  font-size: 1.4vw;
  font-weight: 700;
  color: #333;
  margin: 0 0 0.8vw 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.trip-destination {
  display: flex;
  align-items: center;
  gap: 0.5vw;
  font-size: 1.1vw;
  color: #667eea;
  font-weight: 600;
}

.destination-icon {
  font-size: 1.2vw;
}

/* 卡片主体 */
.card-body {
  position: relative;
}

.info-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1vw;
  margin-bottom: 1vw;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 0.3vw;
  padding: 0.8vw;
  background: rgba(102, 126, 234, 0.05);
  border-radius: 0.8vw;
  border-left: 0.25vw solid #667eea;
}

.info-icon {
  font-size: 1.2vw;
}

.info-label {
  font-size: 0.8vw;
  color: #999;
  font-weight: 500;
}

.info-value {
  font-size: 1vw;
  color: #333;
  font-weight: 700;
}

/* 预算条 */
.budget-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1vw;
  background: linear-gradient(135deg, rgba(67, 233, 123, 0.1) 0%, rgba(56, 249, 215, 0.1) 100%);
  border-radius: 0.8vw;
  border: 0.15vw solid rgba(67, 233, 123, 0.3);
  margin-top: 1.2vw;
}

.budget-label {
  display: flex;
  align-items: center;
  gap: 0.5vw;
  font-size: 1vw;
  color: #666;
  font-weight: 600;
}

.budget-icon {
  font-size: 1.3vw;
}

.budget-amount {
  font-size: 1.3vw;
  font-weight: 700;
  color: #43e97b;
}

/* 卡片底部 */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1.5vw;
  padding-top: 1vw;
  border-top: 0.1vw solid #e0e0e0;
}

.created-time {
  font-size: 0.85vw;
  color: #999;
}

.footer-actions {
  display: flex;
  align-items: center;
  gap: 1vw;
}

.delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.2vw;
  height: 2.2vw;
  padding: 0;
  background: rgba(231, 76, 60, 0.1);
  border: 0.1vw solid rgba(231, 76, 60, 0.3);
  border-radius: 0.5vw;
  cursor: pointer;
  transition: all 0.3s ease;
}

.delete-btn:hover {
  background: rgba(231, 76, 60, 0.2);
  border-color: rgba(231, 76, 60, 0.5);
  transform: scale(1.1);
}

.delete-icon {
  font-size: 1.1vw;
}

.view-arrow {
  font-size: 1.5vw;
  color: #667eea;
  font-weight: bold;
  transition: transform 0.3s ease;
}

.trip-card:hover .view-arrow {
  transform: translateX(0.5vw);
}

/* 空状态 */
.empty-state {
  max-width: 50vw;
  margin: 5vw auto;
  text-align: center;
  position: relative;
}

/* 空状态插图 */
.empty-illustration {
  position: relative;
  width: 15vw;
  height: 15vw;
  margin: 0 auto 2vw;
}

.empty-circle {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 10vw;
  height: 10vw;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1vw 3vw rgba(0, 0, 0, 0.2);
  z-index: 2;
  animation: float-slow 3s ease-in-out infinite;
}

@keyframes float-slow {
  0%, 100% {
    transform: translate(-50%, -50%) translateY(0);
  }
  50% {
    transform: translate(-50%, -50%) translateY(-1vw);
  }
}

.empty-icon {
  font-size: 5vw;
}

/* 波浪效果 */
.empty-waves {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  height: 100%;
}

.wave {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border: 0.2vw solid rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  animation: wave-expand 3s ease-out infinite;
}

.wave-1 {
  width: 10vw;
  height: 10vw;
  animation-delay: 0s;
}

.wave-2 {
  width: 10vw;
  height: 10vw;
  animation-delay: 1s;
}

.wave-3 {
  width: 10vw;
  height: 10vw;
  animation-delay: 2s;
}

@keyframes wave-expand {
  0% {
    width: 10vw;
    height: 10vw;
    opacity: 1;
  }
  100% {
    width: 15vw;
    height: 15vw;
    opacity: 0;
  }
}

.empty-title {
  color: white;
  font-size: 2vw;
  font-weight: 700;
  margin: 0 0 0.8vw 0;
  text-shadow: 0 0.15vw 0.5vw rgba(0, 0, 0, 0.2);
}

.empty-subtitle {
  color: rgba(255, 255, 255, 0.9);
  font-size: 1.2vw;
  margin: 0 0 2vw 0;
  font-weight: 500;
}

.empty-action-button {
  display: inline-flex;
  align-items: center;
  gap: 0.8vw;
  padding: 1.2vw 3vw;
  background: white;
  color: #667eea;
  border: none;
  border-radius: 3vw;
  font-size: 1.2vw;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 0.5vw 2vw rgba(0, 0, 0, 0.2);
}

.empty-action-button:hover {
  transform: translateY(-0.3vw) scale(1.05);
  box-shadow: 0 1vw 3vw rgba(0, 0, 0, 0.3);
}

.empty-action-button:active {
  transform: translateY(0) scale(1);
}

.button-icon {
  font-size: 1.5vw;
}

/* 删除确认弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  backdrop-filter: blur(5px);
}

.modal-container {
  background: white;
  border-radius: 1.5vw;
  width: 35vw;
  max-width: 90vw;
  box-shadow: 0 1.5vw 4vw rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5vw 2vw;
  background: linear-gradient(135deg, rgba(231, 76, 60, 0.1) 0%, rgba(192, 57, 43, 0.1) 100%);
  border-bottom: 0.15vw solid rgba(231, 76, 60, 0.2);
}

.modal-header h2 {
  margin: 0;
  font-size: 1.5vw;
  color: #e74c3c;
  font-weight: 700;
}

.modal-close {
  width: 2vw;
  height: 2vw;
  border: none;
  background: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
  font-size: 1.2vw;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.modal-close:hover {
  background: rgba(231, 76, 60, 0.2);
  transform: rotate(90deg);
}

.modal-body {
  padding: 2vw;
  text-align: center;
}

.modal-warning {
  font-size: 1.2vw;
  color: #333;
  margin: 0 0 1vw 0;
  font-weight: 600;
}

.modal-trip-name {
  font-size: 1.4vw;
  color: #667eea;
  margin: 0 0 1.5vw 0;
  font-weight: 700;
  padding: 1vw;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 0.8vw;
}

.modal-hint {
  font-size: 0.9vw;
  color: #999;
  margin: 0;
  line-height: 1.6;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1vw;
  padding: 1.5vw 2vw;
  background: #f8f8f8;
  border-top: 0.1vw solid #e0e0e0;
}

.modal-button {
  padding: 0.8vw 2vw;
  border: none;
  border-radius: 0.8vw;
  font-size: 1vw;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.modal-button.cancel {
  background: white;
  color: #666;
  border: 0.1vw solid #ddd;
}

.modal-button.cancel:hover {
  background: #f5f5f5;
  border-color: #999;
}

.modal-button.delete {
  background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
  color: white;
}

.modal-button.delete:hover {
  background: linear-gradient(135deg, #c0392b 0%, #e74c3c 100%);
  transform: translateY(-0.1vw);
  box-shadow: 0 0.3vw 0.8vw rgba(231, 76, 60, 0.3);
}

.modal-button.delete:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 弹窗动画 */
.modal-fade-enter-active {
  animation: modal-fade-in 0.3s ease;
}

.modal-fade-leave-active {
  animation: modal-fade-out 0.3s ease;
}

@keyframes modal-fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes modal-fade-out {
  from {
    opacity: 1;
  }
  to {
    opacity: 0;
  }
}

.modal-fade-enter-active .modal-container {
  animation: modal-slide-in 0.3s ease;
}

.modal-fade-leave-active .modal-container {
  animation: modal-slide-out 0.3s ease;
}

@keyframes modal-slide-in {
  from {
    transform: translateY(-3vw);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@keyframes modal-slide-out {
  from {
    transform: translateY(0);
    opacity: 1;
  }
  to {
    transform: translateY(-3vw);
    opacity: 0;
  }
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .trips-grid {
    grid-template-columns: repeat(auto-fill, minmax(40vw, 1fr));
  }
}
</style>

