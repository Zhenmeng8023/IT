const ERROR_TYPE = {
  TIMEOUT: 'timeout',
  UNAUTHORIZED: 'unauthorized',
  FORBIDDEN: 'forbidden',
  SERVER: 'server',
  STREAM_INTERRUPTED: 'stream_interrupted',
  CANCELED: 'canceled',
  NETWORK: 'network',
  HTTP: 'http',
  UNKNOWN: 'unknown'
}

const ERROR_LABEL = {
  [ERROR_TYPE.TIMEOUT]: '请求超时',
  [ERROR_TYPE.UNAUTHORIZED]: '登录已失效',
  [ERROR_TYPE.FORBIDDEN]: '没有权限',
  [ERROR_TYPE.SERVER]: '服务异常',
  [ERROR_TYPE.STREAM_INTERRUPTED]: '流式响应中断',
  [ERROR_TYPE.CANCELED]: '已取消',
  [ERROR_TYPE.NETWORK]: '网络异常',
  [ERROR_TYPE.HTTP]: '请求失败',
  [ERROR_TYPE.UNKNOWN]: '未知错误'
}

function normalizeStatus(value) {
  const status = Number(value || 0)
  return Number.isFinite(status) && status > 0 ? status : 0
}

function readErrorMessage(error) {
  const candidates = [
    error && error.aiMessage,
    error && error.message,
    error && error.response && error.response.data && error.response.data.message,
    error && error.response && error.response.data && error.response.data.msg,
    error && error.response && error.response.data && error.response.data.error,
    error && error.data && error.data.message,
    error && error.data && error.data.msg,
    error && error.statusText
  ]
  for (const item of candidates) {
    if (item !== undefined && item !== null && String(item).trim() !== '') {
      return String(item).trim()
    }
  }
  return ''
}

function classifyAiError(error, fallbackMessage) {
  if (error && error.aiError) {
    return error.aiError
  }

  const status = normalizeStatus(
    error && (error.status || (error.response && error.response.status))
  )
  const code = String((error && (error.code || error.name)) || '')
  const rawMessage = readErrorMessage(error)
  const lowerMessage = rawMessage.toLowerCase()
  let type = ERROR_TYPE.UNKNOWN

  if (
    code === 'AbortError' ||
    code === 'ERR_CANCELED' ||
    lowerMessage.includes('aborted') ||
    lowerMessage.includes('cancel')
  ) {
    type = ERROR_TYPE.CANCELED
  } else if (
    code === 'ECONNABORTED' ||
    code === 'TimeoutError' ||
    lowerMessage.includes('timeout') ||
    lowerMessage.includes('超时')
  ) {
    type = ERROR_TYPE.TIMEOUT
  } else if (status === 401) {
    type = ERROR_TYPE.UNAUTHORIZED
  } else if (status === 403) {
    type = ERROR_TYPE.FORBIDDEN
  } else if (status >= 500) {
    type = ERROR_TYPE.SERVER
  } else if (
    code === 'TypeError' ||
    lowerMessage.includes('failed to fetch') ||
    lowerMessage.includes('network') ||
    lowerMessage.includes('load failed') ||
    lowerMessage.includes('网络')
  ) {
    type = ERROR_TYPE.NETWORK
  } else if (status >= 400) {
    type = ERROR_TYPE.HTTP
  }

  const label = ERROR_LABEL[type] || ERROR_LABEL[ERROR_TYPE.UNKNOWN]
  const message = rawMessage || fallbackMessage || label
  return {
    type,
    status,
    label,
    message,
    retryable: [
      ERROR_TYPE.TIMEOUT,
      ERROR_TYPE.SERVER,
      ERROR_TYPE.STREAM_INTERRUPTED,
      ERROR_TYPE.NETWORK,
      ERROR_TYPE.HTTP
    ].includes(type)
  }
}

function createAiError(type, message, extra) {
  const normalizedType = ERROR_LABEL[type] ? type : ERROR_TYPE.UNKNOWN
  const error = new Error(message || ERROR_LABEL[normalizedType])
  error.name = 'AiRuntimeError'
  error.aiError = {
    type: normalizedType,
    status: normalizeStatus(extra && extra.status),
    label: ERROR_LABEL[normalizedType],
    message: message || ERROR_LABEL[normalizedType],
    retryable: [
      ERROR_TYPE.TIMEOUT,
      ERROR_TYPE.SERVER,
      ERROR_TYPE.STREAM_INTERRUPTED,
      ERROR_TYPE.NETWORK,
      ERROR_TYPE.HTTP
    ].includes(normalizedType),
    ...(extra || {})
  }
  return error
}

function unwrapApiPayload(res) {
  if (res == null) return null
  const payload = res && res.data !== undefined ? res.data : res
  if (
    payload &&
    typeof payload === 'object' &&
    payload.data !== undefined &&
    (payload.code !== undefined || payload.success !== undefined || payload.message !== undefined)
  ) {
    return payload.data
  }
  return payload
}

function extractAnswer(data) {
  if (!data) return ''
  return data.answer || data.content || data.message || data.responseText || data.text || ''
}

function extractStreamDeltaText(chunk) {
  if (chunk === null || chunk === undefined) return ''
  if (typeof chunk === 'string') return chunk
  return (
    chunk.delta ||
    chunk.contentDelta ||
    chunk.answerDelta ||
    chunk.content ||
    chunk.answer ||
    chunk.message ||
    chunk.responseText ||
    chunk.text ||
    ''
  )
}

function parseSseEvent(raw) {
  const event = {
    event: '',
    id: '',
    data: ''
  }
  const lines = String(raw || '').split(/\r?\n/)
  const dataLines = []

  for (const line of lines) {
    if (!line || line.startsWith(':')) continue
    const sep = line.indexOf(':')
    const field = sep >= 0 ? line.slice(0, sep).trim() : line.trim()
    const value = sep >= 0 ? line.slice(sep + 1).replace(/^\s/, '') : ''
    if (field === 'event') event.event = value
    if (field === 'id') event.id = value
    if (field === 'data') dataLines.push(value)
  }

  event.data = dataLines.join('\n')
  return event
}

function consumeSseBuffer(buffer, emit) {
  const normalized = String(buffer || '').replace(/\r\n/g, '\n')
  const parts = normalized.split('\n\n')
  const rest = parts.pop() || ''

  for (const part of parts) {
    const event = parseSseEvent(part)
    if (!event.data) continue
    emit && emit(event)
  }

  return rest
}

function parseStreamPayload(raw) {
  if (raw === undefined || raw === null) return null
  const text = String(raw).trim()
  if (!text || text === '[DONE]') return null
  try {
    return JSON.parse(text)
  } catch (error) {
    return text
  }
}

function isTerminalStreamChunk(chunk) {
  if (!chunk || typeof chunk !== 'object') return false
  return chunk.finished === true || chunk.done === true || chunk.type === 'done' || chunk.event === 'done'
}

function normalizeAiSource(item, extra) {
  const source = item || {}
  const document = source.document || {}
  const chunk = source.chunk || {}
  const knowledgeBase = source.knowledgeBase || source.kb || {}
  return {
    id: source.id || source.chunkId || chunk.id || source.documentId || document.id || null,
    callLogId: (extra && extra.callLogId) || source.callLogId || null,
    title: source.documentTitle || source.title || source.documentName || document.title || document.name || '',
    documentId: source.documentId || document.id || null,
    documentTitle: source.documentTitle || source.title || document.title || document.name || '',
    knowledgeBaseId:
      source.knowledgeBaseId ||
      knowledgeBase.id ||
      (extra && extra.knowledgeBaseId) ||
      null,
    knowledgeBaseName:
      source.knowledgeBaseName ||
      source.kbName ||
      knowledgeBase.name ||
      (extra && extra.knowledgeBaseName) ||
      '',
    chunkId: source.chunkId || chunk.id || null,
    chunkIndex:
      source.chunkIndex !== undefined && source.chunkIndex !== null
        ? source.chunkIndex
        : chunk.chunkIndex !== undefined && chunk.chunkIndex !== null
          ? chunk.chunkIndex
          : source.rankNo !== undefined && source.rankNo !== null
            ? source.rankNo
            : null,
    rankNo: source.rankNo !== undefined && source.rankNo !== null ? source.rankNo : null,
    score: source.score !== undefined ? source.score : null,
    keywordScore: source.keywordScore !== undefined ? source.keywordScore : null,
    vectorScore: source.vectorScore !== undefined ? source.vectorScore : null,
    retrievalMethod: source.retrievalMethod || source.method || '',
    fileName: source.fileName || document.fileName || '',
    archiveEntryPath: source.archiveEntryPath || source.path || document.archiveEntryPath || '',
    snippet: source.snippet || '',
    content: source.content || source.chunkContent || source.text || source.snippet || chunk.content || ''
  }
}

function normalizeAiSources(list, extra) {
  if (!Array.isArray(list)) return []
  return list.filter(Boolean).map(item => normalizeAiSource(item, extra || {}))
}

function extractAiSources(data, extra) {
  if (!data || typeof data !== 'object') return []
  const list =
    data.citations ||
    data.sources ||
    data.references ||
    data.retrievals ||
    data.retrievedChunks ||
    data.hits ||
    []
  return normalizeAiSources(Array.isArray(list) ? list : [], extra || {})
}

module.exports = {
  ERROR_TYPE,
  ERROR_LABEL,
  classifyAiError,
  createAiError,
  unwrapApiPayload,
  extractAnswer,
  extractStreamDeltaText,
  consumeSseBuffer,
  parseSseEvent,
  parseStreamPayload,
  isTerminalStreamChunk,
  normalizeAiSource,
  normalizeAiSources,
  extractAiSources
}
