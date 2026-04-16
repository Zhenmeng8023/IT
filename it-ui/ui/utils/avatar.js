const AVATAR_URL_SUFFIX = 'AvatarUrl'
const AVATAR_SUFFIX = 'Avatar'
const SNAKE_AVATAR_URL_SUFFIX = '_avatar_url'
const SNAKE_AVATAR_SUFFIX = '_avatar'
const POSITION_PATTERN = /(?:^|[&#])avatar-position=\d+(?:\.\d+)?,\d+(?:\.\d+)?/

function isNonEmptyString(value) {
  return typeof value === 'string' && value.trim() !== ''
}

function hasAvatarPosition(value) {
  const raw = String(value || '')
  try {
    return POSITION_PATTERN.test(decodeURIComponent(raw)) || POSITION_PATTERN.test(raw)
  } catch (error) {
    return POSITION_PATTERN.test(raw)
  }
}

function chooseAvatarValue(...values) {
  let fallback = ''
  for (const value of values) {
    if (!isNonEmptyString(value)) continue
    if (hasAvatarPosition(value)) {
      return value
    }
    if (!fallback) {
      fallback = value
    }
  }
  return fallback
}

export function pickAvatarUrl(...values) {
  return chooseAvatarValue(...values)
}

function toCamelAvatarKey(key, suffix) {
  const base = key.slice(0, -suffix.length)
  return base.replace(/_([a-z])/g, (_, char) => char.toUpperCase()) + AVATAR_SUFFIX
}

export function normalizeAvatarAliases(root) {
  const queue = [root]
  const visited = new WeakSet()

  while (queue.length) {
    const current = queue.shift()
    if (!current || typeof current !== 'object') continue
    if (visited.has(current)) continue
    visited.add(current)

    if (Array.isArray(current)) {
      current.forEach(item => {
        if (item && typeof item === 'object') queue.push(item)
      })
      continue
    }

    const normalizedAvatar = chooseAvatarValue(current.avatarUrl, current.avatar_url, current.avatar)
    if (normalizedAvatar) {
      current.avatarUrl = normalizedAvatar
      current.avatar = chooseAvatarValue(current.avatar, normalizedAvatar)
    }

    Object.keys(current).forEach(key => {
      const value = current[key]

      if (isNonEmptyString(value) && key.endsWith(AVATAR_URL_SUFFIX)) {
        const aliasKey = `${key.slice(0, -AVATAR_URL_SUFFIX.length)}${AVATAR_SUFFIX}`
        const normalized = chooseAvatarValue(current[aliasKey], value)
        current[key] = normalized
        current[aliasKey] = normalized
      }

      if (key !== 'avatar_url' && isNonEmptyString(value) && key.endsWith(SNAKE_AVATAR_URL_SUFFIX)) {
        const aliasKey = toCamelAvatarKey(key, SNAKE_AVATAR_URL_SUFFIX)
        const normalized = chooseAvatarValue(current[aliasKey], value)
        current[key] = normalized
        current[aliasKey] = normalized
      }

      if (isNonEmptyString(value) && key.endsWith(SNAKE_AVATAR_SUFFIX)) {
        const aliasKey = toCamelAvatarKey(key, SNAKE_AVATAR_SUFFIX)
        const normalized = chooseAvatarValue(current[aliasKey], value)
        current[key] = normalized
        current[aliasKey] = normalized
      }

      if (value && typeof value === 'object') {
        queue.push(value)
      }
    })
  }

  return root
}

export function installAvatarAliasInterceptor(axiosInstance) {
  if (!axiosInstance || axiosInstance.__itAvatarAliasInterceptorInstalled) {
    return
  }

  axiosInstance.interceptors.response.use(
    response => {
      if (response && response.data !== undefined) {
        normalizeAvatarAliases(response.data)
      }
      return response
    },
    error => Promise.reject(error)
  )
  axiosInstance.__itAvatarAliasInterceptorInstalled = true
}
