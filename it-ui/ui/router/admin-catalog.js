export const adminRouteCatalog = Object.freeze([
  {
    path: '/admin/home',
    component: 'pages/f_homepage/homepage.vue',
    permission: 'view:admin:home',
    title: '首页',
    name: 'admin_home',
    legacyPaths: ['/homepage']
  },
  {
    path: '/admin/dashboard',
    component: 'pages/f_blogmanage/dashboard/dashboard.vue',
    permission: 'view:admin:dashboard',
    title: '仪表盘',
    name: 'admin_dashboard',
    legacyPaths: ['/dashboard']
  },
  {
    path: '/admin/users/info',
    component: 'pages/f_systemmanage/usermanage/info/info.vue',
    permission: 'view:admin:user:info',
    title: '用户信息',
    name: 'admin_users_info',
    legacyPaths: ['/info']
  },
  {
    path: '/admin/users/account',
    component: 'pages/f_systemmanage/usermanage/count/count.vue',
    permission: 'view:admin:user:account',
    title: '账户管理',
    name: 'admin_users_account',
    legacyPaths: ['/count']
  },
  {
    path: '/admin/rbac/role',
    component: 'pages/f_systemmanage/role/role.vue',
    permission: 'view:admin:rbac:role',
    title: '角色管理',
    name: 'admin_rbac_role',
    legacyPaths: ['/role']
  },
  {
    path: '/admin/rbac/menu',
    component: 'pages/f_systemmanage/menu/menu.vue',
    permission: 'view:admin:rbac:menu',
    title: '菜单管理',
    name: 'admin_rbac_menu',
    legacyPaths: ['/menu']
  },
  {
    path: '/admin/rbac/permission',
    component: 'pages/f_systemmanage/permission/permission.vue',
    permission: 'view:admin:rbac:permission',
    title: '权限管理',
    name: 'admin_rbac_permission',
    legacyPaths: ['/permission']
  },
  {
    path: '/admin/system/log',
    component: 'pages/f_systemmanage/log/log.vue',
    permission: 'view:admin:system:log',
    title: '系统日志',
    name: 'admin_system_log',
    legacyPaths: ['/log']
  },
  {
    path: '/admin/system/notification',
    component: 'pages/f_systemmanage/notification/notification.vue',
    permission: 'view:admin:system:notification',
    title: '通知管理',
    name: 'admin_system_notification',
    legacyPaths: ['/notificationmanage']
  },
  {
    path: '/admin/finance/order',
    component: 'pages/f_systemmanage/order/order.vue',
    permission: 'view:admin:finance:order',
    title: '订单管理',
    name: 'admin_finance_order',
    legacyPaths: ['/order']
  },
  {
    path: '/admin/finance/membership',
    component: 'pages/f_systemmanage/membership/membership.vue',
    permission: 'view:admin:finance:membership',
    title: '会员管理',
    name: 'admin_finance_membership',
    legacyPaths: ['/membership']
  },
  {
    path: '/admin/finance/coupon',
    component: 'pages/f_systemmanage/coupon/couponmanage.vue',
    permission: 'view:admin:finance:coupon',
    title: '优惠券管理',
    name: 'admin_finance_coupon',
    legacyPaths: ['/couponmanage']
  },
  {
    path: '/admin/finance/withdraw',
    component: 'pages/f_systemmanage/withdraw/withdraw.vue',
    permission: 'view:admin:finance:withdraw',
    title: '提现管理',
    name: 'admin_finance_withdraw',
    legacyPaths: ['/withdraw']
  },
  {
    path: '/admin/content/blog/audit',
    component: 'pages/f_blogmanage/audit/audit.vue',
    permission: 'view:admin:blog:audit',
    title: '博客审核',
    name: 'admin_content_blog_audit',
    legacyPaths: ['/audit']
  },
  {
    path: '/admin/content/blog/recommend',
    component: 'pages/f_blogmanage/algoreco/algoreco.vue',
    permission: 'view:admin:blog:recommend',
    title: '博客推荐',
    name: 'admin_content_blog_recommend',
    legacyPaths: ['/algoreco']
  },
  {
    path: '/admin/content/tag',
    component: 'pages/f_systemmanage/label/label.vue',
    permission: 'view:admin:content:tag',
    title: '标签管理',
    name: 'admin_content_tag',
    legacyPaths: ['/label']
  },
  {
    path: '/admin/content/circle/manage',
    component: 'pages/f_circlemanage/circlemanage/circlemanage.vue',
    permission: 'view:admin:circle:manage',
    title: '圈子管理',
    name: 'admin_content_circle_manage',
    legacyPaths: ['/circlemanage']
  },
  {
    path: '/admin/content/circle/audit',
    component: 'pages/f_circlemanage/circleaudit/circleaudit.vue',
    permission: 'view:admin:circle:audit',
    title: '圈子审核',
    name: 'admin_content_circle_audit',
    legacyPaths: ['/circleaudit']
  },
  {
    path: '/admin/project/audit',
    component: 'pages/f_projectmanage/projectaudit/projectaudit.vue',
    permission: 'view:admin:project:audit',
    title: '项目审核',
    name: 'admin_project_audit',
    legacyPaths: ['/projectaudit']
  },
  {
    path: '/admin/project/offline',
    component: 'pages/f_projectmanage/projectmiss/projectmiss.vue',
    permission: 'view:admin:project:offline',
    title: '项目下架',
    name: 'admin_project_offline',
    legacyPaths: ['/projectmiss']
  },
  {
    path: '/admin/project/recommend',
    component: 'pages/f_projectmanage/algoreco/algoreco.vue',
    permission: 'view:admin:project:recommend',
    title: '项目推荐',
    name: 'admin_project_recommend',
    legacyPaths: ['/projectalgoreco']
  },
  {
    path: '/admin/ai/knowledge-base',
    component: 'pages/ai/AdminKnowledgeGovernance.vue',
    permission: 'view:admin:ai:knowledge',
    title: '知识库治理',
    name: 'admin_ai_knowledge_governance',
    legacyPaths: ['/knowledge-base']
  },
  {
    path: '/admin/ai/knowledge-usage',
    component: 'pages/ai/AdminKnowledgeUsage.vue',
    permission: 'view:admin:ai:knowledge',
    title: '用户知识库使用管理',
    name: 'admin_ai_knowledge_usage'
  },
  {
    path: '/admin/ai/models',
    component: 'pages/ai/ModelAdmin.vue',
    permission: 'view:admin:ai:model',
    title: '模型管理',
    name: 'admin_ai_models',
    legacyPaths: ['/ai/models']
  },
  {
    path: '/admin/ai/prompts',
    component: 'pages/ai/PromptTemplate.vue',
    permission: 'view:admin:ai:prompt',
    title: '提示词模板',
    name: 'admin_ai_prompts',
    legacyPaths: ['/ai/prompts']
  },
  {
    path: '/admin/ai/logs',
    component: 'pages/ai/AiLog.vue',
    permission: 'view:admin:ai:log',
    title: 'AI 日志',
    name: 'admin_ai_logs',
    legacyPaths: ['/ai/logs']
  }
])

export const adminMenuGroups = Object.freeze([
  { id: 200, path: '/admin', title: '后台首页', icon: 'el-icon-s-home', sortOrder: 10 },
  { id: 210, path: '/admin/users', title: '用户管理', icon: 'el-icon-user', sortOrder: 20 },
  { id: 220, path: '/admin/rbac', title: '权限管理', icon: 'el-icon-lock', sortOrder: 30 },
  { id: 230, path: '/admin/content', title: '内容管理', icon: 'el-icon-notebook-2', sortOrder: 40 },
  { id: 240, path: '/admin/project', title: '项目管理', icon: 'el-icon-folder-opened', sortOrder: 50 },
  { id: 250, path: '/admin/finance', title: '财务管理', icon: 'el-icon-money', sortOrder: 60 },
  { id: 260, path: '/admin/ai', title: 'AI 管理', icon: 'el-icon-cpu', sortOrder: 70 },
  { id: 270, path: '/admin/system', title: '系统管理', icon: 'el-icon-setting', sortOrder: 80 }
])

const groupChildren = Object.freeze({
  '/admin': ['/admin/home', '/admin/dashboard'],
  '/admin/users': ['/admin/users/info', '/admin/users/account'],
  '/admin/rbac': ['/admin/rbac/role', '/admin/rbac/menu', '/admin/rbac/permission'],
  '/admin/content': [
    '/admin/content/blog/audit',
    '/admin/content/blog/recommend',
    '/admin/content/tag',
    '/admin/content/circle/manage',
    '/admin/content/circle/audit'
  ],
  '/admin/project': ['/admin/project/audit', '/admin/project/offline', '/admin/project/recommend'],
  '/admin/finance': [
    '/admin/finance/order',
    '/admin/finance/membership',
    '/admin/finance/coupon',
    '/admin/finance/withdraw'
  ],
  '/admin/ai': ['/admin/ai/knowledge-base', '/admin/ai/knowledge-usage', '/admin/ai/models', '/admin/ai/prompts', '/admin/ai/logs'],
  '/admin/system': ['/admin/system/log', '/admin/system/notification']
})

export const adminRoutePathSet = new Set(adminRouteCatalog.map(route => route.path))

export const adminRouteMap = Object.freeze(adminRouteCatalog.reduce((map, route) => {
  map[route.path] = route
  return map
}, {}))

export const adminLegacyRedirectMap = Object.freeze(adminRouteCatalog.reduce((map, route) => {
  ;(route.legacyPaths || []).forEach((legacyPath) => {
    map[legacyPath] = route.path
  })
  return map
}, {}))

export const adminLegacyRedirects = Object.freeze(Object.keys(adminLegacyRedirectMap).map((legacyPath) => ({
  path: legacyPath,
  redirect: adminLegacyRedirectMap[legacyPath],
  target: adminRouteMap[adminLegacyRedirectMap[legacyPath]]
})))

export function normalizeAdminPath(path) {
  if (!path) {
    return ''
  }

  const normalized = String(path).trim()
  return adminLegacyRedirectMap[normalized] || normalized
}

export function isAdminCatalogPath(path) {
  return adminRoutePathSet.has(normalizeAdminPath(path))
}

export function getAdminRoute(path) {
  return adminRouteMap[normalizeAdminPath(path)] || null
}

export function getAdminRoutePermission(path) {
  const route = getAdminRoute(path)
  return route ? route.permission : null
}

export function getAdminRouteTitle(path) {
  const normalized = normalizeAdminPath(path)
  const route = adminRouteMap[normalized]

  if (route) {
    return route.title
  }

  const group = adminMenuGroups.find(item => item.path === normalized)
  return group ? group.title : ''
}

export function getAdminMenuPathMap() {
  const pathMap = {}

  adminMenuGroups.forEach((group) => {
    pathMap[group.path] = {
      title: group.title,
      name: group.path.replace(/[/:-]+/g, '_').replace(/^_+|_+$/g, '') || `admin_group_${group.id}`
    }
  })

  adminRouteCatalog.forEach((route) => {
    pathMap[route.path] = {
      title: route.title,
      name: route.name
    }
  })

  return pathMap
}

export function getAdminFallbackMenus() {
  const routesByPath = adminRouteMap

  return adminMenuGroups.map((group) => ({
    id: group.id,
    path: group.path,
    name: group.title,
    icon: group.icon,
    type: 'menu',
    sortOrder: group.sortOrder,
    children: (groupChildren[group.path] || []).map((path, index) => {
      const route = routesByPath[path]
      return {
        id: group.id + index + 1,
        path: route.path,
        name: route.title,
        icon: group.icon,
        type: 'menu',
        sortOrder: group.sortOrder + index + 1,
        permission: {
          permissionCode: route.permission
        }
      }
    })
  }))
}
