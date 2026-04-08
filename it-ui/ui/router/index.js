import Vue from 'vue'
import Router from 'vue-router'

export const constantRoutes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/pages/Z_loginpage/logpage.vue'),
    hidden: true
  },
  {
    path: '/',
    name: 'home',
    component: () => import('@/pages/Z_webhomepage/webhome.vue'),
    hidden: true
  },
  {
    path: '/noPermission',
    name: 'noPermission',
    component: () => import('@/pages/S_nopermission/noPermission.vue'),
    hidden: true
  }
]

Vue.use(Router)

export default new Router({
  mode: 'history',
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/pages/Z_webhomepage/webhome.vue'),
      meta: { permissions: ['view:webhome'] }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/Z_loginpage/logpage.vue'),
      meta: { permissions: ['view:login'] }
    },
    {
      path: '/user',
      name: 'user',
      component: () => import('@/pages/Z_userpage/peoplehome.vue'),
      meta: { permissions: ['view:profile'] }
    },
    {
      path: '/other/:id',
      name: 'other',
      component: () => import('@/pages/Z_userpage/ohter.vue'),
      meta: { permissions: ['view:profile'] }
    },
    {
      path: '/collection',
      name: 'collection',
      component: () => import('@/pages/Z_collectionpage/collectionpage.vue'),
      meta: { permissions: ['view:collection'] }
    },
    {
      path: '/blog',
      name: 'blog',
      component: () => import('@/pages/Z_blogpage/blogpage.vue'),
      meta: { permissions: ['view:blog'] }
    },
    {
      path: '/blog/:id',
      name: 'blogdetail',
      component: () => import('@/pages/Z_blogdetail/blogdetail.vue'),
      meta: { permissions: ['view:blog'] }
    },
    {
      path: '/blogwrite',
      name: 'blogwrite',
      component: () => import('@/pages/Z_blogwrite/blogwritepage.vue'),
      meta: { permissions: ['view:blog'] }
    },
    {
      path: '/circle/:id',
      name: 'circledetail',
      component: () => import('@/pages/Z_circledetail/circledetail.vue'),
      meta: { permissions: ['view:circle'] }
    },
    {
      path: '/circle',
      name: 'circle',
      component: () => import('@/pages/Z_circlepage/circlehome.vue'),
      meta: { permissions: ['view:circle'] }
    },
    {
      path: '/audit',
      name: 'audit',
      component: () => import('@/pages/f_blogmanage/audit/audit.vue'),
      meta: { permissions: ['view:admin:blog-audit'] }
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/pages/f_blogmanage/dashboard/dashboard.vue'),
      meta: { permissions: ['view:admin:dashboard'] }
    },
    {
      path: '/circlemanage',
      name: 'circlemanage',
      component: () => import('@/pages/f_circlemanage/circlemanage/circlemanage.vue'),
      meta: { permissions: ['view:admin:circle-manage'] }
    },
    {
      path: '/circleaudit',
      name: 'circleaudit',
      component: () => import('@/pages/f_circlemanage/circleaudit/circleaudit.vue'),
      meta: { permissions: ['view:admin:circle-audit'] }
    },
    {
      path: '/label',
      name: 'label',
      component: () => import('@/pages/f_systemmanage/label/label.vue'),
      meta: { permissions: ['view:admin:label-manage'] }
    },
    {
      path: '/log',
      name: 'log',
      component: () => import('@/pages/f_systemmanage/log/log.vue'),
      meta: { permissions: ['view:admin:system-log'] }
    },
    {
      path: '/menu',
      name: 'menu',
      component: () => import('@/pages/f_systemmanage/menu/menu.vue'),
      meta: { permissions: ['view:menu'] }
    },
    {
      path: '/homepage',
      name: 'homepage',
      component: () => import('@/pages/f_homepage/homepage.vue'),
      meta: { permissions: ['view:homepage'] }
    },
    {
      path: '/info',
      name: 'info',
      component: () => import('@/pages/f_systemmanage/usermanage/info/info.vue'),
      meta: { permissions: ['view:admin:user-info'] }
    },
    {
      path: '/permission',
      name: 'permission',
      component: () => import('@/pages/f_systemmanage/permission/permission.vue'),
      meta: { permissions: ['view:permission'] }
    },
    {
      path: '/role',
      name: 'role',
      component: () => import('@/pages/f_systemmanage/role/role.vue'),
      meta: { permissions: ['view:admin:user-role'] }
    },
    {
      path: '/count',
      name: 'count',
      component: () => import('@/pages/f_systemmanage/usermanage/count/count.vue'),
      meta: { permissions: ['view:admin:user-count'] }
    },
    {
      path: '/registe',
      name: 'registe',
      component: () => import('@/pages/Z_registepage/registepage.vue'),
      meta: { permissions: ['view:registe'] }
    },
    {
      path: '/history',
      name: 'history',
      component: () => import('@/pages/Z_historypage/historypage.vue'),
      meta: { permissions: ['view:profile'] }
    },
    {
      path: '/projectlist',
      name: 'projectlist',
      component: () => import('@/pages/f_project/list/list.vue'),
      meta: { permissions: ['view:project'] }
    },
    {
      path: '/projectdetail',
      name: 'projectdetail',
      component: () => import('@/pages/f_project/projectdetail/projectdetail.vue'),
      meta: { permissions: ['view:project-detail'] }
    },
    {
      path: '/myproject',
      name: 'myproject',
      component: () => import('@/pages/f_project/myproject/myproject.vue'),
      meta: { permissions: ['view:myproject'] }
    },
    {
      path: '/projectcollection',
      name: 'projectcollection',
      component: () => import('@/pages/f_project/projectcollection/projectcollection.vue'),
      meta: { permissions: ['view:project-collection'] }
    },
    {
      path: '/projectmanage',
      name: 'projectmanage',
      component: () => import('@/pages/f_project/projectmanage/projectmanage.vue'),
      meta: { permissions: ['view:project-manage'] }
    },
    {
      path: '/knowledge-base',
      name: 'knowledgeBase',
      component: () => import('@/pages/ai/KnowledgeBase.vue'),
      meta: { permissions: ['view:knowledge-base'] }
    },
    {
      path: '/ai/models',
      name: 'aiModelAdmin',
      component: () => import('@/pages/ai/ModelAdmin.vue'),
      meta: { permissions: ['view:ai:model-admin'] }
    },
    {
      path: '/ai/prompts',
      name: 'aiPromptTemplate',
      component: () => import('@/pages/ai/PromptTemplate.vue'),
      meta: { permissions: ['view:ai:prompt-template'] }
    },
    {
      path: '/ai/logs',
      name: 'aiLogs',
      component: () => import('@/pages/ai/AiLog.vue'),
      meta: { permissions: ['view:ai:log'] }
    },
    {
      path: '/vip',
      name: 'vip',
      component: () => import('@/pages/Z_vip/vip.vue')
    },
    {
      path: '/wallet',
      name: 'wallet',
      component: () => import('@/pages/Z_wallet/wallet.vue')
    },
    {
      path: '/orders_purchases',
      name: 'orders_purchases',
      component: () => import('@/pages/Z_userpage/orders_purchases.vue')
    },
    {
      path: '/payment',
      name: 'payment',
      component: () => import('@/pages/Z_payment/payment.vue')
    },
    {
      path: '/order',
      name: 'order',
      component: () => import('@/pages/f_systemmanage/order/order.vue')
    },
    {
      path: '/membership',
      name: 'membership',
      component: () => import('@/pages/f_systemmanage/membership/membership.vue')
    },
    {
      path: '/coupons',
      name: 'coupons',
      component: () => import('@/pages/Z_userpage/coupons.vue')
    },
    {
      path: '/couponmanage',
      name: 'couponmanage',
      component: () => import('@/pages/f_systemmanage/coupon/couponmanage.vue')
    },
  ]
})
