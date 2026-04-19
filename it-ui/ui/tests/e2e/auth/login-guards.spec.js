const { test, expect } = require('../../fixtures/e2e.fixture')
const { getRoleCredentials, ROLES } = require('../../helpers/auth-state')
const { resolvePrerequisites, getBackendBaseURL } = require('../../helpers/e2e-env')
const { fetchJson } = require('../../helpers/role-context')
const { ROUTE_PATHS } = require('../../helpers/navigation')

async function assertCurrentUser(page, expectedUsername) {
  const result = await fetchJson(page, `${getBackendBaseURL()}/api/users/current`)

  expect(result.status).toBe(200)

  const user = result.data && result.data.data !== undefined
    ? result.data.data
    : result.data

  expect(user).toBeTruthy()
  expect(String(user.username || '')).toContain(expectedUsername)
}

async function loginThroughUi(page, credentials) {
  await page.getByPlaceholder('请输入用户名').fill(credentials.username)
  await page.getByPlaceholder('请输入密码').fill(credentials.password)
  await page.getByRole('button', { name: '立即登录' }).click()
}

test.describe('auth login and route guards', () => {
  test('guest is redirected to login for protected routes', async ({ page, playwright }, testInfo) => {
    test.skip(testInfo.project.name !== 'smoke-guest', 'guest project only')

    const prereq = await resolvePrerequisites(playwright, [])
    test.skip(!prereq.backend.available, `backend unavailable: ${prereq.backend.reason}`)

    await page.goto(ROUTE_PATHS.circleManage, { waitUntil: 'commit' })
    await expect(page).toHaveURL(/\/login(?:\?|$)/)
    await expect(page.getByPlaceholder('请输入用户名')).toBeVisible()
  })

  test('regular user can login and enter blog write page', async ({ page, playwright }, testInfo) => {
    test.skip(testInfo.project.name !== 'smoke-guest', 'guest project only')

    const prereq = await resolvePrerequisites(playwright, [])
    test.skip(!prereq.backend.available, `backend unavailable: ${prereq.backend.reason}`)

    const credentials = getRoleCredentials(ROLES.USER)
    test.skip(!credentials, 'missing user credentials')

    await page.goto(`/login?redirect=${encodeURIComponent(ROUTE_PATHS.blogWrite)}`, {
      waitUntil: 'domcontentloaded'
    })

    await loginThroughUi(page, credentials)
    await expect(page).toHaveURL(new RegExp(`${ROUTE_PATHS.blogWrite.replace('/', '\\/')}(?:\\?.*)?$`))
    await expect(page.getByPlaceholder('请输入博客标题')).toBeVisible()
    await assertCurrentUser(page, credentials.username)
  })

  test('admin can login and reach circle manage page', async ({ page, playwright }, testInfo) => {
    test.skip(testInfo.project.name !== 'smoke-guest', 'guest project only')

    const prereq = await resolvePrerequisites(playwright, [])
    test.skip(!prereq.backend.available, `backend unavailable: ${prereq.backend.reason}`)

    const credentials = getRoleCredentials(ROLES.ADMIN)
    test.skip(!credentials, 'missing admin credentials')

    await page.goto(`/login?redirect=${encodeURIComponent(ROUTE_PATHS.circleManage)}`, {
      waitUntil: 'domcontentloaded'
    })

    await loginThroughUi(page, credentials)
    await expect(page).toHaveURL(new RegExp(`${ROUTE_PATHS.circleManage.replace('/', '\\/')}(?:\\?.*)?$`))
    await expect(page.getByRole('heading', { name: '圈子管理' })).toBeVisible()
    await assertCurrentUser(page, credentials.username)
  })
})
