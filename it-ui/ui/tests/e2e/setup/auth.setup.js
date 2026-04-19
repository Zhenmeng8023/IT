const { test: setup } = require('@playwright/test')
const { ROLES, setupRoleStorageState } = require('../../helpers/auth-state')

setup('prepare user/admin storage states', async ({ playwright }) => {
  const results = []

  results.push(
    await setupRoleStorageState({
      role: ROLES.USER,
      playwright
    })
  )

  results.push(
    await setupRoleStorageState({
      role: ROLES.ADMIN,
      playwright
    })
  )

  for (const result of results) {
    console.log(
      `[setup-auth] role=${result.role} ready=${result.ready} reason=${result.reason} state=${result.statePath}`
    )
  }
})
