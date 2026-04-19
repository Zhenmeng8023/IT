const { test, expect } = require('../../fixtures/e2e.fixture')
const { resolvePrerequisites, getBackendBaseURL, ROLES } = require('../../helpers/e2e-env')
const { fetchJson } = require('../../helpers/role-context')

test.describe('circle and blog authorization', () => {
  test('non-admin user cannot execute privileged review operations', async ({ page, playwright, e2e }, testInfo) => {
    test.skip(testInfo.project.name !== 'smoke-user', 'user project only')

    const prereq = await resolvePrerequisites(playwright, [ROLES.USER])
    test.skip(!prereq.backend.available, `backend unavailable: ${prereq.backend.reason}`)
    test.skip(!e2e.roleState || !e2e.roleState.hasCookies, 'missing user storage state')

    const circleApprove = await fetchJson(page, `${getBackendBaseURL()}/api/circle/manage/approve/1`, {
      method: 'PUT'
    })
    expect(circleApprove.status).toBe(403)

    const blogApprove = await fetchJson(page, `${getBackendBaseURL()}/api/blogs/1/approve`, {
      method: 'PUT'
    })
    expect(blogApprove.status).toBe(403)

    await page.goto('/circlemanage', { waitUntil: 'domcontentloaded' })
    await expect(page).toHaveURL(/\/circlemanage(?:\?.*)?$/)
  })
})
