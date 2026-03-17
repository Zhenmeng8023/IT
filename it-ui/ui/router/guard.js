/**
 * 路由守卫
 * 实现路由前置权限检查和后置处理逻辑
 */
const { getToken, removeToken } = require('@/utils/auth')
const { initPermissions, hasPermission } = require('@/utils/permission')

// 导出一个函数，接收 router 实例作为参数
function setupGuard(router) {
  // 不需要权限的页面（白名单）
  const whiteList = ['/login', '/registe']
  
  // 路由前置守卫
  router.beforeEach(async (to, from, next) => {
    // 检查是否在白名单中
    if (whiteList.includes(to.path)) {
      next()
      return
    }

    // 检查是否有token
    const token = getToken()
    if (!token) {
      next('/login')
      return
    }

    try {
      // 初始化权限（仅在需要时）
      await initPermissions()

      // 检查路由权限
      const routeMeta = to.meta || {}
      const requiredPermissions = routeMeta.permissions || []

      // 检查用户是否拥有所有必需的权限
      const hasAllPermissions = requiredPermissions.every(permission => {
        return hasPermission(permission)
      })

      if (requiredPermissions.length > 0 && !hasAllPermissions) {
        // 没有权限，重定向到首页并显示提示
        console.warn('权限不足，无法访问:', to.path)
        next('/')
        return
      }

      // 权限检查通过，继续导航
      next()
    } catch (error) {
      console.error('权限检查失败:', error)
      // 权限检查失败，清除token并跳转到登录页
      removeToken()
      next('/login')
    }
  })

  // 路由后置守卫
  router.afterEach((to, from) => {
    // 设置页面标题
    document.title = to.meta.title || 'IT平台'
    // 可以在这里添加埋点统计等逻辑
  })
}

module.exports = setupGuard