import request from '@/utils/request'
import { getToken } from '@/utils/auth'

const KB_BASE = '/ai/knowledge-bases'
const SESSION_BASE = '/ai/sessions'
const CHAT_BASE = '/ai/chat'

function normalizePageParams(params = {}) {
  return {
    page: params.page ?? 0,
    size: params.size ?? 10,
    ...params
  }
}

function getApiBaseUrl() {
  const baseURL = request?.defaults?.baseURL || 'http://localhost:18080/api'
  return String(baseURL).replace(/\/$/, '')
}

function readToken() {
  const tokenFromAuth = typeof getToken === 'function' ? getToken() : ''

  if (tokenFromAuth) {
    return tokenFromAuth
  }

  if (typeof window !== 'undefined') {
    return (
      localStorage.getItem('token') ||
      localStorage.getItem('Authorization') ||
      sessionStorage.getItem('token') ||
      sessionStorage.getItem('Authorization') ||
      ''
    )
  }

  return ''
}

function buildAuthHeaders(extraHeaders = {}) {
  const token = readToken()
  const rawToken = token.startsWith('Bearer ') ? token.slice(7).trim() : token

  return {
    'Content-Type': 'application/json',
    ...(rawToken
      ? {
          Authorization: `Bearer ${rawToken}`,
          'X-Token': rawToken
        }
      : {}),
    ...extraHeaders
  }
}

export function pageKnowledgeBasesByOwner(ownerId, params = {}) {
  return request({
    url: `${KB_BASE}/owner/${ownerId}`,
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function pageKnowledgeBasesByProject(projectId, params = {}) {
  return request({
    url: `${KB_BASE}/project/${projectId}`,
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function getKnowledgeBase(id) {
  return request({
    url: `${KB_BASE}/${id}`,
    method: 'get'
  })
}

export function createKnowledgeBase(data) {
  return request({
    url: KB_BASE,
    method: 'post',
    data
  })
}

export function updateKnowledgeBase(id, data) {
  return request({
    url: `${KB_BASE}/${id}`,
    method: 'put',
    data
  })
}

export function pageKnowledgeDocuments(knowledgeBaseId, params = {}) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/documents`,
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function addKnowledgeDocument(knowledgeBaseId, data) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/documents`,
    method: 'post',
    data
  })
}

export function listDocumentChunks(documentId) {
  return request({
    url: `${KB_BASE}/documents/${documentId}/chunks`,
    method: 'get'
  })
}

export function listKnowledgeBaseMembers(knowledgeBaseId) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/members`,
    method: 'get'
  })
}

export function addKnowledgeBaseMember(knowledgeBaseId, data) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/members`,
    method: 'post',
    data
  })
}

export function removeKnowledgeBaseMember(knowledgeBaseId, memberId) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/members/${memberId}`,
    method: 'delete'
  })
}

export function createKnowledgeIndexTask(knowledgeBaseId, data) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/index-tasks`,
    method: 'post',
    data
  })
}

export function listDocumentIndexTasks(documentId) {
  return request({
    url: `${KB_BASE}/documents/${documentId}/index-tasks`,
    method: 'get'
  })
}

export function createAiSession(data) {
  return request({
    url: SESSION_BASE,
    method: 'post',
    data
  })
}

export function getAiSession(sessionId) {
  return request({
    url: `${SESSION_BASE}/${sessionId}`,
    method: 'get'
  })
}

export function pageAiSessions(params = {}) {
  return request({
    url: SESSION_BASE,
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function pageAiSessionMessages(sessionId, params = {}) {
  return request({
    url: `${SESSION_BASE}/${sessionId}/messages`,
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function bindSessionKnowledgeBases(sessionId, knowledgeBaseIds = []) {
  return request({
    url: `${SESSION_BASE}/${sessionId}/knowledge-bases`,
    method: 'put',
    data: { knowledgeBaseIds }
  })
}

export function archiveAiSession(sessionId) {
  return request({
    url: `${SESSION_BASE}/${sessionId}/archive`,
    method: 'put'
  })
}

export function deleteAiSession(sessionId) {
  return request({
    url: `${SESSION_BASE}/${sessionId}`,
    method: 'delete'
  })
}

export function chatWithKnowledgeBase(data) {
  return request({
    url: `${CHAT_BASE}/turn`,
    method: 'post',
    data
  })
}

export async function streamChatWithKnowledgeBase({
  body,
  onMessage,
  onError,
  onFinish,
  headers = {}
}) {
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
            onMessage && onMessage(JSON.parse(raw))
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
          onMessage && onMessage(JSON.parse(raw))
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

export default {
  pageKnowledgeBasesByOwner,
  pageKnowledgeBasesByProject,
  getKnowledgeBase,
  createKnowledgeBase,
  updateKnowledgeBase,
  pageKnowledgeDocuments,
  addKnowledgeDocument,
  listDocumentChunks,
  listKnowledgeBaseMembers,
  addKnowledgeBaseMember,
  removeKnowledgeBaseMember,
  createKnowledgeIndexTask,
  listDocumentIndexTasks,
  createAiSession,
  getAiSession,
  pageAiSessions,
  pageAiSessionMessages,
  bindSessionKnowledgeBases,
  archiveAiSession,
  deleteAiSession,
  chatWithKnowledgeBase,
  streamChatWithKnowledgeBase
}