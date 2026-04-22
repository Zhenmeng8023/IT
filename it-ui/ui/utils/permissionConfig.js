import { routeSource } from '@/router/route-source'
import {
  adminLegacyRedirectMap,
  adminMenuGroups,
  adminRouteCatalog,
  getAdminFallbackMenus as buildAdminFallbackMenus,
  getAdminMenuPathMap,
  getAdminRoute,
  getAdminRoutePermission,
  getAdminRouteTitle,
  isAdminCatalogPath,
  normalizeAdminPath
} from '@/router/admin-catalog'

export const adminPermissionCodes = Object.freeze({
  home: 'view:admin:home',
  dashboard: 'view:admin:dashboard',
  userInfo: 'view:admin:user:info',
  userAccount: 'view:admin:user:account',
  rbacRole: 'view:admin:rbac:role',
  rbacMenu: 'view:admin:rbac:menu',
  rbacPermission: 'view:admin:rbac:permission',
  systemLog: 'view:admin:system:log',
  systemNotification: 'view:admin:system:notification',
  financeOrder: 'view:admin:finance:order',
  financeMembership: 'view:admin:finance:membership',
  financeCoupon: 'view:admin:finance:coupon',
  financeWithdraw: 'view:admin:finance:withdraw',
  blogAudit: 'view:admin:blog:audit',
  blogRecommend: 'view:admin:blog:recommend',
  contentTag: 'view:admin:content:tag',
  circleManage: 'view:admin:circle:manage',
  circleAudit: 'view:admin:circle:audit',
  projectAudit: 'view:admin:project:audit',
  projectOffline: 'view:admin:project:offline',
  projectRecommend: 'view:admin:project:recommend',
  aiKnowledge: 'view:admin:ai:knowledge',
  aiKnowledgeGovernance: 'view:admin:ai:knowledge',
  aiKnowledgeUsage: 'view:admin:ai:knowledge',
  aiModel: 'view:admin:ai:model',
  aiPrompt: 'view:admin:ai:prompt',
  aiLog: 'view:admin:ai:log',
  frontAiAssistant: 'view:front:ai:assistant',
  frontAiKnowledgeSelf: 'view:front:ai:kb:self',
  frontAiKnowledgeSelfEdit: 'edit:front:ai:kb:self',
  frontAiKnowledgeProject: 'view:front:ai:kb:project',
  frontAiKnowledgeProjectEdit: 'edit:front:ai:kb:project',
  frontAiKnowledgeMember: 'manage:front:ai:kb:member'
})

const permissionAliases = Object.freeze({
  'view:admin:home': ['view:homepage'],
  'view:admin:user:info': ['view:admin:user-info'],
  'view:admin:user:account': ['view:admin:user-count'],
  'view:admin:rbac:role': ['view:admin:user-role'],
  'view:admin:rbac:menu': ['view:menu'],
  'view:admin:rbac:permission': ['view:permission'],
  'view:admin:system:log': ['view:admin:system-log'],
  'view:admin:system:notification': ['view:notification'],
  'view:admin:finance:order': ['view:admin:order-manage'],
  'view:admin:finance:membership': ['view:admin:membership-manage'],
  'view:admin:finance:coupon': ['view:admin:coupon-manage'],
  'view:admin:finance:withdraw': ['view:admin:withdraw-manage'],
  'view:admin:blog:audit': ['view:admin:blog-audit'],
  'view:admin:blog:recommend': ['view:admin:algor-reco'],
  'view:admin:content:tag': ['view:admin:label-manage'],
  'view:admin:circle:manage': ['view:admin:circle-manage'],
  'view:admin:circle:audit': ['view:admin:circle-audit'],
  'view:admin:project:audit': ['view:project-manage', 'view:admin:project-audit'],
  'view:admin:project:offline': ['view:project-manage', 'view:admin:official-manage'],
  'view:admin:project:recommend': ['view:project-manage', 'view:admin:system-log'],
  'view:admin:ai:knowledge': ['view:knowledge-base'],
  'view:admin:ai:model': ['view:ai:model-admin'],
  'view:admin:ai:prompt': ['view:ai:prompt-template'],
  'view:admin:ai:log': ['view:ai:log'],
  'view:front:ai:assistant': ['view:knowledge-base'],
  'view:front:ai:kb:self': ['view:knowledge-base'],
  'edit:front:ai:kb:self': ['view:knowledge-base', 'manage:knowledge-base'],
  'view:front:ai:kb:project': ['view:knowledge-base'],
  'edit:front:ai:kb:project': ['view:knowledge-base', 'manage:knowledge-base'],
  'manage:front:ai:kb:member': ['view:knowledge-base', 'manage:knowledge-base'],
  'view:front:user:center': ['view:profile'],
  'view:front:user:profile': ['view:profile'],
  'view:front:user:collection': ['view:collection'],
  'view:front:user:history': ['view:profile'],
  'view:front:user:notification': ['view:profile'],
  'view:front:blog:write': ['view:blog', 'view:writeblog'],
  'view:front:project:mine': ['view:myproject'],
  'view:front:project:template': ['view:myproject'],
  'view:front:project:collection': ['view:project-collection'],
  'view:front:project:manage': ['view:project-manage']
})

const EXTRA_ROUTE_ACCESS = [
  {
    path: '/hybridaction/*',
    public: true,
    requiresAuth: false,
    permissions: []
  }
]

const adminMenuGroupPathSet = new Set(adminMenuGroups.map(group => group.path))

function normalizeRouteAccess(route) {
  const permissions = normalizePermissionList(route.permissions)
  const isPublic = route.public === true

  return {
    path: route.path,
    public: isPublic,
    accessType: route.accessType || (route.path && route.path.startsWith('/admin') ? 'admin' : 'front'),
    requiresAuth: isPublic ? false : route.requiresAuth !== false,
    permissions,
    redirect: route.redirect || null,
    hidden: route.hidden === true
  }
}

function escapeRegExp(value) {
  return value.replace(/[|\\{}()[\]^$+?.]/g, '\\$&')
}

function addPermissionWithVariants(result, permission) {
  const cleanPermission = String(permission || '').trim()

  if (!cleanPermission) {
    return
  }

  const parts = cleanPermission.split(':').filter(Boolean)
  result.push(cleanPermission)

  for (let index = parts.length; index > 0; index -= 1) {
    result.push(`${parts.slice(0, index).join(':')}:*`)
  }
}

function buildPermissionVariants(requiredPermission) {
  const variants = []
  const permission = String(requiredPermission || '').trim()

  addPermissionWithVariants(variants, permission)
  ;(permissionAliases[permission] || []).forEach(alias => addPermissionWithVariants(variants, alias))

  return [...new Set(variants)]
}

function cloneMenuTree(menus = []) {
  return menus.map((menu) => ({
    ...menu,
    permission: menu.permission ? { ...menu.permission } : undefined,
    children: Array.isArray(menu.children) ? cloneMenuTree(menu.children) : []
  }))
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
      return
    }

    if (item && typeof item.permission_code === 'string') {
      const trimmed = item.permission_code.trim()
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

const knownRoutePathSet = new Set(routeAccessList.map(route => route.path))

export const routeAccessMap = routeAccessList.reduce((map, route) => {
  map[route.path] = {
    public: route.public,
    accessType: route.accessType,
    requiresAuth: route.requiresAuth,
    permissions: [...route.permissions],
    redirect: route.redirect,
    hidden: route.hidden
  }
  return map
}, {})

export const adminMenuPathMap = Object.freeze(getAdminMenuPathMap())

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

export function isKnownRoutePath(routePath) {
  return knownRoutePathSet.has(routePath)
}

export function isAdminRoutePath(routePath) {
  if (!routePath) {
    return false
  }

  return String(routePath).startsWith('/admin') || Boolean(adminLegacyRedirectMap[routePath])
}

export function isKnownAdminRoutePath(routePath) {
  return isAdminCatalogPath(routePath)
}

export function isKnownAdminMenuGroupPath(routePath) {
  return adminMenuGroupPathSet.has(normalizeAdminPath(routePath))
}

export function normalizeAdminMenuPath(routePath) {
  return normalizeAdminPath(routePath)
}

export function getAdminMenuRoute(routePath) {
  return getAdminRoute(routePath)
}

export function getAdminMenuRoutePermission(routePath) {
  return getAdminRoutePermission(routePath)
}

export function getAdminMenuRouteTitle(routePath) {
  return getAdminRouteTitle(routePath)
}

export function getAdminFallbackMenus() {
  return cloneMenuTree(buildAdminFallbackMenus())
}

export function getFirstAccessibleAdminPath(userPermissions = []) {
  const normalizedPermissions = normalizePermissionList(userPermissions)
  const route = adminRouteCatalog.find(item => hasPermission(normalizedPermissions, item.permission))
  return route ? route.path : ''
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
