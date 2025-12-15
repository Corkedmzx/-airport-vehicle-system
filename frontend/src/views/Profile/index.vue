<template>
  <div class="profile-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">个人资料</h1>
      <p class="page-description">管理您的个人信息和账户设置</p>
    </div>

    <div class="profile-content">
      <!-- 基本信息卡片 -->
      <el-card class="profile-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon><User /></el-icon>
            <span>基本信息</span>
          </div>
        </template>

        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-width="100px"
          label-position="left"
        >
          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="profileForm.username" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="真实姓名" prop="realName">
                <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="profileForm.email" placeholder="请输入邮箱地址" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="头像" prop="avatar">
                <div class="avatar-upload">
                  <el-avatar :size="80" :src="profileForm.avatar">
                    {{ profileForm.realName?.charAt(0) || profileForm.username?.charAt(0) || 'U' }}
                  </el-avatar>
                  <el-input
                    v-model="profileForm.avatar"
                    placeholder="请输入头像URL"
                    style="margin-top: 12px; width: 100%"
                  />
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="角色">
                <el-tag
                  v-for="role in profileForm.roles"
                  :key="role"
                  :type="getRoleTagType(role)"
                  style="margin-right: 8px"
                >
                  {{ getRoleName(role) }}
                </el-tag>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="账户状态">
                <el-tag :type="profileForm.status === 1 ? 'success' : 'danger'">
                  {{ profileForm.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="最后登录时间">
                <span v-if="profileForm.lastLoginAt" class="time-text">
                  {{ formatDateTime(profileForm.lastLoginAt) }}
                </span>
                <span v-else class="time-text">从未登录</span>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item>
            <el-button type="primary" :loading="saving" @click="saveProfile">
              <el-icon><Check /></el-icon>
              保存修改
            </el-button>
            <el-button @click="resetForm">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 账户信息卡片 -->
      <el-card class="profile-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon><Lock /></el-icon>
            <span>账户安全</span>
          </div>
        </template>

        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="120px"
          label-position="left"
        >
          <el-form-item label="当前密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="请输入当前密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入新密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="changingPassword" @click="changePassword">
              <el-icon><Lock /></el-icon>
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { User, Lock, Check, Refresh } from '@element-plus/icons-vue'
import { getCurrentUserApi, updateUserApi, changePasswordApi } from '@/api/users'
import { useUserStore } from '@/store/user'
import type { User as UserType } from '@/api/types'
import dayjs from 'dayjs'

const userStore = useUserStore()

const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const saving = ref(false)
const changingPassword = ref(false)

const profileForm = reactive<Partial<UserType>>({
  id: '',
  username: '',
  realName: '',
  email: '',
  phone: '',
  avatar: '',
  roles: [],
  status: 1,
  lastLoginAt: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const originalProfile = ref<Partial<UserType>>({})

// 表单验证规则
const profileRules: FormRules = {
  realName: [
    { required: false, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const passwordRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 加载用户信息
const loadUserInfo = async () => {
  try {
    const response = await getCurrentUserApi()
    if (response.data.code === 200) {
      const userData = response.data.data
      Object.assign(profileForm, {
        id: userData.id,
        username: userData.username,
        realName: userData.realName || '',
        email: userData.email || '',
        phone: userData.phone || '',
        avatar: userData.avatar || '',
        roles: userData.roles || [],
        status: userData.status,
        lastLoginAt: userData.lastLoginAt
      })
      originalProfile.value = { ...profileForm }
    } else {
      ElMessage.error(response.data.message || '加载用户信息失败')
    }
  } catch (error: any) {
    console.error('加载用户信息失败:', error)
    ElMessage.error(error?.response?.data?.message || '加载用户信息失败')
  }
}

// 保存个人资料
const saveProfile = async () => {
  if (!profileFormRef.value) return
  
  await profileFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        saving.value = true
        const updateData: any = {}
        
        if (profileForm.realName !== originalProfile.value.realName) {
          updateData.realName = profileForm.realName
        }
        if (profileForm.email !== originalProfile.value.email) {
          updateData.email = profileForm.email
        }
        if (profileForm.phone !== originalProfile.value.phone) {
          updateData.phone = profileForm.phone
        }
        if (profileForm.avatar !== originalProfile.value.avatar) {
          updateData.avatar = profileForm.avatar
        }

        if (Object.keys(updateData).length === 0) {
          ElMessage.info('没有需要保存的修改')
          saving.value = false
          return
        }

        const response = await updateUserApi(Number(profileForm.id), updateData)
        if (response.data.code === 200) {
          ElMessage.success('个人资料已更新')
          originalProfile.value = { ...profileForm }
          // 更新store中的用户信息
          await loadUserInfo()
          // 更新store
          if (userStore.userInfo) {
            Object.assign(userStore.userInfo, {
              realName: profileForm.realName,
              email: profileForm.email,
              phone: profileForm.phone,
              avatar: profileForm.avatar
            })
            localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
          }
        } else {
          ElMessage.error(response.data.message || '保存失败')
        }
      } catch (error: any) {
        console.error('保存个人资料失败:', error)
        ElMessage.error(error?.response?.data?.message || '保存个人资料失败')
      } finally {
        saving.value = false
      }
    }
  })
}

// 修改密码
const changePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await ElMessageBox.confirm(
          '确定要修改密码吗？',
          '提示',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        changingPassword.value = true
        const response = await changePasswordApi(passwordForm.oldPassword, passwordForm.newPassword)
        if (response.data.code === 200) {
          ElMessage.success('密码修改成功，请重新登录')
          // 清空密码表单
          passwordForm.oldPassword = ''
          passwordForm.newPassword = ''
          passwordForm.confirmPassword = ''
          passwordFormRef.value?.resetFields()
          // 延迟退出登录，让用户看到成功消息
          setTimeout(() => {
            userStore.logout()
          }, 1500)
        } else {
          ElMessage.error(response.data.message || '密码修改失败')
        }
      } catch (error: any) {
        if (error !== 'cancel') {
          console.error('修改密码失败:', error)
          ElMessage.error(error?.response?.data?.message || '修改密码失败')
        }
      } finally {
        changingPassword.value = false
      }
    }
  })
}

// 重置表单
const resetForm = () => {
  Object.assign(profileForm, originalProfile.value)
  profileFormRef.value?.resetFields()
}

// 获取角色名称
const getRoleName = (roleCode: string) => {
  const roleMap: Record<string, string> = {
    'ADMIN': '系统管理员',
    'OPERATOR': '操作员',
    'VIEWER': '查看者',
    'DISPATCHER': '调度员',
    'DRIVER': '司机',
    'MAINTENANCE': '维修员',
    'MONITOR': '监控员'
  }
  return roleMap[roleCode] || roleCode
}

// 获取角色标签类型
const getRoleTagType = (roleCode: string) => {
  if (roleCode === 'ADMIN') return 'danger'
  if (roleCode === 'DISPATCHER' || roleCode === 'OPERATOR') return 'warning'
  return 'info'
}

// 格式化日期时间
const formatDateTime = (dateTime: string) => {
  return dayjs(dateTime).format('YYYY-MM-DD HH:mm:ss')
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped lang="scss">
.profile-page {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;

  .page-title {
    font-size: 24px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 8px 0;
  }

  .page-description {
    font-size: 14px;
    color: #909399;
    margin: 0;
  }
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-card {
  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }
}

.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.time-text {
  color: #606266;
  font-size: 14px;
}

@media (max-width: 768px) {
  .profile-page {
    padding: 16px;
  }

  .page-header .page-title {
    font-size: 20px;
  }
}
</style>

