<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <div class="logo">
          <el-icon :size="32" color="#409EFF">
            <Van />
          </el-icon>
          <span class="title">机场车辆监控系统</span>
        </div>
        <p class="subtitle">Airport Vehicle Monitoring System</p>
      </div>
      
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        size="large"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            clearable
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <p>© 2024 机场车辆监控系统. All Rights Reserved.</p>
      </div>
    </div>
    
    <div class="background-decoration">
      <div class="deco-item deco-1"></div>
      <div class="deco-item deco-2"></div>
      <div class="deco-item deco-3"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/store/user'
import type { LoginRequest } from '@/api/auth/types'

const router = useRouter()
const userStore = useUserStore()

// 表单引用
const loginFormRef = ref<FormInstance>()

// 加载状态
const loading = ref(false)

// 表单数据
const loginForm = reactive<LoginRequest>({
  username: '',
  password: ''
})

// 表单验证规则
const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  try {
    const valid = await loginFormRef.value.validate()
    if (!valid) return
    
    loading.value = true
    const success = await userStore.login(loginForm)
    
    if (success) {
      router.push('/')
    }
  } catch (error) {
    console.error('Login validation error:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
  
  .background-decoration {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    
    .deco-item {
      position: absolute;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.1);
      animation: float 6s ease-in-out infinite;
      
      &.deco-1 {
        width: 200px;
        height: 200px;
        top: 10%;
        left: 10%;
        animation-delay: 0s;
      }
      
      &.deco-2 {
        width: 150px;
        height: 150px;
        top: 60%;
        right: 15%;
        animation-delay: 2s;
      }
      
      &.deco-3 {
        width: 100px;
        height: 100px;
        bottom: 20%;
        left: 20%;
        animation-delay: 4s;
      }
    }
  }
  
  @keyframes float {
    0%, 100% { transform: translateY(0px); }
    50% { transform: translateY(-20px); }
  }
}

.login-box {
  width: 420px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 1;
  
  .login-header {
    text-align: center;
    margin-bottom: 40px;
    
    .logo {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      margin-bottom: 8px;
      
      .title {
        font-size: 24px;
        font-weight: 600;
        color: #303133;
      }
    }
    
    .subtitle {
      font-size: 14px;
      color: #909399;
      letter-spacing: 1px;
    }
  }
  
  .login-form {
    .el-form-item {
      margin-bottom: 24px;
      
      .el-input__wrapper {
        box-shadow: 0 0 0 1px #dcdfe6 inset;
        border-radius: 8px;
        
        &:hover {
          box-shadow: 0 0 0 1px #c0c4cc inset;
        }
        
        &.is-focus {
          box-shadow: 0 0 0 1px #409eff inset;
        }
      }
    }
    
    .login-button {
      width: 100%;
      height: 44px;
      font-size: 16px;
      font-weight: 600;
      border-radius: 8px;
      background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
      border: none;
      transition: all 0.3s ease;
      
      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
      }
      
      &:active {
        transform: translateY(0);
      }
    }
  }
  
  .login-footer {
    text-align: center;
    margin-top: 32px;
    padding-top: 24px;
    border-top: 1px solid #ebeef5;
    
    p {
      font-size: 12px;
      color: #909399;
    }
  }
}

@media (max-width: 480px) {
  .login-container {
    padding: 20px;
    
    .login-box {
      width: 100%;
      max-width: 360px;
      padding: 30px 24px;
    }
  }
}
</style>