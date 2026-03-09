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
          component: resolve(__dirname, 'pages/webhomepage/webhome.vue')
        },
        {
          path: '/login',
          component: resolve(__dirname, 'pages/loginpage/logpage.vue')
        },
        {
          path: '/user',
          component: resolve(__dirname, 'pages/userpage/peoplehome.vue')
        },
        {
          path: '/manage',
          component: resolve(__dirname, 'pages/f_systemmanage/manage.vue')
        }
      )
    }
  }

}
