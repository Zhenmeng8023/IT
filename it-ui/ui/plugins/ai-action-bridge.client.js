import { normalizeAiAssistantOpenPayload } from '@/utils/aiOpenPayload'
import { normalizeAiSceneCode } from '@/utils/aiSceneRegistry'

function normalizeText(value) {
  return String(value || '').trim()
}

function normalizeObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : null
}

function normalizeApplyTargets(value) {
  const rawList = Array.isArray(value) ? value : value ? [value] : []
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

function normalizeResultDetail(rawDetail) {
  const detail = normalizeObject(rawDetail) || {}
  const rawResponse = detail.rawResponse !== undefined ? detail.rawResponse : (detail.raw || null)
  const normalizedRaw = rawResponse && typeof rawResponse === 'object' ? rawResponse : rawResponse || null

  return {
    sceneCode: normalizeAiSceneCode(detail.sceneCode || detail.scene_code || detail.scene || ''),
    actionCode: normalizeText(detail.actionCode || detail.action_code || detail.action || '').toLowerCase(),
    structured: normalizeObject(detail.structured) || (normalizedRaw && normalizeObject(normalizedRaw.structured)) || null,
    displayText: normalizeText(detail.displayText || detail.text || ''),
    rawResponse: normalizedRaw,
    applyTargets: normalizeApplyTargets(
      detail.applyTargets || detail.applyTarget || (normalizedRaw && (normalizedRaw.applyTargets || normalizedRaw.applyTarget))
    ),
    origin: detail
  }
}

function normalizeFilterList(value, normalizer) {
  const list = Array.isArray(value) ? value : value ? [value] : []
  const normalized = list
    .map(item => normalizer(item))
    .filter(Boolean)
  return Array.from(new Set(normalized))
}

function createOpenBridge() {
  return function openAssistant(rawDetail = {}) {
    if (typeof window === 'undefined') return null
    const detail = normalizeAiAssistantOpenPayload(rawDetail)
    window.dispatchEvent(new CustomEvent('ai-assistant-open', { detail }))
    return detail
  }
}

function createResultSubscriber() {
  return function subscribeResult(handler, options = {}) {
    if (typeof window === 'undefined' || typeof handler !== 'function') {
      return () => {}
    }

    const allowedScenes = normalizeFilterList(options.sceneCode || options.sceneCodes, item => normalizeAiSceneCode(item || ''))
    const allowedActions = normalizeFilterList(options.actionCode || options.actionCodes, item => normalizeText(item).toLowerCase())

    const listener = event => {
      const detail = normalizeResultDetail(event && event.detail)
      if (allowedScenes.length && !allowedScenes.includes(detail.sceneCode)) return
      if (allowedActions.length && !allowedActions.includes(detail.actionCode)) return
      handler(detail, event)
    }

    window.addEventListener('ai-assistant-result', listener)
    return () => {
      window.removeEventListener('ai-assistant-result', listener)
    }
  }
}

export default (_, inject) => {
  const bridge = {
    open: createOpenBridge(),
    subscribeResult: createResultSubscriber(),
    normalizeResultDetail
  }

  inject('aiActionBridge', bridge)
}
