import { defineStore } from 'pinia'
import { Login, GetRolePermissions } from '@/api'
import { updateRoutes } from '@/router/generator'
import { setToken as setAuthToken, getToken as getAuthToken, removeToken as removeAuthToken } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null, // 与中间件保持一致
    userInfo: null, // 向后兼容
    token: '',
    isLoggedIn: false,
    permissions: []
  }),
  getters: {
    getUserInfo: (state) => state.userInfo || state.user,
    getToken: (state) => state.token || getAuthToken(),
    getIsLoggedIn: (state) => state.isLoggedIn,
    getPermissions: (state) => state.permissions,
    hasPermission: (state) => (permissionCode) => {
      return state.permissions.includes(permissionCode)
    }
  },
  actions: {
    setUserInfo(userInfo) {
      this.userInfo = userInfo
      this.user = userInfo // 与中间件保持一致
      this.isLoggedIn = true
      // 存储用户信息到本地存储
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },
    setToken(token) {
      this.token = token
      // 存储token到本地存储
      localStorage.setItem('userToken', token)
      setAuthToken(token) // 使用auth.js中的方法
    },
    setPermissions(permissions) {
      this.permissions = permissions
      // 存储权限到本地存储
      localStorage.setItem('userPermissions', JSON.stringify(permissions))
    },
    async login(loginData) {
      try {
        console.log('开始登录，登录数据:', loginData)
        
        // 1. 发送登录请求
        const response = await Login(loginData)
        console.log('登录响应:', response)
        
        let token, user, roleId, permissions = []
        
        // 处理登录响应
        if (response.data && (response.data.success || response.data.code == 200)) {
          // 从响应中获取token和roleId
          // 注意：根据实际API响应结构调整
          console.log('登录响应数据:', response.data)
          token = response.data.token || (response.data.other?.token || '')
          roleId = response.data.roleId || (response.data.other?.roleId || null)
          user = response.data.user || (response.data.other?.user || {})
          
          // 额外检查，确保token存在
          if (!token) {
            // 尝试从其他可能的位置获取token
            token = response.token || response.data.access_token || response.data.token_info?.token || ''
            console.log('尝试从其他位置获取token:', token)
          }
          
          // 确保user对象中包含roleId
          if (roleId && !user.roleId) {
            user.roleId = roleId
          }
          
          console.log('从响应中提取的数据:', { token, roleId, user })
          console.log('完整的响应结构:', response.data)
          
          if (!token) {
            throw new Error('登录响应中缺少token')
          }
          
          if (roleId) {
            console.log('开始获取角色权限，角色ID:', roleId)
            // 3. 调用API获取角色权限
            const permissionsResponse = await GetRolePermissions(roleId)
            console.log('权限查询响应:', permissionsResponse)
            
            const rolePermissions = permissionsResponse.data || []
            console.log('角色原始权限数据:', rolePermissions)
            
            // 4. 提取权限代码
            permissions = rolePermissions
              .filter(permission => permission.permissionCode)
              .map(permission => permission.permissionCode)
            console.log('提取的权限代码:', permissions)
          }
        } else {
          throw new Error(response.data?.message || '登录失败')
        }
        
        // 5. 设置用户信息和token
        console.log('开始保存用户信息和权限到store')
        this.setToken(token)
        this.setUserInfo(user)
        this.setPermissions(permissions)
        
        console.log('store中保存的权限:', this.permissions)
        
        // 6. 更新路由配置
        console.log('开始更新路由配置')
        updateRoutes(permissions)
        
        console.log('登录流程完成')
        return response
      } catch (error) {
        console.error('登录失败:', error)
        throw error
      }
    },
    logout() {
      this.userInfo = null
      this.user = null // 与中间件保持一致
      this.token = ''
      this.isLoggedIn = false
      this.permissions = []
      // 清除本地存储中的权限和用户信息
      localStorage.removeItem('userPermissions')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('userToken')
      removeAuthToken() // 使用auth.js中的方法
    },
    
    // 从本地存储恢复权限状态
    restorePermissions() {
      const savedPermissions = localStorage.getItem('userPermissions')
      const savedUserInfo = localStorage.getItem('userInfo')
      const savedToken = localStorage.getItem('userToken')
      
      if (savedUserInfo) {
        try {
          const userInfo = JSON.parse(savedUserInfo)
          this.userInfo = userInfo
          this.user = userInfo // 与中间件保持一致
          this.isLoggedIn = true
        } catch (error) {
          console.error('恢复用户信息失败:', error)
          this.userInfo = null
          this.user = null
          this.isLoggedIn = false
        }
      }
      
      if (savedToken) {
        this.token = savedToken
      }
      
      if (savedPermissions) {
        try {
          this.permissions = JSON.parse(savedPermissions)
          this.isLoggedIn = true
        } catch (error) {
          console.error('恢复权限状态失败:', error)
          this.permissions = []
          this.isLoggedIn = false
        }
      }
    },
    
    // 刷新用户权限
    async refreshPermissions() {
      try {
        // 检查是否有用户信息和角色ID
        const user = this.userInfo || this.user
        if (!user || !user.roleId) {
          console.log('用户信息或角色ID不存在，无法刷新权限')
          return
        }
        
        // 调用API获取角色权限
        const permissionsResponse = await GetRolePermissions(user.roleId)
        console.log('刷新权限响应:', permissionsResponse)
        
        const rolePermissions = permissionsResponse.data || []
        console.log('角色原始权限数据:', rolePermissions)
        
        // 提取权限代码
        const permissions = rolePermissions
          .filter(permission => permission.permissionCode)
          .map(permission => permission.permissionCode)
        console.log('提取的权限代码:', permissions)
        
        // 更新权限状态
        this.setPermissions(permissions)
        console.log('权限刷新完成:', this.permissions)
        
        // 更新路由配置
        updateRoutes(permissions)
        console.log('路由配置更新完成')
        
        return permissions
      } catch (error) {
        console.error('刷新权限失败:', error)
        throw error
      }
    }
  }
})