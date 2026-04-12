import axios from 'axios'
import { ElMessage } from 'element-plus'
import { API_BASE_URL } from '../config/api.js'

// 创建axios实例
const request = axios.create({
  baseURL: API_BASE_URL, // 使用相对路径 '/api'，Vite proxy 会自动转发到 vite.config.js 中配置的 target
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 从localStorage获取token
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

// 响应拦截器
request.interceptors.response.use(
  response => {
    const payload = response.data
    // 兼容两种返回：
    // 1) 统一业务包装 { code, msg, data }
    // 2) 直接返回业务数据，如 { jwt, username }
    if (payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'code')) {
      const { code, msg } = payload
      if (code === 1) {
        return payload
      }
      ElMessage.error(msg || '请求失败')
      return Promise.reject(new Error(msg || '请求失败'))
    }
    // 无 code 字段时，视为成功并包装为统一结构
    return { code: 1, msg: '', data: payload }
  },
  error => {
    // 网络错误或其他错误
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request

