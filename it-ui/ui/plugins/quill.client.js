/**
 * Quill 富文本编辑器客户端插件
 * 文件名必须包含 .client.js 后缀，确保只在客户端加载
 */

import Vue from 'vue'
import Quill from 'quill'
import 'quill/dist/quill.snow.css'
import 'quill/dist/quill.core.css'
import 'quill/dist/quill.bubble.css'

// 将 Quill 挂载到 Vue 原型上
Vue.prototype.$Quill = Quill

// 注入到 Nuxt 上下文
export default ({ app }, inject) => {
  inject('quill', Quill)
}

console.log('✅ Quill 客户端插件已加载')