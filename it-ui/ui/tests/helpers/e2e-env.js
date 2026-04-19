const { ROLES, getBackendBaseURL, getRoleStateSummary } = require('./auth-state')

let backendAvailabilityPromise = null

async function checkBackendAvailability(playwright) {
  if (!backendAvailabilityPromise) {
    backendAvailabilityPromise = (async () => {
      const requestContext = await playwright.request.newContext({
        baseURL: getBackendBaseURL(),
        extraHTTPHeaders: {
          Accept: 'application/json'
        }
      })

      try {
        const response = await requestContext.get('/api/blogs')
        return {
          available: true,
          status: response.status(),
          baseURL: getBackendBaseURL()
        }
      } catch (error) {
        return {
          available: false,
          baseURL: getBackendBaseURL(),
          reason: error.message
        }
      } finally {
        await requestContext.dispose()
      }
    })()
  }

  return backendAvailabilityPromise
}

function summarizeRoleReadiness(requiredRoles = []) {
  const summaries = requiredRoles.map(role => getRoleStateSummary(role))
  const unavailableRoles = summaries.filter(summary => !summary.hasCookies).map(summary => summary.role)
  return {
    summaries,
    unavailableRoles
  }
}

async function resolvePrerequisites(playwright, requiredRoles = [ROLES.USER, ROLES.ADMIN]) {
  const backend = await checkBackendAvailability(playwright)
  const roles = summarizeRoleReadiness(requiredRoles)
  return {
    backend,
    roles
  }
}

module.exports = {
  ROLES,
  getBackendBaseURL,
  checkBackendAvailability,
  summarizeRoleReadiness,
  resolvePrerequisites
}
