import { defineStore } from 'pinia'
import { Login, GetRolePermissions } from '@/api'
import { updateRoutes } from '@/router/generator'
import {
  setToken as setAuthToken,
  getToken as getAuthToken,
  removeToken as removeAuthToken
} from '@/utils/auth'
import { hasPermission } from '@/utils/permissionConfig'

function normalizeLoginPayload(response) {
  return response?.data || response || {}
}

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    userInfo: null,
    token: '',
    isLoggedIn: false,
    permissions: []
  }),

  getters: {
    getUserInfo: (state) => state.userInfo || state.user,
    getToken: (state) => {
      if (state.token) {
        return state.token
      }
      const authToken = getAuthToken()
      if (authToken) {
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
      this.userInfo = userInfo || null
      this.user = userInfo || null
      this.isLoggedIn = !!userInfo

      if (process.client) {
        if (userInfo) {
          localStorage.setItem('userInfo', JSON.stringify(userInfo))
        } else {
          localStorage.removeItem('userInfo')
        }
      }
    },

    setToken(token) {
      this.token = token || ''
      if (!token) {
        return
      }

      if (process.client) {
        localStorage.setItem('token', token)
        localStorage.setItem('userToken', token)
      }

      setAuthToken(token)
    },

    setPermissions(permissions) {
      const list = Array.isArray(permissions) ? permissions : []
      this.permissions = list

      if (process.client) {
        localStorage.setItem('userPermissions', JSON.stringify(list))
      }
    },

    async login(loginData) {
      try {
        const response = await Login(loginData)
        const payload = normalizeLoginPayload(response)

        if (!(payload.success || payload.code === 200)) {
          throw new Error(payload.message || '登录失败')
        }

        const token =
          payload.other?.token ||
          payload.token ||
          payload.access_token ||
          payload.token_info?.token ||
          ''

        if (!token) {
          throw new Error('登录响应中缺少 token')
        }

        const roleId = payload.other?.roleId ?? payload.roleId ?? null
        const user = payload.other?.user || payload.user || {}

        if (roleId != null && user && !user.roleId) {
          user.roleId = roleId
        }

        let permissions = []
        if (roleId != null) {
          const permissionsResponse = await GetRolePermissions(roleId)
          const permissionPayload = permissionsResponse?.data || permissionsResponse || []
          permissions = (Array.isArray(permissionPayload) ? permissionPayload : [])
            .filter(item => item && item.permissionCode)
            .map(item => item.permissionCode)
        }

        this.setToken(token)
        this.setUserInfo(user)
        this.setPermissions(permissions)
        updateRoutes(permissions)

        return payload
      } catch (error) {
        console.error('登录失败:', error)
        throw error
      }
    },

    logout() {
      this.userInfo = null
      this.user = null
      this.token = ''
      this.isLoggedIn = false
      this.permissions = []

      if (process.client) {
        localStorage.removeItem('token')
        localStorage.removeItem('userToken')
        localStorage.removeItem('userPermissions')
        localStorage.removeItem('userInfo')
      }

      removeAuthToken()
    },

    restorePermissions() {
      if (!process.client) {
        return
      }

      const savedPermissions = localStorage.getItem('userPermissions')
      const savedUserInfo = localStorage.getItem('userInfo')
      const savedToken = localStorage.getItem('token') || localStorage.getItem('userToken') || getAuthToken()

      if (savedUserInfo) {
        try {
          const userInfo = JSON.parse(savedUserInfo)
          this.userInfo = userInfo
          this.user = userInfo
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
          if (this.permissions.length > 0 || this.token) {
            this.isLoggedIn = true
          }
        } catch (error) {
          console.error('恢复权限状态失败:', error)
          this.permissions = []
        }
      }
    },

    async refreshPermissions() {
      try {
        const user = this.userInfo || this.user
        if (!user || !user.roleId) {
          return []
        }

        const permissionsResponse = await GetRolePermissions(user.roleId)
        const permissionPayload = permissionsResponse?.data || permissionsResponse || []
        const permissions = (Array.isArray(permissionPayload) ? permissionPayload : [])
          .filter(item => item && item.permissionCode)
          .map(item => item.permissionCode)

        this.setPermissions(permissions)
        updateRoutes(permissions)
        return permissions
      } catch (error) {
        console.error('刷新权限失败:', error)
        throw error
      }
    }
  }
})
