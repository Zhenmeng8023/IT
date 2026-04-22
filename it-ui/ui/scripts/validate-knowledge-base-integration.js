const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const repoRoot = path.resolve(root, '..', '..')

function read(file) {
  return fs.readFileSync(path.join(root, file), 'utf8')
}

function readRepo(file) {
  return fs.readFileSync(path.join(repoRoot, file), 'utf8')
}

function assertIncludes(content, expected, message) {
  assert(content.includes(expected), message)
}

function testRoutes() {
  const routeSource = read('router/route-source.js')
  const adminCatalog = read('router/admin-catalog.js')

  assertIncludes(routeSource, "path: '/personal-knowledge-base'", 'front personal knowledge route is registered')
  assertIncludes(routeSource, "pages/front_ai/personal/KnowledgeBaseCenter.vue", 'front personal page is wired')
  assertIncludes(routeSource, "permissions: ['view:front:ai:kb:self']", 'front personal route uses self permission')
  assertIncludes(routeSource, "path: '/project-knowledge-base'", 'front project knowledge route is registered')
  assertIncludes(routeSource, "pages/front_ai/project/KnowledgeBaseCenter.vue", 'front project page is wired')
  assertIncludes(routeSource, "permissions: ['view:front:ai:kb:project']", 'front project route uses project permission')
  assertIncludes(adminCatalog, "component: 'pages/ai/AdminKnowledgeGovernance.vue'", 'admin governance page is wired')
  assertIncludes(adminCatalog, "path: '/admin/ai/knowledge-usage'", 'admin usage route is registered')
  assertIncludes(adminCatalog, "component: 'pages/ai/AdminKnowledgeUsage.vue'", 'admin usage page is wired')
}

function testPermissions() {
  const content = read('utils/permissionConfig.js')
  assertIncludes(content, "'view:front:ai:kb:self': ['view:knowledge-base']", 'personal permission keeps legacy alias')
  assertIncludes(content, "'view:front:ai:kb:project': ['view:knowledge-base']", 'project permission keeps legacy alias')
  assertIncludes(content, "'manage:front:ai:kb:member': ['view:knowledge-base', 'manage:knowledge-base']", 'member permission keeps legacy alias')
}

function testAssistantJump() {
  const content = read('components/AIAssistant.vue')
  assertIncludes(content, "const targetPath = projectId ? '/project-knowledge-base' : '/personal-knowledge-base'", 'import CTA routes by projectId')
  assertIncludes(content, "path: projectId ? '/project-knowledge-base' : '/personal-knowledge-base'", 'source location routes by projectId')
}

function testSqlSeeds() {
  const fullSeed = readRepo('sql/insert/IT10_v8_insert.sql')
  const rbacSeed = readRepo('sql/insert/it_rbac_front_admin_seed_20260420.sql')

  assertIncludes(fullSeed, "role_menu -> menu.permission_id", 'full seed documents authority injection chain')
  assertIncludes(fullSeed, "/admin/ai/knowledge-usage", 'full seed contains admin usage menu')
  assertIncludes(fullSeed, "pages/ai/AdminKnowledgeGovernance.vue", 'full seed points governance menu to governance page')
  assertIncludes(fullSeed, "/front/ai/personal-knowledge-base", 'full seed contains path-based personal knowledge permission menu')
  assertIncludes(fullSeed, "/front/ai/project-knowledge-base", 'full seed contains path-based project knowledge permission menu')
  assertIncludes(fullSeed, "(1, 315), (1, 316), (1, 317), (1, 318), (1, 319), (1, 320)", 'full seed grants super admin path-based front AI menus')
  assertIncludes(fullSeed, "(4, 315), (4, 316), (4, 317), (4, 318)", 'full seed injects user path-based front AI knowledge authorities')
  assertIncludes(rbacSeed, "/admin/ai/knowledge-usage", 'rbac seed contains admin usage menu')
  assertIncludes(rbacSeed, "/front/ai/personal-knowledge-base", 'rbac seed contains personal knowledge permission menu')
  assertIncludes(rbacSeed, "/front/ai/project-knowledge-base", 'rbac seed contains project knowledge permission menu')
}

testRoutes()
testPermissions()
testAssistantJump()
testSqlSeeds()

console.log('Knowledge base integration validation passed')
