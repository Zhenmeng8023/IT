import { MessageBox, Message } from 'element-ui'
import { getToken,setToken } from '@/utils/auth'

export default ({ $axios }, inject) => {

  // request interceptor
  $axios.interceptors.request.use(
    config => {
      if (getToken()) {
        // let each request carry token
        // ['X-Token'] is a custom headers key
        // please modify it according to the actual situation
        config.headers['X-Token'] = getToken()
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

      // if the custom code is not 20000, it is judged as an error.
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
    },
    error => {
      console.log('666err' + error) // for debug
      // Message({
      //   message: error.message + "66666",
      //   type: 'error',
      //   duration: 5 * 1000
      // })
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
