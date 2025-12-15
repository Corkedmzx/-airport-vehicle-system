<template>
  <div class="users-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">用户管理</h1>
      <p class="page-description">管理系统用户、角色和权限</p>
      <div class="header-actions">
        <el-button 
          v-if="hasPermission('user:create')"
          type="primary" 
          @click="createUser"
        >
          <el-icon><UserFilled /></el-icon>
          添加用户
        </el-button>
        <el-button @click="exportUsers">
          <el-icon><Download /></el-icon>
          导出用户
        </el-button>
      </div>
    </div>

    <!-- 用户统计 -->
    <div class="users-stats">
      <div class="stat-card total">
        <div class="stat-icon">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ userStats.totalUsers }}</div>
          <div class="stat-label">总用户数</div>
          <div class="stat-trend">活跃 {{ userStats.activeUsers }}</div>
        </div>
      </div>

      <div class="stat-card online">
        <div class="stat-icon">
          <el-icon><Connection /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ userStats.onlineUsers }}</div>
          <div class="stat-label">在线用户</div>
          <div class="stat-trend">在线率 {{ userStats.onlineRate }}%</div>
        </div>
      </div>

      <div class="stat-card roles">
        <div class="stat-icon">
          <el-icon><Key /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ userStats.totalRoles }}</div>
          <div class="stat-label">角色总数</div>
          <div class="stat-trend">权限组</div>
        </div>
      </div>

      <div class="stat-card admin">
        <div class="stat-icon">
          <el-icon><Star /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ userStats.adminUsers }}</div>
          <div class="stat-label">管理员</div>
          <div class="stat-trend">系统管理员</div>
        </div>
      </div>
    </div>

    <!-- 搜索和筛选 -->
    <div class="users-toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索用户名、姓名、邮箱"
          style="width: 300px"
          clearable
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-select
          v-model="searchForm.role"
          placeholder="用户角色"
          style="width: 120px; margin-left: 12px"
          clearable
          @change="handleSearch"
        >
          <el-option 
            v-for="role in roleOptions" 
            :key="role.value" 
            :label="role.label" 
            :value="role.value" 
          />
        </el-select>
        
        <el-select
          v-model="searchForm.status"
          placeholder="用户状态"
          style="width: 120px; margin-left: 12px"
          clearable
          @change="handleSearch"
        >
          <el-option label="启用" value="active" />
          <el-option label="禁用" value="inactive" />
          <el-option label="锁定" value="locked" />
        </el-select>
      </div>
      
      <div class="toolbar-right">
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 用户列表 -->
    <div class="users-table">
      <el-table
        v-loading="loading"
        :data="filteredUsers"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="avatar" label="头像" width="80">
          <template #default="{ row }">
            <el-avatar 
              :size="40" 
              :src="row.avatar"
            >
              {{ row.realName?.charAt(0) || row.username?.charAt(0) }}
            </el-avatar>
          </template>
        </el-table-column>
        
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="真实姓名" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag 
              v-if="row.roles && row.roles.length > 0"
              v-for="roleCode in row.roles"
              :key="roleCode"
              :type="getRoleType(roleCode.toLowerCase())" 
              size="small"
              style="margin-right: 4px;"
            >
              {{ getRoleText(roleCode.toLowerCase()) }}
            </el-tag>
            <span v-else style="color: #909399;">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag 
              :type="getStatusType(row.status === 1 ? 'active' : 'inactive')" 
              size="small"
            >
              {{ getStatusText(row.status === 1 ? 'active' : 'inactive') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary"
              size="small" 
              link
              @click="viewUserDetail(row)"
            >
              查看详情
            </el-button>
            <el-button 
              v-if="hasPermission('user:update')"
              type="primary" 
              size="small" 
              @click="editUser(row)"
            >
              编辑
            </el-button>
            <el-dropdown @command="(command) => handleUserCommand(command, row)">
              <el-button size="small">
                更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item 
                    v-if="hasPermission('user:update')"
                    command="resetPassword"
                  >
                    重置密码
                  </el-dropdown-item>
                  <el-dropdown-item 
                    v-if="hasPermission('user:update')"
                    command="toggleStatus"
                  >
                    {{ row.status === 'active' ? '禁用' : '启用' }}
                  </el-dropdown-item>
                  <el-dropdown-item command="viewLogs">
                    操作日志
                  </el-dropdown-item>
                  <el-dropdown-item 
                    v-if="hasPermission('user:delete') && row.role !== 'admin'"
                    command="delete"
                    divided
                  >
                    删除用户
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 用户表单对话框 -->
    <el-dialog
      v-model="userDialogVisible"
      :title="dialogMode === 'create' ? '添加用户' : '编辑用户'"
      width="600px"
      @close="resetUserForm"
    >
      <el-form 
        ref="userFormRef"
        :model="userForm"
        :rules="userRules"
        label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input 
                v-model="userForm.username" 
                placeholder="请输入用户名"
                :disabled="dialogMode === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input 
                v-model="userForm.realName" 
                placeholder="请输入真实姓名"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input 
                v-model="userForm.email" 
                placeholder="请输入邮箱"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input 
                v-model="userForm.phone" 
                placeholder="请输入手机号"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="角色" prop="role">
              <el-select 
                v-model="userForm.role" 
                placeholder="请选择角色"
                style="width: 100%"
                :disabled="dialogMode === 'edit' && !hasPermission('user:update')"
              >
                <el-option
                  v-for="role in roleOptions"
                  :key="role.value"
                  :label="role.label"
                  :value="role.value"
                />
              </el-select>
              <div v-if="dialogMode === 'edit' && !hasPermission('user:update')" style="color: #909399; font-size: 12px; margin-top: 4px;">
                只有系统管理员可以修改用户角色
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item 
          v-if="dialogMode === 'create'" 
          label="密码" 
          prop="password"
        >
          <el-input 
            v-model="userForm.password" 
            type="password" 
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item 
          v-if="dialogMode === 'create'" 
          label="确认密码" 
          prop="confirmPassword"
        >
          <el-input 
            v-model="userForm.confirmPassword" 
            type="password" 
            placeholder="请确认密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio value="active">启用</el-radio>
            <el-radio value="inactive">禁用</el-radio>
            <el-radio value="locked">锁定</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="备注">
          <el-input 
            v-model="userForm.remark" 
            type="textarea" 
            :rows="3"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="userDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveUser">
            {{ dialogMode === 'create' ? '创建' : '保存' }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 用户详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="用户详情"
      width="500px"
    >
      <div v-if="currentUser" class="user-detail">
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <label>用户名:</label>
              <span>{{ currentUser.username }}</span>
            </div>
            <div class="detail-item">
              <label>真实姓名:</label>
              <span>{{ currentUser.realName }}</span>
            </div>
            <div class="detail-item">
              <label>邮箱:</label>
              <span>{{ currentUser.email }}</span>
            </div>
            <div class="detail-item">
              <label>手机号:</label>
              <span>{{ currentUser.phone }}</span>
            </div>
            <div class="detail-item">
              <label>角色:</label>
              <div v-if="currentUser.roles && currentUser.roles.length > 0">
                <el-tag 
                  v-for="roleCode in currentUser.roles"
                  :key="roleCode"
                  :type="getRoleType(roleCode)" 
                  size="small"
                  style="margin-right: 4px;"
                >
                  {{ getRoleText(roleCode) }}
                </el-tag>
              </div>
              <span v-else style="color: #909399;">未分配</span>
            </div>
            <div class="detail-item">
              <label>状态:</label>
              <el-tag :type="getStatusType(currentUser.status)" size="small">
                {{ getStatusText(currentUser.status) }}
              </el-tag>
            </div>
            <div class="detail-item">
              <label>创建时间:</label>
              <span>{{ formatDateTime(currentUser.createTime) }}</span>
            </div>
          </div>
        </div>
        
        <div class="detail-section">
          <h4>登录信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <label>最后登录:</label>
              <span>{{ currentUser.lastLoginTime ? formatDateTime(currentUser.lastLoginTime) : '从未登录' }}</span>
            </div>
            <div class="detail-item">
              <label>登录次数:</label>
              <span>{{ currentUser.loginCount || 0 }} 次</span>
            </div>
          </div>
        </div>
        
        <div v-if="currentUser.remark" class="detail-section">
          <h4>备注信息</h4>
          <p>{{ currentUser.remark }}</p>
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="editUser(currentUser)">
            编辑用户
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus'
import {
  UserFilled, Download, User, Connection, Key, Star,
  Search, Refresh, ArrowDown
} from '@element-plus/icons-vue'
import type { User as UserType } from '@/api/types'
import dayjs from 'dayjs'
import { hasPermission } from '@/utils/permission'

// 用户统计数据
const userStats = ref({
  totalUsers: 0,
  activeUsers: 0,
  onlineUsers: 0,
  onlineRate: 0,
  totalRoles: 0,
  adminUsers: 0
})

// 用户列表
const users = ref<UserType[]>([])

// 角色选项
const roleOptions = ref([
  { label: '系统管理员', value: 'ADMIN' },
  { label: '调度员', value: 'DISPATCHER' },
  { label: '监控员', value: 'MONITOR' },
  { label: '操作员', value: 'OPERATOR' },
  { label: '查看者', value: 'VIEWER' },
  { label: '司机', value: 'DRIVER' },
  { label: '维修员', value: 'MAINTENANCE' }
])

// 搜索表单
const searchForm = ref({
  keyword: '',
  role: '',
  status: ''
})

// 分页信息
const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

// 选中项
const selectedUsers = ref<UserType[]>([])

// 加载状态
const loading = ref(false)

// 用户表单对话框
const userDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const userFormRef = ref<FormInstance>()

// 对话框模式
const dialogMode = ref<'create' | 'edit'>('create')

// 用户表单
const userForm = ref({
  id: '',
  username: '',
  realName: '',
  email: '',
  phone: '',
    role: '',
    password: '',
  confirmPassword: '',
  status: 'active',
  remark: ''
})

// 用户表单验证规则
const userRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: any) => {
        if (value !== userForm.value.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 当前用户
const currentUser = ref<UserType | null>(null)

// 过滤后的用户列表（现在由后端处理，这里保留用于前端额外筛选）
const filteredUsers = computed(() => {
  return users.value
})

// 获取角色类型
const getRoleType = (role: string | undefined) => {
  if (!role) return 'info'
  const typeMap: Record<string, string> = {
    admin: 'danger',
    dispatcher: 'warning',
    monitor: 'primary',
    operator: 'info',
    viewer: 'success',
    driver: 'success',
    maintenance: 'warning'
  }
  return typeMap[role.toLowerCase()] || 'info'
}

// 获取角色文本
const getRoleText = (role: string | undefined) => {
  if (!role) return '未分配'
  const textMap: Record<string, string> = {
    admin: '系统管理员',
    dispatcher: '调度员',
    monitor: '监控员',
    operator: '操作员',
    viewer: '查看者',
    driver: '司机',
    maintenance: '维修员'
  }
  return textMap[role.toLowerCase()] || role
}

// 获取状态类型
const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    active: 'success',
    inactive: 'info',
    locked: 'danger'
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    active: '启用',
    inactive: '禁用',
    locked: '锁定'
  }
  return textMap[status] || status
}

// 格式化日期
const formatDate = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD')
}

// 格式化日期时间
const formatDateTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

// 创建用户
const createUser = () => {
  dialogMode.value = 'create'
  resetUserForm()
  userDialogVisible.value = true
}

// 查看用户详情
const viewUserDetail = (user: UserType) => {
  currentUser.value = user
  detailDialogVisible.value = true
}

// 编辑用户
const editUser = (user: UserType) => {
  dialogMode.value = 'edit'
  // 获取用户的第一个角色（如果用户有多个角色，取第一个）
  const userRole = user.roles && user.roles.length > 0 ? user.roles[0] : ''
  userForm.value = {
    id: user.id,
    username: user.username,
    realName: user.realName,
    email: user.email,
    phone: user.phone,
    role: userRole, // 使用roles数组的第一个角色
    password: '',
    confirmPassword: '',
    status: typeof user.status === 'number' ? (user.status === 1 ? 'active' : 'inactive') : user.status,
    remark: user.remark || ''
  }
  detailDialogVisible.value = false
  userDialogVisible.value = true
}

// 重置用户表单
const resetUserForm = () => {
  userForm.value = {
    id: '',
    username: '',
    realName: '',
    email: '',
    phone: '',
    role: '',
    password: '',
    confirmPassword: '',
    status: 'active',
    remark: ''
  }
  if (userFormRef.value) {
    userFormRef.value.resetFields()
  }
}

// 保存用户
const saveUser = async () => {
  if (!userFormRef.value) return
  
  try {
    await userFormRef.value.validate()
    
    const { createUserApi, updateUserApi } = await import('@/api/users')
    
    if (dialogMode.value === 'create') {
      await createUserApi({
        username: userForm.value.username,
        realName: userForm.value.realName,
        email: userForm.value.email,
        phone: userForm.value.phone,
        password: userForm.value.password,
        status: userForm.value.status === 'active' ? 1 : 0
      })
      ElMessage.success('用户创建成功')
    } else {
      await updateUserApi(Number(userForm.value.id), {
        realName: userForm.value.realName,
        email: userForm.value.email,
        phone: userForm.value.phone,
        role: userForm.value.role, // 添加角色字段
        status: userForm.value.status === 'active' ? 1 : 0
      })
      ElMessage.success('用户信息更新成功')
    }
    
    userDialogVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '保存失败')
  }
}

// 处理用户操作命令
const handleUserCommand = async (command: string, user: UserType) => {
  switch (command) {
    case 'resetPassword':
      await resetPassword(user)
      break
    case 'toggleStatus':
      await toggleUserStatus(user)
      break
    case 'viewLogs':
      viewUserLogs(user)
      break
    case 'delete':
      await deleteUser(user)
      break
  }
}

// 重置密码
const resetPassword = async (user: UserType) => {
  try {
    const { value: newPassword } = await ElMessageBox.prompt(
      '请输入新密码',
      '重置密码',
      {
        confirmButtonText: '确认重置',
        cancelButtonText: '取消',
        inputType: 'password',
        inputPattern: /^.{6,20}$/,
        inputErrorMessage: '密码长度在6到20个字符'
      }
    )
    
    const { resetPasswordApi } = await import('@/api/users')
    await resetPasswordApi(Number(user.id), newPassword)
    ElMessage.success('密码重置成功')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '重置密码失败')
    }
  }
}

// 切换用户状态
const toggleUserStatus = async (user: UserType) => {
  try {
    const action = user.status === 'active' ? '禁用' : '启用'
    await ElMessageBox.confirm(
      `确定要${action}用户 ${user.realName} 吗？`,
      '切换状态',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const { updateUserStatusApi } = await import('@/api/users')
    const newStatus = user.status === 'active' ? 0 : 1
    await updateUserStatusApi(Number(user.id), newStatus)
    ElMessage.success(`用户已${action}`)
    await loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '操作失败')
    }
  }
}

// 查看用户日志
const viewUserLogs = (user: UserType) => {
  ElMessage.info(`查看用户 ${user.realName} 的操作日志功能开发中`)
}

// 删除用户（表格中的删除按钮）
const handleDelete = async (user: UserType) => {
  await deleteUser(user)
}

// 删除用户
const deleteUser = async (user: UserType) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 ${user.realName} 吗？此操作不可恢复！`,
      '删除用户',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'error'
      }
    )
    
    const { deleteUserApi } = await import('@/api/users')
    await deleteUserApi(Number(user.id))
    ElMessage.success('用户删除成功')
    await loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '删除失败')
    }
  }
}

// 导出用户
const exportUsers = async () => {
  try {
    ElMessage.info('正在导出用户数据...')
    
    // 获取所有用户数据
    const { getUsersApi } = await import('@/api/users')
    const response = await getUsersApi({
      page: 0,
      size: 10000 // 获取所有数据
    })
    
    if (response.data.code === 200) {
      const users = response.data.data.content || []
      
      // 转换为CSV格式
      const headers = ['用户ID', '用户名', '真实姓名', '邮箱', '手机号', '角色', '状态', '最后登录时间', '创建时间']
      const rows = users.map((user: any) => [
        user.id,
        user.username,
        user.realName || '-',
        user.email || '-',
        user.phone || '-',
        user.role || '-',
        user.status === 1 ? '启用' : '禁用',
        user.lastLoginTime || '从未登录',
        user.createTime
      ])
      
      // 创建CSV内容
      const csvContent = [
        headers.join(','),
        ...rows.map(row => row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
      ].join('\n')
      
      // 创建下载链接
      const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
      const link = document.createElement('a')
      const url = URL.createObjectURL(blob)
      link.setAttribute('href', url)
      link.setAttribute('download', `用户数据_${new Date().toISOString().split('T')[0]}.csv`)
      link.style.visibility = 'hidden'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      
      ElMessage.success('用户数据导出成功')
    }
  } catch (error) {
    console.error('Export users failed:', error)
    ElMessage.error('导出失败，请稍后重试')
  }
}

// 搜索处理
const handleSearch = () => {
  pagination.value.page = 1
}

// 选择项变化
const handleSelectionChange = (selection: UserType[]) => {
  selectedUsers.value = selection
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  pagination.value.size = size
  pagination.value.page = 1
}

// 页码变化
const handleCurrentChange = (page: number) => {
  pagination.value.page = page
}

// 刷新数据
const refreshData = async () => {
  await loadData()
  ElMessage.success('数据已刷新')
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    await Promise.all([
      loadUserStats(),
      loadUsers()
    ])
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 加载用户统计数据
const loadUserStats = async () => {
  try {
    const { getUserStatisticsApi } = await import('@/api/users')
    const response = await getUserStatisticsApi()
    if (response.data.code === 200) {
      const stats = response.data.data
      userStats.value = {
        totalUsers: stats.totalUsers || 0,
        activeUsers: stats.activeUsers || 0,
        onlineUsers: stats.onlineUsers || 0,
        onlineRate: stats.onlineRate || 0,
        totalRoles: stats.totalRoles || 0,
        adminUsers: stats.adminUsers || 0
      }
    }
  } catch (error) {
    console.error('Load user stats failed:', error)
  }
}

// 加载用户统计数据（旧代码保留作为备用）
const loadUserStatsOld = async () => {
  try {
    // 模拟数据
    userStats.value = {
      totalUsers: 25,
      activeUsers: 23,
      onlineUsers: 8,
      onlineRate: 32,
      totalRoles: 5,
      adminUsers: 2
    }
  } catch (error) {
    console.error('Load user stats failed:', error)
  }
}

// 加载用户列表
const loadUsers = async () => {
  try {
    const { getUsersApi } = await import('@/api/users')
    const response = await getUsersApi({
      page: pagination.value.page - 1,
      size: pagination.value.size,
      keyword: searchForm.value.keyword || undefined,
      status: searchForm.value.status === 'active' ? 1 : searchForm.value.status === 'inactive' ? 0 : undefined
    })
    
    if (response.data.code === 200) {
      const pageData = response.data.data
      users.value = pageData.content || []
      pagination.value.total = pageData.totalElements || 0
    }
  } catch (error) {
    console.error('Load users failed:', error)
    users.value = []
  }
}

// 加载用户列表（旧代码保留作为备用）
const loadUsersOld = async () => {
  try {
    // 模拟数据
    // 数据已由loadUsers从API加载
  } catch (error) {
    console.error('Load users failed:', error)
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.users-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 24px;
    
    .page-title {
      font-size: 28px;
      font-weight: 700;
      color: var(--text-primary-color);
      margin-bottom: 8px;
    }
    
    .page-description {
      color: var(--text-regular-color);
      font-size: 16px;
      margin-bottom: 0;
    }
    
    .header-actions {
      display: flex;
      gap: 12px;
    }
  }
}

.users-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  }
  
  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: white;
    
    &.total {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }
    
    &.online {
      background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
    }
    
    &.roles {
      background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
    }
    
    &.admin {
      background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
    }
  }
  
  .stat-content {
    flex: 1;
    
    .stat-value {
      font-size: 32px;
      font-weight: 700;
      color: var(--text-primary-color);
      line-height: 1;
      margin-bottom: 4px;
    }
    
    .stat-label {
      font-size: 14px;
      color: var(--text-regular-color);
      margin-bottom: 4px;
    }
    
    .stat-trend {
      font-size: 12px;
      color: var(--text-secondary-color);
    }
  }
}

.users-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  
  .toolbar-left {
    display: flex;
    align-items: center;
  }
  
  .toolbar-right {
    display: flex;
    gap: 12px;
  }
}

.users-table {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.pagination-wrapper {
  padding: 20px;
  display: flex;
  justify-content: center;
  border-top: 1px solid var(--border-lighter-color);
}

.user-detail {
  .detail-section {
    margin-bottom: 24px;
    
    h4 {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary-color);
      margin-bottom: 12px;
      padding-bottom: 8px;
      border-bottom: 1px solid var(--border-lighter-color);
    }
  }
  
  .detail-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
    
    .detail-item {
      display: flex;
      align-items: center;
      
      label {
        font-size: 14px;
        color: var(--text-secondary-color);
        width: 80px;
        margin-bottom: 0;
      }
      
      span {
        font-size: 14px;
        color: var(--text-primary-color);
      }
    }
  }
  
  p {
    font-size: 14px;
    color: var(--text-primary-color);
    line-height: 1.6;
  }
}

@media (max-width: 1200px) {
  .users-stats {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .detail-grid {
    grid-template-columns: 1fr !important;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
    
    .header-actions {
      width: 100%;
      justify-content: stretch;
      
      .el-button {
        flex: 1;
      }
    }
  }
  
  .users-toolbar {
    flex-direction: column;
    gap: 16px;
    
    .toolbar-left,
    .toolbar-right {
      width: 100%;
      justify-content: stretch;
    }
    
    .toolbar-left {
      flex-wrap: wrap;
      gap: 12px;
    }
  }
  
  .users-stats {
    grid-template-columns: 1fr;
  }
}
</style>