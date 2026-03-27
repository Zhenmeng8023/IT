import request from '@/utils/request'
import { getToken } from '@/utils/auth'

const CHAT_BASE = '/ai/chat'

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
      .split(/[,，、\n]/)
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
          if (!raw) continue

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
        if (!raw) continue

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
  modelId = 1,
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
    modelId,
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

export async function aiSummarizeProject({
  userId,
  modelId = 1,
  sessionId = null,
  projectId = null,
  title = '',
  content = '',
  project = null
}) {
  const finalTitle = title || pickFirst(project && (project.title || project.projectName || project.name), '未命名项目')
  const finalContent =
    typeof content === 'string' && content.trim()
      ? content
      : buildProjectAiPayload(project || {})

  const prompt = [
    '请你作为项目助手，对下面的项目信息做结构化总结。',
    '输出要求：',
    '1. 项目一句话概述',
    '2. 目标用户/使用场景',
    '3. 当前核心功能',
    '4. 风险与待补项',
    '',
    `项目标题：${finalTitle || '未提供'}`,
    `项目内容：${finalContent || '未提供'}`
  ].join('\n')

  const res = await aiChatTurn(
    buildPayload({
      userId,
      modelId,
      sessionId,
      content: prompt,
      requestType: 'PROJECT_ASSISTANT',
      bizType: 'PROJECT',
      bizId: projectId,
      projectId,
      sceneCode: 'project.detail.summary',
      sessionTitle: `项目总结-${finalTitle || '未命名项目'}`
    })
  )

  return extractAnswer(res?.data)
}

export async function aiSplitProjectTasks({
  userId,
  modelId = 1,
  sessionId = null,
  projectId = null,
  title = '',
  content = '',
  project = null
}) {
  const finalTitle = title || pickFirst(project && (project.title || project.projectName || project.name), '未命名项目')
  const finalContent =
    typeof content === 'string' && content.trim()
      ? content
      : buildProjectAiPayload(project || {})

  const prompt = [
    '请你作为项目助手，把下面的项目信息拆解成可执行任务。',
    '输出要求：',
    '1. 按阶段拆分',
    '2. 每个任务写清目标、产出物、优先级',
    '3. 最后给出建议执行顺序',
    '',
    `项目标题：${finalTitle || '未提供'}`,
    `项目内容：${finalContent || '未提供'}`
  ].join('\n')

  const res = await aiChatTurn(
    buildPayload({
      userId,
      modelId,
      sessionId,
      content: prompt,
      requestType: 'PROJECT_ASSISTANT',
      bizType: 'PROJECT',
      bizId: projectId,
      projectId,
      sceneCode: 'project.detail.tasks',
      sessionTitle: `任务拆解-${finalTitle || '未命名项目'}`
    })
  )

  return extractAnswer(res?.data)
}

export async function aiPolishBlog({
  userId,
  modelId = 1,
  sessionId = null,
  title = '',
  content = ''
}) {
  const prompt = [
    '请你作为博客写作助手，对下面的博客草稿进行润色。',
    '输出要求：',
    '1. 保留原意',
    '2. 提升表达清晰度和可读性',
    '3. 如果结构混乱，请优化段落结构',
    '4. 直接输出润色后的正文',
    '',
    `博客标题：${title || '未提供'}`,
    `博客正文：${content || '未提供'}`
  ].join('\n')

  const res = await aiChatTurn(
    buildPayload({
      userId,
      modelId,
      sessionId,
      content: prompt,
      requestType: 'BLOG_ASSISTANT',
      bizType: 'BLOG',
      sceneCode: 'blog.write.polish',
      sessionTitle: `博客润色-${title || '未命名博客'}`
    })
  )

  return extractAnswer(res?.data)
}

export async function aiGenerateBlogSummary({
  userId,
  modelId = 1,
  sessionId = null,
  title = '',
  content = ''
}) {
  const prompt = [
    '请你作为博客写作助手，为下面的博客内容生成摘要和标签建议。',
    '输出要求：',
    '1. 先给出100字内摘要',
    '2. 再给出5个标签建议',
    '',
    `博客标题：${title || '未提供'}`,
    `博客正文：${content || '未提供'}`
  ].join('\n')

  const res = await aiChatTurn(
    buildPayload({
      userId,
      modelId,
      sessionId,
      content: prompt,
      requestType: 'SUMMARY',
      bizType: 'BLOG',
      sceneCode: 'blog.write.summary',
      sessionTitle: `博客摘要-${title || '未命名博客'}`
    })
  )

  return extractAnswer(res?.data)
}

export default {
  aiChatTurn,
  aiChatStream,
  parseBlogSummaryResult,
  buildProjectAiPayload,
  aiSummarizeProject,
  aiSplitProjectTasks,
  aiPolishBlog,
  aiGenerateBlogSummary
}