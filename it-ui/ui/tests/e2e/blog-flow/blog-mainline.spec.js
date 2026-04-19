const { test, expect } = require('../../fixtures/e2e.fixture')
const { resolvePrerequisites, getBackendBaseURL, ROLES } = require('../../helpers/e2e-env')
const { buildUniqueName, createRoleSession, fetchJson, waitForListItem } = require('../../helpers/role-context')

async function fetchCurrentUserId(page) {
  const result = await fetchJson(page, `${getBackendBaseURL()}/api/users/current`)
  const payload = result.data && result.data.data !== undefined ? result.data.data : result.data
  if (!result.ok || !payload || !payload.id) {
    throw new Error(`failed to resolve user from session: status=${result.status}`)
  }
  return payload.id
}

test.describe('blog mainline flow', () => {
  test('user submits a pending blog, admin approves it, and guest can browse the blog detail', async ({ browser, playwright }, testInfo) => {
    test.skip(testInfo.project.name !== 'smoke-admin', 'single project execution')

    const prereq = await resolvePrerequisites(playwright, [ROLES.USER, ROLES.ADMIN])
    test.skip(!prereq.backend.available, `backend unavailable: ${prereq.backend.reason}`)
    test.skip(
      prereq.roles.unavailableRoles.length > 0,
      `missing role storage state: ${prereq.roles.unavailableRoles.join(', ')}`
    )

    const userSession = await createRoleSession(browser, ROLES.USER, '/blogwrite')
    const adminSession = await createRoleSession(browser, ROLES.ADMIN, '/audit')
    const guestSession = await createRoleSession(browser, 'guest', '/blog')

    try {
      const authorId = await fetchCurrentUserId(userSession.page)
      const title = buildUniqueName('e2e-blog')
      const summary = `博客验收摘要-${Date.now()}`
      const content = `<p>这是一篇用于 Playwright 验收的博客正文，包含联系方式字段以触发人工审核。</p><p>联系方式：请管理员审核后发布。</p>`

      const created = await fetchJson(userSession.page, `${getBackendBaseURL()}/api/blogs`, {
        method: 'POST',
        body: {
          title,
          summary,
          content,
          status: 'pending',
          tagIds: [],
          price: 0
        }
      })
      expect(created.status).toBe(201)

      const createdBlog = await waitForListItem(
        userSession.page,
        `${getBackendBaseURL()}/api/blogs/author/${authorId}`,
        item => String(item.title || '') === title
      )

      await expect(adminSession.page.getByText('博客审核管理', { exact: true })).toBeVisible()
      const approveResult = await fetchJson(
        adminSession.page,
        `${getBackendBaseURL()}/api/blogs/${createdBlog.matchedItem.id}/approve`,
        { method: 'PUT' }
      )
      expect(approveResult.ok).toBeTruthy()

      await guestSession.page.goto(`/blog/${createdBlog.matchedItem.id}`, { waitUntil: 'domcontentloaded' })
      await expect(guestSession.page.getByRole('heading', { level: 1 })).toContainText(title)
      await expect(guestSession.page.locator('.content-body')).toContainText('Playwright 验收')
    } finally {
      await guestSession.close()
      await adminSession.close()
      await userSession.close()
    }
  })
})
