/**
 * 路由生成工具
 * 基于权限的路由过滤逻辑
 * 提供路由更新方法
 */
const router = require('./index')
const { staticRoutes, dynamicRoutes } = require('./index')

/**
 * 基于权限过滤路由
 * @param {Array} permissions - 用户权限列表
 * @returns {Array} 过滤后的路由列表
 */
function filterRoutesByPermissions(permissions) {
  // 静态路由始终保留
  const filteredRoutes = [...staticRoutes]
  
  // 过滤动态路由
  const accessibleDynamicRoutes = dynamicRoutes.filter(route => {
    // 检查路由是否需要权限
    if (!route.meta || !route.meta.permissions) {
      // 没有权限要求的路由默认允许访问
      return true
    }
    
    // 检查用户是否拥有至少一个所需权限
    return route.meta.permissions.some(permission => {
      return permissions.includes(permission)
    })
  })
  
  // 合并静态路由和过滤后的动态路由
  return [...filteredRoutes, ...accessibleDynamicRoutes]
}

/**
 * 更新路由配置
 * @param {Array} permissions - 用户权限列表
 */
function updateRoutes(permissions) {
  // 过滤路由
  const filteredRoutes = filterRoutesByPermissions(permissions)
  
  // 重置路由
  router.matcher = new router.constructor().matcher
  
  // 添加过滤后的路由
  filteredRoutes.forEach(route => {
    router.addRoute(route)
  })
  
  return filteredRoutes
}

/**
 * 检查用户是否有权限访问指定路由
 * @param {Object} route - 路由对象
 * @param {Array} permissions - 用户权限列表
 * @returns {boolean} 是否有权限
 */
function checkRoutePermission(route, permissions) {
  if (!route.meta || !route.meta.permissions) {
    return true
  }
  
  return route.meta.permissions.some(permission => {
    return permissions.includes(permission)
  })
}

/**
 * 获取用户可访问的路由列表
 * @param {Array} permissions - 用户权限列表
 * @returns {Array} 可访问的路由列表
 */
function getAccessibleRoutes(permissions) {
  return filterRoutesByPermissions(permissions)
}

module.exports = {
  filterRoutesByPermissions,
  updateRoutes,
  checkRoutePermission,
  getAccessibleRoutes
}