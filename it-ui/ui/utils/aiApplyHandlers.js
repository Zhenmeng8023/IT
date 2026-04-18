import {
  matchSystemTagsByNames,
  normalizeBlogPolishPayload,
  normalizeBlogSummaryPayload
} from '@/api/aiAssistant'
import {
  normalizeProjectSummaryPayload,
  normalizeProjectTaskPayload
} from '@/api/aiAssistant'
import { recordAiUxMetric } from '@/utils/aiUxMetrics'

function normalizeText(value) {
  if (value === null || value === undefined) return ''
  return String(value).trim()
}

function normalizeObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : null
}

function normalizeTagId(value) {
  if (value === null || value === undefined || value === '') return null
  const num = Number(value)
  if (Number.isFinite(num) && num > 0) return num
  return null
}

function uniqueTextList(list = []) {
  const seen = new Set()
  return (Array.isArray(list) ? list : [])
    .map(item => normalizeText(item).replace(/^#/, ''))
    .filter(Boolean)
    .filter(item => {
      const key = item.toLowerCase()
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
}

function normalizeApplyTargets(value) {
  if (!value) return []
  const rawList = Array.isArray(value) ? value : [value]
  return rawList
    .map(item => {
      if (typeof item === 'string') return normalizeText(item).toLowerCase()
      if (item && typeof item === 'object') {
        return normalizeText(item.code || item.target || item.applyTarget || item.name).toLowerCase()
      }
      return ''
    })
    .filter(Boolean)
}

function resolveStructured(detail = {}) {
  const structured = normalizeObject(detail.structured)
  if (structured) return structured
  const rawResponse = normalizeObject(detail.rawResponse)
  if (!rawResponse) return null
  return normalizeObject(rawResponse.structured)
}

function resolveDisplayText(detail = {}) {
  const direct = normalizeText(detail.displayText)
  if (direct) return direct
  const raw = normalizeObject(detail.rawResponse)
  if (!raw) return ''
  return normalizeText(raw.text || raw.content || raw.message || raw.answer || '')
}

function buildNormalizeSource(detail = {}) {
  return {
    structured: resolveStructured(detail) || undefined,
    text: resolveDisplayText(detail)
  }
}

function resolveTagNames(list = []) {
  const source = Array.isArray(list) ? list : []
  return uniqueTextList(source.map(item => {
    if (item && typeof item === 'object') {
      return item.name || item.label || item.tag || item.value || ''
    }
    return item
  }))
}

function toTextList(list = []) {
  return uniqueTextList((Array.isArray(list) ? list : []).map(item => {
    if (item && typeof item === 'object') {
      return item.title || item.name || item.label || item.value || item.text || ''
    }
    return item
  }))
}

function mergeTagIds(currentTags = [], incomingTags = []) {
  const current = (Array.isArray(currentTags) ? currentTags : [])
    .map(item => {
      if (item && typeof item === 'object') return normalizeTagId(item.id || item.value)
      return normalizeTagId(item)
    })
    .filter(Boolean)
  const incoming = (Array.isArray(incomingTags) ? incomingTags : [])
    .map(item => normalizeTagId(item))
    .filter(Boolean)
  return Array.from(new Set([...current, ...incoming]))
}

function applyPolish(vm, detail = {}) {
  const normalized = normalizeBlogPolishPayload(buildNormalizeSource(detail))
  const polishedContent = normalizeText(
    normalized.polishedContent || (resolveStructured(detail) || {}).polishedContent || resolveDisplayText(detail)
  )

  vm.aiPolishCard = {
    polishedContent,
    changeSummary: Array.isArray(normalized.changeSummary) ? normalized.changeSummary : [],
    warnings: Array.isArray(normalized.warnings) ? normalized.warnings : [],
    titleSuggestions: Array.isArray(normalized.titleSuggestions) ? normalized.titleSuggestions : [],
    rawText: normalized.rawText || resolveDisplayText(detail)
  }
  vm.aiPolishResult = polishedContent || resolveDisplayText(detail)
  vm.showAiResult = true
  vm.aiResultTab = 'polish'

  if (!polishedContent) {
    return {
      applied: false,
      message: 'AI polish result is empty, skipped applying content.',
      updatedFields: []
    }
  }

  vm.blog.content = polishedContent
  if (vm.quill && vm.quill.root) {
    vm.quill.root.innerHTML = polishedContent
  }

  const result = {
    applied: true,
    message: 'AI polished content has been applied to the editor.',
    updatedFields: ['blog.content', 'editor']
  }
  recordAiUxMetric('applySuccess', { sceneCode: 'blog.write', actionCode: 'blog.polish' })
  return result
}

function applySummary(vm, detail = {}) {
  const normalized = normalizeBlogSummaryPayload(buildNormalizeSource(detail))
  const structured = resolveStructured(detail) || {}
  const summary = normalizeText(normalized.summary || structured.summary || resolveDisplayText(detail))
  const rawTagNames = resolveTagNames(
    structured.tags || normalized.tags || structured.tagList || structured.labels || []
  )
  const matchedTags = matchSystemTagsByNames(rawTagNames, vm.tagOptions || [])

  vm.aiSummaryResult = summary || resolveDisplayText(detail)
  vm.aiSummaryCard = {
    summary,
    tags: rawTagNames,
    rejectedTags: resolveTagNames(structured.rejectTags || structured.rejectedTags || normalized.rejectedTags || []),
    rawText: normalized.rawText || resolveDisplayText(detail)
  }
  vm.aiSuggestedTags = matchedTags
  vm.showAiResult = true
  vm.aiResultTab = 'summary'

  const updatedFields = []

  if (summary) {
    vm.blog.summary = summary
    updatedFields.push('blog.summary')
  }

  if (matchedTags.length) {
    vm.blog.tags = mergeTagIds(vm.blog.tags, matchedTags.map(item => item.id))
    updatedFields.push('blog.tags')
  }

  if (!updatedFields.length) {
    return {
      applied: false,
      message: 'AI summary result has no applicable summary or matched tags.',
      updatedFields
    }
  }

  const result = {
    applied: true,
    message: 'AI summary result has been applied to summary/tags.',
    updatedFields
  }
  recordAiUxMetric('applySuccess', { sceneCode: 'blog.write', actionCode: 'blog.summary' })
  return result
}

export function resolveBlogWriteActionCode(detail = {}) {
  const actionCode = normalizeText(detail.actionCode).toLowerCase()
  if (actionCode === 'blog.polish' || actionCode === 'blog.summary') return actionCode

  const applyTargets = normalizeApplyTargets(detail.applyTargets)
  if (applyTargets.includes('blog.polish')) return 'blog.polish'
  if (applyTargets.includes('blog.summary') || applyTargets.includes('blog.tags') || applyTargets.includes('blog.reject-tags')) {
    return 'blog.summary'
  }

  const sceneCode = normalizeText(detail.sceneCode).toLowerCase()
  if (sceneCode === 'blog.polish') return 'blog.polish'
  if (sceneCode === 'blog.summary') return 'blog.summary'

  return ''
}

export function createBlogWriteAiApplyHandlers(vm) {
  return {
    'blog.polish': detail => applyPolish(vm, detail),
    'blog.summary': detail => applySummary(vm, detail)
  }
}

function normalizeRiskLevel(value) {
  const level = normalizeText(value).toUpperCase()
  if (!level) return 'MEDIUM'
  if (['HIGH', 'MEDIUM', 'LOW', 'P0', 'P1', 'P2', 'P3'].includes(level)) return level
  return level
}

function normalizeProjectRiskItems(detail = {}) {
  const structured = resolveStructured(detail) || {}
  const directList = Array.isArray(structured.risks) ? structured.risks : []
  const items = directList
    .map(item => {
      if (typeof item === 'string') {
        const text = normalizeText(item)
        return text ? { title: text, level: '', impact: '', mitigation: '' } : null
      }
      const risk = normalizeObject(item) || {}
      const title = normalizeText(risk.title || risk.name || risk.label || risk.text || risk.summary)
      if (!title) return null
      return {
        title,
        level: normalizeRiskLevel(risk.level || risk.priority || risk.severity),
        impact: normalizeText(risk.impact || risk.effect || risk.reason),
        mitigation: normalizeText(risk.mitigation || risk.suggestion || risk.action)
      }
    })
    .filter(Boolean)

  if (items.length) return items.slice(0, 6)

  return toTextList(structured.riskList || structured.keyRisks || []).map(text => ({
    title: text,
    level: '',
    impact: '',
    mitigation: ''
  })).slice(0, 6)
}

function normalizeProjectNextStepItems(detail = {}) {
  const structured = resolveStructured(detail) || {}
  const directList = Array.isArray(structured.nextSteps)
    ? structured.nextSteps
    : Array.isArray(structured.nextActions)
      ? structured.nextActions
      : []

  return directList
    .map(item => {
      if (typeof item === 'string') {
        const text = normalizeText(item)
        return text ? { title: text, owner: '', timeframe: '', expectedOutcome: '' } : null
      }
      const step = normalizeObject(item) || {}
      const title = normalizeText(step.title || step.name || step.label || step.text || step.action)
      if (!title) return null
      return {
        title,
        owner: normalizeText(step.owner || step.assignee || step.role),
        timeframe: normalizeText(step.timeframe || step.when || step.deadline),
        expectedOutcome: normalizeText(step.expectedOutcome || step.outcome || step.deliverable)
      }
    })
    .filter(Boolean)
    .slice(0, 6)
}

function applyProjectSummary(vm, detail = {}) {
  const normalized = normalizeProjectSummaryPayload(buildNormalizeSource(detail))
  vm.aiSummaryCard = {
    overview: normalizeText(normalized.overview),
    scenarios: toTextList(normalized.scenarios),
    features: toTextList(normalized.features),
    risks: toTextList(normalized.risks),
    nextActions: toTextList(normalized.nextActions),
    rawText: normalized.rawText || resolveDisplayText(detail)
  }
  vm.aiProjectSummary = normalizeText(normalized.displayText || resolveDisplayText(detail))
  vm.aiActiveTab = 'summary'

  if (vm.aiSummaryCard.risks.length) {
    vm.aiRiskCard = {
      overview: vm.aiSummaryCard.overview,
      items: vm.aiSummaryCard.risks.map(item => ({ title: item, level: '', impact: '', mitigation: '' })),
      rawText: vm.aiSummaryCard.rawText
    }
  }

  if (vm.aiSummaryCard.nextActions.length) {
    vm.aiNextStepsCard = {
      overview: vm.aiSummaryCard.overview,
      items: vm.aiSummaryCard.nextActions.map(item => ({ title: item, owner: '', timeframe: '', expectedOutcome: '' })),
      milestones: [],
      rawText: vm.aiSummaryCard.rawText
    }
  }

  const applied = Boolean(
    vm.aiSummaryCard.overview ||
    vm.aiSummaryCard.scenarios.length ||
    vm.aiSummaryCard.features.length ||
    vm.aiSummaryCard.risks.length ||
    vm.aiSummaryCard.nextActions.length
  )
  if (applied) {
    recordAiUxMetric('applySuccess', { sceneCode: 'project.detail', actionCode: 'project.detail.summary' })
  }

  return {
    applied,
    message: applied ? 'AI 项目概览草稿已更新。' : 'AI 项目概览结果为空，已跳过回填。',
    updatedFields: applied ? ['aiSummaryCard', 'aiRiskCard', 'aiNextStepsCard'] : []
  }
}

function applyProjectTasks(vm, detail = {}) {
  const normalized = normalizeProjectTaskPayload(buildNormalizeSource(detail))
  vm.aiTaskCard = {
    phases: Array.isArray(normalized.phases) ? normalized.phases : [],
    executionOrder: toTextList(normalized.executionOrder),
    risks: toTextList(normalized.risks),
    rawText: normalized.rawText || resolveDisplayText(detail)
  }
  vm.aiProjectTasks = normalizeText(normalized.displayText || resolveDisplayText(detail))
  vm.aiActiveTab = 'tasks'

  if (vm.aiTaskCard.risks.length) {
    vm.aiRiskCard = {
      overview: normalizeText((resolveStructured(detail) || {}).overview || ''),
      items: vm.aiTaskCard.risks.map(item => ({ title: item, level: '', impact: '', mitigation: '' })),
      rawText: vm.aiTaskCard.rawText
    }
  }

  const applied = Boolean(vm.aiTaskCard.phases.length || vm.aiTaskCard.executionOrder.length || vm.aiTaskCard.risks.length)
  if (applied) {
    recordAiUxMetric('applySuccess', { sceneCode: 'project.detail', actionCode: 'project.detail.tasks' })
  }

  return {
    applied,
    message: applied ? 'AI 任务草稿已更新。' : 'AI 任务拆解结果为空，已跳过回填。',
    updatedFields: applied ? ['aiTaskCard', 'aiRiskCard'] : []
  }
}

function applyProjectRisks(vm, detail = {}) {
  const structured = resolveStructured(detail) || {}
  const items = normalizeProjectRiskItems(detail)
  const overview = normalizeText(structured.overview || resolveDisplayText(detail))
  vm.aiRiskCard = {
    overview,
    items,
    rawText: resolveDisplayText(detail)
  }
  if (items.length) {
    vm.aiSummaryCard = {
      ...vm.aiSummaryCard,
      risks: items.map(item => item.title)
    }
  }
  vm.aiActiveTab = 'risks'
  const applied = Boolean(overview || items.length)
  if (applied) {
    recordAiUxMetric('applySuccess', { sceneCode: 'project.detail', actionCode: 'project.detail.risks' })
  }
  return {
    applied,
    message: applied ? 'AI 风险提示草稿已更新。' : 'AI 风险结果为空，已跳过回填。',
    updatedFields: applied ? ['aiRiskCard', 'aiSummaryCard.risks'] : []
  }
}

function applyProjectNextSteps(vm, detail = {}) {
  const structured = resolveStructured(detail) || {}
  const items = normalizeProjectNextStepItems(detail)
  const milestones = toTextList(structured.milestones || structured.checkpoints || [])
  const overview = normalizeText(structured.overview || resolveDisplayText(detail))
  vm.aiNextStepsCard = {
    overview,
    items,
    milestones,
    rawText: resolveDisplayText(detail)
  }
  if (items.length) {
    vm.aiSummaryCard = {
      ...vm.aiSummaryCard,
      nextActions: items.map(item => item.title)
    }
  }
  vm.aiActiveTab = 'next-steps'
  const applied = Boolean(overview || items.length || milestones.length)
  if (applied) {
    recordAiUxMetric('applySuccess', { sceneCode: 'project.detail', actionCode: 'project.detail.next-steps' })
  }
  return {
    applied,
    message: applied ? 'AI 下一步建议草稿已更新。' : 'AI 下一步建议结果为空，已跳过回填。',
    updatedFields: applied ? ['aiNextStepsCard', 'aiSummaryCard.nextActions'] : []
  }
}

export function resolveProjectDetailActionCode(detail = {}) {
  const actionCode = normalizeText(detail.actionCode).toLowerCase()
  if ([
    'project.detail.summary',
    'project.detail.tasks',
    'project.detail.risks',
    'project.detail.next-steps'
  ].includes(actionCode)) {
    return actionCode
  }

  const aliasMap = {
    'project-summary': 'project.detail.summary',
    'project.summary': 'project.detail.summary',
    'project-tasks': 'project.detail.tasks',
    'project.tasks': 'project.detail.tasks',
    'project-risks': 'project.detail.risks',
    'project.risks': 'project.detail.risks',
    'project-next-steps': 'project.detail.next-steps',
    'project.next-steps': 'project.detail.next-steps',
    'project.nextsteps': 'project.detail.next-steps'
  }
  if (aliasMap[actionCode]) return aliasMap[actionCode]

  const applyTargets = normalizeApplyTargets(detail.applyTargets)
  if (applyTargets.includes('project.detail.summary') || applyTargets.includes('project.summary')) return 'project.detail.summary'
  if (applyTargets.includes('project.detail.tasks') || applyTargets.includes('project.tasks')) return 'project.detail.tasks'
  if (applyTargets.includes('project.detail.risks') || applyTargets.includes('project.risks')) return 'project.detail.risks'
  if (applyTargets.includes('project.detail.next-steps') || applyTargets.includes('project.next-steps')) return 'project.detail.next-steps'

  return ''
}

export function createProjectDetailAiApplyHandlers(vm) {
  return {
    'project.detail.summary': detail => applyProjectSummary(vm, detail),
    'project.detail.tasks': detail => applyProjectTasks(vm, detail),
    'project.detail.risks': detail => applyProjectRisks(vm, detail),
    'project.detail.next-steps': detail => applyProjectNextSteps(vm, detail)
  }
}
