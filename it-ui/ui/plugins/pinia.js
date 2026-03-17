const Vue = require('vue')
const { createPinia, PiniaVuePlugin } = require('pinia')

// 安装 PiniaVuePlugin
Vue.use(PiniaVuePlugin)

// 创建 Pinia 实例
const pinia = createPinia()

function piniaPlugin(context) {
  // 将 pinia 实例添加到上下文
  context.app.pinia = pinia
  // 在客户端，将 pinia 实例挂载到 window 对象
  if (process.client) {
    window.pinia = pinia
  }
}

module.exports = piniaPlugin
module.exports.pinia = pinia