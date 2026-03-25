import { getToken } from '@/utils/auth'

export default function({ $axios }) {
  $axios.defaults.baseURL = 'http://39.102.72.27:18080/'

  $axios.interceptors.request.use(config => {
    const token = getToken()
    if (token) {
      config.headers['X-Token'] = token
    }
    return config
  })
}