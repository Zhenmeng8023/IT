import axios from 'axios'
import { clearAuthState } from '@/utils/auth'

const request = axios.create({
  baseURL: 'http://localhost:18080/api',
  timeout: 120000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  config => config,
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
      clearAuthState()

      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }

    return Promise.reject(error)
  }
)

export default request
