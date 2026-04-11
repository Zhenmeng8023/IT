import { useUserStore } from '~/store/user'
import { clearAuthState } from '~/utils/auth'
import { getRoutePermission, hasPermission } from '~/utils/permissionConfig'

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
    '/myproject',
    '/hybridaction/*'
  ]

  const isWhiteList = whiteList.some(path => matchWhiteList(path, route.path))
  if (isWhiteList) {
    return
  }

  if (process.server) {
    return
  }

  try {
    const userStore = useUserStore(app.pinia)
    userStore.restorePermissions()

    const sessionState = await userStore.syncSessionFromServer({
      forceReloadPermissions: !userStore.permissions || userStore.permissions.length === 0
    })

    if (!sessionState?.user) {
      return redirect('/login')
    }

    const userPermissions = sessionState.permissions || []
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
    clearAuthState()
    return redirect('/login')
  }
}
