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
                    <!-- 其他角色：显示权限标签（小方框+权限名样式） -->
                    <div v-else>
                      <div style="display: flex; flex-wrap: wrap; gap: 8px;">
                        <div
                          v-for="permission in getPermissionLabels(row.permissions || [])"
                          :key="permission.code"
                          style="display: inline-flex; align-items: center; padding: 4px 8px; border: 1px solid #dcdfe6; border-radius: 4px; background-color: #f5f7fa;"
                        >
                          <span style="display: inline-block; width: 16px; height: 16px; line-height: 16px; text-align: center; border: 1px solid #67c23a; border-radius: 2px; background-color: #67c23a; color: white; margin-right: 6px; font-size: 12px;">√</span>
                          <span>{{ permission.name }}</span>
                        </div>
                      </div>
                      <span v-if="!row.permissions || row.permissions.length === 0" style="color: #909399;">
                        暂无权限
                      </span>
                    </div>
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
                        @click="openEditPermissionsDialog(row)"
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

    <!-- 修改权限对话框 -->
    <el-dialog
      v-model="editPermissionsDialogVisible"
      :title="`修改权限 - ${currentEditRole?.roleName || ''}`"
      width="700px"
      @close="closeEditPermissionsDialog"
      class="permission-edit-dialog"
      :close-on-click-modal="false"
    >
      <div class="permission-edit-header">
        <el-icon style="color: #409eff; margin-right: 8px;"><InfoFilled /></el-icon>
        <span style="color: #606266; font-size: 14px;">请勾选该角色需要拥有的权限：</span>
      </div>
      <div class="permission-checkbox-container" v-if="allPermissions.length > 0">
        <el-checkbox-group 
          v-model="editingPermissions" 
          class="permission-checkbox-group"
        >
          <div 
            v-for="permission in allPermissions" 
            :key="permission.value"
            class="permission-checkbox-item"
          >
            <el-checkbox 
              :label="permission.value"
              class="permission-checkbox"
            >
              <span class="permission-label">{{ permission.label }}</span>
            </el-checkbox>
          </div>
        </el-checkbox-group>
      </div>
      <div v-else style="text-align: center; padding: 40px; color: #909399;">
        <el-icon style="font-size: 48px; margin-bottom: 16px;"><Loading /></el-icon>
        <p>正在加载权限列表...</p>
      </div>
      <template #footer>
        <div class="dialog-footer" style="text-align: right; padding: 10px 0;">
          <el-button @click="closeEditPermissionsDialog" size="default">取消</el-button>
          <el-button 
            type="primary" 
            @click="saveRolePermissionsFromDialog"
            :loading="savingPermissions"
            size="default"
            style="margin-left: 10px;"
          >
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled, Loading } from '@element-plus/icons-vue'
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

// 修改权限对话框
const editPermissionsDialogVisible = ref(false)
const currentEditRole = ref<RolePermissionDTO & { id?: number; saving?: boolean } | null>(null)
const editingPermissions = ref<string[]>([])
const savingPermissions = ref(false)

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
  const permissions = getPermissionLabels(row.permissions || [])
  // 使用小方框+权限名的样式显示，如：√车辆管理
  const permissionList = permissions.length > 0 
    ? permissions.map(p => `
        <div style="display: inline-block; margin: 6px 8px 6px 0; padding: 4px 8px; border: 1px solid #dcdfe6; border-radius: 4px; background-color: #f5f7fa;">
          <span style="display: inline-block; width: 16px; height: 16px; line-height: 16px; text-align: center; border: 1px solid #67c23a; border-radius: 2px; background-color: #67c23a; color: white; margin-right: 6px; font-size: 12px;">√</span>
          <span>${p.name}</span>
        </div>
      `).join('')
    : '<div style="color: #909399; padding: 10px;">暂无权限</div>'
  
  ElMessageBox.alert(
    `<div style="text-align: left; padding: 10px;">
      <p style="margin-bottom: 12px;"><strong style="font-size: 16px;">角色：</strong><span style="font-size: 16px;">${row.roleName}</span></p>
      <p style="margin-bottom: 8px;"><strong>权限列表：</strong></p>
      <div style="max-height: 400px; overflow-y: auto; padding: 8px 0;">
        ${permissionList}
      </div>
      <p style="margin-top: 12px; color: #909399; font-size: 12px;">共 ${permissions.length} 项权限</p>
    </div>`,
    '权限详情',
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '关闭',
      width: '600px'
    }
  )
}

// 打开修改权限对话框
const openEditPermissionsDialog = (row: RolePermissionDTO & { id?: number }) => {
  currentEditRole.value = row
  editingPermissions.value = [...(row.permissions || [])]
  editPermissionsDialogVisible.value = true
}

// 关闭修改权限对话框
const closeEditPermissionsDialog = () => {
  editPermissionsDialogVisible.value = false
  currentEditRole.value = null
  editingPermissions.value = []
}

// 从对话框保存角色权限
const saveRolePermissionsFromDialog = async () => {
  if (!currentEditRole.value || !currentEditRole.value.roleId) {
    ElMessage.error('角色ID不存在')
    return
  }
  
  try {
    // 确认保存
    await ElMessageBox.confirm(
      `确定要保存角色 "${currentEditRole.value.roleName}" 的权限配置吗？`,
      '确认保存',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    savingPermissions.value = true
    const response = await updateRolePermissionsApi(currentEditRole.value.roleId, editingPermissions.value)
    
    if (response.data.code === 200) {
      ElMessage.success({
        message: `角色 ${currentEditRole.value.roleName} 的权限已保存`,
        duration: 3000
      })
      // 关闭对话框
      closeEditPermissionsDialog()
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
    savingPermissions.value = false
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
  width: 100%;
  max-width: var(--content-max-width, 1600px);
  margin: 0 auto;
  min-height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0 var(--content-padding, 24px);
  
  .page-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    padding: 20px 0;
    
    .page-header {
      margin-bottom: 24px;
      flex-shrink: 0;
      
      .page-title {
        font-size: clamp(20px, 2vw, 24px); // 响应式字体大小
        font-weight: 600;
        color: var(--text-primary-color);
        margin-bottom: 8px;
      }
      
      .page-description {
        color: var(--text-regular-color);
        font-size: clamp(12px, 1.2vw, 14px);
      }
    }
    
    :deep(.el-tabs) {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      
      .el-tabs__content {
        flex: 1;
        overflow: hidden;
        display: flex;
        flex-direction: column;
      }
      
      .el-tab-pane {
        flex: 1;
        overflow: hidden;
        display: flex;
        flex-direction: column;
        height: 100%;
      }
    }
  }
  
  // 响应式调整
  @media (max-width: 1600px) {
    max-width: 100%;
    padding: 0 20px;
  }
  
  @media (max-width: 1200px) {
    padding: 0 16px;
    
    .page-container {
      padding: 16px 0;
    }
  }
  
  @media (max-width: 768px) {
    padding: 0 12px;
    
    .page-container {
      padding: 12px 0;
      
      .page-header {
        margin-bottom: 16px;
      }
    }
  }
}

.tab-content {
  padding: 20px;
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  min-height: 0;
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    flex-shrink: 0;
    
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
    flex-shrink: 0;
  }
}

.permission-card,
.config-card {
  margin-top: 20px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 300px);
  
  :deep(.el-card__body) {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }
  
  :deep(.el-table) {
    flex: 1;
    overflow-y: auto;
  }
  
  :deep(.el-table__body-wrapper) {
    max-height: calc(100vh - 450px);
    overflow-y: auto;
  }
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

// 权限编辑对话框样式
:deep(.permission-edit-dialog) {
  .el-dialog {
    display: flex;
    flex-direction: column;
    max-height: 90vh;
    margin-top: 5vh !important;
  }
  
  .el-dialog__body {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    padding: 20px;
    min-height: 0;
  }
  
  .el-dialog__footer {
    padding: 20px !important;
    border-top: 1px solid #e4e7ed;
    display: block !important;
    visibility: visible !important;
    flex-shrink: 0;
    position: sticky;
    bottom: 0;
    background: white;
    z-index: 10;
  }
  
  .dialog-footer {
    display: flex !important;
    justify-content: flex-end;
    align-items: center;
    gap: 10px;
    visibility: visible !important;
    opacity: 1 !important;
  }
  
  .dialog-footer .el-button {
    display: inline-block !important;
    visibility: visible !important;
    opacity: 1 !important;
  }
  
  .permission-edit-header {
    display: flex;
    align-items: center;
    margin-bottom: 20px;
    padding: 12px 16px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 8px;
    color: white;
    font-weight: 500;
    flex-shrink: 0;
    
    .el-icon {
      font-size: 18px;
    }
  }
  
  .permission-checkbox-container {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    background: #fafafa;
    min-height: 0;
    
    &::-webkit-scrollbar {
      width: 8px;
    }
    
    &::-webkit-scrollbar-track {
      background: #f1f1f1;
      border-radius: 4px;
    }
    
    &::-webkit-scrollbar-thumb {
      background: #c1c1c1;
      border-radius: 4px;
      
      &:hover {
        background: #a8a8a8;
      }
    }
  }
  
  .permission-checkbox-group {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  
  .permission-checkbox-item {
    padding: 12px 16px;
    background: white;
    border: 1px solid #e4e7ed;
    border-radius: 6px;
    transition: all 0.3s ease;
    
    &:hover {
      border-color: #409eff;
      box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
      transform: translateX(4px);
    }
    
    .permission-checkbox {
      width: 100%;
      
      :deep(.el-checkbox__label) {
        font-size: 14px;
        color: #303133;
        font-weight: 500;
        padding-left: 8px;
      }
      
      :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
        background-color: #409eff;
        border-color: #409eff;
      }
      
      :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
        color: #409eff;
      }
    }
  }
}
</style>
