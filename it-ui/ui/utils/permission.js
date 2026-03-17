import { getToken } from './auth'
import { GetUserPermissions, GetUserMenus, GetCurrentUser } from '@/api'

// 存储用户权限
let userPermissions = []
let userMenus = []

/**
 * 初始化用户权限
 * @returns {Promise} - 返回Promise对象
 */
export async function initPermissions() {
  if (!getToken()) {
    userPermissions = []
    userMenus = []
    return
  }

  try {
    // 获取用户权限 - 注意：这里应该传递当前用户ID，但为了简化，我们直接调用不带参数的接口
    // 假设后端API支持当前登录用户的权限获取
    const permissionsResponse = await GetUserPermissions()
    userPermissions = permissionsResponse.data || []

    // 获取用户菜单 - 同样假设后端API支持当前登录用户的菜单获取
    const menusResponse = await GetUserMenus()
    userMenus = menusResponse.data || []

    // 存储权限到本地存储
    localStorage.setItem('userPermissions', JSON.stringify(userPermissions))
    localStorage.setItem('userMenus', JSON.stringify(userMenus))
  } catch (error) {
    console.error('初始化权限失败:', error)
    userPermissions = []
    userMenus = []
  }
}

/**
 * 获取用户权限
 * @returns {Array} - 用户权限列表
 */
export function getPermissions() {
  // 从本地存储获取权限
  const storedPermissions = localStorage.getItem('userPermissions')
  if (storedPermissions) {
    userPermissions = JSON.parse(storedPermissions)
  }
  return userPermissions
}

/**
 * 获取用户菜单
 * @returns {Array} - 用户菜单列表
 */
export function getMenus() {
  // 从本地存储获取菜单
  const storedMenus = localStorage.getItem('userMenus')
  if (storedMenus) {
    userMenus = JSON.parse(storedMenus)
  }
  return userMenus
}

/**
 * 检查用户是否有权限
 * @param {string} permission - 权限标识
 * @returns {boolean} - 是否有权限
 */
export function hasPermission(permission) {
  const permissions = getPermissions()
  return permissions.some(p => p === permission)
}

/**
 * 检查用户是否有多个权限中的任意一个
 * @param {Array} permissions - 权限标识列表
 * @returns {boolean} - 是否有任意一个权限
 */
export function hasAnyPermission(permissions) {
  return permissions.some(permission => hasPermission(permission))
}

/**
 * 检查用户是否有所有指定的权限
 * @param {Array} permissions - 权限标识列表
 * @returns {boolean} - 是否有所有权限
 */
export function hasAllPermissions(permissions) {
  return permissions.every(permission => hasPermission(permission))
}

/**
 * 清空用户权限
 */
export function clearPermissions() {
  userPermissions = []
  userMenus = []
  localStorage.removeItem('userPermissions')
  localStorage.removeItem('userMenus')
}