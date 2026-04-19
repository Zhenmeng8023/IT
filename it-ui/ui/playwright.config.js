const path = require('path')
const { defineConfig, devices } = require('@playwright/test')

const baseURL = process.env.E2E_BASE_URL || 'http://localhost:3000'
const useDevServer = process.env.E2E_USE_DEV_SERVER !== '0'
const authDir = path.join(__dirname, 'tests', 'e2e', '.auth')

module.exports = defineConfig({
  testDir: path.join(__dirname, 'tests', 'e2e'),
  timeout: 90 * 1000,
  expect: {
    timeout: 10 * 1000
  },
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : 1,
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: 'playwright-report' }]
  ],
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 10 * 1000,
    navigationTimeout: 15 * 1000
  },
  webServer: useDevServer
    ? {
        command: 'npx nuxt dev --hostname localhost --port 3000',
        url: baseURL,
        timeout: 120 * 1000,
        reuseExistingServer: !process.env.CI
      }
    : undefined,
  projects: [
    {
      name: 'setup-auth',
      testMatch: /setup\/.*\.setup\.js$/
    },
    {
      name: 'smoke-guest',
      dependencies: ['setup-auth'],
      testIgnore: /setup\/.*\.setup\.js$/,
      use: {
        ...devices['Desktop Chrome']
      }
    },
    {
      name: 'smoke-user',
      dependencies: ['setup-auth'],
      testIgnore: /setup\/.*\.setup\.js$/,
      use: {
        ...devices['Desktop Chrome'],
        storageState: path.join(authDir, 'user.json')
      }
    },
    {
      name: 'smoke-admin',
      dependencies: ['setup-auth'],
      testIgnore: /setup\/.*\.setup\.js$/,
      use: {
        ...devices['Desktop Chrome'],
        storageState: path.join(authDir, 'admin.json')
      }
    }
  ]
})
