/**
 * 权限验证中间件
 * 确保用户只能访问有权限的页面
 */
import { getToken } from '@/utils/auth'
import { initPermissions, hasPermission } from '@/utils/permission'
import { Login } from '@/api'

export default async function({ route, redirect, app }) {
  // 不需要权限的页面（白名单）
  const whiteList = ['/login', '/registe', '/']
  
  // 检查是否在白名单中
  if (whiteList.includes(route.path)) {
    return
  }

  // 检查是否有token
  const token = getToken()
  if (!token) {
    return redirect('/login')
  }

  try {
    // 初始化权限
    await initPermissions()

    // 检查路由权限
    const routeMeta = route.meta || {}
    const requiredPermissions = routeMeta.permissions || []

    // 检查用户是否拥有所有必需的权限
    const hasAllPermissions = requiredPermissions.every(permission => {
      return hasPermission(permission)
    })

    if (requiredPermissions.length > 0 && !hasAllPermissions) {
      // 没有权限，重定向到首页
      return redirect('/')
    }
  } catch (error) {
    console.error('权限检查失败:', error)
    // 权限检查失败，清除token并跳转到登录页
    return redirect('/login')
  }
}