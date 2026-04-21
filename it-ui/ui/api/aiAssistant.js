import request from '@/utils/request'
import { buildAuthHeaders as buildSharedAuthHeaders, getAccessToken, getCurrentUser, getToken } from '@/utils/auth'
import { API_V1_BASE_URL } from '@/utils/backend'
import {
  ERROR_TYPE,
  classifyAiError,
  consumeSseBuffer,
  createAiError,
  isTerminalStreamChunk,
  parseStreamPayload
} from '@/utils/aiRuntime'
import { listPromptTemplatesByScene, createAiFeedback } from '@/api/aiAdmin'

const CHAT_BASE = '/ai/chat'
const MODEL_BASE = '/ai/models'

function getApiBaseUrl() {
  const baseURL = request?.defaults?.baseURL || API_V1_BASE_URL
  return String(baseURL).replace(/\/$/, '')
}

function buildAuthHeaders(extraHeaders = {}) {
  return {
    'Content-Type': 'application/json',
    ...buildSharedAuthHeaders(extraHeaders),
    ...extraHeaders
  }
}

function unwrapApiPayload(res) {
  if (res == null) return null
  const payload = res.data !== undefined ? res.data : res
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
  return data.answer || data.content || data.message || data.responseText || ''
}

function toArray(value) {
  if (Array.isArray(value)) {
    return value.filter(Boolean)
  }
  if (typeof value === 'string') {
    return value
      .split(/[\n,，、|]+/)
      .map(item => item.trim())
      .filter(Boolean)
  }
  return []
}

function pickFirst(...values) {
  for (const value of values) {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      return value
    }
  }
  return ''
}

function cleanLinePrefix(text = '') {
  return String(text)
    .replace(/^[-*鈥d]+[.\s銆侊級)]*/, '')
    .trim()
}

function splitTagText(text = '') {
  return String(text)
    .replace(/[，?|]/g, ',')
    .replace(/[，、]/g, ',')
    .split(',')
    .map(item => cleanLinePrefix(item))
    .filter(Boolean)
}

function safeJsonParse(raw) {
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}

function unwrapUserLike(value) {
  if (!value || typeof value !== 'object') return null
  const stack = [value]
  const seen = new Set()
  while (stack.length > 0) {
    const current = stack.shift()
    if (!current || typeof current !== 'object' || seen.has(current)) continue
    seen.add(current)
    const id = Number(
      current.id ||
      current.userId ||
      current.uid ||
      current.user_id ||
      current.memberId ||
      current.accountId ||
      0
    ) || null
    if (id) {
      return {
        ...current,
        id
      }
    }
    const nextCandidates = [
      current.data,
      current.user,
      current.userInfo,
      current.profile,
      current.loginUser,
      current.currentUser,
      current.account,
      current.result
    ]
    nextCandidates.forEach(item => {
      if (item && typeof item === 'object') stack.push(item)
    })
  }
  return null
}

export function extractErrorMessage(err, fallback = '璇锋眰澶辫触锛岃绋嶅悗閲嶈瘯') {
  const aiError = classifyAiError(err, fallback)
  if (aiError && aiError.message) {
    return aiError.message
  }
  const candidates = [
    err?.response?.data?.message,
    err?.response?.data?.msg,
    err?.response?.data?.error,
    err?.data?.message,
    err?.data?.msg,
    err?.message
  ]
  for (const item of candidates) {
    if (item !== undefined && item !== null && String(item).trim() !== '') {
      return String(item).trim()
    }
  }
  return fallback
}

export function getCurrentAiToken() {
  return getAccessToken()
}

export function getCurrentAiUserProfile() {
  return unwrapUserLike(getCurrentUser())
}

export function getCurrentAiUserId() {
  const profile = getCurrentAiUserProfile()
  if (profile && profile.id) return Number(profile.id) || null
  return null
}

export function hasAiLoginContext() {
  return Boolean(getCurrentAiUserId() || getToken() || getAccessToken())
}

export function listAssistantAiModels() {
  return request({
    url: `${MODEL_BASE}/enabled`,
    method: 'get'
  })
}

export function getAssistantActiveAiModel() {
  return request({
    url: `${MODEL_BASE}/active`,
    method: 'get'
  })
}

export function parseBlogSummaryResult(text) {
  const raw = String(text || '').replace(/\r/g, '').trim()

  if (!raw) {
    return {
      summary: '',
      tags: []
    }
  }

  const lines = raw
    .split('\n')
    .map(item => item.trim())
    .filter(Boolean)

  const summaryLines = []
  const tagCandidates = []
  let inTagBlock = false

  for (const line of lines) {
    const cleaned = cleanLinePrefix(line)

    if (/^(摘要|内容摘要|文章摘要|总结)[:：\s]*/i.test(cleaned)) {
      const value = cleaned.replace(/^(摘要|内容摘要|文章摘要|总结)[:：\s]*/i, '').trim()
      if (value) {
        summaryLines.push(value)
      }
      inTagBlock = false
      continue
    }

    if (/^(标签建议|推荐标签|标签|关键词|关键字)[:：\s]*/i.test(cleaned)) {
      const value = cleaned.replace(/^(标签建议|推荐标签|标签|关键词|关键字)[:：\s]*/i, '').trim()
      if (value) {
        tagCandidates.push(...splitTagText(value))
      }
      inTagBlock = true
      continue
    }

    if (inTagBlock) {
      const tagLine = splitTagText(cleaned)
      if (tagLine.length > 0 && cleaned.length <= 30) {
        tagCandidates.push(...tagLine)
        continue
      }
      inTagBlock = false
    }

    if (!inTagBlock) {
      summaryLines.push(cleaned)
    }
  }

  let summary = summaryLines.join('\n').trim()

  if (!summary) {
    const firstNonTagLine = lines.find(line => {
      const cleaned = cleanLinePrefix(line)
      return !/^(标签建议|推荐标签|标签|关键词|关键字)[:：\s]*/i.test(cleaned)
    })
    summary = cleanLinePrefix(firstNonTagLine || '')
  }

  const tags = [...new Set(tagCandidates.map(item => item.trim()).filter(Boolean))].slice(0, 5)

  return {
    summary,
    tags
  }
}

function stripHtmlTags(value = '') {
  return String(value || '').replace(/<[^>]+>/g, ' ')
}

function normalizePlainText(value = '') {
  return stripHtmlTags(value)
    .replace(/\r/g, '')
    .replace(/\n{3,}/g, '\n\n')
    .replace(/[ \t]+/g, ' ')
    .trim()
}

function limitTextByParagraphs(value = '', maxChars = 6000) {
  const text = normalizePlainText(value)
  if (!text) return ''
  if (text.length <= maxChars) return text

  const paragraphs = text.split(/\n{2,}/).map(item => item.trim()).filter(Boolean)
  const result = []
  let size = 0

  for (const item of paragraphs) {
    const next = item.length + (result.length ? 2 : 0)
    if (size + next > maxChars) break
    result.push(item)
    size += next
  }

  if (result.length) return result.join('\n\n')

  return text.slice(0, maxChars)
}

function tryParseSceneJson(value) {
  if (!value) return null
  if (typeof value === 'object') return value

  let raw = String(value || '').trim()
  if (!raw) return null

  const fenceMatch = raw.match(/```(?:json)?\s*([\s\S]*?)```/i)
  if (fenceMatch && fenceMatch[1]) raw = fenceMatch[1].trim()

  try {
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}

function uniqList(list = []) {
  const seen = new Set()
  return (Array.isArray(list) ? list : [])
    .map(item => String(item || '').trim())
    .filter(Boolean)
    .filter(item => {
      const key = item.toLowerCase()
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
}

function limitString(value = '', max = 120) {
  const text = normalizePlainText(value)
  if (text.length <= max) return text
  return text.slice(0, max).trim()
}

function cleanupTagList(list = [], maxCount = 5) {
  const blocked = new Set(['技术', '开发', '项目', '博客', '摘要', '标签', '系统', '总结'])
  return uniqList(list)
    .map(item => item.replace(/^#/, '').trim())
    .filter(item => item.length >= 2 && item.length <= 12)
    .filter(item => !blocked.has(item))
    .slice(0, maxCount)
}

export function normalizeBlogPolishPayload(result = {}) {
  const source = result?.structured || tryParseSceneJson(result?.text || '')
  const rawText = String(result?.text || '').trim()

  const polishedContent = normalizePlainText(
    source?.polishedContent || source?.content || rawText
  )

  return {
    polishedContent,
    changeSummary: uniqList(source?.changeSummary || []).slice(0, 4),
    warnings: uniqList(source?.warnings || []).slice(0, 3),
    titleSuggestions: uniqList(source?.titleSuggestions || []).slice(0, 3),
    rawText,
    displayText: polishedContent || rawText
  }
}

export function normalizeBlogSummaryPayload(result = {}) {
  const source = result?.structured || tryParseSceneJson(result?.text || '')
  const rawText = String(result?.text || '').trim()

  const fallbackParsed = parseBlogSummaryResult(rawText)
  const summary = limitString(
    source?.summary || fallbackParsed?.summary || rawText,
    120
  )

  const tags = cleanupTagList(source?.tags || fallbackParsed?.tags || [], 5)

  return {
    summary,
    tags,
    rejectedTags: cleanupTagList(source?.rejectTags || [], 5),
    rawText,
    displayText: summary
  }
}

export function normalizeProjectSummaryPayload(result = {}) {
  const source = result?.structured || tryParseSceneJson(result?.text || '')
  const rawText = String(result?.text || '').trim()

  return {
    overview: limitString(source?.overview || '', 60),
    scenarios: uniqList(source?.scenarios || []).slice(0, 4),
    features: uniqList(source?.features || []).slice(0, 5),
    risks: uniqList(source?.risks || []).slice(0, 4),
    nextActions: uniqList(source?.nextActions || []).slice(0, 4),
    rawText,
    displayText: rawText
  }
}

export function normalizeProjectTaskPayload(result = {}) {
  const source = result?.structured || tryParseSceneJson(result?.text || '')
  const rawText = String(result?.text || '').trim()

  const phases = Array.isArray(source?.phases)
    ? source.phases.slice(0, 3).map(phase => ({
        name: limitString(phase?.name || 'Unnamed phase', 20),
        tasks: (Array.isArray(phase?.tasks) ? phase.tasks : []).slice(0, 5).map(task => ({
          title: limitString(task?.title || 'Unnamed task', 24),
          goal: limitString(task?.goal || '', 40),
          deliverable: limitString(task?.deliverable || '', 40),
          priority: limitString(task?.priority || 'P2', 8),
          estimate: limitString(task?.estimate || '', 16)
        }))
      }))
    : []

  return {
    phases,
    executionOrder: uniqList(source?.executionOrder || []).slice(0, 8),
    risks: uniqList(source?.risks || []).slice(0, 4),
    rawText,
    displayText: rawText
  }
}

export function matchSystemTagsByNames(tagNames = [], tagOptions = []) {
  const normalizedNames = cleanupTagList(tagNames, 5)
  const optionList = Array.isArray(tagOptions) ? tagOptions : []

  const used = new Set()
  return normalizedNames
    .map(name => {
      const lower = name.toLowerCase()
      return optionList.find(option => {
        const key = String(option?.name || '').trim().toLowerCase()
        if (!key || used.has(key)) return false
        return key === lower || key.includes(lower) || lower.includes(key)
      }) || null
    })
    .filter(Boolean)
    .filter(item => {
      const key = String(item.name || '').trim().toLowerCase()
      if (used.has(key)) return false
      used.add(key)
      return true
    })
    .slice(0, 5)
}
export function buildProjectAiPayload(project = {}) {
  const author = project.author || project.creator || project.owner || project.user || {}
  const technologies = toArray(
    pickFirst(
      project.technologies,
      project.techStack,
      project.tags,
      project.technologyList,
      project.skills
    )
  )
  const features = toArray(
    pickFirst(
      project.features,
      project.featureList,
      project.featureDesc,
      project.highlights
    )
  )
  const members = Array.isArray(project.members)
    ? project.members
    : Array.isArray(project.contributors)
      ? project.contributors
      : []
  const tasks = Array.isArray(project.tasks)
    ? project.tasks
    : Array.isArray(project.projectTasks)
      ? project.projectTasks
      : []
  const files = Array.isArray(project.files)
    ? project.files
    : Array.isArray(project.projectFiles)
      ? project.projectFiles
      : []

  const memberNames = members
    .map(item => pickFirst(item.nickname, item.username, item.name, item.memberName))
    .filter(Boolean)

  const taskTexts = tasks
    .map(item => {
      const title = pickFirst(item.title, item.taskName, item.name, 'Unnamed task')
      const status = pickFirst(item.statusName, item.status, item.state, 'Pending')
      const description = pickFirst(item.description, item.content, item.remark, '')
      return description ? `${title} (${status}): ${description}` : `${title} (${status})`
    })
    .filter(Boolean)

  const fileNames = files
    .map(item => pickFirst(item.name, item.fileName, item.path, item.filePath))
    .filter(Boolean)

  return [
    `Project title: ${pickFirst(project.title, project.projectName, project.name, 'Untitled')}`,
    `Project type: ${pickFirst(project.typeName, project.type, project.categoryName, project.category, 'Unspecified')}`,
    `Project status: ${pickFirst(project.statusName, project.status, project.state, 'Unknown')}`,
    `Project owner: ${pickFirst(author.nickname, author.username, author.name, project.creatorName, project.ownerName, 'Unknown')}`,
    `Project description: ${pickFirst(project.description, project.introduction, project.summary, project.content, 'N/A')}`,
    `Tech stack: ${technologies.length > 0 ? technologies.join(', ') : 'N/A'}`,
    `Main features: ${features.length > 0 ? features.join(', ') : 'N/A'}`,
    `Project members: ${memberNames.length > 0 ? memberNames.join(', ') : 'N/A'}`,
    `Tasks: ${taskTexts.length > 0 ? taskTexts.join('; ') : 'N/A'}`,
    `Files: ${fileNames.length > 0 ? fileNames.join(', ') : 'N/A'}`
  ].join('\n')
}
export function aiChatTurn(data, options = {}) {
  return request({
    url: `${CHAT_BASE}/turn`,
    method: 'post',
    data,
    ...(options || {})
  })
}

export function aiChatStream({
  body,
  onMessage,
  onError,
  onFinish,
  onCancel,
  headers = {},
  timeout = 300000,
  signal
}) {
  const controller = new AbortController()
  let canceled = false
  let timedOut = false
  let finishedByChunk = false
  let timeoutId = null

  if (signal && typeof signal.addEventListener === 'function') {
    signal.addEventListener('abort', () => {
      canceled = true
      controller.abort()
    })
  }

  const emitChunk = (raw) => {
    if (!raw) return
    if (raw === '[DONE]') {
      finishedByChunk = true
      return
    }
    const parsed = parseStreamPayload(raw)
    if (parsed === null || parsed === undefined) return
    if (isTerminalStreamChunk(parsed)) {
      finishedByChunk = true
    }
    onMessage && onMessage(parsed)
  }

  const consume = async () => {
    try {
      if (timeout && timeout > 0) {
        timeoutId = setTimeout(() => {
          timedOut = true
          controller.abort()
        }, timeout)
      }

      const response = await fetch(`${getApiBaseUrl()}${CHAT_BASE}/stream`, {
        method: 'POST',
        headers: buildAuthHeaders(headers),
        credentials: 'include',
        body: JSON.stringify(body || {}),
        signal: controller.signal
      })

      if (!response.ok) {
        let message = `娴佸紡璇锋眰澶辫触: ${response.status}`
        try {
          const errorPayload = await response.json()
          message = errorPayload?.message || errorPayload?.msg || errorPayload?.error || message
        } catch (e) {}
        const err = new Error(message)
        err.status = response.status
        err.response = { status: response.status, data: { message } }
        throw err
      }
      if (!response.body) {
        throw new Error('褰撳墠娴忚鍣ㄤ笉鏀寔娴佸紡鍝嶅簲')
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { value, done } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        buffer = consumeSseBuffer(buffer, event => emitChunk(event.data))
      }

      if (buffer) {
        consumeSseBuffer(`${buffer}\n\n`, event => emitChunk(event.data))
      }

      onFinish && onFinish({ finishedByChunk })
    } catch (err) {
      if (err && err.name === 'AbortError' && timedOut) {
        onError && onError(createAiError(ERROR_TYPE.TIMEOUT, 'AI 娴佸紡璇锋眰瓒呮椂'))
        return
      }
      if (err && err.name === 'AbortError') {
        onCancel && onCancel(classifyAiError(createAiError(ERROR_TYPE.CANCELED, 'request canceled')))
        return
      }
      onError && onError(err)
    } finally {
      if (timeoutId) {
        clearTimeout(timeoutId)
      }
    }
  }

  consume()

  return () => {
    canceled = true
    controller.abort()
  }
}

function buildPayload({
  userId,
  modelId = null,
  promptTemplateId = null,
  sessionId = null,
  content,
  requestType = 'CHAT',
  bizType = 'GENERAL',
  bizId = null,
  projectId = null,
  sceneCode = 'global.assistant',
  sessionTitle = 'AI 鍔╂墜浼氳瘽',
  knowledgeBaseIds = [],
  defaultKnowledgeBaseId = null,
  analysisMode = null,
  strictGrounding = null,
  entryFile = null,
  symbolHint = null,
  traceDepth = null,
  requestParams = {}
}) {
  const mergedParams = {
    ...(requestParams || {})
  }
  if (analysisMode) {
    mergedParams.analysisMode = analysisMode
    mergedParams.analysis_mode = analysisMode
  }
  if (strictGrounding !== null && strictGrounding !== undefined) {
    mergedParams.strictGrounding = strictGrounding
    mergedParams.strict_grounding = strictGrounding
  }
  if (entryFile) {
    mergedParams.entryFile = entryFile
    mergedParams.entry_file = entryFile
  }
  if (symbolHint) {
    mergedParams.symbolHint = symbolHint
    mergedParams.symbol_hint = symbolHint
  }
  if (traceDepth !== null && traceDepth !== undefined) {
    mergedParams.traceDepth = traceDepth
    mergedParams.trace_depth = traceDepth
  }
  return {
    sessionId,
    userId,
    content,
    ...(modelId ? { modelId } : {}),
    ...(promptTemplateId ? { promptTemplateId } : {}),
    requestType,
    bizType,
    bizId,
    projectId,
    sceneCode,
    sessionTitle,
    memoryMode: 'SHORT',
    knowledgeBaseIds,
    defaultKnowledgeBaseId,
    ...(analysisMode ? { analysisMode } : {}),
    ...(strictGrounding !== null && strictGrounding !== undefined ? { strictGrounding } : {}),
    ...(entryFile ? { entryFile } : {}),
    ...(symbolHint ? { symbolHint } : {}),
    ...(traceDepth !== null && traceDepth !== undefined ? { traceDepth } : {}),
    requestParams: mergedParams
  }
}

function choosePromptTemplate(list = []) {
  if (!Array.isArray(list) || list.length === 0) return null
  return (
    list.find(item => item && item.publishStatus === 'PUBLISHED' && item.isEnabled !== false) ||
    list.find(item => item && item.isEnabled !== false) ||
    list[0] ||
    null
  )
}

async function resolveSceneRuntime(sceneCode, preferredModelId = null, options = {}) {
  const { preferTemplateModel = false } = options || {}
  let template = null

  if (sceneCode) {
    try {
      const templateRes = await listPromptTemplatesByScene(sceneCode)
      const templateList = unwrapApiPayload(templateRes)
      template = choosePromptTemplate(Array.isArray(templateList) ? templateList : [])
    } catch (e) {
      console.error(`鍔犺浇鍦烘櫙妯℃澘澶辫触: ${sceneCode}`, e)
    }
  }

  let modelId = preferredModelId || null
  let modelName = ''

  if (!modelId && preferTemplateModel && template?.defaultModel?.id) {
    modelId = template.defaultModel.id
    modelName = template.defaultModel.modelName || ''
  }

  if (!modelId) {
    try {
      const activeRes = await getAssistantActiveAiModel()
      const activeModel = unwrapApiPayload(activeRes)
      if (activeModel && activeModel.id) {
        modelId = activeModel.id
        modelName =
          activeModel.modelName ||
          activeModel.name ||
          activeModel.displayName ||
          activeModel.providerModel ||
          ''
      }
    } catch (e) {
      console.error('Failed to load active AI model', e)
    }
  }

  if (!modelId && template?.defaultModel?.id) {
    modelId = template.defaultModel.id
    modelName = template.defaultModel.modelName || modelName
  }

  return {
    promptTemplateId: template?.id || null,
    promptTemplateName: template?.templateName || '',
    modelId,
    modelName,
    template
  }
}

function normalizeTurnResult(payload, runtime = {}, fallback = {}) {
  const turn = unwrapApiPayload(payload) || {}

  return {
    text: extractAnswer(turn),
    displayText: turn.displayText || extractAnswer(turn),
    structured: turn.structured || null,
    sessionId: turn.sessionId || fallback.sessionId || null,
    userMessageId: turn.userMessageId || null,
    assistantMessageId: turn.assistantMessageId || null,
    callLogId: turn.callLogId || null,
    modelId: turn.modelId || runtime.modelId || null,
    modelName: turn.modelName || runtime.modelName || '',
    promptTemplateId: turn.promptTemplateId || runtime.promptTemplateId || null,
    promptTemplateName: turn.promptTemplateName || runtime.promptTemplateName || '',
    sceneCode: turn.sceneCode || fallback.sceneCode || '',
    citations: Array.isArray(turn.citations) ? turn.citations : [],
    retrievalSummary: turn.retrievalSummary || null,
    groundingStatus: turn.groundingStatus || '',
    strictGrounding: turn.strictGrounding === true
  }
}

export async function callSceneBizAi({
  userId,
  sceneCode,
  requestType,
  content,
  sessionId = null,
  sessionTitle = 'AI 鍔╂墜浼氳瘽',
  modelId = null,
  promptTemplateId = null,
  bizType = 'GENERAL',
  bizId = null,
  projectId = null,
  knowledgeBaseIds = [],
  defaultKnowledgeBaseId = null,
  preferTemplateModel = false,
  requestParams = {},
  axiosOptions = {}
}) {
  const runtime = promptTemplateId
    ? { promptTemplateId, promptTemplateName: '', modelId, modelName: '' }
    : await resolveSceneRuntime(sceneCode, modelId, { preferTemplateModel })

  const response = await aiChatTurn(
    buildPayload({
      userId,
      modelId: modelId || runtime.modelId,
      promptTemplateId: promptTemplateId || runtime.promptTemplateId,
      sessionId,
      content,
      requestType,
      bizType,
      bizId,
      projectId,
      sceneCode,
      sessionTitle,
      knowledgeBaseIds,
      defaultKnowledgeBaseId,
      requestParams: {
        sceneCode,
        ...requestParams
      }
    }),
    axiosOptions
  )

  return normalizeTurnResult(response, runtime, { sessionId, sceneCode })
}

export async function submitAiFeedback({ userId, messageId, callLogId = null, feedbackType, commentText = '' }) {
  return createAiFeedback({ userId, messageId, callLogId, feedbackType, commentText })
}


function extractStreamDeltaText(chunk) {
  if (chunk === null || chunk === undefined) return ''
  if (typeof chunk === 'string') return chunk
  return (
    chunk.delta ||
    chunk.content ||
    chunk.answer ||
    chunk.message ||
    chunk.responseText ||
    ''
  )
}

export function streamSceneBizAi({
  userId,
  sceneCode,
  requestType,
  content,
  sessionId = null,
  sessionTitle = 'AI 鍔╂墜浼氳瘽',
  modelId = null,
  promptTemplateId = null,
  bizType = 'GENERAL',
  bizId = null,
  projectId = null,
  knowledgeBaseIds = [],
  defaultKnowledgeBaseId = null,
  preferTemplateModel = false,
  requestParams = {},
  headers = {},
  onDelta,
  onText,
  onChunk,
  onFinish,
  onError
}) {
  let stopped = false
  let stopReader = null
  let text = ''
  let currentSessionId = sessionId || null
  let currentRuntime = {
    promptTemplateId,
    promptTemplateName: '',
    modelId,
    modelName: ''
  }

  const run = async () => {
    try {
      const runtime = promptTemplateId
        ? currentRuntime
        : await resolveSceneRuntime(sceneCode, modelId, { preferTemplateModel })

      currentRuntime = {
        promptTemplateId: promptTemplateId || runtime.promptTemplateId || null,
        promptTemplateName: runtime.promptTemplateName || '',
        modelId: modelId || runtime.modelId || null,
        modelName: runtime.modelName || ''
      }

      if (stopped) return

      stopReader = aiChatStream({
        headers,
        body: buildPayload({
          userId,
          modelId: currentRuntime.modelId,
          promptTemplateId: currentRuntime.promptTemplateId,
          sessionId: currentSessionId,
          content,
          requestType,
          bizType,
          bizId,
          projectId,
          sceneCode,
          sessionTitle,
          knowledgeBaseIds,
          defaultKnowledgeBaseId,
          requestParams: {
            sceneCode,
            ...requestParams
          }
        }),
        onMessage: (chunk) => {
          if (stopped) return

          if (chunk && typeof chunk === 'object' && chunk.sessionId !== undefined && chunk.sessionId !== null) {
            currentSessionId = chunk.sessionId
          }

          const delta = extractStreamDeltaText(chunk)
          if (delta) {
            text += delta
            onDelta && onDelta(delta, text, chunk)
            onText && onText(text, chunk)
          }

          onChunk && onChunk(chunk, text)
        },
        onError: (err) => {
          if (stopped) return
          onError && onError(err)
        },
        onFinish: () => {
          if (stopped) return
          onFinish && onFinish({
            text,
            sessionId: currentSessionId,
            modelId: currentRuntime.modelId || null,
            modelName: currentRuntime.modelName || '',
            promptTemplateId: currentRuntime.promptTemplateId || null,
            promptTemplateName: currentRuntime.promptTemplateName || '',
            sceneCode
          })
        }
      })
    } catch (err) {
      if (stopped) return
      onError && onError(err)
    }
  }

  run()

  return () => {
    stopped = true
    if (typeof stopReader === 'function') {
      stopReader()
    }
  }
}

function buildProjectSummaryPrompt({ title = '', content = '', project = null }) {
  const finalTitle = title || pickFirst(project && (project.title || project.projectName || project.name), 'Untitled project')
  const finalContent = limitTextByParagraphs(
    typeof content === 'string' && content.trim() ? content : buildProjectAiPayload(project || {}),
    7000
  )

  return [
    'You are a project analysis assistant.',
    'Output JSON only. Do not output markdown or code fences.',
    'Format:',
    '{',
    '  "overview": "short summary",',
    '  "scenarios": ["use case 1", "use case 2"],',
    '  "features": ["feature 1", "feature 2"],',
    '  "risks": ["risk 1", "risk 2"],',
    '  "nextActions": ["action 1", "action 2"]',
    '}',
    'Rules:',
    '1. Keep only useful information.',
    '2. Keep each item concise.',
    '3. overview should be brief and direct.',
    '',
    `Project title: ${finalTitle}`,
    `Project content:\n${finalContent}`
  ].join('\n')
}

function buildProjectTaskPrompt({ title = '', content = '', project = null }) {
  const finalTitle = title || pickFirst(project && (project.title || project.projectName || project.name), 'Untitled project')
  const finalContent = limitTextByParagraphs(
    typeof content === 'string' && content.trim() ? content : buildProjectAiPayload(project || {}),
    7000
  )

  return [
    'You are a project decomposition assistant.',
    'Output JSON only. Do not output markdown or code fences.',
    'Format:',
    '{',
    '  "phases": [',
    '    {',
    '      "name": "phase name",',
    '      "tasks": [',
    '        {',
    '          "title": "task title",',
    '          "goal": "task goal",',
    '          "deliverable": "deliverable",',
    '          "priority": "P1/P2/P3",',
    '          "estimate": "estimated effort"',
    '        }',
    '      ]',
    '    }',
    '  ],',
    '  "executionOrder": ["recommended order"],',
    '  "risks": ["risk 1", "risk 2"]',
    '}',
    'Rules:',
    '1. Max 3 phases.',
    '2. Max 5 tasks per phase.',
    '3. Keep task names short and concrete.',
    '',
    `Project title: ${finalTitle}`,
    `Project content:\n${finalContent}`
  ].join('\n')
}

function buildBlogPolishPrompt({ title = '', content = '' }) {
  const finalContent = limitTextByParagraphs(content, 8000)

  return [
    'You are a technical blog polish assistant.',
    'Output JSON only. Do not output markdown or code fences.',
    'Format:',
    '{',
    '  "polishedContent": "publish-ready article",',
    '  "changeSummary": ["optimization 1", "optimization 2"],',
    '  "warnings": ["warning 1"],',
    '  "titleSuggestions": ["title 1"]',
    '}',
    'Rules:',
    '1. Preserve the original intent.',
    '2. Improve expression, paragraphing, and repetition.',
    '3. polishedContent should be directly usable.',
    '',
    `Blog title: ${title || 'Untitled blog'}`,
    `Blog content:\n${finalContent}`
  ].join('\n')
}

function buildBlogSummaryPrompt({ title = '', content = '' }) {
  const finalContent = limitTextByParagraphs(content, 6000)

  return [
    'You are a technical blog summary assistant.',
    'Output JSON only. Do not output markdown or code fences.',
    'Format:',
    '{',
    '  "summary": "short summary",',
    '  "tags": ["tag1", "tag2"],',
    '  "rejectTags": ["tag you should avoid"]',
    '}',
    'Rules:',
    '1. summary must be <= 120 characters.',
    '2. tags should be specific and searchable.',
    '3. Avoid overly broad tags such as technology or development.',
    '',
    `Blog title: ${title || 'Untitled blog'}`,
    `Blog content:\n${finalContent}`
  ].join('\n')
}

export async function aiSummarizeProject({
  userId,
  modelId = null,
  promptTemplateId = null,
  sessionId = null,
  projectId = null,
  title = '',
  content = '',
  project = null
}) {
  const result = await callSceneBizAi({
    userId,
    sceneCode: 'project.detail.summary',
    requestType: 'PROJECT_ASSISTANT',
    bizType: 'PROJECT',
    bizId: projectId,
    projectId,
    sessionId,
    sessionTitle: `Project Summary - ${title || project?.name || 'Untitled project'}`,
    modelId,
    promptTemplateId,
    content: buildProjectSummaryPrompt({ title, content, project }),
    requestParams: { title, projectId, bizType: 'PROJECT' },
    axiosOptions: { timeout: 300000 }
  })

  return {
    ...result,
    normalized: normalizeProjectSummaryPayload(result)
  }
}

export async function aiSplitProjectTasks({
  userId,
  modelId = null,
  promptTemplateId = null,
  sessionId = null,
  projectId = null,
  title = '',
  content = '',
  project = null
}) {
  const result = await callSceneBizAi({
    userId,
    sceneCode: 'project.detail.tasks',
    requestType: 'PROJECT_ASSISTANT',
    bizType: 'PROJECT',
    bizId: projectId,
    projectId,
    sessionId,
    sessionTitle: `Project Tasks - ${title || project?.name || 'Untitled project'}`,
    modelId,
    promptTemplateId,
    content: buildProjectTaskPrompt({ title, content, project }),
    requestParams: { title, projectId, bizType: 'PROJECT' },
    axiosOptions: { timeout: 300000 }
  })

  return {
    ...result,
    normalized: normalizeProjectTaskPayload(result)
  }
}

export async function aiPolishBlog({
  userId,
  modelId = null,
  promptTemplateId = null,
  sessionId = null,
  title = '',
  content = ''
}) {
  const result = await callSceneBizAi({
    userId,
    sceneCode: 'blog.polish',
    requestType: 'BLOG_ASSISTANT',
    bizType: 'BLOG',
    sessionId,
    sessionTitle: `Blog Polish - ${title || 'Untitled blog'}`,
    modelId,
    promptTemplateId,
    content: buildBlogPolishPrompt({ title, content }),
    requestParams: { title },
    axiosOptions: { timeout: 300000 }
  })

  return {
    ...result,
    normalized: normalizeBlogPolishPayload(result)
  }
}

export function aiPolishBlogStream({
  userId,
  modelId = null,
  promptTemplateId = null,
  sessionId = null,
  title = '',
  content = '',
  onDelta,
  onText,
  onChunk,
  onFinish,
  onError
}) {
  return streamSceneBizAi({
    userId,
    sceneCode: 'blog.polish',
    requestType: 'BLOG_ASSISTANT',
    bizType: 'BLOG',
    sessionId,
    sessionTitle: `Blog Polish - ${title || 'Untitled blog'}`,
    modelId,
    promptTemplateId,
    content: buildBlogPolishPrompt({ title, content }),
    requestParams: { title },
    onDelta,
    onText,
    onChunk,
    onFinish: result => {
      const normalized = normalizeBlogPolishPayload(result)
      onFinish && onFinish({ ...result, normalized })
    },
    onError
  })
}

export async function aiGenerateBlogSummary({
  userId,
  modelId = null,
  promptTemplateId = null,
  sessionId = null,
  title = '',
  content = ''
}) {
  const result = await callSceneBizAi({
    userId,
    sceneCode: 'blog.summary',
    requestType: 'BLOG_ASSISTANT',
    bizType: 'BLOG',
    sessionId,
    sessionTitle: `Blog Summary - ${title || 'Untitled blog'}`,
    modelId,
    promptTemplateId,
    preferTemplateModel: !modelId,
    content: buildBlogSummaryPrompt({ title, content }),
    requestParams: { title },
    axiosOptions: { timeout: 180000 }
  })

  return {
    ...result,
    normalized: normalizeBlogSummaryPayload(result)
  }
}

export function aiGenerateBlogSummaryStream({
  userId,
  modelId = null,
  promptTemplateId = null,
  sessionId = null,
  title = '',
  content = '',
  onDelta,
  onText,
  onChunk,
  onFinish,
  onError
}) {
  return streamSceneBizAi({
    userId,
    sceneCode: 'blog.summary',
    requestType: 'BLOG_ASSISTANT',
    bizType: 'BLOG',
    sessionId,
    sessionTitle: `Blog Summary - ${title || 'Untitled blog'}`,
    modelId,
    promptTemplateId,
    preferTemplateModel: !modelId,
    content: buildBlogSummaryPrompt({ title, content }),
    requestParams: { title },
    onDelta,
    onText,
    onChunk,
    onFinish: result => {
      const normalized = normalizeBlogSummaryPayload(result)
      onFinish && onFinish({ ...result, normalized })
    },
    onError
  })
}
