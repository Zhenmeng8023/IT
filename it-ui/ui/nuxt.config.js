export default {
  // Global page headers
  head: {
    title: 'day00_demo',
    htmlAttrs: {
      lang: 'en'
    },
    meta: [
      { charset: 'utf-8' },
      { name: 'viewport', content: 'width=device-width, initial-scale=1' },
      { hid: 'description', name: 'description', content: '' },
      { name: 'format-detection', content: 'telephone=no' }
    ],
    link: [
      { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' }
    ]
  },

  // Global CSS
  css: [
    '@/static/css/reset.css',
    'element-ui/lib/theme-chalk/index.css',
    'quill/dist/quill.snow.css',
    'quill/dist/quill.bubble.css',
    'quill/dist/quill.core.css'
  ],

  // Plugins
  plugins: [
    '@/plugins/pinia', // 添加 pinia 插件 (确保在其他插件和中间件之前执行)
    '@/plugins/element-ui',
    '@/plugins/permission', // 添加权限指令插件
    { src: '@/plugins/axios', mode: 'client' },
    { src: '@/plugins/quill.client.js', mode: 'client' }, // 添加客户端插件
  ],

  // Auto import components
  components: true,

  // Modules
  buildModules: [
    '@nuxtjs/composition-api/module'
  ],
  modules: [
    '@nuxtjs/axios',
    '@nuxtjs/router'
  ],

  // Axios configuration
  axios: {
    baseURL: 'http://localhost:18080/',
  },

  // Build Configuration
  build: {
    // 转译配置
    transpile: [
      /^element-ui/
    ],
    
    // Babel 配置 - 添加可选链支持
    babel: {
      compact: true,
      sourceType: 'unambiguous',
      presets: [
        ['@babel/preset-env', {
          targets: {
            browsers: ['> 1%', 'last 2 versions', 'not dead']
          },
          useBuiltIns: 'usage',
          corejs: 3
        }]
      ],
      plugins: [
        '@babel/plugin-proposal-optional-chaining',
        '@babel/plugin-proposal-nullish-coalescing-operator'
      ]
    },
    
    // 扩展 Webpack 配置
    extend(config, { isDev, isClient }) {
      // 确保 babel-loader 处理可选链
      const babelLoader = config.module.rules.find(
        rule => rule.use && rule.use[0] && rule.use[0].loader === 'babel-loader'
      );
      
      if (babelLoader) {
        // 确保 node_modules 中的文件也被转译
        const originalExclude = babelLoader.exclude;
        babelLoader.exclude = (filePath) => {
          // 对 .nuxt 目录下的文件也进行转译
          if (filePath.includes('.nuxt')) {
            return false;
          }
          // 其他 node_modules 保持排除
          return originalExclude && originalExclude.test && originalExclude.test(filePath);
        };
      }
      
      // 禁用性能提示
      config.performance = {
        hints: false,
        maxEntrypointSize: 512000,
        maxAssetSize: 512000
      };
      
      // 开发环境下添加 source-map 支持
      if (isDev) {
        config.devtool = 'cheap-module-source-map';
      }
    }
  },

  // 禁用自动路由
  router: {
    middleware: 'auth', // 添加全局中间件
    extendRoutes(routes, resolve) {
      // 清空自动生成的路由
      routes.splice(0, routes.length)
      
      // 添加手动配置的路由
      routes.push(
        {
          path: '/',
          component: resolve(__dirname, 'pages/Z_webhomepage/webhome.vue'),
          meta: {
            permissions: ['view:webhome']
          }
        },
        {
          path: '/login',
          component: resolve(__dirname, 'pages/Z_loginpage/logpage.vue'),
          meta: {
            permissions: ['view:login']
          }
        },
        {
          path: '/user',
          component: resolve(__dirname, 'pages/Z_userpage/peoplehome.vue'),
          meta: {
            permissions: ['view:profile']
          }
        },
        {
          path: '/history',
          component: resolve(__dirname, 'pages/Z_historypage/historypage.vue'),
          meta: {
            permissions: ['view:profile']
          }
        },
        {
          path: '/registe',
          component: resolve(__dirname, 'pages/Z_registepage/registepage.vue'),
          meta: {
            permissions: ['view:registe']
          }
        },
        {
          path: '/collection',
          component: resolve(__dirname, 'pages/Z_collectionpage/collectionpage.vue'),
          meta: {
            permissions: ['view:collection']
          }
        },
        {
          path: '/blog',
          component: resolve(__dirname, 'pages/Z_blogpage/blogpage.vue'),
          meta: {
            permissions: ['view:blog']
          }
        },
        {
          path: '/blog/:id',
          component: resolve(__dirname, 'pages/Z_blogdetail/blogdetail.vue'),
          meta: {
            permissions: ['view:blog']
          }
        },
        {
          path: '/blogwrite',
          component: resolve(__dirname, 'pages/Z_blogwrite/blogwritepage.vue'),
          meta: {
            permissions: ['view:blog']
          }
        },
        {
          path: '/circle/:id',
          component: resolve(__dirname, 'pages/Z_circledetail/circledetail.vue'),
          meta: {
            permissions: ['view:circle']
          }
        },
        {
          path: '/circle',
          component: resolve(__dirname, 'pages/Z_circlepage/circlehome.vue'),
          meta: {
            permissions: ['view:circle']
          }
        },
        {
          path: '/algoreco',
          component: resolve(__dirname, 'pages/f_blogmanage/algoreco/algoreco.vue'),
          meta: {
            permissions: ['view:admin:algor-reco']
          }
        },
        {
          path: '/audit',
          component: resolve(__dirname, 'pages/f_blogmanage/audit/audit.vue'),
          meta: {
            permissions: ['view:admin:blog-audit']
          }
        },
        {
          path: '/dashboard',
          component: resolve(__dirname, 'pages/f_blogmanage/dashboard/dashboard.vue'),
          meta: {
            permissions: ['view:admin:dashboard']
          }
        },
        {
          path: '/label',
          component: resolve(__dirname, 'pages/f_systemmanage/label/label.vue'),
          meta: {
            permissions: ['view:admin:label-manage']
          }
        },
        {
          path: '/circlemanage',
          component: resolve(__dirname, 'pages/f_circlemanage/circlemanage/circlemanage.vue'),
          meta: {
            permissions: ['view:admin:circle-manage']
          }
        },
        // {
        //   path: '/circlesort',
        //   component: resolve(__dirname, 'pages/f_circlemanage/circlesort/circlesort.vue')
        // },
        // {
        //   path: '/circlefriend',
        //   component: resolve(__dirname, 'pages/f_circlemanage/friend/friend.vue')
        // },
        // {
        //   path: '/circleofficial',
        //   component: resolve(__dirname, 'pages/f_circlemanage/official/official.vue')
        // },
        {
          path: '/circleaudit',
          component: resolve(__dirname, 'pages/f_circlemanage/circleaudit/circleaudit.vue'),
          meta: {
            permissions: ['view:admin:circle-audit']
          }
        },
        {
          path: '/log',
          component: resolve(__dirname, 'pages/f_systemmanage/log/log.vue'),
          meta: {
            permissions: ['view:admin:system-log']
          }
        },
        {
          path: '/menu',
          component: resolve(__dirname, 'pages/f_systemmanage/menu/menu.vue'),
          meta: {
            permissions: ['view:menu']
          }
        },
        {
          path: '/count',
          component: resolve(__dirname, 'pages/f_systemmanage/usermanage/count/count.vue'),
          meta: {
            permissions: ['view:admin:user-count']
          }
        },
        {
          path: '/info',
          component: resolve(__dirname, 'pages/f_systemmanage/usermanage/info/info.vue'),
          meta: {
            permissions: ['view:admin:user-info']
          }
        },
        // {
        //   path: '/projectalgoreco',
        //   component: resolve(__dirname, 'pages/f_projectmanage/algoreco/algoreco.vue')
        // },
        // {
        //   path: '/projectaudit',
        //   component: resolve(__dirname, 'pages/f_projectmanage/projectaudit/projectaudit.vue')
        // }, 
        // {
        //   path: '/projectmiss',
        //   component: resolve(__dirname, 'pages/f_projectmanage/projectmiss/projectmiss.vue')
        // },
        {
          path: '/homepage',
          component: resolve(__dirname, 'pages/f_homepage/homepage.vue'),
          meta: {
            permissions: ['view:homepage']
          }
        },
        {
          path:'/permission',
          component: resolve(__dirname, 'pages/f_systemmanage/permission/permission.vue'),
          meta: {
            permissions: ['view:permission']
          }
        },
        {
          path:'/role',
          component: resolve(__dirname, 'pages/f_systemmanage/role/role.vue'),
          meta: {
            permissions: ['view:admin:user-role']
          }
        },
        {
          path:'/noPermission',
          component: resolve(__dirname, 'pages/S_nopermission/noPermission.vue'),
          hidden: true
        },
        {
          path:'/projectlist',
          component: resolve(__dirname, 'pages/f_project/list/list.vue'),
          meta: {
            permissions: ['view:project']
          }
        },
        {
          path:'/projectdetail',
          component: resolve(__dirname, 'pages/f_project/projectdetail/projectdetail.vue'),
          meta: {
            permissions: ['view:project-detail']
          }
        },
        {
          path:'/myproject',
          component: resolve(__dirname, 'pages/f_project/myproject/myproject.vue'),
          meta: {
            permissions: ['view:myproject']
          }
        },
        {
          path:'/projectcollection',
          component: resolve(__dirname, 'pages/f_project/projectcollection/projectcollection.vue'),
          meta: {
            permissions: ['view:project-collection']
          }
        },
        {
          path:'/projectmanage',
          component: resolve(__dirname, 'pages/f_project/projectmanage/projectmanage.vue'),
          meta: {
            permissions: ['view:project-manage']
          }
        },
        {
          path:'/wallet',
          component: resolve(__dirname, 'pages/Z_wallet/wallet.vue'),
          meta: {
            permissions: ['view:pay']
          }
        },
        {
          path:'/orders_purchases',
          component: resolve(__dirname, 'pages/Z_userpage/orders_purchases.vue'),
          meta: {
            permissions: ['view:order-purchases']
          }
        },
        {
          path:'/vip',
          component: resolve(__dirname, 'pages/Z_vip/vip.vue'),
          meta: {
            permissions: ['view:vip']
          }
        },
      )
    }
  },
  
  // 添加缓存配置，加快二次构建速度
  cache: true,
  
  // 中间件配置
  middleware: ['auth'],
  
  // 配置服务器端口
  server: {
    port: 3000,
    host: 'localhost'
  }
}