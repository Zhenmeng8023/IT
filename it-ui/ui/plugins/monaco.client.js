import Vue from 'vue'
import 'monaco-editor/min/vs/editor/editor.main.css'

let monacoPromise = null

const loadMonaco = () => {
  if (!process.client) {
    return Promise.resolve(null)
  }

  if (!monacoPromise) {
    monacoPromise = import('monaco-editor/esm/vs/editor/editor.api')
      .then(module => module.default || module)
  }

  return monacoPromise
}

Vue.prototype.$loadMonaco = loadMonaco

export default (context, inject) => {
  inject('loadMonaco', loadMonaco)
}
