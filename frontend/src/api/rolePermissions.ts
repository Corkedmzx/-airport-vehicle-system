// 角色权限相关API
import request from '@/utils/request'

export interface Role {
  id: number
  roleName: string
  roleCode: string
  description?: string
  status: number
}

export interface Permission {
  id: number
  permissionName: string
  permissionCode: string
  permissionType: string
  resource?: string
  description?: string
  status: number
}

export interface RolePermissionDTO {
  roleId: number
  roleName: string
  roleCode: string
  permissions: string[]
}

// 获取所有角色
export const getAllRolesApi = () => {
  return request.get<Role[]>('/role-permissions/roles')
}

// 获取角色权限
export const getRolePermissionsApi = (roleId: number) => {
  return request.get<RolePermissionDTO>(`/role-permissions/roles/${roleId}/permissions`)
}

// 更新角色权限
export const updateRolePermissionsApi = (roleId: number, permissionCodes: string[]) => {
  return request.put<string>(`/role-permissions/roles/${roleId}/permissions`, permissionCodes)
}

// 获取所有权限
export const getAllPermissionsApi = () => {
  return request.get<Permission[]>('/role-permissions/permissions')
}

