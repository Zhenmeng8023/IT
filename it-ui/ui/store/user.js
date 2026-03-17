import { defineStore } from 'pinia'
import { Login, GetRoleMenus, GetAllPermissions } from '@/api'
import { updateRoutes } from '@/router/generator'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    token: '',
    isLoggedIn: false,
    permissions: []
  }),
  getters: {
    getUserInfo: (state) => state.userInfo,
    getToken: (state) => state.token,
    getIsLoggedIn: (state) => state.isLoggedIn,
    getPermissions: (state) => state.permissions
  },
  actions: {
    setUserInfo(userInfo) {
      this.userInfo = userInfo
      this.isLoggedIn = true
    },
    setToken(token) {
      this.token = token
    },
    setPermissions(permissions) {
      this.permissions = permissions
      // 存储权限到本地存储
      localStorage.setItem('userPermissions', JSON.stringify(permissions))
    },
    async login(loginData) {
      try {
        const response = await Login(loginData)
        let token, user, roleId, permissions = []
        
        // 处理登录响应
        if (response.data && (response.data.success || response.data.code == 200)) {
          // 从响应中获取token和roleId
          token = response.data.token
          roleId = response.data.roleId
          user = response.data.user || {}
          
          if (!token) {
            throw new Error('登录响应中缺少token')
          }
          
          if (roleId) {
            // 调用GetRoleMenus获取用户角色的菜单和按钮
            const menusResponse = await GetRoleMenus(roleId)
            const menus = menusResponse.data || []
            
            // 提取所有权限id
            const permissionIds = new Set()
            const extractPermissionIds = (items) => {
              items.forEach(item => {
                if (item.permissionId) {
                  permissionIds.add(item.permissionId)
                }
                if (item.children && item.children.length > 0) {
                  extractPermissionIds(item.children)
                }
              })
            }
            extractPermissionIds(menus)
            
            // 调用GetAllPermissions获取所有权限信息
            const permissionsResponse = await GetAllPermissions()
            const allPermissions = permissionsResponse.data || []
            
            // 根据权限id匹配权限代码
            permissionIds.forEach(id => {
              const permission = allPermissions.find(p => p.id == id)
              if (permission && permission.permissionCode) {
                permissions.push(permission.permissionCode)
              }
            })
          }
        } else {
          throw new Error(response.data?.message || '登录失败')
        }
        
        // 设置用户信息和token
        this.setToken(token)
        this.setUserInfo(user)
        this.setPermissions(permissions)
        
        // 更新路由配置
        updateRoutes(permissions)
        
        return response
      } catch (error) {
        console.error('登录失败:', error)
        throw error
      }
    },
    logout() {
      this.userInfo = null
      this.token = ''
      this.isLoggedIn = false
      this.permissions = []
      // 清除本地存储中的权限
      localStorage.removeItem('userPermissions')
    }
  }
})