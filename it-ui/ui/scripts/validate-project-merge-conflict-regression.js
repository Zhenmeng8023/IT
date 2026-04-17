const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const projectDetailPath = path.join(root, 'pages', 'f_project', 'projectdetail', 'projectdetail.vue')
const auditCenterPath = path.join(root, 'pages', 'f_project', 'projectmanage', 'components', 'ProjectAuditCenter.vue')
const drawerPath = path.join(root, 'pages', 'f_project', 'projectmanage', 'components', 'ConflictDetailDrawer.vue')
const workbenchPath = path.join(root, 'pages', 'f_project', 'projectmanage', 'components', 'ProjectRepoWorkbench.vue')
const conflictCenterPath = path.join(root, 'pages', 'f_project', 'projectmergeconflict', 'projectmergeconflict.vue')
const conflictDiffEditorPath = path.join(root, 'components', 'project-merge', 'ConflictDiffEditor.vue')
const adapterPath = path.join(root, 'utils', 'projectMergeConflictAdapter.js')
const routeSourcePath = path.join(root, 'router', 'route-source.js')
const nuxtConfigPath = path.join(root, 'nuxt.config.js')
const monacoPluginPath = path.join(root, 'plugins', 'monaco.client.js')
const projectMergeComponentPaths = [
  path.join(root, 'components', 'project-merge', 'ConflictBlockNavigator.vue'),
  path.join(root, 'components', 'project-merge', 'ConflictDiffEditor.vue'),
  path.join(root, 'components', 'project-merge', 'ConflictSidebar.vue'),
  path.join(root, 'components', 'project-merge', 'MergeCheckSummary.vue'),
  path.join(root, 'components', 'project-merge', 'PathConflictPanel.vue'),
  path.join(root, 'components', 'project-merge', 'PreMergeGate.vue')
]

function read(filePath) {
  return fs.readFileSync(filePath, 'utf8')
}

function normalizeTextValue(value) {
  return typeof value === 'string' ? value : ''
}

function hasTextValue(value) {
  return typeof value === 'string'
}

function joinBlockLines(lines) {
  return Array.isArray(lines) ? lines.map(item => (item == null ? '' : String(item))).join('\n') : ''
}

function buildResolvedContentFromBlocks(blocks, fallbackText = '') {
  const rows = Array.isArray(blocks) ? blocks : []
  if (!rows.length) return fallbackText
  const merged = rows.map(item => {
    if (!item || typeof item !== 'object') return ''
    if (typeof item.resolvedContent === 'string') return item.resolvedContent
    if (typeof item.finalContent === 'string') return item.finalContent
    if (typeof item.sourceContent === 'string') return item.sourceContent
    if (typeof item.targetContent === 'string') return item.targetContent
    if (typeof item.baseContent === 'string') return item.baseContent
    if (typeof item.content === 'string') return item.content
    if (Array.isArray(item.sourceLines)) return joinBlockLines(item.sourceLines)
    if (Array.isArray(item.targetLines)) return joinBlockLines(item.targetLines)
    if (Array.isArray(item.baseLines)) return joinBlockLines(item.baseLines)
    return ''
  }).join('')
  return merged || fallbackText
}

function conflictKey(conflict) {
  return (conflict && (conflict.conflictId || conflict.id || conflict.filePath || conflict.path)) || null
}

function conflictType(conflict) {
  return String((conflict && (conflict.conflictType || conflict.type)) || '').toUpperCase()
}

function normalizeContentConflictDetail(payload, conflict) {
  const source = payload && typeof payload === 'object' ? payload : {}
  const conflictId = String(source.conflictId || conflictKey(conflict) || '')
  const baseContent = normalizeTextValue(source.baseContent)
  const sourceContent = normalizeTextValue(source.sourceContent)
  const targetContent = normalizeTextValue(source.targetContent)
  const blocks = Array.isArray(source.blocks) ? source.blocks.map(item => ({
    ...(item && typeof item === 'object' ? item : {})
  })) : []
  const fallbackResolved = hasTextValue(source.sourceContent)
    ? sourceContent
    : hasTextValue(source.targetContent)
      ? targetContent
      : hasTextValue(source.baseContent)
        ? baseContent
        : ''
  const resolvedContent = typeof source.resolvedContent === 'string'
    ? source.resolvedContent
    : buildResolvedContentFromBlocks(blocks, fallbackResolved)
  return {
    conflictId,
    baseContent,
    sourceContent,
    targetContent,
    blocks,
    resolvedContent
  }
}

function buildConflictLocator(conflict) {
  const item = conflict && typeof conflict === 'object' ? conflict : {}
  return {
    conflictId: String(conflictKey(item) || ''),
    conflictType: conflictType(item),
    basePath: String(item.basePath || item.oldPath || ''),
    sourcePath: String(item.sourcePath || item.newPath || ''),
    targetPath: String(item.targetPath || item.path || ''),
    filePath: String(item.filePath || item.path || item.newPath || item.sourcePath || item.targetPath || item.oldPath || '')
  }
}

function matchConflictByLocator(conflict, locator) {
  if (!conflict || !locator) return false
  const candidateId = String(conflictKey(conflict) || '')
  if (locator.conflictId && candidateId === locator.conflictId) return true
  if (conflictType(conflict) !== 'CONTENT_CONFLICT') return false

  const candidatePaths = [
    String(conflict.basePath || conflict.oldPath || ''),
    String(conflict.sourcePath || conflict.newPath || ''),
    String(conflict.targetPath || conflict.path || ''),
    String(conflict.filePath || conflict.path || conflict.newPath || conflict.sourcePath || conflict.targetPath || conflict.oldPath || '')
  ].filter(Boolean)
  const expectedPaths = [
    String(locator.basePath || ''),
    String(locator.sourcePath || ''),
    String(locator.targetPath || ''),
    String(locator.filePath || '')
  ].filter(Boolean)
  return expectedPaths.some(item => candidatePaths.includes(item))
}

function testLatestMergeCheckRefreshContracts() {
  const source = read(auditCenterPath)
  assert(
    source.includes("if (result.latestMergeCheck) {\n          this.conflictDetail = this.normalizeDetail(result.latestMergeCheck, this.conflictTargetRow)"),
    'content conflict save should refresh from response latestMergeCheck'
  )
  assert(
    source.includes("if (result.latestMergeCheck) {\n          this.conflictDetail = this.normalizeDetail(result.latestMergeCheck, this.conflictTargetRow)") &&
      source.includes("const result = unwrap(await resolveProjectMergeConflicts(this.conflictTargetRow.id, { options })) || {}"),
    'structured conflict apply should refresh from response latestMergeCheck'
  )
}

function testSingleBlockBehavior() {
  const normalized = normalizeContentConflictDetail({
    sourceContent: '',
    targetContent: 'target-content-should-not-win',
    baseContent: 'base-content-should-not-win',
    blocks: [
      {
        sourceLines: ['source-only-line'],
        targetLines: ['target-only-line'],
        baseLines: ['base-only-line']
      }
    ]
  }, { conflictId: 'c1' })
  assert.strictEqual(normalized.resolvedContent, 'source-only-line')

  const emptySourceWins = normalizeContentConflictDetail({
    sourceContent: '',
    targetContent: 'target-fallback',
    baseContent: 'base-fallback',
    blocks: []
  }, { conflictId: 'c2' })
  assert.strictEqual(emptySourceWins.resolvedContent, '')
}

function testConflictRelocationByPath() {
  const locator = buildConflictLocator({
    conflictId: 'content-old',
    conflictType: 'CONTENT_CONFLICT',
    sourcePath: 'src/conflict.txt',
    targetPath: 'src/conflict.txt'
  })
  const newConflict = {
    conflictId: 'content-new',
    conflictType: 'CONTENT_CONFLICT',
    sourcePath: 'src/conflict.txt',
    targetPath: 'src/conflict.txt'
  }
  assert.strictEqual(matchConflictByLocator(newConflict, locator), true)
}

function testProjectDetailBranchViewContracts() {
  const source = read(projectDetailPath)
  assert(source.includes('v-model="currentBranchId"'), 'project detail should bind branch selector to currentBranchId')
  assert(source.includes('@change="handleBranchChange"'), 'project detail should reload files when branch selector changes')
  assert(source.includes('listProjectBranches(this.projectId)'), 'project detail should load main/dev branch options')
  assert(source.includes('const nextBranchId = validRouteBranchId || validCurrentBranchId || validDefaultBranchId || fallbackBranchId'), 'project detail should prefer route branch, then current/default branch')
  assert(source.includes('await this.fetchFiles(nextBranchId)'), 'branch changes should fetch the selected branch files')
  assert(source.includes('listProjectFiles(this.projectId, branchId)'), 'file list API should receive the selected branchId')
  assert(source.includes('previewProjectFile(node.id, this.currentBranchId)'), 'file preview should read from current branch')
  assert(source.includes('downloadFile(this.currentFile.id, this.currentBranchId)'), 'file download should read from current branch')
  assert(source.includes('listFileVersions(node.id, this.currentBranchId)'), 'version list should read from current branch')
}

function testFailedCheckDetailContracts() {
  const source = read(auditCenterPath)
  assert(source.includes('const checksFromDetail = Array.isArray(row && row.effectiveChecks)'), 'audit center should prefer backend effective checks')
  assert(source.includes('if (String(item && item.commitId) !== String(commitId)) return'), 'audit center should ignore stale check runs from other commits')
  assert(source.includes("const type = (item && item.checkType ? String(item.checkType) : 'custom').trim().toLowerCase() || 'custom'"), 'audit center should group checks by normalized type')
  assert(source.includes('return summary ? `${checkType}: ${summary}` : `${checkType}: failed`'), 'audit center should show failed check type and summary')
  assert(source.includes('failedCheckSummary(row)'), 'audit center should expose actionable failed check summaries')
  assert(source.includes('diagnosticCheckSummary(row)'), 'audit center should expose diagnostic failed check summaries')
  assert(source.includes('add a newer success check with the same type'), 'audit center should explain same-type success recovery')
}

function testConflictCenterBlockChoiceSaveContracts() {
  const centerSource = read(conflictCenterPath)
  const editorSource = read(conflictDiffEditorPath)
  assert(editorSource.includes('@click="setCurrentBlockChoice(CONTENT_BLOCK_CHOICES.KEEP_SOURCE)"'), 'content editor should allow keep-source block choice')
  assert(editorSource.includes('@click="setCurrentBlockChoice(CONTENT_BLOCK_CHOICES.KEEP_TARGET)"'), 'content editor should allow keep-target block choice')
  assert(editorSource.includes('@click="setCurrentBlockChoice(CONTENT_BLOCK_CHOICES.MANUAL)"'), 'content editor should allow manual per-block choice')
  assert(editorSource.includes('buildBlockChoicesPayload()'), 'content editor should build per-block save payload')
  assert(editorSource.includes("this.$emit('save', {"), 'content editor should emit save payload')
  assert(editorSource.includes('blockChoices: this.buildBlockChoicesPayload()'), 'content editor save payload should include blockChoices')
  assert(centerSource.includes('normalizeContentBlockChoices(blockChoices)'), 'conflict center should sanitize blockChoices before submit')
  assert(centerSource.includes('requestBody.blockChoices = blockChoices'), 'conflict center should submit blockChoices')
  assert(centerSource.includes('resolveProjectMergeContentConflict(this.mergeRequestId, requestBody)'), 'conflict center should save content conflict through backend API')
  assert(centerSource.includes('shouldRetryWithoutBlockChoices(error)'), 'conflict center should keep legacy backend fallback while preserving new block save path')
}

function testTargetPathOccupiedStrategyContracts() {
  const auditSource = read(auditCenterPath)
  const drawerSource = read(drawerPath)
  const expected = ["'KEEP_SOURCE'", "'KEEP_TARGET'", "'SET_TARGET_PATH'"]

  expected.forEach(item => {
    assert(auditSource.includes(item), `ProjectAuditCenter should include ${item} for TARGET_PATH_OCCUPIED`)
    assert(drawerSource.includes(item), `ConflictDetailDrawer should include ${item} for TARGET_PATH_OCCUPIED`)
  })
}

function testConflictCenterPageContracts() {
  const source = read(conflictCenterPath)
  const expectedApiCalls = [
    'getProjectMergeCheckLatest',
    'getProjectPreMergeCheck',
    'recheckProjectMerge',
    'resolveProjectMergeContentConflict',
    'resolveProjectMergeConflicts',
    'mergeProjectMergeRequest'
  ]

  expectedApiCalls.forEach(item => {
    assert(source.includes(item), `conflict center should call ${item}`)
  })

  assert(
    source.includes('this.preMergeCheck.mergeable !== true'),
    'merge button should require an explicit successful pre-merge-check'
  )
  assert(
    source.includes('isSelectedPathConflict || isSelectedStaleBranch'),
    'stale branch conflicts should render in the actionable conflict panel'
  )
  assert(
    source.includes('this.clearContentConflictState()') && source.includes('this.loadSelectedContentConflict(true)'),
    'recheck should clear stale content detail state and reload the selected content conflict'
  )
}

function testAdapterContracts() {
  const source = read(adapterPath)
  const expectedTypes = [
    'CONTENT_CONFLICT',
    'DELETE_MODIFY_CONFLICT',
    'RENAME_CONFLICT',
    'MOVE_CONFLICT',
    'TARGET_PATH_OCCUPIED',
    'STALE_BRANCH'
  ]
  const expectedStrategies = [
    'KEEP_SOURCE',
    'KEEP_TARGET',
    'USE_SOURCE_PATH',
    'USE_TARGET_PATH'
  ]

  expectedTypes.forEach(item => {
    assert(source.includes(item), `adapter should understand ${item}`)
  })
  expectedStrategies.forEach(item => {
    assert(source.includes(item), `adapter should expose ${item}`)
  })
  assert(source.includes('payload.totalConflicts'), 'adapter should normalize backend totalConflicts')
}

function testRouteAndMonacoContracts() {
  const routeSource = read(routeSourcePath)
  const nuxtConfig = read(nuxtConfigPath)
  const monacoPlugin = read(monacoPluginPath)

  assert(routeSource.includes("path: '/projectmergeconflict'"), 'route source should expose conflict center page')
  assert(nuxtConfig.includes("plugins/monaco.client.js"), 'nuxt config should register Monaco client plugin')
  assert(nuxtConfig.includes('MonacoWebpackPlugin'), 'nuxt config should enable Monaco webpack plugin')
  assert(monacoPlugin.includes("monaco-editor/min/vs/editor/editor.main.css"), 'Monaco plugin should load editor CSS')
  assert(monacoPlugin.includes('Vue.prototype.$loadMonaco'), 'Monaco plugin should expose $loadMonaco')
}

function testProjectMergeSmokeFiles() {
  const files = [
    workbenchPath,
    conflictCenterPath,
    adapterPath,
    monacoPluginPath,
    ...projectMergeComponentPaths
  ]

  files.forEach(filePath => {
    const source = read(filePath)
    assert(!source.includes('\uFFFD'), `${path.relative(root, filePath)} should not contain replacement characters`)
  })

  projectMergeComponentPaths.forEach(filePath => {
    const source = read(filePath)
    assert(source.includes('<template>'), `${path.relative(root, filePath)} should have a template block`)
    assert(source.includes('<script>'), `${path.relative(root, filePath)} should have a script block`)
    assert(source.includes('<style scoped>'), `${path.relative(root, filePath)} should keep scoped styling`)
  })
}

function testWorkspaceReviewLabelContracts() {
  const source = read(workbenchPath)
  assert(source.includes('workspaceChangeRows()'), 'workbench should expose normalized workspace change rows')
  assert(source.includes('return Array.isArray(this.workspaceItems) ? this.workspaceItems : []'), 'workbench should fall back to workspace items when changes are absent')
  assert(source.includes('scope.row.conflictFlag'), 'workbench should render real conflict flags on table rows')
  assert(source.includes('isWorkspaceBaseBehind()'), 'workbench should detect stale workspace base commits')
  assert(source.includes('isWorkspaceItemAtRisk(row)'), 'workbench should mark stale workspace rows as risk items')
  assert(source.includes('this.conflictItemCount === 0'), 'workbench should block direct commit while workspace conflicts remain')
  assert(source.includes('工作区基线落后于当前分支 Head'), 'stale workspace label should describe a base-line issue')
  assert(source.includes('建议先核对差异再继续提交'), 'stale workspace warning should explain the next review action')
}

function testStaticRecoveryContracts() {
  const source = read(auditCenterPath)
  assert(source.includes('buildResolvedContentFromBlocks(blocks, fallbackText = \'\')'), 'audit center should define block merge helper')
  assert(source.includes('Array.isArray(item.sourceLines)'), 'audit center should read sourceLines')
  assert(source.includes('Array.isArray(item.targetLines)'), 'audit center should read targetLines')
  assert(source.includes('Array.isArray(item.baseLines)'), 'audit center should read baseLines')
  assert(source.includes('const locator = typeof preferredConflict === \'string\''), 'recheck recovery should build a locator')
  assert(source.includes('this.matchConflictByLocator(item, locator)'), 'recheck recovery should relocate by locator')
  assert(source.includes('Content conflict was rechecked, but the matching editor entry could not be found'), 'recheck recovery should warn instead of crashing')
}

testLatestMergeCheckRefreshContracts()
testSingleBlockBehavior()
testConflictRelocationByPath()
testProjectDetailBranchViewContracts()
testFailedCheckDetailContracts()
testConflictCenterBlockChoiceSaveContracts()
testTargetPathOccupiedStrategyContracts()
testConflictCenterPageContracts()
testAdapterContracts()
testRouteAndMonacoContracts()
testProjectMergeSmokeFiles()
testWorkspaceReviewLabelContracts()
testStaticRecoveryContracts()

console.log('Project merge conflict regression validation passed')
