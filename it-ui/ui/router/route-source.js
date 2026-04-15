function defineRoute(config) {
  const permissions = Array.isArray(config.permissions) ? config.permissions.filter(Boolean) : []
  const isPublic = config.public === true

  return Object.freeze({
    path: config.path,
    component: config.component,
    public: isPublic,
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

export const routeSource = [
  defineRoute({ path: '/', component: 'pages/Z_webhomepage/webhome.vue', public: true, permissions: ['view:webhome'] }),
  defineRoute({ path: '/login', component: 'pages/Z_loginpage/logpage.vue', public: true, permissions: ['view:login'] }),
  defineRoute({ path: '/user', component: 'pages/Z_userpage/peoplehome.vue', permissions: ['view:profile'] }),
  defineRoute({ path: '/other/:id', component: 'pages/Z_userpage/ohter.vue', permissions: ['view:profile'] }),
  defineRoute({ path: '/history', component: 'pages/Z_historypage/historypage.vue', permissions: ['view:profile'] }),
  defineRoute({ path: '/registe', component: 'pages/Z_registepage/registepage.vue', public: true, permissions: ['view:registe'] }),
  defineRoute({ path: '/collection', component: 'pages/Z_collectionpage/collectionpage.vue', permissions: ['view:collection'] }),
  defineRoute({ path: '/notifications', component: 'pages/Z_notification/notification.vue', permissions: ['view:profile'] }),
  defineRoute({ path: '/blog', component: 'pages/Z_blogpage/blogpage.vue', public: true, permissions: ['view:blog'] }),
  defineRoute({ path: '/blog/:id', component: 'pages/Z_blogdetail/blogdetail.vue', public: true, permissions: ['view:blog'] }),
  defineRoute({ path: '/blogwrite', component: 'pages/Z_blogwrite/blogwritepage.vue', permissions: ['view:blog'] }),
  defineRoute({ path: '/circle/:id', component: 'pages/Z_circledetail/circledetail.vue', public: true, permissions: ['view:circle'] }),
  defineRoute({ path: '/circle', component: 'pages/Z_circlepage/circlehome.vue', public: true, permissions: ['view:circle'] }),
  defineRoute({ path: '/algoreco', component: 'pages/f_blogmanage/algoreco/algoreco.vue', permissions: ['view:admin:algor-reco'] }),
  defineRoute({ path: '/audit', component: 'pages/f_blogmanage/audit/audit.vue', permissions: ['view:admin:blog-audit'] }),
  defineRoute({ path: '/dashboard', component: 'pages/f_blogmanage/dashboard/dashboard.vue', permissions: ['view:admin:dashboard'] }),
  defineRoute({ path: '/label', component: 'pages/f_systemmanage/label/label.vue', permissions: ['view:admin:label-manage'] }),
  defineRoute({ path: '/circlemanage', component: 'pages/f_circlemanage/circlemanage/circlemanage.vue', permissions: ['view:admin:circle-manage'] }),
  defineRoute({ path: '/circleaudit', component: 'pages/f_circlemanage/circleaudit/circleaudit.vue', permissions: ['view:admin:circle-audit'] }),
  defineRoute({ path: '/log', component: 'pages/f_systemmanage/log/log.vue', permissions: ['view:admin:system-log'] }),
  defineRoute({ path: '/notificationmanage', component: 'pages/f_systemmanage/notification/notification.vue', permissions: ['view:notification'] }),
  defineRoute({ path: '/menu', component: 'pages/f_systemmanage/menu/menu.vue', permissions: ['view:menu'] }),
  defineRoute({ path: '/count', component: 'pages/f_systemmanage/usermanage/count/count.vue', permissions: ['view:admin:user-count'] }),
  defineRoute({ path: '/info', component: 'pages/f_systemmanage/usermanage/info/info.vue', permissions: ['view:admin:user-info'] }),
  defineRoute({ path: '/projectaudit', component: 'pages/f_projectmanage/projectaudit/projectaudit.vue', permissions: ['view:project-manage'] }),
  defineRoute({ path: '/projectmiss', component: 'pages/f_projectmanage/projectmiss/projectmiss.vue', permissions: ['view:project-manage'] }),
  defineRoute({ path: '/homepage', component: 'pages/f_homepage/homepage.vue', permissions: ['view:homepage'] }),
  defineRoute({ path: '/permission', component: 'pages/f_systemmanage/permission/permission.vue', permissions: ['view:permission'] }),
  defineRoute({ path: '/role', component: 'pages/f_systemmanage/role/role.vue', permissions: ['view:admin:user-role'] }),
  defineRoute({ path: '/noPermission', component: 'pages/S_nopermission/noPermission.vue', public: true, hidden: true }),
  defineRoute({ path: '/projectlist', component: 'pages/f_project/list/list.vue', public: true, permissions: ['view:project'] }),
  defineRoute({ path: '/projectdetail', component: 'pages/f_project/projectdetail/projectdetail.vue', public: true, permissions: ['view:project-detail'] }),
  defineRoute({ path: '/myproject', component: 'pages/f_project/myproject/myproject.vue', permissions: ['view:myproject'] }),
  defineRoute({ path: '/projecttemplates', component: 'pages/f_project/projecttemplates/index.vue', permissions: ['view:myproject'] }),
  defineRoute({ path: '/projectcollection', component: 'pages/f_project/projectcollection/projectcollection.vue', permissions: ['view:project-collection'] }),
  defineRoute({ path: '/projectmanage', component: 'pages/f_project/projectmanage/projectmanage.vue', permissions: ['view:project-manage'] }),
  defineRoute({ path: '/projectalgoreco', component: 'pages/f_projectmanage/algoreco/algoreco.vue', permissions: ['view:project-manage'] }),
  defineRoute({ path: '/knowledge-base', component: 'pages/ai/KnowledgeBase.vue', permissions: ['view:knowledge-base'] }),
  defineRoute({ path: '/ai/models', component: 'pages/ai/ModelAdmin.vue', permissions: ['view:ai:model-admin'] }),
  defineRoute({ path: '/ai/prompts', component: 'pages/ai/PromptTemplate.vue', permissions: ['view:ai:prompt-template'] }),
  defineRoute({ path: '/ai/logs', component: 'pages/ai/AiLog.vue', permissions: ['view:ai:log'] }),
  defineRoute({ path: '/vip', component: 'pages/Z_vip/vip.vue' }),
  defineRoute({ path: '/wallet', component: 'pages/Z_wallet/wallet.vue' }),
  defineRoute({ path: '/orders_purchases', component: 'pages/Z_userpage/orders_purchases.vue' }),
  defineRoute({ path: '/payment', component: 'pages/Z_payment/payment.vue' }),
  defineRoute({ path: '/order', component: 'pages/f_systemmanage/order/order.vue' }),
  defineRoute({ path: '/membership', component: 'pages/f_systemmanage/membership/membership.vue' }),
  defineRoute({ path: '/coupons', component: 'pages/Z_userpage/coupons.vue' }),
  defineRoute({ path: '/couponmanage', component: 'pages/f_systemmanage/coupon/couponmanage.vue' }),
  defineRoute({ path: '/withdraw', component: 'pages/f_systemmanage/withdraw/withdraw.vue' })
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
      component: resolvePage(route.component),
      meta: {
        routeSource: 'nuxt.config.extendRoutes',
        requiresAuth: route.requiresAuth
      }
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
