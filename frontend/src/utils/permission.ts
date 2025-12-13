// 权限工具函数
import { useUserStore } from '@/store/user'
import { computed } from 'vue'

/**
 * 检查用户是否有指定权限
 */
export const hasPermission = (permission: string): boolean => {
  const userStore = useUserStore()
  if (!userStore.userInfo) {
    return false
  }
  
  // 管理员拥有所有权限
  if (userStore.userInfo.roles?.includes('ADMIN')) {
    return true
  }
  
  // 检查用户权限列表
  return userStore.userInfo.permissions?.includes(permission) || false
}

/**
 * 检查用户是否有任一权限
 */
export const hasAnyPermission = (permissions: string[]): boolean => {
  return permissions.some(permission => hasPermission(permission))
}

/**
 * 检查用户是否有所有权限
 */
export const hasAllPermissions = (permissions: string[]): boolean => {
  return permissions.every(permission => hasPermission(permission))
}

/**
 * 检查用户是否是管理员
 */
export const isAdmin = (): boolean => {
  const userStore = useUserStore()
  return userStore.userInfo?.roles?.includes('ADMIN') || false
}

/**
 * 检查用户是否是查看者（只读）
 */
export const isViewer = (): boolean => {
  const userStore = useUserStore()
  const roles = userStore.userInfo?.roles || []
  // 如果只有查看者角色，或者没有编辑权限
  return roles.includes('VIEWER') && !hasAnyPermission(['user:create', 'user:update', 'user:delete', 
                                                       'vehicle:create', 'vehicle:update', 'vehicle:delete',
                                                       'task:create', 'task:update', 'task:delete'])
}

