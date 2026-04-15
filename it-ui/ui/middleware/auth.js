import { useUserStore } from '~/store/user'
import { clearAuthState } from '~/utils/auth'
import {
  getRouteAccessConfig,
  getRoutePermissions,
  hasAnyPermission,
  normalizePermissionList
} from '~/utils/permissionConfig'

function getMetaPermissions(route) {
  const metaList = Array.isArray(route.meta) ? route.meta : []

  return metaList.reduce((permissions, metaItem) => {
    if (metaItem && metaItem.permissions) {
      permissions.push(...normalizePermissionList(metaItem.permissions))
    }
    return permissions
  }, [])
}

export default async function ({ route, redirect, app }) {
  const routeAccess = getRouteAccessConfig(route.path)

  if (routeAccess && !routeAccess.requiresAuth) {
    return
  }

  const redirectToLogin = () => {
    const target = route?.fullPath || route?.path || '/'
    return redirect(`/login?redirect=${encodeURIComponent(target)}`)
  }

  if (process.server) {
    return
  }

  try {
    const userStore = useUserStore(app.pinia)
    userStore.restorePermissions()

    const shouldReloadPermissions = !Array.isArray(userStore.permissions) || userStore.permissions.length === 0
    const sessionState = await userStore.syncSessionFromServer({
      forceReloadPermissions: shouldReloadPermissions
    })

    if (!sessionState?.user) {
      return redirectToLogin()
    }

    const userPermissions = normalizePermissionList(sessionState.permissions || userStore.permissions)
    const requiredPermissions = routeAccess ? routeAccess.permissions : getRoutePermissions(route.path)
    const fallbackMetaPermissions = requiredPermissions.length === 0 ? getMetaPermissions(route) : []
    const permissionsToCheck = requiredPermissions.length > 0 ? requiredPermissions : fallbackMetaPermissions

    if (!hasAnyPermission(userPermissions, permissionsToCheck)) {
      if (app.$message) {
        app.$message.error('当前账号无权访问该页面')
      }
      return redirect('/noPermission')
    }
  } catch (error) {
    clearAuthState()
    return redirectToLogin()
  }
}
