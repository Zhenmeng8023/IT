const { test, expect } = require('../../fixtures/e2e.fixture')
const { expectToastMessage } = require('../../helpers/assertions')
const { resolvePrerequisites, getBackendBaseURL, ROLES } = require('../../helpers/e2e-env')
const {
  buildUniqueName,
  createRoleSession,
  fetchJson,
  unwrapPayload,
  waitForListItem
} = require('../../helpers/role-context')

async function getCurrentUser(page) {
  const result = await fetchJson(page, `${getBackendBaseURL()}/api/users/current`)
  const user = unwrapPayload(result.data)
  if (!result.ok || !user || !user.id) {
    throw new Error(`failed to resolve current user: status=${result.status}`)
  }
  return user
}

test.describe('circle review end-to-end flow', () => {
  test('user creates a circle, admin reviews it, post is approved, and guest can read it', async ({ browser, playwright }, testInfo) => {
    test.skip(testInfo.project.name !== 'smoke-admin', 'single project execution')

    const prereq = await resolvePrerequisites(playwright, [ROLES.USER, ROLES.ADMIN])
    test.skip(!prereq.backend.available, `backend unavailable: ${prereq.backend.reason}`)
    test.skip(
      prereq.roles.unavailableRoles.length > 0,
      `missing role storage state: ${prereq.roles.unavailableRoles.join(', ')}`
    )

    const userSession = await createRoleSession(browser, ROLES.USER, '/circle')
    const adminSession = await createRoleSession(browser, ROLES.ADMIN, '/circleaudit')
    const guestSession = await createRoleSession(browser, 'guest', '/circle')

    try {
      const user = await getCurrentUser(userSession.page)
      const circleName = buildUniqueName('e2e-circle')
      const circleDescription = `公开圈子验收-${Date.now()}`
      const postContent = `E2E 圈子帖子 ${Date.now()}，用于验证审核通过后的公开详情可见。`
      await expect(adminSession.page.getByRole('heading', { name: '圈子管理' })).toBeVisible()

      await expect(userSession.page.getByRole('button', { name: '创建圈子', exact: true })).toBeVisible()
      await userSession.page.getByRole('button', { name: '创建圈子', exact: true }).click()
      const createDialog = userSession.page.locator('.el-dialog__wrapper:visible').last()
      await createDialog.getByPlaceholder('请输入圈子名称').fill(circleName)
      await createDialog.getByPlaceholder('请输入圈子描述').fill(circleDescription)
      await createDialog.getByRole('button', { name: '创建', exact: true }).click()
      await expectToastMessage(userSession.page, /圈子创建成功/)

      const createdCircleInfo = await waitForListItem(
        userSession.page,
        `${getBackendBaseURL()}/api/circle/creator/${user.id}`,
        item => String(item.name || '') === circleName
      )
      const createdCircle = createdCircleInfo.matchedItem
      expect(String(createdCircle.type || '')).toContain('pending')

      const approveCircleResult = await fetchJson(
        adminSession.page,
        `${getBackendBaseURL()}/api/circle/manage/approve/${createdCircle.id}`,
        { method: 'PUT' }
      )
      expect(approveCircleResult.ok).toBeTruthy()

      const publicCircles = await waitForListItem(
        guestSession.page,
        `${getBackendBaseURL()}/api/circle`,
        item => String(item.name || '') === circleName
      )
      expect(String(publicCircles.matchedItem.visibility || '')).toContain('public')

      await userSession.page.reload({ waitUntil: 'domcontentloaded' })
      await expect(userSession.page.getByRole('button', { name: '写帖子', exact: true })).toBeVisible()
      await userSession.page.getByRole('button', { name: '写帖子', exact: true }).click()
      const postDialog = userSession.page.locator('.el-dialog__wrapper:visible').last()
      const selectedCircleCloseIcons = postDialog.locator('.el-select__tags .el-tag__close')
      while (await selectedCircleCloseIcons.count()) {
        await selectedCircleCloseIcons.first().click()
      }
      await postDialog.getByPlaceholder('请输入帖子内容...').fill(postContent)
      await postDialog.getByPlaceholder('请选择要发布的圈子（可多选）').click()
      await userSession.page.locator('.el-select-dropdown__item').filter({ hasText: circleName }).first().click()
      const selectedCircleTags = postDialog.locator('.el-select__tags .el-tag')
      const selectedCircleCloseButtons = postDialog.locator('.el-select__tags .el-tag__close')
      while ((await selectedCircleTags.count()) > 1) {
        await selectedCircleCloseButtons.last().click()
      }
      await userSession.page.keyboard.press('Escape')
      const publishButton = postDialog.locator('.el-dialog__footer .el-button--primary').last()
      await expect(publishButton).toBeVisible()
      await publishButton.click({ force: true })

      const createdPostInfo = await waitForListItem(
        userSession.page,
        `${getBackendBaseURL()}/api/circle/user/${user.id}/posts`,
        item =>
          String(item.content || '').includes(postContent) &&
          String(item.circleId || item.circle?.id || '') === String(createdCircle.id)
      )
      const createdPost = createdPostInfo.matchedItem

      const approvePostResult = await fetchJson(
        adminSession.page,
        `${getBackendBaseURL()}/api/circle/manage/approve-post/${createdPost.id}`,
        { method: 'PUT' }
      )
      expect(approvePostResult.ok).toBeTruthy()

      await guestSession.page.reload({ waitUntil: 'domcontentloaded' })

      const publicPosts = await waitForListItem(
        guestSession.page,
        `${getBackendBaseURL()}/api/circle/posts/all`,
        item => String(item.id) === String(createdPost.id)
      )
      expect(String(publicPosts.matchedItem.status || '')).toBe('published')

      await guestSession.page.goto(`/circle/${createdPost.id}?circleId=${createdCircle.id}`, {
        waitUntil: 'domcontentloaded'
      })
      await expect(guestSession.page.getByRole('heading', { level: 1 })).toContainText('帖子 #')
      await expect(guestSession.page.locator('body')).toContainText(postContent)
    } finally {
      await guestSession.close()
      await adminSession.close()
      await userSession.close()
    }
  })
})
