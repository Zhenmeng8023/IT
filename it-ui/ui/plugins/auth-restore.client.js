import { useUserStore } from '@/store/user'
import { getToken } from '@/utils/auth'

function matchWhiteList(path, routePath) {
  if (path.includes('*')) {
    const regex = new RegExp('^' + path.replace(/\*/g, '.*') + '$')
    return regex.test(routePath)
  }

  if (path.includes(':')) {
    const regex = new RegExp('^' + path.replace(/:\w+/g, '\\w+') + '$')
    return regex.test(routePath)
  }

  return path === routePath
}

function isWhiteListRoute(routePath) {
  const whiteList = [
    '/login',
    '/registe',
    '/',
    '/blog',
    '/blog/:id',
    '/circle',
    '/circle/:id',
    '/user',
    '/collection',
    '/projectlist',
    '/projectdetail',
    '/projectmanage',
    '/projectcollection',
    '/projectmember',
    '/myproject',
    '/hybridaction/*'
  ]

  return whiteList.some(path => matchWhiteList(path, routePath))
}

export default ({ app }) => {
  const userStore = useUserStore(app.pinia)

  const syncHeaders = () => {
    const token = getToken()
    if (token) {
      app.$axios.defaults.headers.common['X-Token'] = token
      app.$axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
      return
    }

    delete app.$axios.defaults.headers.common['X-Token']
    delete app.$axios.defaults.headers.common['Authorization']
  }

  const restoreAuthState = () => {
    userStore.restorePermissions()
    syncHeaders()
  }

  restoreAuthState()

  app.router.beforeEach((to, from, next) => {
    restoreAuthState()

    if (!isWhiteListRoute(to.path) && !getToken()) {
      next('/login')
      return
    }

    next()
  })
}
