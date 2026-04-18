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

function createContextCollectorRegistry() {
  const registry = new Map()

  function getKey(sceneCode = '') {
    return normalizeAiSceneCode(sceneCode || '')
  }

  function readList(sceneCode = '') {
    const key = getKey(sceneCode)
    if (!key) return []
    if (!registry.has(key)) {
      registry.set(key, [])
    }
    return registry.get(key)
  }

  return {
    register(sceneCode, collector) {
      if (typeof collector !== 'function') return () => {}
      const key = getKey(sceneCode)
      if (!key) return () => {}
      const list = readList(key)
      const entry = { collector }
      list.push(entry)
      return () => {
        const nextList = readList(key).filter(item => item !== entry)
        registry.set(key, nextList)
      }
    },
    collect(sceneCode, options = {}) {
      const key = getKey(sceneCode)
      if (!key) return null
      const list = registry.get(key) || []
      const current = list[list.length - 1]
      if (!current || typeof current.collector !== 'function') return null
      try {
        const payload = current.collector(options)
        return payload && typeof payload === 'object' && !Array.isArray(payload) ? payload : null
      } catch (error) {
        console.error('[aiActionBridge] collect context failed:', error)
        return null
      }
    }
  }
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

function createApplyHandlerBinder(subscribeResult) {
  return function bindApplyHandlers({
    sceneCode = '',
    actionCode = '',
    resolveActionCode = null,
    handlerMap = {},
    onHandled = null,
    onSkipped = null,
    onError = null
  } = {}) {
    return subscribeResult(detail => {
      const resolvedActionCode = typeof resolveActionCode === 'function'
        ? normalizeText(resolveActionCode(detail)).toLowerCase()
        : normalizeText(detail.actionCode).toLowerCase()

      if (!resolvedActionCode) {
        if (typeof onSkipped === 'function') onSkipped(detail, '')
        return
      }

      const applyHandler = handlerMap && typeof handlerMap === 'object' ? handlerMap[resolvedActionCode] : null
      if (typeof applyHandler !== 'function') {
        if (typeof onSkipped === 'function') onSkipped(detail, resolvedActionCode)
        return
      }

      try {
        const result = applyHandler(detail, resolvedActionCode)
        if (typeof onHandled === 'function') onHandled(result, detail, resolvedActionCode)
      } catch (error) {
        if (typeof onError === 'function') {
          onError(error, detail, resolvedActionCode)
          return
        }
        console.error('[aiActionBridge] apply handler failed:', error)
      }
    }, { sceneCode, actionCode })
  }
}

export default (_, inject) => {
  const contextRegistry = createContextCollectorRegistry()
  const subscribeResult = createResultSubscriber()
  const bridge = {
    open: createOpenBridge(),
    subscribeResult,
    bindApplyHandlers: createApplyHandlerBinder(subscribeResult),
    registerContextCollector: (sceneCode, collector) => contextRegistry.register(sceneCode, collector),
    collectContext: (sceneCode, options = {}) => contextRegistry.collect(sceneCode, options),
    normalizeResultDetail
  }

  inject('aiActionBridge', bridge)
}
