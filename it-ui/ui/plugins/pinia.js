// plugins/pinia.js
import { createPinia, PiniaVuePlugin } from 'pinia'
import Vue from 'vue'

// 安装 PiniaVuePlugin
Vue.use(PiniaVuePlugin)

// 创建 pinia 实例
const pinia = createPinia()

export default (context) => {
  context.app.pinia = pinia
}

// 导出 pinia 实例
export { pinia }