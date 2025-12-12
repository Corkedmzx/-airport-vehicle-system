<template>
  <div class="settings-page">
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">系统设置</h1>
        <p class="page-description">管理系统配置、用户权限和日志</p>
      </div>
      
      <el-tabs v-model="activeTab" type="border-card">
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
          <div class="tab-content">
            <div class="section-header">
              <h3>权限设置</h3>
            </div>
            <p class="section-description">配置系统权限和角色管理</p>
            
            <el-card class="permission-card">
              <template #header>
                <span>角色权限配置</span>
              </template>
              <el-table :data="rolePermissions" stripe>
                <el-table-column prop="roleName" label="角色" width="150" />
                <el-table-column label="权限" min-width="600">
                  <template #default="{ row }">
                    <el-checkbox-group v-model="row.permissions">
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
                <el-table-column label="操作" width="120">
                  <template #default="{ row }">
                    <el-button type="primary" link @click="saveRolePermissions(row)">
                      保存
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

const activeTab = ref('users')

// 角色权限配置
const rolePermissions = ref([
  {
    roleName: '系统管理员',
    roleCode: 'admin',
    permissions: ['user:create', 'user:update', 'user:delete', 'vehicle:create', 'vehicle:update', 'vehicle:delete', 'task:create', 'task:update', 'task:delete', 'task:assign', 'log:view', 'log:delete', 'config:update']
  },
  {
    roleName: '调度员',
    roleCode: 'dispatcher',
    permissions: ['vehicle:view', 'task:create', 'task:update', 'task:assign', 'task:view']
  },
  {
    roleName: '监控员',
    roleCode: 'monitor',
    permissions: ['vehicle:view', 'task:view', 'log:view']
  },
  {
    roleName: '操作员',
    roleCode: 'operator',
    permissions: ['vehicle:view', 'task:view']
  },
  {
    roleName: '查看者',
    roleCode: 'viewer',
    permissions: ['vehicle:view', 'task:view']
  }
])

const allPermissions = ref([
  { label: '用户创建', value: 'user:create' },
  { label: '用户更新', value: 'user:update' },
  { label: '用户删除', value: 'user:delete' },
  { label: '车辆创建', value: 'vehicle:create' },
  { label: '车辆更新', value: 'vehicle:update' },
  { label: '车辆删除', value: 'vehicle:delete' },
  { label: '任务创建', value: 'task:create' },
  { label: '任务更新', value: 'task:update' },
  { label: '任务删除', value: 'task:delete' },
  { label: '任务分配', value: 'task:assign' },
  { label: '日志查看', value: 'log:view' },
  { label: '日志删除', value: 'log:delete' },
  { label: '配置更新', value: 'config:update' }
])

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

// 保存角色权限
const saveRolePermissions = (row: any) => {
  // TODO: 调用API保存权限配置
  ElMessage.success(`角色 ${row.roleName} 的权限已保存`)
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
</style>
