import { GetCurrentUser, GetRolePermissions } from '~/api/index'
import { getToken, removeToken } from '~/utils/auth'
import { getRoutePermission, hasPermission } from '~/utils/permissionConfig'

let permissionCache = {
  timestamp: 0,
  data: null
}

const CACHE_DURATION = 5 * 60 * 1000

function matchWhiteList(path, routePath) {
  if (path.includes('*')) {
    const regex = new RegExp('^' + path.replace(/\*/g, '.*') + '$')
    return regex.test(routePath)
  }

  if (path.includes(':')) {
    const regex = new RegExp('^' + path.replace(/:\w+/g, '\\w+') + '$')
    return regex.test(routePath)
  }

  return path === routePath
}

function hasRouteAccess(userPermissions, requiredPermission) {
  if (hasPermission(userPermissions, requiredPermission)) {
    return true
  }

  const parts = requiredPermission.split(':')
  for (let i = parts.length - 1; i > 0; i--) {
    const parentPermission = parts.slice(0, i).join(':') + ':*'
    if (hasPermission(userPermissions, parentPermission)) {
      return true
    }
  }

  return false
}

export default async function ({ route, redirect, app, store, req }) {
  const whiteList = [
    '/login',
    '/registe',
    '/',
    '/blog',
    '/blog/:id',
    '/circle',
    '/circle/:id',
    '/user',
    '/collection',
    '/projectlist',
    '/projectdetail',
    '/projectmanage',
    '/projectcollection',
    '/projectmember',
    '/hybridaction/*'
  ]

  const isWhiteList = whiteList.some(path => matchWhiteList(path, route.path))
  if (isWhiteList) {
    return
  }

  try {
    const userStore = store.user || {}
    const token = getToken(req)

    if (!token) {
      return redirect('/login')
    }

    if (app.$axios) {
      app.$axios.defaults.headers.common['X-Token'] = token
      app.$axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
    }

    const now = Date.now()

    if (
      !userStore.user ||
      !userStore.permissions ||
      userStore.permissions.length === 0 ||
      permissionCache.timestamp + CACHE_DURATION < now
    ) {
      const userResponse = await GetCurrentUser()
      let user = userResponse?.data || userResponse?.user || null

      if (!user) {
        throw new Error('获取用户信息失败')
      }

      let permissionCodes = []

      if (user.roleId) {
        const permissionsResponse = await GetRolePermissions(user.roleId)
        const rolePermissions = permissionsResponse?.data || []
        permissionCodes = rolePermissions
          .filter(item => item && item.permissionCode)
          .map(item => item.permissionCode)
      }

      if (userStore.setUserInfo) {
        userStore.setUserInfo(user)
      }

      if (userStore.setPermissions) {
        userStore.setPermissions(permissionCodes)
      }

      permissionCache = {
        timestamp: now,
        data: {
          user,
          permissions: permissionCodes
        }
      }
    }

    const userPermissions = permissionCache.data?.permissions || []
    const requiredPermission = getRoutePermission(route.path)

    if (requiredPermission && !hasRouteAccess(userPermissions, requiredPermission)) {
      if (app.$message) {
        app.$message.error('无权限访问此页面')
      }
      return redirect('/noPermission')
    }

    if (route.meta && route.meta.permissions) {
      const metaPermissions = Array.isArray(route.meta.permissions)
        ? route.meta.permissions
        : [route.meta.permissions]

      const missingPermissions = metaPermissions.filter(
        perm => !hasRouteAccess(userPermissions, perm)
      )

      if (missingPermissions.length > 0) {
        if (app.$message) {
          app.$message.error(`缺少权限: ${missingPermissions.join(', ')}`)
        }
        return redirect('/noPermission')
      }
    }
  } catch (error) {
    permissionCache = {
      timestamp: 0,
      data: null
    }

    try {
      if (process.client) {
        localStorage.removeItem('token')
        localStorage.removeItem('userToken')
        localStorage.removeItem('userPermissions')
        localStorage.removeItem('userInfo')
      }
    } catch (e) {}

    removeToken()
    return redirect('/login')
  }
}