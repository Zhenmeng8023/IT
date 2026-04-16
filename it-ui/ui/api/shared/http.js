import axios from 'axios'
import { buildAuthHeaders } from '@/utils/auth'
import { installAvatarAliasInterceptor } from '@/utils/avatar'

const http = axios.create({
  baseURL: 'http://localhost:18080/',
  withCredentials: true
})

http.interceptors.request.use(config => {
  config.withCredentials = true
  config.headers = buildAuthHeaders(config.headers || {})
  return config
})

installAvatarAliasInterceptor(http)

export default http
