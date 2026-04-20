import request from '@/utils/request'
import { getProjectDetail, getRelatedProjects, pageProjects } from '@/api/project'

export const RECO_SCENE_CODE = 'project.related.recommend'
export const DEFAULT_RECO_SIZE = 6
export const MAX_RECO_SIZE = 12

function hasOwn(obj, key) {
  return Object.prototype.hasOwnProperty.call(obj, key)
}

function normalizeText(value, fallback = '') {
  if (value === null || value === undefined) return fallback
  const text = String(value).trim()
  return text || fallback
}

function normalizeId(value) {
  if (value === null || value === undefined || value === '') return null
  const num = Number(value)
  if (Number.isFinite(num) && num > 0) return num
  return null
}

function unwrapApiPayload(response) {
  if (response === null || response === undefined) return null
  const payload = hasOwn(response, 'data') ? response.data : response
  if (
    payload &&
    typeof payload === 'object' &&
    hasOwn(payload, 'data') &&
    (hasOwn(payload, 'code') || hasOwn(payload, 'success') || hasOwn(payload, 'message'))
  ) {
    return payload.data
  }
  return payload
}

function normalizeListPayload(payload) {
  const unwrapped = unwrapApiPayload(payload)
  if (Array.isArray(unwrapped)) return unwrapped
  if (!unwrapped || typeof unwrapped !== 'object') return []
  if (Array.isArray(unwrapped.items)) return unwrapped.items
  if (Array.isArray(unwrapped.list)) return unwrapped.list
  if (Array.isArray(unwrapped.records)) return unwrapped.records
  if (Array.isArray(unwrapped.rows)) return unwrapped.rows
  if (Array.isArray(unwrapped.content)) return unwrapped.content
  if (Array.isArray(unwrapped.recommendations)) return unwrapped.recommendations
  return []
}

function normalizeTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags.filter(Boolean)
  if (typeof tags === 'string') {
    try {
      const parsed = JSON.parse(tags)
      if (Array.isArray(parsed)) {
        return parsed.filter(Boolean)
      }
    } catch (error) {}
    return tags
      .split(',')
      .map(item => item.trim())
      .filter(Boolean)
  }
  return []
}

function normalizeProjectItem(raw = {}) {
  const id = normalizeId(raw.id || raw.projectId)
  return {
    ...raw,
    id,
    name: normalizeText(raw.name || raw.title || raw.projectName, id ? `项目#${id}` : '未命名项目'),
    description: normalizeText(raw.description || raw.summary),
    type: normalizeText(raw.category || raw.type || raw.projectType || 'other', 'other'),
    status: normalizeText(raw.status || 'unknown', 'unknown').toLowerCase(),
    visibility: normalizeText(raw.visibility || raw.projectVisibility),
    tags: normalizeTags(raw.tags),
    authorName: normalizeText(raw.authorName || raw.ownerName || raw.creatorName),
    updatedAt: raw.updatedAt || raw.updateTime || raw.modifyTime || raw.createdAt || raw.createTime || null,
    createdAt: raw.createdAt || raw.createTime || null
  }
}

function normalizeRecoItem(raw = {}) {
  const normalized = normalizeProjectItem(raw)
  return {
    ...normalized,
    score: raw.score !== undefined ? Number(raw.score) : null,
    reason: normalizeText(raw.reason || raw.recommendReason || raw.matchReason)
  }
}

function normalizeRecoSize(size) {
  const num = Number(size)
  if (!Number.isFinite(num)) return DEFAULT_RECO_SIZE
  return Math.max(1, Math.min(MAX_RECO_SIZE, Math.trunc(num)))
}

function normalizeRecoSource(source, fallback = 'related') {
  const value = normalizeText(source).toLowerCase()
  if (['algorithm', 'mixed', 'fallback', 'related'].includes(value)) return value
  return fallback
}

function shouldFallback(error) {
  const status = Number(error?.response?.status || 0)
  return status === 404 || status === 405 || status === 501
}

export function resolveRecoErrorMessage(error) {
  return (
    error?.response?.data?.message ||
    error?.response?.data?.msg ||
    error?.message ||
    '请求失败'
  )
}

export async function fetchProjectRecoTaskSeeds({ keyword = '', type = '', page = 1, size = 300 } = {}) {
  const params = {
    page: Number(page) || 1,
    size: Number(size) || 300,
    sortBy: 'latest'
  }
  const normalizedKeyword = normalizeText(keyword)
  const normalizedType = normalizeText(type)
  if (normalizedKeyword) params.keyword = normalizedKeyword
  if (normalizedType) params.category = normalizedType

  const response = await pageProjects(params)
  const payload = unwrapApiPayload(response) || {}
  const list = normalizeListPayload(payload)
  return list
    .map(item => normalizeProjectItem(item))
    .filter(item => item.id !== null)
}

export async function triggerProjectRecoTask(projectId, { size = DEFAULT_RECO_SIZE, forceRefresh = false } = {}) {
  const safeProjectId = normalizeId(projectId)
  if (!safeProjectId) {
    throw new Error('项目 ID 非法，无法触发推荐')
  }

  const safeSize = normalizeRecoSize(size)
  const preferredParams = { size: safeSize }
  if (forceRefresh) {
    preferredParams.forceRefresh = true
  }

  let preferredError = null
  try {
    const preferredResponse = await request({
      url: `/project/${safeProjectId}/recommendations`,
      method: 'get',
      params: preferredParams
    })
    const preferredPayload = unwrapApiPayload(preferredResponse) || {}
    const preferredItems = normalizeListPayload(preferredPayload).map(item => normalizeRecoItem(item))
    return {
      projectId: safeProjectId,
      source: normalizeRecoSource(preferredPayload.source, 'algorithm'),
      algorithmVersion: normalizeText(preferredPayload.algorithmVersion),
      generatedAt: preferredPayload.generatedAt || preferredPayload.updatedAt || new Date().toISOString(),
      total: Number(preferredPayload.total || preferredItems.length || 0),
      items: preferredItems,
      raw: preferredPayload,
      usedFallback: false,
      preferredEndpointMissing: false
    }
  } catch (error) {
    preferredError = error
    if (!shouldFallback(error)) {
      throw error
    }
  }

  const fallbackResponse = await getRelatedProjects(safeProjectId, { size: safeSize })
  const fallbackPayload = unwrapApiPayload(fallbackResponse) || []
  const fallbackItems = normalizeListPayload(fallbackPayload).map(item => normalizeRecoItem(item))

  return {
    projectId: safeProjectId,
    source: 'related',
    algorithmVersion: '',
    generatedAt: new Date().toISOString(),
    total: fallbackItems.length,
    items: fallbackItems,
    raw: fallbackPayload,
    usedFallback: true,
    preferredEndpointMissing: shouldFallback(preferredError)
  }
}

export async function fetchProjectRecoDetail(projectId) {
  const safeProjectId = normalizeId(projectId)
  if (!safeProjectId) {
    throw new Error('项目 ID 非法，无法加载详情')
  }
  const response = await getProjectDetail(safeProjectId)
  const payload = unwrapApiPayload(response) || {}
  return normalizeProjectItem(payload)
}
