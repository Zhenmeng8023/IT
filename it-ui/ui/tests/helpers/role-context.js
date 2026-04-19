const { ROLES, authStatePaths, getBackendBaseURL } = require('./auth-state')

function normalizePermissionPayload(payload) {
  if (Array.isArray(payload)) {
    return payload
  }
  if (payload && Array.isArray(payload.data)) {
    return payload.data
  }
  return []
}

async function readResponseJson(response) {
  const text = await response.text()
  try {
    return text ? JSON.parse(text) : null
  } catch (error) {
    return null
  }
}

async function resolveFrontendAuthSeed(context, role) {
  const userResponse = await context.request.get(`${getBackendBaseURL()}/api/users/current`)
  if (!userResponse.ok()) {
    throw new Error(`resolve current user failed: status=${userResponse.status()}`)
  }

  const user = await readResponseJson(userResponse)
  if (!user || !user.id) {
    throw new Error('resolve current user failed: empty payload')
  }

  const permissionsResponse = await context.request.get(
    `${getBackendBaseURL()}/api/roles/${user.roleId}/permissions`
  )
  const permissionsPayload = permissionsResponse.ok()
    ? await readResponseJson(permissionsResponse)
    : []

  return {
    role,
    user,
    permissions: normalizePermissionPayload(permissionsPayload)
  }
}

async function seedFrontendAuthState(page, authSeed) {
  await page.addInitScript(seed => {
    if (!seed || !seed.user) {
      return
    }

    window.localStorage.setItem('serverSessionActive', 'server-session')
    window.localStorage.setItem('userInfo', JSON.stringify(seed.user))
    window.localStorage.setItem('user', JSON.stringify(seed.user))
    window.localStorage.setItem('userPermissions', JSON.stringify(seed.permissions || []))
  }, authSeed)
}

function unwrapPayload(payload) {
  if (payload === null || payload === undefined) {
    return payload
  }
  if (Array.isArray(payload)) {
    return payload
  }
  if (payload.data !== undefined) {
    return unwrapPayload(payload.data)
  }
  return payload
}

function toList(payload) {
  const unwrapped = unwrapPayload(payload)
  if (Array.isArray(unwrapped)) {
    return unwrapped
  }
  if (!unwrapped || typeof unwrapped !== 'object') {
    return []
  }
  const candidates = [
    unwrapped.list,
    unwrapped.items,
    unwrapped.rows,
    unwrapped.records,
    unwrapped.content
  ]
  for (const candidate of candidates) {
    if (Array.isArray(candidate)) {
      return candidate
    }
  }
  return []
}

async function createRoleSession(browser, role = 'guest', basePath = '/') {
  const contextOptions = role === 'guest' ? {} : { storageState: authStatePaths[role] }
  const context = await browser.newContext(contextOptions)
  const page = await context.newPage()

  if (role !== 'guest') {
    const authSeed = await resolveFrontendAuthSeed(context, role)
    await seedFrontendAuthState(page, authSeed)
  }

  await page.goto(basePath, { waitUntil: 'domcontentloaded' })
  return {
    role,
    context,
    page,
    async close() {
      await context.close()
    }
  }
}

async function fetchJson(page, url, init = {}) {
  const headers = { ...(init.headers || {}) }
  const requestInit = {
    ...init,
    headers
  }

  if (
    requestInit.body !== undefined &&
    requestInit.body !== null &&
    typeof requestInit.body !== 'string'
  ) {
    requestInit.body = JSON.stringify(requestInit.body)
    if (!headers['Content-Type'] && !headers['content-type']) {
      headers['Content-Type'] = 'application/json'
    }
  }

  const fetchInit = { ...requestInit }
  if (Object.prototype.hasOwnProperty.call(fetchInit, 'body')) {
    fetchInit.data = fetchInit.body
    delete fetchInit.body
  }

  const response = await page.context().request.fetch(url, fetchInit)
  const text = await response.text()
  let data = null
  try {
    data = text ? JSON.parse(text) : null
  } catch (error) {
    data = null
  }

  return {
    ok: response.ok(),
    status: response.status(),
    text,
    data
  }
}

async function waitForListItem(page, url, predicate, options = {}) {
  const timeout = options.timeout || 20000
  const interval = options.interval || 500
  const startedAt = Date.now()

  while (Date.now() - startedAt < timeout) {
    const result = await fetchJson(page, url, options.init || {})
    const list = toList(result.data)
    const matchedItem = list.find(predicate)
    if (matchedItem) {
      return { result, list, matchedItem }
    }
    await page.waitForTimeout(interval)
  }

  throw new Error(`Timed out waiting for item from ${url}`)
}

function buildUniqueName(prefix) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

module.exports = {
  ROLES,
  unwrapPayload,
  toList,
  createRoleSession,
  fetchJson,
  waitForListItem,
  buildUniqueName
}
