<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <div class="logo">
          <el-icon :size="32" color="#409EFF">
            <Van />
          </el-icon>
          <span class="title">机场车辆监控系统</span>
        </div>
        <p class="subtitle">用户注册</p>
      </div>
      
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="register-form"
        size="large"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            clearable
          />
        </el-form-item>
        
        <el-form-item prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="请输入邮箱"
            prefix-icon="Message"
            clearable
          />
        </el-form-item>
        
        <el-form-item prop="phone">
          <el-input
            v-model="registerForm.phone"
            :placeholder="getCurrentPhonePlaceholder()"
            clearable
          >
            <template #prepend>
              <el-select
                v-model="registerForm.phoneRegion"
                style="width: 130px"
                @change="handleRegionChange"
              >
                <el-option
                  v-for="region in phoneRegions"
                  :key="region.code"
                  :label="region.label"
                  :value="region.code"
                />
              </el-select>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>
        
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请确认密码"
            prefix-icon="Lock"
            show-password
            clearable
            @keyup.enter="handleRegister"
          />
        </el-form-item>
        
        <el-form-item prop="agreement">
          <el-checkbox v-model="registerForm.agreement">
            我已阅读并同意
            <el-link type="primary" :underline="false">《用户协议》</el-link>
            和
            <el-link type="primary" :underline="false">《隐私政策》</el-link>
          </el-checkbox>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            class="register-button"
            :loading="loading"
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '立即注册' }}
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="register-footer">
        <p>
          已有账号？
          <el-link type="primary" :underline="false" @click="goToLogin">立即登录</el-link>
        </p>
        <p class="copyright">© 2024 机场车辆监控系统. All Rights Reserved.</p>
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
import { registerApi } from '@/api/auth'
import type { RegisterRequest } from '@/api/types'

const router = useRouter()

// 表单引用
const registerFormRef = ref<FormInstance>()

// 加载状态
const loading = ref(false)

// 手机号地区选项
const phoneRegions = [
  { code: '+86', label: '中国大陆 (+86)', pattern: /^1[3-9]\d{9}$/, placeholder: '请输入11位手机号' },
  { code: '+852', label: '香港 (+852)', pattern: /^[5-9]\d{7}$/, placeholder: '请输入8位号码' },
  { code: '+853', label: '澳门 (+853)', pattern: /^6\d{7}$/, placeholder: '请输入8位号码' },
  { code: '+886', label: '台湾 (+886)', pattern: /^[0-9]{8,10}$/, placeholder: '请输入8-10位号码' },
  { code: '+1', label: '美国/加拿大 (+1)', pattern: /^[2-9]\d{2}[2-9]\d{2}\d{4}$/, placeholder: '请输入10位号码' },
  { code: '+44', label: '英国 (+44)', pattern: /^[1-9]\d{9,10}$/, placeholder: '请输入10-11位号码' },
  { code: '+81', label: '日本 (+81)', pattern: /^[789]0\d{8}$/, placeholder: '请输入11位号码' },
  { code: '+82', label: '韩国 (+82)', pattern: /^1[0-9]\d{7,8}$/, placeholder: '请输入9-10位号码' },
  { code: '+65', label: '新加坡 (+65)', pattern: /^[689]\d{7}$/, placeholder: '请输入8位号码' },
  { code: '+60', label: '马来西亚 (+60)', pattern: /^1[0-9]\d{7,8}$/, placeholder: '请输入9-10位号码' }
]

// 表单数据
const registerForm = reactive<RegisterRequest & { 
  confirmPassword: string; 
  agreement: boolean;
  phoneRegion: string;
}>({
  username: '',
  email: '',
  phone: '',
  phoneRegion: '+86',
  password: '',
  confirmPassword: '',
  agreement: false
})

// 密码验证函数
const validatePassword = (rule: any, value: string, callback: Function) => {
  if (value === '') {
    callback(new Error('请输入密码'))
  } else if (value.length < 6) {
    callback(new Error('密码长度不能少于6位'))
  } else if (value.length > 20) {
    callback(new Error('密码长度不能超过20位'))
  } else if (!/^(?=.*[a-zA-Z])(?=.*\d).+$/.test(value)) {
    callback(new Error('密码必须包含字母和数字'))
  } else {
    callback()
  }
}

// 确认密码验证函数
const validateConfirmPassword = (rule: any, value: string, callback: Function) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

// 获取当前地区的手机号验证规则
const getCurrentPhonePattern = () => {
  const region = phoneRegions.find(r => r.code === registerForm.phoneRegion)
  return region?.pattern || /^1[3-9]\d{9}$/
}

// 获取当前地区的手机号占位符
const getCurrentPhonePlaceholder = () => {
  const region = phoneRegions.find(r => r.code === registerForm.phoneRegion)
  return region?.placeholder || '请输入手机号'
}

// 地区变化处理
const handleRegionChange = () => {
  registerForm.phone = ''
  // 清除验证错误
  if (registerFormRef.value) {
    registerFormRef.value.clearValidate('phone')
  }
}

// 手机号验证函数
const validatePhone = (rule: any, value: string, callback: Function) => {
  // 如果值为空，不立即验证，等待用户输入
  if (!value || value.trim() === '') {
    callback()
    return
  }
  
  const pattern = getCurrentPhonePattern()
  const region = phoneRegions.find(r => r.code === registerForm.phoneRegion)
  
  if (!pattern.test(value.trim())) {
    callback(new Error(region?.placeholder || '手机号格式不正确'))
  } else {
    callback()
  }
}

// 邮箱验证函数
const validateEmail = (rule: any, value: string, callback: Function) => {
  const emailReg = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  if (value === '') {
    callback(new Error('请输入邮箱'))
  } else if (!emailReg.test(value)) {
    callback(new Error('邮箱格式不正确'))
  } else {
    callback()
  }
}

// 表单验证规则
const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  email: [
    { required: true, validator: validateEmail, trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { validator: validatePhone, trigger: ['blur', 'change'] }
  ],
  password: [
    { required: true, validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ],
  agreement: [
    { required: true, validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('请阅读并同意用户协议和隐私政策'))
      } else {
        callback()
      }
    }, trigger: 'change' }
  ]
}

// 处理注册
const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  try {
    const valid = await registerFormRef.value.validate()
    if (!valid) return
    
    loading.value = true
    
    // 构造注册数据，移除确认密码字段，添加地区代码
    const fullPhone = registerForm.phoneRegion === '+86' 
      ? registerForm.phone 
      : `${registerForm.phoneRegion}${registerForm.phone}`
    
    const registerData = {
      username: registerForm.username,
      email: registerForm.email,
      phone: fullPhone,
      password: registerForm.password
    }
    
    // 调用注册API
    const response = await registerApi(registerData)
    
    if (response.data.code === 200) {
      ElMessage.success('注册成功！')
      router.push('/login')
    } else {
      ElMessage.error(response.data.message || '注册失败，请稍后重试')
    }
  } catch (error: any) {
    console.error('Register error:', error)
    // 显示更详细的错误信息
    const errorMessage = error?.response?.data?.message || error?.message || '注册失败，请稍后重试'
    ElMessage.error(errorMessage)
  } finally {
    loading.value = false
  }
}

// 跳转到登录页
const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped lang="scss">
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
  padding: 20px;
  
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

.register-box {
  width: 460px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 1;
  
  .register-header {
    text-align: center;
    margin-bottom: 30px;
    
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
      font-size: 18px;
      color: #409EFF;
      font-weight: 500;
      margin-top: 5px;
    }
  }
  
  .register-form {
    .el-form-item {
      margin-bottom: 20px;
      
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
      
      .el-checkbox {
        width: 100%;
        margin-left: -10px;
      }
    }
    
    .register-button {
      width: 100%;
      height: 44px;
      font-size: 16px;
      font-weight: 600;
      border-radius: 8px;
      background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
      border: none;
      margin-top: 10px;
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
  
  .register-footer {
    text-align: center;
    margin-top: 25px;
    padding-top: 20px;
    border-top: 1px solid #ebeef5;
    
    p {
      font-size: 14px;
      color: #606266;
      margin-bottom: 8px;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .el-link {
        font-size: 14px;
        margin-left: 4px;
      }
    }
    
    .copyright {
      font-size: 12px;
      color: #909399;
      margin-top: 15px;
    }
  }
}

@media (max-width: 480px) {
  .register-container {
    padding: 15px;
    
    .register-box {
      width: 100%;
      max-width: 400px;
      padding: 30px 20px;
    }
  }
}
</style>