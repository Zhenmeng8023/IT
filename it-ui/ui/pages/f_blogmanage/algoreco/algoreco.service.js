import axios from 'axios'
import { fetchBlogDetail, fetchBlogReviewList, normalizeBlog, unwrapApiPayload } from '@/api/blog'

export const RECO_SCENE_CODE = 'blog.detail.recommend'
export const DEFAULT_RECO_SIZE = 6
export const MAX_RECO_SIZE = 12

function normalizeKeyword(value) {
  return String(value || '').trim()
}

function normalizeListPayload(payload) {
  if (Array.isArray(payload)) return payload
  if (!payload || typeof payload !== 'object') return []
  if (Array.isArray(payload.items)) return payload.items
  if (Array.isArray(payload.list)) return payload.list
  if (Array.isArray(payload.records)) return payload.records
  if (Array.isArray(payload.rows)) return payload.rows
  if (Array.isArray(payload.content)) return payload.content
  return []
}

function normalizeRecoSource(value) {
  const source = String(value || '').trim().toLowerCase()
  if (source === 'algorithm' || source === 'mixed' || source === 'fallback') return source
  return 'fallback'
}

function normalizeRecoSize(size) {
  const num = Number(size)
  if (!Number.isFinite(num)) return DEFAULT_RECO_SIZE
  return Math.max(1, Math.min(MAX_RECO_SIZE, Math.trunc(num)))
}

function filterPublishedBlogs(list) {
  return (Array.isArray(list) ? list : [])
    .map(item => normalizeBlog(item || {}))
    .filter(item => item && item.id !== null && item.status === 'published')
}

function sortBlogsByTimeDesc(list) {
  return [...list].sort((a, b) => {
    const aTime = new Date(a.updatedAt || a.publishTime || a.createdAt || 0).getTime()
    const bTime = new Date(b.updatedAt || b.publishTime || b.createdAt || 0).getTime()
    return bTime - aTime
  })
}

function dedupeById(list) {
  const map = new Map()
  ;(Array.isArray(list) ? list : []).forEach(item => {
    if (item && item.id !== null && item.id !== undefined && !map.has(item.id)) {
      map.set(item.id, item)
    }
  })
  return Array.from(map.values())
}

async function fetchPublishedBySearchQuery(keyword) {
  const params = {
    scope: 'all',
    sort: 'newest',
    status: 'published'
  }
  if (keyword) {
    params.keyword = keyword
  }

  const response = await axios.get('/api/blogs/search/query', { params })
  const payload = unwrapApiPayload(response) || {}
  return normalizeListPayload(payload.items || payload)
}

/**
 * 后端当前没有独立“推荐任务列表”接口，这里用“已发布博客列表”构建任务种子。
 * 优先尝试 /api/blogs/search/query，再回退到现有 blog API 封装。
 */
export async function fetchAlgoRecoTaskSeeds({ keyword = '' } = {}) {
  const normalizedKeyword = normalizeKeyword(keyword)
  let primaryError = null
  let primaryList = []

  try {
    primaryList = await fetchPublishedBySearchQuery(normalizedKeyword)
  } catch (error) {
    primaryError = error
  }

  const primaryPublished = sortBlogsByTimeDesc(dedupeById(filterPublishedBlogs(primaryList)))
  if (primaryPublished.length > 0) {
    return primaryPublished
  }

  let fallbackError = null
  let fallbackList = []
  try {
    const fallback = await fetchBlogReviewList({
      status: 'published',
      keyword: normalizedKeyword,
      page: 1,
      pageSize: 300
    })
    fallbackList = Array.isArray(fallback?.list) ? fallback.list : []
  } catch (error) {
    fallbackError = error
  }

  const fallbackPublished = sortBlogsByTimeDesc(dedupeById(filterPublishedBlogs(fallbackList)))
  if (fallbackPublished.length > 0) {
    return fallbackPublished
  }

  if (primaryError) throw primaryError
  if (fallbackError) throw fallbackError
  return []
}

/**
 * 触发/查询博客推荐结果。
 * forceRefresh=true 时请求后端跳过缓存，重新计算推荐结果。
 */
export async function triggerAlgoRecoTask(blogId, { size = DEFAULT_RECO_SIZE, forceRefresh = false } = {}) {
  const safeId = Number(blogId)
  if (!Number.isFinite(safeId)) {
    throw new Error('博客 ID 非法，无法触发推荐任务')
  }

  const params = {
    size: normalizeRecoSize(size)
  }
  if (forceRefresh) {
    params.forceRefresh = true
  }

  const response = await axios.get(`/api/blogs/${safeId}/recommendations`, { params })
  const payload = unwrapApiPayload(response) || {}
  const items = normalizeListPayload(payload.items || payload).map(item => normalizeBlog(item || {}))

  return {
    blogId: payload.currentBlogId || safeId,
    source: normalizeRecoSource(payload.source),
    algorithmVersion: payload.algorithmVersion || '',
    generatedAt: payload.generatedAt || null,
    total: Number(payload.total || items.length || 0),
    items,
    raw: payload
  }
}

export async function fetchAlgoRecoBlogDetail(blogId) {
  return fetchBlogDetail(blogId)
}
