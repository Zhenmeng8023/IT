import { routeSource } from '@/router/route-source'

const EXTRA_ROUTE_ACCESS = [
  {
    path: '/hybridaction/*',
    public: true,
    requiresAuth: false,
    permissions: []
  }
]

function normalizeRouteAccess(route) {
  const permissions = normalizePermissionList(route.permissions)
  const isPublic = route.public === true

  return {
    path: route.path,
    public: isPublic,
    requiresAuth: isPublic ? false : route.requiresAuth !== false,
    permissions,
    hidden: route.hidden === true
  }
}

function escapeRegExp(value) {
  return value.replace(/[|\\{}()[\]^$+?.]/g, '\\$&')
}

function buildPermissionVariants(requiredPermission) {
  const permission = String(requiredPermission || '').trim()
  if (!permission) {
    return []
  }

  const parts = permission.split(':').filter(Boolean)
  const variants = [permission]

  for (let index = parts.length; index > 0; index -= 1) {
    variants.push(`${parts.slice(0, index).join(':')}:*`)
  }

  return [...new Set(variants)]
}

export function normalizePermissionList(permissions) {
  const source = Array.isArray(permissions) ? permissions : [permissions]
  const normalized = []

  source.forEach((item) => {
    if (typeof item === 'string') {
      const trimmed = item.trim()
      if (trimmed) {
        normalized.push(trimmed)
      }
      return
    }

    if (item && typeof item.permissionCode === 'string') {
      const trimmed = item.permissionCode.trim()
      if (trimmed) {
        normalized.push(trimmed)
      }
    }
  })

  return [...new Set(normalized)]
}

export function matchRoutePattern(pattern, routePath) {
  if (!pattern || !routePath) {
    return false
  }

  if (pattern === routePath) {
    return true
  }

  const regexSource = pattern
    .split('/')
    .map((segment) => {
      if (!segment) {
        return ''
      }
      if (segment === '*') {
        return '.*'
      }
      if (segment.startsWith(':')) {
        return '[^/]+'
      }
      return escapeRegExp(segment)
    })
    .join('/')

  return new RegExp(`^${regexSource}$`).test(routePath)
}

export const routeAccessList = [
  ...routeSource.map(normalizeRouteAccess),
  ...EXTRA_ROUTE_ACCESS.map(normalizeRouteAccess)
]

export const routeAccessMap = routeAccessList.reduce((map, route) => {
  map[route.path] = {
    public: route.public,
    requiresAuth: route.requiresAuth,
    permissions: [...route.permissions],
    hidden: route.hidden
  }
  return map
}, {})

export const routePermissions = routeAccessList.reduce((map, route) => {
  if (route.permissions.length > 0) {
    map[route.path] = route.permissions[0]
  }
  return map
}, {})

export function getRouteAccessConfig(routePath) {
  const matchedRoute = routeAccessList.find(route => matchRoutePattern(route.path, routePath))

  if (!matchedRoute) {
    return null
  }

  return {
    ...matchedRoute,
    permissions: [...matchedRoute.permissions]
  }
}

export function getRoutePermissions(routePath) {
  const routeConfig = getRouteAccessConfig(routePath)
  return routeConfig ? routeConfig.permissions : []
}

export function getRoutePermission(routePath) {
  const permissions = getRoutePermissions(routePath)
  return permissions[0] || null
}

export function isPublicRoute(routePath) {
  const routeConfig = getRouteAccessConfig(routePath)
  return routeConfig ? !routeConfig.requiresAuth : false
}

export function hasPermission(userPermissions, requiredPermission) {
  const normalizedUserPermissions = normalizePermissionList(userPermissions)
  const variants = buildPermissionVariants(requiredPermission)

  if (!variants.length || normalizedUserPermissions.length === 0) {
    return false
  }

  return variants.some(permission => normalizedUserPermissions.includes(permission))
}

export function hasAnyPermission(userPermissions, requiredPermissions) {
  const permissions = normalizePermissionList(requiredPermissions)

  if (permissions.length === 0) {
    return true
  }

  return permissions.some(permission => hasPermission(userPermissions, permission))
}

export function hasAllPermissions(userPermissions, requiredPermissions) {
  const permissions = normalizePermissionList(requiredPermissions)

  if (permissions.length === 0) {
    return true
  }

  return permissions.every(permission => hasPermission(userPermissions, permission))
}
