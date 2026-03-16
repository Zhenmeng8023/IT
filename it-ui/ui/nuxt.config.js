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
    '@/plugins/element-ui',
    { src: '@/plugins/axios', mode: 'client' },
    { src: '@/plugins/quill.client.js', mode: 'client' } // 添加客户端插件
  ],

  // Auto import components
  components: true,

  // Modules
  buildModules: [],
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
          path: '/blog',
          component: resolve(__dirname, 'pages/Z_blogpage/blogpage.vue')
        },
        {
          path: '/blog/:id',
          component: resolve(__dirname, 'pages/Z_blogdetail/blogdetail.vue')
        },
        {
          path: '/blogwrite',
          component: resolve(__dirname, 'pages/Z_blogwrite/blogwritepage.vue')
        },
        {
          path: '/circle/:id',
          component: resolve(__dirname, 'pages/Z_circledetail/circledetail.vue')
        },
        {
          path: '/circle',
          component: resolve(__dirname, 'pages/Z_circlepage/circlehome.vue')
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
  },
  
  // 添加缓存配置，加快二次构建速度
  cache: true,
  
  // 配置服务器端口
  server: {
    port: 3000,
    host: 'localhost'
  }
}