<template>
  <transition name="toast-fade">
    <div v-if="visible" class="toast-container" :class="type">
      <div class="toast-icon">{{ icon }}</div>
      <div class="toast-message">{{ message }}</div>
    </div>
  </transition>
</template>

<script>
import { ref } from 'vue'

export default {
  // eslint-disable-next-line vue/multi-word-component-names
  name: 'Toast',
  setup() {
    const visible = ref(false)
    const message = ref('')
    const type = ref('success') // success, error, info, warning
    let timer = null

    const iconMap = {
      success: '✓',
      error: '✕',
      info: 'ℹ',
      warning: '⚠'
    }

    const icon = ref(iconMap.success)

    const show = (msg, toastType = 'success', duration = 3000) => {
      message.value = msg
      type.value = toastType
      icon.value = iconMap[toastType]
      visible.value = true

      if (timer) {
        clearTimeout(timer)
      }

      timer = setTimeout(() => {
        visible.value = false
      }, duration)
    }

    return {
      visible,
      message,
      type,
      icon,
      show
    }
  }
}
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 2vw;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 0.75vw;
  padding: 1vw 2vw;
  border-radius: 1vw;
  box-shadow: 0 0.5vw 2vw rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
  z-index: 9999;
  font-size: 1vw;
  font-weight: 600;
  min-width: 15vw;
  max-width: 40vw;
}

.toast-icon {
  width: 1.5vw;
  height: 1.5vw;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1vw;
  flex-shrink: 0;
}

.toast-message {
  flex: 1;
}

/* 成功样式 */
.toast-container.success {
  background: rgba(46, 204, 113, 0.95);
  color: white;
}

.toast-container.success .toast-icon {
  background: rgba(255, 255, 255, 0.3);
}

/* 错误样式 */
.toast-container.error {
  background: rgba(231, 76, 60, 0.95);
  color: white;
}

.toast-container.error .toast-icon {
  background: rgba(255, 255, 255, 0.3);
}

/* 信息样式 */
.toast-container.info {
  background: rgba(52, 152, 219, 0.95);
  color: white;
}

.toast-container.info .toast-icon {
  background: rgba(255, 255, 255, 0.3);
}

/* 警告样式 */
.toast-container.warning {
  background: rgba(241, 196, 15, 0.95);
  color: white;
}

.toast-container.warning .toast-icon {
  background: rgba(255, 255, 255, 0.3);
}

/* 淡入淡出动画 */
.toast-fade-enter-active {
  animation: toast-in 0.3s ease-out;
}

.toast-fade-leave-active {
  animation: toast-out 0.3s ease-in;
}

@keyframes toast-in {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-2vw);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

@keyframes toast-out {
  from {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
  to {
    opacity: 0;
    transform: translateX(-50%) translateY(-2vw);
  }
}
</style>
