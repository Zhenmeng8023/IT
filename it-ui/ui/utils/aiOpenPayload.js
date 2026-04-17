import { isValidAnalysisMode } from '@/utils/aiModePreference'
import { normalizeAiSceneCode } from '@/utils/aiSceneRegistry'

function normalizeString(value = '') {
  return String(value || '').trim()
}

function normalizeBoolean(value, defaultValue = true) {
  if (value === undefined || value === null) return defaultValue
  if (typeof value === 'boolean') return value
  const normalized = String(value).trim().toLowerCase()
  if (['0', 'false', 'no', 'off'].includes(normalized)) return false
  if (['1', 'true', 'yes', 'on'].includes(normalized)) return true
  return defaultValue
}

function normalizeObject(value) {
  if (!value) return null
  if (typeof value === 'object' && !Array.isArray(value)) return value
  return null
}

function normalizeKnowledgeBaseIds(rawValue) {
  const flatten = (input) => {
    if (Array.isArray(input)) return input.flatMap(flatten)
    if (typeof input === 'string') {
      const trimmed = input.trim()
      if (!trimmed) return []
      if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
        try {
          return flatten(JSON.parse(trimmed))
        } catch (e) {
          return trimmed
            .slice(1, -1)
            .split(',')
            .map(item => item.trim())
            .filter(Boolean)
        }
      }
      return trimmed.split(',').map(item => item.trim()).filter(Boolean)
    }
    return [input]
  }

  const seen = new Set()
  return flatten(rawValue)
    .map(item => Number(item) || null)
    .filter(Boolean)
    .filter(item => {
      if (seen.has(item)) return false
      seen.add(item)
      return true
    })
}

function pickFirst(detail, keys = []) {
  for (const key of keys) {
    if (detail[key] !== undefined) return detail[key]
  }
  return undefined
}

export function normalizeAiAssistantOpenPayload(rawDetail) {
  const detail = typeof rawDetail === 'string'
    ? { prompt: rawDetail }
    : rawDetail && typeof rawDetail === 'object'
      ? rawDetail
      : {}

  const prompt = normalizeString(pickFirst(detail, ['prompt', 'question', 'input', 'text']) || '')
  const source = normalizeString(pickFirst(detail, ['source', 'from', 'entry']) || 'unknown')
  const sceneCode = normalizeAiSceneCode(pickFirst(detail, ['sceneCode', 'scene_code', 'scene']) || '')
  const actionCode = normalizeString(pickFirst(detail, ['actionCode', 'action_code', 'action']) || '')
  const contextPayload = normalizeObject(
    pickFirst(detail, ['contextPayload', 'context_payload', 'context', 'payload', 'requestParams'])
  )

  const preferredModeRaw = normalizeString(
    pickFirst(detail, ['preferredAnalysisMode', 'preferred_analysis_mode', 'analysisMode', 'analysis_mode', 'mode']) || ''
  ).toUpperCase()
  const preferredAnalysisMode = isValidAnalysisMode(preferredModeRaw) ? preferredModeRaw : ''

  const kbIdsRaw = pickFirst(detail, ['knowledgeBaseIds', 'knowledge_base_ids', 'kbIds', 'knowledgeBaseId', 'kbId'])
  const knowledgeBaseIds = normalizeKnowledgeBaseIds(kbIdsRaw)

  return {
    prompt,
    autoSend: normalizeBoolean(detail.autoSend, true),
    source,
    sceneCode,
    actionCode,
    contextPayload,
    knowledgeBaseIds,
    preferredAnalysisMode,
    sessionId: pickFirst(detail, ['sessionId', 'aiSessionId']) || null
  }
}
