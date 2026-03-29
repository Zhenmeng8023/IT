import request from '@/utils/request'
import { getToken } from '@/utils/auth'
import { listPromptTemplatesByScene, createAiFeedback } from '@/api/aiAdmin'

const CHAT_BASE = '/ai/chat'
const MODEL_BASE = '/ai/models'

function getApiBaseUrl() {
  const baseURL = request?.defaults?.baseURL || 'http://localhost:18080/api'
  return String(baseURL).replace(/\/$/, '')
}

function buildAuthHeaders(extraHeaders = {}) {
  const token = getToken ? getToken() : ''
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}`, 'X-Token': token } : {}),
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

function decodeJwtPayload(token) {
  if (!token || typeof token !== 'string' || token.split('.').length < 2) {
    return null
  }
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const normalized = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')
    if (typeof window !== 'undefined' && typeof window.atob === 'function') {
      const text = decodeURIComponent(
        Array.from(window.atob(normalized))
          .map(ch => `%${ch.charCodeAt(0).toString(16).padStart(2, '0')}`)
          .join('')
      )
      return safeJsonParse(text)
    }
  } catch (e) {
    return null
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
  try {
    return (getToken && getToken()) || localStorage.getItem('userToken') || ''
  } catch (e) {
    return ''
  }
}

export function getCurrentAiUserProfile() {
  if (typeof window === 'undefined') return null

  const storeCandidates = [window.localStorage, window.sessionStorage]
  const keys = [
    'userInfo',
    'user',
    'loginUser',
    'currentUser',
    'login_user',
    'Admin-User',
    'adminUser',
    'user_profile',
    'profile',
    'account',
    'member',
    'memberInfo'
  ]

  for (const store of storeCandidates) {
    if (!store) continue
    for (const key of keys) {
      const parsed = unwrapUserLike(safeJsonParse(store.getItem(key)))
      if (parsed) return parsed
    }
  }

  const tokenPayload = decodeJwtPayload(getCurrentAiToken())
  return unwrapUserLike(tokenPayload)
}

export function getCurrentAiUserId() {
  const profile = getCurrentAiUserProfile()
  if (profile && profile.id) return Number(profile.id) || null

  const payload = decodeJwtPayload(getCurrentAiToken()) || {}
  const id = Number(
    payload.id ||
    payload.userId ||
    payload.uid ||
    payload.user_id ||
    payload.memberId ||
    payload.accountId ||
    payload.sub ||
    0
  ) || null

  return id
}

export function hasAiLoginContext() {
  return Boolean(getCurrentAiToken() || getCurrentAiUserId())
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

export function aiChatTurn(data) {
  return request({
    url: `${CHAT_BASE}/turn`,
    method: 'post',
    data
  })
}

export async function aiChatStream({ body, onMessage, onError, onFinish, headers = {} }) {
  const controller = new AbortController()

  try {
    const response = await fetch(`${getApiBaseUrl()}${CHAT_BASE}/stream`, {
      method: 'POST',
      headers: buildAuthHeaders(headers),
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
        const lines = part
          .split('\n')
          .map(line => line.trim())
          .filter(Boolean)
        for (const line of lines) {
          if (!line.startsWith('data:')) continue
          const raw = line.slice(5).trim()
          if (!raw || raw === '[DONE]') continue
          try {
            const parsed = JSON.parse(raw)
            onMessage && onMessage(parsed)
          } catch (e) {
            onMessage && onMessage(raw)
          }
        }
      }
    }

    if (buffer) {
      const lines = buffer
        .split('\n')
        .map(line => line.trim())
        .filter(Boolean)
      for (const line of lines) {
        if (!line.startsWith('data:')) continue
        const raw = line.slice(5).trim()
        if (!raw || raw === '[DONE]') continue
        try {
          const parsed = JSON.parse(raw)
          onMessage && onMessage(parsed)
        } catch (e) {
          onMessage && onMessage(raw)
        }
      }
    }

    onFinish && onFinish()
  } catch (err) {
    if (err.name !== 'AbortError') {
      onError && onError(err)
    }
  }

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

async function resolveSceneRuntime(sceneCode, preferredModelId = null) {
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

  if (!modelId) {
    try {
      const activeRes = await getAssistantActiveAiModel()
      const activeModel = unwrapApiPayload(activeRes)
      if (activeModel && activeModel.id) {
        modelId = activeModel.id
        modelName = activeModel.modelName || activeModel.name || activeModel.displayName || activeModel.providerModel || ''
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
  requestParams = {}
}) {
  const runtime = promptTemplateId
    ? { promptTemplateId, promptTemplateName: '', modelId, modelName: '' }
    : await resolveSceneRuntime(sceneCode, modelId)

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
    })
  )

  return normalizeTurnResult(response, runtime, { sessionId, sceneCode })
}

export async function submitAiFeedback({ userId, messageId, callLogId = null, feedbackType, commentText = '' }) {
  return createAiFeedback({ userId, messageId, callLogId, feedbackType, commentText })
}

export async function aiSummarizeProject({ userId, projectId, project }) {
  const content = [
    '请根据以下项目信息生成一份简洁、结构清晰的项目总结。',
    '要求：',
    '1. 概括项目目标',
    '2. 提炼核心功能',
    '3. 总结技术亮点',
    '4. 给出当前完成情况与下一步建议',
    '',
    buildProjectAiPayload(project || {})
  ].join('\n')

  return callSceneBizAi({
    userId,
    sceneCode: 'project.summary',
    requestType: 'CHAT',
    content,
    bizType: 'PROJECT',
    bizId: projectId,
    projectId,
    sessionTitle: '项目总结'
  })
}

export async function aiSplitProjectTasks({ userId, projectId, project }) {
  const content = [
    '请根据以下项目信息拆分项目任务。',
    '要求：',
    '1. 输出 5 到 10 个可执行任务',
    '2. 每个任务包含：任务名称、任务说明、优先级、建议负责人类型',
    '3. 尽量按合理开发顺序拆分',
    '',
    buildProjectAiPayload(project || {})
  ].join('\n')

  return callSceneBizAi({
    userId,
    sceneCode: 'project.task.split',
    requestType: 'CHAT',
    content,
    bizType: 'PROJECT',
    bizId: projectId,
    projectId,
    sessionTitle: '任务拆分'
  })
}

export async function aiPolishBlog({ userId, title = '', content = '' }) {
  const text = [
    '请帮我润色下面这篇博客内容。',
    '要求：',
    '1. 保持原意不变',
    '2. 提升表达流畅度与专业性',
    '3. 保持适合中文技术博客发布',
    '',
    `标题：${title || '未命名博客'}`,
    `内容：\n${content || ''}`
  ].join('\n')

  return callSceneBizAi({
    userId,
    sceneCode: 'blog.polish',
    requestType: 'CHAT',
    content: text,
    bizType: 'BLOG',
    sessionTitle: '博客润色'
  })
}

export async function aiGenerateBlogSummary({ userId, title = '', content = '' }) {
  const text = [
    '请根据下面博客内容生成摘要和标签。',
    '输出要求：',
    '1. 先输出摘要',
    '2. 再输出 3 到 5 个标签',
    '3. 标签尽量简洁',
    '',
    `标题：${title || '未命名博客'}`,
    `内容：\n${content || ''}`
  ].join('\n')

  const result = await callSceneBizAi({
    userId,
    sceneCode: 'blog.summary',
    requestType: 'CHAT',
    content: text,
    bizType: 'BLOG',
    sessionTitle: '博客摘要生成'
  })

  return {
    ...result,
    parsed: parseBlogSummaryResult(result?.text || '')
  }
}
