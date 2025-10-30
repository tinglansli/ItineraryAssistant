import axios from 'axios'

// 创建 axios 实例
// TODO: Docker 打包时改为 process.env.VUE_APP_API_BASE_URL
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
    // 401 未授权，清除 token 并跳转到登录页
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/auth'
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
