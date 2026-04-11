import { defineStore } from 'pinia'
import { Login, GetRolePermissions } from '@/api'
import { updateRoutes } from '@/router/generator'
import {
  setToken as setAuthToken,
  getToken as getAuthToken,
  clearAuthState,
  getStoredPermissions,
  getStoredUserInfo,
  removeStoredPermissions,
  removeStoredUserInfo,
  setStoredPermissions,
  setStoredUserInfo
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

      if (userInfo) {
        setStoredUserInfo(userInfo)
      } else {
        removeStoredUserInfo()
      }
    },

    setToken(token) {
      this.token = token || ''
      if (!token) {
        return
      }

      setAuthToken(token)
    },

    setPermissions(permissions) {
      const list = Array.isArray(permissions) ? permissions : []
      this.permissions = list
      setStoredPermissions(list)
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
      clearAuthState()
    },

    restorePermissions() {
      if (!process.client) {
        return
      }

      const savedPermissions = getStoredPermissions()
      const savedUserInfo = getStoredUserInfo()
      const savedToken = getAuthToken()

      if (savedUserInfo) {
        this.userInfo = savedUserInfo
        this.user = savedUserInfo
        this.isLoggedIn = true
      } else {
        this.userInfo = null
        this.user = null
      }

      if (savedToken) {
        this.token = savedToken
      } else {
        this.token = ''
      }

      if (savedPermissions.length > 0) {
        this.permissions = savedPermissions
      } else {
        this.permissions = []
      }

      this.isLoggedIn = !!(savedUserInfo || savedToken || this.permissions.length > 0)

      if (!savedUserInfo && !savedToken) {
        removeStoredPermissions()
        removeStoredUserInfo()
        this.isLoggedIn = false
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
