<template>
  <div class="trip-detail-container">
    <!-- Toast 通知组件 -->
    <Toast ref="toastRef" />

    <!-- 顶部导航栏 -->
    <div class="detail-header">
      <button @click="goBack" class="back-button">
        <span class="back-icon">←</span>
        <span>返回</span>
      </button>
      <h1 class="page-title">🗺️ {{ tripData?.title || '行程详情' }}</h1>
      <div class="header-spacer"></div>
    </div>

    <!-- 主内容区 - 三列布局 -->
    <div class="detail-content">
      <!-- 左侧：天数选择 -->
      <div class="left-sidebar">
        <div class="sidebar-header">
          <h3 class="sidebar-title">🗓️ 行程天数</h3>
        </div>

        <!-- 天数选择器（可滑动） -->
        <div class="days-scroller">
          <!-- 全部 -->
          <button
            class="day-button all-days"
            :class="{ active: selectedDay === 'all' }"
            @click="selectedDay = 'all'"
          >
            <span class="day-label">全部</span>
            <span class="day-count">{{ tripData?.days?.length || 0 }}天</span>
          </button>

          <!-- 每一天 -->
          <button
            v-for="day in tripData?.days"
            :key="day.dayIndex"
            class="day-button"
            :class="{ active: selectedDay === day.dayIndex }"
            @click="selectedDay = day.dayIndex"
            :style="{ borderLeftColor: getDayColor(day.dayIndex) }"
          >
            <span class="day-label">第{{ day.dayIndex }}天</span>
            <span class="day-date">{{ formatDate(day.date) }}</span>
          </button>
        </div>

        <!-- 底部操作按钮 -->
        <div class="bottom-actions">
          <button @click="goToBudget" class="action-button budget-button">
            预算分析
          </button>
          <button @click="goToExpense" class="action-button expense-button">
            记录开销
          </button>
        </div>
      </div>

      <!-- 右侧：地图容器 -->
      <div class="map-container">
        <!-- 地图 -->
        <div id="amap" class="map"></div>

        <!-- 浮动活动面板 -->
        <div class="floating-panel">
          <div class="panel-header">
            <h3 class="panel-title">
              <span class="panel-icon">📋</span>
              {{ selectedDay === 'all' ? '所有活动' : `第${selectedDay}天安排` }}
            </h3>
            <span class="panel-count">{{ getFilteredActivities().length }}</span>
          </div>

          <!-- 活动列表 -->
          <div class="activities-list">
            <div
              v-for="activity in getFilteredActivities()"
              :key="activity.id"
              class="activity-item"
              :style="{ borderLeftColor: getDayColor(activity.dayIndex) }"
              @click="selectActivity(activity)"
            >
              <!-- 天数徽章 -->
              <div class="day-badge" :style="{ background: getDayColor(activity.dayIndex) }">
                Day {{ activity.dayIndex }}
              </div>

              <!-- 活动信息 -->
              <div class="activity-info">
                <h4 class="activity-title">{{ activity.title }}</h4>
                <p class="activity-detail">📍 {{ activity.locationName }}</p>
                <p class="activity-detail">🕐 {{ activity.startTime }} - {{ activity.endTime }}</p>
                <p v-if="activity.estimatedCost" class="activity-detail">
                  💰 ¥{{ (activity.estimatedCost / 100).toFixed(0) }}
                </p>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="getFilteredActivities().length === 0" class="empty-state">
              <div class="empty-icon">🎯</div>
              <p>暂无活动安排</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 页面底部中央悬浮操作按钮（仅未确认的新行程显示） -->
    <div v-if="isNewTrip" class="floating-actions">
      <button @click="regenerateTrip" class="action-btn regenerate-btn">
        <span class="btn-icon">↻</span>
        <span class="btn-text">重新生成</span>
      </button>
      <button @click="confirmTrip" class="action-btn confirm-btn">
        <span class="btn-icon">✓</span>
        <span class="btn-text">确认行程</span>
      </button>
    </div>

    <!-- 重新生成进度弹窗 -->
    <transition name="modal-fade">
      <div v-if="isRegenerating" class="modal-overlay">
        <div class="progress-modal">
          <div class="progress-icon-wrapper">
            <div class="progress-icon">🌍</div>
          </div>
          <h2 class="progress-title">{{ regenerateMessage }}</h2>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: regenerateProgress + '%' }"></div>
          </div>
          <p class="progress-hint">AI 正在为您精心规划行程，请稍候...</p>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Toast from '@/components/Toast.vue'
import apiClient from '@/api/auth'

export default {
  name: 'TripDetailView',
  components: {
    Toast
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const toastRef = ref(null)

    // 响应式数据
    const tripData = ref(null)
    const selectedDay = ref('all')
    const userInput = ref('') // 保存用户原始输入
    const fromPage = ref('') // 记录来源页面
    const isNewTrip = ref(false) // 是否为新生成的行程（未确认）
    const isRegenerating = ref(false) // 重新生成状态
    const regenerateProgress = ref(0) // 重新生成进度
    const regenerateMessage = ref('正在重新生成行程') // 重新生成提示信息
    let mapInstance = null
    const activityMarkers = new Map() // 存储活动ID到标记和信息窗口的映射

    // 显示Toast通知
    const showToast = (message, type = 'success') => {
      if (toastRef.value) {
        toastRef.value.show(message, type)
      }
    }

    // 智能返回（根据来源页面）
    const goBack = () => {
      if (fromPage.value === 'list') {
        router.push('/trips')
      } else if (fromPage.value === 'create') {
        router.push({
          name: 'CreateTrip',
          state: {
            userInput: userInput.value
          }
        })
      } else {
        router.push('/home')
      }
    }

    // 获取日期颜色
    const getDayColor = (dayIndex) => {
      const colors = ['#667eea', '#764ba2', '#f093fb', '#4facfe', '#00f2fe', '#43e97b', '#fa709a', '#fee140']
      return colors[(dayIndex - 1) % colors.length]
    }

    // 格式化日期（完整年月日）
    const formatDate = (date) => {
      if (!date) return ''
      const d = new Date(date)
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    }

    // 获取活动图标
    const getActivityIcon = (type) => {
      const iconMap = {
        transport: '🚗',
        hotel: '🏨',
        sight: '🎯',
        food: '🍽️',
        other: '📌'
      }
      return iconMap[type] || '📌'
    }

    // 获取筛选后的活动
    const getFilteredActivities = () => {
      if (!tripData.value?.days) return []

      if (selectedDay.value === 'all') {
        return tripData.value.days.flatMap(day => day.activities || [])
      }

      const day = tripData.value.days.find(d => d.dayIndex === selectedDay.value)
      return day?.activities || []
    }

    // 选中活动（在地图上聚焦并显示信息窗口）
    const selectActivity = (activity) => {
      if (!mapInstance || !activity.poi || !activity.poi.location) return

      const markerData = activityMarkers.get(activity.id)
      if (!markerData) return

      const { marker, infoWindow } = markerData
      const position = marker.getPosition()

      // 地图平滑移动到标记位置并放大
      mapInstance.setZoomAndCenter(15, position, true, 500)

      // 延迟打开信息窗口，等待地图移动完成
      setTimeout(() => {
        infoWindow.open(mapInstance, position)
      }, 600)
    }

    // 跳转到预算分析
    const goToBudget = () => {
      router.push({
        name: 'BudgetAnalysis',
        params: { tripId: route.params.tripId }
      })
    }

    // 跳转到记录开销
    const goToExpense = () => {
      router.push({
        name: 'ExpenseRecord',
        params: { tripId: route.params.tripId }
      })
    }

    // 确认行程
    const confirmTrip = async () => {
      try {
        const tripId = route.params.tripId

        if (!tripId) {
          showToast('缺少行程ID', 'error')
          console.error('tripId 为空:', tripId)
          return
        }

        console.log('开始确认行程，tripId:', tripId)
        
        // 使用 apiClient 调用确认接口
        const response = await apiClient.post(`/trips/${tripId}/confirm`)
        
        console.log('确认行程响应:', response)

        if (response.success) {
          showToast('行程已保存', 'success')
          // 更新本地数据状态，按钮将消失
          if (tripData.value) {
            tripData.value.updatedAt = new Date().toISOString()
          }
          isNewTrip.value = false
        } else {
          showToast(response.message || '确认失败', 'error')
          console.error('确认失败，响应:', response)
        }
      } catch (error) {
        console.error('确认行程失败，详细错误:', error)
        console.error('错误响应:', error.response)
        showToast(error.response?.data?.message || '确认失败，请稍后重试', 'error')
      }
    }

    // 重新生成行程
    const regenerateTrip = async () => {
      console.log('点击重新生成，当前状态:', {
        isRegenerating: isRegenerating.value,
        userInput: userInput.value
      })

      // 防止重复点击
      if (isRegenerating.value) {
        console.log('正在生成中，忽略重复点击')
        return
      }

      if (!userInput.value) {
        console.error('缺少用户输入:', userInput.value)
        showToast('缺少用户输入，无法重新生成', 'error')
        return
      }

      console.log('开始重新生成行程...')

      isRegenerating.value = true
      regenerateProgress.value = 0
      regenerateMessage.value = '正在分析您的需求'

      // 模拟进度条
      let currentProgress = 0
      const progressInterval = setInterval(() => {
        if (currentProgress < 85) {
          currentProgress += Math.random() * 3
          regenerateProgress.value = Math.min(currentProgress, 85)
          
          if (regenerateProgress.value > 20 && regenerateProgress.value < 50) {
            regenerateMessage.value = '正在规划最佳路线'
          } else if (regenerateProgress.value >= 50) {
            regenerateMessage.value = '正在优化行程细节'
          }
        }
      }, 1000)

      try {
        const response = await apiClient.post('/trips', {
          userInput: userInput.value
        })

        if (response.success) {
          // 完成进度
          regenerateProgress.value = 100
          regenerateMessage.value = '行程生成完成！'
          
          clearInterval(progressInterval)
          
          // 延迟跳转到新行程详情页
          setTimeout(() => {
            isRegenerating.value = false
            
            const newTripId = response.data.tripId || response.data.id
            router.push({
              name: 'TripDetail',
              params: { tripId: newTripId },
              state: { 
                tripData: response.data,
                userInput: userInput.value,
                fromPage: 'create'
              }
            })
          }, 800)
        } else {
          throw new Error(response.message || '生成失败')
        }
      } catch (error) {
        console.error('重新生成行程失败:', error)
        clearInterval(progressInterval)
        isRegenerating.value = false
        showToast(error.response?.data?.message || '生成失败，请稍后重试', 'error')
      }
    }

    // 加载高德地图脚本
    const loadAmapScript = () => {
      return new Promise((resolve, reject) => {
        if (window.AMap) {
          resolve()
          return
        }
        const script = document.createElement('script')
        const apiKey = '8e1060ffae0732dd667f101191a51044' // 从后端配置读取
        script.src = `https://webapi.amap.com/maps?v=2.0&key=${apiKey}`
        script.onload = resolve
        script.onerror = reject
        document.head.appendChild(script)
      })
    }

    // 初始化高德地图
    const initMap = async () => {
      try {
        // 动态加载高德地图脚本
        await loadAmapScript()

        const AMap = window.AMap

        // 创建地图实例
        mapInstance = new AMap.Map('amap', {
          zoom: 10,
          center: [116.397428, 39.90923] // 默认中心（北京）
        })

        // 绘制行程路径
        drawRoute()
      } catch (error) {
        console.error('地图初始化失败:', error)
        showToast('地图加载失败', 'error')
      }
    }

    // 绘制行程路径
    const drawRoute = () => {
      if (!mapInstance || !tripData.value?.days) return

      const AMap = window.AMap
      const activities = getFilteredActivities()

      if (activities.length === 0) return

      // 清除地图上的所有标记和路线
      mapInstance.clearMap()
      activityMarkers.clear()

      // 获取所有 POI 点
      const points = activities
        .filter(a => a.poi && a.poi.location)
        .map((a, index) => {
          const [lng, lat] = a.poi.location.split(',').map(Number)
          return { lng, lat, activity: a, index }
        })

      if (points.length === 0) return

      // 设置地图中心和缩放级别
      if (points.length === 1) {
        mapInstance.setCenter([points[0].lng, points[0].lat])
        mapInstance.setZoom(15)
      } else {
        // 计算边界
        const lngs = points.map(p => p.lng)
        const lats = points.map(p => p.lat)
        const bounds = new AMap.Bounds([Math.min(...lngs), Math.min(...lats)], [Math.max(...lngs), Math.max(...lats)])
        mapInstance.setBounds(bounds)
      }

      // 绘制路径和标记
      let previousPoint = null

      for (let i = 0; i < points.length; i++) {
        const point = points[i]
        const dayIndex = point.activity.dayIndex

        // 创建自定义标记图标内容
        const markerContent = document.createElement('div')
        markerContent.className = 'custom-marker'
        markerContent.style.cssText = `
          width: 2.5vw;
          height: 2.5vw;
          border-radius: 50%;
          background: ${getDayColor(dayIndex)};
          color: white;
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: bold;
          font-size: 1vw;
          box-shadow: 0 0.2vw 0.8vw rgba(0, 0, 0, 0.3);
          border: 0.2vw solid white;
        `
        markerContent.textContent = i + 1

        // 绘制标记
        const marker = new AMap.Marker({
          position: [point.lng, point.lat],
          title: point.activity.title,
          content: markerContent
        })

        // 添加信息窗口
        const infoWindow = new AMap.InfoWindow({
          content: `
            <div style="padding: 1vw; background: white; border-radius: 0.5vw;">
              <h4 style="margin: 0 0 0.5vw 0; font-size: 1vw; color: #333;">${point.activity.title}</h4>
              <p style="margin: 0.25vw 0; font-size: 0.9vw; color: #666;">📍 ${point.activity.locationName}</p>
              <p style="margin: 0.25vw 0; font-size: 0.9vw; color: #666;">🕐 ${point.activity.startTime} - ${point.activity.endTime}</p>
              ${point.activity.estimatedCost ? `<p style="margin: 0.25vw 0; font-size: 0.9vw; color: #666;">💰 ¥${(point.activity.estimatedCost / 100).toFixed(0)}</p>` : ''}
            </div>
          `,
          isCustom: false,
          autoMove: true,
          closeWhenClickMap: true
        })

        marker.on('click', () => {
          infoWindow.open(mapInstance, marker.getPosition())
        })

        mapInstance.add(marker)

        // 存储活动ID到标记和信息窗口的映射
        activityMarkers.set(point.activity.id, { marker, infoWindow })

        // 绘制连线
        if (previousPoint && previousPoint.activity.dayIndex === dayIndex) {
          const polyline = new AMap.Polyline({
            path: [[previousPoint.lng, previousPoint.lat], [point.lng, point.lat]],
            strokeColor: getDayColor(dayIndex),
            strokeWeight: 3,
            strokeOpacity: 0.7,
            strokeStyle: 'solid'
          })
          mapInstance.add(polyline)
        }

        previousPoint = point
      }
    }

    // 监听天数选择变化
    watch(selectedDay, () => {
      if (mapInstance) {
        drawRoute()
      }
    })

    // 加载行程数据
    onMounted(async () => {
      // 从路由状态获取来源页面
      fromPage.value = history.state?.fromPage || ''

      // 从路由状态或参数获取行程数据
      const stateData = history.state?.tripData
      const paramData = route.params?.tripData

      if (stateData) {
        tripData.value = stateData
        // 保存用户输入（如果有）
        userInput.value = history.state?.userInput || ''
        // 判断是否为新行程（没有 updatedAt）
        isNewTrip.value = !stateData.updatedAt
        // 初始化地图
        setTimeout(initMap, 300)
      } else if (paramData) {
        tripData.value = paramData
        // 判断是否为新行程
        isNewTrip.value = !paramData.updatedAt
        // 初始化地图
        setTimeout(initMap, 300)
      } else {
        // 从 API 获取行程数据
        const tripId = route.params.tripId
        if (tripId) {
          try {
            console.log('从API加载行程数据，tripId:', tripId)
            const response = await apiClient.get(`/trips/${tripId}/itinerary`)
            console.log('行程数据响应:', response)
            
            if (response.success) {
              tripData.value = response.data
              // 从 API 加载的行程，判断是否为新行程
              isNewTrip.value = !response.data.updatedAt
              // 初始化地图
              setTimeout(initMap, 300)
            } else {
              showToast(response.message || '无法加载行程数据', 'error')
            }
          } catch (error) {
            console.error('加载行程数据失败:', error)
            showToast('无法加载行程数据，请稍后重试', 'error')
          }
        } else {
          showToast('缺少行程ID', 'error')
        }
      }
    })

    onUnmounted(() => {
      if (mapInstance) {
        mapInstance.destroy()
      }
    })

    return {
      tripData,
      selectedDay,
      toastRef,
      goBack,
      getDayColor,
      formatDate,
      getActivityIcon,
      getFilteredActivities,
      selectActivity,
      goToBudget,
      goToExpense,
      confirmTrip,
      regenerateTrip,
      isNewTrip,
      isRegenerating,
      regenerateProgress,
      regenerateMessage
    }
  }
}
</script>

<style scoped>
.trip-detail-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
  overflow: hidden;
}

/* 顶部导航栏 */
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5vw 2vw;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 0.5vw 1.5vw rgba(0, 0, 0, 0.15);
  z-index: 10;
}

.page-title {
  color: white;
  font-size: 2vw;
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
  border-radius: 1vw;
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

/* 主内容区 - 两栏布局 */
.detail-content {
  flex: 1;
  display: flex;
  gap: 0.5vw;
  padding: 1vw;
  overflow: hidden;
}

/* 左侧：天数选择栏 */
.left-sidebar {
  width: 16vw;
  background: white;
  border-radius: 1vw;
  box-shadow: 0 0.5vw 2vw rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  padding: 1.25vw;
  border-bottom: 0.125vw solid #e0e0e0;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
}

.sidebar-title {
  margin: 0;
  font-size: 1.1vw;
  font-weight: 700;
  color: #333;
}

/* 天数选择器（固定高度） */
.days-scroller {
  display: flex;
  flex-direction: column;
  gap: 0.5vw;
  padding: 0.75vw;
  overflow-y: auto;
  height: 50vh;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE/Edge */
  flex-shrink: 0; /* 不收缩 */
}

/* 隐藏滚动条 */
.days-scroller::-webkit-scrollbar {
  display: none; /* Chrome/Safari */
}

.day-button {
  padding: 0.75vw 0.875vw;
  background: white;
  border: 0.125vw solid #e0e0e0;
  border-left: 0.35vw solid;
  border-radius: 0.4vw;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: left;
}

.day-button:hover {
  background: #f9f9f9;
  border-color: #667eea;
}

.day-button.active {
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.1) 0%, transparent 100%);
  border-color: #667eea;
  font-weight: 600;
}

.day-button.all-days {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.08) 100%);
  border-left-color: #667eea;
  font-weight: 600;
  margin-bottom: 0.25vw;
}

.day-label {
  display: block;
  font-size: 0.95vw;
  color: #333;
  font-weight: 600;
}

.day-date {
  display: block;
  font-size: 0.75vw;
  color: #999;
  margin-top: 0.15vw;
}

.day-count {
  display: block;
  font-size: 0.75vw;
  color: #999;
  margin-top: 0.15vw;
}

/* 底部操作按钮（填充剩余空间） */
.bottom-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 1vw;
  padding: 1.5vw 1vw;
  flex: 1;
  background: linear-gradient(180deg, #fafafa 0%, #ffffff 100%);
  border-top: 0.125vw solid #e8e8e8;
  min-height: 0;
}

.action-button {
  flex: 1;
  max-height: 4vw;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5vw;
  font-size: 1.1vw;
  font-weight: 700;
  border: none;
  border-radius: 0.8vw;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: white;
  position: relative;
  overflow: hidden;
  letter-spacing: 0.05em;
}

.action-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.2);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.action-button:hover::before {
  opacity: 1;
}

.action-button:active {
  transform: scale(0.98);
}

.budget-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 0.3vw 0.8vw rgba(102, 126, 234, 0.3);
}

.budget-button::after {
  content: '📊';
  position: absolute;
  left: 1.5vw;
  font-size: 1.3vw;
}

.budget-button:hover {
  transform: translateY(-0.15vw);
  box-shadow: 0 0.5vw 1.5vw rgba(102, 126, 234, 0.4);
}

.expense-button {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  box-shadow: 0 0.3vw 0.8vw rgba(245, 87, 108, 0.3);
}

.expense-button::after {
  content: '💰';
  position: absolute;
  left: 1.5vw;
  font-size: 1.3vw;
}

.expense-button:hover {
  transform: translateY(-0.15vw);
  box-shadow: 0 0.5vw 1.5vw rgba(245, 87, 108, 0.4);
}

/* 右侧：地图容器（相对定位，用于容纳浮动面板） */
.map-container {
  flex: 1;
  position: relative;
  border-radius: 1vw;
  box-shadow: 0 0.5vw 2vw rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.map {
  width: 100%;
  height: 100%;
}

/* 页面底部中央悬浮操作按钮 */
.floating-actions {
  position: fixed;
  bottom: 5vw;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 2.5vw;  /* 增大按钮之间的间距 */
  z-index: 200;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 0.6vw;
  padding: 1vw 2vw;
  border: none;
  border-radius: 3vw;
  font-size: 1.1vw;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 0.5vw 2vw rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
  letter-spacing: 0.05em;
}

.action-btn:hover {
  transform: translateY(-0.2vw);
  box-shadow: 0 0.8vw 2.5vw rgba(0, 0, 0, 0.3);
}

.action-btn:active {
  transform: translateY(0);
}

.btn-icon {
  font-size: 1.4vw;
  font-weight: bold;
}

.btn-text {
  font-size: 1.1vw;
}

.confirm-btn {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  color: white;
}

.confirm-btn:hover {
  background: linear-gradient(135deg, #38f9d7 0%, #43e97b 100%);
}

.regenerate-btn {
  background: rgba(255, 255, 255, 0.95);
  color: #667eea;
  border: 0.15vw solid #667eea;
}

.regenerate-btn:hover {
  background: rgba(102, 126, 234, 0.1);
  border-color: #764ba2;
  color: #764ba2;
}

/* 浮动活动面板（在地图右侧，上下居中） */
.floating-panel {
  position: absolute;
  top: 50%;
  right: 1.5vw;
  transform: translateY(-50%);
  width: 18vw;
  height: 38vw;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 1vw;
  box-shadow: 0 0.5vw 2.5vw rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 100;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25vw;
  border-bottom: 0.125vw solid #e0e0e0;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
}

.panel-title {
  margin: 0;
  font-size: 1.1vw;
  font-weight: 700;
  color: #333;
  display: flex;
  align-items: center;
  gap: 0.5vw;
}

.panel-icon {
  font-size: 1.2vw;
}

.panel-count {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 0.3vw 0.75vw;
  border-radius: 1vw;
  font-size: 0.85vw;
  font-weight: 600;
}

/* 活动列表（隐藏滚动条） */
.activities-list {
  flex: 1;
  overflow-y: auto;
  padding: 0.75vw;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE/Edge */
}

/* 隐藏滚动条 */
.activities-list::-webkit-scrollbar {
  display: none; /* Chrome/Safari */
}

/* 简洁活动项 */
.activity-item {
  display: flex;
  gap: 0.75vw;
  padding: 0.875vw;
  background: white;
  border-left: 0.4vw solid;
  border: 0.1vw solid #e0e0e0;
  border-left: 0.4vw solid;
  border-radius: 0.75vw;
  margin-bottom: 0.75vw;
  box-shadow: 0 0.2vw 0.6vw rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  cursor: pointer;
  align-items: flex-start;
  position: relative;
}

.activity-item:hover {
  background: #ffffff;
  border-color: #d0d0d0;
  box-shadow: 0 0.4vw 1.2vw rgba(0, 0, 0, 0.15);
  transform: translateY(-0.2vw);
}

/* 天数徽章 */
.day-badge {
  position: absolute;
  top: 0.5vw;
  right: 0.5vw;
  padding: 0.2vw 0.5vw;
  border-radius: 0.4vw;
  font-size: 0.65vw;
  font-weight: 700;
  color: white;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  box-shadow: 0 0.1vw 0.3vw rgba(0, 0, 0, 0.2);
}

.activity-info {
  flex: 1;
  min-width: 0;
}

.activity-title {
  margin: 0 0 0.3vw 0;
  font-size: 0.95vw;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.activity-detail {
  margin: 0.2vw 0;
  font-size: 0.8vw;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.empty-icon {
  font-size: 3vw;
  margin-bottom: 0.5vw;
  opacity: 0.5;
}

.empty-state p {
  margin: 0;
  font-size: 0.9vw;
}

/* 响应式调整 */
@media (max-height: 800px) {
  .sidebar {
    width: 22vw;
  }

  .day-label {
    font-size: 0.9vw;
  }

  .activity-title {
    font-size: 0.9vw;
  }
}

/* 重新生成进度弹窗样式（与 CreateTrip 保持一致） */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(5px);
}

.progress-modal {
  background: white;
  border-radius: 1.5vw;
  padding: 3vw;
  width: 30vw;
  text-align: center;
  box-shadow: 0 1.5vw 4vw rgba(0, 0, 0, 0.3);
}

.progress-icon-wrapper {
  margin-bottom: 1.5vw;
}

.progress-icon {
  font-size: 5vw;
  display: inline-block;
  animation: rotate-slow 3s linear infinite;
}

@keyframes rotate-slow {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.progress-title {
  font-size: 1.5vw;
  color: #333;
  margin: 0 0 1.5vw 0;
  font-weight: 800;
}

.progress-bar {
  width: 100%;
  height: 0.6vw;
  background: #e0e0e0;
  border-radius: 0.3vw;
  overflow: hidden;
  margin-bottom: 1vw;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  transition: width 0.5s ease;
  border-radius: 1vw;
}

.progress-hint {
  font-size: 1vw;
  color: #999;
  margin: 0;
}

/* Modal 过渡动画 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-active .modal-overlay,
.modal-fade-enter-active .progress-modal {
  animation: modal-slide-in 0.3s ease;
}

.modal-fade-leave-active .modal-overlay,
.modal-fade-leave-active .progress-modal {
  animation: modal-slide-out 0.3s ease;
}

@keyframes modal-slide-in {
  from {
    transform: scale(0.9) translateY(-2vw);
    opacity: 0;
  }
  to {
    transform: scale(1) translateY(0);
    opacity: 1;
  }
}

@keyframes modal-slide-out {
  from {
    transform: scale(1) translateY(0);
    opacity: 1;
  }
  to {
    transform: scale(0.9) translateY(-2vw);
    opacity: 0;
  }
}
</style>
