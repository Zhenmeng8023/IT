/**
 * 权限指令插件
 * 实现 v-permission 指令，用于控制页面元素的显示/隐藏
 */
import Vue from 'vue'
import { hasPermission, hasAnyPermission, hasAllPermissions } from '@/utils/permission'

/**
 * 权限指令 - v-permission
 * 用法:
 * 1. 单个权限: v-permission="'blog:create'"
 * 2. 多个权限（任意一个）: v-permission="['blog:create', 'blog:update']"
 * 3. 多个权限（全部需要）: v-permission="{ all: ['blog:create', 'blog:update'] }"
 */
Vue.directive('permission', {
  // 指令绑定到元素时执行
  inserted(el, binding) {
    checkPermission(el, binding)
  },
  // 指令所在组件更新时执行
  update(el, binding) {
    checkPermission(el, binding)
  }
})

/**
 * 检查权限并控制元素显示/隐藏
 * @param {HTMLElement} el - 指令绑定的元素
 * @param {Object} binding - 指令绑定信息
 */
function checkPermission(el, binding) {
  const permission = binding.value
  let hasAccess = false

  if (!permission) {
    // 没有指定权限，默认显示
    hasAccess = true
  } else if (typeof permission === 'string') {
    // 单个权限
    hasAccess = hasPermission(permission)
  } else if (Array.isArray(permission)) {
    // 多个权限，任意一个即可
    hasAccess = hasAnyPermission(permission)
  } else if (typeof permission === 'object' && permission.all) {
    // 多个权限，需要全部拥有
    hasAccess = hasAllPermissions(permission.all)
  }

  if (!hasAccess) {
    // 没有权限，隐藏元素
    el.style.display = 'none'
  }
}

/**
 * 权限按钮组件
 * 基于权限的按钮显示控制
 */
Vue.component('PermissionButton', {
  props: {
    // 权限代码
    permission: {
      type: [String, Array, Object],
      required: true
    },
    // 按钮类型
    type: {
      type: String,
      default: 'primary'
    },
    // 按钮大小
    size: {
      type: String,
      default: 'medium'
    },
    // 是否禁用
    disabled: {
      type: Boolean,
      default: false
    }
  },
  template: `
    <el-button
      :type="type"
      :size="size"
      :disabled="disabled || !hasAccess"
      :style="{ display: hasAccess ? 'inline-block' : 'none' }"
      v-on="$listeners"
    >
      <slot></slot>
    </el-button>
  `,
  computed: {
    hasAccess() {
      const permission = this.permission
      
      if (typeof permission === 'string') {
        return hasPermission(permission)
      } else if (Array.isArray(permission)) {
        return hasAnyPermission(permission)
      } else if (typeof permission === 'object' && permission.all) {
        return hasAllPermissions(permission.all)
      }
      
      return false
    }
  }
})

export default {
  install(Vue) {
    // 注册权限指令
    Vue.directive('permission', {
      inserted(el, binding) {
        checkPermission(el, binding)
      },
      update(el, binding) {
        checkPermission(el, binding)
      }
    })
    
    // 注册权限按钮组件
    Vue.component('PermissionButton', {
      props: {
        permission: {
          type: [String, Array, Object],
          required: true
        },
        type: {
          type: String,
          default: 'primary'
        },
        size: {
          type: String,
          default: 'medium'
        },
        disabled: {
          type: Boolean,
          default: false
        }
      },
      template: `
        <el-button
          :type="type"
          :size="size"
          :disabled="disabled || !hasAccess"
          :style="{ display: hasAccess ? 'inline-block' : 'none' }"
          v-on="$listeners"
        >
          <slot></slot>
        </el-button>
      `,
      computed: {
        hasAccess() {
          const permission = this.permission
          
          if (typeof permission === 'string') {
            return hasPermission(permission)
          } else if (Array.isArray(permission)) {
            return hasAnyPermission(permission)
          } else if (typeof permission === 'object' && permission.all) {
            return hasAllPermissions(permission.all)
          }
          
          return false
        }
      }
    })
  }
}