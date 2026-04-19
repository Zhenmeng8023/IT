const ROUTE_PATHS = Object.freeze({
  home: '/',
  circleHome: '/circle',
  blogHome: '/blog',
  blogWrite: '/blogwrite',
  blogAudit: '/audit',
  circleManage: '/circlemanage',
  circleAudit: '/circleaudit',
  noPermission: '/noPermission'
})

function resolveRoutePath(routeKeyOrPath) {
  if (!routeKeyOrPath) {
    return ROUTE_PATHS.home
  }
  return ROUTE_PATHS[routeKeyOrPath] || routeKeyOrPath
}

async function openRoute(page, routeKeyOrPath, options = {}) {
  const routePath = resolveRoutePath(routeKeyOrPath)
  await page.goto(routePath, {
    waitUntil: 'domcontentloaded',
    timeout: options.timeout || 60 * 1000
  })
  return routePath
}

module.exports = {
  ROUTE_PATHS,
  resolveRoutePath,
  openRoute
}
