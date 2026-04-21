const FRONT_PUBLIC_PATHS = Object.freeze([
  '/',
  '/blog',
  '/blog/1001',
  '/circle',
  '/circle/1001',
  '/projectlist',
  '/projectdetail'
])

const FRONT_AUTH_PATHS = Object.freeze([
  '/user',
  '/other/1001',
  '/collection',
  '/history',
  '/notifications',
  '/blogwrite',
  '/myproject',
  '/projecttemplates',
  '/projectcollection',
  '/projectmanage',
  '/wallet',
  '/vip',
  '/orders_purchases',
  '/payment',
  '/coupons'
])

const FRONT_PERMISSIONS = Object.freeze([
  'view:front:user:center',
  'view:front:user:profile',
  'view:front:user:collection',
  'view:front:user:history',
  'view:front:user:notification',
  'view:front:blog:write',
  'view:front:project:mine',
  'view:front:project:template',
  'view:front:project:collection',
  'view:front:project:manage',
  'view:front:finance:wallet',
  'view:front:finance:vip',
  'view:front:finance:orders',
  'view:front:finance:payment',
  'view:front:finance:coupons'
])

const ADMIN_ROUTES = Object.freeze([
  { path: '/admin/home', permission: 'view:admin:home', group: '/admin' },
  { path: '/admin/dashboard', permission: 'view:admin:dashboard', group: '/admin' },
  { path: '/admin/users/info', permission: 'view:admin:user:info', group: '/admin/users' },
  { path: '/admin/users/account', permission: 'view:admin:user:account', group: '/admin/users' },
  { path: '/admin/rbac/role', permission: 'view:admin:rbac:role', group: '/admin/rbac' },
  { path: '/admin/rbac/menu', permission: 'view:admin:rbac:menu', group: '/admin/rbac' },
  { path: '/admin/rbac/permission', permission: 'view:admin:rbac:permission', group: '/admin/rbac' },
  { path: '/admin/system/log', permission: 'view:admin:system:log', group: '/admin/system' },
  { path: '/admin/system/notification', permission: 'view:admin:system:notification', group: '/admin/system' },
  { path: '/admin/finance/order', permission: 'view:admin:finance:order', group: '/admin/finance' },
  { path: '/admin/finance/membership', permission: 'view:admin:finance:membership', group: '/admin/finance' },
  { path: '/admin/finance/coupon', permission: 'view:admin:finance:coupon', group: '/admin/finance' },
  { path: '/admin/finance/withdraw', permission: 'view:admin:finance:withdraw', group: '/admin/finance' },
  { path: '/admin/content/blog/audit', permission: 'view:admin:blog:audit', group: '/admin/content' },
  { path: '/admin/content/blog/recommend', permission: 'view:admin:blog:recommend', group: '/admin/content' },
  { path: '/admin/content/tag', permission: 'view:admin:content:tag', group: '/admin/content' },
  { path: '/admin/content/circle/manage', permission: 'view:admin:circle:manage', group: '/admin/content' },
  { path: '/admin/content/circle/audit', permission: 'view:admin:circle:audit', group: '/admin/content' },
  { path: '/admin/project/audit', permission: 'view:admin:project:audit', group: '/admin/project' },
  { path: '/admin/project/offline', permission: 'view:admin:project:offline', group: '/admin/project' },
  { path: '/admin/project/recommend', permission: 'view:admin:project:recommend', group: '/admin/project' },
  { path: '/admin/ai/knowledge-base', permission: 'view:admin:ai:knowledge', group: '/admin/ai' },
  { path: '/admin/ai/models', permission: 'view:admin:ai:model', group: '/admin/ai' },
  { path: '/admin/ai/prompts', permission: 'view:admin:ai:prompt', group: '/admin/ai' },
  { path: '/admin/ai/logs', permission: 'view:admin:ai:log', group: '/admin/ai' }
])

const ADMIN_GROUPS = Object.freeze([
  '/admin',
  '/admin/users',
  '/admin/rbac',
  '/admin/content',
  '/admin/project',
  '/admin/finance',
  '/admin/ai',
  '/admin/system'
])

const REVIEWER_ADMIN_PERMISSIONS = Object.freeze([
  'view:admin:home',
  'view:admin:dashboard',
  'view:admin:blog:audit',
  'view:admin:circle:audit',
  'view:admin:project:audit'
])

const ROLE_FIXTURES = Object.freeze({
  guest: null,
  user: {
    user: {
      id: 4001,
      username: 'e2e-user',
      nickname: 'E2E User',
      roleId: 4,
      role_id: 4
    },
    permissions: FRONT_PERMISSIONS
  },
  admin: {
    user: {
      id: 2001,
      username: 'e2e-admin',
      nickname: 'E2E Admin',
      roleId: 2,
      role_id: 2
    },
    permissions: [...FRONT_PERMISSIONS, ...ADMIN_ROUTES.map(route => route.permission)]
  },
  reviewer: {
    user: {
      id: 3001,
      username: 'e2e-reviewer',
      nickname: 'E2E Reviewer',
      roleId: 3,
      role_id: 3
    },
    permissions: REVIEWER_ADMIN_PERMISSIONS
  }
})

const LEGACY_ADMIN_REDIRECTS = Object.freeze([
  { legacyPath: '/audit', targetPath: '/admin/content/blog/audit' },
  { legacyPath: '/circleaudit', targetPath: '/admin/content/circle/audit' },
  { legacyPath: '/projectaudit', targetPath: '/admin/project/audit' }
])

function jsonResponse(route, payload, status = 200) {
  return route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(payload)
  })
}

function buildAdminMenuTree() {
  let id = 100
  const routesByGroup = ADMIN_ROUTES.reduce((map, route) => {
    if (!map[route.group]) {
      map[route.group] = []
    }
    map[route.group].push(route)
    return map
  }, {})

  const adminMenus = ADMIN_GROUPS.map((groupPath, groupIndex) => ({
    id: id++,
    parentId: 0,
    path: groupPath,
    name: `Admin Group ${groupIndex + 1}`,
    type: 'menu',
    sortOrder: (groupIndex + 1) * 10,
    children: (routesByGroup[groupPath] || []).map((route, routeIndex) => ({
      id: id++,
      parentId: groupPath,
      path: route.path,
      name: route.path,
      type: 'menu',
      sortOrder: (groupIndex + 1) * 10 + routeIndex + 1,
      permission: {
        permissionCode: route.permission
      }
    }))
  }))

  adminMenus.push({
    id: id++,
    parentId: 0,
    path: '/user',
    name: 'Front Leak Marker',
    type: 'menu',
    sortOrder: 999,
    permission: {
      permissionCode: 'view:front:user:center'
    },
    children: []
  })

  return adminMenus
}

async function installRbacApiMocks(page, role = 'guest') {
  const fixture = ROLE_FIXTURES[role] || null

  await page.route('http://localhost:18080/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname

    if (path === '/api/users/current') {
      if (!fixture) {
        return jsonResponse(route, { message: 'unauthenticated' }, 401)
      }
      return jsonResponse(route, fixture.user)
    }

    if (path === '/api/users/current/permissions') {
      if (!fixture) {
        return jsonResponse(route, { message: 'unauthenticated' }, 401)
      }
      return jsonResponse(route, fixture.permissions)
    }

    const rolePermissionsMatch = path.match(/^\/api\/roles\/(\d+)\/permissions$/)
    if (rolePermissionsMatch) {
      if (!fixture || String(fixture.user.roleId) !== rolePermissionsMatch[1]) {
        return jsonResponse(route, [])
      }
      return jsonResponse(route, fixture.permissions)
    }

    if (path === '/api/menus') {
      return jsonResponse(route, buildAdminMenuTree())
    }

    if (path === '/login') {
      return jsonResponse(route, { success: true, code: 200, message: 'ok' })
    }

    return jsonResponse(route, {
      success: true,
      code: 200,
      data: [],
      rows: [],
      list: [],
      records: []
    })
  })

  await page.addInitScript(({ user, permissions }) => {
    window.localStorage.clear()
    if (!user) {
      return
    }

    window.localStorage.setItem('serverSessionActive', 'server-session')
    window.localStorage.setItem('userInfo', JSON.stringify(user))
    window.localStorage.setItem('user', JSON.stringify(user))
    window.localStorage.setItem('userPermissions', JSON.stringify(permissions || []))
  }, fixture || {})
}

module.exports = {
  ADMIN_ROUTES,
  FRONT_AUTH_PATHS,
  FRONT_PUBLIC_PATHS,
  FRONT_PERMISSIONS,
  LEGACY_ADMIN_REDIRECTS,
  REVIEWER_ADMIN_PERMISSIONS,
  installRbacApiMocks
}
