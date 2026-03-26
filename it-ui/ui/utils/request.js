import axios from 'axios'
import { getToken, removeToken } from '@/utils/auth'

const request = axios.create({
  baseURL: 'http://localhost:18080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
      config.headers['X-Token'] = token
    }
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => response.data,
  error => {
    const status = error?.response?.status
    const message =
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      error?.message ||
      ''

    if (
      process.client &&
      (status === 401 ||
        message.includes('未登录') ||
        message.includes('登录信息已失效'))
    ) {
      try {
        localStorage.removeItem('token')
        localStorage.removeItem('userToken')
        localStorage.removeItem('userInfo')
        localStorage.removeItem('userPermissions')
      } catch (e) {}

      removeToken()

      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }

    return Promise.reject(error)
  }
)

export default request