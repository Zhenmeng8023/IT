import { defineStore } from 'pinia'
import { Login, GetRolePermissions } from '@/api'
import { updateRoutes } from '@/router/generator'
import { setToken as setAuthToken, getToken as getAuthToken, removeToken as removeAuthToken } from '@/utils/auth'
import { hasPermission } from '@/utils/permissionConfig'

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
    getToken: (state) => {
      // 如果state中有token，优先使用
      if (state.token) {
        return state.token
      }
      // 否则尝试从auth工具获取
      const authToken = getAuthToken()
      if (authToken) {
        // 同步到state
        state.token = authToken
      }
      return authToken
    },
    getIsLoggedIn: (state) => state.isLoggedIn,
    getPermissions: (state) => state.permissions,
    hasPermission: (state) => (permissionCode) => {
      return hasPermission(state.permissions, permissionCode)
    }
  },
  actions: {
    setUserInfo(userInfo) {
      this.userInfo = userInfo
      this.user = userInfo // 与中间件保持一致
      this.isLoggedIn = true
      // 存储用户信息到本地存储（仅在客户端）
      if (process.client) {
        localStorage.setItem('userInfo', JSON.stringify(userInfo))
      }
    },
    setToken(token) {
      console.log('=== setToken() 开始执行 ===')
      console.log('设置token值:', token)
      
      this.token = token
      
      // 存储token到本地存储（仅在客户端）
      if (process.client) {
        console.log('客户端环境，存储到localStorage')
        localStorage.setItem('userToken', token)
        console.log('localStorage存储完成，验证:', localStorage.getItem('userToken'))
      } else {
        console.log('服务端环境，跳过localStorage存储')
      }
      
      // 存储到Cookies
      console.log('存储token到Cookies')
      const result = setAuthToken(token) // 使用auth.js中的方法
      console.log('Cookies存储结果:', result)
      console.log('=== setToken() 执行完成 ===')
    },
    setPermissions(permissions) {
      this.permissions = permissions
      // 存储权限到本地存储（仅在客户端）
      if (process.client) {
        localStorage.setItem('userPermissions', JSON.stringify(permissions))
      }
    },
    async login(loginData) {
      try {
        console.log('开始登录，登录数据:', loginData)
        
        // 1. 发送登录请求
        const response = await Login(loginData)
        console.log('登录响应:', response)
        
        let token, user, roleId, permissions = []
        
        // 处理登录响应
        console.log('完整的登录响应结构:', response)
        console.log('响应data字段:', response.data)
        console.log('响应状态:', response.data?.success, response.data?.code)
        
        if (response.data && (response.data.success || response.data.code == 200)) {
          // 从响应中获取token和roleId
          // 注意：根据实际API响应结构调整
          console.log('登录响应数据:', response.data)
          
          // 详细检查响应结构
          console.log('response.data.token:', response.data.token)
          console.log('response.data.other:', response.data.other)
          console.log('response.token:', response.token)
          console.log('response.data.access_token:', response.data.access_token)
          console.log('response.data.token_info:', response.data.token_info)
          
          token = response.data.token || (response.data.other?.token || '')
          roleId = response.data.roleId || (response.data.other?.roleId || null)
          user = response.data.user || (response.data.other?.user || {})
          
          // 额外检查，确保token存在
          if (!token) {
            // 尝试从其他可能的位置获取token
            token = response.token || response.data.access_token || response.data.token_info?.token || ''
            console.log('尝试从其他位置获取token:', token)
          }
          
          console.log('最终提取的token:', token)
          console.log('最终提取的roleId:', roleId)
          console.log('最终提取的user:', user)
          
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
        console.log('准备设置的token值:', token)
        
        // 先设置token，然后验证
        this.setToken(token)
        console.log('setToken()执行完成，store中的token:', this.token)
        
        // 验证token是否正确存储到Cookies
        const verifyToken = getAuthToken()
        console.log('验证Cookies中的token:', verifyToken)
        
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
      // 清除本地存储中的权限和用户信息（仅在客户端）
      if (process.client) {
        localStorage.removeItem('userPermissions')
        localStorage.removeItem('userInfo')
        localStorage.removeItem('userToken')
      }
      removeAuthToken() // 使用auth.js中的方法
    },
    
    // 恢复权限状态
    restorePermissions() {
      // 服务器端使用空状态，客户端从本地存储恢复
      if (process.client) {
        const savedPermissions = localStorage.getItem('userPermissions')
        const savedUserInfo = localStorage.getItem('userInfo')
        const savedToken = localStorage.getItem('userToken')
        
        if (savedUserInfo) {
          try {
            const userInfo = JSON.parse(savedUserInfo)
            this.userInfo = userInfo
            this.user = userInfo
            this.isLoggedIn = true
          } catch (error) {
            console.error('恢复用户信息失败:', error)
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
          }
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