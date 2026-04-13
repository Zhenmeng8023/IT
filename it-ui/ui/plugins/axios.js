import { MessageBox, Message } from 'element-ui'
import { buildAuthHeaders, clearAuthState } from '@/utils/auth'
import { classifyAiError } from '@/utils/aiRuntime'

export default ({ $axios, app }, inject) => {
  $axios.defaults.baseURL = 'http://localhost:18080/'
  $axios.defaults.withCredentials = true

  $axios.interceptors.request.use(
    config => {
      config.withCredentials = true
      config.headers = {
        ...(config.headers || {}),
        ...buildAuthHeaders(config.headers || {})
      }
      return config
    },
    error => {
      console.log(error) // for debug
      return Promise.reject(error)
    }
  )

  $axios.interceptors.response.use(
    response => {
      const res = response.data

      if (res.success !== undefined) {
        if (!res.success) {
          Message({
            message: res.message || 'Error',
            type: 'error',
            duration: 5 * 1000
          })
          console.info(response.config, res)
          return Promise.reject(new Error(res.message || 'Error'))
        }
        return res
      }
      else if (res.code !== undefined) {
        if (res.code !== 20000) {
          Message({
            message: res.message || 'Error',
            type: 'error',
            duration: 5 * 1000
          })

          if (res.code === 50008 || res.code === 50012 || res.code === 50014) {
            clearAuthState()
            MessageBox.confirm('You have been logged out, you can cancel to stay on this page, or log in again', 'Confirm logout', {
              confirmButtonText: 'Re-Login',
              cancelButtonText: 'Cancel',
              type: 'warning'
            }).then(() => {
              location.reload()
            })
          }
          console.info(response.config, res)
          return Promise.reject(new Error(res.message || 'Error'))
        }
        return res
      }
      else {
        return res
      }
    },
    error => {
      error.aiError = classifyAiError(error)
      console.log('请求失败:', error) // for debug
      if(error.response && error.response.status == 401) {
        clearAuthState()
        MessageBox.confirm('没有权限，将重新登录', '重新登录提示框', {
          confirmButtonText: '重新登录',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          location.reload()
        })
      } else {
        Message({
          message: error.message || 'Error',
          type: 'error',
          duration: 5 * 1000
        })
      }
      return Promise.reject(error)
    }
  )
}
