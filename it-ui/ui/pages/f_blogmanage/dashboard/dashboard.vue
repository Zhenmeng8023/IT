<template>
  <div class="dashboard">
    <div class="page-header">
      <div>
        <h1>博客仪表盘</h1>
        <p>按后端权威统计查看博客审核与举报状态</p>
      </div>
      <el-button type="primary" icon="el-icon-refresh" :loading="loading" @click="fetchAll">刷新</el-button>
    </div>
    <el-row :gutter="20" class="stat-row">
      <el-col :span="4" v-for="item in statCards" :key="item.key">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="never" class="list-card">
          <template #header><div class="card-header"><span>热门博客</span><el-button type="text" @click="gotoAudit">去审核页</el-button></div></template>
          <div v-if="!hotBlogs.length" class="empty-box">暂无数据</div>
          <div v-else class="hot-list">
            <div v-for="(item,index) in hotBlogs" :key="item.id" class="hot-item">
              <div class="hot-rank">{{ index + 1 }}</div>
              <div class="hot-main">
                <div class="hot-title">{{ item.title }}</div>
                <div class="hot-meta">
                  <span>{{ item.author && (item.author.displayName || item.author.nickname || item.author.username) || '未知作者' }}</span>
                  <span>{{ item.viewCount || 0 }} 浏览</span>
                  <span>{{ formatDate(item.publishTime || item.createdAt) }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="list-card">
          <template #header><div class="card-header"><span>快速入口</span></div></template>
          <div class="quick-actions">
            <el-button type="primary" @click="gotoAudit">博客审核</el-button>
            <el-button type="success" @click="gotoWrite">写博客</el-button>
            <el-button type="warning" @click="gotoUserManage">用户管理</el-button>
            <el-button type="info" @click="gotoSettings">系统设置</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: 'BlogDashboard',
  layout: 'manage',
  data() {
    return {
      loading: false,
      overviewData: { totalBlogs: 0, pendingBlogs: 0, publishedBlogs: 0, rejectedBlogs: 0, pendingReports: 0, todayReports: 0, todayViews: 0 },
      hotBlogs: []
    }
  },
  computed: {
    statCards() {
      return [
        { key: 'totalBlogs', label: '总博客数', value: this.overviewData.totalBlogs || 0 },
        { key: 'pendingBlogs', label: '待审核', value: this.overviewData.pendingBlogs || 0 },
        { key: 'publishedBlogs', label: '已发布', value: this.overviewData.publishedBlogs || 0 },
        { key: 'rejectedBlogs', label: '已驳回', value: this.overviewData.rejectedBlogs || 0 },
        { key: 'pendingReports', label: '待处理举报', value: this.overviewData.pendingReports || 0 },
        { key: 'todayReports', label: '今日举报', value: this.overviewData.todayReports || 0 }
      ]
    }
  },
  mounted() { this.fetchAll() },
  methods: {
    async fetchAll() {
      this.loading = true
      try {
        const [statsRes, hotRes] = await Promise.all([this.$axios.get('/api/blogs/admin/stats/overview'), this.$axios.get('/api/blogs/hot')])
        this.overviewData = statsRes && statsRes.data !== undefined ? statsRes.data : statsRes
        const hotPayload = hotRes && hotRes.data !== undefined ? hotRes.data : hotRes
        this.hotBlogs = Array.isArray(hotPayload) ? hotPayload.slice(0, 8) : []
      } catch (e) {
        this.$message.error('加载仪表盘失败')
      } finally { this.loading = false }
    },
    formatDate(value) {
      if (!value) return ''
      try {
        const d = new Date(value)
        if (Number.isNaN(d.getTime())) return ''
        return d.toLocaleString('zh-CN')
      } catch (e) { return '' }
    },
    gotoAudit() { this.$router.push('/admin/content/blog/audit') },
    gotoWrite() { this.$router.push('/blogwrite') },
    gotoUserManage() { this.$router.push('/admin/users/account') },
    gotoSettings() { this.$router.push('/admin/system/log') }
  }
}
</script>

<style scoped>
.dashboard{padding:20px;color:var(--it-text)}.page-header{display:flex;align-items:center;justify-content:space-between;gap:16px}.stat-row{margin:20px 0}.stat-card{text-align:center;border:1px solid var(--it-border);border-radius:18px;background:var(--it-panel-bg);box-shadow:var(--it-shadow-soft)}.stat-value{font-size:28px;font-weight:700;color:var(--it-text)}.stat-label{margin-top:8px;font-size:13px;color:var(--it-text-subtle)}.list-card{min-height:360px;border-radius:18px;border:1px solid var(--it-border);background:var(--it-panel-bg);box-shadow:var(--it-shadow-soft)}.card-header{display:flex;align-items:center;justify-content:space-between;color:var(--it-text)}.hot-list{display:flex;flex-direction:column;gap:12px}.hot-item{display:flex;gap:12px;align-items:flex-start;padding-bottom:12px;border-bottom:1px solid color-mix(in srgb, var(--it-border) 82%, transparent)}.hot-rank{width:28px;height:28px;line-height:28px;border-radius:50%;background:var(--it-primary-gradient);color:var(--it-text-light);text-align:center;font-weight:700;box-shadow:var(--it-button-shadow)}.hot-main{flex:1;min-width:0}.hot-title{font-size:14px;color:var(--it-text);font-weight:600;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.hot-meta{margin-top:6px;display:flex;flex-wrap:wrap;gap:12px;color:var(--it-text-subtle);font-size:12px}.quick-actions{display:flex;flex-wrap:wrap;gap:14px}.empty-box{color:var(--it-text-subtle);padding:40px 0;text-align:center}
</style>
