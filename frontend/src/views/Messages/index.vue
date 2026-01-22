<template>
  <div class="messages-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">站内信</h1>
      <p class="page-description">查看和管理系统站内信通知</p>
      <div class="header-actions">
        <el-button 
          type="danger" 
          @click="batchDeleteMessages" 
          :disabled="selectedMessages.length === 0"
        >
          <el-icon><Delete /></el-icon>
          一键删除 ({{ selectedMessages.length }})
        </el-button>
        <el-button type="primary" @click="markAllAsRead" :disabled="unreadCount === 0">
          <el-icon><Check /></el-icon>
          全部标记为已读
        </el-button>
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="messages-stats">
      <div class="stat-card unread">
        <div class="stat-icon">
          <el-icon><Message /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ unreadCount }}</div>
          <div class="stat-label">未读消息</div>
        </div>
      </div>
      <div class="stat-card total">
        <div class="stat-icon">
          <el-icon><Bell /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ pagination.total }}</div>
          <div class="stat-label">消息总数</div>
        </div>
      </div>
    </div>

    <!-- 筛选和搜索 -->
    <div class="messages-toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索消息标题、内容"
          style="width: 300px"
          clearable
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-select
          v-model="searchForm.read"
          placeholder="阅读状态"
          style="width: 120px; margin-left: 12px"
          clearable
          @change="handleSearch"
        >
          <el-option label="未读" :value="false" />
          <el-option label="已读" :value="true" />
        </el-select>
        
        <el-select
          v-model="searchForm.messageType"
          placeholder="消息类型"
          style="width: 150px; margin-left: 12px"
          clearable
          @change="handleSearch"
        >
          <el-option label="任务分配" value="task_assignment" />
          <el-option label="任务完成" value="task_completion" />
          <el-option label="车辆报告" value="vehicle_report" />
          <el-option label="告警通知" value="alert" />
          <el-option label="维修通知" value="maintenance" />
          <el-option label="系统通知" value="system" />
        </el-select>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="messages-table">
      <el-table
        v-loading="loading"
        :data="messageList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="createTime" label="时间" width="150">
          <template #default="{ row }">
            <div class="message-time">
              <div class="date">{{ formatDate(row.createTime) }}</div>
              <div class="time">{{ formatTime(row.createTime) }}</div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="title" label="消息标题" min-width="200">
          <template #default="{ row }">
            <div class="message-title">
              <el-tag :type="getPriorityType(row.priority)" size="small" style="margin-right: 8px;">
                {{ getPriorityText(row.priority) }}
              </el-tag>
              <span :class="{ 'unread-title': !row.read }">{{ row.title }}</span>
              <el-tag v-if="!row.read" type="danger" size="small" style="margin-left: 8px;">未读</el-tag>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="messageType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getMessageTypeType(row.messageType)" size="small">
              {{ getMessageTypeText(row.messageType) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ 'unread-content': !row.read }">{{ row.content }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="!row.read"
              type="primary" 
              size="small" 
              @click="markAsRead([row.id])"
            >
              标记已读
            </el-button>
            <el-button 
              size="small" 
              @click="viewMessageDetail(row)"
            >
              详情
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              @click="deleteMessage(row.id)"
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

    <!-- 消息详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="消息详情"
      width="600px"
      @close="currentMessage = null"
    >
      <div v-if="currentMessage" class="message-detail">
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <label>消息标题:</label>
              <span>{{ currentMessage.title }}</span>
            </div>
            <div class="detail-item">
              <label>消息类型:</label>
              <el-tag :type="getMessageTypeType(currentMessage.messageType)" size="small">
                {{ getMessageTypeText(currentMessage.messageType) }}
              </el-tag>
            </div>
            <div class="detail-item">
              <label>优先级:</label>
              <el-tag :type="getPriorityType(currentMessage.priority)" size="small">
                {{ getPriorityText(currentMessage.priority) }}
              </el-tag>
            </div>
            <div class="detail-item">
              <label>接收时间:</label>
              <span>{{ formatDateTime(currentMessage.createTime) }}</span>
            </div>
            <div class="detail-item">
              <label>阅读状态:</label>
              <el-tag :type="currentMessage.read ? 'success' : 'danger'" size="small">
                {{ currentMessage.read ? '已读' : '未读' }}
              </el-tag>
            </div>
          </div>
        </div>
        
        <div class="detail-section">
          <h4>消息内容</h4>
          <div class="message-content">
            <p style="white-space: pre-wrap; word-wrap: break-word;">{{ currentMessage.content }}</p>
          </div>
        </div>
        
        <div v-if="currentMessage.relatedType" class="detail-section">
          <h4>关联信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <label>关联类型:</label>
              <span>{{ currentMessage.relatedType }}</span>
            </div>
            <div v-if="currentMessage.relatedId" class="detail-item">
              <label>关联ID:</label>
              <span>{{ currentMessage.relatedId }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <span>
          <el-button v-if="currentMessage && !currentMessage.read" type="primary" @click="markAsRead([currentMessage.id])">
            标记为已读
          </el-button>
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Check, Refresh, Message, Bell, Search, Delete
} from '@element-plus/icons-vue'
import type { Message as MessageType } from '@/api/types'
import dayjs from 'dayjs'
import {
  getMessagesApi,
  getUnreadCountApi,
  markMessagesAsReadApi,
  markAllMessagesAsReadApi,
  deleteMessageApi,
  deleteMessagesApi
} from '@/api/messages'

// 未读消息数量
const unreadCount = ref(0)

// 消息列表
const messages = ref<MessageType[]>([])

// 搜索表单
const searchForm = ref({
  keyword: '',
  read: undefined as boolean | undefined,
  messageType: ''
})

// 分页信息
const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

// 选中项
const selectedMessages = ref<MessageType[]>([])

// 加载状态
const loading = ref(false)

// 详情对话框
const detailDialogVisible = ref(false)
const currentMessage = ref<MessageType | null>(null)

// 过滤后的消息列表
const messageList = computed(() => {
  let result = messages.value
  
  // 关键词搜索
  if (searchForm.value.keyword) {
    const keyword = searchForm.value.keyword.toLowerCase()
    result = result.filter(msg => 
      msg.title.toLowerCase().includes(keyword) ||
      msg.content.toLowerCase().includes(keyword)
    )
  }
  
  return result
})

// 获取优先级类型
const getPriorityType = (priority: string) => {
  const typeMap: Record<string, string> = {
    high: 'danger',
    medium: 'warning',
    low: 'info',
    normal: 'info'
  }
  return typeMap[priority] || 'info'
}

// 获取优先级文本
const getPriorityText = (priority: string) => {
  const textMap: Record<string, string> = {
    high: '高',
    medium: '中',
    low: '低',
    normal: '普通'
  }
  return textMap[priority] || '普通'
}

// 获取消息类型类型
const getMessageTypeType = (messageType: string) => {
  const typeMap: Record<string, string> = {
    task_assignment: 'primary',
    task_completion: 'success',
    vehicle_report: 'warning',
    alert: 'warning',
    maintenance: 'info',
    system: 'info'
  }
  return typeMap[messageType] || 'info'
}

// 获取消息类型文本
const getMessageTypeText = (messageType: string) => {
  const textMap: Record<string, string> = {
    task_assignment: '任务分配',
    task_completion: '任务完成',
    vehicle_report: '车辆报告',
    alert: '告警通知',
    maintenance: '维修通知',
    system: '系统通知'
  }
  return textMap[messageType] || messageType
}

// 格式化日期
const formatDate = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD')
}

// 格式化时间
const formatTime = (time: string) => {
  return dayjs(time).format('HH:mm:ss')
}

// 格式化日期时间
const formatDateTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

// 加载数据
const loadData = async () => {
  try {
    loading.value = true
    
    // 加载消息列表
    const response = await getMessagesApi({
      page: pagination.value.page - 1,
      size: pagination.value.size,
      read: searchForm.value.read,
      messageType: searchForm.value.messageType || undefined
    })
    
    if (response.data.code === 200) {
      messages.value = response.data.data.content || []
      pagination.value.total = response.data.data.totalElements || 0
    } else {
      ElMessage.error(response.data.message || '加载消息列表失败')
    }
    
    // 加载未读数量
    await loadUnreadCount()
  } catch (error: any) {
    console.error('Load messages failed:', error)
    ElMessage.error(error?.response?.data?.message || '加载消息列表失败')
  } finally {
    loading.value = false
  }
}

// 加载未读数量
const loadUnreadCount = async () => {
  try {
    const response = await getUnreadCountApi()
    if (response.data.code === 200) {
      unreadCount.value = response.data.data || 0
    }
  } catch (error: any) {
    console.error('Load unread count failed:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.value.page = 1
  loadData()
}

// 分页大小改变
const handleSizeChange = (size: number) => {
  pagination.value.size = size
  pagination.value.page = 1
  loadData()
}

// 当前页改变
const handleCurrentChange = (page: number) => {
  pagination.value.page = page
  loadData()
}

// 选中项改变
const handleSelectionChange = (selection: MessageType[]) => {
  selectedMessages.value = selection
}

// 查看消息详情
const viewMessageDetail = (message: MessageType) => {
  currentMessage.value = message
  detailDialogVisible.value = true
  
  // 如果未读，自动标记为已读
  if (!message.read) {
    markAsRead([message.id])
  }
}

// 标记为已读
const markAsRead = async (messageIds: number[]) => {
  try {
    const response = await markMessagesAsReadApi(messageIds)
    if (response.data.code === 200) {
      ElMessage.success('标记成功')
      await loadData()
    } else {
      ElMessage.error(response.data.message || '标记失败')
    }
  } catch (error: any) {
    console.error('Mark as read failed:', error)
    ElMessage.error(error?.response?.data?.message || '标记失败')
  }
}

// 标记全部为已读
const markAllAsRead = async () => {
  try {
    await ElMessageBox.confirm('确定要将所有消息标记为已读吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    
    const response = await markAllMessagesAsReadApi()
    if (response.data.code === 200) {
      ElMessage.success('全部标记成功')
      await loadData()
    } else {
      ElMessage.error(response.data.message || '标记失败')
    }
  } catch (error: any) {
    if (error === 'cancel') {
      return
    }
    console.error('Mark all as read failed:', error)
    ElMessage.error(error?.response?.data?.message || '标记失败')
  }
}

// 删除消息
const deleteMessage = async (messageId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条消息吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const response = await deleteMessageApi(messageId)
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
    console.error('Delete message failed:', error)
    ElMessage.error(error?.response?.data?.message || '删除失败')
  }
}

// 批量删除消息
const batchDeleteMessages = async () => {
  if (selectedMessages.value.length === 0) {
    ElMessage.warning('请先选择要删除的消息')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedMessages.value.length} 条消息吗？此操作不可恢复！`,
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const messageIds = selectedMessages.value.map(msg => msg.id)
    const response = await deleteMessagesApi(messageIds)
    if (response.data.code === 200) {
      ElMessage.success(`成功删除 ${messageIds.length} 条消息`)
      selectedMessages.value = []
      await loadData()
    } else {
      ElMessage.error(response.data.message || '批量删除失败')
    }
  } catch (error: any) {
    if (error === 'cancel') {
      return
    }
    console.error('Batch delete messages failed:', error)
    ElMessage.error(error?.response?.data?.message || '批量删除失败')
  }
}

// 刷新数据
const refreshData = () => {
  loadData()
}

// 组件挂载时加载数据
onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.messages-page {
  padding: 20px;
  
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
  
  .messages-stats {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
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
    
    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      color: white;
      
      &.unread {
        background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
      }
      
      &.total {
        background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
      }
    }
    
    .stat-content {
      .stat-value {
        font-size: 32px;
        font-weight: 700;
        color: var(--text-primary-color);
        margin-bottom: 4px;
      }
      
      .stat-label {
        color: var(--text-regular-color);
        font-size: 14px;
      }
    }
  }
  
  .messages-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    .toolbar-left {
      display: flex;
      align-items: center;
    }
  }
  
  .messages-table {
    background: white;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    
    .message-time {
      .date {
        font-size: 14px;
        color: var(--text-primary-color);
      }
      .time {
        font-size: 12px;
        color: var(--text-regular-color);
      }
    }
    
    .message-title {
      display: flex;
      align-items: center;
      
      .unread-title {
        font-weight: 600;
        color: var(--text-primary-color);
      }
    }
    
    .unread-content {
      font-weight: 500;
    }
    
    .pagination-wrapper {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }
  }
  
  .message-detail {
    .detail-section {
      margin-bottom: 24px;
      
      h4 {
        font-size: 16px;
        font-weight: 600;
        color: var(--text-primary-color);
        margin-bottom: 16px;
        padding-bottom: 8px;
        border-bottom: 2px solid #e4e7ed;
      }
      
      .detail-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 16px;
        
        .detail-item {
          display: flex;
          align-items: center;
          
          label {
            font-weight: 600;
            color: var(--text-regular-color);
            margin-right: 8px;
            min-width: 80px;
          }
          
          span {
            color: var(--text-primary-color);
          }
        }
      }
      
      .message-content {
        padding: 16px;
        background: #f5f7fa;
        border-radius: 8px;
        line-height: 1.8;
        color: var(--text-primary-color);
        
        p {
          margin: 0;
          white-space: pre-wrap;
        }
      }
    }
  }
}
</style>
