<template>
  <div class="users-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">用户管理</h1>
      <p class="page-description">管理系统用户、角色和权限</p>
      <div class="header-actions">
        <el-button type="primary" @click="createUser">
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
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag 
              :type="getRoleType(row.role)" 
              size="small"
            >
              {{ getRoleText(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="department" label="部门" width="120" />
        
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag 
              :type="getStatusType(row.status)" 
              size="small"
            >
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="lastLoginAt" label="最后登录" width="150">
          <template #default="{ row }">
            {{ row.lastLoginAt ? formatDateTime(row.lastLoginAt) : '从未登录' }}
          </template>
        </el-table-column>
        
        <el-table-column prop="createdAt" label="创建时间" width="150">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button 
              size="small" 
              @click="viewUserDetail(row)"
            >
              查看
            </el-button>
            <el-button 
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
                  <el-dropdown-item command="resetPassword">
                    重置密码
                  </el-dropdown-item>
                  <el-dropdown-item command="toggleStatus">
                    {{ row.status === 'active' ? '禁用' : '启用' }}
                  </el-dropdown-item>
                  <el-dropdown-item command="viewLogs">
                    操作日志
                  </el-dropdown-item>
                  <el-dropdown-item 
                    v-if="row.role !== 'admin'"
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
              >
                <el-option
                  v-for="role in roleOptions"
                  :key="role.value"
                  :label="role.label"
                  :value="role.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门" prop="department">
              <el-input 
                v-model="userForm.department" 
                placeholder="请输入部门"
              />
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
              <el-tag :type="getRoleType(currentUser.role)" size="small">
                {{ getRoleText(currentUser.role) }}
              </el-tag>
            </div>
            <div class="detail-item">
              <label>部门:</label>
              <span>{{ currentUser.department }}</span>
            </div>
            <div class="detail-item">
              <label>状态:</label>
              <el-tag :type="getStatusType(currentUser.status)" size="small">
                {{ getStatusText(currentUser.status) }}
              </el-tag>
            </div>
            <div class="detail-item">
              <label>创建时间:</label>
              <span>{{ formatDateTime(currentUser.createdAt) }}</span>
            </div>
          </div>
        </div>
        
        <div class="detail-section">
          <h4>登录信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <label>最后登录:</label>
              <span>{{ currentUser.lastLoginAt ? formatDateTime(currentUser.lastLoginAt) : '从未登录' }}</span>
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
  { label: '系统管理员', value: 'admin' },
  { label: '调度员', value: 'dispatcher' },
  { label: '监控员', value: 'monitor' },
  { label: '操作员', value: 'operator' },
  { label: '查看者', value: 'viewer' }
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
  department: '',
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
  department: [
    { required: true, message: '请输入部门', trigger: 'blur' }
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

// 过滤后的用户列表
const filteredUsers = computed(() => {
  let result = users.value
  
  // 关键词搜索
  if (searchForm.value.keyword) {
    const keyword = searchForm.value.keyword.toLowerCase()
    result = result.filter(user => 
      user.username.toLowerCase().includes(keyword) ||
      user.realName.toLowerCase().includes(keyword) ||
      user.email.toLowerCase().includes(keyword)
    )
  }
  
  // 角色筛选
  if (searchForm.value.role) {
    result = result.filter(user => user.role === searchForm.value.role)
  }
  
  // 状态筛选
  if (searchForm.value.status) {
    result = result.filter(user => user.status === searchForm.value.status)
  }
  
  return result
})

// 获取角色类型
const getRoleType = (role: string) => {
  const typeMap: Record<string, string> = {
    admin: 'danger',
    dispatcher: 'warning',
    monitor: 'primary',
    operator: 'info',
    viewer: 'success'
  }
  return typeMap[role] || 'info'
}

// 获取角色文本
const getRoleText = (role: string) => {
  const textMap: Record<string, string> = {
    admin: '系统管理员',
    dispatcher: '调度员',
    monitor: '监控员',
    operator: '操作员',
    viewer: '查看者'
  }
  return textMap[role] || role
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
  userForm.value = {
    id: user.id,
    username: user.username,
    realName: user.realName,
    email: user.email,
    phone: user.phone,
    role: user.role,
    department: user.department,
    password: '',
    confirmPassword: '',
    status: user.status,
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
    department: '',
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
    
    if (dialogMode.value === 'create') {
      // TODO: 调用API创建用户
      ElMessage.success('用户创建成功')
    } else {
      // TODO: 调用API更新用户
      ElMessage.success('用户信息更新成功')
    }
    
    userDialogVisible.value = false
    await loadData()
  } catch (error) {
    console.error('Save user failed:', error)
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
    await ElMessageBox.confirm(
      `确定要重置用户 ${user.realName} 的密码吗？`,
      '重置密码',
      {
        confirmButtonText: '确认重置',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // TODO: 调用API重置密码
    ElMessage.success('密码重置成功，新密码已发送到用户邮箱')
  } catch {
    // 用户取消
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
    
    // TODO: 调用API切换状态
    user.status = user.status === 'active' ? 'inactive' : 'active'
    ElMessage.success(`用户已${action}`)
  } catch {
    // 用户取消
  }
}

// 查看用户日志
const viewUserLogs = (user: UserType) => {
  ElMessage.info(`查看用户 ${user.realName} 的操作日志功能开发中`)
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
    
    // TODO: 调用API删除用户
    const index = users.value.findIndex(u => u.id === user.id)
    if (index > -1) {
      users.value.splice(index, 1)
    }
    
    ElMessage.success('用户删除成功')
  } catch {
    // 用户取消
  }
}

// 导出用户
const exportUsers = () => {
  ElMessage.info('导出用户功能开发中')
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
    // TODO: 调用API获取统计数据
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
    // TODO: 调用API获取用户列表
    // 模拟数据
    users.value = [
      {
        id: '1',
        username: 'admin',
        realName: '系统管理员',
        email: 'admin@airport.com',
        phone: '13800138001',
        role: 'admin',
        department: '信息技术部',
        status: 'active',
        avatar: '',
        lastLoginAt: new Date().toISOString(),
        loginCount: 156,
        createdAt: new Date(Date.now() - 86400000 * 30).toISOString(),
        remark: '系统默认管理员账户'
      },
      {
        id: '2',
        username: 'zhangsan',
        realName: '张三',
        email: 'zhangsan@airport.com',
        phone: '13800138002',
        role: 'dispatcher',
        department: '调度中心',
        status: 'active',
        avatar: '',
        lastLoginAt: new Date(Date.now() - 3600000).toISOString(),
        loginCount: 89,
        createdAt: new Date(Date.now() - 86400000 * 15).toISOString(),
        remark: '资深调度员'
      }
    ]
    pagination.value.total = users.value.length
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