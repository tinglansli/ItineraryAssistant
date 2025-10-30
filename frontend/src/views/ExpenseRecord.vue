<template>
  <div class="placeholder-container">
    <div class="placeholder-card">
      <button @click="goBack" class="back-button">← 返回</button>
      <div class="placeholder-icon">💳</div>
      <h1>记录开销页面</h1>
      <p>该页面正在开发中，敬请期待</p>
    </div>
  </div>
</template>

<script>
import { useRouter } from 'vue-router'

export default {
  name: 'ExpenseRecordView',
  setup() {
    const router = useRouter()

    const goBack = () => {
      router.back()
    }

    return { goBack }
  }
}
</script>

<style scoped>
.placeholder-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2vw;
}

.placeholder-card {
  background: white;
  border-radius: 1.5vw;
  padding: 3vw;
  box-shadow: 0 1vw 3vw rgba(0, 0, 0, 0.2);
  text-align: center;
  max-width: 40vw;
}

.back-button {
  align-self: flex-start;
  padding: 0.75vw 1.5vw;
  background: #f0f0f0;
  border: none;
  border-radius: 0.75vw;
  font-size: 1vw;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 1.5vw;
}

.back-button:hover {
  background: #e0e0e0;
}

.placeholder-icon {
  font-size: 5vw;
  margin-bottom: 1vw;
}

.placeholder-card h1 {
  font-size: 2vw;
  color: #333;
  margin: 0 0 0.5vw 0;
}

.placeholder-card p {
  font-size: 1.1vw;
  color: #666;
  margin: 0;
}
</style>
