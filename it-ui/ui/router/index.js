import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

export default new Router({
  mode: 'history',
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/pages/webhomepage/webhome.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/loginpage/logpage.vue')
    },
    {
      path: '/user',
      name: 'user',
      component: () => import('@/pages/userpage/peoplehome.vue')
    },
    {
      path: '/manage',
      name: 'manage',
      component: () => import('@/pages/f_systemmanage/manage.vue')
    }
  ]
})