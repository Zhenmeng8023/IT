import { defineStore } from 'pinia'
import {
  GetAllPermissions,
  GetPermissionById,
  CreatePermission,
  UpdatePermission,
  DeletePermission
} from '@/api'

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    permissions: [],
    currentPermission: null,
    loading: false,
    error: null
  }),
  getters: {
    getPermissions: (state) => state.permissions,
    getCurrentPermission: (state) => state.currentPermission,
    getLoading: (state) => state.loading,
    getError: (state) => state.error
  },
  actions: {
    // 获取所有权限
    async fetchPermissions() {
      this.loading = true
      this.error = null
      try {
        const response = await GetAllPermissions()
        this.permissions = response.data
      } catch (error) {
        this.error = error.message
        console.error('获取权限列表失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    // 根据ID获取权限
    async fetchPermissionById(id) {
      this.loading = true
      this.error = null
      try {
        const response = await GetPermissionById(id)
        this.currentPermission = response.data
      } catch (error) {
        this.error = error.message
        console.error('获取权限详情失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    // 创建权限
    async createPermission(permissionData) {
      this.loading = true
      this.error = null
      try {
        const response = await CreatePermission(permissionData)
        this.permissions.push(response.data)
        return response.data
      } catch (error) {
        this.error = error.message
        console.error('创建权限失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 更新权限
    async updatePermission(id, permissionData) {
      this.loading = true
      this.error = null
      try {
        const response = await UpdatePermission(id, permissionData)
        const index = this.permissions.findIndex(permission => permission.id === id)
        if (index !== -1) {
          this.permissions[index] = response.data
        }
        if (this.currentPermission && this.currentPermission.id === id) {
          this.currentPermission = response.data
        }
        return response.data
      } catch (error) {
        this.error = error.message
        console.error('更新权限失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 删除权限
    async deletePermission(id) {
      this.loading = true
      this.error = null
      try {
        await DeletePermission(id)
        this.permissions = this.permissions.filter(permission => permission.id !== id)
        if (this.currentPermission && this.currentPermission.id === id) {
          this.currentPermission = null
        }
      } catch (error) {
        this.error = error.message
        console.error('删除权限失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 重置状态
    resetState() {
      this.permissions = []
      this.currentPermission = null
      this.loading = false
      this.error = null
    }
  }
})