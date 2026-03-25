import { pinia } from '~/plugins/pinia'
import { useUserStore } from '~/store/user'
import { GetCurrentUser, GetRolePermissions, setCurrentReq } from '~/api/index'
import { getToken, removeToken } from '~/utils/auth'
import { getRoutePermission, hasPermission } from '~/utils/permissionConfig'

let permissionCache = {
  timestamp: 0,
  data: null
}

const CACHE_DURATION = 5 * 60 * 1000
const WHITE_LIST = ['/login', '/registe', '/', '/blog', '/circle', '/user']
const DYNAMIC_WHITE_LIST = [/^\/blog\/[^/]+$/, /^\/circle\/[^/]+$/, /^\/hybridaction\/.+$/]

function isWhiteRoute(path) {
  if (WHITE_LIST.includes(path)) {
    return true
  }
  return DYNAMIC_WHITE_LIST.some(rule => rule.test(path))
}

export default async function ({ route, redirect, app, req }) {
  if (isWhiteRoute(route.path)) {
    return
  }

  const userStore = useUserStore(pinia)
  const token = getToken(req)

  if (!token) {
    return redirect('/login')
  }

  if (app.$axios) {
    app.$axios.defaults.headers.common['X-Token'] = token
  }

  if (process.client) {
    userStore.restorePermissions()
  }

  try {
    const now = Date.now()
    const needRefresh =
      !permissionCache.data ||
      !userStore.getUserInfo ||
      !Array.isArray(userStore.getPermissions) ||
      userStore.getPermissions.length === 0 ||
      permissionCache.timestamp + CACHE_DURATION < now

    if (needRefresh) {
      setCurrentReq(req)
      const userResponse = await GetCurrentUser()
      const user = userResponse?.data || userResponse?.user || null

      if (!user) {
        throw new Error('获取当前用户失败')
      }

      let permissions = []
      if (user.roleId) {
        const permissionsResponse = await GetRolePermissions(user.roleId)
        permissions = (permissionsResponse?.data || [])
          .filter(item => item && item.permissionCode)
          .map(item => item.permissionCode)
      }

      userStore.setUserInfo(user)
      userStore.setPermissions(permissions)
      permissionCache = {
        timestamp: now,
        data: {
          user,
          permissions
        }
      }
    }

    const userPermissions = userStore.getPermissions || permissionCache.data?.permissions || []
    const requiredPermission = getRoutePermission(route.path)

    if (requiredPermission && !hasPermission(userPermissions, requiredPermission)) {
      return redirect('/noPermission')
    }

    if (route.meta && route.meta.length > 0) {
      const metaPermissions = route.meta
        .flatMap(item => item.permissions || [])
        .filter(Boolean)
      const missing = metaPermissions.filter(code => !hasPermission(userPermissions, code))
      if (missing.length > 0) {
        return redirect('/noPermission')
      }
    }
  } catch (error) {
    console.error('权限检查失败:', error)
    permissionCache = {
      timestamp: 0,
      data: null
    }
    removeToken()
    userStore.logout()
    return redirect('/login')
  }
}
