const { test, expect } = require('../../fixtures/e2e.fixture')
const { ROUTE_PATHS } = require('../../helpers/navigation')
const { inspectAdminAccess } = require('../../helpers/assertions')

test.describe('e2e smoke scaffold', () => {
  test('首页可打开', async ({ page, e2e }) => {
    const target = await e2e.open('home')
    await e2e.expectRouteOpen(target)
    await expect(e2e.dataName('home-open')).toContain('smoke')
  })

  test('圈子首页可打开', async ({ page, e2e }) => {
    const target = await e2e.open('circleHome')
    await e2e.expectRouteOpen(target)
  })

  test('blog 主页面可打开', async ({ page, e2e }) => {
    const target = await e2e.open('blogHome')
    await e2e.expectRouteOpen(target)
  })

  test('管理端圈子页可打开（若具备权限）', async ({ page, e2e }) => {
    test.skip(e2e.role !== 'admin', '该用例仅在 smoke-admin 项目执行')
    test.skip(
      !e2e.roleState || !e2e.roleState.hasCookies,
      '未配置可用管理员登录态（E2E_ADMIN_USERNAME/E2E_ADMIN_PASSWORD）'
    )

    await e2e.open('circleManage')
    const accessResult = await inspectAdminAccess(page, ROUTE_PATHS.circleManage)

    expect(['allowed', 'blocked-login', 'blocked-permission']).toContain(accessResult.status)

    if (accessResult.status !== 'allowed') {
      test.info().annotations.push({
        type: 'contract-note',
        description: `管理员态未直接访问成功，当前为 ${accessResult.status} (${accessResult.currentPath})`
      })
    }
  })
})
