import { useMenuStore } from '~/store/menu'
import { useUserStore } from '~/store/user'
import { clearAuthState } from '~/utils/auth'
import {
  getRouteAccessConfig,
  getRoutePermissions,
  hasAnyPermission,
  isAdminRoutePath,
  normalizePermissionList
} from '~/utils/permissionConfig'

function getMetaPermissions(route) {
  const metaList = Array.isArray(route.matched)
    ? route.matched.map(item => item.meta).filter(Boolean)
    : []

  return metaList.reduce((permissions, metaItem) => {
    if (metaItem && metaItem.permissions) {
      permissions.push(...normalizePermissionList(metaItem.permissions))
    }
    return permissions
  }, [])
}

function buildLoginRedirect(route) {
  const target = route?.fullPath || route?.path || '/'
  return `/login?redirect=${encodeURIComponent(target)}`
}

async function resolveRouteRedirect(app, route) {
  const routeAccess = getRouteAccessConfig(route.path)

  if (routeAccess && !routeAccess.requiresAuth) {
    return ''
  }

  try {
    const userStore = useUserStore(app.pinia)
    userStore.restorePermissions()

    const shouldReloadPermissions =
      !Array.isArray(userStore.permissions) || userStore.permissions.length === 0
    const sessionState = await userStore.syncSessionFromServer({
      forceReloadPermissions: shouldReloadPermissions
    })

    if (!sessionState?.user) {
      return buildLoginRedirect(route)
    }

    const userPermissions = normalizePermissionList(sessionState.permissions || userStore.permissions)

    if (route.path === '/admin') {
      const menuStore = useMenuStore(app.pinia)
      await menuStore.fetchMenus()
      return menuStore.getFirstAvailableAdminPath || '/noPermission'
    }

    const requiredPermissions = routeAccess ? routeAccess.permissions : getRoutePermissions(route.path)
    const fallbackMetaPermissions = requiredPermissions.length === 0 ? getMetaPermissions(route) : []
    const permissionsToCheck = requiredPermissions.length > 0 ? requiredPermissions : fallbackMetaPermissions

    if (isAdminRoutePath(route.path) && permissionsToCheck.length === 0) {
      return '/noPermission'
    }

    if (!hasAnyPermission(userPermissions, permissionsToCheck)) {
      return '/noPermission'
    }

    return ''
  } catch (error) {
    clearAuthState()
    return buildLoginRedirect(route)
  }
}

export default ({ app }) => {
  const router = app.router
  if (!router) {
    return
  }

  let pendingPath = ''

  router.beforeEach(async (to, from, next) => {
    const redirectPath = await resolveRouteRedirect(app, to)
    if (redirectPath && redirectPath !== to.fullPath) {
      next(redirectPath)
      return
    }
    next()
  })

  router.onReady(async () => {
    const currentRoute = router.currentRoute
    if (!currentRoute || pendingPath === currentRoute.fullPath) {
      return
    }

    const redirectPath = await resolveRouteRedirect(app, currentRoute)
    if (redirectPath && redirectPath !== currentRoute.fullPath) {
      pendingPath = currentRoute.fullPath
      router.replace(redirectPath).finally(() => {
        pendingPath = ''
      })
    }
  })
}
