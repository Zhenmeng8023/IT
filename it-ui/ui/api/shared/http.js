import axios from 'axios'
import { buildAuthHeaders } from '@/utils/auth'
import { API_BASE_URL } from '@/utils/backend'
import { installAvatarAliasInterceptor } from '@/utils/avatar'

const http = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true
})

http.interceptors.request.use(config => {
  config.withCredentials = true
  config.headers = buildAuthHeaders(config.headers || {})
  return config
})

installAvatarAliasInterceptor(http)

export default http
