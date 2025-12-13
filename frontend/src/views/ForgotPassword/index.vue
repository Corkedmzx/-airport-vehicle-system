<template>
  <div class="forgot-password-page">
    <div class="forgot-password-container">
      <div class="forgot-password-box">
        <h1 class="system-title">机场车辆监控系统</h1>
        <h2 class="page-title">账户找回</h2>
        
        <el-form
          v-if="step === 1"
          ref="forgotFormRef"
          :model="forgotForm"
          :rules="forgotRules"
          class="forgot-form"
        >
          <el-form-item prop="phone">
            <el-input
              v-model="forgotForm.phone"
              placeholder="请输入注册时使用的手机号"
              clearable
              size="large"
            >
              <template #prefix>
                <el-icon><Phone /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleForgotPassword"
            class="submit-button"
          >
            找回账户
          </el-button>
        </el-form>

        <el-form
          v-else-if="step === 2"
          ref="resetFormRef"
          :model="resetForm"
          :rules="resetRules"
          class="reset-form"
        >
          <el-alert
            type="success"
            :closable="false"
            show-icon
            style="margin-bottom: 20px"
          >
            账户找回成功！请设置新密码
          </el-alert>
          
          <el-form-item prop="username">
            <el-input
              v-model="resetForm.username"
              placeholder="请输入用户名"
              clearable
              size="large"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="resetForm.password"
              type="password"
              placeholder="请输入新密码（至少6位）"
              show-password
              clearable
              size="large"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="resetForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password
              clearable
              size="large"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleResetPassword"
            class="submit-button"
          >
            设置新密码
          </el-button>
        </el-form>

        <div class="footer-links">
          <el-link type="primary" @click="$router.push('/login')">
            返回登录
          </el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Phone, User, Lock } from '@element-plus/icons-vue'
import { forgotPasswordApi, resetPasswordApi } from '@/api/auth'

const router = useRouter()
const forgotFormRef = ref<FormInstance>()
const resetFormRef = ref<FormInstance>()
const loading = ref(false)
const step = ref(1) // 1: 找回账户, 2: 设置新密码
const username = ref('')

const forgotForm = reactive({
  phone: ''
})

const resetForm = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

// 验证密码
const validatePassword = (rule: any, value: string, callback: Function) => {
  if (!value) {
    callback(new Error('请输入新密码'))
    return
  }
  if (value.length < 6) {
    callback(new Error('密码长度至少6位'))
    return
  }
  callback()
}

// 验证确认密码
const validateConfirmPassword = (rule: any, value: string, callback: Function) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
    return
  }
  if (value !== resetForm.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const forgotRules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const resetRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 找回账户
const handleForgotPassword = async () => {
  if (!forgotFormRef.value) return
  try {
    const valid = await forgotFormRef.value.validate()
    if (!valid) return
    
    loading.value = true
    const response = await forgotPasswordApi({ phone: forgotForm.phone })
    
    if (response.data.code === 200) {
      // 从响应中获取用户名
      if (response.data.data && response.data.data.username) {
        resetForm.username = response.data.data.username
        ElMessage.success('账户找回成功，请设置新密码')
        step.value = 2
      } else {
        // 如果没有返回用户名，让用户输入
        ElMessage.success('账户找回成功，请设置新密码')
        step.value = 2
        ElMessage.info('请输入您的用户名以设置新密码')
      }
    } else {
      ElMessage.error(response.data.message || '账户找回失败')
    }
  } catch (error: any) {
    console.error('Forgot password error:', error)
    ElMessage.error(error?.response?.data?.message || '账户找回失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 设置新密码
const handleResetPassword = async () => {
  if (!resetFormRef.value) return
  try {
    const valid = await resetFormRef.value.validate()
    if (!valid) return
    
    loading.value = true
    const response = await resetPasswordApi({
      username: resetForm.username,
      password: resetForm.password
    })
    
    if (response.data.code === 200) {
      ElMessage.success('密码设置成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(response.data.message || '密码设置失败')
    }
  } catch (error: any) {
    console.error('Reset password error:', error)
    ElMessage.error(error?.response?.data?.message || '密码设置失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.forgot-password-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  
  .forgot-password-container {
    width: 100%;
    max-width: 450px;
    padding: 20px;
    
    .forgot-password-box {
      background: white;
      border-radius: 12px;
      padding: 40px;
      box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
      
      .system-title {
        font-size: 24px;
        font-weight: bold;
        text-align: center;
        color: #333;
        margin-bottom: 10px;
      }
      
      .page-title {
        font-size: 20px;
        text-align: center;
        color: #666;
        margin-bottom: 30px;
      }
      
      .forgot-form,
      .reset-form {
        .submit-button {
          width: 100%;
          margin-top: 10px;
        }
      }
      
      .footer-links {
        text-align: center;
        margin-top: 20px;
      }
    }
  }
}
</style>

