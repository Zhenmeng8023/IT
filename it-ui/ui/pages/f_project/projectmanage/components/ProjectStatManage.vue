<template>
  <div class="feature-page">
    <el-row :gutter="16" class="top-row">
      <el-col :xs="24" :sm="8">
        <div class="mini-card">
          <div class="mini-label">浏览</div>
          <div class="mini-value">{{ overview.viewCount || overview.totalViews || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="mini-card">
          <div class="mini-label">下载</div>
          <div class="mini-value">{{ overview.downloadCount || overview.totalDownloads || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="mini-card">
          <div class="mini-label">星标</div>
          <div class="mini-value">{{ overview.starCount || overview.totalStars || 0 }}</div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <div slot="header" class="bar">
        <span>统计分析</span>
        <div class="bar-actions">
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

      <div class="table-title">趋势数据</div>
      <el-table :data="trend" border v-loading="loading">
        <el-table-column prop="statDate" label="日期" width="120" />
        <el-table-column prop="viewCount" label="浏览" width="100" />
        <el-table-column prop="downloadCount" label="下载" width="100" />
        <el-table-column prop="starCount" label="星标" width="100" />
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
      page: 1,
      size: 10,
      total: 0,
      range: [],
      overview: {},
      trend: [],
      rows: []
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
.top-row {
  margin-bottom: 0;
}
.mini-card {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fff;
  padding: 16px;
}
.mini-label {
  color: #909399;
  font-size: 13px;
}
.mini-value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
  color: #303133;
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
</style>
