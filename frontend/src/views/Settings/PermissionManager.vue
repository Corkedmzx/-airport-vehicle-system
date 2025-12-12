<!-- frontend/src/components/settings/PermissionManager.vue -->
<template>
  <div class="permission-manager">
    <el-card>
      <template #header>
        <span>角色权限管理</span>
      </template>
      
      <el-table :data="roles" style="width: 100%">
        <el-table-column prop="name" label="角色名称" width="120" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="permissions" label="权限" width="200">
          <template #default="scope">
            <el-tag 
              v-for="permission in scope.row.permissions" 
              :key="permission"
              size="small"
              style="margin-right: 5px"
            >
              {{ permission }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button size="small" @click="editRole(scope.row)">编辑</el-button>
            <el-button 
              size="small" 
              type="danger" 
              @click="deleteRole(scope.row.id)"
              v-if="scope.row.name !== 'admin'"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>