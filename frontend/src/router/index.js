import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Auth from '../views/Auth.vue'
import CreateTrip from '../views/CreateTrip.vue'
import TripDetail from '../views/TripDetail.vue'
import TripList from '../views/TripList.vue'
import apiClient from '@/api/auth'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: Home,
    meta: { requiresAuth: true }
  },
  {
    path: '/auth',
    name: 'Auth',
    component: Auth,
    meta: { requiresAuth: false }
  },
  {
    path: '/create-trip',
    name: 'CreateTrip',
    component: CreateTrip,
    meta: { requiresAuth: true }
  },
  {
    path: '/trips',
    name: 'TripList',
    component: TripList,
    meta: { requiresAuth: true }
  },
  {
    path: '/trip/:tripId',
    name: 'TripDetail',
    component: TripDetail,
    meta: { requiresAuth: true }
  },
  {
    path: '/trip/:tripId/budget',
    name: 'BudgetAnalysis',
    component: () => import('../views/BudgetAnalysis.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/trip/:tripId/expense',
    name: 'ExpenseRecord',
    component: () => import('../views/ExpenseRecord.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes,
})

// Token 验证缓存，避免频繁调用接口
let tokenValidationCache = {
  isValid: false,
  timestamp: 0,
  token: null
}

// 缓存有效期 5 分钟
const CACHE_DURATION = 5 * 60 * 1000

/**
 * 验证 token 是否有效（带缓存）
 * @param {boolean} force - 是否强制验证，忽略缓存
 * @returns {Promise<boolean>}
 */
const validateToken = async (force = false) => {
  const token = localStorage.getItem('token')
  
  if (!token) {
    tokenValidationCache = { isValid: false, timestamp: 0, token: null }
    return false
  }

  const now = Date.now()
  
  // 如果 token 未变化且缓存未过期，返回缓存结果
  if (
    !force &&
    tokenValidationCache.token === token &&
    tokenValidationCache.timestamp > 0 &&
    (now - tokenValidationCache.timestamp) < CACHE_DURATION
  ) {
    return tokenValidationCache.isValid
  }

  // 调用后端接口验证 token
  try {
    const response = await apiClient.get('/users/me')
    const isValid = response.success === true
    
    // 更新缓存
    tokenValidationCache = {
      isValid,
      timestamp: now,
      token
    }
    
    return isValid
  } catch (error) {
    // 验证失败，清空缓存
    tokenValidationCache = { isValid: false, timestamp: 0, token: null }
    return false
  }
}

/**
 * 清除认证信息
 */
const clearAuth = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  tokenValidationCache = { isValid: false, timestamp: 0, token: null }
}

// 路由守卫 - 检查登录状态
router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('token')
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)

  // 如果目标页面不需要认证
  if (!requiresAuth) {
    // 如果是登录页且有 token，验证 token
    if (to.path === '/auth' && token) {
      const isValid = await validateToken()
      if (isValid) {
        // Token 有效，跳转到主页
        next('/home')
      } else {
        // Token 无效，清除并留在登录页
        clearAuth()
        next()
      }
    } else {
      next()
    }
    return
  }

  // 需要认证的页面
  if (!token) {
    // 没有 token，跳转到登录页
    next('/auth')
    return
  }

  // 有 token，验证其有效性
  const isValid = await validateToken()
  if (isValid) {
    // Token 有效，放行
    next()
  } else {
    // Token 无效，清除并跳转到登录页
    clearAuth()
    next('/auth')
  }
})

/**
 * 登出功能（供其他组件使用）
 */
export const logout = () => {
  clearAuth()
  router.push('/auth')
}

export default router