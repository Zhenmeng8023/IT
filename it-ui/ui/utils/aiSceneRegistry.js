import { toAiQuickPromptList } from '@/utils/aiSuggestionPreset'

const DEFAULT_SCENE_CODE = 'global.assistant'

const sceneRegistry = Object.freeze({
  'project.detail': Object.freeze({
    sceneCode: 'project.detail',
    bizType: 'PROJECT',
    requestType: 'PROJECT_ASSISTANT',
    defaultAnalysisMode: 'CODE_LOCATE',
    allowKnowledgeBinding: true,
    label: 'Current scene: Project detail',
    defaultQuickPrompts: Object.freeze(toAiQuickPromptList('project.detail'))
  }),
  'project.board': Object.freeze({
    sceneCode: 'project.board',
    bizType: 'PROJECT',
    requestType: 'PROJECT_ASSISTANT',
    defaultAnalysisMode: 'CODE_LOCATE',
    allowKnowledgeBinding: true,
    label: 'Current scene: Project board',
    defaultQuickPrompts: Object.freeze([
      Object.freeze({ label: 'Board summary', prompt: 'Please summarize current board progress, blockers, and top priorities.' }),
      Object.freeze({ label: 'Next sprint plan', prompt: 'Please propose next sprint tasks based on current board state.' }),
      Object.freeze({ label: 'Dependency check', prompt: 'Please identify critical dependencies and risk points in this board.' })
    ])
  }),
  'project.merge': Object.freeze({
    sceneCode: 'project.merge',
    bizType: 'PROJECT',
    requestType: 'PROJECT_ASSISTANT',
    defaultAnalysisMode: 'CODE_LOGIC',
    allowKnowledgeBinding: true,
    label: 'Current scene: Merge conflict',
    defaultQuickPrompts: Object.freeze([
      Object.freeze({ label: 'Conflict summary', prompt: 'Please summarize the current merge conflicts and likely root causes.' }),
      Object.freeze({ label: 'Resolution plan', prompt: 'Please provide a safe conflict-resolution plan with validation steps.' }),
      Object.freeze({ label: 'Logic impact', prompt: 'Please analyze potential logic impacts after resolving these conflicts.' })
    ])
  }),
  'blog.write': Object.freeze({
    sceneCode: 'blog.write',
    bizType: 'BLOG',
    requestType: 'BLOG_ASSISTANT',
    defaultAnalysisMode: 'DOC_QA',
    allowKnowledgeBinding: true,
    label: 'Current scene: Blog writing',
    defaultQuickPrompts: Object.freeze(toAiQuickPromptList('blog.write'))
  }),
  'blog.detail': Object.freeze({
    sceneCode: 'blog.detail',
    bizType: 'BLOG',
    requestType: 'BLOG_ASSISTANT',
    defaultAnalysisMode: 'DOC_QA',
    allowKnowledgeBinding: true,
    label: 'Current scene: Blog detail',
    defaultQuickPrompts: Object.freeze(toAiQuickPromptList('blog.detail'))
  }),
  'knowledge.base': Object.freeze({
    sceneCode: 'knowledge.base',
    bizType: 'GENERAL',
    requestType: 'KNOWLEDGE_QA',
    defaultAnalysisMode: 'DOC_QA',
    allowKnowledgeBinding: true,
    label: 'Current scene: Knowledge base',
    defaultQuickPrompts: Object.freeze(toAiQuickPromptList('knowledge.base'))
  }),
  'global.assistant': Object.freeze({
    sceneCode: 'global.assistant',
    bizType: 'GENERAL',
    requestType: 'CHAT',
    defaultAnalysisMode: 'DOC_QA',
    allowKnowledgeBinding: true,
    label: 'Current scene: Global assistant',
    defaultQuickPrompts: Object.freeze(toAiQuickPromptList('global.assistant'))
  })
})

const legacySceneAliasMap = Object.freeze({
  'project-detail': 'project.detail',
  projectdetail: 'project.detail',
  'project-board': 'project.board',
  projectboard: 'project.board',
  projectmanage: 'project.board',
  'project-merge': 'project.merge',
  projectmerge: 'project.merge',
  projectmergeconflict: 'project.merge',
  'blog-write': 'blog.write',
  blogwrite: 'blog.write',
  'blog-detail': 'blog.detail',
  blogdetail: 'blog.detail',
  'knowledge-base': 'knowledge.base',
  knowledgebase: 'knowledge.base',
  general: 'global.assistant'
})

function normalizeQuickPrompts(list = []) {
  return (Array.isArray(list) ? list : [])
    .map(item => {
      if (!item) return null
      if (typeof item === 'string') {
        return { label: item, prompt: item }
      }
      const prompt = String(item.prompt || item.value || item.text || '').trim()
      if (!prompt) return null
      const label = String(item.label || prompt).trim()
      return { label: label || prompt, prompt }
    })
    .filter(Boolean)
}

function resolveRoutePath(route) {
  return String((route && route.path) || '').toLowerCase()
}

function resolveRouteSceneCode(route) {
  const path = resolveRoutePath(route)
  if (!path) return DEFAULT_SCENE_CODE
  if (path.includes('/projectmergeconflict')) return 'project.merge'
  if (path.includes('/projectmanage')) return 'project.board'
  if (path.includes('/projectdetail')) return 'project.detail'
  if (path.includes('/blogwrite')) return 'blog.write'
  if (path.includes('/blogdetail')) return 'blog.detail'
  if (/\/blog\/[^/]+/.test(path)) return 'blog.detail'
  if (path.includes('/knowledge-base')) return 'knowledge.base'
  return DEFAULT_SCENE_CODE
}

function resolveProjectId({ route, currentKnowledgeBase = null, projectId = null } = {}) {
  const routeQueryId =
    (route && route.query && route.query.projectId) ||
    (route && route.params && route.params.projectId) ||
    null
  const fallbackId = currentKnowledgeBase && currentKnowledgeBase.projectId
  return Number(projectId || routeQueryId || fallbackId || 0) || null
}

function buildSceneLabel(scene, { currentKnowledgeBase = null } = {}) {
  if (!scene || !scene.sceneCode) return 'Current scene: Global assistant'
  if (scene.sceneCode === 'knowledge.base' && currentKnowledgeBase && currentKnowledgeBase.name) {
    return `${scene.label} / ${currentKnowledgeBase.name}`
  }
  return scene.label
}

function toResolvedScene(scene, { sceneCode = '', currentKnowledgeBase = null, projectId = null } = {}) {
  const registryCode = scene && scene.sceneCode ? scene.sceneCode : DEFAULT_SCENE_CODE
  const mergedCode = sceneCode || registryCode
  return {
    sceneCode: mergedCode,
    bizType: scene && scene.bizType ? scene.bizType : 'GENERAL',
    requestType: scene && scene.requestType ? scene.requestType : 'CHAT',
    defaultAnalysisMode: scene && scene.defaultAnalysisMode ? scene.defaultAnalysisMode : 'DOC_QA',
    allowKnowledgeBinding: scene ? scene.allowKnowledgeBinding !== false : true,
    defaultQuickPrompts: normalizeQuickPrompts(scene && scene.defaultQuickPrompts),
    label: buildSceneLabel(scene, { currentKnowledgeBase }),
    projectId: Number(projectId || 0) || null
  }
}

export function normalizeAiSceneCode(sceneCode = '') {
  const rawCode = String(sceneCode || '').trim()
  if (!rawCode) return ''
  const lowerCode = rawCode.toLowerCase()
  if (sceneRegistry[rawCode]) return rawCode
  if (sceneRegistry[lowerCode]) return lowerCode
  if (legacySceneAliasMap[rawCode]) return legacySceneAliasMap[rawCode]
  if (legacySceneAliasMap[lowerCode]) return legacySceneAliasMap[lowerCode]
  return rawCode
}

export function getAiSceneRegistration(sceneCode = '') {
  const normalizedCode = normalizeAiSceneCode(sceneCode)
  return sceneRegistry[normalizedCode] || null
}

export function resolveAiSceneMeta({ route, sceneCode = '', currentKnowledgeBase = null, projectId = null } = {}) {
  const normalizedInputCode = normalizeAiSceneCode(sceneCode)
  const routeSceneCode = resolveRouteSceneCode(route)
  const resolvedCode = normalizedInputCode || routeSceneCode || DEFAULT_SCENE_CODE
  const registration = getAiSceneRegistration(resolvedCode) || getAiSceneRegistration(DEFAULT_SCENE_CODE)
  const resolvedProjectId = resolveProjectId({ route, currentKnowledgeBase, projectId })
  return toResolvedScene(registration, {
    sceneCode: resolvedCode,
    currentKnowledgeBase,
    projectId: resolvedProjectId
  })
}

export function getAiSceneRegistry() {
  return Object.keys(sceneRegistry).map(key => toResolvedScene(sceneRegistry[key], { sceneCode: key }))
}
