/**
 * 路由权限配置
 * 键为路由路径，值为所需的权限代码
 * 根据 router/index.js 文件生成
 */
export const routePermissions = {
  // 基础页面
  '/': 'view:webhome',
  '/login': 'view:login',
  '/registe': 'view:registe',
  
  // 用户相关
  '/user': 'view:profile',
  '/history': 'view:profile',
  '/collection': 'view:collection',
  
  // 博客相关
  '/blog': 'view:blog',
  '/blog/:id': 'view:blog',
  '/blogwrite': 'view:blog',
  
  // 圈子相关
  '/circle': 'view:circle',
  '/circle/:id': 'view:circle',
  
  // 后台管理 - 博客管理
  '/dashboard': 'view:admin:dashboard',
  '/audit': 'view:admin:blog-audit',
  
  // 后台管理 - 圈子管理
  '/circlemanage': 'view:admin:circle-manage',
  '/circleaudit': 'view:admin:circle-audit',
  
  // 后台管理 - 系统管理
  '/label': 'view:admin:label-manage',
  '/log': 'view:admin:system-log',
  '/menu': 'view:menu',
  '/homepage': 'view:homepage',
  '/info': 'view:admin:user-info',
  '/permission': 'view:permission',
  '/role': 'view:admin:user-role',
  '/count': 'view:admin:user-count'
}

/**
 * 根据路由路径获取所需权限
 * @param {string} path - 路由路径
 * @returns {string|null} - 权限代码或null
 */
export const getRoutePermission = (path) => {
  return routePermissions[path] || null
}

/**
 * 检查用户是否具有指定权限
 * @param {Array} userPermissions - 用户权限列表
 * @param {string} requiredPermission - 所需权限
 * @returns {boolean} - 是否具有权限
 */
export const hasPermission = (userPermissions, requiredPermission) => {
  if (!userPermissions || userPermissions.length === 0) {
    return false
  }
  
  // 检查精确权限
  const hasExactPermission = userPermissions.some(perm => {
    return perm.permissionCode === requiredPermission
  })
  
  if (hasExactPermission) {
    return true
  }
  
  // 检查通配符权限
  const permissionParts = requiredPermission.split(':')
  for (let i = permissionParts.length; i > 0; i--) {
    const wildcardPermission = permissionParts.slice(0, i).join(':') + ':*'
    if (userPermissions.some(perm => perm.permissionCode === wildcardPermission)) {
      return true
    }
  }
  
  return false
}