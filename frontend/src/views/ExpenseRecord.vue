<template>
  <div class="expense-container">
    <!-- Toast 通知组件 -->
    <Toast ref="toastRef" />

    <!-- 顶部导航栏 -->
    <div class="expense-header">
      <button @click="goBack" class="back-button">
        <span class="back-icon">←</span>
        <span>返回</span>
      </button>
      <h1 class="page-title">💰 记录开销</h1>
      <div class="header-spacer"></div>
    </div>

    <!-- 主内容区 -->
    <div class="expense-content">
      <!-- 左侧：开销列表 -->
      <div class="expense-list-section">
        <div class="section-header">
          <h2 class="section-title">📋 开销列表</h2>
          <div class="expense-summary">
            <span class="total-label">总计:</span>
            <span class="total-amount">¥{{ formatAmount(totalExpense) }}</span>
          </div>
        </div>

        <!-- 开销列表 -->
        <div class="expense-list" v-if="expenses.length > 0">
          <transition-group name="expense-item">
            <div
              v-for="expense in expenses"
              :key="expense.id"
              class="expense-item"
              :class="`category-${getCategoryKey(expense.category)}`"
            >
              <div class="expense-icon" :style="{ background: getCategoryColor(expense.category) }">
                {{ getCategoryIcon(expense.category) }}
              </div>
              <div class="expense-info">
                <div class="expense-main">
                  <div class="expense-description">{{ expense.note }}</div>
                  <div class="expense-amount">¥{{ formatAmount(expense.amountCents) }}</div>
                </div>
                <div class="expense-meta">
                  <span class="expense-category">{{ getCategoryLabel(expense.category) }}</span>
                  <span class="expense-time">{{ formatTime(expense.happenedAt) }}</span>
                </div>
              </div>
            </div>
          </transition-group>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <div class="empty-icon">📝</div>
          <p class="empty-text">还没有开销记录</p>
          <p class="empty-hint">快来记录您的第一笔开销吧~</p>
        </div>
      </div>

      <!-- 右侧：输入区域 -->
      <div class="input-section">
        <div class="input-card">
          <h3 class="input-title">✍️ 新增开销</h3>
          
          <!-- 输入提示 -->
          <div class="input-hint">
            <div class="hint-icon">💡</div>
            <div class="hint-text">
              <p>请描述开销内容,例如:</p>
              <ul>
                <li>"早上买了门票60块"</li>
                <li>"中午吃饭花了120元"</li>
                <li>"打车去机场80块钱"</li>
              </ul>
            </div>
          </div>

          <!-- 输入框区域 -->
          <div class="input-container">
            <div class="textarea-wrapper">
              <textarea
                v-model="expenseInput"
                class="expense-input"
                placeholder="例如：中午吃饭花了120元..."
                :disabled="isRecording || isSubmitting"
                @input="adjustTextareaHeight"
                ref="textareaRef"
              ></textarea>
              
              <!-- 语音按钮（右下角） -->
              <button
                @click="toggleRecording"
                class="voice-button-inline"
                :class="{ recording: isRecording }"
                :title="isRecording ? '完成录音' : '语音输入'"
                :disabled="isSubmitting"
              >
                <svg v-if="!isRecording" class="mic-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
                  <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
                  <line x1="12" y1="19" x2="12" y2="23"></line>
                  <line x1="8" y1="23" x2="16" y2="23"></line>
                </svg>
                <svg v-else class="stop-icon" viewBox="0 0 24 24" fill="currentColor">
                  <rect x="6" y="6" width="12" height="12" rx="2"></rect>
                </svg>
              </button>
            </div>
          </div>

          <!-- 录音提示 -->
          <transition name="fade">
            <div v-if="isRecording" class="recording-hint">
              <span class="recording-dot"></span>
              <span>正在录音中，请说出开销内容...</span>
            </div>
          </transition>

          <!-- 提交按钮 -->
          <button
            @click="submitExpense"
            class="submit-button"
            :disabled="!expenseInput.trim() || isSubmitting"
          >
            <span v-if="!isSubmitting" class="submit-icon">💸</span>
            <span v-else class="submit-icon rotating">⚙️</span>
            <span>{{ isSubmitting ? '记录中...' : '记录开销' }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 语音识别弹窗 -->
    <transition name="modal-fade">
      <div v-if="showVoiceModal" class="modal-overlay">
        <div class="modal-content">
          <div class="voice-animation">
            <div class="wave-circle wave-1"></div>
            <div class="wave-circle wave-2"></div>
            <div class="wave-circle wave-3"></div>
            <div class="microphone-icon">🎤</div>
          </div>
          <h3 class="modal-title">正在识别语音...</h3>
          <p class="modal-subtitle">请稍候,AI正在处理您的语音</p>
        </div>
      </div>
    </transition>

    <!-- 提交开销弹窗 -->
    <transition name="modal-fade">
      <div v-if="showSubmitModal" class="modal-overlay">
        <div class="modal-content">
          <div class="coin-animation">
            <div class="wallet-icon">💰</div>
            <div class="coin coin-1">💴</div>
            <div class="coin coin-2">💵</div>
            <div class="coin coin-3">💶</div>
          </div>
          <h3 class="modal-title">正在记录开销...</h3>
          <p class="modal-subtitle">马上就好</p>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Toast from '@/components/Toast.vue'
import apiClient from '@/api/auth'
import AudioRecorder from '@/utils/audioRecorder'

export default {
  name: 'ExpenseRecord',
  components: {
    Toast
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const toastRef = ref(null)
    const textareaRef = ref(null)

    // 数据
    const expenses = ref([])
    const expenseInput = ref('')
    const isRecording = ref(false)
    const isSubmitting = ref(false)
    const showVoiceModal = ref(false)
    const showSubmitModal = ref(false)

    // 录音相关
    let audioRecorder = null

    // 分类配置 - 每个分类有独特的图标和颜色
    const categoryConfig = {
      'transport': { icon: '🚗', color: '#667eea', label: '交通' },
      'hotel': { icon: '🏨', color: '#f093fb', label: '住宿' },
      'sight': { icon: '🎯', color: '#4facfe', label: '景点' },
      'food': { icon: '🍽️', color: '#43e97b', label: '餐饮' },
      'other': { icon: '📌', color: '#fa709a', label: '其他' }
    }
    
    // 根据API返回的category获取配置
    const getCategoryConfig = (category) => {
      return categoryConfig[category] || categoryConfig['other']
    }

    // 显示Toast
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

    // 格式化时间 - 显示完整的年月日时分
    const formatTime = (timestamp) => {
      if (!timestamp) return ''
      const date = new Date(timestamp)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hour = String(date.getHours()).padStart(2, '0')
      const minute = String(date.getMinutes()).padStart(2, '0')
      
      return `${year}/${month}/${day} ${hour}:${minute}`
    }

    // 获取分类key (就是category本身)
    const getCategoryKey = (category) => {
      return category || 'other'
    }

    // 获取分类图标
    const getCategoryIcon = (category) => {
      return getCategoryConfig(category).icon
    }
    
    // 获取分类颜色
    const getCategoryColor = (category) => {
      return getCategoryConfig(category).color
    }
    
    // 获取分类标签
    const getCategoryLabel = (category) => {
      return getCategoryConfig(category).label
    }

    // 计算总开销
    const totalExpense = computed(() => {
      return expenses.value.reduce((sum, expense) => sum + (expense.amountCents || 0), 0)
    })

    // 自动调整textarea高度
    const adjustTextareaHeight = () => {
      nextTick(() => {
        const textarea = textareaRef.value
        if (textarea) {
          textarea.style.height = 'auto'
          textarea.style.height = Math.min(textarea.scrollHeight, 500) + 'px'
        }
      })
    }

    // 切换录音状态
    const toggleRecording = () => {
      if (isRecording.value) {
        stopRecording()
      } else {
        startRecording()
      }
    }

    // 开始录音
    const startRecording = async () => {
      try {
        // 检查浏览器是否支持
        if (!AudioRecorder.isSupported()) {
          showToast('您的浏览器不支持录音功能', 'error')
          return
        }

        // 先检查是否有麦克风设备
        const devices = await navigator.mediaDevices.enumerateDevices()
        const hasMicrophone = devices.some(device => device.kind === 'audioinput')
        
        if (!hasMicrophone) {
          showToast('未检测到麦克风设备，请连接麦克风后重试', 'error')
          return
        }

        // 创建录音器实例
        audioRecorder = new AudioRecorder()
        await audioRecorder.start()
        
        isRecording.value = true
        console.log('开始录音，将自动转换为 WAV 格式')
      } catch (error) {
        console.error('录音失败:', error)
        
        let errorMessage = '无法访问麦克风'
        if (error.name === 'NotFoundError') {
          errorMessage = '未找到麦克风设备，请检查麦克风是否正确连接'
        } else if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
          errorMessage = '麦克风权限被拒绝，请在浏览器设置中允许使用麦克风'
        } else if (error.name === 'NotReadableError') {
          errorMessage = '麦克风被其他应用占用，请关闭其他使用麦克风的程序'
        } else if (error.name === 'SecurityError') {
          errorMessage = '无法访问麦克风：请使用 HTTPS 或 localhost 访问'
        }
        
        showToast(errorMessage, 'error')
      }
    }

    // 停止录音
    const stopRecording = async () => {
      if (audioRecorder && isRecording.value) {
        isRecording.value = false
        
        try {
          // 停止录音并获取 WAV 格式的音频
          const wavBlob = await audioRecorder.stop()
          console.log('录音完成，音频大小:', wavBlob.size, 'bytes')
          
          // 转换为文字
          await transcribeAudio(wavBlob)
        } catch (error) {
          console.error('停止录音失败:', error)
          showToast('录音处理失败，请重试', 'error')
        }
        
        audioRecorder = null
      }
    }

    // 语音转文字
    const transcribeAudio = async (audioBlob) => {
      showVoiceModal.value = true

      try {
        console.log('上传 WAV 音频，大小:', audioBlob.size, 'bytes')
        
        const formData = new FormData()
        formData.append('audio', audioBlob, 'recording.wav')

        const response = await apiClient.post('/speech/transcribe', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })

        showVoiceModal.value = false

        if (response.success) {
          expenseInput.value = response.data
          adjustTextareaHeight()
          showToast('语音识别成功！', 'success')
        } else {
          showToast(response.message || '语音识别失败', 'error')
        }
      } catch (error) {
        console.error('语音转文字失败:', error)
        showVoiceModal.value = false
        
        const errorMsg = error.response?.data?.message || '语音识别失败，请重试'
        showToast(errorMsg, 'error')
      }
    }

    // 提交开销
    const submitExpense = async () => {
      if (!expenseInput.value.trim()) {
        return
      }

      const tripId = route.params.tripId
      if (!tripId) {
        showToast('缺少行程ID', 'error')
        return
      }

      isSubmitting.value = true
      showSubmitModal.value = true

      try {
        const response = await apiClient.post(`/trips/${tripId}/expenses`, {
          textInput: expenseInput.value.trim()
        })

        // 短暂延迟后关闭弹窗
        setTimeout(() => {
          showSubmitModal.value = false

          if (response.success) {
            // 添加到列表顶部
            expenses.value.unshift(response.data)
            
            // 清空输入框
            expenseInput.value = ''
            adjustTextareaHeight()
            
            showToast('开销记录成功！', 'success')
          } else {
            showToast(response.message || '记录失败', 'error')
          }

          isSubmitting.value = false
        }, 800)
      } catch (error) {
        console.error('创建开销失败:', error)
        showSubmitModal.value = false
        showToast(error.response?.data?.message || '记录失败，请重试', 'error')
        isSubmitting.value = false
      }
    }

    // 加载开销列表
    const loadExpenses = async () => {
      const tripId = route.params.tripId
      if (!tripId) {
        showToast('缺少行程ID', 'error')
        return
      }

      try {
        const response = await apiClient.get(`/trips/${tripId}/expenses`)
        if (response.success) {
          expenses.value = response.data || []
        } else {
          showToast(response.message || '加载开销列表失败', 'error')
        }
      } catch (error) {
        console.error('加载开销列表失败:', error)
        showToast('加载失败，请刷新页面重试', 'error')
      }
    }

    onMounted(() => {
      document.body.style.overflow = 'hidden'
      loadExpenses()
    })

    onUnmounted(() => {
      document.body.style.overflow = ''
      
      // 清理录音资源
      if (audioRecorder && isRecording.value) {
        stopRecording()
      }
    })

    return {
      toastRef,
      textareaRef,
      expenses,
      expenseInput,
      isRecording,
      isSubmitting,
      showVoiceModal,
      showSubmitModal,
      totalExpense,
      goBack,
      formatAmount,
      formatTime,
      getCategoryKey,
      getCategoryIcon,
      getCategoryColor,
      getCategoryLabel,
      adjustTextareaHeight,
      toggleRecording,
      submitExpense
    }
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.expense-container {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 顶部导航栏 */
.expense-header {
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

/* 主内容区 */
.expense-content {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2vw;
  padding: 2vw;
  overflow: hidden;
}

/* 左侧开销列表区域 */
.expense-list-section {
  background: white;
  border-radius: 1.5vw;
  padding: 2vw;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1vw 2.5vw rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5vw;
  padding-bottom: 1vw;
  border-bottom: 0.15vw solid #f0f0f0;
}

.section-title {
  font-size: 1.6vw;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.expense-summary {
  display: flex;
  align-items: center;
  gap: 0.5vw;
}

.total-label {
  font-size: 1vw;
  color: #999;
  font-weight: 600;
}

.total-amount {
  font-size: 1.6vw;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* 开销列表 */
.expense-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 0.5vw;
}

.expense-list::-webkit-scrollbar {
  width: 0.5vw;
}

.expense-list::-webkit-scrollbar-track {
  background: #f0f0f0;
  border-radius: 0.25vw;
}

.expense-list::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 0.25vw;
}

.expense-list::-webkit-scrollbar-thumb:hover {
  background: #ccc;
}

/* 开销条目 */
.expense-item {
  display: flex;
  align-items: center;
  gap: 1.2vw;
  padding: 1.2vw;
  margin-bottom: 1vw;
  background: white;
  border-radius: 1vw;
  border-left: 0.4vw solid;
  box-shadow: 0 0.3vw 0.8vw rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.expense-item:hover {
  transform: translateX(0.5vw);
  box-shadow: 0 0.5vw 1.2vw rgba(0, 0, 0, 0.12);
}

/* 不同分类的颜色 */
.expense-item.category-transport {
  border-left-color: #667eea;
  background: linear-gradient(135deg, #fff 0%, rgba(102, 126, 234, 0.05) 100%);
}

.expense-item.category-hotel {
  border-left-color: #f093fb;
  background: linear-gradient(135deg, #fff 0%, rgba(240, 147, 251, 0.05) 100%);
}

.expense-item.category-sight {
  border-left-color: #4facfe;
  background: linear-gradient(135deg, #fff 0%, rgba(79, 172, 254, 0.05) 100%);
}

.expense-item.category-food {
  border-left-color: #43e97b;
  background: linear-gradient(135deg, #fff 0%, rgba(67, 233, 123, 0.05) 100%);
}

.expense-item.category-other {
  border-left-color: #fa709a;
  background: linear-gradient(135deg, #fff 0%, rgba(250, 112, 154, 0.05) 100%);
}

.expense-icon {
  width: 3.5vw;
  height: 3.5vw;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.8vw;
  flex-shrink: 0;
  box-shadow: 0 0.2vw 0.8vw rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.expense-item:hover .expense-icon {
  transform: scale(1.1);
  box-shadow: 0 0.4vw 1.2vw rgba(0, 0, 0, 0.15);
}

.expense-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5vw;
}

.expense-main {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 1vw;
}

.expense-description {
  font-size: 1.2vw;
  font-weight: 600;
  color: #333;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.expense-amount {
  font-size: 1.6vw;
  font-weight: 800;
  color: #333;
  flex-shrink: 0;
}

.expense-meta {
  display: flex;
  gap: 1vw;
  font-size: 0.9vw;
  color: #aaa;
  align-items: center;
}

.expense-category {
  padding: 0.25vw 0.7vw;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 0.4vw;
  font-weight: 500;
  color: #888;
}

.expense-time {
  color: #bbb;
  font-weight: 400;
}

/* 列表动画 */
.expense-item-enter-active {
  animation: slide-in-down 0.4s ease-out;
}

@keyframes slide-in-down {
  from {
    opacity: 0;
    transform: translateY(-2vw);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.empty-icon {
  font-size: 5vw;
  margin-bottom: 1.5vw;
  opacity: 0.5;
}

.empty-text {
  font-size: 1.3vw;
  font-weight: 600;
  margin: 0 0 0.5vw 0;
}

.empty-hint {
  font-size: 1vw;
  margin: 0;
  opacity: 0.7;
}

/* 右侧输入区域 */
.input-section {
  display: flex;
  flex-direction: column;
}

.input-card {
  background: white;
  border-radius: 1.5vw;
  padding: 2vw;
  box-shadow: 0 1vw 2.5vw rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  height: 100%;
}

.input-title {
  font-size: 1.6vw;
  font-weight: 700;
  color: #333;
  margin: 0 0 1.5vw 0;
}

/* 输入提示 */
.input-hint {
  display: flex;
  gap: 1vw;
  padding: 1.2vw;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8f4f8 100%);
  border-radius: 1vw;
  margin-bottom: 1.5vw;
  border-left: 0.4vw solid #667eea;
}

.hint-icon {
  font-size: 1.8vw;
  flex-shrink: 0;
}

.hint-text {
  flex: 1;
}

.hint-text p {
  font-size: 1vw;
  color: #666;
  margin: 0 0 0.6vw 0;
  font-weight: 600;
}

.hint-text ul {
  margin: 0;
  padding-left: 1.5vw;
  font-size: 0.9vw;
  color: #999;
}

.hint-text li {
  margin-bottom: 0.3vw;
}

/* 输入容器 */
.input-container {
  position: relative;
  margin-bottom: 1.5vw;
}

.textarea-wrapper {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 1vw;
}

.expense-input {
  flex: 1;
  min-height: 12vw;
  max-height: 20vw;
  padding: 1.2vw;
  border: 0.15vw solid #e0e0e0;
  border-radius: 1vw;
  font-size: 1.1vw;
  font-family: inherit;
  resize: none;
  transition: all 0.3s ease;
  background: #fafafa;
  box-sizing: border-box;
}

.expense-input:focus {
  outline: none;
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 0.3vw rgba(102, 126, 234, 0.1);
}

.expense-input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.expense-input::placeholder {
  color: #bbb;
}

/* 语音按钮（右下角） */
.voice-button-inline {
  position: absolute;
  right: 1vw;
  bottom: 1vw;
  width: 2.5vw;
  height: 2.5vw;
  background: transparent;
  border: 0.125vw solid #ccc;
  border-radius: 0.5vw;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  z-index: 10;
}

.voice-button-inline:hover:not(:disabled) {
  background: rgba(102, 126, 234, 0.05);
  border-color: #667eea;
  color: #667eea;
}

.voice-button-inline:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.voice-button-inline.recording {
  background: rgba(231, 76, 60, 0.1);
  border-color: #e74c3c;
  color: #e74c3c;
  animation: recording-pulse-inline 1.5s ease-in-out infinite;
}

@keyframes recording-pulse-inline {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(231, 76, 60, 0.4);
  }
  50% {
    box-shadow: 0 0 0 0.5vw rgba(231, 76, 60, 0);
  }
}

.mic-icon,
.stop-icon {
  width: 1.25vw;
  height: 1.25vw;
}

/* 录音提示 */
.recording-hint {
  display: flex;
  align-items: center;
  gap: 0.6vw;
  padding: 0.8vw 1.2vw;
  background: rgba(245, 87, 108, 0.1);
  border-radius: 0.8vw;
  font-size: 0.95vw;
  color: #F5576C;
  margin-bottom: 1vw;
}

.recording-dot {
  width: 0.6vw;
  height: 0.6vw;
  border-radius: 50%;
  background: #F5576C;
  animation: blink 1s ease-in-out infinite;
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}

/* 提交按钮 */
.submit-button {
  padding: 1.2vw;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 1vw;
  font-size: 1.2vw;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.8vw;
  box-shadow: 0 0.5vw 1.5vw rgba(102, 126, 234, 0.3);
}

.submit-button:hover:not(:disabled) {
  transform: translateY(-0.3vw);
  box-shadow: 0 0.8vw 2vw rgba(102, 126, 234, 0.4);
}

.submit-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.submit-icon {
  font-size: 1.5vw;
}

.submit-icon.rotating {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
}

.modal-content {
  background: white;
  border-radius: 2vw;
  padding: 3vw;
  max-width: 35vw;
  text-align: center;
  box-shadow: 0 1.5vw 4vw rgba(0, 0, 0, 0.3);
  animation: modal-in 0.3s ease-out;
}

@keyframes modal-in {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(2vw);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 语音动画 */
.voice-animation {
  position: relative;
  width: 12vw;
  height: 12vw;
  margin: 0 auto 2vw auto;
}

.wave-circle {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  border: 0.3vw solid #667eea;
  opacity: 0;
  animation: wave-pulse 2s ease-out infinite;
}

.wave-1 {
  width: 8vw;
  height: 8vw;
  animation-delay: 0s;
}

.wave-2 {
  width: 10vw;
  height: 10vw;
  animation-delay: 0.6s;
}

.wave-3 {
  width: 12vw;
  height: 12vw;
  animation-delay: 1.2s;
}

@keyframes wave-pulse {
  0% {
    transform: translate(-50%, -50%) scale(0.5);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) scale(1.2);
    opacity: 0;
  }
}

.microphone-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 3.5vw;
  animation: mic-bounce 1s ease-in-out infinite;
}

@keyframes mic-bounce {
  0%, 100% {
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    transform: translate(-50%, -50%) scale(1.1);
  }
}

/* 金币动画 */
.coin-animation {
  position: relative;
  width: 10vw;
  height: 10vw;
  margin: 0 auto 2vw auto;
}

.wallet-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 4vw;
  animation: wallet-shake 0.5s ease-in-out infinite;
}

@keyframes wallet-shake {
  0%, 100% {
    transform: translate(-50%, -50%) rotate(-5deg);
  }
  50% {
    transform: translate(-50%, -50%) rotate(5deg);
  }
}

.coin {
  position: absolute;
  font-size: 2vw;
  opacity: 0;
  animation: coin-fly 1.5s ease-in-out infinite;
}

.coin-1 {
  top: -1vw;
  left: 50%;
  animation-delay: 0s;
}

.coin-2 {
  top: -1vw;
  left: 50%;
  animation-delay: 0.5s;
}

.coin-3 {
  top: -1vw;
  left: 50%;
  animation-delay: 1s;
}

@keyframes coin-fly {
  0% {
    transform: translate(-50%, 0) scale(0.5) rotate(0deg);
    opacity: 1;
  }
  50% {
    transform: translate(-50%, -3vw) scale(1) rotate(180deg);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, 5vw) scale(0.8) rotate(360deg);
    opacity: 0;
  }
}

/* 弹窗文字 */
.modal-title {
  font-size: 1.6vw;
  font-weight: 700;
  color: #333;
  margin: 0 0 0.8vw 0;
}

.modal-subtitle {
  font-size: 1.1vw;
  color: #999;
  margin: 0;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-active .modal-content {
  animation: modal-in 0.3s ease-out;
}

.modal-fade-leave-active .modal-content {
  animation: modal-out 0.3s ease-in;
}

@keyframes modal-out {
  from {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
  to {
    opacity: 0;
    transform: scale(0.9) translateY(2vw);
  }
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .expense-content {
    grid-template-columns: 1fr;
    grid-template-rows: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .expense-content {
    grid-template-columns: 1fr;
    gap: 1.5vw;
  }

  .expense-list-section,
  .input-card {
    padding: 1.5vw;
  }
}
</style>