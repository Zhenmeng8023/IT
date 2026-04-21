const DEFAULT_API_BASE_URL = 'http://localhost:18080/'

function ensureTrailingSlash(value = '') {
  const normalized = String(value || '')
  return normalized.endsWith('/') ? normalized : `${normalized}/`
}

export const API_BASE_URL = ensureTrailingSlash(
  process.env.API_BASE_URL || process.env.NUXT_ENV_API_BASE_URL || DEFAULT_API_BASE_URL
)

export const API_ROOT = String(API_BASE_URL).replace(/\/$/, '')

export const API_V1_BASE_URL = `${API_ROOT}/api`
