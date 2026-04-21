<template>
  <div
    ref="screenRoot"
    class="screen-dashboard"
    :class="{ 'is-fullscreen': isFullscreen }"
  >
    <div class="screen-dashboard__bg">
      <span class="screen-dashboard__orb screen-dashboard__orb--left"></span>
      <span class="screen-dashboard__orb screen-dashboard__orb--right"></span>
      <span class="screen-dashboard__grid"></span>
    </div>

    <header class="dashboard-hero panel-shell">
      <div class="dashboard-hero__main">
        <span class="panel-eyebrow">CONTENT OPERATIONS COMMAND</span>
        <h1>内容运营指挥中心</h1>
        <p>统一接入博客统计、趋势、热榜、审核队列与风险告警，保持后台主题一致的实时数字大屏。</p>

        <div class="hero-chip-row">
          <span class="hero-chip">{{ commandHealthText }}</span>
          <span class="hero-chip">7 日趋势联动</span>
          <span class="hero-chip">推荐与热榜联查</span>
        </div>
      </div>

      <div class="dashboard-hero__side">
        <div class="time-card">
          <span class="time-card__label">当前时间</span>
          <strong class="time-card__value">{{ currentTimeText }}</strong>
          <span class="time-card__sub">{{ currentDateText }}</span>
        </div>

        <div class="hero-statuses">
          <div class="status-pill">
            <span class="status-pill__dot" :class="{ 'status-pill__dot--live': !loading }"></span>
            <span>{{ loading ? '数据刷新中' : '实时轮询中' }}</span>
          </div>
          <div class="status-pill status-pill--compact">
            <span>最近同步</span>
            <strong>{{ lastUpdatedText }}</strong>
          </div>
        </div>

        <div class="hero-actions">
          <el-button
            size="small"
            icon="el-icon-refresh"
            :loading="loading"
            @click="fetchAll"
          >
            刷新数据
          </el-button>
          <el-button
            size="small"
            type="primary"
            icon="el-icon-monitor"
            @click="toggleFullscreen"
          >
            {{ isFullscreen ? '退出全屏' : '全屏展示' }}
          </el-button>
        </div>
      </div>
    </header>

    <section class="metric-grid">
      <article
        v-for="item in metricCards"
        :key="item.key"
        class="metric-card panel-shell"
        :class="`metric-card--${item.tone}`"
      >
        <div class="metric-card__header">
          <span class="metric-card__label">{{ item.label }}</span>
          <span class="metric-card__badge">{{ item.badge }}</span>
        </div>
        <div class="metric-card__value">{{ item.value }}</div>
        <div class="metric-card__footer">
          <span>{{ item.helper }}</span>
          <span :class="['metric-card__delta', `is-${item.deltaTone}`]">{{ item.delta }}</span>
        </div>
        <div class="metric-card__bar">
          <span :style="{ width: `${item.percent}%` }"></span>
        </div>
      </article>
    </section>

    <section class="dashboard-main">
      <article class="panel-shell command-panel">
        <div class="panel-heading">
          <div>
            <span class="panel-eyebrow">RISK OVERVIEW</span>
            <h2>审核与举报态势</h2>
          </div>
          <span class="panel-chip">{{ commandHealthText }}</span>
        </div>

        <div class="overview-grid">
          <div
            v-for="item in overviewTiles"
            :key="item.label"
            class="overview-tile"
            :class="`overview-tile--${item.tone}`"
          >
            <span class="overview-tile__label">{{ item.label }}</span>
            <strong class="overview-tile__value">{{ item.value }}</strong>
            <span class="overview-tile__desc">{{ item.desc }}</span>
          </div>
        </div>

        <div class="queue-grid">
          <section class="queue-card">
            <div class="queue-card__header">
              <h3>待审稿件</h3>
              <span>{{ pendingBlogRows.length }} / {{ formatNumber(pendingBlogsRaw) }}</span>
            </div>
            <div v-if="pendingBlogRows.length" class="queue-list">
              <div
                v-for="item in pendingBlogRows"
                :key="item.id"
                class="queue-item"
              >
                <div class="queue-item__title">{{ item.title || '未命名博客' }}</div>
                <div class="queue-item__meta">
                  <span>{{ getAuthorName(item.author) }}</span>
                  <span>{{ formatShortDate(item.createdAt) }}</span>
                </div>
              </div>
            </div>
            <div v-else class="panel-empty">当前没有待审核稿件</div>
          </section>

          <section class="queue-card">
            <div class="queue-card__header">
              <h3>待处理举报</h3>
              <span>{{ pendingReportRows.length }} / {{ formatNumber(pendingReportsRaw) }}</span>
            </div>
            <div v-if="pendingReportRows.length" class="queue-list">
              <div
                v-for="item in pendingReportRows"
                :key="item.id"
                class="queue-item"
              >
                <div class="queue-item__title">{{ item.targetTitle || '未命名目标内容' }}</div>
                <div class="queue-item__desc">{{ item.reason || '未填写举报原因' }}</div>
                <div class="queue-item__meta">
                  <span>{{ item.reporterName || '匿名用户' }}</span>
                  <span>{{ formatShortDate(item.createdAt) }}</span>
                </div>
              </div>
            </div>
            <div v-else class="panel-empty">当前没有待处理举报</div>
          </section>
        </div>
      </article>

      <article class="panel-shell trend-panel">
        <div class="panel-heading">
          <div>
            <span class="panel-eyebrow">REAL TREND</span>
            <h2>7 日趋势看板</h2>
          </div>
          <span class="panel-chip">实时接口</span>
        </div>

        <div class="trend-summary">
          <div
            v-for="item in trendSummary"
            :key="item.label"
            class="trend-summary__item"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>

        <div class="trend-card-grid">
          <article
            v-for="item in trendCards"
            :key="item.key"
            class="trend-card"
            :class="`trend-card--${item.tone}`"
          >
            <div class="trend-card__header">
              <div>
                <span class="trend-card__label">{{ item.label }}</span>
                <strong class="trend-card__value">{{ item.value }}</strong>
              </div>
              <span :class="['trend-card__delta', `is-${item.deltaTone}`]">{{ item.delta }}</span>
            </div>
            <svg class="trend-card__chart" viewBox="0 0 220 76" preserveAspectRatio="none">
              <polygon :points="item.areaPoints" class="trend-card__area"></polygon>
              <polyline :points="item.linePoints" class="trend-card__line"></polyline>
              <circle
                v-for="(dot, index) in item.dots"
                :key="`${item.key}-${index}`"
                class="trend-card__dot"
                :cx="dot.x"
                :cy="dot.y"
                r="3"
              ></circle>
            </svg>
          </article>
        </div>

        <div class="trend-axis">
          <span v-for="label in trendLabels" :key="label">{{ label }}</span>
        </div>

        <div class="hot-board">
          <div v-if="featuredBlog" class="hot-board__featured">
            <div class="hot-board__rank">#1</div>
            <div class="hot-board__main">
              <h3>{{ featuredBlog.title }}</h3>
              <p>{{ featuredBlog.summary || '当前热榜头部内容保持较高曝光，建议同步关注互动质量与举报联动。' }}</p>
              <div class="hot-board__meta">
                <span>{{ getAuthorName(featuredBlog.author) }}</span>
                <span>{{ formatShortDate(featuredBlog.publishTime || featuredBlog.createdAt) }}</span>
              </div>
            </div>
            <div class="hot-board__stats">
              <div>
                <span>浏览</span>
                <strong>{{ formatNumber(featuredBlog.viewCount || 0) }}</strong>
              </div>
              <div>
                <span>举报</span>
                <strong>{{ formatNumber(featuredBlog.reportCount || 0) }}</strong>
              </div>
            </div>
          </div>
          <div v-else class="panel-empty">暂无热榜数据</div>

          <div class="hot-board__list" v-if="hotBlogs.length">
            <div
              v-for="(item, index) in hotBlogs.slice(1, 6)"
              :key="item.id"
              class="hot-board__row"
            >
              <div class="hot-board__index">{{ index + 2 }}</div>
              <div class="hot-board__content">
                <div class="hot-board__title">{{ item.title }}</div>
                <div class="hot-board__sub">
                  <span>{{ getAuthorName(item.author) }}</span>
                  <span>{{ formatShortDate(item.publishTime || item.createdAt) }}</span>
                </div>
              </div>
              <div class="hot-board__value">{{ formatNumber(item.viewCount || 0) }}</div>
              <div class="hot-board__bar">
                <span :style="{ width: `${calcRankPercent(item.viewCount || 0, maxHotBlogViews)}%` }"></span>
              </div>
            </div>
          </div>
        </div>
      </article>

      <div class="dashboard-side">
        <article class="panel-shell alert-panel">
          <div class="panel-heading">
            <div>
              <span class="panel-eyebrow">ALERT MATRIX</span>
              <h2>告警与处置建议</h2>
            </div>
            <span class="panel-chip">{{ commandHealthText }}</span>
          </div>

          <div class="alert-stack">
            <div
              v-for="item in alertItems"
              :key="item.title"
              class="alert-card"
              :class="`alert-card--${item.level}`"
            >
              <div class="alert-card__header">
                <strong>{{ item.title }}</strong>
                <span>{{ item.badge }}</span>
              </div>
              <p>{{ item.desc }}</p>
              <div class="alert-card__foot">{{ item.hint }}</div>
            </div>
          </div>
        </article>

        <article class="panel-shell ranking-panel">
          <div class="panel-heading">
            <div>
              <span class="panel-eyebrow">RANKING MATRIX</span>
              <h2>作者与标签排行</h2>
            </div>
            <span class="panel-chip">热度聚合</span>
          </div>

          <div class="ranking-section">
            <div class="ranking-section__title">作者热度</div>
            <div v-if="hotAuthors.length" class="ranking-list">
              <div
                v-for="(item, index) in hotAuthors"
                :key="`author-${item.name}-${index}`"
                class="ranking-row"
              >
                <div class="ranking-row__main">
                  <span class="ranking-row__index">{{ index + 1 }}</span>
                  <div class="ranking-row__copy">
                    <strong>{{ item.name }}</strong>
                    <span>{{ item.count || 0 }} 篇上榜</span>
                  </div>
                </div>
                <div class="ranking-row__value">{{ formatNumber(item.heat || 0) }}</div>
                <div class="ranking-row__bar">
                  <span :style="{ width: `${calcRankPercent(item.heat || 0, maxAuthorHeat)}%` }"></span>
                </div>
              </div>
            </div>
            <div v-else class="panel-empty">暂无作者排行</div>
          </div>

          <div class="ranking-section">
            <div class="ranking-section__title">标签热度</div>
            <div v-if="hotTags.length" class="ranking-list">
              <div
                v-for="(item, index) in hotTags"
                :key="`tag-${item.name}-${index}`"
                class="ranking-row"
              >
                <div class="ranking-row__main">
                  <span class="ranking-row__index">{{ index + 1 }}</span>
                  <div class="ranking-row__copy">
                    <strong>{{ item.name }}</strong>
                    <span>{{ item.count || 0 }} 次命中</span>
                  </div>
                </div>
                <div class="ranking-row__value">{{ formatNumber(item.heat || 0) }}</div>
                <div class="ranking-row__bar">
                  <span :style="{ width: `${calcRankPercent(item.heat || 0, maxTagHeat)}%` }"></span>
                </div>
              </div>
            </div>
            <div v-else class="panel-empty">暂无标签排行</div>
          </div>
        </article>
      </div>
    </section>

    <section class="dashboard-footer">
      <article class="panel-shell action-panel">
        <div class="panel-heading">
          <div>
            <span class="panel-eyebrow">COMMAND ENTRY</span>
            <h2>运营指挥入口</h2>
          </div>
          <span class="panel-chip">全屏自适应</span>
        </div>

        <div class="action-grid">
          <button
            v-for="item in quickActions"
            :key="item.label"
            type="button"
            class="action-card"
            :class="`action-card--${item.tone}`"
            @click="item.handler"
          >
            <span class="action-card__label">{{ item.label }}</span>
            <strong class="action-card__value">{{ item.value }}</strong>
            <span class="action-card__desc">{{ item.desc }}</span>
          </button>
        </div>
      </article>

      <article class="panel-shell feed-panel">
        <div class="panel-heading">
          <div>
            <span class="panel-eyebrow">LIVE FEED</span>
            <h2>实时动态流</h2>
          </div>
          <span class="panel-chip">{{ formatNumber(activityFeed.length) }} 条</span>
        </div>

        <div class="feed-list">
          <div
            v-for="(item, index) in activityFeed"
            :key="`${item.title}-${index}`"
            class="feed-row"
            :class="`feed-row--${item.level || 'notice'}`"
          >
            <div class="feed-row__dot"></div>
            <div class="feed-row__content">
              <div class="feed-row__title">
                <span v-if="item.category" class="feed-row__category">{{ item.category }}</span>
                <span>{{ item.title }}</span>
              </div>
              <div class="feed-row__desc">{{ item.desc }}</div>
            </div>
            <div class="feed-row__time">{{ item.time }}</div>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script>
function safeNumber(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value))
}

function clampPercent(value) {
  return clamp(Math.round(safeNumber(value)), 4, 100)
}

function unwrapPayload(response) {
  return response && response.data !== undefined ? response.data : response
}

export default {
  name: 'BlogDashboard',
  layout: 'manage',
  data() {
    return {
      loading: false,
      isFullscreen: false,
      currentTime: new Date(),
      lastUpdatedAt: null,
      clockTimer: null,
      refreshTimer: null,
      metricAnimationFrame: null,
      overviewData: {
        totalBlogs: 0,
        pendingBlogs: 0,
        publishedBlogs: 0,
        rejectedBlogs: 0,
        pendingReports: 0,
        todayReports: 0,
        todayViews: 0
      },
      trendData: [],
      rankData: {
        hotBlogs: [],
        hotAuthors: [],
        hotTags: []
      },
      pendingBlogRows: [],
      pendingReportRows: [],
      activityItems: [],
      displayMetrics: {
        totalBlogs: 0,
        publishedBlogs: 0,
        pendingBlogs: 0,
        rejectedBlogs: 0,
        pendingReports: 0,
        todayReports: 0,
        todayViews: 0,
        activeAuthors: 0
      }
    }
  },
  computed: {
    currentTimeText() {
      const date = this.currentTime
      return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
    },
    currentDateText() {
      const date = this.currentTime
      const weekMap = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${weekMap[date.getDay()]}`
    },
    lastUpdatedText() {
      return this.lastUpdatedAt ? this.formatDateTime(this.lastUpdatedAt) : '尚未同步'
    },
    totalBlogsRaw() {
      return safeNumber(this.overviewData.totalBlogs)
    },
    publishedBlogsRaw() {
      return safeNumber(this.overviewData.publishedBlogs)
    },
    pendingBlogsRaw() {
      return safeNumber(this.overviewData.pendingBlogs)
    },
    rejectedBlogsRaw() {
      return safeNumber(this.overviewData.rejectedBlogs)
    },
    pendingReportsRaw() {
      return safeNumber(this.overviewData.pendingReports)
    },
    todayReportsRaw() {
      return safeNumber(this.overviewData.todayReports)
    },
    todayViewsRaw() {
      return safeNumber(this.overviewData.todayViews)
    },
    hotBlogs() {
      return Array.isArray(this.rankData.hotBlogs) ? this.rankData.hotBlogs : []
    },
    hotAuthors() {
      return Array.isArray(this.rankData.hotAuthors) ? this.rankData.hotAuthors : []
    },
    hotTags() {
      return Array.isArray(this.rankData.hotTags) ? this.rankData.hotTags : []
    },
    featuredBlog() {
      return this.hotBlogs.length ? this.hotBlogs[0] : null
    },
    maxHotBlogViews() {
      return this.hotBlogs.reduce((result, item) => Math.max(result, safeNumber(item.viewCount)), 0) || 1
    },
    maxAuthorHeat() {
      return this.hotAuthors.reduce((result, item) => Math.max(result, safeNumber(item.heat)), 0) || 1
    },
    maxTagHeat() {
      return this.hotTags.reduce((result, item) => Math.max(result, safeNumber(item.heat)), 0) || 1
    },
    activeAuthorsRaw() {
      return this.hotAuthors.length
    },
    latestTrendPoint() {
      return this.trendData.length
        ? this.trendData[this.trendData.length - 1]
        : { createdCount: 0, publishedCount: 0, viewCount: 0, reportCount: 0 }
    },
    previousTrendPoint() {
      return this.trendData.length > 1
        ? this.trendData[this.trendData.length - 2]
        : { createdCount: 0, publishedCount: 0, viewCount: 0, reportCount: 0 }
    },
    publishRate() {
      return this.calcRatio(this.publishedBlogsRaw, this.totalBlogsRaw)
    },
    rejectRate() {
      return this.calcRatio(this.rejectedBlogsRaw, this.totalBlogsRaw)
    },
    reviewPressure() {
      return this.calcRatio(this.pendingBlogsRaw, Math.max(this.totalBlogsRaw, 1))
    },
    reportPressure() {
      return this.calcRatio(this.pendingReportsRaw, Math.max(this.pendingReportsRaw + this.todayReportsRaw, 1))
    },
    latestReportSpike() {
      const current = safeNumber(this.latestTrendPoint.reportCount)
      const previous = safeNumber(this.previousTrendPoint.reportCount)
      if (!previous) {
        return current > 0 ? 100 : 0
      }
      return Math.round(((current - previous) / previous) * 100)
    },
    latestViewSpike() {
      const current = safeNumber(this.latestTrendPoint.viewCount)
      const previous = safeNumber(this.previousTrendPoint.viewCount)
      if (!previous) {
        return current > 0 ? 100 : 0
      }
      return Math.round(((current - previous) / previous) * 100)
    },
    commandHealthLevel() {
      if (this.pendingReportsRaw >= 3 || this.pendingBlogsRaw >= 6 || this.latestReportSpike >= 100) {
        return 'critical'
      }
      if (this.pendingReportsRaw > 0 || this.pendingBlogsRaw > 0 || this.publishRate < 65) {
        return 'warning'
      }
      if (this.latestViewSpike >= 60 || this.maxHotBlogViews >= 150) {
        return 'notice'
      }
      return 'stable'
    },
    commandHealthText() {
      const map = {
        critical: '高压态势',
        warning: '重点盯防',
        notice: '流量波动',
        stable: '运行稳定'
      }
      return map[this.commandHealthLevel] || '运行稳定'
    },
    metricCards() {
      return [
        {
          key: 'totalBlogs',
          label: '总博客数',
          value: this.formatNumber(this.displayMetrics.totalBlogs),
          badge: '全量',
          helper: '内容池规模',
          delta: `7 日新增 ${this.sumTrendValue('createdCount')}`,
          deltaTone: 'neutral',
          percent: clampPercent((this.totalBlogsRaw / Math.max(this.totalBlogsRaw, 12)) * 100),
          tone: 'primary'
        },
        {
          key: 'publishedBlogs',
          label: '已发布',
          value: this.formatNumber(this.displayMetrics.publishedBlogs),
          badge: this.publishRate >= 70 ? '稳定' : '关注',
          helper: '对外可见内容',
          delta: `发布率 ${this.publishRate}%`,
          deltaTone: this.publishRate >= 70 ? 'up' : 'down',
          percent: clampPercent(this.publishRate),
          tone: 'success'
        },
        {
          key: 'pendingBlogs',
          label: '待审核',
          value: this.formatNumber(this.displayMetrics.pendingBlogs),
          badge: this.pendingBlogsRaw > 0 ? '积压' : '清零',
          helper: '审核队列存量',
          delta: this.pendingBlogsRaw > 0 ? `压力 ${this.reviewPressure}%` : '当前无积压',
          deltaTone: this.pendingBlogsRaw > 0 ? 'down' : 'up',
          percent: clampPercent(this.reviewPressure),
          tone: 'warning'
        },
        {
          key: 'rejectedBlogs',
          label: '已驳回',
          value: this.formatNumber(this.displayMetrics.rejectedBlogs),
          badge: this.rejectedBlogsRaw > 0 ? '复核' : '平稳',
          helper: '质量回退内容',
          delta: `驳回率 ${this.rejectRate}%`,
          deltaTone: this.rejectRate > 20 ? 'down' : 'neutral',
          percent: clampPercent(this.rejectRate),
          tone: 'danger'
        },
        {
          key: 'pendingReports',
          label: '待处理举报',
          value: this.formatNumber(this.displayMetrics.pendingReports),
          badge: this.pendingReportsRaw > 0 ? '告警' : '正常',
          helper: '风险工单存量',
          delta: this.pendingReportsRaw > 0 ? `处置压力 ${this.reportPressure}%` : '举报清零',
          deltaTone: this.pendingReportsRaw > 0 ? 'down' : 'up',
          percent: clampPercent(this.reportPressure),
          tone: 'danger'
        },
        {
          key: 'todayReports',
          label: '今日举报',
          value: this.formatNumber(this.displayMetrics.todayReports),
          badge: this.latestReportSpike > 0 ? '波动' : '平稳',
          helper: '24 小时新增',
          delta: this.formatDeltaText(this.todayReportsRaw, safeNumber(this.previousTrendPoint.reportCount), false),
          deltaTone: this.latestReportSpike > 0 ? 'down' : 'neutral',
          percent: clampPercent(this.calcRatio(this.todayReportsRaw, Math.max(this.pendingReportsRaw, 1))),
          tone: 'warning'
        },
        {
          key: 'todayViews',
          label: '今日浏览',
          value: this.formatNumber(this.displayMetrics.todayViews),
          badge: this.latestViewSpike >= 60 ? '冲高' : '热度',
          helper: '内容传播强度',
          delta: this.formatDeltaText(this.todayViewsRaw, safeNumber(this.previousTrendPoint.viewCount), true),
          deltaTone: this.latestViewSpike >= 0 ? 'up' : 'neutral',
          percent: clampPercent((this.todayViewsRaw / Math.max(this.todayViewsRaw, this.maxHotBlogViews, 100)) * 100),
          tone: 'accent'
        },
        {
          key: 'activeAuthors',
          label: '活跃作者',
          value: this.formatNumber(this.displayMetrics.activeAuthors),
          badge: '热榜',
          helper: '当前进入排行榜作者',
          delta: this.hotAuthors[0] ? `${this.hotAuthors[0].name} 领跑` : '等待热榜样本',
          deltaTone: 'neutral',
          percent: clampPercent((this.activeAuthorsRaw / Math.max(this.activeAuthorsRaw, 5)) * 100),
          tone: 'violet'
        }
      ]
    },
    overviewTiles() {
      return [
        {
          label: '审核队列',
          value: this.formatNumber(this.pendingBlogsRaw),
          desc: this.pendingBlogsRaw > 0 ? '建议优先处理待审稿件' : '当前队列已清空',
          tone: this.pendingBlogsRaw > 0 ? 'warning' : 'stable'
        },
        {
          label: '举报工单',
          value: this.formatNumber(this.pendingReportsRaw),
          desc: this.pendingReportsRaw > 0 ? '存在待处置风险内容' : '风险工单处于低位',
          tone: this.pendingReportsRaw > 0 ? 'danger' : 'stable'
        },
        {
          label: '今日新增举报',
          value: this.formatNumber(this.todayReportsRaw),
          desc: this.latestReportSpike > 0 ? '今日举报较昨日抬升' : '今日波动平稳',
          tone: this.latestReportSpike > 0 ? 'warning' : 'stable'
        },
        {
          label: '内容发布率',
          value: `${this.publishRate}%`,
          desc: this.publishRate >= 70 ? '内容转化保持稳定' : '发布效率偏低',
          tone: this.publishRate >= 70 ? 'success' : 'warning'
        }
      ]
    },
    trendSummary() {
      return [
        {
          label: '7 日新增稿件',
          value: this.formatNumber(this.sumTrendValue('createdCount'))
        },
        {
          label: '7 日发布量',
          value: this.formatNumber(this.sumTrendValue('publishedCount'))
        },
        {
          label: '7 日浏览量',
          value: this.formatNumber(this.sumTrendValue('viewCount'))
        },
        {
          label: '7 日举报量',
          value: this.formatNumber(this.sumTrendValue('reportCount'))
        }
      ]
    },
    trendCards() {
      const items = [
        {
          key: 'createdCount',
          label: '新增稿件',
          current: safeNumber(this.latestTrendPoint.createdCount),
          previous: safeNumber(this.previousTrendPoint.createdCount),
          tone: 'accent'
        },
        {
          key: 'publishedCount',
          label: '发布数量',
          current: safeNumber(this.latestTrendPoint.publishedCount),
          previous: safeNumber(this.previousTrendPoint.publishedCount),
          tone: 'success'
        },
        {
          key: 'viewCount',
          label: '浏览趋势',
          current: safeNumber(this.latestTrendPoint.viewCount),
          previous: safeNumber(this.previousTrendPoint.viewCount),
          tone: 'primary'
        },
        {
          key: 'reportCount',
          label: '举报趋势',
          current: safeNumber(this.latestTrendPoint.reportCount),
          previous: safeNumber(this.previousTrendPoint.reportCount),
          tone: 'warning'
        }
      ]

      return items.map(item => {
        const geometry = this.buildSparklineGeometry(this.trendData.map(point => safeNumber(point[item.key])))
        const deltaPositive = item.key !== 'reportCount'
        return {
          key: item.key,
          label: item.label,
          value: this.formatNumber(item.current),
          delta: this.formatDeltaText(item.current, item.previous, deltaPositive),
          deltaTone: this.resolveDeltaTone(item.current, item.previous, deltaPositive),
          tone: item.tone,
          linePoints: geometry.linePoints,
          areaPoints: geometry.areaPoints,
          dots: geometry.dots
        }
      })
    },
    trendLabels() {
      return this.trendData.map(item => this.formatTrendLabel(item.statDate))
    },
    alertItems() {
      const items = []

      if (this.pendingReportsRaw >= 3) {
        items.push({
          level: 'critical',
          title: '举报工单高压',
          badge: `${this.pendingReportsRaw} 条`,
          desc: '待处理举报已进入高压区间，建议立即进入审核页优先处理头部内容。',
          hint: '优先动作：处置举报 > 检查同作者关联内容'
        })
      } else if (this.pendingReportsRaw > 0) {
        items.push({
          level: 'warning',
          title: '举报待处理',
          badge: `${this.pendingReportsRaw} 条`,
          desc: '当前仍存在待处理举报，建议在内容审核流程中插队处理。',
          hint: '优先动作：核查被举报原因与热榜内容联动'
        })
      }

      if (this.pendingBlogsRaw >= 6) {
        items.push({
          level: 'critical',
          title: '审核队列积压',
          badge: `${this.pendingBlogsRaw} 篇`,
          desc: '待审稿件已出现明显积压，审核周期可能继续拉长。',
          hint: '优先动作：开启集中审核，先清理高热和高风险稿件'
        })
      } else if (this.pendingBlogsRaw > 0) {
        items.push({
          level: 'warning',
          title: '审核队列待清理',
          badge: `${this.pendingBlogsRaw} 篇`,
          desc: '仍有博客等待审核，建议在当前班次内完成队列清空。',
          hint: '优先动作：进入审核中心逐条处理'
        })
      }

      if (this.latestReportSpike >= 100 && this.todayReportsRaw > 0) {
        items.push({
          level: 'critical',
          title: '举报量突增',
          badge: `${this.latestReportSpike}%`,
          desc: '今日举报较昨日明显抬升，可能存在内容争议或热点外溢。',
          hint: '优先动作：检查热榜头部内容与举报原因'
        })
      }

      if (this.latestViewSpike >= 60 && this.todayViewsRaw > 0) {
        items.push({
          level: 'notice',
          title: '流量冲高',
          badge: `${this.latestViewSpike}%`,
          desc: '今日浏览相对昨日出现放大，建议同步观察互动与举报联动变化。',
          hint: '优先动作：检查热榜内容评论与举报状态'
        })
      }

      if (this.publishRate < 65 && this.totalBlogsRaw >= 3) {
        items.push({
          level: 'warning',
          title: '发布效率偏低',
          badge: `${this.publishRate}%`,
          desc: '已发布内容占比偏低，说明内容转化效率仍有提升空间。',
          hint: '优先动作：核查驳回原因和审核时效'
        })
      }

      if (!items.length) {
        items.push({
          level: 'stable',
          title: '整体运行稳定',
          badge: 'OK',
          desc: '审核、举报、趋势与热榜数据均处于稳定区间，暂无明显风险点。',
          hint: '优先动作：持续观察热榜和作者热度变化'
        })
      }

      const weights = {
        critical: 4,
        warning: 3,
        notice: 2,
        stable: 1
      }

      return items
        .sort((a, b) => (weights[b.level] || 0) - (weights[a.level] || 0))
        .slice(0, 4)
    },
    quickActions() {
      return [
        {
          label: '博客审核',
          value: this.formatNumber(this.pendingBlogsRaw),
          desc: '进入审核中心处理待审和举报工单',
          handler: this.gotoAudit,
          tone: 'warning'
        },
        {
          label: '快速发文',
          value: this.formatNumber(this.publishedBlogsRaw),
          desc: '打开创作入口补充内容供给',
          handler: this.gotoWrite,
          tone: 'accent'
        },
        {
          label: '用户管理',
          value: this.formatNumber(this.activeAuthorsRaw),
          desc: '查看热榜作者与账号状态',
          handler: this.gotoUserManage,
          tone: 'primary'
        },
        {
          label: '系统日志',
          value: this.formatNumber(this.pendingReportsRaw + this.pendingBlogsRaw),
          desc: '检查后台异常与处理闭环',
          handler: this.gotoSettings,
          tone: 'danger'
        }
      ]
    },
    activityFeed() {
      if (this.activityItems.length) {
        return this.activityItems.map(item => ({
          title: item.title || '最新动态',
          desc: item.desc || '暂无更多说明',
          time: this.formatShortTime(item.time),
          level: item.level || 'notice',
          category: item.category || ''
        }))
      }

      const rows = []

      rows.push({
        title: '仪表盘同步完成',
        desc: this.lastUpdatedAt ? `最近同步时间 ${this.lastUpdatedText}` : '等待首轮数据加载完成',
        time: this.lastUpdatedAt ? this.formatShortTime(this.lastUpdatedAt) : '--:--'
      })

      if (this.featuredBlog) {
        rows.push({
          title: '热榜头部更新',
          desc: `《${this.featuredBlog.title}》当前浏览 ${this.formatNumber(this.featuredBlog.viewCount || 0)}，保持榜首。`,
          time: this.formatShortTime(this.featuredBlog.publishTime || this.featuredBlog.createdAt)
        })
      }

      if (this.pendingBlogRows[0]) {
        rows.push({
          title: '待审稿件进入队列',
          desc: `《${this.pendingBlogRows[0].title || '未命名博客'}》等待审核，作者 ${this.getAuthorName(this.pendingBlogRows[0].author)}。`,
          time: this.formatShortTime(this.pendingBlogRows[0].createdAt)
        })
      }

      if (this.pendingReportRows[0]) {
        rows.push({
          title: '举报工单待处理',
          desc: `《${this.pendingReportRows[0].targetTitle || '未命名内容'}》被举报，原因：${this.pendingReportRows[0].reason || '未填写'}。`,
          time: this.formatShortTime(this.pendingReportRows[0].createdAt)
        })
      }

      if (this.hotAuthors[0]) {
        rows.push({
          title: '作者热度领跑',
          desc: `${this.hotAuthors[0].name} 当前累计热度 ${this.formatNumber(this.hotAuthors[0].heat || 0)}。`,
          time: this.currentTimeText
        })
      }

      if (this.hotTags[0]) {
        rows.push({
          title: '标签关注上升',
          desc: `标签 ${this.hotTags[0].name} 已命中 ${this.formatNumber(this.hotTags[0].count || 0)} 次。`,
          time: this.currentTimeText
        })
      }

      return rows.slice(0, 6)
    }
  },
  mounted() {
    this.fetchAll()
    this.startClock()
    this.startAutoRefresh()
    document.addEventListener('fullscreenchange', this.handleFullscreenChange)
  },
  beforeDestroy() {
    this.stopClock()
    this.stopAutoRefresh()
    this.stopMetricAnimation()
    document.removeEventListener('fullscreenchange', this.handleFullscreenChange)
  },
  methods: {
    async fetchAll() {
      this.loading = true

      try {
        const results = await Promise.allSettled([
          this.$axios.get('/api/blogs/admin/stats/overview'),
          this.$axios.get('/api/blogs/admin/stats/trend', { params: { days: 7 } }),
          this.$axios.get('/api/blogs/admin/rank/overview', { params: { top: 8 } }),
          this.$axios.get('/api/blogs/pending', { params: { page: 0, size: 5 } }),
          this.$axios.get('/api/blogs/admin/reports/pending'),
          this.$axios.get('/api/blogs/admin/activity/recent', { params: { limit: 6 } })
        ])

        const [overviewRes, trendRes, rankRes, pendingRes, reportRes, activityRes] = results

        if (overviewRes.status === 'fulfilled') {
          this.overviewData = unwrapPayload(overviewRes.value) || {}
        }

        if (trendRes.status === 'fulfilled') {
          const payload = unwrapPayload(trendRes.value)
          this.trendData = Array.isArray(payload) ? payload : []
        }

        if (rankRes.status === 'fulfilled') {
          const payload = unwrapPayload(rankRes.value) || {}
          this.rankData = {
            hotBlogs: Array.isArray(payload.hotBlogs) ? payload.hotBlogs : [],
            hotAuthors: Array.isArray(payload.hotAuthors) ? payload.hotAuthors : [],
            hotTags: Array.isArray(payload.hotTags) ? payload.hotTags : []
          }
        }

        if (pendingRes.status === 'fulfilled') {
          const payload = unwrapPayload(pendingRes.value)
          this.pendingBlogRows = this.extractPageRecords(payload).slice(0, 5)
        }

        if (reportRes.status === 'fulfilled') {
          const payload = unwrapPayload(reportRes.value)
          this.pendingReportRows = Array.isArray(payload) ? payload.slice(0, 5) : []
        }

        if (activityRes.status === 'fulfilled') {
          const payload = unwrapPayload(activityRes.value)
          this.activityItems = Array.isArray(payload) ? payload.slice(0, 6) : []
        }

        this.animateMetrics({
          totalBlogs: this.totalBlogsRaw,
          publishedBlogs: this.publishedBlogsRaw,
          pendingBlogs: this.pendingBlogsRaw,
          rejectedBlogs: this.rejectedBlogsRaw,
          pendingReports: this.pendingReportsRaw,
          todayReports: this.todayReportsRaw,
          todayViews: this.todayViewsRaw,
          activeAuthors: this.activeAuthorsRaw
        })

        this.lastUpdatedAt = new Date()
      } catch (error) {
        console.error('加载博客仪表盘失败', error)
        this.$message.error('加载仪表盘失败')
      } finally {
        this.loading = false
      }
    },
    extractPageRecords(payload) {
      if (!payload) return []
      if (Array.isArray(payload.content)) return payload.content
      if (Array.isArray(payload.records)) return payload.records
      if (Array.isArray(payload.list)) return payload.list
      if (Array.isArray(payload.items)) return payload.items
      return Array.isArray(payload) ? payload : []
    },
    startClock() {
      this.stopClock()
      this.clockTimer = window.setInterval(() => {
        this.currentTime = new Date()
      }, 1000)
    },
    stopClock() {
      if (this.clockTimer) {
        window.clearInterval(this.clockTimer)
        this.clockTimer = null
      }
    },
    startAutoRefresh() {
      this.stopAutoRefresh()
      this.refreshTimer = window.setInterval(() => {
        this.fetchAll()
      }, 60000)
    },
    stopAutoRefresh() {
      if (this.refreshTimer) {
        window.clearInterval(this.refreshTimer)
        this.refreshTimer = null
      }
    },
    handleFullscreenChange() {
      this.isFullscreen = !!document.fullscreenElement
    },
    async toggleFullscreen() {
      const target = this.$refs.screenRoot
      if (!target) return

      try {
        if (document.fullscreenElement) {
          await document.exitFullscreen()
        } else if (target.requestFullscreen) {
          await target.requestFullscreen()
        }
      } catch (error) {
        this.$message.warning('当前环境暂不支持全屏切换')
      }
    },
    stopMetricAnimation() {
      if (this.metricAnimationFrame) {
        window.cancelAnimationFrame(this.metricAnimationFrame)
        this.metricAnimationFrame = null
      }
    },
    animateMetrics(targets) {
      this.stopMetricAnimation()

      const startValues = { ...this.displayMetrics }
      const duration = 900
      const startAt = performance.now()

      const step = now => {
        const progress = clamp((now - startAt) / duration, 0, 1)
        const eased = 1 - Math.pow(1 - progress, 3)
        const nextValues = {}

        Object.keys(targets).forEach(key => {
          const startValue = safeNumber(startValues[key])
          const targetValue = safeNumber(targets[key])
          nextValues[key] = Math.round(startValue + (targetValue - startValue) * eased)
        })

        this.displayMetrics = {
          ...this.displayMetrics,
          ...nextValues
        }

        if (progress < 1) {
          this.metricAnimationFrame = window.requestAnimationFrame(step)
        } else {
          this.metricAnimationFrame = null
        }
      }

      this.metricAnimationFrame = window.requestAnimationFrame(step)
    },
    calcRatio(value, base) {
      const safeBase = safeNumber(base)
      if (!safeBase) return 0
      return Math.round((safeNumber(value) / safeBase) * 100)
    },
    calcRankPercent(value, max) {
      return clampPercent((safeNumber(value) / Math.max(safeNumber(max), 1)) * 100)
    },
    sumTrendValue(key) {
      return this.trendData.reduce((sum, item) => sum + safeNumber(item[key]), 0)
    },
    buildSparklineGeometry(values) {
      const source = Array.isArray(values) && values.length ? values : [0]
      const width = 220
      const height = 76
      const padding = 8
      const maxValue = Math.max(...source.map(value => safeNumber(value)), 1)
      const stepX = source.length > 1 ? (width - padding * 2) / (source.length - 1) : 0

      const dots = source.map((value, index) => {
        const x = padding + stepX * index
        const y = height - padding - (safeNumber(value) / maxValue) * (height - padding * 2)
        return {
          x: Number(x.toFixed(2)),
          y: Number(y.toFixed(2))
        }
      })

      const linePoints = dots.map(dot => `${dot.x},${dot.y}`).join(' ')
      const areaPoints = [
        `${padding},${height - padding}`,
        ...dots.map(dot => `${dot.x},${dot.y}`),
        `${dots[dots.length - 1].x},${height - padding}`
      ].join(' ')

      return {
        linePoints,
        areaPoints,
        dots
      }
    },
    resolveDeltaTone(current, previous, positiveIsGood = true) {
      if (current === previous) {
        return 'neutral'
      }
      if (current > previous) {
        return positiveIsGood ? 'up' : 'down'
      }
      return positiveIsGood ? 'down' : 'up'
    },
    formatDeltaText(current, previous, positiveIsGood = true) {
      const currentValue = safeNumber(current)
      const previousValue = safeNumber(previous)
      if (currentValue === previousValue) {
        return '较昨日持平'
      }
      const diff = Math.abs(currentValue - previousValue)
      const direction = currentValue > previousValue ? '上升' : '下降'
      const prefix = currentValue > previousValue && positiveIsGood ? '+' : currentValue > previousValue ? '风险' : '优化'
      if (previousValue === 0) {
        return currentValue > 0 ? `${direction} ${this.formatNumber(diff)}` : '较昨日持平'
      }
      return `${prefix} ${this.formatNumber(diff)}`
    },
    formatTrendLabel(value) {
      if (!value) return '--'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) {
        return String(value).slice(5)
      }
      return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
    },
    getAuthorName(author) {
      if (!author) return '未知作者'
      if (typeof author === 'string') return author
      return author.displayName || author.nickname || author.username || '未知作者'
    },
    formatNumber(value) {
      return safeNumber(value).toLocaleString('zh-CN')
    },
    formatShortTime(value) {
      if (!value) return '--:--'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return '--:--'
      return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
    },
    formatShortDate(value) {
      if (!value) return '--'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return '--'
      return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
    },
    formatDateTime(value) {
      if (!value) return ''
      const date = value instanceof Date ? value : new Date(value)
      if (Number.isNaN(date.getTime())) return ''
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    },
    gotoAudit() {
      this.$router.push('/admin/content/blog/audit')
    },
    gotoWrite() {
      this.$router.push('/blogwrite')
    },
    gotoUserManage() {
      this.$router.push('/admin/users/account')
    },
    gotoSettings() {
      this.$router.push('/admin/system/log')
    }
  }
}
</script>

<style scoped>
.screen-dashboard {
  --screen-panel: color-mix(in srgb, var(--it-panel-bg-strong, var(--it-surface-solid)) 92%, rgba(7, 21, 41, 0.08));
  --screen-border: color-mix(in srgb, var(--it-border-strong, var(--it-border)) 74%, rgba(36, 90, 150, 0.42));
  --screen-text: var(--it-text);
  --screen-text-soft: var(--it-text-subtle);
  --screen-title: color-mix(in srgb, var(--it-text) 92%, white 8%);
  --screen-accent: var(--it-accent, #58b7ff);
  --screen-accent-soft: color-mix(in srgb, var(--it-accent-soft, rgba(88, 183, 255, 0.16)) 88%, transparent);
  --screen-shadow: 0 18px 42px color-mix(in srgb, var(--it-shadow, rgba(15, 23, 42, 0.18)) 72%, rgba(7, 17, 31, 0.08));
  position: relative;
  min-height: calc(100vh - 84px);
  padding: 24px;
  overflow: hidden;
  color: var(--screen-text);
  background:
    radial-gradient(circle at 14% 12%, color-mix(in srgb, var(--screen-accent) 12%, transparent), transparent 28%),
    radial-gradient(circle at 86% 18%, color-mix(in srgb, #7f89ff 14%, transparent), transparent 30%),
    linear-gradient(180deg, color-mix(in srgb, var(--it-page-bg) 94%, white 6%) 0%, var(--it-page-bg) 100%);
}

.screen-dashboard__bg,
.screen-dashboard__orb,
.screen-dashboard__grid {
  pointer-events: none;
  position: absolute;
  inset: 0;
}

.screen-dashboard__orb {
  filter: blur(72px);
  opacity: 0.38;
}

.screen-dashboard__orb--left {
  inset: 80px auto auto 6%;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--screen-accent) 48%, transparent);
}

.screen-dashboard__orb--right {
  inset: 120px 4% auto auto;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: rgba(126, 118, 255, 0.34);
}

.screen-dashboard__grid {
  background-image:
    linear-gradient(color-mix(in srgb, var(--it-border) 65%, transparent) 1px, transparent 1px),
    linear-gradient(90deg, color-mix(in srgb, var(--it-border) 65%, transparent) 1px, transparent 1px);
  background-size: 72px 72px;
  opacity: 0.1;
}

.screen-dashboard.is-fullscreen,
.screen-dashboard:fullscreen {
  min-height: 100vh;
  padding: 28px;
  background:
    radial-gradient(circle at 14% 12%, rgba(30, 157, 255, 0.18), transparent 30%),
    radial-gradient(circle at 86% 20%, rgba(107, 117, 255, 0.18), transparent 32%),
    linear-gradient(160deg, #081220 0%, #0b1d31 42%, #071019 100%);
  --screen-panel: rgba(10, 26, 48, 0.88);
  --screen-border: rgba(87, 163, 255, 0.24);
  --screen-text: #edf6ff;
  --screen-text-soft: rgba(208, 227, 245, 0.72);
  --screen-title: #ffffff;
  --screen-shadow: 0 20px 48px rgba(3, 12, 24, 0.38);
}

.panel-shell {
  position: relative;
  z-index: 1;
  border: 1px solid var(--screen-border);
  border-radius: 24px;
  background: var(--screen-panel);
  box-shadow: var(--screen-shadow);
  backdrop-filter: blur(16px);
}

.dashboard-hero,
.dashboard-main,
.dashboard-footer,
.metric-grid {
  position: relative;
  z-index: 1;
}

.dashboard-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(360px, 0.95fr);
  gap: 24px;
  padding: 28px 30px;
  background:
    linear-gradient(
      135deg,
      color-mix(in srgb, #102b48 82%, var(--screen-accent) 8%),
      color-mix(in srgb, #0c233d 86%, var(--screen-panel) 14%)
    );
  margin-bottom: 22px;
}

.dashboard-hero__main,
.dashboard-hero__side {
  display: flex;
  flex-direction: column;
}

.dashboard-hero__main {
  gap: 16px;
  justify-content: center;
}

.dashboard-hero__side {
  gap: 14px;
  align-items: stretch;
}

.panel-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  line-height: 1;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: color-mix(in srgb, var(--screen-accent) 72%, white 28%);
}

.dashboard-hero h1,
.panel-heading h2 {
  margin: 0;
  font-size: 36px;
  line-height: 1.16;
  color: var(--screen-title);
}

.dashboard-hero p {
  margin: 0;
  max-width: 620px;
  font-size: 15px;
  line-height: 1.72;
  color: var(--screen-text-soft);
}

.hero-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-chip,
.panel-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--screen-accent) 22%, var(--screen-border));
  background: color-mix(in srgb, var(--screen-accent-soft) 70%, transparent);
  color: color-mix(in srgb, var(--screen-accent) 88%, white 12%);
  font-size: 12px;
  font-weight: 600;
}

.time-card,
.status-pill {
  border-radius: 18px;
  border: 1px solid color-mix(in srgb, var(--screen-border) 92%, transparent);
  background: color-mix(in srgb, var(--screen-panel) 88%, rgba(255, 255, 255, 0.04));
}

.time-card {
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.time-card__label {
  font-size: 12px;
  color: var(--screen-text-soft);
}

.time-card__value {
  font-size: 42px;
  line-height: 1;
  letter-spacing: 0.04em;
  color: color-mix(in srgb, var(--screen-accent) 60%, #dff6ff 40%);
  text-shadow: 0 0 16px color-mix(in srgb, var(--screen-accent) 28%, transparent);
}

.time-card__sub {
  color: var(--screen-text-soft);
  font-size: 13px;
}

.hero-statuses {
  display: grid;
  gap: 12px;
}

.status-pill {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 52px;
  padding: 0 18px;
  font-size: 13px;
  color: var(--screen-text-soft);
}

.status-pill strong {
  color: var(--screen-title);
  font-size: 13px;
}

.status-pill__dot {
  width: 10px;
  height: 10px;
  margin-right: 8px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--it-warning, #e6a23c) 70%, #fff 30%);
  box-shadow: 0 0 0 6px color-mix(in srgb, var(--it-warning-soft, rgba(230, 162, 60, 0.18)) 85%, transparent);
}

.status-pill__dot--live {
  background: color-mix(in srgb, var(--it-success, #12a594) 72%, #fff 28%);
  box-shadow: 0 0 0 6px color-mix(in srgb, var(--it-success-soft, rgba(18, 165, 148, 0.16)) 88%, transparent);
}

.hero-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.metric-card {
  padding: 18px 18px 16px;
  overflow: hidden;
}

.metric-card::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 3px;
  background: linear-gradient(90deg, transparent, var(--metric-color, var(--screen-accent)), transparent);
  opacity: 0.95;
}

.metric-card::after {
  content: '';
  position: absolute;
  inset: -32% auto auto -12%;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: radial-gradient(circle, color-mix(in srgb, var(--metric-color, var(--screen-accent)) 16%, transparent), transparent 68%);
  opacity: 0.55;
}

.metric-card--primary { --metric-color: #54b3ff; }
.metric-card--success { --metric-color: #35d0a8; }
.metric-card--warning { --metric-color: #f3b45b; }
.metric-card--danger { --metric-color: #f47f7f; }
.metric-card--accent { --metric-color: #5fc8ff; }
.metric-card--violet { --metric-color: #8f88ff; }

.metric-card__header,
.metric-card__footer {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.metric-card__label {
  font-size: 13px;
  color: var(--screen-text-soft);
}

.metric-card__badge {
  padding: 5px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--metric-color) 12%, transparent);
  color: color-mix(in srgb, var(--metric-color) 82%, white 18%);
  font-size: 12px;
  font-weight: 600;
}

.metric-card__value {
  position: relative;
  z-index: 1;
  margin-top: 14px;
  font-size: 42px;
  line-height: 1;
  font-weight: 700;
  color: var(--screen-title);
}

.metric-card__footer {
  margin-top: 14px;
  color: var(--screen-text-soft);
  font-size: 12px;
}

.metric-card__delta.is-up,
.trend-card__delta.is-up {
  color: color-mix(in srgb, var(--it-success, #12a594) 84%, white 16%);
}

.metric-card__delta.is-down,
.trend-card__delta.is-down {
  color: color-mix(in srgb, var(--it-danger, #f56c6c) 88%, white 12%);
}

.metric-card__delta.is-neutral,
.trend-card__delta.is-neutral {
  color: var(--screen-text-soft);
}

.metric-card__bar {
  position: relative;
  z-index: 1;
  margin-top: 14px;
  height: 8px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--it-border) 80%, transparent);
  overflow: hidden;
}

.metric-card__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--metric-color), color-mix(in srgb, var(--metric-color) 55%, #ffffff 45%));
  box-shadow: 0 0 16px color-mix(in srgb, var(--metric-color) 42%, transparent);
}

.dashboard-main {
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(0, 1.48fr) minmax(330px, 0.9fr);
  gap: 18px;
}

.dashboard-side {
  display: grid;
  gap: 18px;
}

.command-panel,
.trend-panel,
.alert-panel,
.ranking-panel,
.action-panel,
.feed-panel {
  padding: 22px;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-heading h2 {
  font-size: 24px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.overview-tile {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--screen-border);
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--screen-panel) 88%, transparent),
    color-mix(in srgb, var(--it-surface-elevated, var(--it-surface-solid)) 92%, transparent)
  );
}

.overview-tile--warning {
  border-color: color-mix(in srgb, var(--it-warning, #e6a23c) 28%, var(--screen-border));
}

.overview-tile--danger {
  border-color: color-mix(in srgb, var(--it-danger, #f56c6c) 28%, var(--screen-border));
}

.overview-tile--success,
.overview-tile--stable {
  border-color: color-mix(in srgb, var(--it-success, #12a594) 24%, var(--screen-border));
}

.overview-tile__label,
.overview-tile__desc,
.queue-card__header span,
.queue-item__meta,
.queue-item__desc,
.hot-board__sub,
.ranking-row__copy span,
.feed-row__desc,
.feed-row__time,
.trend-summary__item span {
  color: var(--screen-text-soft);
}

.overview-tile__label {
  display: block;
  font-size: 12px;
}

.overview-tile__value {
  display: block;
  margin-top: 10px;
  font-size: 34px;
  line-height: 1;
  color: var(--screen-title);
}

.overview-tile__desc {
  display: block;
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.5;
}

.queue-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.queue-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--screen-border);
  background: linear-gradient(
    180deg,
    color-mix(in srgb, var(--screen-panel) 90%, transparent),
    color-mix(in srgb, var(--it-surface-elevated, var(--it-surface-solid)) 94%, transparent)
  );
}

.queue-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.queue-card__header h3,
.ranking-section__title {
  margin: 0;
  font-size: 15px;
  color: var(--screen-title);
}

.queue-list {
  display: grid;
  gap: 10px;
}

.queue-item {
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid color-mix(in srgb, var(--screen-border) 78%, transparent);
  background: color-mix(in srgb, var(--screen-panel) 88%, transparent);
}

.queue-item__title,
.hot-board__title,
.feed-row__title,
.ranking-row__copy strong {
  color: var(--screen-title);
}

.queue-item__title,
.hot-board__title {
  font-size: 14px;
  font-weight: 600;
}

.queue-item__desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
}

.queue-item__meta,
.hot-board__sub {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  font-size: 12px;
}

.trend-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.trend-summary__item {
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--screen-border);
  background: color-mix(in srgb, var(--screen-panel) 90%, transparent);
}

.trend-summary__item strong {
  display: block;
  margin-top: 6px;
  font-size: 24px;
  color: var(--screen-title);
}

.trend-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.trend-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--screen-border);
  background: linear-gradient(
    180deg,
    color-mix(in srgb, var(--screen-panel) 88%, transparent),
    color-mix(in srgb, var(--it-surface-elevated, var(--it-surface-solid)) 92%, transparent)
  );
  overflow: hidden;
}

.trend-card--accent { --trend-color: #55c9ff; }
.trend-card--success { --trend-color: #35d0a8; }
.trend-card--primary { --trend-color: #599dff; }
.trend-card--warning { --trend-color: #f5b55b; }

.trend-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.trend-card__label {
  display: block;
  font-size: 12px;
  color: var(--screen-text-soft);
}

.trend-card__value {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  line-height: 1;
  color: var(--screen-title);
}

.trend-card__delta {
  font-size: 12px;
  font-weight: 600;
}

.trend-card__chart {
  display: block;
  width: 100%;
  height: 76px;
  margin-top: 12px;
}

.trend-card__area {
  fill: color-mix(in srgb, var(--trend-color) 18%, transparent);
}

.trend-card__line {
  fill: none;
  stroke: var(--trend-color);
  stroke-width: 2.5;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.trend-card__dot {
  fill: var(--trend-color);
  stroke: rgba(255, 255, 255, 0.72);
  stroke-width: 1;
}

.trend-axis {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
  color: var(--screen-text-soft);
  font-size: 12px;
  text-align: center;
}

.hot-board {
  margin-top: 18px;
}

.hot-board__featured {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) 140px;
  gap: 18px;
  align-items: center;
  padding: 20px;
  border-radius: 20px;
  border: 1px solid color-mix(in srgb, var(--screen-accent) 22%, var(--screen-border));
  background: linear-gradient(
    135deg,
    color-mix(in srgb, #123964 80%, var(--screen-accent) 10%),
    color-mix(in srgb, #0b243f 82%, var(--screen-panel) 18%)
  );
}

.hot-board__rank {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  border-radius: 22px;
  background: color-mix(in srgb, var(--screen-accent) 26%, transparent);
  color: #ffffff;
  font-size: 34px;
  font-weight: 700;
}

.hot-board__main h3 {
  margin: 0;
  font-size: 22px;
  line-height: 1.35;
  color: #ffffff;
}

.hot-board__main p {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: rgba(224, 239, 255, 0.78);
}

.hot-board__meta {
  display: flex;
  gap: 14px;
  margin-top: 12px;
  color: rgba(222, 237, 255, 0.72);
  font-size: 12px;
}

.hot-board__stats {
  display: grid;
  gap: 12px;
}

.hot-board__stats > div {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(126, 186, 255, 0.16);
  background: rgba(6, 18, 35, 0.34);
}

.hot-board__stats span {
  display: block;
  color: rgba(220, 236, 255, 0.68);
  font-size: 12px;
}

.hot-board__stats strong {
  display: block;
  margin-top: 6px;
  font-size: 30px;
  line-height: 1;
  color: #ffffff;
}

.hot-board__list {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.hot-board__row,
.ranking-row {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) 76px;
  align-items: center;
  gap: 12px;
  padding: 12px 14px 10px;
  border-radius: 16px;
  border: 1px solid var(--screen-border);
  background: color-mix(in srgb, var(--screen-panel) 90%, transparent);
}

.hot-board__index,
.ranking-row__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--screen-accent-soft) 84%, transparent);
  color: color-mix(in srgb, var(--screen-accent) 82%, white 18%);
  font-size: 13px;
  font-weight: 700;
}

.hot-board__value,
.ranking-row__value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
  color: var(--screen-title);
  text-align: right;
}

.hot-board__bar,
.ranking-row__bar {
  grid-column: 2 / 4;
  height: 7px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--it-border) 82%, transparent);
  overflow: hidden;
}

.hot-board__bar span,
.ranking-row__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--screen-accent), color-mix(in srgb, var(--screen-accent) 48%, #ffffff 52%));
}

.alert-stack,
.ranking-section {
  display: grid;
  gap: 12px;
}

.alert-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--screen-border);
  background: color-mix(in srgb, var(--screen-panel) 90%, transparent);
}

.alert-card--critical {
  border-color: color-mix(in srgb, var(--it-danger, #f56c6c) 34%, var(--screen-border));
  background: linear-gradient(135deg, rgba(112, 34, 34, 0.18), color-mix(in srgb, var(--screen-panel) 90%, transparent));
}

.alert-card--warning {
  border-color: color-mix(in srgb, var(--it-warning, #e6a23c) 34%, var(--screen-border));
  background: linear-gradient(135deg, rgba(117, 78, 24, 0.16), color-mix(in srgb, var(--screen-panel) 90%, transparent));
}

.alert-card--notice {
  border-color: color-mix(in srgb, var(--screen-accent) 30%, var(--screen-border));
}

.alert-card--stable {
  border-color: color-mix(in srgb, var(--it-success, #12a594) 26%, var(--screen-border));
}

.alert-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--screen-title);
}

.alert-card__header span {
  color: var(--screen-text-soft);
  font-size: 12px;
}

.alert-card p {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.72;
  color: var(--screen-text-soft);
}

.alert-card__foot {
  margin-top: 10px;
  font-size: 12px;
  color: color-mix(in srgb, var(--screen-accent) 74%, white 26%);
}

.ranking-panel {
  display: grid;
  gap: 16px;
}

.ranking-list {
  display: grid;
  gap: 10px;
}

.ranking-row__main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ranking-row__copy {
  display: grid;
  gap: 4px;
}

.dashboard-footer {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  gap: 18px;
  margin-top: 18px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.action-card {
  position: relative;
  padding: 18px;
  text-align: left;
  border-radius: 18px;
  border: 1px solid var(--screen-border);
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--screen-panel) 90%, transparent),
    color-mix(in srgb, var(--it-surface-elevated, var(--it-surface-solid)) 94%, transparent)
  );
  color: var(--screen-text);
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
  cursor: pointer;
}

.action-card:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--screen-accent) 30%, var(--screen-border));
  box-shadow: 0 18px 36px color-mix(in srgb, var(--screen-accent-soft) 44%, transparent);
}

.action-card--warning::before,
.action-card--accent::before,
.action-card--primary::before,
.action-card--danger::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 3px;
  border-radius: 18px 18px 0 0;
}

.action-card--warning::before { background: linear-gradient(90deg, #f5b55b, transparent); }
.action-card--accent::before { background: linear-gradient(90deg, #56c6ff, transparent); }
.action-card--primary::before { background: linear-gradient(90deg, #5f9dff, transparent); }
.action-card--danger::before { background: linear-gradient(90deg, #f58484, transparent); }

.action-card__label {
  display: block;
  font-size: 13px;
  color: var(--screen-text-soft);
}

.action-card__value {
  display: block;
  margin-top: 10px;
  font-size: 34px;
  line-height: 1;
  color: var(--screen-title);
}

.action-card__desc {
  display: block;
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--screen-text-soft);
}

.feed-list {
  display: grid;
  gap: 12px;
}

.feed-row {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr) 72px;
  gap: 12px;
  align-items: start;
  padding: 14px 0;
  border-bottom: 1px solid color-mix(in srgb, var(--screen-border) 70%, transparent);
}

.feed-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.feed-row__dot {
  width: 10px;
  height: 10px;
  margin-top: 8px;
  border-radius: 50%;
  background: var(--screen-accent);
  box-shadow: 0 0 0 6px color-mix(in srgb, var(--screen-accent-soft) 88%, transparent);
}

.feed-row--critical .feed-row__dot,
.feed-row--danger .feed-row__dot {
  background: color-mix(in srgb, var(--it-danger, #f56c6c) 82%, white 18%);
  box-shadow: 0 0 0 6px color-mix(in srgb, var(--it-danger-soft, rgba(245, 108, 108, 0.14)) 90%, transparent);
}

.feed-row--warning .feed-row__dot {
  background: color-mix(in srgb, var(--it-warning, #e6a23c) 84%, white 16%);
  box-shadow: 0 0 0 6px color-mix(in srgb, var(--it-warning-soft, rgba(230, 162, 60, 0.14)) 88%, transparent);
}

.feed-row--success .feed-row__dot {
  background: color-mix(in srgb, var(--it-success, #12a594) 82%, white 18%);
  box-shadow: 0 0 0 6px color-mix(in srgb, var(--it-success-soft, rgba(18, 165, 148, 0.12)) 88%, transparent);
}

.feed-row__content {
  display: grid;
  gap: 6px;
}

.feed-row__title {
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.feed-row__category {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--screen-accent-soft) 84%, transparent);
  color: color-mix(in srgb, var(--screen-accent) 86%, white 14%);
  font-size: 11px;
  font-weight: 700;
}

.feed-row__desc,
.feed-row__time {
  font-size: 12px;
  line-height: 1.7;
}

.panel-empty {
  padding: 22px 16px;
  border-radius: 16px;
  border: 1px dashed color-mix(in srgb, var(--screen-border) 82%, transparent);
  background: color-mix(in srgb, var(--screen-panel) 78%, transparent);
  color: var(--screen-text-soft);
  text-align: center;
  font-size: 13px;
}

@media (max-width: 1680px) {
  .dashboard-main {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1.15fr);
  }

  .dashboard-side {
    grid-column: 1 / -1;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1440px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-hero {
    grid-template-columns: minmax(0, 1fr);
  }

  .trend-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-footer {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1180px) {
  .dashboard-main,
  .dashboard-side,
  .queue-grid,
  .trend-card-grid,
  .action-grid {
    grid-template-columns: 1fr;
  }

  .hot-board__featured {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .screen-dashboard {
    padding: 16px;
  }

  .metric-grid,
  .overview-grid,
  .trend-summary {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    flex-direction: column;
  }

  .hot-board__row,
  .ranking-row,
  .feed-row {
    grid-template-columns: 1fr;
  }

  .hot-board__bar,
  .ranking-row__bar {
    grid-column: auto;
  }
}
</style>
