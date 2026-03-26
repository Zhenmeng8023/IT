import axios from 'axios'

function getTokenFromCookie(name = 'token') {
  const cookies = document.cookie ? document.cookie.split('; ') : []
  for (const item of cookies) {
    const index = item.indexOf('=')
    const key = index > -1 ? item.substring(0, index) : item
    const value = index > -1 ? item.substring(index + 1) : ''
    if (key === name || key === 'Admin-Token') {
      return decodeURIComponent(value)
    }
  }
  return null
}

function getToken() {
  return (
    localStorage.getItem('token') ||
    localStorage.getItem('userToken') ||
    getTokenFromCookie('token')
  )
}

const request = axios.create({
  baseURL: 'http://localhost:18080/api',
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => response.data,
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

export default request
