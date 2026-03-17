// index.js
import Vue from 'vue'
import {Router, createRouter, createWebHistory} from 'vue-router'


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
  }
]



Vue.use(Router)

/**
 * 路由配置及页面权限映射表
 * 
 * 页面权限代码格式: view:<模块名>:<页面名>
 * 
 * 相关API权限可在权限代码文档中查找，例如：
 * - 用户管理页面 (user-manage) 需要 user:list, user:create, user:update, user:delete 等API权限
 */
export default new Router({
  mode: 'history',
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/pages/Z_webhomepage/webhome.vue'),
      meta: {
        permissions: ['view:home:page'] // 页面显示权限
      }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/Z_loginpage/logpage.vue'),
      meta: {
        permissions: ['view:auth:login-page']
      }
    },
    {
      path: '/user',
      name: 'user',
      component: () => import('@/pages/Z_userpage/peoplehome.vue'),
      meta: {
        permissions: ['view:user:profile'] // 需要 user:mine:read 等API权限来加载个人数据
      }
    },
    {
      path: '/history',
      name: 'history',
      component: () => import('@/pages/Z_historypage/historypage.vue'),
      meta: {
        permissions: ['view:history:view'] // 可能需要 blog:list-by-author 等API权限来获取历史记录
      }
    },
    {
      path: '/registe',
      name: 'registe',
      component: () => import('@/pages/Z_registepage/registepage.vue'),
      meta: {
        permissions: ['view:auth:register-page']
      }
    },
    {
      path: '/collection',
      name: 'collection',
      component: () => import('@/pages/Z_collectionpage/collectionpage.vue'),
      meta: {
        permissions: ['view:collection:page'] // 需要 blog:my-collections API权限
      }
    },
    {
      path: '/blog',
      name: 'blog',
      component: () => import('@/pages/Z_blogpage/blogpage.vue'),
      meta: {
        permissions: ['view:blog:list'] // 需要 blog:list, blog:search 等API权限
      }
    },
    {
      path: '/blog/:id',
      name: 'blogdetail',
      component: () => import('@/pages/Z_blogdetail/blogdetail.vue'),
      meta: {
        permissions: ['view:blog:detail'] // 需要 blog:read, comment:list-by-post, like:check-user-target 等API权限
      }
    },
    {
      path: '/blogwrite',
      name: 'blogwrite',
      component: () => import('@/pages/Z_blogwrite/blogwritepage.vue'),
      meta: {
        permissions: ['view:blog:write'] // 需要 blog:create 或 blog:update API权限
      }
    },
    {
      path: '/circle/:id',
      name: 'circledetail',
      component: () => import('@/pages/Z_circledetail/circledetail.vue'),
      meta: {
        permissions: ['view:circle:detail'] // 需要 circle:read, blog:list-by-circle 等API权限
      }
    },
    {
      path: '/circle',
      name: 'circle',
      component: () => import('@/pages/Z_circlepage/circlehome.vue'),
      meta: {
        permissions: ['view:circle:list'] // 需要 circle:list, circle:list-public 等API权限
      }
    },
    // {
    //   path: '/algoreco',
    //   name: 'algoreco',
    //   component: () => import('@/pages/f_blogmanage/algoreco/algoreco.vue'),
    //   meta: {
    //     permissions: ['view:admin:algor-reco'] // 后台管理页面，需要 admin:* 相关权限
    //   }
    // },
    {
      path: '/audit',
      name: 'audit',
      component: () => import('@/pages/f_blogmanage/audit/audit.vue'),
      meta: {
        permissions: ['view:admin:blog-audit'] // 需要 admin:blog:pending-list-page, admin:blog:approve 等API权限
      }
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/pages/f_blogmanage/dashboard/dashboard.vue'),
      meta: {
        permissions: ['view:admin:dashboard'] // 需要 admin:stat:* 相关API权限
      }
    },
    {
      path: '/circlemanage',
      name: 'circlemanage',
      component: () => import('@/pages/f_circlemanage/circlemanage/circlemanage.vue'),
      meta: {
        permissions: ['view:admin:circle-manage'] // 需要 circle:list-page, circle:update, circle:delete 等API权限
      }
    },
    // {
    //   path: '/circlesort',
    //   name: 'circlesort',
    //   component: () => import('@/pages/f_circlemanage/circlesort/circlesort.vue'),
    //   meta: {
    //     permissions: ['view:admin:circle-sort']
    //   }
    // },
    // {
    //   path: '/circlefriend',
    //   name: 'circlefriend',
    //   component: () => import('@/pages/f_circlemanage/friend/friend.vue'),
    //   meta: {
    //     permissions: ['view:admin:circle-friend']
    //   }
    // },
    // {
    //   path: '/official',
    //   name: 'circleofficial',
    //   component: () => import('@/pages/f_circlemanage/official/official.vue'),
    //   meta: {
    //     permissions: ['view:admin:circle-official']
    //   }
    // },
    {
      path: '/circleaudit',
      name: 'circleaudit',
      component: () => import('@/pages/f_circlemanage/circleaudit/circleaudit.vue'),
      meta: {
        permissions: ['view:admin:circle-audit'] // 需要 circle:list-pending 等API权限
      }
    },
    // {
    //   path: '/projectalgoreco',
    //   name: 'projectalgoreco',
    //   component: () => import('@/pages/f_projectmanage/algoreco/algoreco.vue'),
    //   meta: {
    //     permissions: ['view:admin:proj-algor-reco']
    //   }
    // },
    // {
    //   path: '/projectaudit',
    //   name: 'projectaudit',
    //   component: () => import('@/pages/f_projectmanage/projectaudit/projectaudit.vue'),
    //   meta: {
    //     permissions: ['view:admin:proj-audit']
    //   }
    // },
    // {
    //   path: '/projectmiss',
    //   name: 'projectmiss',
    //   component: () => import('@/pages/f_projectmanage/projectmiss/projectmiss.vue'),
    //   meta: {
    //     permissions: ['view:admin:proj-miss']
    //   }
    // },
    {
      path: '/label',
      name: 'label',
      component: () => import('@/pages/f_systemmanage/label/label.vue'),
      meta: {
        permissions: ['view:admin:label-manage'] // 需要 common:tag:list-page, common:tag:create 等API权限
      }
    },
    {
      path: '/log',
      name: 'log',
      component: () => import('@/pages/f_systemmanage/log/log.vue'),
      meta: {
        permissions: ['view:admin:log-view'] // 需要 admin:log:operation-list-page 等API权限
      }
    },
    {
      path: '/menu',
      name: 'menu',
      component: () => import('@/pages/f_systemmanage/menu/menu.vue'),
      meta: {
        permissions: ['view:admin:menu-manage'] // 需要 menu:list, menu:create, menu:update 等API权限
      }
    },
    {
      path: '/count',
      name: 'count',
      component: () => import('@/pages/f_systemmanage/usermanage/count/count.vue'),
      meta: {
        permissions: ['view:admin:stat-count'] // 需要 admin:stat:* 相关API权限
      }
    },
    {
      path: '/homepage',
      name: 'homepage',
      component: () => import('@/pages/f_homepage/homepage.vue'),
      meta: {
        permissions: ['view:admin:home'] // 后台首页
      }
    },
    {
      path: '/info',
      name: 'info',
      component: () => import('@/pages/f_systemmanage/usermanage/info/info.vue'),
      meta: {
        permissions: ['view:admin:user-info'] // 需要 admin:user:list-page 等API权限
      }
    },
    {
      path: '/rolelimit',
      name: 'rolelimit',
      component: () => import('@/pages/f_systemmanage/usermanage/rolelimit/rolelimit.vue'),
      meta: {
        permissions: ['view:admin:role-limit'] // 需要 role:list, permission:list 等API权限
      }
    }
  ]
})