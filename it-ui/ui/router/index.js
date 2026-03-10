import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

export default new Router({
  mode: 'history',
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/pages/Z_webhomepage/webhome.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/Z_loginpage/logpage.vue')
    },
    {
      path: '/user',
      name: 'user',
      component: () => import('@/pages/Z_userpage/peoplehome.vue')
    },
    {
      path: '/algoreco',
      name: 'algoreco',
      component: () => import('@/pages/f_blogmanage/algoreco/algoreco.vue')
    },
    {
      path: '/audit',
      name: 'audit',
      component: () => import('@/pages/f_blogmanage/audit/audit.vue')
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/pages/f_blogmanage/dashboard/dashboard.vue')
    },
    {
      path: '/circlemanage',
      name: 'circlemanage',
      component: () => import('@/pages/f_circlemanage/circlemanage/circlemanage.vue')
    },
    {
      path: '/circlesort',
      name: 'circlesort',
      component: () => import('@/pages/f_circlemanage/circlesort/circlesort.vue')
    },
    {
      path: '/circlefriend',
      name: 'circlefriend',
      component: () => import('@/pages/f_circlemanage/friend/friend.vue')
    },
    {
      path: '/official',
      name: 'circleofficial',
      component: () => import('@/pages/f_circlemanage/official/official.vue')
    },
    {
      path: '/circleaudit',
      name: 'circleaudit',
      component: () => import('@/pages/f_circlemanage/circleaudit/circleaudit.vue')
    },
    {
      path: '/projectalgoreco',
      name: 'projectalgoreco',
      component: () => import('@/pages/f_projectmanage/algoreco/algoreco.vue')
    },
    {
      path: '/projectaudit',
      name: 'projectaudit',
      component: () => import('@/pages/f_projectmanage/projectaudit/projectaudit.vue')
    },
    {
      path: '/projectmiss',
      name: 'projectmiss',
      component: () => import('@/pages/f_projectmanage/projectmiss/projectmiss.vue')
    },
    {
      path: '/label',
      name: 'label',
      component: () => import('@/pages/f_systemmanage/label/label.vue')
    }, 
    {
      path: '/log',
      name: 'log',
      component: () => import('@/pages/f_systemmanage/log/log.vue')
    },
    {
      path: '/menu',
      name: 'menu',
      component: () => import('@/pages/f_systemmanage/menu/menu.vue')
    },
    {
      path: '/usermanage',
      name: 'usermanage',
      component: () => import('@/pages/f_systemmanage/usermanage/usermanage.vue')
    },
    {
      path: '/history',
      name: 'history',
      component: () => import('@/pages/Z_historypage/historypage.vue')
    },
    {
      path: '/homepage',
      name: 'homepage',
      component: () => import('@/pages/f_homepage/homepage.vue')
    },
    {
      path: '/registe',
      name: 'registe',
      component: () => import('@/pages/Z_registepage/registepage.vue')
    }
  ]
})