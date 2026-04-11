import { defineStore } from 'pinia'
import { GetCurrentUser, GetRolePermissions, Login, Logout } from '@/api'
import { updateRoutes } from '@/router/generator'
import {
  clearAuthState,
  getStoredPermissions,
  getStoredUserInfo,
  setToken as setAuthToken,
  getToken as getAuthToken,
  removeToken as removeAuthToken,
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

    setToken(token = 'server-session') {
      this.token = token || ''
      setAuthToken(token || 'server-session')
    },

    setPermissions(permissions) {
      const list = Array.isArray(permissions) ? permissions : []
      this.permissions = list
      setStoredPermissions(list)
    },

    clearLocalState() {
      this.userInfo = null
      this.user = null
      this.token = ''
      this.isLoggedIn = false
      this.permissions = []
      clearAuthState()
    },

    async loadPermissionsByRoleId(roleId) {
      if (roleId == null) {
        return []
      }

      const permissionsResponse = await GetRolePermissions(roleId)
      const permissionPayload = permissionsResponse?.data || permissionsResponse || []
      return (Array.isArray(permissionPayload) ? permissionPayload : [])
        .filter(item => item && item.permissionCode)
        .map(item => item.permissionCode)
    },

    async syncSessionFromServer(options = {}) {
      const { forceReloadPermissions = false } = options || {}
      const userResponse = await GetCurrentUser()
      const user = userResponse?.data || userResponse?.user || userResponse || null

      if (!user || typeof user !== 'object') {
        throw new Error('获取当前会话用户失败')
      }

      const cachedPermissions = Array.isArray(this.permissions) ? this.permissions : []
      const previousRoleId = this.userInfo?.roleId ?? this.user?.roleId ?? null
      const currentRoleId = user.roleId ?? null
      const roleChanged =
        previousRoleId !== null &&
        currentRoleId !== null &&
        String(previousRoleId) !== String(currentRoleId)
      const shouldReloadPermissions =
        forceReloadPermissions || roleChanged || cachedPermissions.length === 0
      const permissions = shouldReloadPermissions
        ? await this.loadPermissionsByRoleId(user.roleId)
        : cachedPermissions

      this.setToken('server-session')
      this.setUserInfo(user)
      if (shouldReloadPermissions) {
        this.setPermissions(permissions)
      } else {
        this.permissions = permissions
      }
      this.isLoggedIn = true
      updateRoutes(permissions)

      return {
        user,
        permissions
      }
    },

    async login(loginData) {
      try {
        const response = await Login(loginData)
        const payload = normalizeLoginPayload(response)

        if (!(payload.success || payload.code === 200)) {
          throw new Error(payload.message || '登录失败')
        }

        await this.syncSessionFromServer()
        return payload
      } catch (error) {
        console.error('登录失败:', error)
        throw error
      }
    },

    async logout(options = {}) {
      const { silent = true } = options || {}

      try {
        await Logout()
      } catch (error) {
        if (!silent) {
          throw error
        }
      }

      this.clearLocalState()
      removeAuthToken()
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

        const permissions = await this.loadPermissionsByRoleId(user.roleId)
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
