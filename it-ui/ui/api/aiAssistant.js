import request from '@/utils/request'
import { getCurrentUser } from '@/utils/auth'
import { listPromptTemplatesByScene, createAiFeedback } from '@/api/aiAdmin'

const CHAT_BASE = '/ai/chat'
const MODEL_BASE = '/ai/models'

function getApiBaseUrl() {
  const baseURL = request?.defaults?.baseURL || 'http://localhost:18080/api'
  return String(baseURL).replace(/\/$/, '')
}

function buildAuthHeaders(extraHeaders = {}) {
  return {
    'Content-Type': 'application/json',
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
      .split(/[\n,，、]/)
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
    .replace(/^[-*•\d]+[.\s、）)]*/, '')
    .trim()
}

function splitTagText(text = '') {
  return String(text)
    .replace(/[；;|]/g, ',')
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

export function extractErrorMessage(err, fallback = '请求失败，请稍后重试') {
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
  return ''
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
  return Boolean(getCurrentAiUserId())
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

    if (/^(摘要|内容摘要|文章摘要|总结)[:：]?\s*/i.test(cleaned)) {
      const value = cleaned.replace(/^(摘要|内容摘要|文章摘要|总结)[:：]?\s*/i, '').trim()
      if (value) {
        summaryLines.push(value)
      }
      inTagBlock = false
      continue
    }

    if (/^(标签建议|推荐标签|标签|关键词|关键字)[:：]?\s*/i.test(cleaned)) {
      const value = cleaned.replace(/^(标签建议|推荐标签|标签|关键词|关键字)[:：]?\s*/i, '').trim()
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
      return !/^(标签建议|推荐标签|标签|关键词|关键字)[:：]?\s*/i.test(cleaned)
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
        name: limitString(phase?.name || '未命名阶段', 20),
        tasks: (Array.isArray(phase?.tasks) ? phase.tasks : []).slice(0, 5).map(task => ({
          title: limitString(task?.title || '未命名任务', 24),
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
      const title = pickFirst(item.title, item.taskName, item.name, '未命名任务')
      const status = pickFirst(item.statusName, item.status, item.state, '待处理')
      const description = pickFirst(item.description, item.content, item.remark, '')
      return description ? `${title}（${status}）：${description}` : `${title}（${status}）`
    })
    .filter(Boolean)

  const fileNames = files
    .map(item => pickFirst(item.name, item.fileName, item.path, item.filePath))
    .filter(Boolean)

  return [
    `项目标题：${pickFirst(project.title, project.projectName, project.name, '未提供')}`,
    `项目类型：${pickFirst(project.typeName, project.type, project.categoryName, project.category, '未提供')}`,
    `项目状态：${pickFirst(project.statusName, project.status, project.state, '未提供')}`,
    `项目作者：${pickFirst(author.nickname, author.username, author.name, project.creatorName, project.ownerName, '未提供')}`,
    `项目描述：${pickFirst(project.description, project.introduction, project.summary, project.content, '未提供')}`,
    `技术栈：${technologies.length > 0 ? technologies.join('、') : '未提供'}`,
    `主要特性：${features.length > 0 ? features.join('、') : '未提供'}`,
    `项目成员：${memberNames.length > 0 ? memberNames.join('、') : '未提供'}`,
    `已有任务：${taskTexts.length > 0 ? taskTexts.join('；') : '未提供'}`,
    `项目文件：${fileNames.length > 0 ? fileNames.join('、') : '未提供'}`
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

export function aiChatStream({ body, onMessage, onError, onFinish, headers = {} }) {
  const controller = new AbortController()

  const emitChunk = (raw) => {
    if (!raw || raw === '[DONE]') return
    try {
      const parsed = JSON.parse(raw)
      onMessage && onMessage(parsed)
    } catch (e) {
      onMessage && onMessage(raw)
    }
  }

  const consume = async () => {
    try {
      const response = await fetch(`${getApiBaseUrl()}${CHAT_BASE}/stream`, {
        method: 'POST',
        headers: buildAuthHeaders(headers),
        credentials: 'include',
        body: JSON.stringify(body || {}),
        signal: controller.signal
      })

      if (!response.ok) {
        throw new Error(`流式请求失败: ${response.status}`)
      }
      if (!response.body) {
        throw new Error('当前浏览器不支持流式响应')
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { value, done } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const parts = buffer.split('\n\n')
        buffer = parts.pop() || ''

        for (const part of parts) {
          const lines = String(part || '')
            .split('\n')
            .map(line => line.trim())
            .filter(Boolean)

          for (const line of lines) {
            if (!line.startsWith('data:')) continue
            emitChunk(line.slice(5).trim())
          }
        }
      }

      if (buffer) {
        const lines = String(buffer || '')
          .split('\n')
          .map(line => line.trim())
          .filter(Boolean)

        for (const line of lines) {
          if (!line.startsWith('data:')) continue
          emitChunk(line.slice(5).trim())
        }
      }

      onFinish && onFinish()
    } catch (err) {
      if (err && err.name === 'AbortError') return
      onError && onError(err)
    }
  }

  consume()

  return () => controller.abort()
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
  sessionTitle = 'AI 助手会话',
  knowledgeBaseIds = [],
  defaultKnowledgeBaseId = null,
  requestParams = {}
}) {
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
    requestParams
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
      console.error(`加载场景模板失败: ${sceneCode}`, e)
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
      console.error('加载激活模型失败', e)
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
    citations: Array.isArray(turn.citations) ? turn.citations : []
  }
}

export async function callSceneBizAi({
  userId,
  sceneCode,
  requestType,
  content,
  sessionId = null,
  sessionTitle = 'AI 助手会话',
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
  sessionTitle = 'AI 助手会话',
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
  const finalTitle = title || pickFirst(project && (project.title || project.projectName || project.name), '未命名项目')
  const finalContent = limitTextByParagraphs(
    typeof content === 'string' && content.trim() ? content : buildProjectAiPayload(project || {}),
    7000
  )

  return [
    '你是项目分析助手。',
    '请只输出 JSON，不要输出 markdown，不要输出 ```。',
    '输出格式：',
    '{',
    '  "overview": "50字内一句话概述",',
    '  "scenarios": ["2-4条目标用户/使用场景"],',
    '  "features": ["3-5条核心功能"],',
    '  "risks": ["2-4条风险/待补项"],',
    '  "nextActions": ["2-4条下一步建议"]',
    '}',
    '规则：',
    '1. 只保留最有价值的信息，不要重复。',
    '2. 每条尽量短句。',
    '3. overview 超过 50 字时主动压缩。',
    '',
    `项目标题：${finalTitle}`,
    `项目内容：\n${finalContent}`
  ].join('\n')
}

function buildProjectTaskPrompt({ title = '', content = '', project = null }) {
  const finalTitle = title || pickFirst(project && (project.title || project.projectName || project.name), '未命名项目')
  const finalContent = limitTextByParagraphs(
    typeof content === 'string' && content.trim() ? content : buildProjectAiPayload(project || {}),
    7000
  )

  return [
    '你是项目拆解助手。',
    '请只输出 JSON，不要输出 markdown，不要输出 ```。',
    '输出格式：',
    '{',
    '  "phases": [',
    '    {',
    '      "name": "阶段名",',
    '      "tasks": [',
    '        {',
    '          "title": "任务名",',
    '          "goal": "任务目标",',
    '          "deliverable": "产出物",',
    '          "priority": "P1/P2/P3",',
    '          "estimate": "预计耗时"',
    '        }',
    '      ]',
    '    }',
    '  ],',
    '  "executionOrder": ["建议执行顺序"],',
    '  "risks": ["依赖或阻塞点"]',
    '}',
    '规则：',
    '1. 最多 3 个阶段。',
    '2. 每阶段最多 5 个任务。',
    '3. 任务名尽量短，不要空话。',
    '',
    `项目标题：${finalTitle}`,
    `项目内容：\n${finalContent}`
  ].join('\n')
}

function buildBlogPolishPrompt({ title = '', content = '' }) {
  const finalContent = limitTextByParagraphs(content, 8000)

  return [
    '你是技术博客润色助手。',
    '请只输出 JSON，不要输出 markdown，不要输出 ```。',
    '输出格式：',
    '{',
    '  "polishedContent": "可直接发布的正文",',
    '  "changeSummary": ["2-4条本次主要优化点"],',
    '  "warnings": ["0-3条仍建议作者手动确认的问题"],',
    '  "titleSuggestions": ["0-3个更适合的标题"]',
    '}',
    '规则：',
    '1. 保留原意，不编造事实。',
    '2. 重点优化表达、段落、重复句。',
    '3. polishedContent 直接可用于博客正文。',
    '',
    `博客标题：${title || '未命名博客'}`,
    `博客内容：\n${finalContent}`
  ].join('\n')
}

function buildBlogSummaryPrompt({ title = '', content = '' }) {
  const finalContent = limitTextByParagraphs(content, 6000)

  return [
    '你是技术博客摘要助手。',
    '请只输出 JSON，不要输出 markdown，不要输出 ```。',
    '输出格式：',
    '{',
    '  "summary": "120字内摘要",',
    '  "tags": ["3-5个标签"],',
    '  "rejectTags": ["不建议使用的过泛标签"]',
    '}',
    '规则：',
    '1. summary 必须 <= 120 字。',
    '2. tags 只保留具体、可检索的标签。',
    '3. 避免“技术、开发、项目、博客”这类过泛词。',
    '',
    `博客标题：${title || '未命名博客'}`,
    `博客内容：\n${finalContent}`
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
    sessionTitle: `项目总结-${title || project?.name || '未命名项目'}`,
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
    sessionTitle: `任务拆解-${title || project?.name || '未命名项目'}`,
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
    sessionTitle: `博客润色-${title || '未命名博客'}`,
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
    sessionTitle: `博客润色-${title || '未命名博客'}`,
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
    sessionTitle: `博客摘要-${title || '未命名博客'}`,
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
    sessionTitle: `博客摘要-${title || '未命名博客'}`,
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
