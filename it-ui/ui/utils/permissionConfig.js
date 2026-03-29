/**
 * 路由权限配置
 * 键为路由路径，值为所需的权限代码
 * 这份配置已补齐 AI 相关页面，供 middleware/auth.js 使用。
 */
export const routePermissions = {
  '/': 'view:webhome',
  '/login': 'view:login',
  '/registe': 'view:registe',

  '/user': 'view:profile',
  '/history': 'view:profile',
  '/collection': 'view:collection',

  '/blog': 'view:blog',
  '/blog/:id': 'view:blog',
  '/blogwrite': 'view:blog',

  '/circle': 'view:circle',
  '/circle/:id': 'view:circle',

  '/dashboard': 'view:admin:dashboard',
  '/audit': 'view:admin:blog-audit',
  '/circlemanage': 'view:admin:circle-manage',
  '/circleaudit': 'view:admin:circle-audit',
  '/label': 'view:admin:label-manage',
  '/log': 'view:admin:system-log',
  '/menu': 'view:menu',
  '/homepage': 'view:homepage',
  '/info': 'view:admin:user-info',
  '/permission': 'view:permission',
  '/role': 'view:admin:user-role',
  '/count': 'view:admin:user-count',

  '/projectlist': 'view:project',
  '/projectdetail': 'view:project-detail',
  '/myproject': 'view:myproject',
  '/projectcollection': 'view:project-collection',
  '/projectmanage': 'view:project-manage',

  '/knowledge-base': 'view:knowledge-base',
  '/ai/models': 'view:ai:model-admin',
  '/ai/prompts': 'view:ai:prompt-template',
  '/ai/logs': 'view:ai:log'
}

export const getRoutePermission = (path) => {
  return routePermissions[path] || null
}

export const hasPermission = (userPermissions, requiredPermission) => {
  if (!userPermissions || userPermissions.length === 0) {
    return false
  }

  const hasExactPermission = userPermissions.some(perm => {
    if (typeof perm === 'string') {
      return perm === requiredPermission
    }
    return perm && perm.permissionCode === requiredPermission
  })

  if (hasExactPermission) {
    return true
  }

  const permissionParts = requiredPermission.split(':')
  for (let i = permissionParts.length; i > 0; i--) {
    const wildcardPermission = permissionParts.slice(0, i).join(':') + ':*'
    const matched = userPermissions.some(perm => {
      if (typeof perm === 'string') {
        return perm === wildcardPermission
      }
      return perm && perm.permissionCode === wildcardPermission
    })
    if (matched) {
      return true
    }
  }

  return false
}
