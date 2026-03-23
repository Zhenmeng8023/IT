import { MessageBox, Message } from 'element-ui'
import { getToken,setToken } from '@/utils/auth'

export default ({ $axios, app }, inject) => {
  // 设置基础URL
  $axios.defaults.baseURL = 'http://39.102.72.27:18080/'

  // request interceptor
  $axios.interceptors.request.use(
    config => {
      try {
        // 在服务端环境下从app.context.req获取请求对象
        const req = process.server ? app.context.req : null
        const token = getToken(req)
        if (token) {
          // let each request carry token
          // ['X-Token'] is a custom headers key
          // please modify it according to the actual situation
          config.headers['X-Token'] = token
        }
      } catch (error) {
        console.error('获取token失败:', error)
      }
      return config
    },
    error => {
      // do something with request error
      console.log(error) // for debug
      return Promise.reject(error)
    }
  )

  // response interceptor
  $axios.interceptors.response.use(
    /**
     * If you want to get http information such as headers or status
     * Please return  response => response
    */

    /**
     * Determine the request status by custom code
     * Here is just an example
     * You can also judge the status by HTTP Status Code
     */
    response => {
      const res = response.data

      // 检查后端返回的响应格式
      // 如果有success字段，使用success字段判断
      if (res.success !== undefined) {
        if (!res.success) {
          Message({
            message: res.message || 'Error',
            type: 'error',
            duration: 5 * 1000
          })
          // 请求异常时，打印提示(路径等,数据等)
          console.info(response.config, res)
          return Promise.reject(new Error(res.message || 'Error'))
        } else {
          // 保存token
          if(res.other && res.other.token) {
            setToken(res.other.token)
          }
          return res
        }
      }
      // 否则使用code字段判断（兼容旧格式）
      else if (res.code !== undefined) {
        if (res.code !== 20000) {
          Message({
            message: res.message || 'Error',
            type: 'error',
            duration: 5 * 1000
          })

          // 50008: Illegal token; 50012: Other clients logged in; 50014: Token expired;
          if (res.code === 50008 || res.code === 50012 || res.code === 50014) {
            // to re-login
            MessageBox.confirm('You have been logged out, you can cancel to stay on this page, or log in again', 'Confirm logout', {
              confirmButtonText: 'Re-Login',
              cancelButtonText: 'Cancel',
              type: 'warning'
            }).then(() => {
              location.reload()
            })
          }
          // 请求异常时，打印提示(路径等,数据等)
          console.info(response.config, res)
          return Promise.reject(new Error(res.message || 'Error'))
        } else {
          // 保存token
          if(res.other && res.other.token) {
            setToken(res.other.token)
          }

          return res
        }
      }
      // 处理直接返回数据的情况（如用户信息）
      else {
        return res
      }
    },
    error => {
      console.log('请求失败:', error) // for debug
      if(error.response && error.response.status == 401) {
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


  //inject('request', request)
}