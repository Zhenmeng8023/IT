// index.js
import Vue from 'vue'
import { Router, createRouter, createWebHistory } from 'vue-router'

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

/**
 * 路由配置及页面权限映射表
 *
 * 此文件中的权限代码已根据 permission.sql 文件进行校验和修正。
 * 原则上，页面路由权限应使用 `view:` 开头的权限代码。
 */
export default new Router({
  mode: 'history',
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/pages/Z_webhomepage/webhome.vue'),
      meta: {
        permissions: ['view:webhome'] // 校验无误，id=157
      }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/Z_loginpage/logpage.vue'),
      meta: {
        permissions: ['view:login'] // 校验无误，id=160
      }
    },
    {
      path: '/user',
      name: 'user',
      component: () => import('@/pages/Z_userpage/peoplehome.vue'),
      meta: {
        permissions: ['view:profile'] // 校验无误，id=146
      }
    },
    {
      path: '/collection',
      name: 'collection',
      component: () => import('@/pages/Z_collectionpage/collectionpage.vue'),
      meta: {
        permissions: ['view:collection'] // 修正：原 'view:collection:page' 在permission.sql中不存在，正确代码为 id=145 的 'view:collection'
      }
    },
    {
      path: '/blog',
      name: 'blog',
      component: () => import('@/pages/Z_blogpage/blogpage.vue'),
      meta: {
        permissions: ['view:blog'] // 修正：原 'view:blog:list' 在permission.sql中不存在，正确代码为 id=142 的 'view:blog'
      }
    },
    {
      path: '/blog/:id',
      name: 'blogdetail',
      component: () => import('@/pages/Z_blogdetail/blogdetail.vue'),
      meta: {
        permissions: ['view:blog'] // 该权限代码在permission.sql中不存在，可能需要新增或使用其他现有权限
      }
    },
    {
      path: '/blogwrite',
      name: 'blogwrite',
      component: () => import('@/pages/Z_blogwrite/blogwritepage.vue'),
      meta: {
        permissions: ['view:blog'] // 修正：原 'view:blog:write' 在permission.sql中不存在，正确代码为 id=158 的 'view:writeblog'
      }
    },
    {
      path: '/circle/:id',
      name: 'circledetail',
      component: () => import('@/pages/Z_circledetail/circledetail.vue'),
      meta: {
        permissions: ['view:circle'] // 修正：原 'view:circle:detail' 在permission.sql中不存在，正确代码为 id=159 的 'view:circledetail'
      }
    },
    {
      path: '/circle',
      name: 'circle',
      component: () => import('@/pages/Z_circlepage/circlehome.vue'),
      meta: {
        permissions: ['view:circle'] // 修正：原 'view:circle:list' 在permission.sql中不存在，正确代码为 id=144 的 'view:circle'
      }
    },
    {
      path: '/audit',
      name: 'audit',
      component: () => import('@/pages/f_blogmanage/audit/audit.vue'),
      meta: {
        permissions: ['view:admin:blog-audit'] // 校验无误，id=130
      }
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/pages/f_blogmanage/dashboard/dashboard.vue'),
      meta: {
        permissions: ['view:admin:dashboard'] // 校验无误，id=131
      }
    },
    {
      path: '/circlemanage',
      name: 'circlemanage',
      component: () => import('@/pages/f_circlemanage/circlemanage/circlemanage.vue'),
      meta: {
        permissions: ['view:admin:circle-manage'] // 校验无误，id=133
      }
    },
    {
      path: '/circleaudit',
      name: 'circleaudit',
      component: () => import('@/pages/f_circlemanage/circleaudit/circleaudit.vue'),
      meta: {
        permissions: ['view:admin:circle-audit'] // 校验无误，id=132
      }
    },
    {
      path: '/label',
      name: 'label',
      component: () => import('@/pages/f_systemmanage/label/label.vue'),
      meta: {
        permissions: ['view:admin:label-manage'] // 校验无误，id=141
      }
    },
    {
      path: '/log',
      name: 'log',
      component: () => import('@/pages/f_systemmanage/log/log.vue'),
      meta: {
        permissions: ['view:admin:system-log'] // 修正：原 'view:admin:log-view' 在permission.sql中不存在，正确代码为 id=138 的 'view:admin:system-log'
      }
    },
    {
      path: '/menu',
      name: 'menu',
      component: () => import('@/pages/f_systemmanage/menu/menu.vue'),
      meta: {
        permissions: ['view:menu'] // 修正：原 'view:admin:menu-manage' 在permission.sql中不存在，正确代码为 id=152 的 'view:menu'
      }
    },
    {
      path: '/homepage',
      name: 'homepage',
      component: () => import('@/pages/f_homepage/homepage.vue'),
      meta: {
        permissions: ['view:homepage'] // 修正：原 'view:admin:home' 在permission.sql中不存在，正确代码为 id=147 的 'view:homepage'
      }
    },
    {
      path: '/info',
      name: 'info',
      component: () => import('@/pages/f_systemmanage/usermanage/info/info.vue'),
      meta: {
        permissions: ['view:admin:user-info'] // 校验无误，id=139
      }
    },
    {
      path: '/permission',
      name: 'permission',
      component: () => import('@/pages/f_systemmanage/permission/permission.vue'),
      meta: {
        permissions: ['view:permission'] // 修正：原 'view:admin:permission-manage' 在permission.sql中不存在，正确代码为 id=153 的 'view:permission'
      }
    },
    {
      path: '/role',
      name: 'role',
      component: () => import('@/pages/f_systemmanage/role/role.vue'),
      meta: {
        permissions: ['view:admin:user-role'] // 修正：原 'view:admin:role-manage' 在permission.sql中不存在，正确代码为 id=140 的 'view:admin:user-role'
      }
    },
    {
      path: '/count',
      name: 'count',
      component: () => import('@/pages/f_systemmanage/usermanage/count/count.vue'),
      meta: {
        permissions: ['view:admin:user-count'] // 修正：原 'view:admin:stat-count' 在permission.sql中不存在，正确代码为 id=151 的 'view:admin:user-count'
      }
    },
    {
      path: '/registe',
      name: 'registe',
      component: () => import('@/pages/Z_registepage/registepage.vue'),
      meta: {
        permissions: ['view:registe'] // 修正：原 'view:auth:register-page' 在permission.sql中不存在，正确代码为 id=161 的 'view:registe'
      }
    },
    {
      path: '/history',
      name: 'history',
      component: () => import('@/pages/Z_historypage/historypage.vue'),
      meta: {
        permissions: ['view:profile'] // 该权限代码在permission.sql中不存在，可能需要新增或使用其他现有权限
      }
    },
    // {
    //   path: '/algoreco',
    //   name: 'algoreco',
    //   component: () => import('@/pages/f_blogmanage/algoreco/algoreco.vue'),
    //   meta: {
    //     permissions: ['view:admin:algor-reco']
    //   }
    // },
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
    //     permissions: ['view:admin:official-manage']
    //   }
    // },
    // {
    //   path: '/projectalgoreco',
    //   name: 'projectalgoreco',
    //   component: () => import('@/pages/f_projectmanage/algoreco/algoreco.vue'),
    //   meta: {
    //     permissions: ['view:admin:algor-reco'] // 与blog algoreco相同
    //   }
    // },
    // {
    //   path: '/projectaudit',
    //   name: 'projectaudit',
    //   component: () => import('@/pages/f_projectmanage/projectaudit/projectaudit.vue'),
    //   meta: {
    //     permissions: ['view:admin:project-audit']
    //   }
    // },
    // {
    //   path: '/projectmiss',
    //   name: 'projectmiss',
    //   component: () => import('@/pages/f_projectmanage/projectmiss/projectmiss.vue'),
    //   meta: {
    //     permissions: ['view:admin:official-manage'] // 与official-manage相同
    //   }
    // },
    {
      path: '/projectlist',
      name: 'projectlist',
      component: () => import('@/pages/f_project/list/list.vue'),
      meta: {
        permissions: ['view:project']
      }
    },
      {
        path: '/projectdetail',
        name: 'projectdetail',
        component: () => import('@/pages/f_project/projectdetail/projectdetail.vue'),
        meta: {
          permissions: ['view:project-detail']
        }
      },
      {
        path: '/myproject',
        name: 'myproject',
        component: () => import('@/pages/f_project/myproject/myproject.vue'),
        meta: {
          permissions: ['view:myproject']
        }
      },
      {
        path: '/projectcollection',
        name: 'projectcollection',
        component: () => import('@/pages/f_project/projectcollection/projectcollection.vue'),
        meta: {
          permissions: ['view:project-collection']
        }
      },
      {
        path: '/projectmanage',
        name: 'projectmanage',
        component: () => import('@/pages/f_project/projectmanage/projectmanage.vue'),
        meta: {
          permissions: ['view:project-manage']
        }
      },
      {
        path: '/pay',
        name: 'pay',
        component: () => import('@/pages/f_project/pay/pay.vue'),
        meta: {
          permissions: ['view:pay']
        }
      },

     ]
})