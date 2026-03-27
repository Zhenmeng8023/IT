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

export function aiChatTurn(data) {
  return request({
    url: `${CHAT_BASE}/turn`,
    method: 'post',
    data
  })
}

export async function aiChatStream({
  body,
  onMessage,
  onError,
  onFinish,
  headers = {}
}) {
  const controller = new AbortController()

  try {
    const response = await fetch(`${getApiBaseUrl()}/ai/chat/stream`, {
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
  content = ''
}) {
  const prompt = [
    '请你作为项目助手，对下面的项目信息做结构化总结。',
    '输出要求：',
    '1. 项目一句话概述',
    '2. 目标用户/使用场景',
    '3. 当前核心功能',
    '4. 风险与待补项',
    '',
    `项目标题：${title || '未提供'}`,
    `项目内容：${content || '未提供'}`
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
      sessionTitle: `项目总结-${title || '未命名项目'}`
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
  content = ''
}) {
  const prompt = [
    '请你作为项目助手，把下面的项目信息拆解成可执行任务。',
    '输出要求：',
    '1. 按阶段拆分',
    '2. 每个任务写清目标、产出物、优先级',
    '3. 最后给出建议执行顺序',
    '',
    `项目标题：${title || '未提供'}`,
    `项目内容：${content || '未提供'}`
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
      sessionTitle: `任务拆解-${title || '未命名项目'}`
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
  aiSummarizeProject,
  aiSplitProjectTasks,
  aiPolishBlog,
  aiGenerateBlogSummary
}