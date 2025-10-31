import axios from 'axios'
import router from '@/router'

// 创建 axios 实例
const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 自动添加 Token
apiClient.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 统一处理响应
apiClient.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    // 401 未授权，清除 token
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      
      // 使用 router.push 而不是 window.location.href，避免页面刷新
      // 但只在不是登录页时跳转
      if (router.currentRoute.value.path !== '/auth') {
        router.push('/auth')
      }
    }
    return Promise.reject(error)
  }
)

/**
 * 用户登录
 * @param {string} username - 用户名
 * @param {string} password - 密码
 * @returns {Promise} 返回登录结果
 */
export const login = (username, password) => {
  return apiClient.post('/users/login', {
    username,
    password
  })
}

/**
 * 用户注册
 * @param {string} username - 用户名
 * @param {string} password - 密码
 * @returns {Promise} 返回注册结果
 */
export const register = (username, password) => {
  return apiClient.post('/users/register', {
    username,
    password
  })
}

/**
 * 获取当前用户信息
 * @returns {Promise} 返回用户信息
 */
export const getCurrentUser = () => {
  return apiClient.get('/users/me')
}

export default apiClient