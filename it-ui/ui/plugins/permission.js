import Vue from 'vue'
import { hasPermission } from '~/utils/permissionConfig'

export default (context, inject) => {
  // 注册v-permission指令
  Vue.directive('permission', {
    bind(el, binding) {
      const { value } = binding

      // 检查权限并处理元素
      checkPermission(el, value, context)
    },
    update(el, binding) {
      const { value } = binding

      // 检查权限并处理元素
      checkPermission(el, value, context)
    }
  })

  // 注册全局权限检查方法
  const hasPermissionMethod = (permission) => {
    let userPermissions = []
    // 尝试从Pinia实例获取用户权限
    if (context.app && context.app.pinia) {
      try {
        const { useUserStore } = require('~/store/user')
        const userStore = useUserStore(context.app.pinia)
        userPermissions = userStore.getPermissions || []
      } catch (error) {
        console.error('从Pinia store获取权限失败:', error)
      }
    }
    // 尝试从context.store获取用户权限（旧的存储结构）
    else if (context.store) {
      if (context.store.state && context.store.state.user) {
        userPermissions = context.store.state.user.permissions || []
      } else if (context.store.user) {
        userPermissions = context.store.user.permissions || []
      }
    }
    return hasPermission(userPermissions, permission)
  }

  // 使用inject方法注册全局方法（Nuxt.js推荐的方式）
  inject('hasPermission', hasPermissionMethod)

  // 不再直接修改Vue.prototype，避免重复定义错误
  // 在Nuxt.js中，inject方法会自动将方法添加到Vue实例中，可通过this.$hasPermission访问
}

// 检查权限并处理元素的函数
function checkPermission(el, value, context) {
  // 获取用户权限
  let userPermissions = []

  console.log('开始检查权限')

  // 尝试从Pinia实例获取用户权限（Nuxt.js + Pinia 正确方式）
  if (context.app && context.app.pinia) {
    console.log('Pinia实例存在')
    try {
      // 动态导入user store
      const { useUserStore } = require('~/store/user')
      const userStore = useUserStore(context.app.pinia)
      userPermissions = userStore.getPermissions || []
      console.log('从Pinia store获取的权限:', userPermissions)
    } catch (error) {
      console.error('从Pinia store获取权限失败:', error)
    }
  }
  // 尝试从context.store获取用户权限（旧的存储结构）
  else if (context.store) {
    console.log('context.store存在，检查其结构')
    if (context.store.state && context.store.state.user) {
      userPermissions = context.store.state.user.permissions || []
      console.log('从旧存储结构获取的权限:', userPermissions)
    } else if (context.store.user) {
      userPermissions = context.store.user.permissions || []
      console.log('从旧存储结构获取的权限:', userPermissions)
    }
  } else {
    console.log('未找到存储实例')
  }

  // 检查权限
  if (value) {
    const requiredPermissions = Array.isArray(value) ? value : [value]
    console.log('检查权限:', requiredPermissions, '用户权限:', userPermissions)

    const hasAccess = requiredPermissions.some(permission =>
      hasPermission(userPermissions, permission)
    )

    console.log('权限检查结果:', hasAccess)

    if (!hasAccess) {
      // 没有权限，隐藏元素
      el.style.display = 'none'
    } else {
      // 有权限，显示元素
      el.style.display = ''
    }
  }
}