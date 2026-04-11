import Cookies from 'js-cookie'

const TokenKey = 'Admin-Token'
const TOKEN_STORAGE_KEYS = ['userToken', 'token', 'Authorization', 'access_token']
const USER_STORAGE_KEYS = ['userInfo', 'user']
const PERMISSION_STORAGE_KEYS = ['userPermissions']

const memoryStorage = {}

function getClientStorage(type) {
  if (!process.client) {
    return null
  }

  try {
    return type === 'local' ? window.localStorage : window.sessionStorage
  } catch (error) {
    return null
  }
}

function safeRead(storage, key) {
  if (!storage || !key) {
    return ''
  }

  try {
    const value = storage.getItem(key)
    return value == null ? '' : String(value)
  } catch (error) {
    return ''
  }
}

function safeWrite(storage, key, value) {
  if (!key) {
    return
  }

  if (!storage) {
    memoryStorage[key] = value == null ? '' : String(value)
    return
  }

  try {
    storage.setItem(key, value == null ? '' : String(value))
  } catch (error) {
    memoryStorage[key] = value == null ? '' : String(value)
  }
}

function safeRemove(storage, key) {
  if (!key) {
    return
  }

  if (!storage) {
    delete memoryStorage[key]
    return
  }

  try {
    storage.removeItem(key)
  } catch (error) {
    delete memoryStorage[key]
  }
}

function readFirstValue(storage, keys = []) {
  for (const key of keys) {
    const value = safeRead(storage, key)
    if (value) {
      return value
    }
  }

  return ''
}

function parseJson(raw, fallback = null) {
  if (!raw) {
    return fallback
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    return fallback
  }
}

function clearLegacyCookie() {
  if (!process.client) {
    return
  }

  try {
    Cookies.remove(TokenKey, { path: '/' })
    Cookies.remove(TokenKey)
  } catch (error) {}
}

function clearLegacyLocalAuth() {
  const local = getClientStorage('local')
  ;[
    ...TOKEN_STORAGE_KEYS,
    ...USER_STORAGE_KEYS,
    ...PERMISSION_STORAGE_KEYS
  ].forEach(key => safeRemove(local, key))
  clearLegacyCookie()
}

function readSessionValue(keys = []) {
  if (!process.client) {
    return ''
  }

  const session = getClientStorage('session')
  const sessionValue = readFirstValue(session, keys)
  if (sessionValue) {
    return sessionValue
  }

  for (const key of keys) {
    if (memoryStorage[key]) {
      return memoryStorage[key]
    }
  }

  return ''
}

function writeSessionValue(keys = [], value = '') {
  if (!process.client) {
    return
  }

  const session = getClientStorage('session')
  const normalized = value == null ? '' : String(value)

  keys.forEach(key => {
    if (!key) {
      return
    }
    if (normalized) {
      safeWrite(session, key, normalized)
    } else {
      safeRemove(session, key)
    }
  })
}

function removeSessionValue(keys = []) {
  if (!process.client) {
    return
  }

  const session = getClientStorage('session')
  keys.forEach(key => safeRemove(session, key))
}

function pickUserId(user) {
  if (!user || typeof user !== 'object') {
    return null
  }

  const candidates = [
    user.id,
    user.userId,
    user.uid,
    user.user_id,
    user.memberId,
    user.accountId,
    user.sub
  ]

  for (const candidate of candidates) {
    if (candidate !== undefined && candidate !== null && String(candidate).trim() !== '') {
      return Number(candidate) || candidate
    }
  }

  return null
}

export function parseJwtPayload(token) {
  if (!process.client || !token || typeof token !== 'string' || token.split('.').length < 2) {
    return null
  }

  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const normalized = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')
    const text = decodeURIComponent(
      Array.from(window.atob(normalized))
        .map(ch => `%${ch.charCodeAt(0).toString(16).padStart(2, '0')}`)
        .join('')
    )
    return parseJson(text, null)
  } catch (error) {
    return null
  }
}

export function getToken() {
  if (!process.client) {
    return null
  }

  return readSessionValue(TOKEN_STORAGE_KEYS) || null
}

export function setToken(token) {
  const normalized = token == null ? '' : String(token).trim()

  if (!normalized) {
    removeToken()
    return ''
  }

  writeSessionValue(['token', 'userToken'], normalized)
  clearLegacyLocalAuth()
  return normalized
}

export function removeToken() {
  removeSessionValue(TOKEN_STORAGE_KEYS)
  clearLegacyLocalAuth()
  return null
}

export function getStoredUserInfo() {
  const raw = readSessionValue(USER_STORAGE_KEYS)
  return parseJson(raw, null)
}

export function setStoredUserInfo(userInfo) {
  if (!userInfo) {
    removeStoredUserInfo()
    return null
  }

  writeSessionValue(['userInfo', 'user'], JSON.stringify(userInfo))
  const local = getClientStorage('local')
  safeRemove(local, 'userInfo')
  safeRemove(local, 'user')
  return userInfo
}

export function removeStoredUserInfo() {
  removeSessionValue(USER_STORAGE_KEYS)
}

export function getStoredPermissions() {
  const raw = readSessionValue(PERMISSION_STORAGE_KEYS)
  const parsed = parseJson(raw, [])
  return Array.isArray(parsed) ? parsed : []
}

export function setStoredPermissions(permissions = []) {
  const list = Array.isArray(permissions) ? permissions : []
  writeSessionValue(['userPermissions'], JSON.stringify(list))
  const local = getClientStorage('local')
  safeRemove(local, 'userPermissions')
  return list
}

export function removeStoredPermissions() {
  removeSessionValue(PERMISSION_STORAGE_KEYS)
}

export function getCurrentUser() {
  const storedUser = getStoredUserInfo()
  if (storedUser && typeof storedUser === 'object') {
    return storedUser
  }

  const payload = parseJwtPayload(getToken() || '')
  return payload && typeof payload === 'object' ? payload : null
}

export function getCurrentUserId() {
  return pickUserId(getCurrentUser())
}

export function clearAuthState() {
  removeSessionValue([
    ...TOKEN_STORAGE_KEYS,
    ...USER_STORAGE_KEYS,
    ...PERMISSION_STORAGE_KEYS
  ])
  clearLegacyLocalAuth()
}
