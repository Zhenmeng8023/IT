const SESSION_FLAG_KEY = 'serverSessionActive'
const USER_INFO_KEYS = ['userInfo', 'user']
const PERMISSION_KEYS = ['userPermissions']
const LEGACY_TOKEN_KEYS = ['token', 'userToken', 'Authorization', 'access_token']

function getLocalStorage() {
  if (!process.client) {
    return null
  }

  try {
    return window.localStorage
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
  if (!storage || !key) {
    return
  }

  try {
    storage.setItem(key, value == null ? '' : String(value))
  } catch (error) {}
}

function safeRemove(storage, key) {
  if (!storage || !key) {
    return
  }

  try {
    storage.removeItem(key)
  } catch (error) {}
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

function readFirstValue(keys = []) {
  const storage = getLocalStorage()
  if (!storage) {
    return ''
  }

  for (const key of keys) {
    const value = safeRead(storage, key)
    if (value) {
      return value
    }
  }

  return ''
}

function readFirstStorageValue(keys = []) {
  if (!process.client) {
    return ''
  }

  const storages = []
  try {
    if (window.localStorage) storages.push(window.localStorage)
  } catch (error) {}
  try {
    if (window.sessionStorage) storages.push(window.sessionStorage)
  } catch (error) {}

  for (const storage of storages) {
    for (const key of keys) {
      const value = safeRead(storage, key)
      if (value) {
        return value
      }
    }
  }

  return ''
}

function removeLegacyTokenKeys() {
  const storage = getLocalStorage()
  LEGACY_TOKEN_KEYS.forEach(key => safeRemove(storage, key))
}

export function getToken() {
  const marker = readFirstValue([SESSION_FLAG_KEY])
  if (marker) {
    return marker
  }

  return getStoredUserInfo() ? 'server-session' : null
}

export function getAccessToken() {
  const raw = readFirstStorageValue(LEGACY_TOKEN_KEYS)
  const token = raw ? String(raw).trim() : ''
  if (!token || token === 'server-session') {
    return ''
  }
  return token
}

export function buildAuthHeaders(extraHeaders = {}) {
  const token = getAccessToken()
  const headers = {
    ...(extraHeaders || {})
  }

  if (token && !headers.Authorization && !headers.authorization) {
    headers.Authorization = /^Bearer\s+/i.test(token) ? token : `Bearer ${token}`
  }

  return headers
}

export function setToken(token = 'server-session') {
  const storage = getLocalStorage()
  if (!storage) {
    return null
  }

  safeWrite(storage, SESSION_FLAG_KEY, token || 'server-session')
  removeLegacyTokenKeys()
  return token || 'server-session'
}

export function removeToken() {
  const storage = getLocalStorage()
  safeRemove(storage, SESSION_FLAG_KEY)
  removeLegacyTokenKeys()
  return null
}

export function getStoredUserInfo() {
  const raw = readFirstValue(USER_INFO_KEYS)
  return parseJson(raw, null)
}

export function setStoredUserInfo(userInfo) {
  const storage = getLocalStorage()
  if (!storage) {
    return null
  }

  if (!userInfo) {
    removeStoredUserInfo()
    return null
  }

  const serialized = JSON.stringify(userInfo)
  USER_INFO_KEYS.forEach(key => safeWrite(storage, key, serialized))
  setToken('server-session')
  return userInfo
}

export function removeStoredUserInfo() {
  const storage = getLocalStorage()
  USER_INFO_KEYS.forEach(key => safeRemove(storage, key))
}

export function getStoredPermissions() {
  const raw = readFirstValue(PERMISSION_KEYS)
  const parsed = parseJson(raw, [])
  return Array.isArray(parsed) ? parsed : []
}

export function setStoredPermissions(permissions = []) {
  const storage = getLocalStorage()
  if (!storage) {
    return []
  }

  const list = Array.isArray(permissions) ? permissions : []
  PERMISSION_KEYS.forEach(key => safeWrite(storage, key, JSON.stringify(list)))
  if (list.length > 0) {
    setToken('server-session')
  }
  return list
}

export function removeStoredPermissions() {
  const storage = getLocalStorage()
  PERMISSION_KEYS.forEach(key => safeRemove(storage, key))
}

export function getCurrentUser() {
  const user = getStoredUserInfo()
  return user && typeof user === 'object' ? user : null
}

export function getCurrentUserId() {
  const user = getCurrentUser()
  if (!user || typeof user !== 'object') {
    return null
  }

  const candidates = [
    user.id,
    user.userId,
    user.uid,
    user.user_id,
    user.memberId,
    user.accountId
  ]

  for (const candidate of candidates) {
    if (candidate !== undefined && candidate !== null && String(candidate).trim() !== '') {
      return Number(candidate) || candidate
    }
  }

  return null
}

export function clearAuthState() {
  removeToken()
  removeStoredUserInfo()
  removeStoredPermissions()
}
