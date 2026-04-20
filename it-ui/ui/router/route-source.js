import { adminLegacyRedirects, adminRouteCatalog } from './admin-catalog'

function defineRoute(config) {
  const permissions = Array.isArray(config.permissions) ? config.permissions.filter(Boolean) : []
  const isPublic = config.public === true

  return Object.freeze({
    path: config.path,
    component: config.component || null,
    redirect: config.redirect || null,
    public: isPublic,
    accessType: config.accessType || (config.path && config.path.startsWith('/admin') ? 'admin' : 'front'),
    requiresAuth: isPublic ? false : config.requiresAuth !== false,
    permissions,
    hidden: config.hidden === true
  })
}

function cloneRoute(route) {
  return {
    ...route,
    permissions: [...route.permissions]
  }
}

const frontRouteSource = [
  defineRoute({ path: '/', component: 'pages/Z_webhomepage/webhome.vue', public: true }),
  defineRoute({ path: '/login', component: 'pages/Z_loginpage/logpage.vue', public: true }),
  defineRoute({ path: '/registe', component: 'pages/Z_registepage/registepage.vue', public: true }),
  defineRoute({ path: '/noPermission', component: 'pages/S_nopermission/noPermission.vue', public: true, hidden: true }),

  defineRoute({ path: '/blog', component: 'pages/Z_blogpage/blogpage.vue', public: true }),
  defineRoute({ path: '/blog/:id', component: 'pages/Z_blogdetail/blogdetail.vue', public: true }),
  defineRoute({ path: '/circle', component: 'pages/Z_circlepage/circlehome.vue', public: true }),
  defineRoute({ path: '/circle/:id', component: 'pages/Z_circledetail/circledetail.vue', public: true }),
  defineRoute({ path: '/projectlist', component: 'pages/f_project/list/list.vue', public: true }),
  defineRoute({ path: '/projectdetail', component: 'pages/f_project/projectdetail/projectdetail.vue', public: true }),

  defineRoute({ path: '/user', component: 'pages/Z_userpage/peoplehome.vue', permissions: ['view:front:user:center'] }),
  defineRoute({ path: '/other/:id', component: 'pages/Z_userpage/ohter.vue', permissions: ['view:front:user:profile'] }),
  defineRoute({ path: '/history', component: 'pages/Z_historypage/historypage.vue', permissions: ['view:front:user:history'] }),
  defineRoute({ path: '/collection', component: 'pages/Z_collectionpage/collectionpage.vue', permissions: ['view:front:user:collection'] }),
  defineRoute({ path: '/notifications', component: 'pages/Z_notification/notification.vue', permissions: ['view:front:user:notification'] }),
  defineRoute({ path: '/notification', redirect: '/notifications', permissions: ['view:front:user:notification'], hidden: true }),
  defineRoute({ path: '/blogwrite', component: 'pages/Z_blogwrite/blogwritepage.vue', permissions: ['view:front:blog:write'] }),
  defineRoute({ path: '/myproject', component: 'pages/f_project/myproject/myproject.vue', permissions: ['view:front:project:mine'] }),
  defineRoute({ path: '/projecttemplates', component: 'pages/f_project/projecttemplates/index.vue', permissions: ['view:front:project:template'] }),
  defineRoute({ path: '/projectcollection', component: 'pages/f_project/projectcollection/projectcollection.vue', permissions: ['view:front:project:collection'] }),
  defineRoute({ path: '/projectmanage', component: 'pages/f_project/projectmanage/projectmanage.vue', permissions: ['view:front:project:manage'] }),
  defineRoute({ path: '/projectmergeconflict', component: 'pages/f_project/projectmergeconflict/projectmergeconflict.vue', permissions: ['view:front:project:manage'] }),
  defineRoute({ path: '/vip', component: 'pages/Z_vip/vip.vue', permissions: ['view:front:finance:vip'] }),
  defineRoute({ path: '/wallet', component: 'pages/Z_wallet/wallet.vue', permissions: ['view:front:finance:wallet'] }),
  defineRoute({ path: '/orders_purchases', component: 'pages/Z_userpage/orders_purchases.vue', permissions: ['view:front:finance:orders'] }),
  defineRoute({ path: '/payment', component: 'pages/Z_payment/payment.vue', permissions: ['view:front:finance:payment'] }),
  defineRoute({ path: '/coupons', component: 'pages/Z_userpage/coupons.vue', permissions: ['view:front:finance:coupons'] })
]

const adminRouteSource = [
  defineRoute({
    path: '/admin',
    component: 'pages/f_homepage/homepage.vue',
    accessType: 'admin',
    hidden: true
  }),
  ...adminRouteCatalog.map(route => defineRoute({
    path: route.path,
    component: route.component,
    accessType: 'admin',
    permissions: [route.permission]
  }))
]

const legacyAdminRedirectSource = adminLegacyRedirects.map(item => defineRoute({
  path: item.path,
  redirect: item.redirect,
  accessType: 'admin',
  permissions: item.target && item.target.permission ? [item.target.permission] : [],
  hidden: true
}))

export const routeSource = [
  ...frontRouteSource,
  ...adminRouteSource,
  ...legacyAdminRedirectSource
]

export function getRouteSource() {
  return routeSource.map(cloneRoute)
}

export function getPublicRouteSource() {
  return routeSource.filter(route => !route.requiresAuth).map(cloneRoute)
}

export function buildNuxtRoutes(resolvePage) {
  return routeSource.map((route) => {
    const record = {
      path: route.path,
      meta: {
        routeSource: 'nuxt.config.extendRoutes',
        requiresAuth: route.requiresAuth,
        accessType: route.accessType
      }
    }

    if (route.component) {
      record.component = resolvePage(route.component)
    }

    if (route.redirect) {
      record.redirect = route.redirect
    }

    if (route.permissions.length > 0) {
      record.meta.permissions = [...route.permissions]
    }

    if (route.hidden) {
      record.hidden = true
    }

    return record
  })
}
