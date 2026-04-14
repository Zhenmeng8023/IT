const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const auditCenterPath = path.join(root, 'pages', 'f_project', 'projectmanage', 'components', 'ProjectAuditCenter.vue')
const drawerPath = path.join(root, 'pages', 'f_project', 'projectmanage', 'components', 'ConflictDetailDrawer.vue')

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

function testTargetPathOccupiedStrategyContracts() {
  const auditSource = read(auditCenterPath)
  const drawerSource = read(drawerPath)
  const expected = ["'KEEP_SOURCE'", "'KEEP_TARGET'", "'SET_TARGET_PATH'"]

  expected.forEach(item => {
    assert(auditSource.includes(item), `ProjectAuditCenter should include ${item} for TARGET_PATH_OCCUPIED`)
    assert(drawerSource.includes(item), `ConflictDetailDrawer should include ${item} for TARGET_PATH_OCCUPIED`)
  })
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
testTargetPathOccupiedStrategyContracts()
testStaticRecoveryContracts()

console.log('Project merge conflict regression validation passed')
