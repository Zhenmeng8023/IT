const { expect } = require('@playwright/test')

function normalizePathname(pageUrl) {
  try {
    return new URL(pageUrl).pathname
  } catch (error) {
    return ''
  }
}

function escapeRegExp(source) {
  return source.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

async function expectPageShellReady(page) {
  await expect(page.locator('body')).toBeVisible()
}

async function expectCurrentPath(page, expectedPath) {
  const safePath = expectedPath || '/'
  await expect(page).toHaveURL(new RegExp(`${escapeRegExp(safePath)}(?:\\?.*)?$`))
}

async function expectPageOpen(page, expectedPath) {
  await expectPageShellReady(page)
  await expectCurrentPath(page, expectedPath)
}

async function expectToastMessage(page, messagePattern, options = {}) {
  const locator = page.locator('.el-message').filter({ hasText: messagePattern }).last()
  await expect(locator).toBeVisible({ timeout: options.timeout || 10000 })
  return locator
}

async function inspectAdminAccess(page, expectedPath) {
  await expectPageShellReady(page)

  const currentPath = normalizePathname(page.url())
  if (currentPath === expectedPath) {
    return { status: 'allowed', currentPath }
  }
  if (currentPath === '/login') {
    return { status: 'blocked-login', currentPath }
  }
  if (currentPath === '/noPermission') {
    return { status: 'blocked-permission', currentPath }
  }

  return { status: 'unexpected', currentPath }
}

module.exports = {
  expectPageShellReady,
  expectCurrentPath,
  expectPageOpen,
  expectToastMessage,
  inspectAdminAccess
}
