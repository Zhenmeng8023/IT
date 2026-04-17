import {
  matchSystemTagsByNames,
  normalizeBlogPolishPayload,
  normalizeBlogSummaryPayload
} from '@/api/aiAssistant'

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

  return {
    applied: true,
    message: 'AI polished content has been applied to the editor.',
    updatedFields: ['blog.content', 'editor']
  }
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

  return {
    applied: true,
    message: 'AI summary result has been applied to summary/tags.',
    updatedFields
  }
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
