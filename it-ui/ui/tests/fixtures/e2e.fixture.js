const base = require('@playwright/test')
const { openRoute } = require('../helpers/navigation')
const { buildDataName } = require('../helpers/naming')
const { expectPageOpen, expectPageShellReady } = require('../helpers/assertions')
const {
  resolveRoleFromProject,
  getRoleStateSummary
} = require('../helpers/auth-state')

const test = base.test.extend({
  e2e: async ({ page }, use, testInfo) => {
    const role = resolveRoleFromProject(testInfo.project.name)
    const roleState = role === 'guest' ? null : getRoleStateSummary(role)

    await use({
      role,
      roleState,
      open: async (routeKeyOrPath, options = {}) => openRoute(page, routeKeyOrPath, options),
      expectShellReady: async () => expectPageShellReady(page),
      expectRouteOpen: async expectedPath => expectPageOpen(page, expectedPath),
      dataName: suffix =>
        buildDataName({
          feature: testInfo.titlePath[1] || 'smoke',
          role,
          suffix
        })
    })
  }
})

module.exports = {
  test,
  expect: base.expect
}
