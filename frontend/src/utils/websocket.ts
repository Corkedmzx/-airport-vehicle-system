// frontend/src/utils/websocket.ts
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

export class WebSocketClient {
  private ws: WebSocket | null = null
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
        console.warn('未登录，跳过WebSocket连接')
        return
      }

      // 使用SockJS降级方案
      const wsUrl = this.getWebSocketUrl(token)
      this.ws = new WebSocket(wsUrl)
      
      this.setupEventListeners()
      
    } catch (error) {
      console.error('WebSocket连接失败:', error)
      this.scheduleReconnect()
    }
  }

  /**
   * 获取WebSocket URL
   */
  private getWebSocketUrl(token: string): string {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const wsPath = '/api/ws/vehicles'
    
    // 使用token参数
    return `${protocol}//${host}${wsPath}?token=${encodeURIComponent(token)}`
  }

  /**
   * 设置事件监听器
   */
  private setupEventListeners(): void {
    if (!this.ws) return

    this.ws.onopen = () => {
      console.log('WebSocket连接已建立')
      this.reconnectAttempts = 0
      this.startHeartbeat()
      this.notifyListeners('connect', {})
    }

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        this.handleMessage(data)
      } catch (error) {
        console.error('解析WebSocket消息失败:', error)
      }
    }

    this.ws.onclose = (event) => {
      console.log('WebSocket连接已关闭:', event.reason)
      this.stopHeartbeat()
      this.notifyListeners('disconnect', { reason: event.reason })
      
      if (!event.wasClean) {
        this.scheduleReconnect()
      }
    }

    this.ws.onerror = (error) => {
      console.error('WebSocket错误:', error)
      this.notifyListeners('error', { error })
    }
  }

  /**
   * 处理接收到的消息
   */
  private handleMessage(data: any): void {
    const { type, data: messageData } = data
    
    // 触发对应类型的监听器
    this.notifyListeners(type, messageData)
    
    // 特殊处理某些消息类型
    switch (type) {
      case 'VEHICLE_LOCATION_UPDATE':
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
    // 更新车辆位置
    this.notifyListeners('vehicle_location', data)
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
    if (this.listeners.has(event)) {
      this.listeners.get(event)!.forEach(callback => {
        try {
          callback(data)
        } catch (error) {
          console.error(`WebSocket监听器执行错误 (${event}):`, error)
        }
      })
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