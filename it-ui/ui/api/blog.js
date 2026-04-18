import axios from 'axios'
import {
  AddComment,
  AddLike,
  BlogApprove,
  BlogReject,
  CancelCollectBlog,
  CheckUserLiked,
  CollectBlog,
  CreateBlog,
  DeleteBlog,
  DeleteComment,
  DeleteLike,
  GetAllBlogs,
  GetAllTags,
  GetBlogById,
  GetBlogRecommendations,
  GetBlogsByAuthorId,
  GetCommentsByPost,
  GetCurrentUser,
  GetRejectedBlogs,
  GetReportsPage,
  HandleReport,
  IsCollected,
  ReplyComment,
  ReportBlog,
  SearchBlogs,
  SearchBlogsByAuthor,
  SearchBlogsByTag,
  SortBlogs,
  UpdateBlog,
  UploadFile
} from '@/api/index'

export const BLOG_STATUS = Object.freeze({
  DRAFT: 'draft',
  PENDING: 'pending',
  PUBLISHED: 'published',
  REJECTED: 'rejected'
})

const BLOG_STATUS_META = Object.freeze({
  [BLOG_STATUS.DRAFT]: { text: '草稿', tagType: 'info', group: 'editing' },
  [BLOG_STATUS.PENDING]: { text: '待审核', tagType: 'warning', group: 'review' },
  [BLOG_STATUS.PUBLISHED]: { text: '已发布', tagType: 'success', group: 'online' },
  [BLOG_STATUS.REJECTED]: { text: '已拒绝', tagType: 'danger', group: 'offline' }
})

const BLOG_NEXT_STATUS_MAP = Object.freeze({
  [BLOG_STATUS.DRAFT]: [BLOG_STATUS.PENDING],
  [BLOG_STATUS.PENDING]: [BLOG_STATUS.DRAFT, BLOG_STATUS.PUBLISHED, BLOG_STATUS.REJECTED],
  [BLOG_STATUS.PUBLISHED]: [BLOG_STATUS.REJECTED, BLOG_STATUS.DRAFT],
  [BLOG_STATUS.REJECTED]: [BLOG_STATUS.DRAFT, BLOG_STATUS.PENDING]
})

function hasOwn(obj, key) {
  return Object.prototype.hasOwnProperty.call(obj, key)
}

export function unwrapApiPayload(response) {
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

function normalizeId(value) {
  if (value === null || value === undefined || value === '') return null
  const asNumber = Number(value)
  if (Number.isFinite(asNumber)) return asNumber
  const text = String(value).trim()
  return text ? text : null
}

function normalizeInt(value, fallback = 0) {
  const num = Number(value)
  return Number.isFinite(num) ? Math.trunc(num) : fallback
}

function normalizeText(value, fallback = '') {
  if (value === null || value === undefined) return fallback
  const text = String(value).trim()
  return text || fallback
}

function normalizeStatus(status) {
  const normalized = normalizeText(status, BLOG_STATUS.DRAFT).toLowerCase()
  if (BLOG_STATUS_META[normalized]) return normalized
  return BLOG_STATUS.DRAFT
}

function normalizeTagNames(tags) {
  if (Array.isArray(tags)) {
    return tags
      .map(item => normalizeText(item))
      .filter(Boolean)
  }
  if (tags && typeof tags === 'object') {
    return Object.values(tags)
      .map(item => normalizeText(item))
      .filter(Boolean)
  }
  return []
}

function normalizeTagIds(tagIds, tagsObject) {
  if (Array.isArray(tagIds)) {
    return tagIds
      .map(item => normalizeId(item))
      .filter(item => item !== null)
  }
  if (tagsObject && typeof tagsObject === 'object') {
    return Object.keys(tagsObject)
      .map(item => normalizeId(item))
      .filter(item => item !== null)
  }
  return []
}

function normalizeAuthor(author, fallback = {}) {
  if (author && typeof author === 'object') {
    const id = normalizeId(author.id || fallback.authorId || fallback.userId)
    const nickname = normalizeText(author.nickname)
    const username = normalizeText(author.username)
    const displayName = normalizeText(author.displayName || nickname || username || fallback.authorName || '未知作者')
    return {
      id,
      username,
      nickname,
      displayName,
      avatar: author.avatar || author.avatarUrl || fallback.avatar || '',
      avatarUrl: author.avatarUrl || author.avatar || fallback.avatar || '',
      email: normalizeText(author.email)
    }
  }

  const id = normalizeId(fallback.authorId || fallback.userId)
  const displayName = normalizeText(author || fallback.authorName || fallback.username || '未知作者')
  return {
    id,
    username: normalizeText(fallback.username),
    nickname: normalizeText(fallback.nickname),
    displayName,
    avatar: fallback.avatar || '',
    avatarUrl: fallback.avatar || '',
    email: normalizeText(fallback.email)
  }
}

export function normalizeBlog(raw = {}) {
  const status = normalizeStatus(raw.status)
  const statusMeta = BLOG_STATUS_META[status]
  const author = normalizeAuthor(raw.author, {
    authorId: raw.authorId,
    authorName: raw.authorName || raw.author,
    avatar: raw.avatar || raw.avatarUrl
  })

  return {
    ...raw,
    id: normalizeId(raw.id),
    title: normalizeText(raw.title, '无标题'),
    summary: normalizeText(raw.summary),
    content: raw.content || '',
    previewContent: raw.previewContent || '',
    coverImageUrl: raw.coverImageUrl || '',
    tags: normalizeTagNames(raw.tags),
    tagIds: normalizeTagIds(raw.tagIds, raw.tags),
    author,
    status,
    statusLabel: normalizeText(raw.statusLabel, statusMeta.text),
    statusGroup: normalizeText(raw.statusGroup, statusMeta.group),
    nextStatuses: Array.isArray(raw.nextStatuses) ? raw.nextStatuses.map(item => normalizeStatus(item)) : BLOG_NEXT_STATUS_MAP[status],
    publishTime: raw.publishTime || null,
    createdAt: raw.createdAt || null,
    updatedAt: raw.updatedAt || null,
    viewCount: normalizeInt(raw.viewCount),
    likeCount: normalizeInt(raw.likeCount),
    collectCount: normalizeInt(raw.collectCount),
    downloadCount: normalizeInt(raw.downloadCount),
    reportCount: normalizeInt(raw.reportCount),
    price: normalizeInt(raw.price),
    locked: raw.locked === true,
    lockType: normalizeText(raw.lockType, 'none'),
    hasAccess: raw.hasAccess !== false,
    hasPurchased: raw.hasPurchased === true,
    isVipUser: raw.isVipUser === true,
    rejectReason: normalizeText(raw.rejectReason || raw.auditReason),
    auditReason: normalizeText(raw.auditReason),
    isMarked: raw.isMarked === true
  }
}

export function normalizeReport(raw = {}) {
  return {
    ...raw,
    id: normalizeId(raw.id),
    reporterId: normalizeId(raw.reporterId),
    reporterName: normalizeText(raw.reporterName, '未知用户'),
    targetType: normalizeText(raw.targetType).toLowerCase(),
    targetId: normalizeId(raw.targetId),
    targetTitle: normalizeText(raw.targetTitle, raw.targetId ? `博客#${raw.targetId}` : '未知博客'),
    reason: normalizeText(raw.reason),
    status: normalizeText(raw.status, 'pending').toLowerCase(),
    createdAt: raw.createdAt || null,
    processedAt: raw.processedAt || null
  }
}

export function normalizeComment(raw = {}) {
  const commentAuthor = normalizeAuthor(raw.author, {
    authorId: raw.authorId,
    authorName: raw.nickname || raw.username,
    avatar: raw.avatar
  })
  const status = normalizeText(raw.status, 'normal').toLowerCase()
  const deleted = raw.deleted === true || status === 'deleted'

  return {
    ...raw,
    id: normalizeId(raw.id || raw.commentId),
    parentCommentId: normalizeId(raw.parentCommentId || raw.parentId || raw.parent_id),
    postId: normalizeId(raw.postId || raw.post_id),
    authorId: normalizeId(raw.authorId || commentAuthor.id),
    content: normalizeText(raw.content || raw.body),
    createdAt: raw.createdAt || raw.createTime || raw.createDate || null,
    likes: normalizeInt(raw.likes),
    status,
    deleted,
    canDelete: raw.canDelete === true,
    nickname: normalizeText(raw.nickname || commentAuthor.displayName, '匿名用户'),
    avatar: raw.avatar || commentAuthor.avatar || '',
    author: commentAuthor
  }
}

function normalizeListPayload(response) {
  const payload = unwrapApiPayload(response)
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.items)) return payload.items
  if (Array.isArray(payload?.list)) return payload.list
  if (Array.isArray(payload?.rows)) return payload.rows
  if (Array.isArray(payload?.records)) return payload.records
  if (Array.isArray(payload?.content)) return payload.content
  return []
}

function normalizePagePayload(response) {
  const payload = unwrapApiPayload(response) || {}
  if (Array.isArray(payload)) {
    return {
      list: payload,
      total: payload.length,
      page: 1,
      pageSize: payload.length || 0
    }
  }

  const list = normalizeListPayload(payload)
  const totalCandidate = payload.totalElements ?? payload.total ?? payload.count ?? list.length
  const total = Number.isFinite(Number(totalCandidate)) ? Number(totalCandidate) : list.length
  const pageCandidate = payload.number ?? payload.page ?? 0
  const page = Number.isFinite(Number(pageCandidate))
    ? (hasOwn(payload, 'number') ? Number(pageCandidate) + 1 : Number(pageCandidate) || 1)
    : 1
  const pageSizeCandidate = payload.size ?? payload.pageSize ?? payload.limit ?? list.length
  const pageSize = Number.isFinite(Number(pageSizeCandidate)) ? Number(pageSizeCandidate) : list.length

  return { list, total, page, pageSize }
}

function paginateList(list, page, pageSize) {
  const safePageSize = Math.max(1, Number(pageSize) || 10)
  const safePage = Math.max(1, Number(page) || 1)
  const start = (safePage - 1) * safePageSize
  return {
    list: list.slice(start, start + safePageSize),
    total: list.length,
    page: safePage,
    pageSize: safePageSize
  }
}

function sortRouteByType(sortType = 'time_desc') {
  const map = {
    hot: 'hot',
    time_desc: 'time/newest',
    time_asc: 'time/oldest'
  }
  return map[sortType] || map.time_desc
}

function getBlogSortTime(blog = {}) {
  const raw = blog.publishTime || blog.createdAt || blog.updatedAt
  if (!raw) return 0
  const time = new Date(raw).getTime()
  return Number.isFinite(time) ? time : 0
}

function getBlogHotScore(blog = {}) {
  return normalizeInt(blog.viewCount) +
    normalizeInt(blog.likeCount) * 5 +
    normalizeInt(blog.collectCount) * 10 +
    normalizeInt(blog.downloadCount) * 8
}

function sortBlogList(list = [], sortType = 'time_desc') {
  const sorted = Array.isArray(list) ? [...list] : []
  return sorted.sort((a, b) => {
    if (sortType === 'hot') {
      const scoreDiff = getBlogHotScore(b) - getBlogHotScore(a)
      if (scoreDiff !== 0) return scoreDiff
      return getBlogSortTime(b) - getBlogSortTime(a)
    }
    if (sortType === 'time_asc') {
      return getBlogSortTime(a) - getBlogSortTime(b)
    }
    return getBlogSortTime(b) - getBlogSortTime(a)
  })
}

export function blogStatusText(status) {
  return (BLOG_STATUS_META[normalizeStatus(status)] || BLOG_STATUS_META[BLOG_STATUS.DRAFT]).text
}

export function blogStatusTagType(status) {
  return (BLOG_STATUS_META[normalizeStatus(status)] || BLOG_STATUS_META[BLOG_STATUS.DRAFT]).tagType
}

export async function fetchCurrentUserProfile() {
  const response = await GetCurrentUser()
  return unwrapApiPayload(response)
}

export async function fetchBlogFeed({
  sortType = 'time_desc',
  keyword = '',
  tag = '',
  author = '',
  page = 1,
  pageSize = 5
} = {}) {
  const normalizedKeyword = normalizeText(keyword)
  const normalizedTag = normalizeText(tag)
  const normalizedAuthor = normalizeText(author)

  let response
  if (normalizedKeyword) {
    response = await SearchBlogs({ keyword: normalizedKeyword })
  } else if (normalizedTag) {
    response = await SearchBlogsByTag({ keyword: normalizedTag })
  } else if (normalizedAuthor) {
    response = await SearchBlogsByAuthor({ keyword: normalizedAuthor })
  } else {
    response = await SortBlogs(sortRouteByType(sortType))
  }

  const normalized = normalizeListPayload(response).map(normalizeBlog)
  return paginateList(sortBlogList(normalized, sortType), page, pageSize)
}

export async function fetchBlogDetail(blogId) {
  const response = await GetBlogById(blogId)
  return normalizeBlog(unwrapApiPayload(response) || {})
}

export async function fetchBlogRecommendations(blogId, { size = 6 } = {}) {
  const response = await GetBlogRecommendations(blogId, { size })
  const payload = unwrapApiPayload(response) || {}
  return {
    items: normalizeListPayload(payload).map(normalizeBlog),
    source: normalizeText(payload.source, 'fallback'),
    algorithmVersion: normalizeText(payload.algorithmVersion),
    generatedAt: payload.generatedAt || null
  }
}

export async function fetchBlogComments(blogId) {
  const response = await GetCommentsByPost(blogId)
  return normalizeListPayload(response).map(normalizeComment)
}

export async function createBlogComment(data) {
  const response = await AddComment(data)
  return normalizeComment(unwrapApiPayload(response) || {})
}

export async function replyBlogComment(data) {
  const response = await ReplyComment(data)
  return normalizeComment(unwrapApiPayload(response) || {})
}

export async function removeBlogComment(commentId) {
  await DeleteComment(commentId)
}

export async function fetchLikeRecord(userId, targetId) {
  try {
    const response = await CheckUserLiked(userId, 'blog', targetId)
    const payload = unwrapApiPayload(response)
    return payload && payload.id ? payload : null
  } catch (error) {
    if (error && error.response && error.response.status === 404) {
      return null
    }
    throw error
  }
}

export async function createLikeRecord(data) {
  const response = await AddLike(data)
  return unwrapApiPayload(response)
}

export async function removeLikeRecord(likeId) {
  await DeleteLike(likeId)
}

export async function fetchCollectRecord(userId, targetId) {
  try {
    const response = await IsCollected(userId, 'blog', targetId)
    const payload = unwrapApiPayload(response)
    return payload && payload.id ? payload : null
  } catch (error) {
    if (error && error.response && error.response.status === 404) {
      return null
    }
    throw error
  }
}

export async function createCollectRecord(data) {
  const response = await CollectBlog(data)
  return unwrapApiPayload(response)
}

export async function removeCollectRecord(collectId) {
  await CancelCollectBlog(collectId)
}

export async function submitBlogReport(blogId, reason) {
  const response = await ReportBlog(blogId, { reason })
  return normalizeReport(unwrapApiPayload(response) || {})
}

export async function fetchBlogTags() {
  const response = await GetAllTags()
  return normalizeListPayload(response).map(item => ({
    id: normalizeId(item.id),
    name: normalizeText(item.name)
  })).filter(item => item.id !== null && item.name)
}

function normalizeBlogSavePayload(payload = {}) {
  const tagIds = Array.isArray(payload.tagIds) ? payload.tagIds : []
  return {
    title: normalizeText(payload.title),
    summary: normalizeText(payload.summary),
    content: payload.content || '',
    tagIds: tagIds
      .map(item => normalizeId(item))
      .filter(item => item !== null),
    status: normalizeStatus(payload.status || BLOG_STATUS.DRAFT),
    price: normalizeInt(payload.price, 0)
  }
}

export async function saveBlog(payload = {}) {
  const normalizedPayload = normalizeBlogSavePayload(payload)
  const response = payload.id
    ? await UpdateBlog(payload.id, normalizedPayload)
    : await CreateBlog(normalizedPayload)
  return normalizeBlog(unwrapApiPayload(response) || {})
}

export async function removeBlog(blogId) {
  await DeleteBlog(blogId)
}

export async function fetchMyBlogList({
  userId,
  page = 1,
  pageSize = 10,
  status = '',
  keyword = ''
} = {}) {
  if (!userId) {
    return { list: [], total: 0, page: 1, pageSize: Math.max(1, Number(pageSize) || 10) }
  }

  const response = await GetBlogsByAuthorId(userId)
  const normalizedStatus = normalizeText(status).toLowerCase()
  const normalizedKeyword = normalizeText(keyword).toLowerCase()

  let list = normalizeListPayload(response).map(normalizeBlog)
  if (normalizedStatus) {
    list = list.filter(item => item.status === normalizedStatus)
  }
  if (normalizedKeyword) {
    list = list.filter(item => {
      const title = normalizeText(item.title).toLowerCase()
      const summary = normalizeText(item.summary).toLowerCase()
      return title.includes(normalizedKeyword) || summary.includes(normalizedKeyword)
    })
  }

  list = list.sort((a, b) => {
    const aTime = new Date(a.updatedAt || a.createdAt || 0).getTime()
    const bTime = new Date(b.updatedAt || b.createdAt || 0).getTime()
    return bTime - aTime
  })

  return paginateList(list, page, pageSize)
}

export async function fetchBlogReviewList({
  status = BLOG_STATUS.PENDING,
  keyword = '',
  page = 1,
  pageSize = 10
} = {}) {
  const normalizedStatus = normalizeStatus(status)
  const normalizedKeyword = normalizeText(keyword)

  if (normalizedKeyword) {
    const response = await axios.get('/api/blogs/search/query', {
      params: {
        keyword: normalizedKeyword,
        scope: 'all',
        sort: 'newest',
        status: normalizedStatus
      }
    })
    const payload = unwrapApiPayload(response) || {}
    const list = normalizeListPayload(payload.items || payload).map(normalizeBlog)
    return paginateList(list, page, pageSize)
  }

  if (normalizedStatus === BLOG_STATUS.PENDING) {
    const response = await axios.get('/api/blogs/pending', {
      params: {
        page: Math.max(0, Number(page) - 1),
        size: pageSize
      }
    })
    const pagePayload = normalizePagePayload(response)
    return {
      list: pagePayload.list.map(normalizeBlog),
      total: pagePayload.total,
      page: pagePayload.page,
      pageSize: pagePayload.pageSize
    }
  }

  const response = normalizedStatus === BLOG_STATUS.REJECTED
    ? await GetRejectedBlogs()
    : await GetAllBlogs()

  const list = normalizeListPayload(response).map(normalizeBlog)
  return paginateList(list, page, pageSize)
}

export async function approveBlog(blogId) {
  const response = await BlogApprove(blogId)
  return normalizeBlog(unwrapApiPayload(response) || {})
}

export async function rejectBlog(blogId, reason) {
  const response = await BlogReject(blogId, { reason })
  return normalizeBlog(unwrapApiPayload(response) || {})
}

export async function fetchReportReviewList({
  status = '',
  page = 1,
  pageSize = 10,
  targetType = 'blog'
} = {}) {
  const response = await GetReportsPage({
    page: Math.max(0, Number(page) - 1),
    size: pageSize,
    targetType,
    status: normalizeText(status).toLowerCase() || undefined
  })
  const pagePayload = normalizePagePayload(response)
  return {
    list: pagePayload.list.map(normalizeReport),
    total: pagePayload.total,
    page: pagePayload.page,
    pageSize: pagePayload.pageSize
  }
}

export async function processReport(reportId, action) {
  const response = await HandleReport(reportId, { status: action })
  return normalizeReport(unwrapApiPayload(response) || {})
}

export async function uploadBlogImage(formData) {
  const response = await UploadFile(formData)
  return unwrapApiPayload(response) || {}
}
