import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, logoutApi } from '@/api/auth'
// @ts-ignore: ignore missing module/type declaration for .vue files
import type { LoginRequest, LoginResponse } from '@/api/auth/types'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<LoginResponse['user'] | null>(null)
  
  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  
  // 登录
  const login = async (data: LoginRequest) => {
    try {
      const response = await loginApi(data)
      if (response.data.code === 200) {
        const { token: newToken, user, expiresIn } = response.data.data
        
        // 保存token和用户信息
        token.value = newToken
        userInfo.value = user
        
        // 存储到本地
        localStorage.setItem('token', newToken)
        localStorage.setItem('userInfo', JSON.stringify(user))
        
        // 设置token过期时间
        if (expiresIn) {
          const expiryTime = Date.now() + expiresIn * 1000
          localStorage.setItem('tokenExpiry', expiryTime.toString())
        }
        
        ElMessage.success('登录成功')
        return true
      } else {
        ElMessage.error(response.data.message || '登录失败')
        return false
      }
    } catch (error) {
      console.error('Login error:', error)
      ElMessage.error('登录失败，请稍后重试')
      return false
    }
  }
  
  // 登出
  const logout = async () => {
    try {
      await logoutApi()
    } catch (error) {
      console.error('Logout error:', error)
    } finally {
      // 清除状态
      token.value = ''
      userInfo.value = null
      
      // 清除本地存储
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('tokenExpiry')
      
      ElMessage.success('已退出登录')
    }
  }
  
  // 检查token是否过期
  const checkTokenExpiry = () => {
    const expiryTime = localStorage.getItem('tokenExpiry')
    if (expiryTime && Date.now() > parseInt(expiryTime)) {
      logout()
      return false
    }
    return true
  }
  
  // 初始化用户信息
  const initUserInfo = () => {
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr && token.value) {
      try {
        userInfo.value = JSON.parse(userInfoStr)
      } catch (error) {
        console.error('Parse user info error:', error)
        localStorage.removeItem('userInfo')
      }
    }
  }
  
  // 恢复登录状态
  const restoreLogin = () => {
    if (token.value && checkTokenExpiry()) {
      initUserInfo()
      return true
    }
    return false
  }
  
  return {
    // 状态
    token,
    userInfo,
    // 计算属性
    isLoggedIn,
    // 方法
    login,
    logout,
    checkTokenExpiry,
    initUserInfo,
    restoreLogin
  }
})