import { defineStore } from 'pinia'
import {
  GetAllRoles,
  GetRoleById,
  CreateRole,
  UpdateRole,
  DeleteRole,
  AssignPermissionsToRole,
  GetRolePermissions,
  AssignRolesToUser,
  GetUserRoles,
  GetUserPermissions,
  RoleAssignMenus,
  GetRoleMenus
} from '@/api'

export const useRoleStore = defineStore('role', {
  state: () => ({
    roles: [],
    currentRole: null,
    rolePermissions: [],
    roleMenus: [],
    loading: false,
    error: null
  }),
  getters: {
    getRoles: (state) => state.roles,
    getCurrentRole: (state) => state.currentRole,
    getRolePermissions: (state) => state.rolePermissions,
    getRoleMenus: (state) => state.roleMenus,
    getLoading: (state) => state.loading,
    getError: (state) => state.error
  },
  actions: {
    // 获取所有角色
    async fetchRoles() {
      this.loading = true
      this.error = null
      try {
        const response = await GetAllRoles()
        this.roles = response.data
      } catch (error) {
        this.error = error.message
        console.error('获取角色列表失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    // 根据ID获取角色
    async fetchRoleById(id) {
      this.loading = true
      this.error = null
      try {
        const response = await GetRoleById(id)
        this.currentRole = response.data
      } catch (error) {
        this.error = error.message
        console.error('获取角色详情失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    // 创建角色
    async createRole(roleData) {
      this.loading = true
      this.error = null
      try {
        const response = await CreateRole(roleData)
        this.roles.push(response.data)
        return response.data
      } catch (error) {
        this.error = error.message
        console.error('创建角色失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 更新角色
    async updateRole(id, roleData) {
      this.loading = true
      this.error = null
      try {
        const response = await UpdateRole(id, roleData)
        const index = this.roles.findIndex(role => role.id === id)
        if (index !== -1) {
          this.roles[index] = response.data
        }
        if (this.currentRole && this.currentRole.id === id) {
          this.currentRole = response.data
        }
        return response.data
      } catch (error) {
        this.error = error.message
        console.error('更新角色失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 删除角色
    async deleteRole(id) {
      this.loading = true
      this.error = null
      try {
        await DeleteRole(id)
        this.roles = this.roles.filter(role => role.id !== id)
        if (this.currentRole && this.currentRole.id === id) {
          this.currentRole = null
        }
      } catch (error) {
        this.error = error.message
        console.error('删除角色失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 获取角色权限
    async fetchRolePermissions(roleId) {
      this.loading = true
      this.error = null
      try {
        const response = await GetRolePermissions(roleId)
        this.rolePermissions = response.data
      } catch (error) {
        this.error = error.message
        console.error('获取角色权限失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    // 为角色分配权限
    async assignPermissions(roleId, permissionIds) {
      this.loading = true
      this.error = null
      try {
        await AssignPermissionsToRole(roleId, { permissions: permissionIds })
        // 重新获取角色权限
        await this.fetchRolePermissions(roleId)
      } catch (error) {
        this.error = error.message
        console.error('分配权限失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 获取角色菜单
    async fetchRoleMenus(roleId) {
      this.loading = true
      this.error = null
      try {
        const response = await GetRoleMenus(roleId)
        this.roleMenus = response.data
      } catch (error) {
        this.error = error.message
        console.error('获取角色菜单失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    // 为角色分配菜单
    async assignMenus(roleId, menuIds) {
      this.loading = true
      this.error = null
      try {
        await RoleAssignMenus(roleId, menuIds)
        // 重新获取角色菜单
        await this.fetchRoleMenus(roleId)
      } catch (error) {
        this.error = error.message
        console.error('分配菜单失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 为用户分配角色
    async assignRolesToUser(userId, roleIds) {
      this.loading = true
      this.error = null
      try {
        await AssignRolesToUser(userId, { roles: roleIds })
      } catch (error) {
        this.error = error.message
        console.error('为用户分配角色失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 获取用户角色
    async fetchUserRoles(userId) {
      this.loading = true
      this.error = null
      try {
        const response = await GetUserRoles(userId)
        return response.data
      } catch (error) {
        this.error = error.message
        console.error('获取用户角色失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 获取用户权限
    async fetchUserPermissions(userId) {
      this.loading = true
      this.error = null
      try {
        const response = await GetUserPermissions(userId)
        return response.data
      } catch (error) {
        this.error = error.message
        console.error('获取用户权限失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 重置状态
    resetState() {
      this.roles = []
      this.currentRole = null
      this.rolePermissions = []
      this.roleMenus = []
      this.loading = false
      this.error = null
    }
  }
})