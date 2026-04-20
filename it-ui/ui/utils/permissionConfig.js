import { routeSource } from '@/router/route-source'

export const adminPermissionCodes = Object.freeze({
  homepage: 'view:homepage',
  dashboard: 'view:admin:dashboard',
  userInfo: 'view:admin:user-info',
  userCount: 'view:admin:user-count',
  userRole: 'view:admin:user-role',
  menuManage: 'view:menu',
  permissionManage: 'view:permission',
  systemLog: 'view:admin:system-log',
  notificationManage: 'view:notification',
  orderManage: 'view:admin:order-manage',
  membershipManage: 'view:admin:membership-manage',
  couponManage: 'view:admin:coupon-manage',
  withdrawManage: 'view:admin:withdraw-manage',
  blogAudit: 'view:admin:blog-audit',
  labelManage: 'view:admin:label-manage',
  circleManage: 'view:admin:circle-manage',
  circleAudit: 'view:admin:circle-audit',
  projectManage: 'view:project-manage',
  knowledgeBase: 'view:knowledge-base',
  aiModelAdmin: 'view:ai:model-admin',
  aiPromptTemplate: 'view:ai:prompt-template',
  aiLog: 'view:ai:log'
})

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

function createMenuItem(config) {
  const children = Array.isArray(config.children) ? config.children.map(createMenuItem) : []
  const menu = {
    id: config.id,
    path: config.path,
    name: config.name,
    icon: config.icon,
    type: config.type || 'menu'
  }

  if (config.permissionCode) {
    menu.permission = {
      permissionCode: config.permissionCode
    }
  }

  if (children.length > 0) {
    menu.children = children
  }

  return menu
}

function cloneMenuTree(menus = []) {
  return menus.map((menu) => ({
    ...menu,
    permission: menu.permission ? { ...menu.permission } : undefined,
    children: Array.isArray(menu.children) ? cloneMenuTree(menu.children) : []
  }))
}

function buildMenuPathMap(menus = [], pathMap = {}) {
  menus.forEach((menu) => {
    if (menu.path) {
      pathMap[menu.path] = {
        title: menu.name,
        name: String(menu.path).replace(/[/:-]+/g, '_').replace(/^_+|_+$/g, '') || `menu_${menu.id}`
      }
    }

    if (Array.isArray(menu.children) && menu.children.length > 0) {
      buildMenuPathMap(menu.children, pathMap)
    }
  })

  return pathMap
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

const knownRoutePathSet = new Set(routeAccessList.map(route => route.path))

export const routeAccessMap = routeAccessList.reduce((map, route) => {
  map[route.path] = {
    public: route.public,
    requiresAuth: route.requiresAuth,
    permissions: [...route.permissions],
    hidden: route.hidden
  }
  return map
}, {})

const adminFallbackMenuSource = [
  createMenuItem({
    id: 1,
    path: '/homepage',
    name: '首页',
    icon: 'el-icon-s-home',
    permissionCode: adminPermissionCodes.homepage
  }),
  createMenuItem({
    id: 2,
    path: '/dashboard',
    name: '仪表盘',
    icon: 'el-icon-s-home',
    permissionCode: adminPermissionCodes.dashboard
  }),
  createMenuItem({
    id: 10,
    path: '/usermanage',
    name: '用户管理',
    icon: 'el-icon-user',
    children: [
      {
        id: 11,
        path: '/info',
        name: '用户信息管理',
        icon: 'el-icon-user-solid',
        permissionCode: adminPermissionCodes.userInfo
      },
      {
        id: 12,
        path: '/count',
        name: '账户管理',
        icon: 'el-icon-s-finance',
        permissionCode: adminPermissionCodes.userCount
      }
    ]
  }),
  createMenuItem({
    id: 20,
    path: '/system',
    name: '系统管理',
    icon: 'el-icon-setting',
    children: [
      {
        id: 21,
        path: '/role',
        name: '角色管理',
        icon: 'el-icon-rank',
        permissionCode: adminPermissionCodes.userRole
      },
      {
        id: 22,
        path: '/menu',
        name: '菜单管理',
        icon: 'el-icon-menu',
        permissionCode: adminPermissionCodes.menuManage
      },
      {
        id: 23,
        path: '/permission',
        name: '权限管理',
        icon: 'el-icon-lock',
        permissionCode: adminPermissionCodes.permissionManage
      },
      {
        id: 24,
        path: '/log',
        name: '日志管理',
        icon: 'el-icon-document',
        permissionCode: adminPermissionCodes.systemLog
      },
      {
        id: 25,
        path: '/notificationmanage',
        name: '消息通知管理',
        icon: 'el-icon-message-solid',
        permissionCode: adminPermissionCodes.notificationManage
      },
      {
        id: 26,
        path: '/order',
        name: '订单管理',
        icon: 'el-icon-s-order',
        permissionCode: adminPermissionCodes.orderManage
      },
      {
        id: 27,
        path: '/membership',
        name: '会员管理',
        icon: 'el-icon-user-solid',
        permissionCode: adminPermissionCodes.membershipManage
      },
      {
        id: 28,
        path: '/couponmanage',
        name: '优惠券管理',
        icon: 'el-icon-s-ticket',
        permissionCode: adminPermissionCodes.couponManage
      },
      {
        id: 29,
        path: '/withdraw',
        name: '提现管理',
        icon: 'el-icon-s-finance',
        permissionCode: adminPermissionCodes.withdrawManage
      }
    ]
  }),
  createMenuItem({
    id: 30,
    path: '/blogmanage',
    name: '博客管理',
    icon: 'el-icon-edit',
    children: [
      {
        id: 31,
        path: '/audit',
        name: '博客审核',
        icon: 'el-icon-check',
        permissionCode: adminPermissionCodes.blogAudit
      },
      {
        id: 32,
        path: '/label',
        name: '标签管理',
        icon: 'el-icon-tag',
        permissionCode: adminPermissionCodes.labelManage
      }
    ]
  }),
  createMenuItem({
    id: 40,
    path: '/circle',
    name: '圈子管理',
    icon: 'el-icon-chat-dot-round',
    children: [
      {
        id: 41,
        path: '/circlemanage',
        name: '圈子管理',
        icon: 'el-icon-chat-dot-round',
        permissionCode: adminPermissionCodes.circleManage
      },
      {
        id: 42,
        path: '/circleaudit',
        name: '圈子审核',
        icon: 'el-icon-check',
        permissionCode: adminPermissionCodes.circleAudit
      }
    ]
  }),
  createMenuItem({
    id: 50,
    path: '/project',
    name: '项目审核中心',
    icon: 'el-icon-s-check',
    children: [
      {
        id: 51,
        path: '/projectaudit',
        name: '项目审核中心',
        icon: 'el-icon-s-claim',
        permissionCode: adminPermissionCodes.projectManage
      },
      {
        id: 52,
        path: '/projectmiss',
        name: '项目下架管理',
        icon: 'el-icon-remove-outline',
        permissionCode: adminPermissionCodes.projectManage
      }
    ]
  }),
  createMenuItem({
    id: 60,
    path: '/ai',
    name: 'AI 管理',
    icon: 'el-icon-menu',
    children: [
      {
        id: 61,
        path: '/knowledge-base',
        name: '知识库管理',
        icon: 'el-icon-document',
        permissionCode: adminPermissionCodes.knowledgeBase
      },
      {
        id: 62,
        path: '/ai/models',
        name: '模型管理',
        icon: 'el-icon-setting',
        permissionCode: adminPermissionCodes.aiModelAdmin
      },
      {
        id: 63,
        path: '/ai/prompts',
        name: '提示词模板',
        icon: 'el-icon-edit',
        permissionCode: adminPermissionCodes.aiPromptTemplate
      },
      {
        id: 64,
        path: '/ai/logs',
        name: 'AI 日志',
        icon: 'el-icon-document',
        permissionCode: adminPermissionCodes.aiLog
      }
    ]
  })
]

export const adminMenuPathMap = Object.freeze(buildMenuPathMap(adminFallbackMenuSource))

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

export function getAdminFallbackMenus() {
  return cloneMenuTree(adminFallbackMenuSource)
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
