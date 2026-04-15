import Vue from 'vue'
import { hasAnyPermission, normalizePermissionList } from '~/utils/permissionConfig'

function readUserPermissions(context) {
  if (context.app && context.app.pinia) {
    try {
      const { useUserStore } = require('~/store/user')
      const userStore = useUserStore(context.app.pinia)
      return normalizePermissionList(userStore.getPermissions || userStore.permissions || [])
    } catch (error) {
      return []
    }
  }

  if (context.store && context.store.state && context.store.state.user) {
    return normalizePermissionList(context.store.state.user.permissions || [])
  }

  return []
}

function applyPermissionState(el, binding, context) {
  const requiredPermissions = normalizePermissionList(binding.value)

  if (requiredPermissions.length === 0) {
    el.style.display = ''
    return
  }

  const visible = hasAnyPermission(readUserPermissions(context), requiredPermissions)
  el.style.display = visible ? '' : 'none'
}

export default (context, inject) => {
  Vue.directive('permission', {
    bind(el, binding) {
      applyPermissionState(el, binding, context)
    },
    update(el, binding) {
      applyPermissionState(el, binding, context)
    }
  })

  const hasPermissionMethod = (permission) => {
    return hasAnyPermission(readUserPermissions(context), permission)
  }

  inject('hasPermission', hasPermissionMethod)
  inject('canAccessPermissions', (permissions) => hasAnyPermission(readUserPermissions(context), permissions))
}
