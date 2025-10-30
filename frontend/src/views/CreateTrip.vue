<template>
  <div class="create-trip-container">
    <!-- Toast 通知组件 -->
    <Toast ref="toastRef" />
    
    <!-- 顶部导航栏 -->
    <div class="trip-header">
      <button @click="goBack" class="back-button">
        <span class="back-icon">←</span>
        <span>返回</span>
      </button>
      <h1 class="page-title">✨ 创建您的专属行程</h1>
      <button @click="showPreferenceModal = true" class="preference-button">
        <span class="preference-icon">⚙️</span>
        <span>编辑偏好</span>
      </button>
    </div>

    <!-- 主内容区 -->
    <div class="trip-content">
      <!-- 提示卡片 -->
      <div class="hint-card">
        <div class="hint-icon">💡</div>
        <div class="hint-text">
          <p class="hint-title">告诉我您的旅行计划</p>
          <p class="hint-desc">请描述旅行目的地、日期、预算、同行人数、偏好等信息，让AI为您定制专属行程</p>
          <p class="hint-note">💡 提示：行程天数越长，AI 生成时间越久（约 45-60 秒），请耐心等待</p>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-section">
        <div class="input-wrapper">
          <!-- 文本输入框（内含语音按钮） -->
          <div class="input-container">
            <textarea
              v-model="userInput"
              class="trip-input"
              placeholder="例如：我想在2025年5月去云南旅游5天，预算5000元，两个大人一个小孩，我不喜欢爬山，喜欢美食..."
              :disabled="isRecording"
              @input="adjustTextareaHeight"
              ref="textareaRef"
            ></textarea>
            
            <!-- 内置语音按钮 -->
            <button
              @click="toggleRecording"
              class="voice-button-inline"
              :class="{ recording: isRecording }"
              :title="isRecording ? '完成录音' : '语音输入'"
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

          <!-- 提交按钮 -->
          <button
            @click="submitTrip"
            class="submit-button"
            :disabled="!userInput.trim() || isGenerating"
          >
            <span v-if="!isGenerating" class="submit-icon">🚀</span>
            <span v-else class="submit-icon rotating">⚙️</span>
          </button>
        </div>

        <!-- 录音提示 -->
        <transition name="fade">
          <div v-if="isRecording" class="recording-hint">
            <span class="recording-dot"></span>
            <span>正在录音中，请说出您的旅行计划...</span>
          </div>
        </transition>
      </div>
    </div>

    <!-- 偏好设置弹窗 -->
    <transition name="modal-fade">
      <div v-if="showPreferenceModal" class="modal-overlay" @click.self="showPreferenceModal = false">
        <div class="modal-container">
          <div class="modal-header">
            <h2>⚙️ 编辑我的偏好</h2>
            <button @click="showPreferenceModal = false" class="modal-close">✕</button>
          </div>
          <div class="modal-body">
            <p class="modal-hint">请输入您的旅行偏好，多个偏好请用分号（;）分隔</p>
            <textarea
              v-model="preferences"
              class="preference-input"
              placeholder="例如：喜欢自然风光；偏好当地美食；避免过度商业化的景点；喜欢摄影"
            ></textarea>
          </div>
          <div class="modal-footer">
            <button @click="showPreferenceModal = false" class="modal-button cancel">取消</button>
            <button @click="savePreferences" class="modal-button confirm" :disabled="isSavingPreference">
              {{ isSavingPreference ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 生成进度弹窗 -->
    <transition name="modal-fade">
      <div v-if="isGenerating" class="modal-overlay">
        <div class="progress-modal">
          <div class="progress-icon-wrapper">
            <div class="progress-icon">🌍</div>
          </div>
          <h2 class="progress-title">{{ generatingMessage }}</h2>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progress + '%' }"></div>
          </div>
          <p class="progress-hint">AI 正在为您精心规划行程，请稍候...</p>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import apiClient from '@/api/auth'
import Toast from '@/components/Toast.vue'

export default {
  name: 'CreateTripView',
  components: {
    Toast
  },
  setup() {
    const router = useRouter()
    
    // 响应式数据
    const userInput = ref('')
    const textareaRef = ref(null)
    const toastRef = ref(null)
    const isRecording = ref(false)
    const isGenerating = ref(false)
    const generatingMessage = ref('正在生成行程')
    const progress = ref(0)
    const showPreferenceModal = ref(false)
    const preferences = ref('')
    const isSavingPreference = ref(false)
    
    // 语音相关
    let mediaRecorder = null
    let audioChunks = []
    let progressInterval = null

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

    // 自动调整文本框高度
    const adjustTextareaHeight = () => {
      nextTick(() => {
        const textarea = textareaRef.value
        if (textarea) {
          textarea.style.height = 'auto'
          textarea.style.height = Math.min(textarea.scrollHeight, 300) + 'px'
        }
      })
    }

    // 切换录音状态
    const toggleRecording = async () => {
      if (isRecording.value) {
        // 停止录音
        stopRecording()
      } else {
        // 开始录音
        startRecording()
      }
    }

    // 开始录音
    const startRecording = async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
        mediaRecorder = new MediaRecorder(stream)
        audioChunks = []

        mediaRecorder.ondataavailable = (event) => {
          audioChunks.push(event.data)
        }

        mediaRecorder.onstop = async () => {
          const audioBlob = new Blob(audioChunks, { type: 'audio/wav' })
          await transcribeAudio(audioBlob)
          
          // 停止所有音轨
          stream.getTracks().forEach(track => track.stop())
        }

        mediaRecorder.start()
        isRecording.value = true
      } catch (error) {
        console.error('录音失败:', error)
        alert('无法访问麦克风，请检查权限设置')
      }
    }

    // 停止录音
    const stopRecording = () => {
      if (mediaRecorder && mediaRecorder.state !== 'inactive') {
        mediaRecorder.stop()
        isRecording.value = false
      }
    }

    // 语音转文字
    const transcribeAudio = async (audioBlob) => {
      try {
        const formData = new FormData()
        formData.append('audio', audioBlob, 'recording.wav')

        const response = await apiClient.post('/speech/transcribe', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })

        if (response.success) {
          userInput.value = response.data
          adjustTextareaHeight()
          showToast('语音识别成功！', 'success')
        } else {
          showToast(response.message || '语音识别失败', 'error')
        }
      } catch (error) {
        console.error('语音转文字失败:', error)
        showToast('语音识别失败，请重试或直接输入文字', 'error')
      }
    }

    // 提交行程
    const submitTrip = async () => {
      if (!userInput.value.trim()) {
        return
      }

      isGenerating.value = true
      progress.value = 0
      generatingMessage.value = '正在分析您的需求'

      // 模拟进度条 - 放慢速度以匹配实际生成时间（30-60秒）
      let currentProgress = 0
      progressInterval = setInterval(() => {
        if (currentProgress < 85) {
          currentProgress += Math.random() * 3  // 从15降到3，大幅放慢速度
          progress.value = Math.min(currentProgress, 85)
          
          if (progress.value > 20 && progress.value < 50) {
            generatingMessage.value = '正在规划最佳路线'
          } else if (progress.value >= 50) {
            generatingMessage.value = '正在优化行程细节'
          }
        }
      }, 1000)  // 从500ms改为1000ms

      try {
        const response = await apiClient.post('/trips', {
          userInput: userInput.value
        })

        if (response.success) {
          // 完成进度
          progress.value = 100
          generatingMessage.value = '行程生成完成！'
          
          // 清除定时器
          clearInterval(progressInterval)
          
          console.log('API 返回数据:', response.data)
          
          // 延迟跳转，让用户看到完成状态
          setTimeout(() => {
            isGenerating.value = false
            // 跳转到行程详情页，携带行程数据
            // 注意：后端返回的是 Trip 对象，tripId 在对象内部
            const tripId = response.data.tripId || response.data.id
            router.push({
              name: 'TripDetail',
              params: { tripId: tripId },
              state: { tripData: response.data }
            })
          }, 800)
        } else {
          throw new Error(response.message || '生成失败')
        }
      } catch (error) {
        console.error('生成行程失败:', error)
        clearInterval(progressInterval)
        isGenerating.value = false
        
        const errorMsg = error.response?.data?.message || error.message || '生成失败，请重试'
        showToast(errorMsg, 'error')
      }
    }

    // 加载用户偏好
    const loadPreferences = async () => {
      try {
        const response = await apiClient.get('/users/preferences')
        if (response.success) {
          preferences.value = response.data || ''
        }
      } catch (error) {
        console.error('加载偏好失败:', error)
      }
    }

    // 保存用户偏好
    const savePreferences = async () => {
      isSavingPreference.value = true
      try {
        const response = await apiClient.put('/users/preferences', {
          preferences: preferences.value
        })

        if (response.success) {
          showToast('偏好更新成功！', 'success')
          showPreferenceModal.value = false
        } else {
          throw new Error(response.message || '保存失败')
        }
      } catch (error) {
        console.error('保存偏好失败:', error)
        showToast(error.response?.data?.message || error.message || '保存失败，请重试', 'error')
      } finally {
        isSavingPreference.value = false
      }
    }

    // 组件挂载时加载偏好
    onMounted(() => {
      loadPreferences()
    })

    // 组件卸载时清理
    onUnmounted(() => {
      if (progressInterval) {
        clearInterval(progressInterval)
      }
      if (isRecording.value) {
        stopRecording()
      }
    })

    return {
      userInput,
      textareaRef,
      toastRef,
      isRecording,
      isGenerating,
      generatingMessage,
      progress,
      showPreferenceModal,
      preferences,
      isSavingPreference,
      goBack,
      adjustTextareaHeight,
      toggleRecording,
      submitTrip,
      savePreferences
    }
  }
}
</script>

<style scoped>
.create-trip-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 2vw;
}

/* 顶部导航栏 */
.trip-header {
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
.preference-button {
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
.preference-button:hover {
  background: rgba(255, 255, 255, 0.25);
  border-color: white;
  transform: translateY(-0.1vw);
  box-shadow: 0 0.3vw 0.8vw rgba(0, 0, 0, 0.15);
}

.back-icon,
.preference-icon {
  font-size: 1.25vw;
}

/* 主内容区 */
.trip-content {
  max-width: 85vw;  /* 从75vw增加到85vw，减少两侧空白 */
  margin: 3vw auto;
}

/* 提示卡片 */
.hint-card {
  display: flex;
  align-items: flex-start;
  gap: 1.5vw;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 1.5vw;
  padding: 2vw;
  margin-bottom: 2vw;
  box-shadow: 0 0.75vw 2vw rgba(0, 0, 0, 0.15);
}

.hint-icon {
  font-size: 3vw;
  flex-shrink: 0;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

.hint-text {
  flex: 1;
}

.hint-title {
  font-size: 1.5vw;
  font-weight: 700;
  color: #333;
  margin: 0 0 0.5vw 0;
}

.hint-desc {
  font-size: 1vw;
  color: #666;
  margin: 0 0 0.5vw 0;
  line-height: 1.6;
}

.hint-note {
  font-size: 0.9vw;
  color: #999;
  margin: 0;
  font-style: italic;
  line-height: 1.6;
}

/* 输入区域 */
.input-section {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 1.5vw;
  padding: 2vw;
  box-shadow: 0 0.75vw 2vw rgba(0, 0, 0, 0.15);
}

.input-wrapper {
  display: flex;
  gap: 1.5vw;
  align-items: center;  /* 改为center，让按钮和输入框中线对齐 */
}

/* 输入框容器 */
.input-container {
  flex: 1;
  position: relative;
}

.trip-input {
  width: 100%;
  min-height: 10vw;
  max-height: 20vw;
  padding: 1.25vw 4vw 1.25vw 1.25vw;  /* 右侧留出空间给语音按钮 */
  font-size: 1vw;
  border: 0.15vw solid #e0e0e0;
  border-radius: 1vw;
  outline: none;
  resize: none;
  font-family: inherit;
  line-height: 1.6;
  transition: all 0.3s ease;
  box-sizing: border-box;
}

.trip-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 0.25vw rgba(102, 126, 234, 0.1);
}

.trip-input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.trip-input::placeholder {
  color: #aaa;
}

/* 内置语音按钮 */
.voice-button-inline {
  position: absolute;
  right: 1vw;
  top: 1vw;
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
}

.voice-button-inline:hover {
  background: rgba(102, 126, 234, 0.05);
  border-color: #667eea;
  color: #667eea;
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

/* 提交按钮 - 圆形设计 */
.submit-button {
  width: 5vw;
  height: 5vw;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 0.5vw 1.5vw rgba(102, 126, 234, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.submit-button:hover:not(:disabled) {
  transform: scale(1.1);
  box-shadow: 0 0.75vw 2vw rgba(102, 126, 234, 0.6);
}

.submit-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.submit-icon {
  font-size: 2.25vw;
  display: flex;
  align-items: center;
  justify-content: center;
}

.submit-icon.rotating {
  animation: rotate-icon 2s linear infinite;
}

@keyframes rotate-icon {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 录音提示 */
.recording-hint {
  display: flex;
  align-items: center;
  gap: 0.75vw;
  margin-top: 1vw;
  padding: 0.75vw 1.25vw;
  background: rgba(231, 76, 60, 0.1);
  border-left: 0.25vw solid #e74c3c;
  border-radius: 0.5vw;
  color: #e74c3c;
  font-size: 0.9vw;
}

.recording-dot {
  width: 0.6vw;
  height: 0.6vw;
  background: #e74c3c;
  border-radius: 50%;
  animation: blink 1s ease-in-out infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* 弹窗样式 */
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
  z-index: 1000;
  backdrop-filter: blur(5px);
}

.modal-container {
  width: 40vw;
  background: white;
  border-radius: 1.5vw;
  box-shadow: 0 1.5vw 4vw rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5vw 2vw;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.modal-header h2 {
  font-size: 1.5vw;
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  color: white;
  font-size: 1.75vw;
  cursor: pointer;
  width: 2vw;
  height: 2vw;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.modal-close:hover {
  background: rgba(255, 255, 255, 0.2);
}

.modal-body {
  padding: 2vw;
}

.modal-hint {
  font-size: 0.9vw;
  color: #666;
  margin: 0 0 1vw 0;
}

.preference-input {
  width: 100%;
  min-height: 8vw;
  padding: 1vw;
  font-size: 1vw;
  border: 0.15vw solid #e0e0e0;
  border-radius: 0.75vw;
  outline: none;
  resize: vertical;
  font-family: inherit;
  line-height: 1.6;
  box-sizing: border-box;
}

.preference-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 0.2vw rgba(102, 126, 234, 0.1);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1vw;
  padding: 1.5vw 2vw;
  background: #f8f8f8;
}

.modal-button {
  padding: 0.75vw 2vw;
  font-size: 1vw;
  font-weight: 600;
  border: none;
  border-radius: 0.75vw;
  cursor: pointer;
  transition: all 0.3s ease;
}

.modal-button.cancel {
  background: #e0e0e0;
  color: #666;
}

.modal-button.cancel:hover {
  background: #d0d0d0;
}

.modal-button.confirm {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.modal-button.confirm:hover:not(:disabled) {
  transform: translateY(-0.1vw);
  box-shadow: 0 0.3vw 0.8vw rgba(102, 126, 234, 0.4);
}

.modal-button.confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 生成进度弹窗 */
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
  font-weight: 700;
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
  border-radius: 0.3vw;
}

.progress-hint {
  font-size: 0.9vw;
  color: #999;
  margin: 0;
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

.modal-fade-enter-active .modal-container,
.modal-fade-enter-active .progress-modal {
  animation: modal-slide-in 0.3s ease;
}

.modal-fade-leave-active .modal-container,
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
