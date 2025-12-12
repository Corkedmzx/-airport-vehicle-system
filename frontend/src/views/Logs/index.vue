<template>
  <div class="logs-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">系统日志</h1>
      <p class="page-description">查看系统操作日志、错误日志和审计日志</p>
      <div class="header-actions">
        <el-button @click="exportLogs">
          <el-icon><Download /></el-icon>
          导出日志
        </el-button>
        <el-button @click="clearLogs">
          <el-icon><Delete /></el-icon>
          清空日志
        </el-button>
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 日志统计 -->
    <div class="logs-stats">
      <div class="stat-card total">
        <div class="stat-icon">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ logStats.totalLogs }}</div>
          <div class="stat-label">总日志数</div>
          <div class="stat-trend">今日 {{ logStats.todayLogs }}</div>
        </div>
      </div>

      <div class="stat-card errors">
        <div class="stat-icon">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ logStats.errorLogs }}</div>
          <div class="stat-label">错误日志</div>
          <div class="stat-trend">需关注 {{ logStats.unreadErrors }}</div>
        </div>
      </div>

      <div class="stat-card warnings">
        <div class="stat-icon">
          <el-icon><InfoFilled /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ logStats.warningLogs }}</div>
          <div class="stat-label">警告日志</div>
          <div class="stat-trend">今日 {{ logStats.todayWarnings }}</div>
        </div>
      </div>

      <div class="stat-card info">
        <div class="stat-icon">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ logStats.infoLogs }}</div>
          <div class="stat-label">信息日志</div>
          <div class="stat-trend">正常运行</div>
        </div>
      </div>
    </div>

    <!-- 搜索和筛选 -->
    <div class="logs-toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索日志内容、操作人、IP地址"
          style="width: 300px"
          clearable
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-select
          v-model="searchForm.level"
          placeholder="日志级别"
          style="width: 120px; margin-left: 12px"
          clearable
          @change="handleSearch"
        >
          <el-option label="ERROR" value="ERROR" />
          <el-option label="WARN" value="WARN" />
          <el-option label="INFO" value="INFO" />
          <el-option label="DEBUG" value="DEBUG" />
        </el-select>
        
        <el-select
          v-model="searchForm.category"
          placeholder="日志类别"
          style="width: 120px; margin-left: 12px"
          clearable
          @change="handleSearch"
        >
          <el-option label="操作日志" value="OPERATION" />
          <el-option label="系统日志" value="SYSTEM" />
          <el-option label="错误日志" value="ERROR" />
          <el-option label="审计日志" value="AUDIT" />
        </el-select>
        
        <el-date-picker
          v-model="searchForm.dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          style="width: 320px; margin-left: 12px"
          @change="handleSearch"
        />
      </div>
      
      <div class="toolbar-right">
        <el-button @click="toggleAutoRefresh">
          <el-icon><Refresh /></el-icon>
          {{ autoRefresh ? '停止刷新' : '自动刷新' }}
        </el-button>
      </div>
    </div>

    <!-- 日志列表 -->
    <div class="logs-table">
      <el-table
        v-loading="loading"
        :data="filteredLogs"
        style="width: 100%"
        @selection-change="handleSelectionChange"
        :row-class-name="getRowClassName"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="timestamp" label="时间" width="160">
          <template #default="{ row }">
            <div class="log-time">
              <div class="date">{{ formatDate(row.timestamp) }}</div>
              <div class="time">{{ formatTime(row.timestamp) }}</div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <el-tag 
              :type="getLevelType(row.level)" 
              size="small"
            >
              {{ row.level }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="category" label="类别" width="120">
          <template #default="{ row }">
            {{ getCategoryText(row.category) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="message" label="日志内容" min-width="300">
          <template #default="{ row }">
            <div class="log-message">
              <div class="message-text" :class="getMessageClass(row.level)">
                {{ row.message }}
              </div>
              <div v-if="row.stackTrace" class="stack-trace">
                <el-button 
                  type="primary" 
                  link 
                  size="small" 
                  @click="showStackTrace(row)"
                >
                  查看详情
                </el-button>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="user" label="操作人" width="120">
          <template #default="{ row }">
            <el-button 
              v-if="row.userId"
              type="primary" 
              link 
              @click="viewUser(row.userId)"
            >
              {{ row.user }}
            </el-button>
            <span v-else>{{ row.user || '-' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="ipAddress" label="IP地址" width="120" />
        
        <el-table-column prop="module" label="模块" width="120" />
        
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button 
              size="small" 
              @click="viewLogDetail(row)"
            >
              详情
            </el-button>
            <el-button 
              v-if="row.level === 'ERROR'"
              type="warning" 
              size="small" 
              @click="markAsRead(row)"
            >
              标记已读
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[20, 50, 100, 200]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 日志详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="日志详情"
      width="800px"
      @close="resetDetailForm"
    >
      <div v-if="currentLog" class="log-detail">
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <label>时间:</label>
              <span>{{ formatDateTime(currentLog.timestamp) }}</span>
            </div>
            <div class="detail-item">
              <label>级别:</label>
              <el-tag :type="getLevelType(currentLog.level)" size="small">
                {{ currentLog.level }}
              </el-tag>
            </div>
            <div class="detail-item">
              <label>类别:</label>
              <span>{{ getCategoryText(currentLog.category) }}</span>
            </div>
            <div class="detail-item">
              <label>模块:</label>
              <span>{{ currentLog.module }}</span>
            </div>
            <div class="detail-item">
              <label>操作人:</label>
              <span>{{ currentLog.user || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>IP地址:</label>
              <span>{{ currentLog.ipAddress }}</span>
            </div>
          </div>
        </div>
        
        <div class="detail-section">
          <h4>日志内容</h4>
          <div class="log-content">
            <pre class="message-pre">{{ currentLog.message }}</pre>
          </div>
        </div>
        
        <div v-if="currentLog.stackTrace" class="detail-section">
          <h4>堆栈跟踪</h4>
          <div class="stack-content">
            <pre class="stack-pre">{{ currentLog.stackTrace }}</pre>
          </div>
        </div>
        
        <div v-if="currentLog.context" class="detail-section">
          <h4>上下文信息</h4>
          <div class="context-content">
            <pre class="context-pre">{{ JSON.stringify(currentLog.context, null, 2) }}</pre>
          </div>
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button 
            v-if="currentLog?.level === 'ERROR'"
            type="warning" 
            @click="markAsRead(currentLog)"
          >
            标记已读
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Download, Delete, Refresh, Document, Warning, InfoFilled, CircleCheck,
  Search
} from '@element-plus/icons-vue'
import type { Log } from '@/api/types'
import dayjs from 'dayjs'

const router = useRouter()

// 日志统计数据
const logStats = ref({
  totalLogs: 0,
  todayLogs: 0,
  errorLogs: 0,
  unreadErrors: 0,
  warningLogs: 0,
  todayWarnings: 0,
  infoLogs: 0
})

// 日志列表
const logs = ref<Log[]>([])

// 搜索表单
const searchForm = ref({
  keyword: '',
  level: '',
  category: '',
  dateRange: []
})

// 分页信息
const pagination = ref({
  page: 1,
  size: 50,
  total: 0
})

// 选中项
const selectedLogs = ref<Log[]>([])

// 加载状态
const loading = ref(false)

// 自动刷新
const autoRefresh = ref(false)
let refreshTimer: NodeJS.Timeout | null = null

// 详情对话框
const detailDialogVisible = ref(false)
const currentLog = ref<Log | null>(null)

// 过滤后的日志列表
const filteredLogs = computed(() => {
  let result = logs.value
  
  // 关键词搜索
  if (searchForm.value.keyword) {
    const keyword = searchForm.value.keyword.toLowerCase()
    result = result.filter(log => 
      log.message.toLowerCase().includes(keyword) ||
      (log.user && log.user.toLowerCase().includes(keyword)) ||
      (log.ipAddress && log.ipAddress.includes(keyword))
    )
  }
  
  // 级别筛选
  if (searchForm.value.level) {
    result = result.filter(log => log.level === searchForm.value.level)
  }
  
  // 类别筛选
  if (searchForm.value.category) {
    result = result.filter(log => log.category === searchForm.value.category)
  }
  
  // 日期范围筛选
  if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
    const [startDate, endDate] = searchForm.value.dateRange
    result = result.filter(log => {
      const logDate = dayjs(log.timestamp)
      return logDate.isAfter(startDate) && logDate.isBefore(endDate)
    })
  }
  
  return result
})

// 获取日志级别类型
const getLevelType = (level: string) => {
  const typeMap: Record<string, string> = {
    ERROR: 'danger',
    WARN: 'warning',
    INFO: 'info',
    DEBUG: 'success'
  }
  return typeMap[level] || 'info'
}

// 获取类别文本
const getCategoryText = (category: string) => {
  const textMap: Record<string, string> = {
    OPERATION: '操作日志',
    SYSTEM: '系统日志',
    ERROR: '错误日志',
    AUDIT: '审计日志'
  }
  return textMap[category] || category
}

// 获取消息样式类
const getMessageClass = (level: string) => {
  const classMap: Record<string, string> = {
    ERROR: 'message-error',
    WARN: 'message-warning',
    INFO: 'message-info',
    DEBUG: 'message-debug'
  }
  return classMap[level] || 'message-info'
}

// 获取行样式类
const getRowClassName = ({ row }: { row: Log }) => {
  if (row.level === 'ERROR' && !row.read) {
    return 'error-row'
  }
  return ''
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

// 查看用户
const viewUser = (userId: string) => {
  router.push(`/users/${userId}`)
}

// 查看日志详情
const viewLogDetail = (log: Log) => {
  currentLog.value = log
  detailDialogVisible.value = true
}

// 重置详情表单
const resetDetailForm = () => {
  currentLog.value = null
}

// 显示堆栈跟踪
const showStackTrace = (log: Log) => {
  if (log.stackTrace) {
    viewLogDetail(log)
  }
}

// 标记已读
const markAsRead = async (log: Log) => {
  try {
    // TODO: 调用API标记为已读
    log.read = true
    ElMessage.success('已标记为已读')
    await loadLogStats()
  } catch (error) {
    ElMessage.error('标记失败')
  }
}

// 导出日志
const exportLogs = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要导出当前筛选的日志吗？',
      '导出日志',
      {
        confirmButtonText: '确认导出',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    ElMessage.info('正在导出日志数据...')
    
    // 获取所有日志数据
    const { getLogsApi } = await import('@/api/logs')
    const response = await getLogsApi({
      page: 0,
      size: 10000, // 获取所有数据
      level: searchForm.level || undefined,
      category: searchForm.category || undefined
    })
    
    if (response.data.code === 200) {
      const logs = response.data.data.content || []
      
      // 转换为CSV格式
      const headers = ['日志ID', '级别', '类别', '消息', '用户名', 'IP地址', '模块', '请求URL', '请求方法', '执行时间(ms)', '创建时间']
      const rows = logs.map((log: any) => [
        log.id,
        log.level,
        log.category,
        log.message,
        log.username || '-',
        log.ipAddress || '-',
        log.module || '-',
        log.requestUrl || '-',
        log.requestMethod || '-',
        log.executionTime || '-',
        log.createTime
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
      link.setAttribute('download', `系统日志_${new Date().toISOString().split('T')[0]}.csv`)
      link.style.visibility = 'hidden'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      
      ElMessage.success('日志数据导出成功')
    }
  } catch (error) {
    console.error('Export logs failed:', error)
    ElMessage.error('导出失败，请稍后重试')
  }
}

// 清空日志
const clearLogs = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空所有日志吗？此操作不可恢复！',
      '清空日志',
      {
        confirmButtonText: '确认清空',
        cancelButtonText: '取消',
        type: 'error'
      }
    )
    
    const { clearLogsApi } = await import('@/api/logs')
    await clearLogsApi()
    logs.value = []
    pagination.value.total = 0
    ElMessage.success('日志已清空')
    await loadLogStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '清空失败')
    }
  }
}

// 切换自动刷新
const toggleAutoRefresh = () => {
  autoRefresh.value = !autoRefresh.value
  if (autoRefresh.value) {
    refreshTimer = setInterval(() => {
      loadLogs()
    }, 10000) // 10秒刷新一次
    ElMessage.success('已开启自动刷新')
  } else {
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
    ElMessage.info('已关闭自动刷新')
  }
}

// 搜索处理
const handleSearch = () => {
  pagination.value.page = 1
}

// 选择项变化
const handleSelectionChange = (selection: Log[]) => {
  selectedLogs.value = selection
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
      loadLogStats(),
      loadLogs()
    ])
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 加载日志统计数据
const loadLogStats = async () => {
  try {
    const { getLogStatisticsApi } = await import('@/api/logs')
    const response = await getLogStatisticsApi()
    if (response.data.code === 200) {
      const stats = response.data.data
      logStats.value = {
        totalLogs: stats.totalLogs || 0,
        todayLogs: stats.todayLogs || 0,
        errorLogs: stats.errorLogs || 0,
        unreadErrors: stats.unreadErrors || 0,
        warningLogs: stats.warningLogs || 0,
        todayWarnings: 0,
        infoLogs: stats.infoLogs || 0
      }
    }
  } catch (error) {
    console.error('Load log stats failed:', error)
  }
}

// 加载日志统计数据（旧代码保留作为备用）
const loadLogStatsOld = async () => {
  try {
    // 模拟数据
    logStats.value = {
      totalLogs: 15420,
      todayLogs: 156,
      errorLogs: 89,
      unreadErrors: 12,
      warningLogs: 234,
      todayWarnings: 23,
      infoLogs: 15097
    }
  } catch (error) {
    console.error('Load log stats failed:', error)
  }
}

// 加载日志列表
const loadLogs = async () => {
  try {
    const { getLogsApi } = await import('@/api/logs')
    const response = await getLogsApi({
      page: pagination.value.page - 1,
      size: pagination.value.size,
      level: searchForm.value.level || undefined,
      category: searchForm.value.category || undefined,
      keyword: searchForm.value.keyword || undefined
    })
    
    if (response.data.code === 200) {
      const pageData = response.data.data
      logs.value = (pageData.content || []).map((log: any) => ({
        id: log.id.toString(),
        timestamp: log.createTime,
        level: log.level,
        category: log.category,
        message: log.message,
        stackTrace: log.exception,
        user: log.username,
        userId: log.userId?.toString(),
        ipAddress: log.ipAddress || '',
        module: log.module || '',
        read: false,
        context: {}
      }))
      pagination.value.total = pageData.totalElements || 0
    }
  } catch (error) {
    console.error('Load logs failed:', error)
    logs.value = []
  }
}

// 加载日志列表（旧代码保留作为备用）
const loadLogsOld = async () => {
  try {
    // 模拟数据
    logs.value = [
      {
        id: '1',
        timestamp: new Date().toISOString(),
        level: 'ERROR',
        category: 'ERROR',
        message: '数据库连接失败：Connection refused',
        stackTrace: 'java.sql.SQLException: Connection refused\nat com.mysql.jdbc...\nat org.springframework.jdbc.core...',
        user: 'system',
        userId: '',
        ipAddress: '192.168.1.100',
        module: 'Database',
        read: false,
        context: {
          url: '/api/vehicles',
          method: 'GET',
          userAgent: 'Mozilla/5.0...'
        }
      },
      {
        id: '2',
        timestamp: new Date(Date.now() - 600000).toISOString(),
        level: 'WARN',
        category: 'SYSTEM',
        message: '车辆京A12345长时间离线，建议检查设备状态',
        user: '',
        userId: '',
        ipAddress: '192.168.1.50',
        module: 'Monitoring',
        read: true,
        context: {
          vehicleId: '1',
          offlineDuration: '45分钟'
        }
      },
      {
        id: '3',
        timestamp: new Date(Date.now() - 1200000).toISOString(),
        level: 'INFO',
        category: 'OPERATION',
        message: '用户 张三 创建了新任务：T3-02接机任务',
        user: '张三',
        userId: '2',
        ipAddress: '192.168.1.20',
        module: 'TaskManagement',
        read: true,
        context: {
          taskId: 'task_001',
          taskType: 'pickup'
        }
      }
    ]
    pagination.value.total = logs.value.length
  } catch (error) {
    console.error('Load logs failed:', error)
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadData()
})

// 组件卸载时清理定时器
onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped lang="scss">
.logs-page {
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

.logs-stats {
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
    
    &.errors {
      background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
    }
    
    &.warnings {
      background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
    }
    
    &.info {
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

.logs-toolbar {
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

.logs-table {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.log-time {
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

.log-message {
  .message-text {
    font-size: 14px;
    line-height: 1.4;
    margin-bottom: 4px;
    
    &.message-error {
      color: #f56c6c;
      font-weight: 500;
    }
    
    &.message-warning {
      color: #e6a23c;
    }
    
    &.message-info {
      color: var(--text-primary-color);
    }
    
    &.message-debug {
      color: var(--text-secondary-color);
    }
  }
  
  .stack-trace {
    .el-button {
      padding: 0;
      height: auto;
    }
  }
}

.pagination-wrapper {
  padding: 20px;
  display: flex;
  justify-content: center;
  border-top: 1px solid var(--border-lighter-color);
}

.log-detail {
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
  
  .log-content,
  .stack-content,
  .context-content {
    background: var(--background-extra-light-color);
    border-radius: 6px;
    padding: 16px;
    max-height: 300px;
    overflow-y: auto;
    
    .message-pre,
    .stack-pre,
    .context-pre {
      margin: 0;
      font-size: 12px;
      line-height: 1.5;
      color: var(--text-primary-color);
      white-space: pre-wrap;
      word-break: break-all;
    }
  }
}

:deep(.error-row) {
  background-color: #fef0f0;
  
  &:hover {
    background-color: #fde2e2;
  }
}

@media (max-width: 1200px) {
  .logs-stats {
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
  
  .logs-toolbar {
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
  
  .logs-stats {
    grid-template-columns: 1fr;
  }
}
</style>