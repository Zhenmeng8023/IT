import request from '@/utils/request'

function normalizePageParams(params = {}) {
  return {
    page: params.page ?? 0,
    size: params.size ?? 10,
    ...params
  }
}

export function extractApiData(res) {
  if (res == null) return null
  if (res.data !== undefined) return res.data
  return res
}

export function extractPageContent(res) {
  const data = extractApiData(res) || {}
  return {
    content: data.content || data.records || data.list || [],
    total: Number(data.totalElements || data.total || 0)
  }
}

/* ===================== AI 模型管理 ===================== */

export function pageAiModels(params = {}) {
  return request({
    url: '/admin/ai/models',
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function listEnabledAiModels() {
  return request({
    url: '/admin/ai/models/enabled',
    method: 'get'
  })
}

export function getActiveAiModel() {
  return request({
    url: '/admin/ai/models/active',
    method: 'get'
  })
}

export function getAiModel(id) {
  return request({
    url: `/admin/ai/models/${id}`,
    method: 'get'
  })
}

export function saveAiModel(data) {
  return request({
    url: '/admin/ai/models',
    method: 'post',
    data
  })
}

export function enableAiModel(id) {
  return request({
    url: `/admin/ai/models/${id}/enable`,
    method: 'put'
  })
}

export function disableAiModel(id) {
  return request({
    url: `/admin/ai/models/${id}/disable`,
    method: 'put'
  })
}

/* ===================== 提示词模板管理 ===================== */

export function pagePromptTemplates(params = {}) {
  return request({
    url: '/ai/prompt-templates',
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function listPromptTemplatesByScene(sceneCode) {
  return request({
    url: `/ai/prompt-templates/scene/${encodeURIComponent(sceneCode)}`,
    method: 'get'
  })
}

export function listPromptTemplatesByOwner(ownerId) {
  return request({
    url: `/ai/prompt-templates/owner/${ownerId}`,
    method: 'get'
  })
}

export function getPromptTemplate(id) {
  return request({
    url: `/ai/prompt-templates/${id}`,
    method: 'get'
  })
}

export function createPromptTemplate(data) {
  return request({
    url: '/ai/prompt-templates',
    method: 'post',
    data
  })
}

export function updatePromptTemplate(id, data) {
  return request({
    url: `/ai/prompt-templates/${id}`,
    method: 'put',
    data
  })
}

export function publishPromptTemplate(id) {
  return request({
    url: `/ai/prompt-templates/${id}/publish`,
    method: 'put'
  })
}

export function disablePromptTemplate(id) {
  return request({
    url: `/ai/prompt-templates/${id}/disable`,
    method: 'put'
  })
}

/* ===================== AI 日志管理 ===================== */

export function createAiCallLog(data) {
  return request({
    url: '/ai/logs/call',
    method: 'post',
    data
  })
}

export function createAiFeedback(data) {
  return request({
    url: '/ai/logs/feedback',
    method: 'post',
    data
  })
}

export function pageUserAiCalls(userId, params = {}) {
  return request({
    url: `/ai/logs/user/${userId}/calls`,
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function pageSessionAiCalls(sessionId, params = {}) {
  return request({
    url: `/ai/logs/session/${sessionId}/calls`,
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function listCallRetrievals(callLogId) {
  return request({
    url: `/ai/logs/call/${callLogId}/retrievals`,
    method: 'get'
  })
}

export function listMessageFeedbacks(messageId) {
  return request({
    url: `/ai/logs/message/${messageId}/feedbacks`,
    method: 'get'
  })
}

export function listUserFeedbacks(userId) {
  return request({
    url: `/ai/logs/user/${userId}/feedbacks`,
    method: 'get'
  })
}

export default {
  extractApiData,
  extractPageContent,

  pageAiModels,
  listEnabledAiModels,
  getActiveAiModel,
  getAiModel,
  saveAiModel,
  enableAiModel,
  disableAiModel,

  pagePromptTemplates,
  listPromptTemplatesByScene,
  listPromptTemplatesByOwner,
  getPromptTemplate,
  createPromptTemplate,
  updatePromptTemplate,
  publishPromptTemplate,
  disablePromptTemplate,

  createAiCallLog,
  createAiFeedback,
  pageUserAiCalls,
  pageSessionAiCalls,
  listCallRetrievals,
  listMessageFeedbacks,
  listUserFeedbacks
}