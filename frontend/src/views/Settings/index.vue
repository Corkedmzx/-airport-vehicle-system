<template>
  <div class="settings-page">
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">系统设置</h1>
        <p class="page-description">管理系统配置、用户权限和日志</p>
      </div>
      
      <el-tabs v-model="activeTab" type="border-card" @tab-change="handleTabChange">
        <!-- 用户管理 -->
        <el-tab-pane label="用户管理" name="users">
          <div class="tab-content">
            <div class="section-header">
              <h3>用户管理</h3>
              <el-button type="primary" @click="goToUsers">
                前往用户管理页面
              </el-button>
            </div>
            <p class="section-description">管理系统用户、角色和权限</p>
          </div>
        </el-tab-pane>
        
        <!-- 日志管理 -->
        <el-tab-pane label="日志管理" name="logs">
          <div class="tab-content">
            <div class="section-header">
              <h3>日志管理</h3>
              <el-button type="primary" @click="goToLogs">
                前往日志管理页面
              </el-button>
            </div>
            <p class="section-description">查看和管理系统操作日志、错误日志和审计日志</p>
          </div>
        </el-tab-pane>
        
        <!-- 权限设置 -->
        <el-tab-pane label="权限设置" name="permissions">
          <!-- 仅admin用户可操作 -->
          <div class="tab-content" v-if="isAdminUser">
            <div class="section-header">
              <h3>权限设置</h3>
            </div>
            <p class="section-description">配置系统权限和角色管理（仅系统管理员admin可操作）</p>
            
            <el-card class="permission-card">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span>角色权限配置</span>
                  <el-button type="primary" size="small" @click="loadRolePermissions" :loading="loading">
                    刷新
                  </el-button>
                </div>
              </template>
              <el-table v-loading="loading" :data="rolePermissions" stripe>
                <el-table-column prop="roleName" label="角色" width="150">
                  <template #default="{ row }">
                    <span>{{ row.roleName }}</span>
                    <el-tag v-if="row.roleCode === 'ADMIN'" type="warning" size="small" style="margin-left: 8px;">
                      仅查看
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="权限" min-width="600">
                  <template #default="{ row }">
                    <!-- ADMIN角色：只读显示 -->
                    <div v-if="row.roleCode === 'ADMIN'">
                      <el-tag
                        v-for="permission in getPermissionLabels(row.permissions || [])"
                        :key="permission.code"
                        type="info"
                        style="margin-right: 8px; margin-bottom: 8px;"
                      >
                        {{ permission.name }}
                      </el-tag>
                      <p style="color: #909399; font-size: 12px; margin-top: 8px;">
                        系统管理员权限不可修改，以防止误操作
                      </p>
                    </div>
                    <!-- 其他角色：可编辑 -->
                    <el-checkbox-group v-else v-model="row.permissions">
                      <el-checkbox 
                        v-for="permission in allPermissions" 
                        :key="permission.value"
                        :label="permission.value"
                      >
                        {{ permission.label }}
                      </el-checkbox>
                    </el-checkbox-group>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="200">
                  <template #default="{ row }">
                    <div style="display: flex; gap: 8px;">
                      <!-- 查看权限内容按钮 -->
                      <el-button 
                        type="info" 
                        link 
                        size="small"
                        @click="viewRolePermissions(row)"
                      >
                        查看权限内容
                      </el-button>
                      <!-- 修改权限按钮（ADMIN角色不显示） -->
                      <el-button 
                        v-if="row.roleCode !== 'ADMIN'"
                        type="primary" 
                        link 
                        size="small"
                        @click="editRolePermissions(row)"
                        :loading="row.saving"
                      >
                        修改权限
                      </el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>
          <!-- 非admin用户：只显示自己的权限 -->
          <div class="tab-content" v-else>
            <div class="section-header">
              <h3>我的权限</h3>
            </div>
            <p class="section-description">查看您当前拥有的权限</p>
            
            <el-card class="permission-card">
              <template #header>
                <span>当前用户权限信息</span>
              </template>
              <div class="user-permission-info">
                <div class="info-item">
                  <label>用户名：</label>
                  <span>{{ userStore.userInfo?.username }}</span>
                </div>
                <div class="info-item">
                  <label>角色：</label>
                  <el-tag
                    v-for="role in userStore.userInfo?.roles || []"
                    :key="role"
                    type="primary"
                    style="margin-right: 8px;"
                  >
                    {{ getRoleName(role) }}
                  </el-tag>
                </div>
                <div class="info-item">
                  <label>权限列表：</label>
                  <div class="permission-tags">
                    <el-tag
                      v-for="permission in userStore.userInfo?.permissions || []"
                      :key="permission"
                      type="success"
                      style="margin-right: 8px; margin-bottom: 8px;"
                    >
                      {{ getPermissionName(permission) }}
                    </el-tag>
                    <span v-if="!userStore.userInfo?.permissions || userStore.userInfo.permissions.length === 0" style="color: #909399;">
                      暂无权限
                    </span>
                  </div>
                </div>
                <el-alert
                  title="提示"
                  type="info"
                  :closable="false"
                  style="margin-top: 20px;"
                >
                  <template #default>
                    <p>只有系统管理员（admin）可以配置角色权限。如需修改权限，请联系系统管理员。</p>
                  </template>
                </el-alert>
              </div>
            </el-card>
          </div>
        </el-tab-pane>
        
        <!-- 系统配置 -->
        <el-tab-pane label="系统配置" name="config">
          <div class="tab-content">
            <div class="section-header">
              <h3>系统配置</h3>
            </div>
            <p class="section-description">配置系统参数和功能开关</p>
            
            <el-card class="config-card">
              <template #header>
                <span>基本配置</span>
              </template>
              <el-form :model="systemConfig" label-width="200px">
                <el-form-item label="车辆位置更新间隔">
                  <el-input-number 
                    v-model="systemConfig.locationUpdateInterval" 
                    :min="10" 
                    :max="300"
                  />
                  <span style="margin-left: 8px;">秒</span>
                </el-form-item>
                <el-form-item label="自动任务分配">
                  <el-switch v-model="systemConfig.autoAssignEnabled" />
                </el-form-item>
                <el-form-item label="维护提醒提前天数">
                  <el-input-number 
                    v-model="systemConfig.maintenanceReminderDays" 
                    :min="1" 
                    :max="30"
                  />
                  <span style="margin-left: 8px;">天</span>
                </el-form-item>
                <el-form-item label="地图服务提供商">
                  <el-select v-model="systemConfig.mapProvider" style="width: 200px">
                    <el-option label="百度地图" value="baidu" />
                    <el-option label="高德地图" value="gaode" />
                    <el-option label="腾讯地图" value="tencent" />
                  </el-select>
                </el-form-item>
                <el-form-item label="位置追踪">
                  <el-switch v-model="systemConfig.locationTrackingEnabled" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="saveSystemConfig">
                    保存配置
                  </el-button>
                  <el-button @click="resetSystemConfig">
                    重置
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAllRolesApi, getRolePermissionsApi, updateRolePermissionsApi, getAllPermissionsApi } from '@/api/rolePermissions'
import type { Role, Permission, RolePermissionDTO } from '@/api/rolePermissions'
import { isAdmin } from '@/utils/permission'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('users')
const loading = ref(false)

// 检查是否是admin用户（仅admin可操作）
const isAdminUser = computed(() => {
  return userStore.userInfo?.username === 'admin' && isAdmin()
})

// 角色权限配置
const rolePermissions = ref<Array<RolePermissionDTO & { id?: number }>>([])

// 所有权限列表
const allPermissions = ref<Array<{ label: string; value: string }>>([])

// 加载角色和权限数据
const loadRolePermissions = async () => {
  try {
    loading.value = true
    const [rolesResponse, permissionsResponse] = await Promise.all([
      getAllRolesApi(),
      getAllPermissionsApi()
    ])
    
    if (rolesResponse.data.code === 200 && permissionsResponse.data.code === 200) {
      const roles = rolesResponse.data.data || []
      const permissions = permissionsResponse.data.data || []
      
      // 构建权限选项
      allPermissions.value = permissions.map(p => ({
        label: p.permissionName,
        value: p.permissionCode
      }))
      
      // 加载每个角色的权限
      const rolePerms = await Promise.all(
        roles.map(async (role: Role) => {
          try {
            const permResponse = await getRolePermissionsApi(role.id)
            if (permResponse.data.code === 200) {
              return {
                ...permResponse.data.data,
                saving: false
              }
            }
            return {
              roleId: role.id,
              roleName: role.roleName,
              roleCode: role.roleCode,
              permissions: [],
              saving: false
            }
          } catch (error) {
            console.error(`加载角色 ${role.roleName} 的权限失败:`, error)
            return {
              roleId: role.id,
              roleName: role.roleName,
              roleCode: role.roleCode,
              permissions: [],
              saving: false
            }
          }
        })
      )
      
      rolePermissions.value = rolePerms
    }
  } catch (error) {
    console.error('加载角色权限失败:', error)
    ElMessage.error('加载角色权限失败')
  } finally {
    loading.value = false
  }
}

// 系统配置
const systemConfig = reactive({
  locationUpdateInterval: 30,
  autoAssignEnabled: true,
  maintenanceReminderDays: 7,
  mapProvider: 'baidu',
  locationTrackingEnabled: true
})

// 前往用户管理页面
const goToUsers = () => {
  router.push('/users')
}

// 前往日志管理页面
const goToLogs = () => {
  router.push('/logs')
}

// 查看角色权限
const viewRolePermissions = (row: RolePermissionDTO & { id?: number }) => {
  const permissionNames = getPermissionLabels(row.permissions || []).map(p => p.name).join('、')
  ElMessageBox.alert(
    `<div style="text-align: left;">
      <p><strong>角色：</strong>${row.roleName}</p>
      <p><strong>权限列表：</strong></p>
      <p>${permissionNames || '暂无权限'}</p>
    </div>`,
    '权限详情',
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '关闭'
    }
  )
}

// 编辑角色权限
const editRolePermissions = (row: RolePermissionDTO & { id?: number; saving?: boolean }) => {
  // 提示用户可以在表格中直接勾选权限
  ElMessageBox.confirm(
    '请在下方表格中勾选权限，然后点击"保存"按钮',
    '编辑权限',
    {
      confirmButtonText: '我知道了',
      showCancelButton: false,
      type: 'info'
    }
  )
}

// 保存角色权限
const saveRolePermissions = async (row: RolePermissionDTO & { id?: number; saving?: boolean }) => {
  try {
    if (!row.roleId) {
      ElMessage.error('角色ID不存在')
      return
    }
    
    // 防止修改ADMIN角色权限
    if (row.roleCode === 'ADMIN') {
      ElMessage.warning('系统管理员权限不可修改')
      return
    }
    
    // 确认保存
    await ElMessageBox.confirm(
      `确定要保存角色 "${row.roleName}" 的权限配置吗？`,
      '确认保存',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    row.saving = true
    const response = await updateRolePermissionsApi(row.roleId, row.permissions || [])
    
    if (response.data.code === 200) {
      ElMessage.success({
        message: `角色 ${row.roleName} 的权限已保存`,
        duration: 3000
      })
      // 重新加载数据
      await loadRolePermissions()
    } else {
      ElMessage.error(response.data.message || '保存失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('保存角色权限失败:', error)
      ElMessage.error(error?.response?.data?.message || '保存角色权限失败')
    }
  } finally {
    if (row) {
      row.saving = false
    }
  }
}

// 获取权限标签（用于ADMIN角色只读显示）
const getPermissionLabels = (permissionCodes: string[]) => {
  return permissionCodes.map(code => {
    const permission = allPermissions.value.find(p => p.value === code)
    return {
      code,
      name: permission?.label || code
    }
  })
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

// 获取权限名称
const getPermissionName = (permissionCode: string) => {
  const permission = allPermissions.value.find(p => p.value === permissionCode)
  return permission?.label || permissionCode
}

// 保存系统配置
const saveSystemConfig = () => {
  // TODO: 调用API保存系统配置
  ElMessage.success('系统配置已保存')
}

// 重置系统配置
const resetSystemConfig = () => {
  systemConfig.locationUpdateInterval = 30
  systemConfig.autoAssignEnabled = true
  systemConfig.maintenanceReminderDays = 7
  systemConfig.mapProvider = 'baidu'
  systemConfig.locationTrackingEnabled = true
  ElMessage.info('配置已重置')
}

// 组件挂载时加载数据
onMounted(() => {
  if (activeTab.value === 'permissions' && isAdminUser.value) {
    loadRolePermissions()
  } else if (activeTab.value === 'permissions' && !isAdminUser.value) {
    // 非admin用户也需要加载权限列表以显示权限名称
    getAllPermissionsApi().then(response => {
      if (response.data.code === 200) {
        allPermissions.value = (response.data.data || []).map((p: Permission) => ({
          label: p.permissionName,
          value: p.permissionCode
        }))
      }
    }).catch(error => {
      console.error('加载权限列表失败:', error)
    })
  }
})

// 监听标签页切换
const handleTabChange = (tabName: string) => {
  if (tabName === 'permissions') {
    if (isAdminUser.value && rolePermissions.value.length === 0) {
      loadRolePermissions()
    } else if (!isAdminUser.value && allPermissions.value.length === 0) {
      // 非admin用户加载权限列表以显示权限名称
      getAllPermissionsApi().then(response => {
        if (response.data.code === 200) {
          allPermissions.value = (response.data.data || []).map((p: Permission) => ({
            label: p.permissionName,
            value: p.permissionCode
          }))
        }
      }).catch(error => {
        console.error('加载权限列表失败:', error)
      })
    }
  }
}
</script>

<style scoped lang="scss">
.settings-page {
  .page-container {
    .page-header {
      margin-bottom: 24px;
      
      .page-title {
        font-size: 24px;
        font-weight: 600;
        color: var(--text-primary-color);
        margin-bottom: 8px;
      }
      
      .page-description {
        color: var(--text-regular-color);
        font-size: 14px;
      }
    }
  }
}

.tab-content {
  padding: 20px;
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    
    h3 {
      font-size: 18px;
      font-weight: 600;
      color: var(--text-primary-color);
      margin: 0;
    }
  }
  
  .section-description {
    color: var(--text-regular-color);
    margin-bottom: 24px;
  }
}

.permission-card,
.config-card {
  margin-top: 20px;
}

:deep(.el-checkbox-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

:deep(.el-checkbox) {
  margin-right: 0;
}

.user-permission-info {
  padding: 20px;
  
  .info-item {
    margin-bottom: 24px;
    
    label {
      display: inline-block;
      width: 100px;
      font-weight: 600;
      color: var(--text-primary-color);
    }
    
    span {
      color: var(--text-regular-color);
    }
  }
  
  .permission-tags {
    display: flex;
    flex-wrap: wrap;
    margin-top: 8px;
  }
}
</style>
