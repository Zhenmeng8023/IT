export default {
  // Global page headers: https://go.nuxtjs.dev/config-head
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

  // Global CSS: https://go.nuxtjs.dev/config-css
  css: [
    '@/static/css/reset.css',
    'element-ui/lib/theme-chalk/index.css'
  ],

  // Plugins to run before rendering page: https://go.nuxtjs.dev/config-plugins
  plugins: [
    '@/plugins/element-ui',
    { src: '@/plugins/axios', mode: 'client' }
  ],

  // Auto import components: https://go.nuxtjs.dev/config-components
  components: true,

  // Modules for dev and build (recommended): https://go.nuxtjs.dev/config-modules
  buildModules: [
  ],

  // Modules: https://go.nuxtjs.dev/config-modules
  modules: [
    // https://go.nuxtjs.dev/axios
    '@nuxtjs/axios',
    '@nuxtjs/router'
  ],

  // Axios module configuration: https://go.nuxtjs.dev/config-axios
  axios: {
    // Workaround to avoid enforcing hard-coded localhost:3000: https://github.com/nuxt-community/axios-module/issues/308
    baseURL: 'http://localhost:18080/',
  },

  // Build Configuration: https://go.nuxtjs.dev/config-build
  build: {
    transpile: [/^element-ui/],
  },

  // 禁用自动路由
  router: {
    extendRoutes(routes, resolve) {
      // 清空自动生成的路由
      routes.splice(0, routes.length)
      
      // 添加手动配置的路由
      routes.push(
        {
          path: '/',
          component: resolve(__dirname, 'pages/Z_webhomepage/webhome.vue')
        },
        {
          path: '/login',
          component: resolve(__dirname, 'pages/Z_loginpage/logpage.vue')
        },
        {
          path: '/user',
          component: resolve(__dirname, 'pages/Z_userpage/peoplehome.vue')
        },
        {
          path: '/history',
          component: resolve(__dirname, 'pages/Z_historypage/historypage.vue')
        },
        {
          path: '/registe',
          component: resolve(__dirname, 'pages/Z_registepage/registepage.vue')
        },
        {
          path: '/collection',
          component: resolve(__dirname, 'pages/Z_collectionpage/collectionpage.vue')
        },
        {
          path: '/algoreco',
          component: resolve(__dirname, 'pages/f_blogmanage/algoreco/algoreco.vue')
        },
        {
          path: '/audit',
          component: resolve(__dirname, 'pages/f_blogmanage/audit/audit.vue')
        },
        {
          path: '/dashboard',
          component: resolve(__dirname, 'pages/f_blogmanage/dashboard/dashboard.vue')
        },
        {
          path: '/label',
          component: resolve(__dirname, 'pages/f_systemmanage/label/label.vue') 
        },
        {
          path: '/circlemanage',
          component: resolve(__dirname, 'pages/f_circlemanage/circlemanage/circlemanage.vue')
        },
        {
          path: '/circlesort',
          component: resolve(__dirname, 'pages/f_circlemanage/circlesort/circlesort.vue')
        },
        {
          path: '/circlefriend',
          component: resolve(__dirname, 'pages/f_circlemanage/friend/friend.vue')
        },
        {
          path: '/circleofficial',
          component: resolve(__dirname, 'pages/f_circlemanage/official/official.vue')
        },
        {
          path: '/circleaudit',
          component: resolve(__dirname, 'pages/f_circlemanage/circleaudit/circleaudit.vue')
        },
        {
          path: '/log',
          component: resolve(__dirname, 'pages/f_systemmanage/log/log.vue')
        },
        {
          path: '/menu',
          component: resolve(__dirname, 'pages/f_systemmanage/menu/menu.vue')
        },
        {
          path: '/count',
          component: resolve(__dirname, 'pages/f_systemmanage/usermanage/count/count.vue') 
        },
        {
          path: '/info',
          component: resolve(__dirname, 'pages/f_systemmanage/usermanage/info/info.vue') 
        },
        {
          path: '/rolelimit',
          component: resolve(__dirname, 'pages/f_systemmanage/usermanage/rolelimit/rolelimit.vue') 
        },
        {
          path: '/projectalgoreco',
          component: resolve(__dirname, 'pages/f_projectmanage/algoreco/algoreco.vue')
        },
        {
          path: '/projectaudit',
          component: resolve(__dirname, 'pages/f_projectmanage/projectaudit/projectaudit.vue')
        }, 
        {
          path: '/projectmiss',
          component: resolve(__dirname, 'pages/f_projectmanage/projectmiss/projectmiss.vue')
        },
        {
          path: '/homepage',
          component: resolve(__dirname, 'pages/f_homepage/homepage.vue')
        }
      )
    }
  }

}