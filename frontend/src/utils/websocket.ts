// frontend/src/utils/websocket.ts
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

export class WebSocketClient {
  private ws: WebSocket | null = null
  private pendingAuthToken: string | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectTimeout: number = 3000
  private heartbeatInterval: NodeJS.Timeout | null = null
  private listeners: Map<string, Array<(data: any) => void>> = new Map()

  constructor() {
    // 自动连接
    this.connect()
  }

  /**
   * 连接WebSocket
   */
  public connect(): void {
    try {
      const userStore = useUserStore()
      const token = userStore.token
      
      if (!token) {
        console.warn('[WebSocket] ⚠️ 未登录，跳过WebSocket连接')
        return
      }

      console.log('[WebSocket] ========== 开始连接WebSocket ==========')

      this.pendingAuthToken = token

      if (this.ws) {
        console.log('[WebSocket] 关闭现有连接')
        this.ws.close()
        this.ws = null
      }

      const wsUrl = this.getWebSocketUrl()
      console.log('[WebSocket] 创建 WebSocket（鉴权经 AUTH 帧，不含 URL token）:', wsUrl)
      this.ws = new WebSocket(wsUrl)
      
      this.setupEventListeners()
      
      console.log('[WebSocket] WebSocket对象已创建，等待连接...')
      
    } catch (error) {
      console.error('[WebSocket] ❌ WebSocket连接失败:', error)
      this.scheduleReconnect()
    }
  }

  /**
   * 获取WebSocket URL
   */
  private getWebSocketUrl(): string {
    const isDev = import.meta.env.DEV
    let protocol = 'ws:'
    let host = 'localhost:8080'

    if (!isDev) {
      protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      host = window.location.host
    }

    return `${protocol}//${host}/api/ws/vehicles`
  }

  private sendAuthFrame(): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN || !this.pendingAuthToken) {
      return
    }
    this.ws.send(JSON.stringify({
      type: 'AUTH',
      data: { token: this.pendingAuthToken }
    }))
  }

  /**
   * 设置事件监听器
   */
  private setupEventListeners(): void {
    if (!this.ws) return

    this.ws.onopen = () => {
      console.log('[WebSocket] 握手完成，发送 AUTH 帧')
      this.sendAuthFrame()
    }

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        console.log('[WebSocket] ========== 收到WebSocket消息 ==========')
        console.log('[WebSocket] 原始消息字符串:', event.data)
        console.log('[WebSocket] 解析后的消息对象:', JSON.stringify(data, null, 2))
        console.log('[WebSocket] 消息类型:', data.type)
        console.log('[WebSocket] 消息数据:', JSON.stringify(data.data, null, 2))
        this.handleMessage(data)
      } catch (error) {
        console.error('[WebSocket] ❌ 解析WebSocket消息失败:', error, '原始数据:', event.data)
      }
    }

    this.ws.onclose = (event) => {
      console.log('[WebSocket] ⚠️ WebSocket连接已关闭')
      console.log('[WebSocket] 关闭原因:', event.reason)
      console.log('[WebSocket] 关闭代码:', event.code)
      console.log('[WebSocket] 是否正常关闭:', event.wasClean)
      this.stopHeartbeat()
      this.notifyListeners('disconnect', { reason: event.reason })
      
      if (!event.wasClean) {
        console.log('[WebSocket] 连接异常关闭，将在3秒后重连...')
        this.scheduleReconnect()
      }
    }

    this.ws.onerror = (error) => {
      console.error('[WebSocket] ❌ WebSocket错误:', error)
      console.error('[WebSocket] 连接URL:', this.ws?.url)
      console.error('[WebSocket] 连接状态:', this.ws?.readyState)
      this.notifyListeners('error', { error })
    }
  }

  /**
   * 处理接收到的消息
   */
  private handleMessage(data: any): void {
    const { type, data: messageData } = data
    
    console.log('[WebSocket] 处理消息, type:', type, ', messageData:', messageData)
    
    // 触发对应类型的监听器
    this.notifyListeners(type, messageData)
    
    // 特殊处理某些消息类型
    switch (type) {
      case 'AUTH_REQUIRED':
        this.sendAuthFrame()
        break
      case 'CONNECTED':
        console.log('[WebSocket] ========== ✅ 已认证并连接 ==========')
        this.reconnectAttempts = 0
        this.startHeartbeat()
        this.notifyListeners('connect', {})
        break
      case 'VEHICLE_LOCATION_UPDATE':
        console.log('[WebSocket] VEHICLE_LOCATION_UPDATE消息，调用handleVehicleLocationUpdate')
        this.handleVehicleLocationUpdate(messageData)
        break
      case 'ALERT_NOTIFICATION':
        this.handleAlertNotification(messageData)
        break
      case 'TASK_STATUS_UPDATE':
        this.handleTaskStatusUpdate(messageData)
        break
      case 'SYSTEM_MESSAGE':
        this.handleSystemMessage(messageData)
        break
    }
  }

  /**
   * 处理车辆位置更新
   */
  private handleVehicleLocationUpdate(data: any): void {
    console.log('[WebSocket] ========== handleVehicleLocationUpdate被调用 ==========')
    console.log('[WebSocket] 位置数据:', JSON.stringify(data, null, 2))
    console.log('[WebSocket] 数据字段检查:', {
      source: data.source,
      deviceName: data.deviceName,
      userId: data.userId,
      userName: data.userName,
      longitude: data.longitude,
      latitude: data.latitude,
      hasSource: !!data.source,
      hasDeviceName: !!data.deviceName,
      hasUserId: !!data.userId
    })
    // 更新车辆位置
    this.notifyListeners('vehicle_location', data)
    const listenerCount = this.listeners.get('vehicle_location')?.length || 0
    console.log('[WebSocket] ✅ 已通知vehicle_location监听器，监听器数量:', listenerCount)
    if (listenerCount === 0) {
      console.warn('[WebSocket] ⚠️ 警告：没有注册vehicle_location监听器！')
    }
  }

  /**
   * 处理告警通知
   */
  private handleAlertNotification(data: any): void {
    // 显示告警通知
    ElMessage.warning(`新告警: ${data.title || '未知告警'}`)
    this.notifyListeners('alert', data)
  }

  /**
   * 处理任务状态更新
   */
  private handleTaskStatusUpdate(data: any): void {
    this.notifyListeners('task_update', data)
  }

  /**
   * 处理系统消息
   */
  private handleSystemMessage(data: any): void {
    const { message, level } = data
    if (level === 'WARN' || level === 'ERROR') {
      ElMessage.warning(message)
    }
    this.notifyListeners('system_message', data)
  }

  /**
   * 开始心跳检测
   */
  private startHeartbeat(): void {
    this.heartbeatInterval = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.send({
          type: 'PING',
          data: { timestamp: Date.now() }
        })
      }
    }, 30000) // 每30秒发送一次心跳
  }

  /**
   * 停止心跳检测
   */
  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  /**
   * 发送消息
   */
  public send(data: any): boolean {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket未连接，无法发送消息')
      return false
    }

    try {
      const message = typeof data === 'string' ? data : JSON.stringify(data)
      this.ws.send(message)
      return true
    } catch (error) {
      console.error('发送WebSocket消息失败:', error)
      return false
    }
  }

  /**
   * 订阅车辆
   */
  public subscribeVehicle(vehicleId: number): boolean {
    return this.send({
      type: 'SUBSCRIBE_VEHICLE',
      data: { vehicleId }
    })
  }

  /**
   * 取消订阅车辆
   */
  public unsubscribeVehicle(vehicleId: number): boolean {
    return this.send({
      type: 'UNSUBSCRIBE_VEHICLE',
      data: { vehicleId }
    })
  }

  /**
   * 断开连接
   */
  public disconnect(): void {
    this.stopHeartbeat()
    
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  /**
   * 调度重连
   */
  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('达到最大重连次数，停止重连')
      return
    }

    this.reconnectAttempts++
    const delay = this.reconnectTimeout * this.reconnectAttempts
    
    console.log(`将在 ${delay}ms 后尝试重连，第 ${this.reconnectAttempts} 次`)
    
    setTimeout(() => {
      if (!this.ws || this.ws.readyState === WebSocket.CLOSED) {
        this.connect()
      }
    }, delay)
  }

  /**
   * 添加事件监听器
   */
  public on(event: string, callback: (data: any) => void): void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event)!.push(callback)
  }

  /**
   * 移除事件监听器
   */
  public off(event: string, callback?: (data: any) => void): void {
    if (!this.listeners.has(event)) return
    
    if (callback) {
      const callbacks = this.listeners.get(event)!
      const index = callbacks.indexOf(callback)
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    } else {
      this.listeners.delete(event)
    }
  }

  /**
   * 通知监听器
   */
  private notifyListeners(event: string, data: any): void {
    const listeners = this.listeners.get(event)
    console.log(`[WebSocket] notifyListeners: event=${event}, listeners数量=${listeners?.length || 0}`)
    if (listeners && listeners.length > 0) {
      listeners.forEach((callback, index) => {
        try {
          console.log(`[WebSocket] 执行监听器 [${event}][${index}]`)
          callback(data)
          console.log(`[WebSocket] 监听器 [${event}][${index}] 执行成功`)
        } catch (error) {
          console.error(`[WebSocket] ❌ 执行监听器失败 [${event}][${index}]:`, error)
        }
      })
    } else {
      console.warn(`[WebSocket] ⚠️ 没有找到事件 [${event}] 的监听器`)
    }
  }

  /**
   * 获取连接状态
   */
  public get isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }
}

// 创建全局WebSocket实例
export const webSocketClient = new WebSocketClient()