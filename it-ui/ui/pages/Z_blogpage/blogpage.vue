<template>
  <div data-testid="blog-feed-page" class="blog-discovery-page">
    <front-hero-panel
      class="blog-discovery-hero"
      compact
      badge="技术内容发现页"
      :title="featuredHeroTitle"
      :subtitle="featuredHeroSubtitle"
      :stats="heroStats"
      :stats-columns="3"
      data-testid="blog-featured-hero"
    >
      <template #actions>
        <el-button
          v-if="featuredPost"
          type="primary"
          size="small"
          data-testid="blog-featured-open-btn"
          @click="goToDetail(featuredPost)"
        >
          阅读精选
        </el-button>
      </template>

      <div v-if="featuredPost" data-testid="blog-featured-meta" class="featured-meta">
        <div class="featured-meta__line">
          <el-tag :type="getPriceTagType(featuredPost)" size="mini">{{ getPriceTagText(featuredPost) }}</el-tag>
          <span>{{ formatDate(featuredPost.publishTime || featuredPost.createdAt || featuredPost.updatedAt) }}</span>
        </div>
        <div class="featured-meta__author" @click="goToAuthorPage(featuredPost.author)">
          <el-avatar :size="28" :src="getAuthorAvatar(featuredPost.author)"></el-avatar>
          <span>{{ getAuthorName(featuredPost.author) }}</span>
        </div>
        <div class="featured-meta__stats">
          <span><i class="el-icon-view"></i>{{ normalizeCount(featuredPost.viewCount) }}</span>
          <span><i class="el-icon-star-off"></i>{{ normalizeCount(featuredPost.likeCount) }}</span>
          <span><i class="el-icon-chat-line-round"></i>{{ normalizeCount(featuredPost.commentCount) }}</span>
          <span><i class="el-icon-collection-tag"></i>{{ normalizeCount(featuredPost.collectCount) }}</span>
        </div>
      </div>
    </front-hero-panel>

    <div data-testid="blog-feed-sort-toolbar" class="blog-toolbar-wrap">
      <span class="toolbar-summary" data-testid="blog-toolbar-summary">
        当前显示 {{ posts.length }} 篇（总计 {{ total || posts.length }} 篇）
      </span>
    </div>

    <front-page-shell
      class="blog-discovery-shell"
      layout="two"
      two-column-side="right"
      :max-width="'100%'"
      :right-width="300"
      :gap="12"
      :padding-top="0"
      :padding-bottom="0"
      :show-background="false"
      content-class="blog-discovery-main"
    >
      <section class="blog-stream">
        <div v-loading="loading" element-loading-text="正在加载文章..." class="blog-stream-loading">
          <div v-if="!loading && posts.length > 0" data-testid="blog-feed-list" class="blog-post-list">
            <el-card
              v-for="(post, index) in normalPosts"
              :key="post.id || `${post.title || 'post'}-${index}`"
              :data-testid="`blog-feed-card-${post.id || index}`"
              class="blog-post-card"
              shadow="hover"
              @click.native="goToDetail(post)"
            >
              <div class="blog-post-card__head">
                <div class="blog-post-card__head-left">
                  <el-tag :type="getPriceTagType(post)" size="mini">{{ getPriceTagText(post) }}</el-tag>
                  <span>{{ formatDate(post.publishTime || post.createdAt || post.updatedAt) }}</span>
                </div>
                <span class="blog-post-card__open">阅读全文</span>
              </div>

              <h3 class="blog-post-card__title" :data-testid="`blog-feed-title-${post.id || index}`">
                {{ post.title || '无标题' }}
              </h3>

              <p class="blog-post-card__excerpt">{{ formatContent(post, 140) || '暂无摘要' }}</p>

              <div class="blog-post-card__author-row">
                <div class="blog-post-card__author" @click.stop="goToAuthorPage(post.author)">
                  <el-avatar :size="28" :src="getAuthorAvatar(post.author)"></el-avatar>
                  <span>{{ getAuthorName(post.author) }}</span>
                </div>
                <span class="blog-post-card__author-label">作者</span>
              </div>

              <div v-if="getPostTagItems(post).length" class="blog-post-card__tags">
                <el-tag
                  v-for="item in getPostTagItems(post)"
                  :key="`${post.id}-${item.id}`"
                  size="mini"
                  class="blog-post-card__tag"
                  @click.stop="filterByTag(item.id)"
                >
                  #{{ item.label }}
                </el-tag>
              </div>

              <div class="blog-post-card__stats">
                <span><i class="el-icon-view"></i>{{ normalizeCount(post.viewCount) }} 阅读</span>
                <span><i class="el-icon-star-off"></i>{{ normalizeCount(post.likeCount) }} 点赞</span>
                <span><i class="el-icon-chat-line-round"></i>{{ normalizeCount(post.commentCount) }} 评论</span>
                <span><i class="el-icon-collection-tag"></i>{{ normalizeCount(post.collectCount) }} 收藏</span>
              </div>
            </el-card>

            <front-empty-state
              v-if="normalPosts.length === 0"
              data-testid="blog-feed-only-featured"
              icon="el-icon-document-checked"
              title="当前筛选只有精选文章"
              description="你可以切换标签、关键词或排序，发现更多技术内容。"
              size="sm"
            />
          </div>

          <front-empty-state
            v-if="!loading && posts.length === 0"
            data-testid="blog-feed-empty"
            icon="el-icon-document-delete"
            title="暂无文章"
            description="当前筛选条件下没有匹配结果，请调整筛选后重试。"
            size="lg"
          />
        </div>
      </section>

      <template #right>
        <front-right-rail
          title="发现侧栏"
          subtitle="今日热榜、热门标签和推荐作者"
          data-testid="blog-right-rail"
        >
          <section class="front-right-rail__section rail-section" data-testid="blog-right-hot-posts">
            <h4 class="rail-section__title">今日热榜</h4>
            <ul v-if="hotPosts.length" class="rail-hot-list">
              <li v-for="(post, index) in hotPosts" :key="`hot-${post.id || index}`">
                <button type="button" class="rail-hot-item" @click="goToDetail(post)">
                  <span class="rail-hot-item__rank">{{ index + 1 }}</span>
                  <span class="rail-hot-item__title">{{ post.title || '无标题' }}</span>
                  <span class="rail-hot-item__meta">
                    {{ normalizeCount(post.viewCount) }} 阅读 · {{ normalizeCount(post.likeCount) }} 点赞
                  </span>
                </button>
              </li>
            </ul>
            <p v-else class="rail-empty-tip">暂无热榜数据</p>
          </section>

          <front-tag-cloud
            data-testid="blog-right-popular-tags"
            title="热门标签"
            :tags="popularTags"
            :active-values="tag ? [tag] : []"
            :multiple="false"
            :show-count="true"
            empty-text="暂无标签"
            @tag-click="handleTagCloudSelect"
          />

          <section class="front-right-rail__section rail-section" data-testid="blog-right-authors">
            <h4 class="rail-section__title">推荐作者</h4>
            <ul v-if="recommendedAuthors.length" class="rail-author-list">
              <li v-for="authorItem in recommendedAuthors" :key="authorItem.key" class="rail-author-item">
                <button type="button" class="rail-author-item__main" @click="filterByAuthor(authorItem.name)">
                  <el-avatar :size="30" :src="authorItem.avatar"></el-avatar>
                  <span class="rail-author-item__text">
                    <strong>{{ authorItem.name }}</strong>
                    <em>{{ authorItem.postCount }} 篇文章</em>
                  </span>
                </button>
                <el-button type="text" @click.stop="goToAuthorProfile(authorItem)">主页</el-button>
              </li>
            </ul>
            <p v-else class="rail-empty-tip">暂无推荐作者</p>
          </section>
        </front-right-rail>
      </template>
    </front-page-shell>

    <div v-if="total > pageSize" class="pagination-wrapper">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page.sync="currentPage"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script>
import { fetchBlogFeed, normalizeBlog, resolveBlogAccessType, unwrapApiPayload } from '@/api/blog'
import { GetAllBlogs, GetAllTags } from '@/api/index'
import { richContentToPlainText } from '@/utils/richContent'
import {
  FrontEmptyState,
  FrontHeroPanel,
  FrontPageShell,
  FrontRightRail,
  FrontTagCloud
} from '@/components/front'

export default {
  layout: 'blog',
  components: {
    FrontEmptyState,
    FrontHeroPanel,
    FrontPageShell,
    FrontRightRail,
    FrontTagCloud
  },
  data() {
    return {
      posts: [],
      total: 0,
      pageSize: 5,
      loading: false,
      fallbackActive: false,
      sortType: 'time_desc',
      searchDraft: '',
      tagCatalogLoaded: false,
      tagCatalogLoading: null,
      tagMapById: {},
      tagMapByName: {},
      sortOptions: [
        { id: 'hot', label: '热度', value: 'hot', icon: 'el-icon-fire' },
        { id: 'time_desc', label: '最新', value: 'time_desc', icon: 'el-icon-bottom' },
        { id: 'time_asc', label: '最早', value: 'time_asc', icon: 'el-icon-top' }
      ]
    }
  },
  computed: {
    currentPage: {
      get() {
        const page = parseInt(this.$route.query.page, 10)
        return Number.isFinite(page) && page > 0 ? page : 1
      },
      set(page) {
        const nextQuery = { ...this.$route.query }
        if (page > 1) nextQuery.page = page
        else delete nextQuery.page
        this.pushRouteQuery(nextQuery)
      }
    },
    author() {
      return this.$route.query.author || ''
    },
    keyword() {
      return this.$route.query.keyword || ''
    },
    tag() {
      return this.$route.query.tag || ''
    },
    sortFromRoute() {
      return this.$route.query.sort || 'time_desc'
    },
    featuredPost() {
      if (!this.posts.length) return null
      const firstPost = this.posts[0]
      const hottestPost = [...this.posts].sort((a, b) => this.getHotScore(b) - this.getHotScore(a))[0]
      return this.sortType === 'hot' ? hottestPost || firstPost : firstPost || hottestPost
    },
    normalPosts() {
      if (!this.featuredPost) return this.posts
      let skipped = false
      return this.posts.filter(post => {
        const matched = this.isSamePost(post, this.featuredPost)
        if (matched && !skipped) {
          skipped = true
          return false
        }
        return true
      })
    },
    hotPosts() {
      return [...this.posts]
        .sort((a, b) => this.getHotScore(b) - this.getHotScore(a))
        .slice(0, 6)
    },
    popularTags() {
      const counter = new Map()
      this.posts.forEach(post => {
        this.getPostTagItems(post).forEach(item => {
          const id = this.normalizeTagValue(item.id)
          const label = String(item.label || '').trim()
          if (!id || !label) return
          const previous = counter.get(id) || { id, label, value: id, count: 0 }
          previous.count += 1
          counter.set(id, previous)
        })
      })
      return Array.from(counter.values())
        .sort((a, b) => b.count - a.count || a.label.localeCompare(b.label))
        .slice(0, 20)
    },
    popularRootTags() {
      const counter = new Map()
      this.posts.forEach(post => {
        this.getPostRootTagItems(post).forEach(item => {
          const id = this.normalizeTagValue(item.id)
          const label = String(item.label || '').trim()
          if (!id || !label) return
          const previous = counter.get(id) || { id, label, value: id, count: 0 }
          previous.count += 1
          counter.set(id, previous)
        })
      })
      return Array.from(counter.values())
        .sort((a, b) => b.count - a.count || a.label.localeCompare(b.label))
    },
    recommendedAuthors() {
      const bucket = new Map()
      this.posts.forEach(post => {
        const author = post.author || {}
        const name = this.getAuthorName(author)
        if (!name || name === '未知作者') return
        const key = author.id || name
        const previous = bucket.get(key) || {
          key,
          id: author.id || null,
          name,
          avatar: this.getAuthorAvatar(author),
          postCount: 0,
          score: 0
        }
        previous.postCount += 1
        previous.score += this.getHotScore(post)
        if (!previous.avatar) previous.avatar = this.getAuthorAvatar(author)
        bucket.set(key, previous)
      })
      return Array.from(bucket.values())
        .sort((a, b) => b.score - a.score || b.postCount - a.postCount)
        .slice(0, 6)
    },
    categoryTabs() {
      const tabs = [
        {
          id: 'all',
          label: '全部',
          value: 'all',
          count: this.total || this.posts.length
        }
      ]
      this.popularRootTags.slice(0, 8).forEach(item => {
        tabs.push({
          id: `tag-root-${item.value}`,
          label: item.label,
          value: item.value,
          count: item.count
        })
      })
      return tabs
    },
    activeTagTab() {
      if (!this.tag) return 'all'
      const meta = this.resolveTagMeta(this.tag)
      return meta && meta.id ? meta.id : this.tag
    },
    heroStats() {
      return [
        { id: 'current', label: '当前页文章', value: this.posts.length },
        { id: 'total', label: '可浏览总数', value: this.total || this.posts.length },
        { id: 'topic', label: '当前主题', value: this.tag ? this.resolveTagLabel(this.tag) : (this.keyword || this.author || '全部') }
      ]
    },
    featuredHeroTitle() {
      if (this.featuredPost && this.featuredPost.title) return this.featuredPost.title
      return '发现值得深入阅读的技术内容'
    },
    featuredHeroSubtitle() {
      if (this.featuredPost) return this.formatContent(this.featuredPost, 160) || '这篇文章正在被更多开发者关注。'
      return '从最新实践到深度经验总结，在这里快速找到高价值技术文章。'
    }
  },
  watch: {
    '$route.query': {
      handler() {
        this.sortType = this.sortFromRoute
        this.searchDraft = this.keyword
        this.fetchPosts()
      },
      immediate: true,
      deep: true
    }
  },
  methods: {
    pushRouteQuery(query) {
      const nextQuery = { ...query }
      Object.keys(nextQuery).forEach(key => {
        if (nextQuery[key] === undefined || nextQuery[key] === null || nextQuery[key] === '') delete nextQuery[key]
      })
      if (Number(nextQuery.page) <= 1) delete nextQuery.page
      const navigation = this.$router.push({ path: this.$route.path, query: nextQuery })
      if (navigation && typeof navigation.catch === 'function') {
        navigation.catch(error => {
          if (!error || error.name !== 'NavigationDuplicated') {
            console.error('路由更新失败:', error)
          }
        })
      }
    },
    updateQuery(patch = {}, { resetPage = false } = {}) {
      const nextQuery = { ...this.$route.query, ...patch }
      if (resetPage) nextQuery.page = 1
      this.pushRouteQuery(nextQuery)
    },
    goToDetail(post) {
      if (!post || !post.id) return
      this.$router.push(`/blog/${post.id}`)
    },
    goToAuthorPage(author) {
      if (author && typeof author === 'object' && author.id) {
        this.$router.push(`/other/${author.id}`)
        return
      }
      const authorName = this.getAuthorName(author)
      if (authorName && authorName !== '未知作者') {
        this.filterByAuthor(authorName)
        return
      }
      this.$message.warning('作者信息不可用')
    },
    goToAuthorProfile(authorItem) {
      if (authorItem && authorItem.id) {
        this.$router.push(`/other/${authorItem.id}`)
        return
      }
      if (authorItem && authorItem.name) {
        this.filterByAuthor(authorItem.name)
      }
    },
    filterByTag(tag) {
      const nextTag = this.normalizeTagValue(tag)
      const currentMeta = this.resolveTagMeta(this.tag)
      const currentTag = this.normalizeTagValue(currentMeta && currentMeta.id ? currentMeta.id : this.tag)
      if (!nextTag || nextTag === currentTag) {
        this.updateQuery({ tag: '' }, { resetPage: true })
        return
      }
      this.updateQuery({ tag: nextTag }, { resetPage: true })
    },
    filterByAuthor(author) {
      this.updateQuery({ author }, { resetPage: true })
    },
    handleCategoryTabChange(value) {
      if (!value || value === 'all' || value === this.activeTagTab) {
        this.updateQuery({ tag: '' }, { resetPage: true })
        return
      }
      this.filterByTag(value)
    },
    handleSortChange(value) {
      this.sortType = value || 'time_desc'
      this.updateQuery({ sort: this.sortType }, { resetPage: true })
    },
    applyKeywordSearch() {
      this.updateQuery({ keyword: this.searchDraft || '' }, { resetPage: true })
    },
    clearActiveFilters() {
      this.searchDraft = ''
      this.updateQuery({ tag: '', author: '', keyword: '' }, { resetPage: true })
    },
    handleTagCloudSelect(payload) {
      if (!payload || !payload.value) return
      this.filterByTag(payload.value)
    },
    async ensureTagCatalog() {
      if (this.tagCatalogLoaded) return
      if (this.tagCatalogLoading) {
        await this.tagCatalogLoading
        return
      }
      this.tagCatalogLoading = (async () => {
        try {
          const response = await GetAllTags()
          const payload = unwrapApiPayload(response)
          const list = this.extractBlogListFromPayload(payload)
          this.applyTagCatalog(list)
          this.tagCatalogLoaded = true
        } catch (error) {
          console.warn('[blog-feed] load tag catalog failed', error)
          this.applyTagCatalog([])
        } finally {
          this.tagCatalogLoading = null
        }
      })()
      await this.tagCatalogLoading
    },
    applyTagCatalog(tags = []) {
      const byId = {}
      const byName = {}
      ;(Array.isArray(tags) ? tags : []).forEach(raw => {
        if (!raw || typeof raw !== 'object') return
        const id = this.normalizeTagValue(raw.id)
        const name = String(raw.name || raw.label || '').trim()
        const parentId = this.normalizeTagValue(raw.parentId || raw.parent_id)
        if (!id) return
        const item = {
          id,
          name: name || id,
          parentId: parentId || '',
          category: String(raw.category || '').trim()
        }
        byId[id] = item
        if (item.name) byName[item.name.toLowerCase()] = item
      })
      this.tagMapById = byId
      this.tagMapByName = byName
    },
    decoratePosts(list = []) {
      return (Array.isArray(list) ? list : []).map(item => this.decoratePost(item))
    },
    decoratePost(post = {}) {
      return {
        ...post,
        accessType: resolveBlogAccessType(post),
        tagItems: this.getPostTagItems(post)
      }
    },
    normalizeTagValue(value) {
      if (value === undefined || value === null) return ''
      const text = String(value).trim()
      if (!text || text.toLowerCase() === 'null' || text.toLowerCase() === 'undefined') return ''
      return text
    },
    resolveTagMeta(value) {
      const normalized = this.normalizeTagValue(value)
      if (!normalized) return null
      if (this.tagMapById[normalized]) return this.tagMapById[normalized]
      return this.tagMapByName[normalized.toLowerCase()] || null
    },
    resolveRootTagMeta(meta) {
      if (!meta || !meta.id) return null
      const visited = new Set([meta.id])
      let current = meta
      while (current.parentId) {
        const parent = this.tagMapById[current.parentId]
        if (!parent || visited.has(parent.id)) break
        current = parent
        visited.add(current.id)
      }
      return current
    },
    resolveTagLabel(value) {
      const meta = this.resolveTagMeta(value)
      if (meta && meta.name) return meta.name
      return this.normalizeTagValue(value) || '未分类'
    },
    getPostTagItems(post) {
      if (!post || typeof post !== 'object') return []
      if (Array.isArray(post.tagItems)) return post.tagItems

      const refs = []
      if (Array.isArray(post.tagIds)) refs.push(...post.tagIds)
      if (Array.isArray(post.tags)) refs.push(...post.tags)

      const dedupe = new Map()
      refs.forEach(rawValue => {
        const normalized = this.normalizeTagValue(rawValue)
        if (!normalized) return

        const meta = this.resolveTagMeta(normalized)
        if (meta) {
          const rootMeta = this.resolveRootTagMeta(meta) || meta
          const id = this.normalizeTagValue(meta.id)
          if (!id || dedupe.has(id)) return
          dedupe.set(id, {
            id,
            label: meta.name || id,
            rootId: this.normalizeTagValue(rootMeta.id) || id,
            rootLabel: rootMeta.name || meta.name || id
          })
          return
        }

        if (dedupe.has(normalized)) return
        dedupe.set(normalized, {
          id: normalized,
          label: normalized,
          rootId: normalized,
          rootLabel: normalized
        })
      })
      return Array.from(dedupe.values())
    },
    getPostRootTagItems(post) {
      const rootDedupe = new Map()
      this.getPostTagItems(post).forEach(item => {
        const rootId = this.normalizeTagValue(item.rootId || item.id)
        const rootLabel = String(item.rootLabel || item.label || '').trim()
        if (!rootId || !rootLabel || rootDedupe.has(rootId)) return
        rootDedupe.set(rootId, { id: rootId, label: rootLabel, value: rootId })
      })
      return Array.from(rootDedupe.values())
    },
    isPostMatchedByTag(post, tagQuery = this.tag) {
      const normalizedQuery = this.normalizeTagValue(tagQuery)
      if (!normalizedQuery) return true

      const queryMeta = this.resolveTagMeta(normalizedQuery)
      const queryLower = normalizedQuery.toLowerCase()
      const queryId = queryMeta ? this.normalizeTagValue(queryMeta.id) : normalizedQuery
      const queryRoot = queryMeta ? this.resolveRootTagMeta(queryMeta) : null
      const isRootQuery = !!(queryMeta && queryRoot && queryRoot.id === queryMeta.id)

      return this.getPostTagItems(post).some(item => {
        const itemId = this.normalizeTagValue(item.id)
        const itemRootId = this.normalizeTagValue(item.rootId || item.id)
        const itemLabel = String(item.label || '').trim().toLowerCase()
        const rootLabel = String(item.rootLabel || '').trim().toLowerCase()

        if (isRootQuery) {
          return itemRootId === queryId || rootLabel === queryLower
        }
        return itemId === queryId || itemLabel === queryLower || itemRootId === normalizedQuery
      })
    },
    async fetchPosts() {
      this.loading = true
      try {
        await this.ensureTagCatalog()
        const resolvedTag = this.resolveTagMeta(this.tag)
        const result = await fetchBlogFeed({
          sortType: this.sortType,
          keyword: this.keyword,
          tag: resolvedTag && resolvedTag.id ? resolvedTag.id : this.tag,
          author: this.author,
          page: this.currentPage,
          pageSize: this.pageSize
        })
        this.posts = this.decoratePosts(result.list || [])
        this.total = result.total || 0
        this.fallbackActive = false
      } catch (error) {
        console.warn('[blog-feed] fetchBlogFeed failed, switching to fallback list API', error)
        await this.fetchPostsWithFallback(error)
      } finally {
        this.loading = false
      }
    },
    async fetchPostsWithFallback(originError) {
      try {
        const response = await GetAllBlogs()
        const payload = unwrapApiPayload(response)
        const baseList = this.extractBlogListFromPayload(payload).map(item => normalizeBlog(item))
        const filtered = this.filterPostsForCurrentQuery(baseList)
        const sorted = this.sortPostsByCurrentType(filtered)
        const paged = this.slicePostsByPage(sorted, this.currentPage, this.pageSize)

        this.posts = this.decoratePosts(paged.list || [])
        this.total = paged.total
        if (!this.fallbackActive) {
          this.$message.warning('排序服务暂不可用，已切换到兜底列表模式')
        }
        this.fallbackActive = true
      } catch (fallbackError) {
        this.posts = []
        this.total = 0
        const primaryMessage = (originError && originError.response && originError.response.data && originError.response.data.message) ||
          (originError && originError.message) ||
          '网络错误'
        const fallbackMessage = (fallbackError && fallbackError.response && fallbackError.response.data && fallbackError.response.data.message) ||
          (fallbackError && fallbackError.message) ||
          '网络错误'
        this.$message.error(`获取博客列表失败：主接口(${primaryMessage})；兜底接口(${fallbackMessage})`)
      }
    },
    extractBlogListFromPayload(payload) {
      if (Array.isArray(payload)) return payload
      if (!payload || typeof payload !== 'object') return []
      if (Array.isArray(payload.list)) return payload.list
      if (Array.isArray(payload.items)) return payload.items
      if (Array.isArray(payload.rows)) return payload.rows
      if (Array.isArray(payload.records)) return payload.records
      if (Array.isArray(payload.content)) return payload.content
      if (Array.isArray(payload.data)) return payload.data
      return []
    },
    filterPostsForCurrentQuery(posts) {
      const normalizedKeyword = (this.keyword || '').trim().toLowerCase()
      const normalizedTag = (this.tag || '').trim().toLowerCase()
      const normalizedAuthor = (this.author || '').trim().toLowerCase()

      return (posts || []).filter(post => {
        if (normalizedKeyword) {
          const haystack = [
            post.title,
            post.summary,
            post.previewContent,
            richContentToPlainText(post.content || '')
          ].join(' ').toLowerCase()
          if (!haystack.includes(normalizedKeyword)) return false
        }

        if (normalizedTag) {
          if (!this.isPostMatchedByTag(post, this.tag)) return false
        }

        if (normalizedAuthor) {
          const authorName = this.getAuthorName(post.author).trim().toLowerCase()
          if (!authorName.includes(normalizedAuthor)) return false
        }

        return true
      })
    },
    sortPostsByCurrentType(posts) {
      const list = Array.isArray(posts) ? [...posts] : []
      return list.sort((a, b) => {
        if (this.sortType === 'hot') {
          const scoreDiff = this.getHotScore(b) - this.getHotScore(a)
          if (scoreDiff !== 0) return scoreDiff
        }
        const timeA = this.getPostSortTime(a)
        const timeB = this.getPostSortTime(b)
        if (this.sortType === 'time_asc') return timeA - timeB
        return timeB - timeA
      })
    },
    slicePostsByPage(posts, page, pageSize) {
      const safePageSize = Math.max(1, Number(pageSize) || 5)
      const safePage = Math.max(1, Number(page) || 1)
      const start = (safePage - 1) * safePageSize
      return {
        list: posts.slice(start, start + safePageSize),
        total: posts.length
      }
    },
    getPostSortTime(post) {
      if (!post) return 0
      const raw = post.publishTime || post.createdAt || post.updatedAt
      if (!raw) return 0
      const time = new Date(raw).getTime()
      return Number.isFinite(time) ? time : 0
    },
    handlePageChange(page) {
      this.currentPage = page
    },
    normalizeCount(value) {
      const num = Number(value)
      return Number.isFinite(num) ? num : 0
    },
    getHotScore(post) {
      if (!post) return 0
      const views = this.normalizeCount(post.viewCount)
      const likes = this.normalizeCount(post.likeCount)
      const comments = this.normalizeCount(post.commentCount)
      const collects = this.normalizeCount(post.collectCount)
      return views + likes * 4 + comments * 6 + collects * 5
    },
    formatContent(post, maxLength = 100) {
      const source = post && typeof post === 'object'
        ? (post.summary || post.previewContent || post.content)
        : post
      const text = richContentToPlainText(source)
      if (!text) return ''
      if (text.length <= maxLength) return text
      return `${text.slice(0, maxLength)}...`
    },
    formatDate(value) {
      if (!value) return '未知时间'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return '未知时间'
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    getAuthorName(author) {
      if (!author) return '未知作者'
      if (typeof author === 'string') return author
      return author.displayName || author.nickname || author.username || '未知作者'
    },
    getAuthorAvatar(author) {
      if (!author || typeof author !== 'object') return ''
      return author.avatar || author.avatarUrl || ''
    },
    getPriceTagType(post) {
      const accessType = resolveBlogAccessType(post)
      if (accessType === 'free') return 'success'
      if (accessType === 'vip') return 'warning'
      return 'danger'
    },
    getPriceTagText(post) {
      const accessType = resolveBlogAccessType(post)
      if (accessType === 'free') return '免费'
      if (accessType === 'vip') return 'VIP'
      return `付费 ￥${this.normalizeCount(post.price)}`
    },
    isSamePost(left, right) {
      if (!left || !right) return false
      if (left.id !== undefined && left.id !== null && right.id !== undefined && right.id !== null) {
        return String(left.id) === String(right.id)
      }
      return left.title === right.title
    }
  }
}
</script>

<style scoped>
.blog-discovery-page {
  --blog-radius-card: 8px;
  --blog-radius-control: 8px;
  --blog-border: var(--it-border);
  --blog-border-strong: var(--it-border-strong);
  --blog-bg-card: var(--it-panel-bg-strong, var(--it-surface));
  --blog-bg-soft: var(--it-surface-muted);
  --blog-text: var(--it-text);
  --blog-text-muted: var(--it-text-muted);
  --blog-text-subtle: var(--it-text-subtle);
  --blog-accent: var(--it-accent);
  --blog-accent-soft: var(--it-accent-soft);
  --blog-shadow: var(--it-shadow-soft, var(--it-shadow));
  --blog-shadow-hover: var(--it-shadow-hover, var(--it-shadow-strong));
  --blog-space-xs: 8px;
  --blog-space-sm: 13px;
  --blog-space-md: 21px;

  width: 100%;
  max-width: none;
  margin: 0 auto;
  padding: 13px 8px 18px;
  display: grid;
  gap: var(--blog-space-sm);
  color: var(--blog-text);
}

.blog-discovery-hero {
  margin: 0;
}

.blog-discovery-hero :deep(.front-hero-panel) {
  padding: var(--blog-space-sm);
  gap: var(--blog-space-sm);
  border-radius: var(--blog-radius-card);
  border-color: var(--blog-border);
  background: var(--blog-bg-card);
  box-shadow: var(--blog-shadow);
}

.blog-discovery-hero :deep(.front-hero-panel__main) {
  gap: var(--blog-space-sm);
}

.blog-discovery-hero :deep(.front-hero-panel__title) {
  font-size: clamp(20px, 2.1vw, 32px);
  line-height: 1.12;
}

.blog-discovery-hero :deep(.front-hero-panel__subtitle) {
  margin-top: var(--blog-space-xs);
  font-size: 12px;
  line-height: 1.4;
  color: var(--blog-text-muted);
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.blog-discovery-hero :deep(.front-hero-panel__stats) {
  gap: var(--blog-space-xs);
}

.blog-discovery-hero :deep(.front-hero-panel__stat-card) {
  padding: var(--blog-space-xs) var(--blog-space-sm);
  display: flex;
  align-items: baseline;
  gap: var(--blog-space-sm);
  border-radius: var(--blog-radius-control);
  border-color: var(--blog-border);
  background: color-mix(in srgb, var(--blog-bg-card) 86%, var(--blog-accent-soft));
}

.blog-discovery-hero :deep(.front-hero-panel__stat-value) {
  margin: 0;
  font-size: 18px;
  color: var(--blog-text);
}

.blog-discovery-hero :deep(.front-hero-panel__stat-label) {
  margin: 0;
  font-size: 11px;
  color: var(--blog-text-muted);
}

.blog-discovery-hero :deep(.front-hero-panel__badge) {
  min-height: 24px;
  padding: 0 9px;
  border-radius: 999px;
  border-color: var(--blog-border);
  background: var(--blog-accent-soft);
  color: var(--blog-accent);
}

.featured-meta {
  margin-top: var(--blog-space-xs);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--blog-space-xs) var(--blog-space-sm);
}

.featured-meta__line,
.featured-meta__stats {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--blog-space-sm);
  color: var(--blog-text-muted);
  font-size: 12px;
}

.featured-meta__author {
  display: inline-flex;
  align-items: center;
  gap: var(--blog-space-xs);
  cursor: pointer;
  width: fit-content;
  color: var(--blog-text);
  font-size: 12px;
}

.featured-meta__stats span {
  display: inline-flex;
  align-items: center;
  gap: var(--blog-space-xs);
}

.blog-toolbar-wrap {
  margin: 0 0 var(--blog-space-sm);
  padding: 10px 14px;
  border-radius: var(--blog-radius-card);
  border: 1px solid var(--blog-border);
  background: var(--blog-bg-card);
  box-shadow: var(--blog-shadow);
}

.toolbar-summary {
  display: inline-flex;
  align-items: center;
  color: var(--blog-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.blog-discovery-shell :deep(.front-page-shell__main.blog-discovery-main) {
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 0;
}

.blog-discovery-shell :deep(.front-page-shell__rail--right .front-right-rail) {
  padding: var(--blog-space-sm);
  border-radius: var(--blog-radius-card);
  border-color: var(--blog-border);
  background: var(--blog-bg-card);
  box-shadow: var(--blog-shadow);
}

.blog-discovery-shell :deep(.front-page-shell__rail--right .front-right-rail__body) {
  gap: var(--blog-space-sm);
}

.blog-discovery-shell :deep(.front-page-shell__rail--right .front-right-rail__section) {
  padding: var(--blog-space-xs);
  border-radius: var(--blog-radius-control);
  border-color: var(--blog-border);
}

.blog-discovery-shell :deep(.front-page-shell__rail--right .front-right-rail__title) {
  color: var(--blog-text);
}

.blog-discovery-shell :deep(.front-page-shell__rail--right .front-right-rail__subtitle) {
  color: var(--blog-text-muted);
}

.blog-discovery-shell :deep(.front-tag-cloud) {
  padding: var(--blog-space-sm);
  border-radius: var(--blog-radius-control);
  border-color: var(--blog-border);
  box-shadow: none;
}

.blog-discovery-shell :deep(.front-tag-cloud__item) {
  min-height: 24px;
  padding: 0 9px;
  border-radius: 999px;
  border-color: var(--blog-border);
  background: var(--blog-bg-soft);
  color: var(--blog-text-muted);
}

.blog-discovery-shell :deep(.front-tag-cloud__item:hover) {
  border-color: var(--blog-border-strong);
  color: var(--blog-accent);
  background: var(--blog-accent-soft);
}

.blog-discovery-shell :deep(.front-tag-cloud__item.is-active) {
  border-color: transparent;
  background: var(--it-primary-gradient);
  color: #fff;
}

.blog-stream-loading {
  min-height: 180px;
}

.blog-post-list {
  display: grid;
  gap: var(--blog-space-sm);
}

.blog-post-card {
  cursor: pointer;
  border-radius: var(--blog-radius-card);
  border: 1px solid var(--blog-border);
  background: var(--blog-bg-card);
  box-shadow: var(--blog-shadow);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.blog-post-card:hover {
  border-color: var(--blog-border-strong);
  box-shadow: var(--blog-shadow-hover);
}

.blog-post-card :deep(.el-card__body) {
  padding: var(--blog-space-sm) 14px;
}

.blog-post-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--blog-space-xs);
  margin-bottom: var(--blog-space-xs);
}

.blog-post-card__head-left {
  display: inline-flex;
  align-items: center;
  gap: var(--blog-space-xs);
  color: var(--blog-text-muted);
  font-size: 11px;
}

.blog-post-card__open {
  color: var(--blog-accent);
  font-size: 11px;
  font-weight: 600;
}

.blog-post-card__title {
  margin: 0;
  color: var(--blog-text);
  font-size: 15px;
  line-height: 1.3;
  font-weight: 700;
}

.blog-post-card__excerpt {
  margin: var(--blog-space-xs) 0;
  color: var(--blog-text-muted);
  font-size: 12px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
}

.blog-post-card__author-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--blog-space-sm);
  flex-wrap: wrap;
}

.blog-post-card__author {
  display: inline-flex;
  align-items: center;
  gap: var(--blog-space-xs);
  cursor: pointer;
  color: var(--blog-text);
  font-size: 12px;
}

.blog-post-card__author-label {
  color: var(--blog-text-subtle);
  font-size: 11px;
}

.blog-post-card__tags {
  margin-top: var(--blog-space-xs);
  display: flex;
  flex-wrap: wrap;
  gap: var(--blog-space-xs);
}

.blog-post-card__tag {
  cursor: pointer;
  border-radius: 999px;
  border-color: var(--blog-border);
  background: var(--blog-bg-soft);
  color: var(--blog-text-muted);
}

.blog-post-card__tag:hover {
  color: var(--blog-accent);
  background: var(--blog-accent-soft);
  border-color: var(--blog-border-strong);
}

.blog-post-card__stats {
  margin-top: var(--blog-space-sm);
  padding-top: var(--blog-space-xs);
  border-top: 1px solid var(--blog-border);
  display: flex;
  flex-wrap: wrap;
  gap: var(--blog-space-sm);
  color: var(--blog-text-muted);
  font-size: 11px;
}

.blog-post-card__stats span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.rail-section {
  display: grid;
  gap: var(--blog-space-xs);
}

.rail-section__title {
  margin: 0;
  color: var(--blog-text);
  font-size: 13px;
}

.rail-hot-list,
.rail-author-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: var(--blog-space-xs);
}

.rail-hot-item {
  width: 100%;
  border: 1px solid var(--blog-border);
  border-radius: 8px;
  background: var(--blog-bg-soft);
  padding: var(--blog-space-xs);
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  gap: var(--blog-space-xs) var(--blog-space-sm);
  text-align: left;
  cursor: pointer;
}

.rail-hot-item:hover {
  border-color: var(--blog-border-strong);
  background: var(--blog-accent-soft);
}

.rail-hot-item__rank {
  grid-row: span 2;
  color: var(--blog-accent);
  font-weight: 700;
}

.rail-hot-item__title {
  color: var(--blog-text);
  font-size: 11px;
  line-height: 1.25;
  word-break: break-word;
}

.rail-hot-item__meta {
  color: var(--blog-text-muted);
  font-size: 10px;
}

.rail-author-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--blog-space-xs);
}

.rail-author-item__main {
  flex: 1;
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: var(--blog-space-xs);
  min-width: 0;
}

.rail-author-item__text {
  min-width: 0;
  display: grid;
}

.rail-author-item__text strong {
  color: var(--blog-text);
  font-size: 12px;
  line-height: 1.3;
}

.rail-author-item__text em {
  color: var(--blog-text-muted);
  font-style: normal;
  font-size: 11px;
}

.rail-empty-tip {
  margin: 0;
  color: var(--blog-text-muted);
  font-size: 11px;
}

.blog-discovery-page :deep(.el-button--small) {
  height: 34px;
  line-height: 32px;
  padding: 0 16px;
  border-radius: var(--blog-radius-control);
  border-color: var(--blog-border);
  font-size: 13px;
  background: var(--blog-bg-soft);
  color: var(--blog-text-muted);
}

.blog-discovery-page :deep(.el-button--small:hover),
.blog-discovery-page :deep(.el-button--small:focus) {
  color: var(--blog-accent);
  border-color: var(--blog-border-strong);
  background: var(--blog-accent-soft);
}

.blog-discovery-page :deep(.el-button--small.el-button--primary) {
  color: #fff;
  border-color: transparent;
  background: var(--it-primary-gradient);
  box-shadow: 0 8px 20px color-mix(in srgb, var(--blog-accent) 28%, transparent);
}

.blog-discovery-page :deep(.el-button--small.el-button--primary:hover),
.blog-discovery-page :deep(.el-button--small.el-button--primary:focus) {
  color: #fff;
  border-color: transparent;
  filter: brightness(1.04);
}

.blog-discovery-page :deep(.el-button--text) {
  min-height: 24px;
  padding: 0 var(--blog-space-xs);
  color: var(--blog-accent);
  border-radius: 6px;
}

.blog-discovery-page :deep(.el-button--text:hover),
.blog-discovery-page :deep(.el-button--text:focus) {
  background: var(--blog-accent-soft);
}

.blog-discovery-page :deep(.el-pagination.is-background .btn-prev),
.blog-discovery-page :deep(.el-pagination.is-background .btn-next),
.blog-discovery-page :deep(.el-pagination.is-background .el-pager li) {
  min-width: 28px;
  height: 28px;
  line-height: 28px;
  border-radius: 8px;
  border: 1px solid var(--blog-border);
  background: var(--blog-bg-soft);
  color: var(--blog-text-muted);
}

.blog-discovery-page :deep(.el-pagination.is-background .el-pager li:not(.disabled).active) {
  border-color: transparent;
  background: var(--it-primary-gradient);
  color: #fff;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 0;
}

@media screen and (max-width: 768px) {
  .blog-discovery-page {
    padding: 8px 8px 14px;
  }

  .featured-meta__stats,
  .blog-post-card__stats {
    gap: 10px;
  }
}
</style>
