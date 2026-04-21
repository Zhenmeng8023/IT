import axios from 'axios'
import { buildAuthHeaders, clearAuthState } from '@/utils/auth'
import { classifyAiError } from '@/utils/aiRuntime'
import { normalizeAvatarAliases } from '@/utils/avatar'
import { API_V1_BASE_URL } from '@/utils/backend'

const request = axios.create({
  baseURL: API_V1_BASE_URL,
  timeout: 120000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  config => {
    config.withCredentials = true
    config.headers = {
      ...(config.headers || {}),
      ...buildAuthHeaders(config.headers || {})
    }
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => {
    if (response && response.data !== undefined) {
      normalizeAvatarAliases(response.data)
    }
    return response.data
  },
  error => {
    error.aiError = classifyAiError(error)
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
      const redirect = `${window.location.pathname || '/'}${window.location.search || ''}`
      const loginUrl = `/login?redirect=${encodeURIComponent(redirect)}`

      if (window.location.pathname !== '/login') {
        window.location.href = loginUrl
      }
    }

    return Promise.reject(error)
  }
)

export default request
