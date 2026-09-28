// src/utils/request.ts
import axios from 'axios'

// 创建 axios 实例
const apiClient = axios.create({
  baseURL: '',  // 留空走 Vite 代理（见 vite.config.ts），避免跨域
  timeout: 60000
})

// 🔥 请求拦截器：自动添加 Token
apiClient.interceptors.request.use(
  config => {
    const token = localStorage.getItem('authToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

export default apiClient