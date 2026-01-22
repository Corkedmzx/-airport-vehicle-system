<template>
  <div class="alerts-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">告警管理</h1>
      <p class="page-description">监控和管理系统告警信息</p>
      <div class="header-actions">
        <el-button type="primary" @click="createAlertRule">
          <el-icon><Setting /></el-icon>
          告警规则
        </el-button>
        <el-button @click="exportAlerts">
          <el-icon><Download /></el-icon>
          导出报表
        </el-button>
      </div>
    </div>

    <!-- 告警统计 -->
    <div class="alerts-stats">
      <div class="stat-card high">
        <div class="stat-icon">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ alertStats.highPriority }}</div>
          <div class="stat-label">高优先级告警</div>
          <div class="stat-trend">需要立即处理</div>
        </div>
      </div>

      <div class="stat-card medium">
        <div class="stat-icon">
          <el-icon><InfoFilled /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ alertStats.mediumPriority }}</div>
          <div class="stat-label">中优先级告警</div>
          <div class="stat-trend">待处理 {{ alertStats.unprocessedMedium }}</div>
        </div>
      </div>

      <div class="stat-card resolved">
        <div class="stat-icon">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ alertStats.resolvedToday }}</div>
          <div class="stat-label">今日已解决</div>
          <div class="stat-trend">处理率 {{ alertStats.resolutionRate }}%</div>
        </div>
      </div>

      <div class="stat-card total">
        <div class="stat-icon">
          <el-icon><Bell /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ alertStats.totalToday }}</div>
          <div class="stat-label">今日告警总数</div>
          <div class="stat-trend">较昨日 {{ alertStats.changeRate > 0 ? '+' : '' }}{{ alertStats.changeRate }}%</div>
        </div>
      </div>
    </div>

    <!-- 搜索和筛选 -->
    <div class="alerts-toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索告警标题、描述、车辆信息"
          style="width: 300px"
          clearable
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-select
          v-model="searchForm.severity"
          placeholder="告警级别"
          style="width: 120px; margin-left: 12px"
          clearable
          @change="handleSearch"
        >
          <el-option label="高优先级" value="high" />
          <el-option label="中优先级" value="medium" />
          <el-option label="低优先级" value="low" />
        </el-select>
        
        <el-select
          v-model="searchForm.status"
          placeholder="处理状态"
          style="width: 120px; margin-left: 12px"
          clearable
          @change="handleSearch"
        >
          <el-option label="未处理" value="unprocessed" />
          <el-option label="处理中" value="processing" />
          <el-option label="已解决" value="resolved" />
        </el-select>
        
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 240px; margin-left: 12px"
          @change="handleSearch"
        />
      </div>
      
      <div class="toolbar-right">
        <el-button 
          type="danger" 
          @click="batchDeleteAlerts" 
          :disabled="selectedAlerts.length === 0"
        >
          <el-icon><Delete /></el-icon>
          一键删除 ({{ selectedAlerts.length }})
        </el-button>
        <el-button type="primary" @click="batchAcknowledge">
          <el-icon><Check /></el-icon>
          批量确认
        </el-button>
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 告警列表 -->
    <div class="alerts-table">
      <el-table
        v-loading="loading"
        :data="filteredAlerts"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="createdAt" label="告警时间" width="150">
          <template #default="{ row }">
            <div class="alert-time">
              <div class="date">{{ formatDate(row.createdAt) }}</div>
              <div class="time">{{ formatTime(row.createdAt) }}</div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="severity" label="级别" width="100">
          <template #default="{ row }">
            <el-tag 
              :type="getSeverityType(row.severity)" 
              size="small"
            >
              {{ getSeverityText(row.severity) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="title" label="告警标题" min-width="200">
          <template #default="{ row }">
            <div class="alert-title">
              <div class="title-text">{{ row.title }}</div>
              <div class="title-description">{{ row.description }}</div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="vehiclePlate" label="相关车辆" width="120">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              link 
              @click="viewVehicle(row.vehicleId)"
            >
              {{ row.vehiclePlate }}
            </el-button>
          </template>
        </el-table-column>
        
        <el-table-column prop="category" label="类别" width="120">
          <template #default="{ row }">
            {{ getCategoryText(row.category) }}
          </template>
        </el-table-column>
        
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
        
        <el-table-column prop="assignee" label="处理人" width="100">
          <template #default="{ row }">
            {{ row.assignee || '-' }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <!-- 维修员按钮：确认、完成、报告 -->
            <template v-if="isMaintenanceRole">
              <el-button 
                v-if="row.status === 'unprocessed'"
                type="primary" 
                size="small" 
                @click="acknowledgeAlert(row)"
              >
                确认
              </el-button>
              <el-button 
                v-if="row.status === 'processing'"
                type="success" 
                size="small" 
                @click="resolveAlert(row)"
              >
                完成
              </el-button>
              <el-button 
                type="warning" 
                size="small" 
                @click="sendReportEmail(row)"
              >
                报告
              </el-button>
            </template>
            
            <!-- 管理员按钮：分配任务 -->
            <template v-if="isAdminRole">
              <el-button 
                v-if="row.category === 'vehicle_fault' && row.status !== 'resolved'"
                type="warning" 
                size="small" 
                @click="assignTaskFromAlert(row)"
              >
                分配任务
              </el-button>
            </template>
            
            <!-- 通用按钮：详情、删除 -->
            <el-button 
              size="small" 
              @click="viewAlertDetail(row)"
            >
              详情
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              @click="deleteAlert(row)"
            >
              删除
            </el-button>
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

    <!-- 告警详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="告警详情"
      width="600px"
      @close="resetDetailForm"
    >
      <div v-if="currentAlert" class="alert-detail">
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <label>告警ID:</label>
              <span>{{ currentAlert.id }}</span>
            </div>
            <div class="detail-item">
              <label>告警级别:</label>
              <el-tag :type="getSeverityType(currentAlert.severity)" size="small">
                {{ getSeverityText(currentAlert.severity) }}
              </el-tag>
            </div>
            <div class="detail-item">
              <label>告警时间:</label>
              <span>{{ formatDateTime(currentAlert.createdAt) }}</span>
            </div>
            <div class="detail-item">
              <label>处理状态:</label>
              <el-tag :type="getStatusType(currentAlert.status)" size="small">
                {{ getStatusText(currentAlert.status) }}
              </el-tag>
            </div>
          </div>
        </div>
        
        <div class="detail-section">
          <h4>告警内容</h4>
          <div class="alert-content">
            <div class="content-item">
              <label>标题:</label>
              <span>{{ currentAlert.title }}</span>
            </div>
            <div class="content-item">
              <label>描述:</label>
              <span>{{ currentAlert.description }}</span>
            </div>
            <div class="content-item">
              <label>相关车辆:</label>
              <el-button type="primary" link @click="viewVehicle(currentAlert.vehicleId)">
                {{ currentAlert.vehiclePlate || '查看车辆' }}
              </el-button>
            </div>
            <div v-if="currentAlert.reportId" class="content-item">
              <label>关联报告:</label>
              <el-button type="primary" link @click="viewReport(currentAlert.reportId)">
                查看报告 #{{ currentAlert.reportId }}
              </el-button>
            </div>
          </div>
        </div>
        
        <div v-if="currentAlert.resolution" class="detail-section">
          <h4>处理信息</h4>
          <div class="resolution-content">
            <div class="content-item">
              <label>处理人:</label>
              <span>{{ currentAlert.resolution.assignee }}</span>
            </div>
            <div class="content-item">
              <label>处理时间:</label>
              <span>{{ formatDateTime(currentAlert.resolution.resolvedAt) }}</span>
            </div>
            <div class="content-item">
              <label>处理说明:</label>
              <span>{{ currentAlert.resolution.notes }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button 
            v-if="currentAlert?.status === 'unprocessed'"
            type="primary" 
            @click="acknowledgeAlert(currentAlert)"
          >
            确认告警
          </el-button>
          <el-button 
            v-if="currentAlert?.status === 'processing'"
            type="success" 
            @click="resolveAlert(currentAlert)"
          >
            标记已解决
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 分配任务对话框 -->
    <el-dialog
      v-model="assignTaskDialogVisible"
      title="从告警创建维护任务"
      width="600px"
      @close="resetAssignTaskForm"
    >
      <el-form
        ref="assignTaskFormRef"
        :model="assignTaskForm"
        :rules="assignTaskRules"
        label-width="120px"
      >
        <el-form-item label="告警信息">
          <el-input :value="currentAlert?.title" disabled />
        </el-form-item>
        
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="assignTaskForm.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        
        <el-form-item label="任务类型">
          <el-input value="维护调度" disabled />
        </el-form-item>
        
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="assignTaskForm.priority" style="width: 100%">
            <el-option label="低" :value="1" />
            <el-option label="中" :value="2" />
            <el-option label="高" :value="3" />
            <el-option label="紧急" :value="4" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="车辆位置" prop="startLocation">
          <el-input v-model="assignTaskForm.startLocation" placeholder="请输入车辆位置" />
        </el-form-item>
        
        <el-form-item label="维修地点" prop="endLocation">
          <el-input v-model="assignTaskForm.endLocation" placeholder="请输入维修地点" />
        </el-form-item>
        
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="assignTaskForm.startTime"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        
        <el-form-item label="任务描述" prop="description">
          <el-input
            v-model="assignTaskForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入任务描述（将从告警信息自动填充）"
          />
        </el-form-item>
        
        <el-form-item label="分配维修员" prop="maintenanceUsername">
          <el-select
            v-model="assignTaskForm.maintenanceUsername"
            placeholder="请选择维修员"
            style="width: 100%"
            filterable
            @focus="loadMaintenance"
          >
            <el-option
              v-for="maintenance in availableMaintenance"
              :key="maintenance.id"
              :label="`${maintenance.realName || maintenance.username} (${maintenance.username})`"
              :value="maintenance.username"
            >
              <span>{{ maintenance.realName || maintenance.username }}</span>
              <span style="color: #8492a6; font-size: 13px; margin-left: 8px;">{{ maintenance.username }}</span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="assignTaskDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmAssignTask" :loading="assignTaskLoading">
            创建并分配任务
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 告警规则管理对话框 -->
    <el-dialog
      v-model="ruleDialogVisible"
      title="告警规则管理"
      width="900px"
      @close="ruleForm = {
        id: undefined,
        ruleName: '',
        ruleType: 'vehicle_fault',
        conditionType: '大于',
        conditionValue: '',
        severity: 'medium',
        enabled: true,
        description: ''
      }"
    >
      <el-tabs>
        <el-tab-pane label="规则列表">
          <el-table :data="alertRules" style="width: 100%">
            <el-table-column prop="ruleName" label="规则名称" width="150" />
            <el-table-column prop="ruleType" label="规则类型" width="120">
              <template #default="{ row }">
                {{ ruleTypes.find(t => t.value === row.ruleType)?.label || row.ruleType }}
              </template>
            </el-table-column>
            <el-table-column prop="conditionType" label="条件类型" width="100" />
            <el-table-column prop="conditionValue" label="条件值" width="100" />
            <el-table-column prop="severity" label="严重程度" width="100">
              <template #default="{ row }">
                <el-tag :type="getSeverityType(row.severity)" size="small">
                  {{ severities.find(s => s.value === row.severity)?.label || row.severity }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="enabled" label="状态" width="80">
              <template #default="{ row }">
                <el-switch
                  v-model="row.enabled"
                  @change="toggleAlertRule(row.id)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button type="primary" link @click="editAlertRule(row)">
                  编辑
                </el-button>
                <el-button type="danger" link @click="deleteAlertRule(row.id)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="ruleForm.id ? '编辑规则' : '新建规则'">
          <el-form :model="ruleForm" label-width="120px">
            <el-form-item label="规则名称">
              <el-input v-model="ruleForm.ruleName" placeholder="请输入规则名称" />
            </el-form-item>
            <el-form-item label="规则类型">
              <el-select v-model="ruleForm.ruleType" style="width: 100%">
                <el-option
                  v-for="type in ruleTypes"
                  :key="type.value"
                  :label="type.label"
                  :value="type.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="条件类型">
              <el-select v-model="ruleForm.conditionType" style="width: 100%">
                <el-option
                  v-for="type in conditionTypes"
                  :key="type.value"
                  :label="type.label"
                  :value="type.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="条件值">
              <el-input v-model="ruleForm.conditionValue" placeholder="请输入条件值" />
            </el-form-item>
            <el-form-item label="严重程度">
              <el-select v-model="ruleForm.severity" style="width: 100%">
                <el-option
                  v-for="severity in severities"
                  :key="severity.value"
                  :label="severity.label"
                  :value="severity.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="是否启用">
              <el-switch v-model="ruleForm.enabled" />
            </el-form-item>
            <el-form-item label="规则描述">
              <el-input
                v-model="ruleForm.description"
                type="textarea"
                :rows="3"
                placeholder="请输入规则描述"
              />
            </el-form-item>
          </el-form>
          <div style="text-align: right; margin-top: 20px">
            <el-button @click="ruleDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="saveAlertRule">保存</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Setting, Download, Warning, InfoFilled, CircleCheck, Bell,
  Search, Check, Refresh, Message, Delete
} from '@element-plus/icons-vue'
import type { Alert } from '@/api/types'
import dayjs from 'dayjs'
import { sendAlertEmailApi } from '@/api/email'
import { useUserStore } from '@/store/user'
import { isMaintenance, hasPermission } from '@/utils/permission'

const router = useRouter()
const userStore = useUserStore()

// 判断是否为维修员
const isMaintenanceRole = computed(() => isMaintenance())

// 判断是否为管理员（有任务分配权限）
const isAdminRole = computed(() => hasPermission('task:assign'))

// 告警统计数据
const alertStats = ref({
  highPriority: 0,
  mediumPriority: 0,
  lowPriority: 0,
  unprocessedMedium: 0,
  resolvedToday: 0,
  resolutionRate: 0,
  totalToday: 0,
  changeRate: 0
})

// 告警列表
const alerts = ref<Alert[]>([])

// 搜索表单
const searchForm = ref({
  keyword: '',
  severity: '',
  status: '',
  dateRange: []
})

// 分页信息
const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

// 选中项
const selectedAlerts = ref<Alert[]>([])

// 加载状态
const loading = ref(false)

// 详情对话框
const detailDialogVisible = ref(false)
const currentAlert = ref<Alert | null>(null)

// 分配任务对话框
const assignTaskDialogVisible = ref(false)
const assignTaskFormRef = ref()
const assignTaskLoading = ref(false)
const assignTaskForm = ref({
  taskName: '',
  priority: 3,
  startLocation: '',
  endLocation: '',
  startTime: '',
  description: '',
  maintenanceUsername: ''
})
const assignTaskRules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  startLocation: [{ required: true, message: '请输入车辆位置', trigger: 'blur' }],
  endLocation: [{ required: true, message: '请输入维修地点', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  maintenanceUsername: [{ required: true, message: '请选择维修员', trigger: 'change' }]
}

// 可用维修员列表
const availableMaintenance = ref<any[]>([])
const maintenanceLoaded = ref(false)

// 过滤后的告警列表
const filteredAlerts = computed(() => {
  let result = alerts.value
  
  // 关键词搜索
  if (searchForm.value.keyword) {
    const keyword = searchForm.value.keyword.toLowerCase()
    result = result.filter(alert => 
      alert.title.toLowerCase().includes(keyword) ||
      alert.description.toLowerCase().includes(keyword) ||
      alert.vehiclePlate.toLowerCase().includes(keyword)
    )
  }
  
  // 级别筛选
  if (searchForm.value.severity) {
    result = result.filter(alert => alert.severity === searchForm.value.severity)
  }
  
  // 状态筛选
  if (searchForm.value.status) {
    result = result.filter(alert => alert.status === searchForm.value.status)
  }
  
  // 日期范围筛选
  if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
    const [startDate, endDate] = searchForm.value.dateRange
    result = result.filter(alert => {
      const alertDate = dayjs(alert.createdAt)
      return alertDate.isAfter(startDate) && alertDate.isBefore(endDate)
    })
  }
  
  return result
})

// 获取告警级别类型
const getSeverityType = (severity: string) => {
  const typeMap: Record<string, string> = {
    high: 'danger',
    medium: 'warning',
    low: 'info'
  }
  return typeMap[severity] || 'info'
}

// 获取告警级别文本
const getSeverityText = (severity: string) => {
  const textMap: Record<string, string> = {
    high: '高优先级',
    medium: '中优先级',
    low: '低优先级'
  }
  return textMap[severity] || '未知'
}

// 获取状态类型
const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    unprocessed: 'danger',
    processing: 'warning',
    resolved: 'success'
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    unprocessed: '未处理',
    processing: '处理中',
    resolved: '已解决'
  }
  return textMap[status] || '未知'
}

// 获取类别文本
const getCategoryText = (category: string) => {
  const textMap: Record<string, string> = {
    vehicle_fault: '车辆故障',
    task_timeout: '任务超时',
    system_error: '系统错误',
    safety_alert: '安全告警'
  }
  return textMap[category] || category
}

// 格式化日期
const formatDate = (time: string) => {
  return dayjs(time).format('MM-DD')
}

// 格式化时间
const formatTime = (time: string) => {
  return dayjs(time).format('HH:mm:ss')
}

// 格式化日期时间
const formatDateTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

// 查看车辆
const viewVehicle = (vehicleId: string | number) => {
  router.push(`/vehicles/${vehicleId}`)
}

// 查看报告
const viewReport = (reportId: string | number) => {
  // 可以跳转到报告详情页面，或者显示报告信息
  ElMessage.info(`报告ID: ${reportId}，报告详情功能开发中`)
  // 如果将来有报告详情页面，可以使用：
  // router.push(`/vehicle-reports/${reportId}`)
}

// 查看告警详情
const viewAlertDetail = (alert: Alert) => {
  currentAlert.value = alert
  detailDialogVisible.value = true
}

// 重置详情表单
const resetDetailForm = () => {
  currentAlert.value = null
}

// 从告警分配任务
const assignTaskFromAlert = async (alert: Alert) => {
  currentAlert.value = alert
  
  // 加载车辆信息
  if (alert.vehicleId) {
    try {
      const { getVehicleApi } = await import('@/api/vehicles')
      const response = await getVehicleApi(Number(alert.vehicleId))
      if (response.data.code === 200) {
        const vehicle = response.data.data
        assignTaskForm.value.startLocation = vehicle.locationAddress || vehicle.vehicleNo + ' 当前位置'
      }
    } catch (error) {
      console.error('加载车辆信息失败:', error)
    }
  }
  
  // 填充表单
  assignTaskForm.value = {
    taskName: `维修任务 - ${alert.title}`,
    priority: alert.severity === 'high' ? 4 : alert.severity === 'medium' ? 3 : 2,
    startLocation: assignTaskForm.value.startLocation || '待确认',
    endLocation: '维修车间',
    startTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    description: alert.description || '',
    maintenanceUsername: ''
  }
  
  assignTaskDialogVisible.value = true
  loadMaintenance()
}

// 加载维修员列表
const loadMaintenance = async () => {
  if (maintenanceLoaded.value && availableMaintenance.value.length > 0) return
  
  try {
    const { getUsersApi } = await import('@/api/users')
    const response = await getUsersApi({ page: 0, size: 1000 })
    
    if (response.data.code === 200) {
      let users: any[] = []
      if (response.data.data?.content && Array.isArray(response.data.data.content)) {
        users = response.data.data.content
      } else if (Array.isArray(response.data.data)) {
        users = response.data.data
      }
      
      // 筛选出维修员角色的用户
      availableMaintenance.value = users.filter((user: any) => {
        if (user.status !== 1 && user.status !== 'active') {
          return false
        }
        
        if (user.roles && Array.isArray(user.roles)) {
          if (user.roles.length > 0 && typeof user.roles[0] === 'object') {
            return user.roles.some((role: any) => 
              role.roleCode === 'MAINTENANCE' || 
              role.roleName?.includes('维修') ||
              role.roleName?.toLowerCase().includes('maintenance')
            )
          } else if (typeof user.roles[0] === 'string') {
            return user.roles.includes('MAINTENANCE') || 
                   user.roles.some((code: string) => code.toLowerCase().includes('maintenance'))
          }
        }
        
        if (user.role === 'MAINTENANCE' || user.role?.toLowerCase() === 'maintenance') {
          return true
        }
        
        return false
      })
      
      maintenanceLoaded.value = true
    }
  } catch (error) {
    console.error('加载维修员列表失败:', error)
    ElMessage.warning('加载维修员列表失败，请刷新重试')
  }
}

// 确认分配任务
const confirmAssignTask = async () => {
  if (!assignTaskFormRef.value) return
  
  try {
    await assignTaskFormRef.value.validate()
    
    if (!currentAlert.value) {
      ElMessage.error('告警信息不存在')
      return
    }
    
    assignTaskLoading.value = true
    
    // 创建维护调度任务
    const { createTaskApi } = await import('@/api/tasks')
    const taskData = {
      taskName: assignTaskForm.value.taskName,
      taskType: '维护调度',
      priority: assignTaskForm.value.priority,
      startLocation: assignTaskForm.value.startLocation,
      endLocation: assignTaskForm.value.endLocation,
      startTime: assignTaskForm.value.startTime,
      description: assignTaskForm.value.description,
      status: 1 // 待分配
    }
    
    const createResponse = await createTaskApi(taskData)
    
    if (createResponse.data.code === 200) {
      const createdTask = createResponse.data.data
      
      // 分配任务给维修员
      if (currentAlert.value.vehicleId) {
        const { assignTaskToMaintenanceApi } = await import('@/api/tasks')
        await assignTaskToMaintenanceApi(
          createdTask.id,
          Number(currentAlert.value.vehicleId),
          undefined,
          assignTaskForm.value.maintenanceUsername
        )
        
        // 更新告警状态为处理中，并关联任务ID
        const { acknowledgeAlertApi, updateAlertApi } = await import('@/api/alerts')
        // 先更新告警关联任务ID
        try {
          await updateAlertApi(Number(currentAlert.value.id), { taskId: createdTask.id })
        } catch (error) {
          console.warn('更新告警任务ID失败:', error)
        }
        // 然后确认告警
        await acknowledgeAlertApi(Number(currentAlert.value.id))
        
        ElMessage.success('任务创建并分配成功，已通知维修员')
        assignTaskDialogVisible.value = false
        await loadAlerts()
        await loadAlertStats()
      } else {
        ElMessage.error('告警未关联车辆，无法分配任务')
      }
    } else {
      ElMessage.error(createResponse.data.message || '创建任务失败')
    }
  } catch (error: any) {
    if (error !== false) {
      ElMessage.error(error?.response?.data?.message || '分配任务失败')
    }
  } finally {
    assignTaskLoading.value = false
  }
}

// 重置分配任务表单
const resetAssignTaskForm = () => {
  assignTaskForm.value = {
    taskName: '',
    priority: 3,
    startLocation: '',
    endLocation: '',
    startTime: '',
    description: '',
    maintenanceUsername: ''
  }
  assignTaskFormRef.value?.clearValidate()
}

// 确认告警
const acknowledgeAlert = async (alert: Alert) => {
  try {
    await ElMessageBox.confirm(
      `确认处理告警"${alert.title}"吗？`,
      '确认告警',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const { acknowledgeAlertApi } = await import('@/api/alerts')
    await acknowledgeAlertApi(Number(alert.id))
    
    ElMessage.success('告警已确认')
    detailDialogVisible.value = false
    await loadAlertStats()
    await loadAlerts()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '确认失败')
    }
  }
}

// 解决告警
const resolveAlert = async (alert: Alert) => {
  try {
    const { value: notes } = await ElMessageBox.prompt(
      '请输入处理说明',
      '解决告警',
      {
        confirmButtonText: '确认解决',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '请输入处理说明'
      }
    )
    
    const { resolveAlertApi } = await import('@/api/alerts')
    await resolveAlertApi(Number(alert.id), notes)
    
    ElMessage.success('告警已解决')
    detailDialogVisible.value = false
    await loadAlertStats()
    await loadAlerts()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '解决失败')
    }
  }
}

// 批量确认
const batchAcknowledge = async () => {
  if (selectedAlerts.value.length === 0) {
    ElMessage.warning('请选择要确认的告警')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确认处理选中的 ${selectedAlerts.value.length} 个告警吗？`,
      '批量确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const { acknowledgeAlertApi } = await import('@/api/alerts')
    await Promise.all(
      selectedAlerts.value.map(alert => acknowledgeAlertApi(Number(alert.id)))
    )
    
    ElMessage.success(`已确认 ${selectedAlerts.value.length} 个告警`)
    await loadAlertStats()
    await loadAlerts()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '批量确认失败')
    }
  }
}

// 告警规则管理
const ruleDialogVisible = ref(false)
const alertRules = ref<any[]>([])
const ruleForm = ref({
  id: undefined as number | undefined,
  ruleName: '',
  ruleType: 'vehicle_fault',
  conditionType: '大于',
  conditionValue: '',
  severity: 'medium',
  enabled: true,
  description: ''
})

const ruleTypes = [
  { label: '车辆故障', value: 'vehicle_fault' },
  { label: '任务超时', value: 'task_timeout' },
  { label: '系统错误', value: 'system_error' },
  { label: '安全告警', value: 'safety_alert' },
  { label: '油量低', value: 'fuel_low' },
  { label: '速度超限', value: 'speed_exceed' }
]

const conditionTypes = [
  { label: '大于', value: '大于' },
  { label: '小于', value: '小于' },
  { label: '等于', value: '等于' },
  { label: '范围', value: '范围' }
]

const severities = [
  { label: '高优先级', value: 'high' },
  { label: '中优先级', value: 'medium' },
  { label: '低优先级', value: 'low' }
]

// 创建/编辑告警规则
const createAlertRule = async () => {
  ruleForm.value = {
    id: undefined,
    ruleName: '',
    ruleType: 'vehicle_fault',
    conditionType: '大于',
    conditionValue: '',
    severity: 'medium',
    enabled: true,
    description: ''
  }
  ruleDialogVisible.value = true
  await loadAlertRules()
}

// 加载告警规则列表
const loadAlertRules = async () => {
  try {
    const { getAlertRulesApi } = await import('@/api/alertRules')
    const response = await getAlertRulesApi()
    if (response.data && Array.isArray(response.data)) {
      alertRules.value = response.data
    } else if (response.data?.code === 200) {
      alertRules.value = response.data.data || []
    }
  } catch (error) {
    console.error('Load alert rules failed:', error)
  }
}

// 编辑告警规则
const editAlertRule = (rule: any) => {
  ruleForm.value = { ...rule }
  ruleDialogVisible.value = true
}

// 删除告警规则
const deleteAlertRule = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该告警规则吗？', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const { deleteAlertRuleApi } = await import('@/api/alertRules')
    await deleteAlertRuleApi(id)
    ElMessage.success('告警规则删除成功')
    await loadAlertRules()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '删除失败')
    }
  }
}

// 保存告警规则
const saveAlertRule = async () => {
  try {
    const { createAlertRuleApi, updateAlertRuleApi } = await import('@/api/alertRules')
    if (ruleForm.value.id) {
      await updateAlertRuleApi(ruleForm.value.id, ruleForm.value)
      ElMessage.success('告警规则更新成功')
    } else {
      await createAlertRuleApi(ruleForm.value)
      ElMessage.success('告警规则创建成功')
    }
    ruleDialogVisible.value = false
    await loadAlertRules()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '保存失败')
  }
}

// 切换告警规则状态
const toggleAlertRule = async (id: number) => {
  try {
    const { toggleAlertRuleApi } = await import('@/api/alertRules')
    await toggleAlertRuleApi(id)
    ElMessage.success('告警规则状态更新成功')
    await loadAlertRules()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '更新失败')
  }
}

// 导出告警
const exportAlerts = async () => {
  try {
    ElMessage.info('正在导出告警数据...')
    
    // 获取所有告警数据
    const { getAlertsApi } = await import('@/api/alerts')
    const response = await getAlertsApi({
      page: 0,
      size: 10000 // 获取所有数据
    })
    
    if (response.data.code === 200) {
      const alerts = response.data.data.content || []
      
      // 转换为CSV格式
      const headers = ['告警ID', '标题', '描述', '严重程度', '类别', '状态', '创建时间', '解决时间']
      const rows = alerts.map((alert: any) => [
        alert.id,
        alert.title,
        alert.description,
        alert.severity === 'high' ? '高' : alert.severity === 'medium' ? '中' : '低',
        alert.category,
        alert.status === 'resolved' ? '已解决' : alert.status === 'acknowledged' ? '已确认' : '未处理',
        alert.createdAt,
        alert.resolution || '-'
      ])
      
      // 创建CSV内容
      const csvContent = [
        headers.join(','),
        ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
      ].join('\n')
      
      // 创建下载链接
      const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
      const link = document.createElement('a')
      const url = URL.createObjectURL(blob)
      link.setAttribute('href', url)
      link.setAttribute('download', `告警数据_${new Date().toISOString().split('T')[0]}.csv`)
      link.style.visibility = 'hidden'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      
      ElMessage.success('告警数据导出成功')
    }
  } catch (error) {
    console.error('Export alerts failed:', error)
    ElMessage.error('导出失败，请稍后重试')
  }
}

// 搜索处理
const handleSearch = () => {
  pagination.value.page = 1
}

// 选择项变化
const handleSelectionChange = (selection: Alert[]) => {
  selectedAlerts.value = selection
}

// 删除告警
const deleteAlert = async (alert: Alert) => {
  try {
    await ElMessageBox.confirm('确定要删除这条告警吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const { deleteAlertApi } = await import('@/api/alerts')
    const response = await deleteAlertApi(Number(alert.id))
    if (response.data.code === 200) {
      ElMessage.success('删除成功')
      await loadData()
    } else {
      ElMessage.error(response.data.message || '删除失败')
    }
  } catch (error: any) {
    if (error === 'cancel') {
      return
    }
    console.error('Delete alert failed:', error)
    ElMessage.error(error?.response?.data?.message || '删除失败')
  }
}

// 批量删除告警
const batchDeleteAlerts = async () => {
  if (selectedAlerts.value.length === 0) {
    ElMessage.warning('请先选择要删除的告警')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedAlerts.value.length} 条告警吗？此操作不可恢复！`,
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const { deleteAlertsApi } = await import('@/api/alerts')
    const alertIds = selectedAlerts.value.map(alert => Number(alert.id))
    const response = await deleteAlertsApi(alertIds)
    if (response.data.code === 200) {
      ElMessage.success(`成功删除 ${alertIds.length} 条告警`)
      selectedAlerts.value = []
      await loadData()
    } else {
      ElMessage.error(response.data.message || '批量删除失败')
    }
  } catch (error: any) {
    if (error === 'cancel') {
      return
    }
    console.error('Batch delete alerts failed:', error)
    ElMessage.error(error?.response?.data?.message || '批量删除失败')
  }
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
      loadAlertStats(),
      loadAlerts()
    ])
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 加载告警统计数据
const loadAlertStats = async () => {
  try {
    const { getAlertStatisticsApi } = await import('@/api/alerts')
    const response = await getAlertStatisticsApi()
    if (response.data.code === 200) {
      const stats = response.data.data
      alertStats.value = {
        highPriority: stats.highPriority || 0,
        mediumPriority: stats.mediumPriority || 0,
        lowPriority: stats.lowPriority || 0,
        unprocessedMedium: stats.unprocessedMedium || 0,
        resolvedToday: stats.resolvedToday || 0,
        resolutionRate: stats.resolutionRate || 0,
        totalToday: stats.totalToday || 0,
        changeRate: stats.changeRate || 0
      }
    }
  } catch (error) {
    console.error('Load alert stats failed:', error)
  }
}

// 加载告警统计数据（旧代码保留作为备用）
const loadAlertStatsOld = async () => {
  try {
    // 模拟数据
    alertStats.value = {
      highPriority: 3,
      mediumPriority: 8,
      lowPriority: 15,
      unprocessedMedium: 2,
      resolvedToday: 12,
      resolutionRate: 85,
      totalToday: 26,
      changeRate: -12
    }
  } catch (error) {
    console.error('Load alert stats failed:', error)
  }
}

// 加载告警列表
const loadAlerts = async () => {
  try {
    const { getAlertsApi } = await import('@/api/alerts')
    const response = await getAlertsApi({
      page: pagination.value.page - 1,
      size: pagination.value.size,
      severity: searchForm.value.severity || undefined,
      status: searchForm.value.status || undefined,
      keyword: searchForm.value.keyword || undefined
    })
    
    if (response.data.code === 200) {
      const pageData = response.data.data
      alerts.value = (pageData.content || []).map((alert: any) => ({
        id: alert.id.toString(),
        title: alert.title,
        description: alert.description,
        severity: alert.severity,
        category: alert.category,
        vehicleId: alert.vehicleId?.toString(),
        vehiclePlate: alert.vehicleId ? `车辆${alert.vehicleId}` : '-',
        status: alert.status,
        assignee: alert.assignee,
        createdAt: alert.createTime,
        acknowledged: alert.acknowledged || false,
        resolution: alert.resolvedTime ? {
          assignee: alert.assignee || '',
          resolvedAt: alert.resolvedTime,
          notes: alert.resolutionNotes || ''
        } : undefined
      }))
      pagination.value.total = pageData.totalElements || 0
    }
  } catch (error) {
    console.error('Load alerts failed:', error)
    alerts.value = []
  }
}

// 加载告警列表（旧代码保留作为备用）
const loadAlertsOld = async () => {
  try {
    // 模拟数据
    alerts.value = [
      {
        id: '1',
        title: '车辆引擎温度过高',
        description: '京A12345车辆引擎温度超过正常范围',
        severity: 'high',
        category: 'vehicle_fault',
        vehicleId: '1',
        vehiclePlate: '京A12345',
        status: 'unprocessed',
        createdAt: new Date(Date.now() - 300000).toISOString(),
        acknowledged: false
      },
      {
        id: '2',
        title: '任务执行超时',
        description: 'T3-01接机任务执行时间超时',
        severity: 'medium',
        category: 'task_timeout',
        vehicleId: '1',
        vehiclePlate: '京A12345',
        status: 'processing',
        assignee: '张三',
        createdAt: new Date(Date.now() - 600000).toISOString(),
        acknowledged: true,
        resolution: {
          assignee: '张三',
          resolvedAt: new Date(Date.now() - 300000).toISOString(),
          notes: '已重新调度任务'
        }
      },
      {
        id: '3',
        title: '车辆离线告警',
        description: '京B67890车辆已离线超过30分钟',
        severity: 'medium',
        category: 'system_error',
        vehicleId: '2',
        vehiclePlate: '京B67890',
        status: 'resolved',
        assignee: '李四',
        createdAt: new Date(Date.now() - 1800000).toISOString(),
        acknowledged: true,
        resolution: {
          assignee: '李四',
          resolvedAt: new Date(Date.now() - 900000).toISOString(),
          notes: '已联系司机确认设备故障，已安排维修'
        }
      }
    ]
    pagination.value.total = alerts.value.length
  } catch (error) {
    console.error('Load alerts failed:', error)
  }
}

// 维修员发送报告邮件给管理员
const sendReportEmail = async (alert: Alert) => {
  try {
    await ElMessageBox.confirm(
      '确定要向管理员发送告警报告邮件吗？',
      '发送报告邮件',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    const { default: request } = await import('@/utils/request')
    const response = await request.post(`/alerts/${alert.id}/send-report-email`)
    
    if (response.data.code === 200) {
      ElMessage.success('报告邮件已发送给管理员')
    } else {
      ElMessage.error(response.data.message || '发送失败')
    }
  } catch (error: any) {
    if (error === 'cancel') {
      return
    }
    console.error('发送报告邮件失败:', error)
    ElMessage.error(error?.response?.data?.message || '发送报告邮件失败')
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.alerts-page {
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

.alerts-stats {
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
    
    &.high {
      background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
    }
    
    &.medium {
      background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
    }
    
    &.resolved {
      background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
    }
    
    &.total {
      background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
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

.alerts-toolbar {
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

.alerts-table {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.alert-time {
  .date {
    font-size: 14px;
    color: var(--text-primary-color);
    margin-bottom: 2px;
  }
  
  .time {
    font-size: 12px;
    color: var(--text-secondary-color);
  }
}

.alert-title {
  .title-text {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary-color);
    margin-bottom: 4px;
  }
  
  .title-description {
    font-size: 14px;
    color: var(--text-regular-color);
    line-height: 1.4;
  }
}

.pagination-wrapper {
  padding: 20px;
  display: flex;
  justify-content: center;
  border-top: 1px solid var(--border-lighter-color);
}

.alert-detail {
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
  
  .alert-content,
  .resolution-content {
    .content-item {
      display: flex;
      align-items: flex-start;
      margin-bottom: 12px;
      
      label {
        font-size: 14px;
        color: var(--text-secondary-color);
        width: 80px;
        margin-bottom: 0;
        margin-top: 4px;
      }
      
      span {
        font-size: 14px;
        color: var(--text-primary-color);
        flex: 1;
      }
    }
  }
}

@media (max-width: 1200px) {
  .alerts-stats {
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
  
  .alerts-toolbar {
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
  
  .alerts-stats {
    grid-template-columns: 1fr;
  }
}
</style>