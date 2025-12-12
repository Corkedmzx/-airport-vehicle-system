import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

// 创建axios实例
const service: AxiosInstance = axios.create({
  baseURL: (import.meta as any).env?.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 显示进度条
    NProgress.start()
    
    const userStore = useUserStore()
    
    // 添加token
    if (userStore.token) {
      config.headers = config.headers || ({} as any)
      ;(config.headers as Record<string, any>)['Authorization'] = `Bearer ${userStore.token}`
    }
    
    return config
  },
  (error) => {
    NProgress.done()
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    NProgress.done()
    
    const { data } = response
    
    // 如果是文件下载，直接返回
    if (response.config.responseType === 'blob') {
      return response
    }
    
    // 业务状态码处理
    if (data.code === 200) {
      return response
    } else if (data.code === 401) {
      // token过期或无效
      ElMessage.error('登录已过期，请重新登录')
      const userStore = useUserStore()
      userStore.logout()
      router.push('/login')
      return Promise.reject(new Error('登录已过期'))
    } else if (data.code === 403) {
      ElMessage.error('没有权限访问该资源')
      return Promise.reject(new Error('没有权限'))
    } else if (data.code === 404) {
      ElMessage.error('请求的资源不存在')
      return Promise.reject(new Error('资源不存在'))
    } else {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || '请求失败'))
    }
  },
  (error) => {
    NProgress.done()
    
    let message = '网络错误'
    
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 400:
          message = data.message || '请求参数错误'
          break
        case 401:
          message = '未授权，请重新登录'
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求资源不存在'
          break
        case 500:
          message = '服务器内部错误'
          break
        default:
          message = `连接错误${status}`
      }
    } else if (error.request) {
      message = '网络连接超时'
    } else {
      message = error.message || '请求失败'
    }
    
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service