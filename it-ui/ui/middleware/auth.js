import { GetCurrentUser, GetRolePermissions } from '~/api/index'
import { getToken, removeToken } from '~/utils/auth'
import { getRoutePermission, hasPermission } from '~/utils/permissionConfig'
// 在中间件中避免直接使用useUserStore，因为中间件可能在服务器端执行
// import { useUserStore } from '~/store/user'

// 权限缓存，避免重复请求
let permissionCache = {
  timestamp: 0,
  data: null
}
const CACHE_DURATION = 5 * 60 * 1000 // 缓存5分钟

export default async function ({ route, redirect, app, store, req }) {
  console.log('权限中间件执行，当前路由:', route.path)
  
  // 不需要登录的页面
  const whiteList = ['/login', '/registe', '/', '/blog', '/blog/:id', '/circle', '/circle/:id', '/user',
    // 添加异常路由到白名单
    '/hybridaction/*'
  ]
  
 // 检查是否在白名单中
const isWhiteList = whiteList.some(path => {
  if (path.includes('*')) {
    // 处理通配符路由
    const regex = new RegExp('^' + path.replace(/\*/g, '.*') + '$')
    return regex.test(route.path)
  } else if (path.includes(':')) {
    // 处理带参数的路由
    const regex = new RegExp('^' + path.replace(/:\w+/g, '\\w+') + '$')
    return regex.test(route.path)
  }
  return path === route.path
})
  
  console.log('是否在白名单中:', isWhiteList)
  
  if (isWhiteList) {
    console.log('在白名单中，跳过权限检查')
    return
  }
  
  
  try {
    // 正确访问Pinia store状态
    let userStore = store.user || {}

    // 首先检查token是否存在，传递req参数
    let token = getToken(req)
    
    console.log('获取到的token:', token ? '存在' : '不存在')
    
    if (!token) {
      console.log('token不存在，跳转到登录页')
      return redirect('/login')
    }

      // 设置token到axios默认头，避免后续API请求重复获取
      if (app.$axios) {
        app.$axios.defaults.headers.common['X-Token'] = token
      }

    
    //console.log('当前用户信息:', userStore.user)
    //console.log('当前用户权限:', userStore.permissions)
    
    // 检查用户信息是否已存在或缓存是否有效
    const now = Date.now()
    if (!userStore.user || !userStore.permissions || userStore.permissions.length === 0 || (permissionCache.timestamp + CACHE_DURATION) < now) {
      console.log('用户信息不存在或缓存过期，重新获取')
      
      // 获取当前用户信息
      const userResponse = await GetCurrentUser()
      console.log('获取用户信息响应:', userResponse)
      
      // 支持不同的响应结构
      let user = userResponse.data
      if (!user) {
        // 尝试其他可能的响应结构
        user = userResponse.user || null
      }
      
      //console.log('提取的用户信息:', user)
      
      if (!user) {
        throw new Error('获取用户信息失败')
      }
      
      // 获取用户权限（通过角色ID）
      let permissionCodes = []
      if (user.roleId) {
        //console.log('开始获取角色权限，角色ID:', user.roleId)
        // 调用API获取角色权限
        const permissionsResponse = await GetRolePermissions(user.roleId)
        //console.log('获取权限响应:', permissionsResponse)
        
        const rolePermissions = permissionsResponse.data || []
        //console.log('角色原始权限数据:', rolePermissions)
        
        // 提取权限代码
        permissionCodes = rolePermissions
          .filter(permission => permission.permissionCode)
          .map(permission => permission.permissionCode)
        console.log('提取的权限代码:', permissionCodes)
      } else {
        console.log('用户没有角色ID，无法获取权限')
      }
      
      // 存储用户信息和权限到全局状态
      if (userStore.setUserInfo) {
        userStore.setUserInfo(user)
      }
      if (userStore.setPermissions) {
        userStore.setPermissions(permissionCodes)
      }
      
      // 更新缓存
      permissionCache = {
        timestamp: now,
        data: {
          user,
          permissions: permissionCodes
        }
      }
      
      console.log('权限信息已更新:', permissionCodes.length, '个权限')
    }
    
    // 确保权限数组存在
    let userPermissions = permissionCache.data?.permissions || []
    
    console.log('最终使用的权限数组:', userPermissions)
    
    // 检查页面权限
    const requiredPermission = getRoutePermission(route.path)
    console.log('当前路由所需权限:', requiredPermission)
    
    if (requiredPermission) {
      // 支持多级权限检查
      const permissionParts = requiredPermission.split(':')
      let hasAccess = false
      
      // 检查精确权限
      if (hasPermission(userPermissions, requiredPermission)) {
        hasAccess = true
        console.log('用户具有精确权限:', requiredPermission)
      } else {
        // 检查父级权限 (如 system:user:manage 包含 system:user:view)
        for (let i = permissionParts.length - 1; i > 0; i--) {
          const parentPermission = permissionParts.slice(0, i).join(':') + ':*'
          if (hasPermission(userPermissions, parentPermission)) {
            hasAccess = true
            console.log('用户具有父级权限:', parentPermission)
            break
          }
        }
      }
      
      if (!hasAccess) {
        console.log('用户无权限访问此页面')
        if (app.$message) {
          app.$message.error('无权限访问此页面')
        }
        // 重定向到用户有权限的页面
        return redirect('/')
        //return
      }
    }
    
    // 检查路由元信息中的权限
    if (route.meta && route.meta.permissions) {
      const metaPermissions = Array.isArray(route.meta.permissions) ? route.meta.permissions : [route.meta.permissions]
      console.log('路由元信息中的权限:', metaPermissions)
      
      const missingPermissions = metaPermissions.filter(perm => !hasPermission(userPermissions, perm))
      
      if (missingPermissions.length > 0) {
        console.log('用户缺少权限:', missingPermissions)
        if (app.$message) {
          app.$message.error(`缺少权限: ${missingPermissions.join(', ')}`)
        }
        //return redirect('/')
        return
      }
    }
    
    console.log('权限检查通过，允许访问')
    
  } catch (error) {
    console.log('权限检查失败:', error)
    
    // 清除缓存
    permissionCache = {
      timestamp: 0,
      data: null
    }
    
    // 清除token并跳转到登录页
    removeToken()
    return redirect('/login')
  }
}