// 告警规则相关API
import request from '@/utils/request'

export interface AlertRule {
  id?: number
  ruleName: string
  ruleType: string
  conditionType: string
  conditionValue: string
  severity: string
  enabled: boolean
  description?: string
  createTime?: string
  updateTime?: string
}

// 获取告警规则列表
export const getAlertRulesApi = (enabled?: boolean) => {
  return request.get<AlertRule[]>('/alert-rules', { params: { enabled } })
}

// 获取告警规则详情
export const getAlertRuleApi = (id: number) => {
  return request.get<AlertRule>(`/alert-rules/${id}`)
}

// 创建告警规则
export const createAlertRuleApi = (data: AlertRule) => {
  return request.post<AlertRule>('/alert-rules', data)
}

// 更新告警规则
export const updateAlertRuleApi = (id: number, data: AlertRule) => {
  return request.put<AlertRule>(`/alert-rules/${id}`, data)
}

// 删除告警规则
export const deleteAlertRuleApi = (id: number) => {
  return request.delete(`/alert-rules/${id}`)
}

// 启用/禁用告警规则
export const toggleAlertRuleApi = (id: number) => {
  return request.put<AlertRule>(`/alert-rules/${id}/toggle`)
}

