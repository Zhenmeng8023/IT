const { test, expect } = require('../../fixtures/e2e.fixture')
const {
  ADMIN_ROUTES,
  FRONT_AUTH_PATHS,
  FRONT_PUBLIC_PATHS,
  LEGACY_ADMIN_REDIRECTS,
  REVIEWER_ADMIN_PERMISSIONS,
  installRbacApiMocks
} = require('../../helpers/rbac-mock')

function pathnameOf(page) {
  return new URL(page.url()).pathname
}

function expectPathname(page, expectedPath) {
  return expect
    .poll(() => pathnameOf(page), { timeout: 10000 })
    .toBe(expectedPath)
}

async function gotoAndWaitForClientRedirect(page, path) {
  await page.goto(path, { waitUntil: 'domcontentloaded', timeout: 60 * 1000 })
}

async function readManageLayoutMenus(page) {
  return page.evaluate(() => {
    const collectPaths = (menus = [], result = []) => {
      menus.forEach((menu) => {
        if (menu && menu.path) {
          result.push(menu.path)
        }
        if (menu && Array.isArray(menu.children)) {
          collectPaths(menu.children, result)
        }
      })
      return result
    }

    const collectNames = (menus = [], result = []) => {
      menus.forEach((menu) => {
        if (menu && menu.name) {
          result.push(menu.name)
        }
        if (menu && Array.isArray(menu.children)) {
          collectNames(menu.children, result)
        }
      })
      return result
    }

    const seen = new Set()
    const findManageLayout = (vm) => {
      if (!vm || seen.has(vm)) {
        return null
      }

      seen.add(vm)

      if (vm.$options && vm.$options.name === 'ManageLayout' && Array.isArray(vm.menus)) {
        return vm
      }

      for (const child of vm.$children || []) {
        const matched = findManageLayout(child)
        if (matched) {
          return matched
        }
      }

      return null
    }

    const manageLayout = findManageLayout(window.$nuxt)
    if (!manageLayout) {
      return {
        found: false,
        paths: [],
        names: []
      }
    }

    return {
      found: true,
      paths: collectPaths(manageLayout.menus),
      names: collectNames(manageLayout.menus)
    }
  })
}

async function waitForAdminMenus(page) {
  await expect(page.locator('.admin-shell')).toBeVisible({ timeout: 15000 })
  await expect
    .poll(async () => {
      const menus = await readManageLayoutMenus(page)
      return menus.found && menus.paths.length > 0
    }, { timeout: 15000 })
    .toBe(true)
  return readManageLayoutMenus(page)
}

test.describe('front and admin RBAC route contracts', () => {
  test.beforeEach(async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'smoke-guest', 'mocked RBAC contracts run once')
    await page.context().clearCookies()
  })

  test('guest can open public front routes without login', async ({ page }) => {
    await installRbacApiMocks(page, 'guest')

    for (const routePath of FRONT_PUBLIC_PATHS) {
      await gotoAndWaitForClientRedirect(page, routePath)
      await expectPathname(page, routePath)
      await expect(page.locator('body')).toBeVisible()
    }
  })

  test('guest is redirected to login for front routes requiring authentication', async ({ page }) => {
    await installRbacApiMocks(page, 'guest')

    for (const routePath of FRONT_AUTH_PATHS) {
      await gotoAndWaitForClientRedirect(page, routePath)
      await expectPathname(page, '/login')
      expect(page.url()).toContain(`redirect=${encodeURIComponent(routePath)}`)
    }
  })

  test('regular user can open front routes with view:front permissions', async ({ page }) => {
    await installRbacApiMocks(page, 'user')

    for (const routePath of FRONT_AUTH_PATHS) {
      await gotoAndWaitForClientRedirect(page, routePath)
      await expectPathname(page, routePath)
    }
  })

  test('regular user cannot enter admin routes directly', async ({ page }) => {
    await installRbacApiMocks(page, 'user')

    await gotoAndWaitForClientRedirect(page, '/admin/home')

    await expect
      .poll(() => pathnameOf(page), { timeout: 10000 })
      .toMatch(/^\/(?:login|noPermission)$/)
  })

  test('admin sidebar contains only admin menu paths', async ({ page }) => {
    await installRbacApiMocks(page, 'admin')

    await gotoAndWaitForClientRedirect(page, '/admin/home')
    await expectPathname(page, '/admin/home')

    const menus = await waitForAdminMenus(page)
    const routePaths = menus.paths.filter(path => path !== '/admin' && path.startsWith('/admin/'))

    expect(routePaths.length).toBeGreaterThan(0)
    expect(routePaths.every(path => path.startsWith('/admin/'))).toBe(true)
    expect(menus.paths).not.toContain('/user')
    expect(menus.paths).not.toContain('/blogwrite')
    expect(menus.names).not.toContain('Front Leak Marker')
  })

  test('reviewer sidebar is limited to dashboard and audit modules', async ({ page }) => {
    await installRbacApiMocks(page, 'reviewer')

    await gotoAndWaitForClientRedirect(page, '/admin/home')
    await expectPathname(page, '/admin/home')

    const menus = await waitForAdminMenus(page)
    const visibleAdminRoutes = menus.paths.filter(path => path.startsWith('/admin/'))
    const expectedReviewerRoutes = ADMIN_ROUTES
      .filter(route => REVIEWER_ADMIN_PERMISSIONS.includes(route.permission))
      .map(route => route.path)
    const forbiddenReviewerRoutes = ADMIN_ROUTES
      .filter(route => !REVIEWER_ADMIN_PERMISSIONS.includes(route.permission))
      .map(route => route.path)

    expect(visibleAdminRoutes).toEqual(expect.arrayContaining(expectedReviewerRoutes))

    for (const forbiddenRoute of forbiddenReviewerRoutes) {
      expect(visibleAdminRoutes).not.toContain(forbiddenRoute)
    }
  })

  test('/admin redirects to the first available backend menu', async ({ page }) => {
    await installRbacApiMocks(page, 'reviewer')

    await gotoAndWaitForClientRedirect(page, '/admin')

    await expectPathname(page, '/admin/home')
  })

  for (const { legacyPath, targetPath } of LEGACY_ADMIN_REDIRECTS) {
    test(`legacy admin path ${legacyPath} redirects to ${targetPath}`, async ({ page }) => {
      await installRbacApiMocks(page, 'admin')

      await gotoAndWaitForClientRedirect(page, legacyPath)

      await expectPathname(page, targetPath)
    })
  }
})
