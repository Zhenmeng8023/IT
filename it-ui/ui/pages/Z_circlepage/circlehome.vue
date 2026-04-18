<template>
  <div class="circle-container">
    <section class="circle-hero">
      <div class="hero-copy">
        <span class="hero-badge">Circle Hub</span>
        <h1 class="hero-title">开发者圈子</h1>
        <p class="hero-subtitle">浏览实时讨论、经验沉淀和项目交流，让每次进入圈子都能快速找到值得参与的话题。</p>
      </div>
      <div class="hero-panel">
        <div class="hero-panel-item">
          <span class="hero-panel-label">已加载帖子</span>
          <strong class="hero-panel-value">{{ posts.length }}</strong>
        </div>
        <div class="hero-panel-item">
          <span class="hero-panel-label">圈子数量</span>
          <strong class="hero-panel-value">{{ circles.length }}</strong>
        </div>
        <div class="hero-panel-item">
          <span class="hero-panel-label">当前搜索</span>
          <strong class="hero-panel-value hero-panel-text">{{ keyword || '全部主题' }}</strong>
        </div>
      </div>
    </section>

    <!-- 帖子列表，使用 Element UI 的无限滚动指令 -->
    <div
      class="post-list"
      v-infinite-scroll="loadMore"
      infinite-scroll-disabled="scrollDisabled"
      infinite-scroll-distance="10"
      v-loading="loading"
      element-loading-text="加载中..."
    >
      <el-card
        v-for="post in posts"
        :key="post.id"
        class="post-card"
        shadow="hover"
        @click.native="goToPostDetail(post)"
      >
        <div class="post-topline">
          <div class="circle-name">
            <i class="el-icon-chat-dot-round"></i> {{ getCircleNameById(post.circleId) || '未知圈子' }}
          </div>
          <span class="post-time">{{ formatTime(post.createdAt) }}</span>
        </div>

        <h3 v-if="post.title" class="post-title">{{ post.title }}</h3>
        
        <!-- 作者信息 -->
        <div class="post-author">
          <el-avatar :size="32" :src="post.authorAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"></el-avatar>
          <div class="author-meta">
            <span class="author-name">{{ post.author || '未知用户' }}</span>
            <span class="author-role">活跃讨论者</span>
          </div>
        </div>
        
        <!-- 帖子内容摘要 -->
        <div class="post-content">
          {{ post.summary || post.content || '暂无内容介绍' }}
        </div>
        
        <!-- 帖子底部信息：评论数、点赞数 -->
        <div class="post-footer">
          <div class="post-footer-main">
            <span class="post-stat">
              <i class="el-icon-chat-line-round"></i> {{ post.commentCount || 0 }}
            </span>
            <span class="post-stat">
              <i class="el-icon-star-off"></i> {{ post.likes || 0 }}
            </span>
          </div>
          <span class="post-enter">
            进入讨论
            <i class="el-icon-right"></i>
          </span>
        </div>
      </el-card>

      <!-- 没有更多数据时的提示 -->
      <div v-if="!hasMore && posts.length > 0" class="no-more">
        没有更多了
      </div>
      
      <!-- 没有帖子时的提示 -->
      <div v-if="!loading && posts.length === 0" class="no-posts">
        暂无帖子，快去发帖吧！
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
      currentCircleId: null
    }
  },

  computed: {
    keyword() {
      return normalizeText(this.$route.query.keyword)
    },
    circleIdFromRoute() {
      return normalizeId(this.$route.query.circleId)
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
    getCircleNameById(circleId) {
      if (!circleId || !this.circles.length) {
        return '\u672a\u77e5\u5708\u5b50'
      }

      const target = String(circleId)
      const circle = this.circles.find(item => String(item.id) === target)
      return circle ? circle.name : '\u672a\u77e5\u5708\u5b50'
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
          nickname: normalizeText(user.nickname || user.username || user.name, '\u533f\u540d\u7528\u6237'),
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
        if (!this.currentCircleId && this.circles.length) {
          this.currentCircleId = normalizeId(this.circles[0].id)
        }
      } catch (error) {
        this.circles = []
        this.$message.error('\u83b7\u53d6\u5708\u5b50\u5217\u8868\u5931\u8d25')
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

      if (diff < 60) return '\u521a\u521a'
      if (diff < 3600) return `${Math.floor(diff / 60)}\u5206\u949f\u524d`
      if (diff < 86400) return `${Math.floor(diff / 3600)}\u5c0f\u65f6\u524d`
      if (diff < 2592000) return `${Math.floor(diff / 86400)}\u5929\u524d`

      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    normalizePost(post) {
      const authorSource = post.author && typeof post.author === 'object' ? post.author : null
      const circleId = normalizeId(post.circleId || post.circle?.id || post.groupId)
      const authorId = normalizeId(post.authorId || post.userId || post.creatorId || (authorSource && authorSource.id))
      const title = normalizeText(post.title || post.subject, '\u65e0\u6807\u9898')
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
          '\u533f\u540d\u7528\u6237'
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
          this.$message.error('\u83b7\u53d6\u5e16\u5b50\u5217\u8868\u5931\u8d25')
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
        this.$message.error('\u83b7\u53d6\u5e16\u5b50\u5217\u8868\u5931\u8d25')
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
.circle-container {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 20px 40px;
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.12), transparent 30%),
    radial-gradient(circle at bottom right, rgba(59, 130, 246, 0.12), transparent 28%),
    linear-gradient(180deg, #f7fbff 0%, #eef5ff 100%);
  min-height: 100vh;
}

.circle-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(280px, 0.95fr);
  gap: 22px;
  margin-bottom: 26px;
}

.hero-copy,
.hero-panel,
.post-card,
.no-more,
.no-posts {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(14px);
}

.hero-copy {
  padding: 32px;
  border-radius: 28px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 7px 14px;
  border-radius: 999px;
  background: rgba(2, 132, 199, 0.08);
  color: #0284c7;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 14px;
}

.hero-title {
  margin: 0 0 12px;
  font-size: clamp(2rem, 4vw, 3.1rem);
  line-height: 1.08;
  color: #0f172a;
}

.hero-subtitle {
  margin: 0;
  max-width: 620px;
  color: #475569;
  font-size: 15px;
  line-height: 1.8;
}

.hero-panel {
  border-radius: 28px;
  padding: 20px;
  display: grid;
  gap: 14px;
}

.hero-panel-item {
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.hero-panel-label {
  display: block;
  margin-bottom: 10px;
  font-size: 12px;
  color: #64748b;
}

.hero-panel-value {
  display: block;
  font-size: 1.8rem;
  line-height: 1.1;
  color: #0f172a;
}

.hero-panel-text {
  font-size: 1.05rem;
  word-break: break-word;
}

.post-list {
  min-height: 400px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.post-card {
  cursor: pointer;
  transition: transform 0.28s ease, box-shadow 0.28s ease, border-color 0.28s ease;
  border-radius: 24px;
  overflow: hidden;
}

.post-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 24px 45px rgba(15, 23, 42, 0.12);
  border-color: rgba(2, 132, 199, 0.22);
}

.post-topline,
.post-author,
.post-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.post-topline {
  margin-bottom: 14px;
}

.circle-name {
  font-size: 13px;
  color: #0284c7;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  background: #e0f2fe;
  font-weight: 700;
}

.circle-name i {
  font-size: 14px;
}

.post-time {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 600;
}

.post-title {
  font-size: 21px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 14px;
  line-height: 1.45;
}

.post-author {
  justify-content: flex-start;
  margin-bottom: 16px;
}

.author-meta {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.author-name {
  font-size: 14px;
  font-weight: 700;
  color: #334155;
}

.author-role {
  font-size: 12px;
  color: #94a3b8;
}

.post-content {
  font-size: 14px;
  line-height: 1.8;
  color: #475569;
  margin-bottom: 18px;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  min-height: 100px;
}

.post-footer {
  padding-top: 14px;
  border-top: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 14px;
  margin-top: auto;
}

.post-footer-main {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.post-stat {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  transition: color 0.2s ease;
}

.post-stat:hover,
.post-enter {
  color: #0284c7;
}

.post-stat i,
.post-enter i {
  font-size: 15px;
}

.post-enter {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
}

.no-more,
.no-posts {
  grid-column: 1 / -1;
  text-align: center;
  padding: 28px 20px;
  color: #64748b;
  font-size: 14px;
  border-radius: 22px;
}

.no-posts {
  padding: 64px 20px;
  font-size: 16px;
}

@media screen and (max-width: 1024px) {
  .circle-hero {
    grid-template-columns: 1fr;
  }

  .hero-panel {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media screen and (max-width: 768px) {
  .circle-container {
    padding: 18px 12px 32px;
  }

  .hero-copy {
    padding: 24px 20px;
  }

  .hero-panel {
    grid-template-columns: 1fr;
  }

  .post-list {
    grid-template-columns: 1fr;
  }

  .post-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media screen and (max-width: 480px) {
  .hero-title {
    font-size: 1.9rem;
  }

  .post-topline {
    flex-direction: column;
    align-items: flex-start;
  }

  .post-title {
    font-size: 18px;
  }

  .post-content {
    min-height: auto;
  }
}
</style>

