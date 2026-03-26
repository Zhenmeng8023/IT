import axios from 'axios'
import { getToken } from '@/utils/auth'

const request = axios.create({
  baseURL: 'http://localhost:18080/api',
  timeout: 5000,
  withCredentials: true,
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
    console.error('请求错误:', error)

    if (process.client && error.response && error.response.status === 401) {
      window.location.href = '/login'
    }

    return Promise.reject(error)
  }
)

export default request