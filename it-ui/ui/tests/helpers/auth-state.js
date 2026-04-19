const fs = require('fs')
const path = require('path')

const ROLES = Object.freeze({
  USER: 'user',
  ADMIN: 'admin'
})

const ROLE_ENV_MAP = Object.freeze({
  [ROLES.USER]: {
    username: 'E2E_USER_USERNAME',
    password: 'E2E_USER_PASSWORD',
    defaultUsername: 'szw',
    defaultPassword: '123123123'
  },
  [ROLES.ADMIN]: {
    username: 'E2E_ADMIN_USERNAME',
    password: 'E2E_ADMIN_PASSWORD',
    defaultUsername: 'admin',
    defaultPassword: '123123123'
  }
})

const authStateDir = path.join(__dirname, '..', 'e2e', '.auth')
const authStatePaths = Object.freeze({
  [ROLES.USER]: path.join(authStateDir, 'user.json'),
  [ROLES.ADMIN]: path.join(authStateDir, 'admin.json')
})

function emptyStorageState() {
  return {
    cookies: [],
    origins: []
  }
}

function getFrontendBaseURL() {
  return process.env.E2E_BASE_URL || 'http://localhost:3000'
}

function getBackendBaseURL() {
  return process.env.E2E_API_BASE_URL || 'http://localhost:18080'
}

function getRoleCredentials(role) {
  const envConfig = ROLE_ENV_MAP[role]
  if (!envConfig) {
    return null
  }

  const username = (process.env[envConfig.username] || envConfig.defaultUsername || '').trim()
  const password = (process.env[envConfig.password] || envConfig.defaultPassword || '').trim()

  if (!username || !password) {
    return null
  }

  return { username, password }
}

function isRoleCredentialConfigured(role) {
  return Boolean(getRoleCredentials(role))
}

function resolveRoleFromProject(projectName = '') {
  if (projectName.includes('admin')) {
    return ROLES.ADMIN
  }
  if (projectName.includes('user')) {
    return ROLES.USER
  }
  return 'guest'
}

function ensureAuthStateDir() {
  fs.mkdirSync(authStateDir, { recursive: true })
}

function writeEmptyState(role) {
  const statePath = authStatePaths[role]
  fs.writeFileSync(statePath, `${JSON.stringify(emptyStorageState(), null, 2)}\n`, 'utf8')
  return statePath
}

function getRoleStateSummary(role) {
  const statePath = authStatePaths[role]
  if (!statePath || !fs.existsSync(statePath)) {
    return {
      role,
      exists: false,
      hasCookies: false,
      hasOrigins: false,
      path: statePath
    }
  }

  try {
    const parsed = JSON.parse(fs.readFileSync(statePath, 'utf8'))
    const cookieCount = Array.isArray(parsed.cookies) ? parsed.cookies.length : 0
    const originCount = Array.isArray(parsed.origins) ? parsed.origins.length : 0
    return {
      role,
      exists: true,
      hasCookies: cookieCount > 0,
      hasOrigins: originCount > 0,
      path: statePath
    }
  } catch (error) {
    return {
      role,
      exists: true,
      hasCookies: false,
      hasOrigins: false,
      path: statePath,
      parseError: error.message
    }
  }
}

async function setupRoleStorageState({ role, playwright }) {
  ensureAuthStateDir()

  const statePath = authStatePaths[role]
  const credentials = getRoleCredentials(role)

  if (!statePath) {
    return { role, ready: false, reason: 'unsupported-role', statePath: '' }
  }

  if (!credentials) {
    writeEmptyState(role)
    return {
      role,
      ready: false,
      reason: `missing-credentials(${ROLE_ENV_MAP[role].username}/${ROLE_ENV_MAP[role].password})`,
      statePath
    }
  }

  const apiContext = await playwright.request.newContext({
    baseURL: getBackendBaseURL(),
    extraHTTPHeaders: {
      'Content-Type': 'application/json'
    }
  })

  try {
    const loginResponse = await apiContext.post('/login', {
      data: {
        username: credentials.username,
        password: credentials.password
      }
    })

    if (!loginResponse.ok()) {
      writeEmptyState(role)
      return {
        role,
        ready: false,
        reason: `login-failed(${loginResponse.status()})`,
        statePath
      }
    }

    const userResponse = await apiContext.get('/api/users/current')
    if (!userResponse.ok()) {
      writeEmptyState(role)
      return {
        role,
        ready: false,
        reason: `session-check-failed(${userResponse.status()})`,
        statePath
      }
    }

    await apiContext.storageState({ path: statePath })

    return {
      role,
      ready: true,
      reason: 'ok',
      statePath
    }
  } catch (error) {
    writeEmptyState(role)
    return {
      role,
      ready: false,
      reason: `error(${error.message})`,
      statePath
    }
  } finally {
    await apiContext.dispose()
  }
}

module.exports = {
  ROLES,
  authStateDir,
  authStatePaths,
  getFrontendBaseURL,
  getBackendBaseURL,
  getRoleCredentials,
  isRoleCredentialConfigured,
  resolveRoleFromProject,
  ensureAuthStateDir,
  writeEmptyState,
  getRoleStateSummary,
  setupRoleStorageState
}
