// 邮件相关API
import request from '@/utils/request'
import type { Alert } from './types'

// 发送告警邮件通知
export const sendAlertEmailApi = (alertId: string | number, roleCodes: string[]) => {
  return request.post(`/alerts/${alertId}/send-email`, { roleCodes })
}

// 发送测试邮件
export const sendTestEmailApi = (email: string, subject?: string) => {
  return request.post('/email/test', { email, subject: subject || '测试邮件' })
}

// 发送任务邮件通知
export const sendTaskEmailApi = (taskId: number, emailType: 'assignment' | 'completion' | 'unassignment') => {
  return request.post(`/tasks/${taskId}/send-email`, { emailType })
}
