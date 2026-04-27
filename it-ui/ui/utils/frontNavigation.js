const FRONT_NAV_GROUPS = Object.freeze([
  {
    key: 'front-primary',
    asTopLevel: true,
    title: '',
    icon: '',
    items: Object.freeze([
      { index: '/', icon: 'el-icon-s-home', title: '首页' },
      { index: '/blog', icon: 'el-icon-document', title: '博客' },
      { index: '/circle', icon: 'el-icon-chat-dot-round', title: '圈子' }
    ])
  },
  {
    key: 'front-project',
    title: '项目',
    icon: 'el-icon-folder-opened',
    items: Object.freeze([
      { index: '/projectlist', icon: 'el-icon-s-grid', title: '项目广场' },
      { index: '/myproject', icon: 'el-icon-notebook-2', title: '我的项目' },
      { index: '/projectcollection', icon: 'el-icon-star-on', title: '项目收藏' }
    ])
  },
  {
    key: 'front-personal',
    title: '个人',
    icon: 'el-icon-user',
    items: Object.freeze([
      { index: '/user', icon: 'el-icon-user-solid', title: '个人主页' },
      { index: '/notifications', icon: 'el-icon-bell', title: '通知中心' },
      { index: '/collection', icon: 'el-icon-collection', title: '内容收藏' },
      { index: '/history', icon: 'el-icon-time', title: '浏览历史' }
    ])
  },
  {
    key: 'front-asset',
    title: '资产',
    icon: 'el-icon-wallet',
    items: Object.freeze([
      { index: '/wallet', icon: 'el-icon-bank-card', title: '钱包' },
      { index: '/vip', icon: 'el-icon-s-opportunity', title: 'VIP' },
      { index: '/orders_purchases', icon: 'el-icon-tickets', title: '订单' },
      { index: '/coupons', icon: 'el-icon-s-ticket', title: '优惠券' }
    ])
  }
])

const FRONT_PROTECTED_ROUTE_PREFIXES = Object.freeze([
  '/myproject',
  '/projectcollection',
  '/projectmanage',
  '/projectmergeconflict',
  '/project-knowledge-base',
  '/user',
  '/peoplehome',
  '/other',
  '/notifications',
  '/notification',
  '/collection',
  '/history',
  '/personal-knowledge-base',
  '/wallet',
  '/vip',
  '/orders_purchases',
  '/payment',
  '/coupons'
])

function normalizePath(path) {
  return String(path || '').trim()
}

function normalizeRouteInput(routeOrPath) {
  if (routeOrPath && typeof routeOrPath === 'object') {
    const path = normalizePath(routeOrPath.path)
    const query = routeOrPath.query && typeof routeOrPath.query === 'object' ? routeOrPath.query : {}
    return { path, query }
  }
  return { path: normalizePath(routeOrPath), query: {} }
}

export function getFrontNavigationGroups() {
  return FRONT_NAV_GROUPS.map(group => ({
    ...group,
    items: group.items.map(item => ({ ...item }))
  }))
}

export function isFrontProtectedRoute(path) {
  const target = normalizePath(path)
  if (!target) {
    return false
  }
  return FRONT_PROTECTED_ROUTE_PREFIXES.some(prefix => target === prefix || target.startsWith(`${prefix}/`))
}

export function resolveFrontActiveMenu(routeOrPath) {
  const { path: target } = normalizeRouteInput(routeOrPath)
  if (!target || target === '/') return '/'

  if (target.startsWith('/blog')) return '/blog'
  if (target.startsWith('/circle')) return '/circle'
  if (target.startsWith('/projectlist') || target.startsWith('/projectdetail') || target.startsWith('/projecttemplates')) return '/projectlist'
  if (target.startsWith('/myproject') || target.startsWith('/projectmanage') || target.startsWith('/projectmergeconflict') || target.startsWith('/project-knowledge-base')) return '/myproject'
  if (target.startsWith('/projectcollection')) return '/projectcollection'
  if (target.startsWith('/user') || target.startsWith('/peoplehome') || target.startsWith('/other')) return '/user'
  if (target.startsWith('/notifications') || target.startsWith('/notification')) return '/notifications'
  if (target.startsWith('/collection')) return '/collection'
  if (target.startsWith('/history')) return '/history'
  if (target.startsWith('/personal-knowledge-base')) return '/user'
  if (target.startsWith('/wallet')) return '/wallet'
  if (target.startsWith('/vip')) return '/vip'
  if (target.startsWith('/orders_purchases') || target.startsWith('/payment')) return '/orders_purchases'
  if (target.startsWith('/coupons')) return '/coupons'

  return '/'
}
