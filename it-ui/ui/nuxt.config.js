import { buildNuxtRoutes } from './router/route-source'

export default {
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
    ],
    script: [
      {
        hid: 'theme-init',
        innerHTML: "(function(){try{var key='it-ui-theme';var saved=localStorage.getItem(key);var prefers=window.matchMedia&&window.matchMedia('(prefers-color-scheme: dark)').matches;var theme=saved||(prefers?'dark':'light');document.documentElement.setAttribute('data-theme',theme);}catch(e){document.documentElement.setAttribute('data-theme','light');}})();"
      }
    ],
    __dangerouslyDisableSanitizersByTagID: {
      'theme-init': ['innerHTML']
    }
  },

  css: [
    '@/static/css/reset.css',
    'element-ui/lib/theme-chalk/index.css',
    'quill/dist/quill.snow.css',
    'quill/dist/quill.bubble.css',
    'quill/dist/quill.core.css',
    '@/static/css/theme.css'
  ],

  plugins: [
    '@/plugins/pinia',
    { src: '@/plugins/theme.client.js', mode: 'client' },
    { src: '@/plugins/avatar-position.client.js', mode: 'client' },
    '@/plugins/element-ui',
    '@/plugins/permission',
    { src: '@/plugins/axios', mode: 'client' },
    { src: '@/plugins/quill.client.js', mode: 'client' }
  ],

  components: true,

  buildModules: [
    '@nuxtjs/composition-api/module'
  ],

  modules: [
    '@nuxtjs/axios'
  ],

  axios: {
    baseURL: 'http://localhost:18080/',
    credentials: true
  },

  build: {
    transpile: [
      /^element-ui/
    ],

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

    extend(config, { isDev }) {
      const babelLoader = config.module.rules.find(
        rule => rule.use && rule.use[0] && rule.use[0].loader === 'babel-loader'
      )

      if (babelLoader) {
        const originalExclude = babelLoader.exclude
        babelLoader.exclude = (filePath) => {
          if (filePath.includes('.nuxt')) {
            return false
          }
          return originalExclude && originalExclude.test && originalExclude.test(filePath)
        }
      }

      config.performance = {
        hints: false,
        maxEntrypointSize: 512000,
        maxAssetSize: 512000
      }

      if (isDev) {
        config.devtool = 'cheap-module-source-map'
      }
    }
  },

  router: {
    middleware: 'auth',
    extendRoutes(routes, resolve) {
      routes.splice(0, routes.length, ...buildNuxtRoutes(pagePath => resolve(__dirname, pagePath)))
    }
  },

  cache: true,

  server: {
    port: 3000,
    host: 'localhost'
  }
}
