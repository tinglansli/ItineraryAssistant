<template>
  <div class="budget-analysis-container">
    <!-- Toast 通知组件 -->
    <Toast ref="toastRef" />

    <!-- 顶部导航栏 -->
    <div class="analysis-header">
      <button @click="goBack" class="back-button">
        <span class="back-icon">←</span>
        <span>返回</span>
      </button>
      <h1 class="page-title">📊 预算分析</h1>
      <div class="header-spacer"></div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner">
        <div class="spinner-icon">💰</div>
        <p>正在加载预算数据...</p>
      </div>
    </div>

    <!-- 主内容区 -->
    <div v-else-if="budgetData" class="analysis-content">
      <!-- 主浮窗卡片 -->
      <div class="main-card">
        <!-- 左侧书签式选择器 -->
        <div class="bookmark-tabs">
          <div
            class="bookmark-tab all-tab"
            :class="{ active: selectedCategory === 'all' }"
            @click="selectedCategory = 'all'"
          >
            <div class="bookmark-content">
              <span class="bookmark-icon">📊</span>
              <span class="bookmark-text">全部</span>
            </div>
          </div>
          <div
            v-for="category in categoryList"
            :key="category.key"
            class="bookmark-tab"
            :class="[
              { active: selectedCategory === category.key },
              `tab-${category.key}`
            ]"
            @click="selectedCategory = category.key"
          >
            <div class="bookmark-content">
              <span class="bookmark-icon">{{ category.icon }}</span>
              <span class="bookmark-text">{{ category.name }}</span>
            </div>
          </div>
        </div>

        <!-- 右侧图表展示区 -->
        <div class="chart-display">
          <!-- 标题 -->
          <div class="chart-title">
            <h2>{{ getCurrentCategoryName() }}</h2>
            <div class="title-decoration"></div>
          </div>

          <!-- 环形图容器 -->
          <div class="donut-container">
            <svg viewBox="0 0 400 400" class="donut-chart">
              <!-- 背景圆环 -->
              <circle
                cx="200"
                cy="200"
                r="150"
                fill="none"
                stroke="#f0f0f0"
                stroke-width="60"
              />
              <!-- 进度圆环 -->
              <circle
                cx="200"
                cy="200"
                r="150"
                fill="none"
                :stroke="getCurrentColor()"
                stroke-width="60"
                :stroke-dasharray="`${getCurrentProgress()} ${getCurrentRemainingProgress()}`"
                stroke-dashoffset="235.5"
                class="donut-progress"
                stroke-linecap="round"
              />
              <!-- 内圈装饰 -->
              <circle
                cx="200"
                cy="200"
                r="90"
                fill="none"
                :stroke="getCurrentColor()"
                stroke-width="2"
                opacity="0.3"
              />
            </svg>

            <!-- 中心数据 -->
            <div class="donut-center">
              <div class="percentage" :style="{ color: getCurrentColor() }">
                {{ getCurrentPercentage().toFixed(1) }}%
              </div>
              <div class="percentage-label">使用率</div>
            </div>
          </div>

          <!-- 数据卡片组 -->
          <div class="data-cards">
            <div class="data-card budget-card">
              <div class="card-icon">💰</div>
              <div class="card-content">
                <div class="card-label">预算金额</div>
                <div class="card-value">¥{{ formatAmount(getCurrentBudget()) }}</div>
              </div>
            </div>

            <div class="data-card expense-card">
              <div class="card-icon">💸</div>
              <div class="card-content">
                <div class="card-label">已使用</div>
                <div class="card-value">¥{{ formatAmount(getCurrentExpense()) }}</div>
              </div>
            </div>

            <div class="data-card remaining-card" :class="{ 'over-budget': getCurrentRemaining() < 0 }">
              <div class="card-icon">{{ getCurrentRemaining() >= 0 ? '💎' : '⚠️' }}</div>
              <div class="card-content">
                <div class="card-label">{{ getCurrentRemaining() >= 0 ? '剩余' : '超支' }}</div>
                <div class="card-value">¥{{ formatAmount(Math.abs(getCurrentRemaining())) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else class="error-container">
      <div class="error-icon">😔</div>
      <h2>无法加载预算数据</h2>
      <p>请稍后重试</p>
      <button @click="loadBudgetData" class="retry-button">重新加载</button>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Toast from '@/components/Toast.vue'
import apiClient from '@/api/auth'

export default {
  name: 'BudgetAnalysisView',
  components: {
    Toast
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const toastRef = ref(null)

    const loading = ref(true)
    const budgetData = ref(null)
    const selectedCategory = ref('all')

    const categoryConfig = {
      transport: { name: '交通', icon: '🚗', color: '#FF6B6B' },
      hotel: { name: '住宿', icon: '🏨', color: '#4ECDC4' },
      sight: { name: '景点', icon: '🎯', color: '#45B7D1' },
      food: { name: '餐饮', icon: '🍽️', color: '#FFA07A' },
      other: { name: '其他', icon: '📌', color: '#DDA15E' }
    }

    // 显示Toast通知
    const showToast = (message, type = 'success') => {
      if (toastRef.value) {
        toastRef.value.show(message, type)
      }
    }

    // 返回上一页
    const goBack = () => {
      router.back()
    }

    // 格式化金额
    const formatAmount = (amount) => {
      if (amount === undefined || amount === null) return '0'
      const value = Math.round(amount / 100)
      return value.toLocaleString('zh-CN')
    }

    // 处理分类数据
    const categoryList = computed(() => {
      if (!budgetData.value) return []
      
      const list = []
      const categories = ['transport', 'hotel', 'sight', 'food', 'other']
      
      categories.forEach(key => {
        const planned = budgetData.value.plannedBudget[key] || 0
        const actual = budgetData.value.actualExpense[key] || 0
        
        // 只显示有预算或有开销的分类
        if (planned > 0 || actual > 0) {
          const remaining = planned - actual
          const percentage = planned > 0 ? (actual / planned) * 100 : 0
          
          list.push({
            key,
            name: categoryConfig[key].name,
            icon: categoryConfig[key].icon,
            color: categoryConfig[key].color,
            planned,
            actual,
            remaining,
            percentage,
            isOverBudget: remaining < 0
          })
        }
      })
      
      // 按预算金额排序
      return list.sort((a, b) => b.planned - a.planned)
    })

    // 获取当前选中的分类名称
    const getCurrentCategoryName = () => {
      if (selectedCategory.value === 'all') {
        return '总预算使用情况'
      }
      const category = categoryList.value.find(c => c.key === selectedCategory.value)
      return category ? category.name + '预算使用情况' : ''
    }

    // 获取当前选中的预算
    const getCurrentBudget = () => {
      if (selectedCategory.value === 'all') {
        return budgetData.value?.totalPlanned || 0
      }
      return budgetData.value?.plannedBudget[selectedCategory.value] || 0
    }

    // 获取当前选中的开销
    const getCurrentExpense = () => {
      if (selectedCategory.value === 'all') {
        return budgetData.value?.totalActual || 0
      }
      return budgetData.value?.actualExpense[selectedCategory.value] || 0
    }

    // 获取当前选中的剩余
    const getCurrentRemaining = () => {
      return getCurrentBudget() - getCurrentExpense()
    }

    // 获取当前选中的使用百分比
    const getCurrentPercentage = () => {
      const budget = getCurrentBudget()
      if (budget === 0) return 0
      return (getCurrentExpense() / budget) * 100
    }

    // 获取当前选中的颜色
    const getCurrentColor = () => {
      if (selectedCategory.value === 'all') {
        const percentage = getCurrentPercentage()
        if (percentage > 100) return '#F5576C'
        if (percentage > 80) return '#FFB84D'
        return '#51CF66'
      }
      const category = categoryList.value.find(c => c.key === selectedCategory.value)
      return category ? category.color : '#667EEA'
    }

    // 获取当前选中的进度（环形图）
    const getCurrentProgress = () => {
      const circumference = 2 * Math.PI * 150
      const percentage = Math.min(getCurrentPercentage(), 100)
      return (percentage / 100) * circumference
    }

    // 获取当前选中的剩余进度
    const getCurrentRemainingProgress = () => {
      const circumference = 2 * Math.PI * 150
      return circumference - getCurrentProgress()
    }

    // 加载预算数据
    const loadBudgetData = async () => {
      loading.value = true
      const tripId = route.params.tripId

      if (!tripId) {
        showToast('缺少行程ID', 'error')
        loading.value = false
        return
      }

      try {
        console.log('加载预算数据，tripId:', tripId)
        const response = await apiClient.get(`/trips/${tripId}/budget`)
        console.log('预算数据响应:', response)

        if (response.success) {
          budgetData.value = response.data
          // 移除成功提示Toast
        } else {
          showToast(response.message || '加载失败', 'error')
          budgetData.value = null
        }
      } catch (error) {
        console.error('加载预算数据失败:', error)
        showToast(error.response?.data?.message || '加载失败，请稍后重试', 'error')
        budgetData.value = null
      } finally {
        loading.value = false
      }
    }

    onMounted(() => {
      // 禁止body滚动
      document.body.style.overflow = 'hidden'
      loadBudgetData()
    })

    // 组件卸载时恢复body滚动
    onUnmounted(() => {
      document.body.style.overflow = ''
    })

    return {
      toastRef,
      loading,
      budgetData,
      selectedCategory,
      goBack,
      formatAmount,
      categoryList,
      loadBudgetData,
      getCurrentCategoryName,
      getCurrentBudget,
      getCurrentExpense,
      getCurrentRemaining,
      getCurrentPercentage,
      getCurrentColor,
      getCurrentProgress,
      getCurrentRemainingProgress
    }
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.budget-analysis-container {
  width: 100%;
  height: 100vh; /* 固定为视口高度 */
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 禁止滚动 */
}

/* 顶部导航栏 */
.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.2vw 2vw;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 0.5vw 1.5vw rgba(0, 0, 0, 0.15);
  z-index: 10;
}

.page-title {
  color: white;
  font-size: 1.8vw;
  font-weight: 700;
  margin: 0;
  text-shadow: 0 0.15vw 0.5vw rgba(0, 0, 0, 0.2);
}

.back-button {
  display: flex;
  align-items: center;
  gap: 0.5vw;
  padding: 0.6vw 1.2vw;
  background: rgba(255, 255, 255, 0.15);
  color: white;
  border: 0.125vw solid rgba(255, 255, 255, 0.4);
  border-radius: 0.8vw;
  font-size: 0.9vw;
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
  font-size: 1.1vw;
}

.header-spacer {
  width: 7vw;
}

/* 加载状态 */
.loading-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: calc(100vh - 4.2vw);
  overflow: hidden;
}

.loading-spinner {
  text-align: center;
  color: white;
}

.spinner-icon {
  font-size: 4.5vw;
  animation: spin 2s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.loading-spinner p {
  margin-top: 1.2vw;
  font-size: 1.1vw;
  font-weight: 500;
}

/* 错误状态 */
.error-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  height: calc(100vh - 4.2vw);
  overflow: hidden;
}

.error-icon {
  font-size: 4.5vw;
  margin-bottom: 1.2vw;
}

.error-container h2 {
  font-size: 1.6vw;
  margin: 0.5vw 0;
}

.error-container p {
  font-size: 1vw;
  margin: 0.5vw 0 1.8vw 0;
  opacity: 0.9;
}

.retry-button {
  padding: 0.8vw 2.2vw;
  background: white;
  color: #667eea;
  border: none;
  border-radius: 0.9vw;
  font-size: 0.95vw;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 0.35vw 1vw rgba(0, 0, 0, 0.2);
}

.retry-button:hover {
  transform: translateY(-0.2vw);
  box-shadow: 0 0.5vw 1.3vw rgba(0, 0, 0, 0.3);
}

/* 主内容区 */
.analysis-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5vw;
  height: calc(100vh - 4.2vw); /* 精确计算:视口高度-导航栏高度 */
  overflow: hidden; /* 禁止内容区滚动 */
}

/* 主浮窗卡片 - 调整高度适配单屏 */
.main-card {
  position: relative;
  background: white;
  border-radius: 1.8vw;
  box-shadow: 0 1.2vw 3.5vw rgba(0, 0, 0, 0.3);
  width: 100%;
  max-width: 85vw;
  height: calc(100vh - 7.2vw); /* 精确计算:视口高度-导航-内容区padding */
  max-height: calc(100vh - 7.2vw);
  display: flex;
  overflow: hidden;
}

/* 左侧书签式选择器 */
.bookmark-tabs {
  position: absolute;
  left: -3.2vw;
  top: 5vw;
  display: flex;
  flex-direction: column;
  gap: 0.75vw;
  z-index: 10;
}

.bookmark-tab {
  position: relative;
  cursor: pointer;
  transition: all 0.3s ease;
}

.bookmark-content {
  display: flex;
  align-items: center;
  gap: 0.55vw;
  padding: 0.75vw 1.3vw 0.75vw 3.5vw;
  background: white;
  border-radius: 0 0.9vw 0.9vw 0;
  box-shadow: 0.13vw 0.13vw 0.5vw rgba(0, 0, 0, 0.1);
  min-width: 9vw;
  transition: all 0.3s ease;
  border-left: 0.35vw solid #ddd;
}

.bookmark-icon {
  font-size: 1.35vw;
  transition: transform 0.3s ease;
}

.bookmark-text {
  font-size: 1vw;
  font-weight: 600;
  color: #666;
  transition: color 0.3s ease;
}

/* 全部标签样式 */
.all-tab .bookmark-content {
  border-left-color: #667eea;
}

.all-tab.active .bookmark-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding-left: 4vw;
  box-shadow: 0.27vw 0.27vw 1vw rgba(102, 126, 234, 0.4);
}

.all-tab.active .bookmark-text {
  color: white;
}

.all-tab:hover .bookmark-content {
  padding-left: 3.8vw;
  box-shadow: 0.2vw 0.2vw 0.8vw rgba(102, 126, 234, 0.3);
}

/* 各分类标签颜色 */
.tab-transport .bookmark-content {
  border-left-color: #FF6B6B;
}

.tab-transport.active .bookmark-content {
  background: linear-gradient(135deg, #FF6B6B 0%, #FF8E8E 100%);
}

.tab-hotel .bookmark-content {
  border-left-color: #4ECDC4;
}

.tab-hotel.active .bookmark-content {
  background: linear-gradient(135deg, #4ECDC4 0%, #6FE0D8 100%);
}

.tab-sight .bookmark-content {
  border-left-color: #45B7D1;
}

.tab-sight.active .bookmark-content {
  background: linear-gradient(135deg, #45B7D1 0%, #66C7E0 100%);
}

.tab-food .bookmark-content {
  border-left-color: #FFA07A;
}

.tab-food.active .bookmark-content {
  background: linear-gradient(135deg, #FFA07A 0%, #FFB599 100%);
}

.tab-other .bookmark-content {
  border-left-color: #DDA15E;
}

.tab-other.active .bookmark-content {
  background: linear-gradient(135deg, #DDA15E 0%, #E8B77D 100%);
}

.bookmark-tab.active .bookmark-content {
  padding-left: 4vw;
  box-shadow: 0.27vw 0.27vw 1vw rgba(0, 0, 0, 0.2);
  transform: translateX(0.18vw);
}

.bookmark-tab.active .bookmark-text {
  color: white;
}

.bookmark-tab:hover .bookmark-content {
  padding-left: 3.8vw;
  box-shadow: 0.2vw 0.2vw 0.8vw rgba(0, 0, 0, 0.15);
  transform: translateX(0.13vw);
}

.bookmark-tab:hover .bookmark-icon {
  transform: scale(1.1);
}

/* 右侧图表展示区 - 优化间距 */
.chart-display {
  flex: 1;
  padding: 2.5vw 3.5vw 2.5vw 5vw;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

/* 标题 - 减小间距 */
.chart-title {
  text-align: center;
  margin-bottom: 2vw;
}

.chart-title h2 {
  font-size: 1.8vw;
  font-weight: 700;
  color: #333;
  margin: 0 0 0.6vw 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.title-decoration {
  width: 5.5vw;
  height: 0.27vw;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  margin: 0 auto;
  border-radius: 0.14vw;
}

/* 环形图容器 - 缩小尺寸 */
.donut-container {
  position: relative;
  width: 22vw;
  height: 22vw;
  margin-bottom: 2.5vw;
}

.donut-chart {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
  filter: drop-shadow(0 0.5vw 1.3vw rgba(0, 0, 0, 0.1));
}

.donut-progress {
  transition: stroke-dasharray 1s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 中心数据 */
.donut-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.percentage {
  font-size: 3.5vw;
  font-weight: 800;
  line-height: 1;
  margin-bottom: 0.5vw;
  text-shadow: 0 0.13vw 0.65vw rgba(0, 0, 0, 0.1);
}

.percentage-label {
  font-size: 1.05vw;
  color: #999;
  font-weight: 600;
  letter-spacing: 0.065vw;
}

/* 数据卡片组 - 缩小尺寸 */
.data-cards {
  display: flex;
  gap: 1.5vw;
  width: 100%;
  justify-content: center;
}

.data-card {
  flex: 1;
  max-width: 13vw;
  padding: 1.3vw;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
  border-radius: 1vw;
  box-shadow: 0 0.27vw 0.95vw rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  border: 0.13vw solid transparent;
  display: flex;
  align-items: center;
  gap: 1vw;
}

.data-card:hover {
  transform: translateY(-0.27vw);
  box-shadow: 0 0.52vw 1.65vw rgba(0, 0, 0, 0.12);
}

.budget-card {
  border-color: #667eea;
}

.budget-card:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.budget-card:hover .card-label,
.budget-card:hover .card-value {
  color: white;
}

.expense-card {
  border-color: #f093fb;
}

.expense-card:hover {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.expense-card:hover .card-label,
.expense-card:hover .card-value {
  color: white;
}

.remaining-card {
  border-color: #51CF66;
}

.remaining-card:hover {
  background: linear-gradient(135deg, #51CF66 0%, #43e97b 100%);
}

.remaining-card:hover .card-label,
.remaining-card:hover .card-value {
  color: white;
}

.remaining-card.over-budget {
  border-color: #F5576C;
}

.remaining-card.over-budget:hover {
  background: linear-gradient(135deg, #F5576C 0%, #ff6b81 100%);
}

.card-icon {
  font-size: 2.3vw;
  transition: transform 0.3s ease;
}

.data-card:hover .card-icon {
  transform: scale(1.15);
}

.card-content {
  flex: 1;
}

.card-label {
  font-size: 0.85vw;
  color: #999;
  margin-bottom: 0.4vw;
  font-weight: 600;
  letter-spacing: 0.032vw;
  transition: color 0.3s ease;
}

.card-value {
  font-size: 1.4vw;
  font-weight: 700;
  color: #333;
  transition: color 0.3s ease;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .main-card {
    max-width: 75vw;
  }

  .chart-display {
    padding: 2.2vw 3vw 2.2vw 4.5vw;
  }

  .donut-container {
    width: 20vw;
    height: 20vw;
  }

  .percentage {
    font-size: 3.2vw;
  }
}

@media (max-width: 768px) {
  .analysis-content {
    padding: 1.2vw;
  }

  .main-card {
    flex-direction: column;
    height: auto;
    max-height: none;
  }

  .bookmark-tabs {
    position: static;
    flex-direction: row;
    flex-wrap: wrap;
    padding: 1.2vw;
    gap: 0.5vw;
  }

  .bookmark-content {
    padding: 0.65vw 1vw !important;
    border-radius: 0.8vw;
    border-left: none !important;
    border-bottom: 0.27vw solid;
  }

  .bookmark-tab.active .bookmark-content,
  .bookmark-tab:hover .bookmark-content {
    transform: translateY(-0.13vw);
  }

  .chart-display {
    padding: 1.9vw 1.2vw;
  }

  .donut-container {
    width: 18vw;
    height: 18vw;
  }

  .data-cards {
    flex-direction: column;
    gap: 1vw;
  }

  .data-card {
    max-width: 100%;
  }
}
</style>