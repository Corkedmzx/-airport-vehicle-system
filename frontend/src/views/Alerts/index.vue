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
        
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
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
              解决
            </el-button>
            <el-button 
              size="small" 
              @click="viewAlertDetail(row)"
            >
              详情
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
                {{ currentAlert.vehiclePlate }}
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
  Search, Check, Refresh
} from '@element-plus/icons-vue'
import type { Alert } from '@/api/types'
import dayjs from 'dayjs'

const router = useRouter()

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
const viewVehicle = (vehicleId: string) => {
  router.push(`/vehicles/${vehicleId}`)
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