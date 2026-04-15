import {
  GetCurrentUser,
  GetUserById,
  GetLikesByUser,
  GetCollectsByUser,
  GetLikesReceivedByAuthor,
  GetCollectsReceivedByAuthor,
  GetUserLogs,
  GetBlogsByAuthorId,
  GetUserCirclePosts,
  GetMyKnowledgeProducts,
  GetUserTotalRevenue,
  GetUserActivityHeatmap
} from '@/api/index.js'

const DEFAULT_AVATAR = '/pic/choubi.jpg'

function unwrapResponse(response) {
  if (response && response.data !== undefined) {
    return response.data
  }
  return response
}

function ensureArray(source) {
  if (Array.isArray(source)) return source
  if (source && Array.isArray(source.records)) return source.records
  if (source && Array.isArray(source.rows)) return source.rows
  if (source && Array.isArray(source.list)) return source.list
  if (source && source.data && Array.isArray(source.data)) return source.data
  if (source && source.data && Array.isArray(source.data.records)) return source.data.records
  return []
}

function normalizeNumber(value, fallback = 0) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

function normalizeAvatarUrl(url) {
  if (!url) return DEFAULT_AVATAR
  const isLocalPath = /^[A-Za-z]:\\/i.test(url) || /^file:\/\//i.test(url)
  return isLocalPath ? DEFAULT_AVATAR : url
}

function normalizeUserProfile(user = {}) {
  return {
    id: user.id || '',
    username: user.username || '',
    nickname: user.nickname || '',
    email: user.email || '',
    phone: user.phone || '',
    bio: user.bio || '',
    gender: user.gender || '',
    birthday: user.birthday || '',
    regionId: user.regionId || null,
    regionName: user.regionName || '',
    authorTagId: user.authorTagId || user.author_tags_id || null,
    authorTagName: user.authorTagName || '',
    avatarUrl: normalizeAvatarUrl(user.avatarUrl)
  }
}

function mapBlogList(list = []) {
  return list.map((blog) => ({
    id: blog.id,
    title: blog.title || '未命名博客',
    summary: blog.summary || blog.description || blog.excerpt || '',
    content: blog.content || '',
    viewCount: normalizeNumber(blog.viewCount),
    likeCount: normalizeNumber(blog.likeCount),
    createTime: blog.createTime || blog.createdAt || blog.publishTime || '',
    status: blog.status || (blog.published ? 'published' : 'draft'),
    price: normalizeNumber(blog.price)
  }))
}

function mapPostList(list = []) {
  return list.map((post) => ({
    id: post.id,
    title: post.title || '未命名帖子',
    summary: post.summary || '',
    content: post.content || '',
    commentCount: normalizeNumber(post.commentCount),
    likeCount: normalizeNumber(post.likeCount),
    createTime: post.createTime || post.createdAt || post.publishTime || ''
  }))
}

function mapKnowledgeList(list = []) {
  return list.map((item) => ({
    id: item.id,
    title: item.title || '未命名知识产品',
    summary: item.summary || item.description || '',
    content: item.content || '',
    price: normalizeNumber(item.price),
    viewCount: normalizeNumber(item.viewCount),
    likeCount: normalizeNumber(item.likeCount),
    createTime: item.publishTime || item.createTime || item.createdAt || '',
    status: item.status || 'draft'
  }))
}

function isKnowledgeCandidate(blog = {}) {
  if (blog.isKnowledgeProduct === true) return true
  if (normalizeNumber(blog.price) > 0) return true
  const type = String(blog.blogType || blog.type || blog.category || '').toLowerCase()
  return type.includes('knowledge') || type.includes('paid')
}

function toDateOnly(value) {
  if (!value) return null
  const normalizedValue = typeof value === 'string' ? value.replace(/-/g, '/') : value
  const date = new Date(normalizedValue)
  if (Number.isNaN(date.getTime())) return null
  date.setHours(0, 0, 0, 0)
  return date
}

function dateKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function parseActivityPayload(response) {
  const raw = unwrapResponse(response)
  const payload = raw && raw.data !== undefined ? raw.data : raw
  const activities = Array.isArray(payload && payload.activities) ? payload.activities : []
  const summary = payload && payload.summary ? payload.summary : {}

  return {
    activities: activities.map((item) => ({
      date: item.date,
      count: normalizeNumber(item.count),
      breakdown: item.breakdown || {}
    })),
    summary: {
      commits: normalizeNumber(summary.commits),
      blogs: normalizeNumber(summary.blogs),
      posts: normalizeNumber(summary.posts),
      likes: normalizeNumber(summary.likes),
      collects: normalizeNumber(summary.collects),
      logs: normalizeNumber(summary.logs)
    }
  }
}

function pushDateBucket(bucket, value, weight = 1) {
  const date = toDateOnly(value)
  if (!date) return
  const key = dateKey(date)
  bucket[key] = (bucket[key] || 0) + weight
}

function buildFallbackActivity({ blogs, posts, likes, collects, logs }, days = 30) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const start = new Date(today)
  start.setDate(start.getDate() - (days - 1))
  start.setHours(0, 0, 0, 0)

  const counters = {}

  blogs.forEach(item => pushDateBucket(counters, item.createTime, 4))
  posts.forEach(item => pushDateBucket(counters, item.createTime, 2))
  likes.forEach(item => pushDateBucket(counters, item.createTime || item.likedAt, 1))
  collects.forEach(item => pushDateBucket(counters, item.createTime || item.collectedAt, 1))
  logs.forEach(item => pushDateBucket(counters, item.createTime || item.operateTime, 1))

  const activities = Object.keys(counters)
    .filter((key) => {
      const date = toDateOnly(key)
      return date && date.getTime() >= start.getTime() && date.getTime() <= today.getTime()
    })
    .sort()
    .map((key) => ({
      date: key,
      count: counters[key],
      breakdown: {}
    }))

  return {
    activities,
    summary: {
      commits: 0,
      blogs: blogs.length,
      posts: posts.length,
      likes: likes.length,
      collects: collects.length,
      logs: logs.length
    }
  }
}

function parseRevenue(response) {
  const raw = unwrapResponse(response)
  if (typeof raw === 'number') return raw
  if (raw && typeof raw.total === 'number') return raw.total
  if (raw && raw.data !== undefined) return normalizeNumber(raw.data)
  return normalizeNumber(raw)
}

function safeResult(result, fallback) {
  return result && result.status === 'fulfilled' ? result.value : fallback
}

async function buildProfileOverview({ userInfo, isSelf }) {
  const profile = normalizeUserProfile(userInfo)
  const userId = profile.id
  if (!userId) {
    throw new Error('用户 ID 不存在，无法加载主页')
  }

  const likesTask = GetLikesReceivedByAuthor(userId).catch(() => GetLikesByUser(userId))
  const collectsTask = GetCollectsReceivedByAuthor(userId).catch(() => GetCollectsByUser(userId))

  const [
    likesRes,
    collectsRes,
    logsRes,
    activityRes,
    blogsRes,
    postsRes,
    knowledgeRes,
    revenueRes
  ] = await Promise.allSettled([
    likesTask,
    collectsTask,
    GetUserLogs(userId),
    GetUserActivityHeatmap(userId, { days: 30 }),
    GetBlogsByAuthorId(userId),
    GetUserCirclePosts(userId),
    isSelf ? GetMyKnowledgeProducts() : Promise.resolve([]),
    isSelf ? GetUserTotalRevenue(userId) : Promise.resolve(null)
  ])

  const likesList = ensureArray(unwrapResponse(safeResult(likesRes, [])))
  const collectsList = ensureArray(unwrapResponse(safeResult(collectsRes, [])))
  const logsList = ensureArray(unwrapResponse(safeResult(logsRes, [])))

  const blogList = mapBlogList(ensureArray(unwrapResponse(safeResult(blogsRes, []))))
  const postList = mapPostList(ensureArray(unwrapResponse(safeResult(postsRes, []))))

  const knowledgeList = isSelf
    ? mapKnowledgeList(ensureArray(unwrapResponse(safeResult(knowledgeRes, []))))
    : mapKnowledgeList(blogList.filter(isKnowledgeCandidate))

  const activityFromApi = parseActivityPayload(safeResult(activityRes, {}))
  const fallbackActivity = buildFallbackActivity({
    blogs: blogList,
    posts: postList,
    likes: likesList,
    collects: collectsList,
    logs: logsList
  })

  const activity = activityFromApi.activities.length ? activityFromApi : fallbackActivity

  return {
    profile,
    stats: {
      totalLikes: likesList.length,
      totalCollects: collectsList.length,
      historyCount: logsList.length,
      totalKnowledge: knowledgeList.length,
      totalRevenue: isSelf ? parseRevenue(safeResult(revenueRes, 0)) : null
    },
    activity,
    blogList,
    postList,
    knowledgeList
  }
}

export async function getMyProfileOverview() {
  const response = await GetCurrentUser()
  const userInfo = unwrapResponse(response)
  return buildProfileOverview({ userInfo, isSelf: true })
}

export async function getUserPublicProfileOverview(userId) {
  if (!userId) {
    throw new Error('缺少用户 ID')
  }
  const response = await GetUserById(userId)
  const userInfo = unwrapResponse(response)
  return buildProfileOverview({ userInfo, isSelf: false })
}
