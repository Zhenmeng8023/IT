import Cookies from 'js-cookie'

const TokenKey = 'Admin-Token'

// utils/auth.js
export function getToken(req) {
  console.log('=== getToken() 开始执行 ===')
  
  // 服务端环境下从请求头获取
  if (process.server && req) {
    console.log('服务端环境，尝试从请求头获取token')
    const cookieHeader = req.headers.cookie
    if (cookieHeader) {
      const cookies = {}
      cookieHeader.split(';').forEach(cookie => {
        const parts = cookie.split('=')
        if (parts.length === 2) {
          cookies[parts[0].trim()] = parts[1].trim()
        }
      })
      const token = cookies[TokenKey]
      console.log('从请求头获取token:', token ? '成功' : '失败', token)
      if (token) {
        console.log('=== getToken() 从请求头返回 ===')
        return token
      }
    }
  }
  
  // 首先从Cookies中获取token
  let tokenFromCookie = null
  try {
    tokenFromCookie = Cookies.get(TokenKey)
    console.log('从Cookies获取token:', tokenFromCookie ? '成功' : '失败', tokenFromCookie)
  } catch (error) {
    console.error('从Cookies获取token失败:', error)
  }
  
  if (tokenFromCookie) {
    console.log('=== getToken() 从Cookies返回 ===')
    return tokenFromCookie
  }
  
  // 如果Cookies中没有，尝试从localStorage中获取（仅在客户端）
  if (process.client) {
    console.log('客户端环境，尝试从localStorage获取')
    try {
      const tokenFromLocalStorage = localStorage.getItem('userToken')
      console.log('从localStorage获取token:', tokenFromLocalStorage ? '成功' : '失败', tokenFromLocalStorage)
      
      if (tokenFromLocalStorage) {
        // 将localStorage中的token同步到Cookies中
        console.log('将localStorage token同步到Cookies')
        setToken(tokenFromLocalStorage)
        console.log('=== getToken() 从localStorage返回 ===')
        return tokenFromLocalStorage
      }
    } catch (error) {
      console.error('从localStorage获取token失败:', error)
    }
  } else {
    console.log('服务端环境，跳过localStorage检查')
  }
  
  console.log('=== getToken() 无token返回null ===')
  return null
}

export function setToken(token) {
  // 设置cookie属性，确保token能正确存储和被访问
  const cookieOptions = {
    expires: 7, // 7天过期时间
    path: '/', // 全站点可访问
    domain: '', // 空字符串表示当前域名
    secure: false, // 开发环境设置为false，生产环境建议设置为true
    sameSite: 'Lax' // 防止CSRF攻击
  }
  
  console.log('设置Cookies token:', token)
  console.log('Cookie选项:', cookieOptions)
  
  const result = Cookies.set(TokenKey, token, cookieOptions)
  console.log('Cookies.set() 返回结果:', result)
  
  // 验证设置是否成功
  const verifyToken = Cookies.get(TokenKey)
  console.log('验证Cookies设置结果:', verifyToken === token ? '成功' : '失败')
  
  // 再次验证不同路径下的访问
  if (process.client) {
    setTimeout(() => {
      const tokenCheck = Cookies.get(TokenKey)
      console.log('1秒后验证Cookies token:', tokenCheck)
    }, 1000)
  }
  
  return result
}

export function removeToken() {
  // 清除cookie时也需要指定路径
  const cookieOptions = {
    path: '/'
  }
  
  console.log('清除Cookies token')
  const result = Cookies.remove(TokenKey, cookieOptions)
  
  // 同时清除localStorage中的token（客户端环境）
  if (process.client) {
    localStorage.removeItem('userToken')
    console.log('清除localStorage token')
  }
  
  console.log('Cookies.remove() 返回结果:', result)
  return result
}