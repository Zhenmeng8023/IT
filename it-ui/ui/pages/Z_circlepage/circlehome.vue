<template>
  <div data-testid="circle-feed-page" class="circle-home-page">
    <section class="circle-home-hero">
      <div class="hero-main">
        <span class="hero-badge">Circle Hub</span>
        <h1 class="hero-title">开发者圈子</h1>
        <p class="hero-subtitle">
          浏览实时讨论、经验沉淀和项目交流，快速定位值得参与的话题与活跃圈层。
        </p>

        <div class="hero-filter-row">
          <span class="hero-chip hero-chip--accent">最新讨论</span>
          <span v-if="currentCircleName" class="hero-chip">{{ currentCircleName }}</span>
          <span v-if="keyword" class="hero-chip">关键词：{{ keyword }}</span>
          <span v-if="!keyword && !currentCircleName" class="hero-chip">当前展示全部主题</span>
        </div>
      </div>

      <div class="hero-side">
        <div class="hero-stat-card">
          <span class="hero-stat-label">已加载帖子</span>
          <strong class="hero-stat-value">{{ posts.length }}</strong>
          <p>列表已按发布时间排序，优先展示最新内容。</p>
        </div>
        <div class="hero-stat-card">
          <span class="hero-stat-label">圈子数量</span>
          <strong class="hero-stat-value">{{ circles.length }}</strong>
          <p>支持从搜索与入口页筛选目标圈子。</p>
        </div>
        <div class="hero-stat-card">
          <span class="hero-stat-label">筛选状态</span>
          <strong class="hero-stat-value hero-stat-value--text">{{ keyword || currentCircleName || '全部主题' }}</strong>
          <p>卡片信息已统一为同一视觉语言，便于快速扫读。</p>
        </div>
      </div>
    </section>

    <section data-testid="circle-feed-heading" class="list-heading">
      <div>
        <h2>帖子列表</h2>
        <p>统一展示圈子、作者、摘要和互动信息，减少视觉跳跃。</p>
      </div>
      <span data-testid="circle-feed-total" class="list-total">{{ filteredPosts.length }} 篇内容</span>
    </section>

    <div
      data-testid="circle-post-list"
      class="post-list"
      v-infinite-scroll="loadMore"
      infinite-scroll-disabled="scrollDisabled"
      infinite-scroll-distance="10"
      v-loading="loading"
      element-loading-text="加载中..."
    >
      <article
        v-for="post in posts"
        :key="post.id"
        class="post-card-shell"
        :data-post-id="post.id"
        :data-testid="`circle-post-card-${post.id}`"
      >
        <el-card class="post-card" shadow="never" @click.native="goToPostDetail(post)">
          <div class="post-card-top">
            <span :data-testid="`circle-post-circle-${post.id}`" class="post-circle-chip">
              <i class="el-icon-chat-dot-round"></i>
              {{ getCircleNameById(post.circleId) || '未知圈子' }}
            </span>
            <span class="post-time">{{ formatTime(post.createdAt) }}</span>
          </div>

          <h3 :data-testid="`circle-post-title-${post.id}`" class="post-title">{{ resolveTitle(post) }}</h3>

          <p class="post-summary">
            {{ post.summary || post.content || '暂无内容介绍' }}
          </p>

          <div class="post-author-row">
            <div class="post-author">
              <el-avatar :size="36" :src="post.authorAvatar || defaultAvatar"></el-avatar>
              <div class="author-copy">
                <span class="author-name">{{ post.author || '未知用户' }}</span>
                <span class="author-role">活跃讨论者</span>
              </div>
            </div>

            <div class="post-stats">
              <span class="post-stat">
                <i class="el-icon-chat-line-round"></i>
                {{ post.commentCount || 0 }}
              </span>
              <span class="post-stat">
                <i class="el-icon-star-off"></i>
                {{ post.likes || 0 }}
              </span>
            </div>
          </div>

          <div class="post-card-footer">
            <span class="post-footer-text">查看详情与全部回复</span>
            <span class="post-enter-link">
              进入讨论
              <i class="el-icon-right"></i>
            </span>
          </div>
        </el-card>
      </article>

      <div v-if="!hasMore && posts.length > 0" class="list-status">
        没有更多了
      </div>

      <div v-if="!loading && posts.length === 0" data-testid="circle-post-empty" class="list-empty">
        <i class="el-icon-files"></i>
        <span>暂无帖子，快去发帖吧！</span>
      </div>
    </div>
  </div>
</template>

<script>
import { GetAllCirclePosts, GetUserById, GetAllCircles } from '@/api/index.js'
import { pickAvatarUrl } from '@/utils/avatar'

const DEFAULT_AVATAR = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

function unwrapPayload(payload) {
  if (!payload || typeof payload !== 'object') {
    return payload
  }

  if (Array.isArray(payload)) {
    return payload
  }

  if (Array.isArray(payload.data)) {
    return payload.data
  }

  if (payload.data && typeof payload.data === 'object') {
    const nested = payload.data
    if (Array.isArray(nested.data)) return nested.data
    if (Array.isArray(nested.rows)) return nested.rows
    if (Array.isArray(nested.list)) return nested.list
    if (Array.isArray(nested.items)) return nested.items
  }

  if (Array.isArray(payload.rows)) return payload.rows
  if (Array.isArray(payload.list)) return payload.list
  if (Array.isArray(payload.items)) return payload.items

  return payload
}

function normalizeId(value) {
  if (value === null || value === undefined || value === '') {
    return null
  }

  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : null
  }

  const text = String(value).trim()
  if (!text || text === 'null' || text === 'undefined') {
    return null
  }

  return /^\d+$/.test(text) ? Number(text) : text
}

function normalizeText(value, fallback = '') {
  if (value === null || value === undefined) {
    return fallback
  }

  const text = String(value).trim()
  return text || fallback
}

function normalizeNumber(...values) {
  for (const value of values) {
    const number = Number(value)
    if (Number.isFinite(number)) {
      return number
    }
  }

  return 0
}

function buildSummary(content, summary) {
  const normalizedSummary = normalizeText(summary)
  if (normalizedSummary) {
    return normalizedSummary
  }

  const normalizedContent = normalizeText(content)
  if (!normalizedContent) {
    return ''
  }

  return normalizedContent.length > 100
    ? `${normalizedContent.slice(0, 100)}...`
    : normalizedContent
}

export default {
  layout: 'circle',
  data() {
    return {
      page: 1,
      pageSize: 10,
      hasMore: true,
      loading: false,
      posts: [],
      allPosts: [],
      circles: [],
      defaultAvatar: DEFAULT_AVATAR
    }
  },
  computed: {
    keyword() {
      return normalizeText(this.$route.query.keyword)
    },
    circleIdFromRoute() {
      return normalizeId(this.$route.query.circleId)
    },
    currentCircleName() {
      if (this.circleIdFromRoute === null) {
        return ''
      }

      return this.getCircleNameById(this.circleIdFromRoute)
    },
    scrollDisabled() {
      return this.loading || !this.hasMore
    },
    filteredPosts() {
      let filtered = Array.isArray(this.allPosts) ? [...this.allPosts] : []

      if (this.circleIdFromRoute !== null) {
        const targetCircleId = String(this.circleIdFromRoute)
        filtered = filtered.filter(post => String(post.circleId) === targetCircleId)
      }

      if (this.keyword) {
        const keyword = this.keyword.toLowerCase()
        filtered = filtered.filter(post => {
          const fields = [post.title, post.author, post.summary, post.content]
          return fields.some(field => normalizeText(field).toLowerCase().includes(keyword))
        })
      }

      return filtered.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    }
  },
  watch: {
    '$route.query': {
      handler() {
        this.resetAndFetch()
      },
      deep: true,
      immediate: false
    }
  },
  async created() {
    await this.loadAllCircles()
    await this.fetchPosts()
  },
  methods: {
    resolveTitle(post) {
      if (normalizeText(post.title)) {
        return post.title
      }

      const summary = normalizeText(post.summary || post.content, '未命名帖子')
      return summary.length > 30 ? `${summary.slice(0, 30)}...` : summary
    },
    getCircleNameById(circleId) {
      if (!circleId || !this.circles.length) {
        return '未知圈子'
      }

      const target = String(circleId)
      const circle = this.circles.find(item => String(item.id) === target)
      return circle ? circle.name : '未知圈子'
    },
    async getAuthorInfoById(authorId) {
      if (!authorId) {
        return null
      }

      try {
        const payload = await GetUserById(authorId)
        const user = unwrapPayload(payload)
        if (!user || typeof user !== 'object') {
          return null
        }

        return {
          nickname: normalizeText(user.nickname || user.username || user.name, '匿名用户'),
          avatarUrl: pickAvatarUrl(user.avatarUrl, user.avatar, DEFAULT_AVATAR)
        }
      } catch (error) {
        return null
      }
    },
    async enrichAuthors(posts) {
      const authorIds = [...new Set(posts.map(post => normalizeId(post.authorId)).filter(Boolean))]
      if (!authorIds.length) {
        return posts
      }

      const authorEntries = await Promise.all(
        authorIds.map(async authorId => [authorId, await this.getAuthorInfoById(authorId)])
      )
      const authorMap = new Map(authorEntries)

      return posts.map(post => {
        const authorInfo = authorMap.get(normalizeId(post.authorId))
        if (!authorInfo) {
          return post
        }

        return {
          ...post,
          author: authorInfo.nickname || post.author,
          authorAvatar: authorInfo.avatarUrl || post.authorAvatar
        }
      })
    },
    async loadAllCircles() {
      try {
        const payload = await GetAllCircles()
        const circles = unwrapPayload(payload)
        this.circles = Array.isArray(circles) ? circles : []
      } catch (error) {
        this.circles = []
        this.$message.error('获取圈子列表失败')
      }
    },
    goToPostDetail(post) {
      if (!post || !post.id) {
        return
      }

      this.$router.push({
        path: `/circle/${post.id}`,
        query: post.circleId ? { circleId: post.circleId } : {}
      })
    },
    formatTime(time) {
      if (!time) return ''
      const date = new Date(time)
      if (!Number.isFinite(date.getTime())) {
        return ''
      }

      const now = Date.now()
      const diff = Math.floor((now - date.getTime()) / 1000)

      if (diff < 60) return '刚刚'
      if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
      if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
      if (diff < 2592000) return `${Math.floor(diff / 86400)}天前`

      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    normalizePost(post) {
      const authorSource = post.author && typeof post.author === 'object' ? post.author : null
      const circleId = normalizeId(post.circleId || post.circle?.id || post.groupId)
      const authorId = normalizeId(post.authorId || post.userId || post.creatorId || (authorSource && authorSource.id))
      const title = normalizeText(post.title || post.subject, '无标题')
      const content = normalizeText(post.content || post.body)

      return {
        id: normalizeId(post.id || post.postId || post.commentId),
        circleId,
        title,
        content,
        summary: buildSummary(content, post.summary),
        authorId,
        author: normalizeText(
          post.userName ||
            post.creator ||
            post.nickname ||
            (authorSource && (authorSource.nickname || authorSource.username || authorSource.name)),
          '匿名用户'
        ),
        authorAvatar: pickAvatarUrl(
          post.avatarUrl,
          post.avatar,
          post.userAvatar,
          authorSource && authorSource.avatarUrl,
          authorSource && authorSource.avatar,
          DEFAULT_AVATAR
        ),
        createdAt: post.createTime || post.createdAt || post.createDate || new Date().toISOString(),
        likes: normalizeNumber(post.likeCount, post.likes),
        commentCount: normalizeNumber(post.commentCount, post.replyCount)
      }
    },
    async fetchPosts() {
      this.loading = true
      try {
        const payload = await GetAllCirclePosts()
        const posts = unwrapPayload(payload)
        if (!Array.isArray(posts)) {
          this.allPosts = []
          this.posts = []
          this.hasMore = false
          this.$message.error('获取帖子列表失败')
          return
        }

        const normalizedPosts = posts
          .map(post => this.normalizePost(post))
          .filter(post => post.id)
        this.allPosts = await this.enrichAuthors(normalizedPosts)
        this.page = 1
        this.loadPageData(true)
      } catch (error) {
        this.allPosts = []
        this.posts = []
        this.hasMore = false
        this.$message.error('获取帖子列表失败')
      } finally {
        this.loading = false
      }
    },
    loadPageData(reset = false) {
      const start = (this.page - 1) * this.pageSize
      const end = start + this.pageSize
      const pageData = this.filteredPosts.slice(start, end)

      this.posts = reset ? pageData : [...this.posts, ...pageData]
      this.hasMore = end < this.filteredPosts.length
    },
    async resetAndFetch() {
      this.posts = []
      this.page = 1
      this.hasMore = true
      await this.fetchPosts()
    },
    async loadMore() {
      if (this.loading || !this.hasMore) {
        return
      }

      this.page += 1
      this.loadPageData()
    }
  }
}
</script>

<style scoped>
.circle-home-page {
  max-width: 1120px;
  margin: 0 auto;
  padding: 24px var(--it-shell-padding-x) 48px;
  min-height: 100vh;
  color: var(--it-text);
}

.circle-home-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(300px, 1fr);
  gap: 20px;
  margin-bottom: 22px;
}

.hero-main,
.hero-side,
.post-card,
.list-status,
.list-empty {
  border: 1px solid var(--it-border);
  background: var(--it-surface);
  box-shadow: var(--it-shadow);
}

.hero-main {
  position: relative;
  padding: 30px;
  border-radius: 28px;
  overflow: hidden;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--it-accent-soft) 45%, transparent), transparent 56%),
    var(--it-surface);
}

.hero-main::after,
.hero-side::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.08), transparent 40%);
}

.hero-badge,
.hero-chip,
.post-circle-chip,
.list-total {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.hero-badge {
  margin-bottom: 16px;
  background: color-mix(in srgb, var(--it-accent-soft) 88%, transparent);
  color: var(--it-accent);
}

.hero-title {
  margin: 0 0 12px;
  font-size: clamp(2rem, 4vw, 3.1rem);
  line-height: 1.06;
  letter-spacing: -0.03em;
  color: var(--it-text);
}

.hero-subtitle {
  margin: 0;
  max-width: 640px;
  color: var(--it-text-muted);
  font-size: 15px;
  line-height: 1.85;
}

.hero-filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.hero-chip {
  border: 1px solid var(--it-border);
  background: color-mix(in srgb, var(--it-surface-muted) 84%, transparent);
  color: var(--it-text-muted);
}

.hero-chip--accent {
  border-color: transparent;
  background: var(--it-primary-gradient);
  color: #fff;
}

.hero-side {
  position: relative;
  display: grid;
  gap: 12px;
  padding: 20px;
  border-radius: 28px;
  overflow: hidden;
}

.hero-stat-card {
  padding: 18px;
  border-radius: 20px;
  border: 1px solid var(--it-border);
  background: color-mix(in srgb, var(--it-surface-solid) 92%, transparent);
}

.hero-stat-label {
  display: block;
  margin-bottom: 10px;
  font-size: 12px;
  font-weight: 700;
  color: var(--it-text-subtle);
  letter-spacing: 0.04em;
}

.hero-stat-value {
  display: block;
  margin-bottom: 8px;
  font-size: 1.95rem;
  line-height: 1.1;
  color: var(--it-text);
}

.hero-stat-value--text {
  font-size: 1.05rem;
}

.hero-stat-card p {
  margin: 0;
  color: var(--it-text-muted);
  line-height: 1.7;
  font-size: 13px;
}

.list-heading,
.post-card-top,
.post-author-row,
.post-author,
.post-stats,
.post-card-footer {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.list-heading {
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
}

.list-heading h2 {
  margin: 0 0 6px;
  font-size: 20px;
  color: var(--it-text);
}

.list-heading p {
  margin: 0;
  color: var(--it-text-muted);
  font-size: 13px;
}

.list-total {
  border: 1px solid var(--it-border);
  background: color-mix(in srgb, var(--it-surface-muted) 86%, transparent);
  color: var(--it-text-muted);
}

.post-list {
  min-height: 320px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 18px;
}

.post-card-shell {
  min-width: 0;
  cursor: pointer;
}

.post-card {
  height: 100%;
  border-radius: 24px !important;
  border-color: var(--it-border) !important;
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
}

.post-card-shell:hover .post-card {
  transform: translateY(-4px);
  box-shadow: var(--it-shadow-strong) !important;
  border-color: color-mix(in srgb, var(--it-accent) 20%, var(--it-border)) !important;
}

.post-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  padding: 22px;
}

.post-card-top {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.post-circle-chip {
  gap: 6px;
  border: 1px solid color-mix(in srgb, var(--it-accent) 14%, transparent);
  background: color-mix(in srgb, var(--it-accent-soft) 86%, transparent);
  color: var(--it-accent);
}

.post-time {
  font-size: 12px;
  color: var(--it-text-subtle);
  font-weight: 600;
}

.post-title {
  margin: 0 0 14px;
  font-size: 21px;
  line-height: 1.4;
  color: var(--it-text);
}

.post-summary {
  margin: 0 0 18px;
  color: var(--it-text-muted);
  line-height: 1.8;
  font-size: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 100px;
}

.post-author-row {
  justify-content: space-between;
  gap: 14px;
  margin-top: auto;
}

.post-author {
  gap: 12px;
  min-width: 0;
}

.author-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--it-text);
}

.author-role {
  font-size: 12px;
  color: var(--it-text-subtle);
}

.post-stats {
  gap: 10px;
}

.post-stat {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--it-surface-muted) 86%, transparent);
  color: var(--it-text-muted);
  font-size: 12px;
  font-weight: 700;
}

.post-card-footer {
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--it-border);
}

.post-footer-text {
  color: var(--it-text-subtle);
  font-size: 13px;
}

.post-enter-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--it-accent);
  font-size: 13px;
  font-weight: 700;
}

.list-status,
.list-empty {
  grid-column: 1 / -1;
  border-radius: 22px;
  text-align: center;
}

.list-status {
  padding: 26px 20px;
  color: var(--it-text-muted);
}

.list-empty {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 220px;
  color: var(--it-text-muted);
}

.list-empty i {
  font-size: 30px;
  color: var(--it-accent);
}

@media screen and (max-width: 1024px) {
  .circle-home-hero {
    grid-template-columns: 1fr;
  }

  .hero-side {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media screen and (max-width: 768px) {
  .circle-home-page {
    padding: 16px 12px 40px;
  }

  .hero-main,
  .hero-side {
    padding: 18px;
    border-radius: 22px;
  }

  .hero-side {
    grid-template-columns: 1fr;
  }

  .post-list {
    grid-template-columns: 1fr;
  }

  .list-heading,
  .post-author-row,
  .post-card-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .post-summary {
    min-height: auto;
  }
}

@media screen and (max-width: 480px) {
  .hero-title {
    font-size: 1.95rem;
  }

  .post-title {
    font-size: 18px;
  }

  .post-card :deep(.el-card__body) {
    padding: 18px;
  }

  .post-card-top {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
