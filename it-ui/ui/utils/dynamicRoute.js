import { getMenus, hasPermission } from './permission'

/**
 * 生成动态路由
 * @returns {Array} - 动态路由列表
 */
export function generateRoutes() {
  const menus = getMenus()
  return generateRoutesFromMenus(menus)
}

/**
 * 从菜单生成路由
 * @param {Array} menus - 菜单列表
 * @returns {Array} - 路由列表
 */
function generateRoutesFromMenus(menus) {
  const routes = []

  menus.forEach(menu => {
    // 检查菜单权限
    if (menu.permission && !hasPermission(menu.permission)) {
      return
    }

    const route = {
      path: menu.path,
      name: menu.name,
      component: () => import(`@/pages${menu.component}`),
      meta: {
        title: menu.title,
        permission: menu.permission
      }
    }

    // 处理子菜单
    if (menu.children && menu.children.length > 0) {
      route.children = generateRoutesFromMenus(menu.children)
    }

    routes.push(route)
  })

  return routes
}

/**
 * 检查路由权限
 * @param {Object} route - 路由对象
 * @returns {boolean} - 是否有权限
 */
export function checkRoutePermission(route) {
  const meta = route.meta || {}
  const permission = meta.permission

  if (!permission) {
    return true
  }

  return hasPermission(permission)
}