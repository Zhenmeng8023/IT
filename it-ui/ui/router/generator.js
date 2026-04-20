import { getRouteSource } from './route-source'
import { hasAnyPermission, normalizePermissionList } from '@/utils/permissionConfig'

function canAccessRoute(route, userPermissions) {
  if (!route.requiresAuth) {
    return true
  }

  if (route.permissions.length === 0) {
    return true
  }

  return hasAnyPermission(userPermissions, route.permissions)
}

function toToolRoute(route) {
  return {
    path: route.path,
    component: route.component,
    redirect: route.redirect,
    accessType: route.accessType,
    requiresAuth: route.requiresAuth,
    permissions: [...route.permissions],
    hidden: route.hidden
  }
}

export function generateRoutes(userPermissions = []) {
  const normalizedPermissions = normalizePermissionList(userPermissions)

  return getRouteSource()
    .filter(route => canAccessRoute(route, normalizedPermissions))
    .map(toToolRoute)
}

export function getAccessiblePaths(userPermissions = []) {
  return generateRoutes(userPermissions).map(route => route.path)
}

export function updateRoutes(userPermissions = []) {
  return generateRoutes(userPermissions)
}
