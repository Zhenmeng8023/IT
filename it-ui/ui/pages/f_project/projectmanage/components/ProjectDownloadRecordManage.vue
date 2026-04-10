<template>
  <div class="feature-page">
    <el-row :gutter="16" class="top-row">
      <el-col :xs="24" :sm="8">
        <div class="mini-card">
          <div class="mini-label">总下载</div>
          <div class="mini-value">{{ summary.totalDownloads || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="mini-card">
          <div class="mini-label">独立下载用户</div>
          <div class="mini-value">{{ summary.uniqueUsers || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="mini-card">
          <div class="mini-label">近 7 天下载</div>
          <div class="mini-value">{{ summary.last7DaysDownloads || 0 }}</div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <div slot="header" class="bar">
        <span>下载记录</span>
        <div class="bar-actions">
          <el-button size="small" @click="loadAll">刷新</el-button>
        </div>
      </div>

      <el-table :data="rows" border v-loading="loading">
        <el-table-column prop="id" label="记录ID" width="90" />
        <el-table-column prop="fileId" label="文件ID" width="100" />
        <el-table-column prop="fileVersionId" label="版本ID" width="100" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="ipAddress" label="IP" min-width="140" />
        <el-table-column prop="userAgent" label="User-Agent" min-width="280" show-overflow-tooltip />
        <el-table-column prop="downloadedAt" label="下载时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.downloadedAt) }}</template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page.sync="page"
          :page-size="size"
          :total="total"
          @current-change="loadPage"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import { getProjectDownloadSummary, pageProjectDownloadRecords } from '@/api/projectDownload'

function p(r) {
  if (r && r.data !== undefined) return r.data
  return r || {}
}

export default {
  name: 'ProjectDownloadRecordManage',
  props: {
    projectId: { type: [String, Number], required: true }
  },
  data() {
    return {
      loading: false,
      page: 1,
      size: 10,
      total: 0,
      summary: {},
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
    formatTime(v) {
      if (!v) return ''
      const d = new Date(v)
      if (Number.isNaN(d.getTime())) return v
      return d.toLocaleString('zh-CN')
    },
    async loadAll() {
      await Promise.all([this.loadSummary(), this.loadPage(this.page)])
    },
    async loadSummary() {
      const r = await getProjectDownloadSummary(this.projectId).catch(() => ({}))
      this.summary = p(r)
    },
    async loadPage(v) {
      this.page = v || 1
      this.loading = true
      try {
        const r = await pageProjectDownloadRecords(this.projectId, { page: this.page, size: this.size })
        const d = p(r)
        this.rows = Array.isArray(d.list) ? d.list : (Array.isArray(d.records) ? d.records : [])
        this.total = Number(d.total || 0)
      } finally {
        this.loading = false
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
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
