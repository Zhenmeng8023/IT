<template>
  <div data-testid="circle-feed-page" class="circle-home-page">
    <front-hero-panel
      class="circle-home-hero"
      compact
      badge="Circle Hub"
      title="开发者圈子社区广场"
      :subtitle="heroSubtitle"
      :stats="heroStats"
      :stats-columns="3"
    >
      <div class="hero-filter-row">
        <span class="hero-chip hero-chip--accent">讨论流</span>
        <span v-if="currentCircleName" class="hero-chip">圈子：{{ currentCircleName }}</span>
        <span v-if="keyword" class="hero-chip">关键词：{{ keyword }}</span>
        <span v-if="!keyword && !currentCircleName" class="hero-chip">当前展示全部帖子</span>
      </div>
    </front-hero-panel>

    <section class="hot-circle-strip">
      <header class="section-header">
        <h2>热门圈子</h2>
        <button type="button" class="text-link" @click="updateCircleFilter(null)">查看全部</button>
      </header>
      <div class="hot-circle-scroll">
        <button
          v-for="circle in hotCircles.slice(0, 10)"
          :key="`hot-circle-${circle.id}`"
          type="button"
          class="hot-circle-item"
          :class="{ 'is-active': String(circleIdFromRoute) === String(circle.id) }"
          @click="updateCircleFilter(circle.id)"
        >
          <strong>{{ circle.name }}</strong>
          <span>{{ circle.postCount }} 帖</span>
        </button>
      </div>
    </section>

    <front-page-shell
      class="circle-forum-shell"
      layout="three"
      :max-width="'100%'"
      :left-width="240"
      :right-width="300"
      :gap="12"
      :padding-top="0"
      :padding-bottom="0"
      :show-background="false"
      content-class="circle-forum-main"
    >
      <template #left>
        <aside class="forum-left-nav">
          <section class="left-panel">
            <h3>我的圈子</h3>
            <ul v-if="myCircles.length" class="circle-list">
              <li v-for="circle in myCircles" :key="`my-${circle.id}`">
                <button type="button" @click="updateCircleFilter(circle.id)">
                  <span>{{ circle.name }}</span>
                  <em>{{ circle.postCount }} 帖</em>
                </button>
              </li>
            </ul>
            <p v-else class="left-empty">暂无数据</p>
          </section>

          <section class="left-panel">
            <h3>推荐圈子</h3>
            <ul v-if="recommendedCircles.length" class="circle-list">
              <li v-for="circle in recommendedCircles" :key="`rec-${circle.id}`">
                <button type="button" @click="updateCircleFilter(circle.id)">
                  <span>{{ circle.name }}</span>
                  <em>{{ circle.postCount }} 帖</em>
                </button>
              </li>
            </ul>
            <p v-else class="left-empty">暂无推荐</p>
          </section>

          <section class="left-panel">
            <h3>分类入口</h3>
            <ul v-if="categoryEntries.length" class="category-list">
              <li v-for="entry in categoryEntries" :key="`cat-${entry.name}`">
                <button type="button" @click="applyCategory(entry.name)">
                  <span>{{ entry.name }}</span>
                  <em>{{ entry.count }}</em>
                </button>
              </li>
            </ul>
            <p v-else class="left-empty">暂无分类</p>
          </section>
        </aside>
      </template>

      <section class="forum-stream">
        <section data-testid="circle-feed-heading" class="stream-heading">
          <div>
            <h2>社区帖子流</h2>
            <p>按置顶、精华、普通层级组织，支持最新/热门/精华/零回复视图切换。</p>
          </div>
          <span data-testid="circle-feed-total" class="stream-total">{{ orderedPosts.length }} 帖</span>
        </section>

        <front-feed-toolbar
          class="stream-toolbar"
          :sort-options="sortOptions"
          :sort-value="sortView"
          sort-label="排序"
          :filters="activeFilters"
          filter-label="当前筛选"
          clear-text="清空"
          @sort-change="handleSortChange"
          @clear-filters="clearFilters"
          @remove-filter="handleFilterRemove"
        >
          <template #leading>
            <span class="toolbar-summary">
              当前视图：{{ currentSortLabel }}，已加载 {{ posts.length }} / {{ orderedPosts.length }} 帖
            </span>
          </template>
        </front-feed-toolbar>

        <div
          data-testid="circle-post-list"
          class="post-list"
          v-infinite-scroll="loadMore"
          :infinite-scroll-disabled="scrollDisabled"
          :infinite-scroll-distance="10"
          v-loading="loading"
          element-loading-text="正在加载帖子..."
        >
          <article
            v-for="post in posts"
            :key="post.id"
            class="forum-post-shell"
            :data-post-id="post.id"
            :data-testid="`circle-post-card-${post.id}`"
          >
            <el-card class="forum-post-card" shadow="hover" @click.native="goToPostDetail(post)">
              <div class="forum-post-card__head">
                <span :data-testid="`circle-post-circle-${post.id}`" class="forum-circle-chip">
                  {{ getCircleNameById(post.circleId) }}
                </span>
                <div class="forum-post-card__badges">
                  <el-tag v-if="post.isPinned" type="danger" size="mini">置顶</el-tag>
                  <el-tag v-else-if="post.isFeatured" type="warning" size="mini">精华</el-tag>
                  <span class="forum-post-card__update-time" :title="formatDateTime(post.updatedAt || post.createdAt)">
                    更新于 {{ formatTime(post.updatedAt || post.createdAt) }}
                  </span>
                </div>
              </div>

              <h3 :data-testid="`circle-post-title-${post.id}`" class="forum-post-card__title">
                {{ resolveTitle(post) }}
              </h3>

              <p class="forum-post-card__summary">
                {{ post.summary || '暂无摘要' }}
              </p>

              <div class="forum-post-card__meta">
                <div class="forum-post-card__author">
                  <el-avatar :size="30" :src="post.authorAvatar || defaultAvatar"></el-avatar>
                  <div class="forum-post-card__author-copy">
                    <strong>{{ post.author || '匿名用户' }}</strong>
                    <span>发布于 {{ formatDateTime(post.createdAt) }}</span>
                  </div>
                </div>
                <div class="forum-post-card__stats">
                  <span><i class="el-icon-chat-line-round"></i>{{ post.commentCount || 0 }} 回复</span>
                  <span><i class="el-icon-star-off"></i>{{ post.likes || 0 }} 点赞</span>
                  <span class="forum-post-card__action">进入讨论</span>
                </div>
              </div>
            </el-card>
          </article>

          <div v-if="!hasMore && posts.length > 0" class="list-status">没有更多帖子了</div>

          <front-empty-state
            v-if="!loading && posts.length === 0"
            data-testid="circle-post-empty"
            icon="el-icon-document-delete"
            title="暂无帖子，快去发帖吧！"
            description="当前筛选条件下还没有可展示的帖子，试试切换圈子或关键词。"
            size="lg"
          />
        </div>
      </section>

      <template #right>
        <front-right-rail
          title="社区信息"
          subtitle="公告、成员、热门话题与发帖规范"
          data-testid="circle-right-rail"
        >
          <section class="front-right-rail__section rail-section">
            <h4>圈子公告</h4>
            <p>{{ communityAnnouncement }}</p>
          </section>

          <section class="front-right-rail__section rail-section">
            <h4>活跃成员</h4>
            <ul v-if="activeMembers.length" class="member-list">
              <li v-for="member in activeMembers" :key="member.key">
                <el-avatar :size="26" :src="member.avatar || defaultAvatar"></el-avatar>
                <span class="member-main">
                  <strong>{{ member.name }}</strong>
                  <em>{{ member.postCount }} 帖 · {{ member.replyCount }} 回复</em>
                </span>
              </li>
            </ul>
            <p v-else class="rail-empty">暂无活跃成员数据</p>
          </section>

          <front-tag-cloud
            title="热门话题"
            :tags="activeTopics"
            :show-count="true"
            :multiple="false"
            empty-text="暂无热门话题"
            compact
          />

          <section class="front-right-rail__section rail-section">
            <h4>发帖规则</h4>
            <ul class="rule-list">
              <li v-for="rule in postingRules" :key="rule">{{ rule }}</li>
            </ul>
          </section>
        </front-right-rail>
      </template>
    </front-page-shell>
  </div>
</template>

<script>
import { GetAllCirclePosts, GetAllCircles } from '@/api/circle'
import { GetUserById } from '@/api/index.js'
import { pickAvatarUrl } from '@/utils/avatar'
import {
  FrontEmptyState,
  FrontFeedToolbar,
  FrontHeroPanel,
  FrontPageShell,
  FrontRightRail,
  FrontTagCloud
} from '@/components/front'

const DEFAULT_AVATAR = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

function unwrapPayload(payload) {
  if (!payload || typeof payload !== 'object') return payload
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload.data)) return payload.data
  if (payload.data && typeof payload.data === 'object') {
    if (Array.isArray(payload.data.data)) return payload.data.data
    if (Array.isArray(payload.data.rows)) return payload.data.rows
    if (Array.isArray(payload.data.list)) return payload.data.list
    if (Array.isArray(payload.data.items)) return payload.data.items
  }
  if (Array.isArray(payload.rows)) return payload.rows
  if (Array.isArray(payload.list)) return payload.list
  if (Array.isArray(payload.items)) return payload.items
  return payload
}

function normalizeId(value) {
  if (value === null || value === undefined || value === '') return null
  if (typeof value === 'number') return Number.isFinite(value) ? value : null
  const text = String(value).trim()
  if (!text || text === 'null' || text === 'undefined') return null
  return /^\d+$/.test(text) ? Number(text) : text
}

function normalizeText(value, fallback = '') {
  if (value === null || value === undefined) return fallback
  const text = String(value).trim()
  return text || fallback
}

function normalizeNumber(...values) {
  for (const value of values) {
    const number = Number(value)
    if (Number.isFinite(number)) return number
  }
  return 0
}

function toTimestamp(value) {
  if (!value) return 0
  const time = new Date(value).getTime()
  return Number.isFinite(time) ? time : 0
}

function buildSummary(content, summary) {
  const normalizedSummary = normalizeText(summary)
  if (normalizedSummary) return normalizedSummary
  const normalizedContent = normalizeText(content)
  if (!normalizedContent) return ''
  return normalizedContent.length > 120 ? `${normalizedContent.slice(0, 120)}...` : normalizedContent
}

function normalizeBoolean(...values) {
  for (const value of values) {
    if (typeof value === 'boolean') return value
    if (typeof value === 'number') return value > 0
    const text = normalizeText(value).toLowerCase()
    if (!text) continue
    if (['1', 'true', 'yes', 'y', 'top', 'pinned', 'sticky'].includes(text)) return true
    if (['0', 'false', 'no', 'n'].includes(text)) return false
  }
  return false
}

function mergePostLists(...lists) {
  const result = []
  const visited = new Set()
  lists.forEach(list => {
    ;(list || []).forEach(post => {
      const id = String(post && post.id)
      if (!id || visited.has(id)) return
      visited.add(id)
      result.push(post)
    })
  })
  return result
}

function sortByTimeDesc(left, right) {
  return toTimestamp(right.updatedAt || right.createdAt) - toTimestamp(left.updatedAt || left.createdAt)
}

export default {
  layout: 'circle',
  components: {
    FrontEmptyState,
    FrontFeedToolbar,
    FrontHeroPanel,
    FrontPageShell,
    FrontRightRail,
    FrontTagCloud
  },
  data() {
    return {
      page: 1,
      pageSize: 10,
      hasMore: true,
      loading: false,
      posts: [],
      allPosts: [],
      circles: [],
      sortView: 'latest',
      defaultAvatar: DEFAULT_AVATAR,
      sortOptions: [
        { id: 'latest', value: 'latest', label: '最新', icon: 'el-icon-time' },
        { id: 'hot', value: 'hot', label: '热门', icon: 'el-icon-fire' },
        { id: 'featured', value: 'featured', label: '精华', icon: 'el-icon-medal' },
        { id: 'zero_reply', value: 'zero_reply', label: '零回复', icon: 'el-icon-chat-dot-round' }
      ]
    }
  },
  computed: {
    keyword() {
      return normalizeText(this.$route.query.keyword)
    },
    circleIdFromRoute() {
      return normalizeId(this.$route.query.circleId)
    },
    circleNameMap() {
      const map = new Map()
      this.circles.forEach(circle => {
        const id = normalizeId(circle.id)
        if (id !== null) {
          map.set(String(id), normalizeText(circle.name, '未命名圈子'))
        }
      })
      return map
    },
    currentCircleName() {
      if (this.circleIdFromRoute === null) return ''
      return this.getCircleNameById(this.circleIdFromRoute)
    },
    scrollDisabled() {
      return this.loading || !this.hasMore
    },
    currentSortLabel() {
      const current = this.sortOptions.find(option => option.value === this.sortView)
      return current ? current.label : '最新'
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

      return filtered
    },
    hotCircles() {
      const metrics = new Map()
      this.allPosts.forEach(post => {
        const circleId = normalizeId(post.circleId)
        if (circleId === null) return
        const key = String(circleId)
        const current = metrics.get(key) || { postCount: 0, score: 0 }
        current.postCount += 1
        current.score += this.getHotScore(post)
        metrics.set(key, current)
      })

      const circles = this.circles.map(circle => {
        const id = normalizeId(circle.id)
        const key = String(id)
        const metric = metrics.get(key) || { postCount: 0, score: 0 }
        return {
          id: id === null ? key : id,
          name: normalizeText(circle.name, '未命名圈子'),
          category: normalizeText(circle.type || circle.category || circle.classify, '综合'),
          postCount: normalizeNumber(circle.postCount, metric.postCount),
          score: metric.score
        }
      })

      metrics.forEach((metric, key) => {
        if (circles.some(circle => String(circle.id) === key)) return
        circles.push({
          id: key,
          name: this.getCircleNameById(key),
          category: '综合',
          postCount: metric.postCount,
          score: metric.score
        })
      })

      return circles
        .sort((a, b) => b.score - a.score || b.postCount - a.postCount || a.name.localeCompare(b.name))
        .slice(0, 20)
    },
    pinnedPosts() {
      return this.filteredPosts.filter(post => post.isPinned).sort(sortByTimeDesc)
    },
    featuredPosts() {
      return this.filteredPosts
        .filter(post => !post.isPinned && post.isFeatured)
        .sort((a, b) => this.getHotScore(b) - this.getHotScore(a) || sortByTimeDesc(a, b))
    },
    hotPosts() {
      return [...this.filteredPosts]
        .sort((a, b) => this.getHotScore(b) - this.getHotScore(a) || sortByTimeDesc(a, b))
    },
    zeroReplyPosts() {
      return this.filteredPosts.filter(post => normalizeNumber(post.commentCount) === 0).sort(sortByTimeDesc)
    },
    normalPosts() {
      return this.filteredPosts.filter(post => !post.isPinned && !post.isFeatured).sort(sortByTimeDesc)
    },
    orderedPosts() {
      if (this.sortView === 'hot') return mergePostLists(this.pinnedPosts, this.hotPosts)
      if (this.sortView === 'featured') return mergePostLists(this.pinnedPosts, this.featuredPosts)
      if (this.sortView === 'zero_reply') return this.zeroReplyPosts
      return mergePostLists(this.pinnedPosts, this.featuredPosts, this.normalPosts)
    },
    activeTopics() {
      const counter = new Map()
      this.filteredPosts.forEach(post => {
        const tags = []
        const hashTags = normalizeText(post.title).match(/#[\u4e00-\u9fa5A-Za-z0-9_-]{2,20}/g) || []
        hashTags.forEach(item => tags.push(item.replace('#', '')))
        if (Array.isArray(post.tags)) tags.push(...post.tags)
        tags.push(post.topic)
        tags.push(this.getCircleNameById(post.circleId))

        tags.forEach(tag => {
          const name = normalizeText(tag)
          if (!name || name === '未知圈子' || name.length > 20) return
          counter.set(name, (counter.get(name) || 0) + 1)
        })
      })

      return Array.from(counter.entries())
        .map(([name, count]) => ({ id: name, label: name, value: name, count }))
        .sort((a, b) => b.count - a.count || a.label.localeCompare(b.label))
        .slice(0, 18)
    },
    myCircles() {
      return this.hotCircles.filter(item => item.postCount >= 2).slice(0, 6)
    },
    recommendedCircles() {
      const myIds = new Set(this.myCircles.map(item => String(item.id)))
      return this.hotCircles.filter(item => !myIds.has(String(item.id))).slice(0, 6)
    },
    categoryEntries() {
      const counter = new Map()
      this.hotCircles.forEach(circle => {
        const key = normalizeText(circle.category, '综合')
        counter.set(key, (counter.get(key) || 0) + 1)
      })
      return Array.from(counter.entries())
        .map(([name, count]) => ({ name, count }))
        .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name))
        .slice(0, 8)
    },
    heroSubtitle() {
      return '面向真实讨论场景的社区流：左侧导航圈子入口，中间按层级展示帖子，右侧查看公告与话题。'
    },
    heroStats() {
      return [
        { id: 'post-count', label: '当前帖子', value: this.orderedPosts.length, hint: '按当前筛选和排序视图统计' },
        { id: 'circle-count', label: '圈子总数', value: this.circles.length, hint: '支持按圈子快速切换' },
        { id: 'topic-count', label: '活跃话题', value: this.activeTopics.length, hint: '来自帖子标题和圈子热度' }
      ]
    },
    activeFilters() {
      const filters = []
      if (this.circleIdFromRoute !== null) {
        filters.push({
          id: 'circleId',
          key: 'circleId',
          text: `圈子：${this.currentCircleName || this.circleIdFromRoute}`,
          value: this.circleIdFromRoute
        })
      }
      if (this.keyword) {
        filters.push({ id: 'keyword', key: 'keyword', text: `关键词：${this.keyword}`, value: this.keyword })
      }
      return filters
    },
    communityAnnouncement() {
      return '请遵守社区规范，友好交流、禁止广告刷屏与攻击性言论。'
    },
    activeMembers() {
      const map = new Map()
      this.filteredPosts.forEach(post => {
        const key = post.authorId !== null ? String(post.authorId) : normalizeText(post.author)
        if (!key) return
        const current = map.get(key) || {
          key,
          name: normalizeText(post.author, '匿名用户'),
          avatar: post.authorAvatar || DEFAULT_AVATAR,
          postCount: 0,
          replyCount: 0,
          likes: 0,
          score: 0
        }
        current.postCount += 1
        current.replyCount += normalizeNumber(post.commentCount)
        current.likes += normalizeNumber(post.likes)
        current.score = current.postCount * 3 + current.replyCount * 2 + current.likes
        map.set(key, current)
      })
      return Array.from(map.values())
        .sort((a, b) => b.score - a.score || b.postCount - a.postCount)
        .slice(0, 8)
    },
    postingRules() {
      return [
        '标题准确描述问题或观点，避免空泛标题。',
        '技术讨论请提供上下文与复现信息，便于他人回复。',
        '禁止广告、引流和人身攻击，违规则会被处理。'
      ]
    }
  },
  watch: {
    '$route.query': {
      handler() {
        this.resetVisiblePosts()
      },
      deep: true
    },
    sortView() {
      this.resetVisiblePosts()
    }
  },
  async created() {
    await this.loadAllCircles()
    await this.fetchPosts()
  },
  methods: {
    resolveTitle(post) {
      if (normalizeText(post.title)) return post.title
      const fallback = normalizeText(post.summary || post.content, '未命名帖子')
      return fallback.length > 36 ? `${fallback.slice(0, 36)}...` : fallback
    },
    getCircleNameById(circleId) {
      if (circleId === null || circleId === undefined || !this.circleNameMap.size) return '未知圈子'
      return this.circleNameMap.get(String(circleId)) || '未知圈子'
    },
    getHotScore(post) {
      const likes = normalizeNumber(post.likes)
      const replies = normalizeNumber(post.commentCount)
      const ageHours = Math.max(1, (Date.now() - toTimestamp(post.updatedAt || post.createdAt)) / 3600000)
      const recencyBonus = Math.max(0, 96 - ageHours)
      return replies * 6 + likes * 4 + recencyBonus
    },
    async getAuthorInfoById(authorId) {
      if (!authorId) return null
      try {
        const payload = await GetUserById(authorId)
        const user = unwrapPayload(payload)
        if (!user || typeof user !== 'object') return null
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
      if (!authorIds.length) return posts
      const authorEntries = await Promise.all(
        authorIds.map(async authorId => [authorId, await this.getAuthorInfoById(authorId)])
      )
      const authorMap = new Map(authorEntries)
      return posts.map(post => {
        const authorInfo = authorMap.get(normalizeId(post.authorId))
        if (!authorInfo) return post
        return {
          ...post,
          author: authorInfo.nickname || post.author,
          authorAvatar: authorInfo.avatarUrl || post.authorAvatar
        }
      })
    },
    normalizePost(post) {
      const authorSource = post.author && typeof post.author === 'object' ? post.author : null
      const circleId = normalizeId(post.circleId || (post.circle && post.circle.id) || post.groupId)
      const authorId = normalizeId(post.authorId || post.userId || post.creatorId || (authorSource && authorSource.id))
      const title = normalizeText(post.title || post.subject, '无标题')
      const content = normalizeText(post.content || post.body)
      const createdAt = post.createTime || post.createdAt || post.createDate || new Date().toISOString()
      const updatedAt = post.updateTime || post.updatedAt || post.lastReplyTime || createdAt

      return {
        id: normalizeId(post.id || post.postId || post.commentId),
        circleId,
        title,
        content,
        summary: buildSummary(content, post.summary),
        topic: normalizeText(post.topic || post.subject),
        tags: Array.isArray(post.tags) ? post.tags : (Array.isArray(post.tagList) ? post.tagList : []),
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
        createdAt,
        updatedAt,
        likes: normalizeNumber(post.likeCount, post.likes),
        commentCount: normalizeNumber(post.commentCount, post.replyCount),
        isPinned: normalizeBoolean(post.isPinned, post.pinned, post.isTop, post.top, post.sticky),
        isFeatured: normalizeBoolean(post.isEssence, post.essence, post.isFeatured, post.featured, post.recommend)
      }
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
    async fetchPosts() {
      this.loading = true
      try {
        const payload = await GetAllCirclePosts()
        const rows = unwrapPayload(payload)
        if (!Array.isArray(rows)) {
          this.allPosts = []
          this.posts = []
          this.hasMore = false
          this.$message.error('获取帖子列表失败')
          return
        }
        const normalizedPosts = rows
          .map(post => this.normalizePost(post))
          .filter(post => post.id !== null && post.id !== undefined)
        this.allPosts = await this.enrichAuthors(normalizedPosts)
        this.resetVisiblePosts()
      } catch (error) {
        this.allPosts = []
        this.posts = []
        this.hasMore = false
        this.$message.error('获取帖子列表失败')
      } finally {
        this.loading = false
      }
    },
    pushRouteQuery(query) {
      const nextQuery = { ...query }
      Object.keys(nextQuery).forEach(key => {
        if (nextQuery[key] === undefined || nextQuery[key] === null || nextQuery[key] === '') {
          delete nextQuery[key]
        }
      })
      this.$router.push({ path: this.$route.path, query: nextQuery }).catch(error => {
        if (!error || error.name !== 'NavigationDuplicated') {
          console.error('更新筛选失败:', error)
        }
      })
    },
    updateCircleFilter(circleId) {
      const nextQuery = { ...this.$route.query }
      if (circleId === null || circleId === undefined || circleId === '') {
        delete nextQuery.circleId
      } else {
        nextQuery.circleId = circleId
      }
      this.pushRouteQuery(nextQuery)
    },
    applyCategory(categoryName) {
      const target = this.hotCircles.find(item => item.category === categoryName)
      if (!target) {
        this.$message.info('该分类下暂无可选圈子')
        return
      }
      this.updateCircleFilter(target.id)
    },
    clearFilters() {
      const nextQuery = { ...this.$route.query }
      delete nextQuery.circleId
      delete nextQuery.keyword
      this.pushRouteQuery(nextQuery)
    },
    handleFilterRemove(payload) {
      const key = payload && payload.item ? payload.item.key : ''
      if (!key) return
      const nextQuery = { ...this.$route.query }
      delete nextQuery[key]
      this.pushRouteQuery(nextQuery)
    },
    handleSortChange(value) {
      this.sortView = value || 'latest'
    },
    goToPostDetail(post) {
      if (!post || !post.id) return
      this.$router.push({
        path: `/circle/${post.id}`,
        query: post.circleId ? { circleId: post.circleId } : {}
      })
    },
    formatTime(time) {
      if (!time) return ''
      const date = new Date(time)
      if (!Number.isFinite(date.getTime())) return ''

      const diff = Math.floor((Date.now() - date.getTime()) / 1000)
      if (diff < 60) return '刚刚'
      if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
      if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
      if (diff < 2592000) return `${Math.floor(diff / 86400)} 天前`

      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    formatDateTime(time) {
      if (!time) return '未知时间'
      const date = new Date(time)
      if (!Number.isFinite(date.getTime())) return '未知时间'
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}`
    },
    loadPageData(reset = false) {
      const source = this.orderedPosts
      const start = (this.page - 1) * this.pageSize
      const end = start + this.pageSize
      const pageData = source.slice(start, end)
      this.posts = reset ? pageData : [...this.posts, ...pageData]
      this.hasMore = end < source.length
    },
    resetVisiblePosts() {
      this.page = 1
      this.posts = []
      this.hasMore = true
      this.loadPageData(true)
    },
    loadMore() {
      if (this.loading || !this.hasMore) return
      this.page += 1
      this.loadPageData()
    }
  }
}
</script>

<style scoped>
.circle-home-page {
  --circle-radius: 8px;
  --circle-border: var(--it-border);
  --circle-border-strong: var(--it-border-strong);
  --circle-bg-card: var(--it-panel-bg-strong, var(--it-surface));
  --circle-bg-soft: var(--it-surface-muted);
  --circle-text: var(--it-text);
  --circle-muted: var(--it-text-muted);
  --circle-subtle: var(--it-text-subtle);
  --circle-accent: var(--it-accent);
  --circle-accent-soft: var(--it-accent-soft);
  --circle-shadow: var(--it-shadow-soft, var(--it-shadow));

  width: 100%;
  max-width: none;
  margin: 0 auto;
  padding: 13px 8px 18px;
  display: grid;
  gap: 13px;
  color: var(--circle-text);
}

.circle-home-hero :deep(.front-hero-panel) {
  border-radius: var(--circle-radius);
  border-color: var(--circle-border);
  background: var(--circle-bg-card);
  box-shadow: var(--circle-shadow);
  padding: 13px;
  gap: 13px;
}

.circle-home-hero :deep(.front-hero-panel__title) {
  font-size: clamp(20px, 2.1vw, 30px);
}

.circle-home-hero :deep(.front-hero-panel__subtitle) {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
}

.circle-home-hero :deep(.front-hero-panel__stat-card) {
  border-radius: 8px;
  border-color: var(--circle-border);
  background: color-mix(in srgb, var(--circle-bg-card) 88%, var(--circle-accent-soft));
  padding: 10px;
}

.circle-home-hero :deep(.front-hero-panel__stat-value) {
  font-size: 18px;
}

.hero-filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-chip {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border: 1px solid var(--circle-border);
  border-radius: 999px;
  background: var(--circle-bg-soft);
  color: var(--circle-muted);
  font-size: 12px;
}

.hero-chip--accent {
  border-color: transparent;
  background: var(--it-primary-gradient);
  color: #fff;
}

.hot-circle-strip {
  border: 1px solid var(--circle-border);
  border-radius: var(--circle-radius);
  background: var(--circle-bg-card);
  box-shadow: var(--circle-shadow);
  padding: 13px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.section-header h2 {
  margin: 0;
  font-size: 15px;
}

.text-link {
  border: none;
  background: transparent;
  color: var(--circle-accent);
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}

.hot-circle-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.hot-circle-item {
  border: 1px solid var(--circle-border);
  border-radius: 8px;
  background: var(--circle-bg-soft);
  color: var(--circle-muted);
  min-width: 130px;
  padding: 8px 10px;
  text-align: left;
  display: grid;
  gap: 4px;
  cursor: pointer;
}

.hot-circle-item strong {
  font-size: 13px;
  color: var(--circle-text);
}

.hot-circle-item span {
  font-size: 11px;
}

.hot-circle-item.is-active,
.hot-circle-item:hover {
  border-color: var(--circle-border-strong);
  background: var(--circle-accent-soft);
}

.circle-forum-shell :deep(.front-page-shell__main.circle-forum-main) {
  border: none;
  box-shadow: none;
  background: transparent;
  padding: 0;
}

.forum-left-nav {
  display: grid;
  gap: 13px;
}

.left-panel {
  border: 1px solid var(--circle-border);
  border-radius: var(--circle-radius);
  background: var(--circle-bg-card);
  box-shadow: var(--circle-shadow);
  padding: 13px;
}

.left-panel h3 {
  margin: 0 0 8px;
  font-size: 13px;
}

.circle-list,
.category-list,
.member-list,
.rule-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 6px;
}

.circle-list button,
.category-list button {
  width: 100%;
  border: 1px solid var(--circle-border);
  border-radius: 8px;
  background: var(--circle-bg-soft);
  color: var(--circle-muted);
  padding: 6px 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  cursor: pointer;
}

.circle-list button span,
.category-list button span {
  color: var(--circle-text);
  font-size: 12px;
}

.circle-list button em,
.category-list button em {
  font-style: normal;
  font-size: 11px;
}

.circle-list button:hover,
.category-list button:hover {
  border-color: var(--circle-border-strong);
  background: var(--circle-accent-soft);
}

.left-empty,
.rail-empty {
  margin: 0;
  font-size: 12px;
  color: var(--circle-muted);
}

.forum-stream {
  display: grid;
  gap: 13px;
}

.stream-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.stream-heading h2 {
  margin: 0 0 4px;
  font-size: 18px;
}

.stream-heading p {
  margin: 0;
  color: var(--circle-muted);
  font-size: 12px;
  line-height: 1.5;
}

.stream-total {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid var(--circle-border);
  background: var(--circle-bg-soft);
  color: var(--circle-muted);
  font-size: 12px;
}

.stream-toolbar :deep(.front-feed-toolbar) {
  border-radius: var(--circle-radius);
  border-color: var(--circle-border);
  box-shadow: var(--circle-shadow);
  background: var(--circle-bg-card);
  padding: 13px;
}

.stream-toolbar :deep(.front-feed-toolbar__sort .el-radio-button__inner) {
  border-radius: 8px;
}

.toolbar-summary {
  color: var(--circle-muted);
  font-size: 12px;
}

.post-list {
  min-height: 220px;
  display: grid;
  gap: 13px;
}

.forum-post-shell {
  min-width: 0;
}

.forum-post-card {
  border-radius: var(--circle-radius);
  border: 1px solid var(--circle-border);
  background: var(--circle-bg-card);
  box-shadow: var(--circle-shadow);
  cursor: pointer;
}

.forum-post-card:hover {
  border-color: var(--circle-border-strong);
}

.forum-post-card :deep(.el-card__body) {
  padding: 13px;
  display: grid;
  gap: 13px;
}

.forum-post-card__head,
.forum-post-card__meta,
.forum-post-card__author,
.forum-post-card__stats,
.forum-post-card__badges {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.forum-post-card__head,
.forum-post-card__meta {
  justify-content: space-between;
}

.forum-circle-chip {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid var(--circle-border);
  background: var(--circle-accent-soft);
  color: var(--circle-accent);
  font-size: 12px;
  font-weight: 600;
}

.forum-post-card__update-time {
  color: var(--circle-subtle);
  font-size: 12px;
}

.forum-post-card__title {
  margin: 0;
  font-size: 17px;
  line-height: 1.35;
  color: var(--circle-text);
}

.forum-post-card__summary {
  margin: 0;
  color: var(--circle-muted);
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.forum-post-card__author-copy {
  display: grid;
  gap: 2px;
}

.forum-post-card__author-copy strong {
  font-size: 13px;
}

.forum-post-card__author-copy span,
.forum-post-card__stats span {
  color: var(--circle-muted);
  font-size: 12px;
}

.forum-post-card__stats span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.forum-post-card__action {
  color: var(--circle-accent) !important;
  font-weight: 600;
}

.list-status {
  text-align: center;
  color: var(--circle-muted);
  font-size: 12px;
  padding: 10px;
}

.circle-forum-shell :deep(.front-page-shell__rail--right .front-right-rail) {
  border-radius: var(--circle-radius);
  border-color: var(--circle-border);
  box-shadow: var(--circle-shadow);
  background: var(--circle-bg-card);
  padding: 13px;
}

.circle-forum-shell :deep(.front-page-shell__rail--right .front-right-rail__body) {
  gap: 13px;
}

.circle-forum-shell :deep(.front-page-shell__rail--right .front-right-rail__section) {
  border-radius: 8px;
  border-color: var(--circle-border);
  background: var(--circle-bg-soft);
  padding: 8px;
}

.rail-section h4 {
  margin: 0 0 6px;
  font-size: 13px;
}

.rail-section p {
  margin: 0;
  color: var(--circle-muted);
  font-size: 12px;
  line-height: 1.6;
}

.member-list li {
  display: flex;
  align-items: center;
  gap: 8px;
}

.member-main {
  display: grid;
  min-width: 0;
}

.member-main strong {
  font-size: 12px;
}

.member-main em {
  font-style: normal;
  color: var(--circle-muted);
  font-size: 11px;
}

.rule-list li {
  position: relative;
  padding-left: 12px;
  color: var(--circle-muted);
  font-size: 12px;
  line-height: 1.5;
}

.rule-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: var(--circle-accent);
}

@media screen and (max-width: 768px) {
  .circle-home-page {
    padding: 8px 8px 14px;
  }

  .stream-heading {
    flex-direction: column;
  }

  .toolbar-summary {
    display: none;
  }

  .forum-post-card__meta {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
