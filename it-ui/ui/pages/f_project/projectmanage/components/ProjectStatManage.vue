<template>
  <div class="feature-page">
    <div class="overview-grid">
      <div
        v-for="card in overviewCards"
        :key="card.key"
        class="overview-card"
        :style="{ '--overview-accent': card.color, '--overview-soft': card.soft, '--overview-border': card.border }"
      >
        <div class="overview-card-label">{{ card.label }}</div>
        <div class="overview-card-value">{{ card.value }}</div>
        <div class="overview-card-desc">{{ card.desc }}</div>
      </div>
    </div>

    <el-card shadow="never">
      <div slot="header" class="bar">
        <span>统计分析</span>
        <div class="bar-actions">
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button label="chart">图表模式</el-radio-button>
            <el-radio-button label="table">表格模式</el-radio-button>
          </el-radio-group>
          <el-date-picker
            v-model="range"
            type="daterange"
            value-format="yyyy-MM-dd"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            size="small"
          />
          <el-button size="small" @click="loadAll">刷新</el-button>
          <el-button size="small" type="warning" @click="rebuild">重建日报</el-button>
        </div>
      </div>

      <div class="insight-strip">
        <div class="insight-card">
          <div class="insight-label">统计范围</div>
          <div class="insight-value">{{ rangeLabel }}</div>
        </div>
        <div class="insight-card">
          <div class="insight-label">趋势样本</div>
          <div class="insight-value">{{ trend.length }} 天</div>
        </div>
        <div class="insight-card">
          <div class="insight-label">主线观察</div>
          <div class="insight-value">{{ overview.commitCount || 0 }} 提交 / {{ overview.mergeCount || 0 }} 合并 / {{ overview.releaseCount || 0 }} 发布</div>
        </div>
      </div>

      <div v-if="viewMode === 'chart'" class="chart-mode">
        <div class="chart-title">研发主线趋势</div>
        <div class="chart-grid" v-loading="loading">
          <div
            v-for="metric in metricCards"
            :key="metric.key"
            class="chart-card"
            :style="{ '--metric-color': metric.color, '--metric-soft': metric.soft, '--metric-border': metric.border }"
          >
            <div class="chart-card-head">
              <span>{{ metric.label }}</span>
              <strong>{{ metric.total }}</strong>
            </div>
            <div class="chart-bars">
              <div v-for="point in metric.points" :key="metric.key + '-' + point.statDate" class="chart-bar-item">
                <div class="chart-bar-track">
                  <div class="chart-bar-fill" :style="{ height: point.height + '%' }"></div>
                </div>
                <div class="chart-bar-value">{{ point.value }}</div>
                <div class="chart-bar-label">{{ shortDate(point.statDate) }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="chart-title second">最近日报摘要</div>
        <div class="daily-summary-grid">
          <div v-for="row in rows.slice(0, 6)" :key="row.statDate" class="daily-summary-card">
            <div class="daily-summary-date">{{ row.statDate }}</div>
            <div class="daily-summary-line">浏览 {{ row.viewCount || 0 }}</div>
            <div class="daily-summary-line">下载 {{ row.downloadCount || 0 }}</div>
            <div class="daily-summary-line">提交 {{ row.commitCount || 0 }}</div>
            <div class="daily-summary-line">发布 {{ row.releaseCount || 0 }}</div>
          </div>
        </div>
      </div>

      <div v-else>
        <div class="table-title">趋势数据</div>
        <el-table :data="trend" border v-loading="loading">
          <el-table-column prop="statDate" label="日期" width="120" />
          <el-table-column prop="viewCount" label="浏览" width="100" />
          <el-table-column prop="downloadCount" label="下载" width="100" />
          <el-table-column prop="starCount" label="星标" width="100" />
          <el-table-column prop="commitCount" label="提交" width="100" />
          <el-table-column prop="mergeCount" label="合并" width="100" />
          <el-table-column prop="releaseCount" label="发布" width="100" />
          <el-table-column prop="memberActiveCount" label="活跃成员" width="120" />
          <el-table-column prop="taskCreatedCount" label="创建任务" width="120" />
          <el-table-column prop="taskCompletedCount" label="完成任务" width="120" />
        </el-table>

        <div class="table-title second">统计日报分页</div>
        <el-table :data="rows" border v-loading="dailyLoading">
          <el-table-column prop="statDate" label="日期" width="120" />
          <el-table-column prop="viewCount" label="浏览" width="100" />
          <el-table-column prop="uniqueVisitorCount" label="访客" width="100" />
          <el-table-column prop="downloadCount" label="下载" width="100" />
          <el-table-column prop="uniqueDownloadUserCount" label="独立下载用户" width="140" />
          <el-table-column prop="commitCount" label="提交" width="100" />
          <el-table-column prop="mergeCount" label="合并" width="100" />
          <el-table-column prop="releaseCount" label="发布" width="100" />
          <el-table-column prop="starCount" label="星标" width="100" />
          <el-table-column prop="commentCount" label="评论" width="100" />
        </el-table>

        <div class="pager">
          <el-pagination
            background
            layout="prev, pager, next, total"
            :current-page.sync="page"
            :page-size="size"
            :total="total"
            @current-change="loadDaily"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import {
  getProjectStatOverview,
  getProjectStatTrend,
  pageProjectStatDaily,
  rebuildProjectStatDaily
} from '@/api/projectStat'

function p(r) {
  if (r && r.data !== undefined) return r.data
  return r || {}
}

export default {
  name: 'ProjectStatManage',
  props: {
    projectId: { type: [String, Number], required: true }
  },
  data() {
    return {
      loading: false,
      dailyLoading: false,
      viewMode: 'chart',
      page: 1,
      size: 10,
      total: 0,
      range: [],
      overview: {},
      trend: [],
      rows: []
    }
  },
  computed: {
    overviewCards() {
      return [
        { key: 'views', label: '浏览', value: this.overview.viewCount || this.overview.totalViews || 0, desc: '项目曝光与关注入口', color: '#2563eb', soft: '#eff6ff', border: '#bfdbfe' },
        { key: 'downloads', label: '下载', value: this.overview.downloadCount || this.overview.totalDownloads || 0, desc: '被实际拉取与使用的次数', color: '#0f766e', soft: '#ecfeff', border: '#99f6e4' },
        { key: 'stars', label: '星标', value: this.overview.starCount || this.overview.totalStars || 0, desc: '收藏意愿与长期关注度', color: '#b45309', soft: '#fff7ed', border: '#fed7aa' },
        { key: 'commits', label: '提交', value: this.overview.commitCount || 0, desc: '最近主线研发节奏', color: '#7c3aed', soft: '#f5f3ff', border: '#ddd6fe' },
        { key: 'merges', label: '合并', value: this.overview.mergeCount || 0, desc: '从分支进入主线的频率', color: '#dc2626', soft: '#fef2f2', border: '#fecaca' },
        { key: 'releases', label: '发布', value: this.overview.releaseCount || 0, desc: '对外交付版本的节奏', color: '#0891b2', soft: '#ecfeff', border: '#bae6fd' }
      ]
    },
    metricCards() {
      const definitions = [
        { key: 'viewCount', label: '浏览趋势', color: '#2563eb', soft: '#eff6ff', border: '#bfdbfe' },
        { key: 'downloadCount', label: '下载趋势', color: '#0f766e', soft: '#ecfeff', border: '#99f6e4' },
        { key: 'commitCount', label: '提交趋势', color: '#7c3aed', soft: '#f5f3ff', border: '#ddd6fe' },
        { key: 'mergeCount', label: '合并趋势', color: '#dc2626', soft: '#fef2f2', border: '#fecaca' },
        { key: 'releaseCount', label: '发布趋势', color: '#0891b2', soft: '#ecfeff', border: '#bae6fd' },
        { key: 'starCount', label: '星标趋势', color: '#b45309', soft: '#fff7ed', border: '#fed7aa' }
      ]
      return definitions.map(definition => {
        const points = this.buildMetricPoints(definition.key)
        return {
          ...definition,
          total: points.reduce((sum, item) => sum + item.value, 0),
          points
        }
      })
    },
    rangeLabel() {
      if (this.range && this.range[0] && this.range[1]) {
        return `${this.range[0]} 至 ${this.range[1]}`
      }
      return '按系统默认区间统计'
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler() {
        if (this.projectId) this.loadAll()
      }
    }
  },
  methods: {
    query() {
      return {
        startDate: this.range && this.range[0] ? this.range[0] : undefined,
        endDate: this.range && this.range[1] ? this.range[1] : undefined
      }
    },
    async loadAll() {
      await Promise.all([this.loadOverview(), this.loadTrend(), this.loadDaily(this.page)])
    },
    async loadOverview() {
      const r = await getProjectStatOverview(this.projectId).catch(() => ({}))
      this.overview = p(r)
    },
    async loadTrend() {
      this.loading = true
      try {
        const r = await getProjectStatTrend(this.projectId, this.query())
        this.trend = Array.isArray(p(r)) ? p(r) : []
      } finally {
        this.loading = false
      }
    },
    async loadDaily(v) {
      this.page = v || 1
      this.dailyLoading = true
      try {
        const r = await pageProjectStatDaily(this.projectId, { ...this.query(), page: this.page, size: this.size })
        const d = p(r)
        this.rows = Array.isArray(d.list) ? d.list : (Array.isArray(d.records) ? d.records : [])
        this.total = Number(d.total || 0)
      } finally {
        this.dailyLoading = false
      }
    },
    async rebuild() {
      try {
        await rebuildProjectStatDaily(this.projectId, this.query())
        this.$message.success('统计日报已重建')
        await this.loadAll()
      } catch (e) {
        this.$message.error(e.response?.data?.message || '重建失败')
      }
    },
    buildMetricPoints(key) {
      const values = (this.trend || []).map(item => Number(item && item[key]) || 0)
      const max = values.length ? Math.max(...values, 1) : 1
      return (this.trend || []).map(item => ({
        statDate: item.statDate,
        value: Number(item && item[key]) || 0,
        height: Math.max(8, Math.round(((Number(item && item[key]) || 0) / max) * 100))
      }))
    },
    shortDate(value) {
      if (!value) return ''
      const parts = String(value).split('-')
      return parts.length >= 3 ? `${parts[1]}/${parts[2]}` : value
    }
  }
}
</script>

<style scoped>
.feature-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}
.overview-card {
  padding: 18px;
  border-radius: 18px;
  border: 1px solid var(--overview-border);
  background: linear-gradient(180deg, var(--overview-soft) 0%, #ffffff 100%);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}
.overview-card-label {
  color: #64748b;
  font-size: 13px;
}
.overview-card-value {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
}
.overview-card-desc {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.7;
}
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.bar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.insight-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}
.insight-card {
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e5ecf6;
}
.insight-label {
  font-size: 12px;
  color: #64748b;
}
.insight-value {
  margin-top: 6px;
  color: #1f2937;
  font-size: 13px;
  line-height: 1.7;
  font-weight: 600;
}
.chart-mode {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.chart-title {
  margin-top: 4px;
  font-size: 14px;
  font-weight: 700;
  color: #303133;
}
.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.chart-card {
  border: 1px solid var(--metric-border);
  border-radius: 14px;
  background: linear-gradient(180deg, var(--metric-soft) 0%, #ffffff 100%);
  padding: 16px;
}
.chart-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #303133;
  font-weight: 600;
}
.chart-bars {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  margin-top: 16px;
  overflow-x: auto;
  padding-bottom: 4px;
}
.chart-bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex: 0 0 42px;
}
.chart-bar-track {
  width: 100%;
  height: 96px;
  border-radius: 999px;
  background: #eef3f8;
  position: relative;
  overflow: hidden;
}
.chart-bar-fill {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 999px;
  background: linear-gradient(180deg, #ffffff 0%, var(--metric-color) 100%);
}
.chart-bar-value {
  font-size: 12px;
  color: #303133;
  font-weight: 600;
}
.chart-bar-label {
  font-size: 11px;
  color: #909399;
}
.daily-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.daily-summary-card {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fff;
  padding: 14px;
}
.daily-summary-date {
  font-size: 14px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}
.daily-summary-line {
  color: #606266;
  line-height: 1.8;
  font-size: 13px;
}
.table-title {
  margin: 4px 0 12px;
  font-size: 14px;
  font-weight: 700;
  color: #303133;
}
.table-title.second {
  margin-top: 18px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
@media (max-width: 768px) {
  .overview-grid,
  .insight-strip,
  .chart-grid,
  .daily-summary-grid {
    grid-template-columns: 1fr;
  }
}


.overview-card,
.insight-card,
.chart-card,
.daily-summary-card {
  box-shadow: var(--it-shadow-soft);
}
.overview-card {
  background: linear-gradient(180deg, var(--overview-soft, var(--it-tone-info-soft)) 0%, var(--it-panel-bg-strong) 100%);
  border-color: var(--overview-border, var(--it-border));
}
.overview-card-label,
.insight-label,
.daily-summary-line,
.chart-bar-label { color: var(--it-text-muted); }
.overview-card-value,
.chart-card-head,
.chart-title,
.daily-summary-date,
.table-title,
.insight-value,
.chart-bar-value { color: var(--it-text); }
.overview-card-desc { color: var(--it-text-subtle); }
.insight-card,
.daily-summary-card,
.chart-card {
  background: var(--it-panel-bg);
  border-color: var(--it-border);
}
.chart-bar-track { background: var(--it-surface-muted); }
.chart-bar-fill { background: linear-gradient(180deg, var(--it-surface-elevated) 0%, var(--metric-color, var(--it-accent)) 100%); }

</style>
