<template>
  <div class="history-page" v-loading="loading">
    <section class="page-hero">
      <div>
        <span class="hero-badge">Browsing Chronicle</span>
        <h1 class="hero-title">浏览历史</h1>
        <p class="hero-subtitle">展示最近 30 天的浏览足迹，方便快速回到上次看到的内容。</p>
      </div>
      <div class="hero-stats">
        <div class="hero-stat">
          <span class="hero-stat-label">浏览条目</span>
          <strong class="hero-stat-value">{{ historyList.length }}</strong>
        </div>
        <div class="hero-stat">
          <span class="hero-stat-label">账号昵称</span>
          <strong class="hero-stat-value">{{ username || '未设置' }}</strong>
        </div>
        <div class="hero-stat">
          <span class="hero-stat-label">兴趣标签</span>
          <strong class="hero-stat-value">{{ userbog.length ? userbog.join(' / ') : '待完善' }}</strong>
        </div>
      </div>
    </section>

    <div class="content-grid">
      <aside class="profile-panel">
        <div class="panel-header">
          <div>
            <span class="panel-tag">Profile Snapshot</span>
            <h2>个人信息</h2>
          </div>
          <el-button plain size="small" @click="goBack">返回个人主页</el-button>
        </div>
        <div class="profile-items">
          <div class="profile-item"><span class="item-label">昵称</span><span class="item-value">{{ username || '未设置昵称' }}</span></div>
          <div class="profile-item"><span class="item-label">生日</span><span class="item-value">{{ formatDate(userbrithday) || '未设置生日' }}</span></div>
          <div class="profile-item"><span class="item-label">邮箱</span><span class="item-value">{{ useremail || '未设置邮箱' }}</span></div>
          <div class="profile-item"><span class="item-label">联系地址</span><span class="item-value">{{ useraddress || '未设置地址' }}</span></div>
          <div class="profile-item"><span class="item-label">性别</span><span class="item-value">{{ usersex || '未设置性别' }}</span></div>
          <div class="profile-item"><span class="item-label">标签</span><span class="item-value">{{ userbog.length ? userbog.join('、') : '未设置标签' }}</span></div>
          <div class="profile-item"><span class="item-label">签名</span><span class="item-value multiline">{{ usersign || '这个用户还没有留下签名。' }}</span></div>
        </div>
      </aside>

      <section class="history-panel">
        <div class="panel-header">
          <div>
            <span class="panel-tag">Recent Visits</span>
            <h2>最近阅读内容</h2>
          </div>
          <el-button plain size="small" icon="el-icon-refresh" :loading="loading" @click="loadPage">刷新记录</el-button>
        </div>

        <div v-if="historyList.length" class="history-list">
          <article
            v-for="(item, index) in historyList"
            :key="`${item.targetId}-${index}`"
            class="history-card"
            :class="{ 'history-card--disabled': !item.canOpen }"
            @click="openHistoryItem(item)"
          >
            <div class="history-cover">
              <el-image
                :src="item.pic"
                fit="cover"
                :preview-src-list="item.pic ? [item.pic] : []"
                class="cover-image"
              >
                <div slot="error" class="cover-fallback">
                  <i class="el-icon-picture-outline"></i>
                </div>
              </el-image>
              <span class="history-index">{{ String(index + 1).padStart(2, '0') }}</span>
            </div>
            <div class="history-body">
              <div class="history-meta">
                <span class="history-author"><i class="el-icon-user-solid"></i>{{ item.zuozhe }}</span>
                <span class="history-chip">{{ item.visitTimeText }}</span>
              </div>
              <h3 class="history-title">{{ item.title }}</h3>
              <p class="history-desc">{{ item.summary || '保留最近一次阅读入口，方便继续回看内容脉络与作者观点。' }}</p>
              <div class="history-footer">
                <span class="history-count">浏览次数 {{ item.viewCount }}</span>
                <el-button
                  size="mini"
                  type="primary"
                  plain
                  :disabled="!item.canOpen"
                  @click.stop="openHistoryItem(item)"
                >
                  {{ item.canOpen ? '继续阅读' : '内容不可访问' }}
                </el-button>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="empty-state">
          <i class="el-icon-time"></i>
          <h3>暂无历史记录</h3>
          <p>先去看看博客或圈子内容，这里会自动为你沉淀浏览足迹。</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { GetAllBlogs, GetCurrentUser, GetUserLogs } from '@/api/index.js'
import { useUserStore } from '@/store/user'

const DEFAULT_COVER = '/pic/choubi.jpg'

export default {
  name: 'HistoryPage',
  layout: 'default',
  data() {
    return {
      loading: false,
      historyList: [],
      username: '',
      userbrithday: '',
      useremail: '',
      useraddress: '',
      usersex: '',
      userbog: [],
      usersign: '',
      currentUserId: null
    }
  },
  created() {
    this.loadPage()
  },
  methods: {
    async loadPage() {
      this.loading = true
      try {
        const user = await this.resolveCurrentUser()
        if (!user || !user.id) {
          throw new Error('未获取到当前用户信息')
        }

        this.currentUserId = user.id
        this.applyUserProfile(user)

        const [logsResponse, blogsResponse] = await Promise.all([
          GetUserLogs(user.id),
          GetAllBlogs()
        ])

        const logs = this.ensureArray(this.unwrapResponse(logsResponse))
        const blogs = this.ensureArray(this.unwrapResponse(blogsResponse))
        this.historyList = this.buildHistoryList(logs, blogs)
      } catch (error) {
        console.error('加载浏览历史失败:', error)
        this.historyList = []
        this.$message.error('加载浏览历史失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },

    async resolveCurrentUser() {
      try {
        return this.unwrapResponse(await GetCurrentUser())
      } catch (error) {
        const userStore = useUserStore()
        return userStore.getUserInfo || userStore.userInfo || userStore.user || null
      }
    },

    unwrapResponse(response) {
      if (response && response.data !== undefined) {
        const payload = response.data
        if (payload && payload.data !== undefined && (payload.code !== undefined || payload.success !== undefined)) {
          return payload.data
        }
        return payload
      }
      return response
    },

    ensureArray(source) {
      if (Array.isArray(source)) return source
      if (source && Array.isArray(source.list)) return source.list
      if (source && Array.isArray(source.records)) return source.records
      if (source && Array.isArray(source.rows)) return source.rows
      if (source && source.data && Array.isArray(source.data)) return source.data
      return []
    },

    applyUserProfile(user = {}) {
      this.username = user.nickname || user.username || ''
      this.userbrithday = user.birthday || ''
      this.useremail = user.email || ''
      this.useraddress = user.regionName || user.address || ''
      this.usersex = this.formatGender(user.gender)
      this.userbog = this.normalizeTags(user.authorTagName)
      this.usersign = user.bio || ''
    },

    normalizeTags(value) {
      if (Array.isArray(value)) return value.filter(Boolean)
      if (!value) return []
      return String(value)
        .split(/[、,，/]/)
        .map(item => item.trim())
        .filter(Boolean)
    },

    buildHistoryList(logs, blogs) {
      const blogMap = new Map(
        blogs
          .filter(item => item && item.id !== undefined && item.id !== null)
          .map((item) => [String(item.id), item])
      )

      const seen = new Set()
      return logs
        .filter(item => String(item.targetType || '').toLowerCase() === 'blog' && item.targetId)
        .sort((a, b) => this.getTimeValue(b.createdAt) - this.getTimeValue(a.createdAt))
        .filter((item) => {
          const key = `${item.targetType}-${item.targetId}`
          if (seen.has(key)) return false
          seen.add(key)
          return true
        })
        .map((item) => {
          const blog = blogMap.get(String(item.targetId))
          const author = blog && blog.author ? (blog.author.displayName || blog.author.nickname || blog.author.username) : ''
          return {
            targetId: item.targetId,
            zuozhe: author || '未知作者',
            title: blog && blog.title ? blog.title : `博客 #${item.targetId}`,
            summary: this.normalizeSummary(blog && (blog.summary || blog.previewContent || blog.content)),
            pic: blog && blog.coverImageUrl ? blog.coverImageUrl : DEFAULT_COVER,
            visitTime: item.createdAt || '',
            visitTimeText: this.formatDateTime(item.createdAt),
            viewCount: this.countViewsForTarget(logs, item.targetId),
            canOpen: !!blog
          }
        })
    },

    countViewsForTarget(logs, targetId) {
      return logs.filter(item => String(item.targetId) === String(targetId)).length
    },

    normalizeSummary(value) {
      if (!value) return ''
      const text = String(value).replace(/\s+/g, ' ').trim()
      return text.length > 72 ? `${text.slice(0, 72)}...` : text
    },

    formatGender(value) {
      const map = {
        male: '男',
        female: '女',
        other: '其他'
      }
      return map[value] || value || ''
    },

    formatDate(date) {
      if (!date) return ''
      const d = new Date(date)
      if (Number.isNaN(d.getTime())) return ''
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${year}年${month}月${day}日`
    },

    formatDateTime(date) {
      if (!date) return '浏览时间未知'
      const d = new Date(date)
      if (Number.isNaN(d.getTime())) return '浏览时间未知'
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const hour = String(d.getHours()).padStart(2, '0')
      const minute = String(d.getMinutes()).padStart(2, '0')
      return `${month}-${day} ${hour}:${minute}`
    },

    getTimeValue(value) {
      const time = new Date(value).getTime()
      return Number.isFinite(time) ? time : 0
    },

    openHistoryItem(item) {
      if (!item || !item.canOpen) return
      this.$router.push(`/blog/${item.targetId}`)
    },

    goBack() {
      this.$router.push('/user')
    }
  },
  beforeRouteLeave(to, from, next) {
    document.body.style.overflow = ''
    next()
  },
  destroyed() {
    document.body.style.overflow = ''
  }
}
</script>

<style scoped>
.history-page {
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 0 48px;
  color: var(--it-text);
}

.page-hero,
.profile-panel,
.history-panel {
  border-radius: var(--it-radius-card-lg);
  border: 1px solid var(--it-border);
  background: var(--it-surface);
  box-shadow: var(--it-shadow);
}

.page-hero {
  padding: 30px 32px;
  margin-bottom: 24px;
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(240px, 1fr);
  gap: 20px;
}

.hero-badge,
.panel-tag,
.history-chip,
.history-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--it-accent-soft);
  color: var(--it-accent);
  font-size: 12px;
  font-weight: 700;
}

.hero-title {
  margin: 14px 0 10px;
  font-size: clamp(32px, 5vw, 44px);
  line-height: 1.08;
}

.hero-subtitle,
.history-desc,
.item-label,
.hero-stat-label {
  color: var(--it-text-muted);
  line-height: 1.75;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.hero-stat {
  padding: 18px 20px;
  border-radius: var(--it-radius-card);
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
}

.hero-stat-value {
  display: block;
  margin-top: 10px;
  font-size: 22px;
  color: var(--it-text);
}

.content-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 20px;
}

.profile-panel,
.history-panel {
  padding: 22px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
}

.panel-header h2 {
  margin: 12px 0 0;
  font-size: 24px;
}

.profile-items {
  display: grid;
  gap: 12px;
}

.profile-item {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border-radius: var(--it-radius-card);
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
}

.item-value {
  color: var(--it-text);
  font-weight: 600;
}

.multiline {
  white-space: pre-wrap;
  line-height: 1.7;
}

.history-list {
  display: grid;
  gap: 16px;
}

.history-card {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 18px;
  padding: 16px;
  border-radius: var(--it-radius-card);
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.history-card:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--it-accent) 24%, var(--it-border));
  box-shadow: 0 18px 30px rgba(20, 32, 46, 0.08);
}

.history-card--disabled {
  cursor: default;
  opacity: 0.82;
}

.history-card--disabled:hover {
  transform: none;
  box-shadow: none;
  border-color: var(--it-border);
}

.history-cover {
  position: relative;
}

.cover-image,
.history-cover :deep(.el-image) {
  width: 100%;
  height: 160px;
  border-radius: 18px;
  overflow: hidden;
}

.cover-fallback {
  width: 100%;
  height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(231, 238, 245, 0.9), rgba(214, 224, 235, 0.9));
  color: #6d7c90;
  font-size: 28px;
}

.history-index {
  position: absolute;
  top: 12px;
  left: 12px;
}

.history-body {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.history-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.history-author {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--it-text-light);
  font-size: 13px;
}

.history-title {
  margin: 0 0 12px;
  font-size: 22px;
  line-height: 1.35;
}

.history-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}

.history-count {
  color: var(--it-text-muted);
  font-size: 13px;
}

.empty-state {
  min-height: 260px;
  border-radius: var(--it-radius-card);
  border: 1px dashed var(--it-border-strong);
  background: color-mix(in srgb, var(--it-surface-solid) 88%, transparent);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.empty-state i {
  font-size: 30px;
  color: var(--it-accent);
}

@media (max-width: 1100px) {
  .content-grid,
  .page-hero {
    grid-template-columns: 1fr;
  }

  .hero-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .hero-stats,
  .history-card {
    grid-template-columns: 1fr;
  }

  .history-footer {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
