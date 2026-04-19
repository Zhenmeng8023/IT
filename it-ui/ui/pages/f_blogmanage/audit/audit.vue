<template>
  <div class="blog-audit-page">
    <div class="page-header">
      <div>
        <h1>博客审核管理</h1>
        <p>博客审核、举报处理统一在这里完成</p>
      </div>
      <div class="header-actions">
        <el-button size="small" icon="el-icon-refresh" @click="refreshCurrentTab">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never" class="tab-card">
      <el-tabs v-model="activeTab" @tab-click="handleTabChange">
        <el-tab-pane label="博客审核" name="blogAudit">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-select data-testid="blog-audit-status-filter" v-model="blogFilter.status" size="small" style="width: 140px" @change="handleBlogFilterChange">
                <el-option label="待审核" value="pending" />
                <el-option label="已驳回" value="rejected" />
                <el-option label="已发布" value="published" />
              </el-select>
              <el-input
                data-testid="blog-audit-search-input"
                v-model.trim="blogFilter.keyword"
                size="small"
                style="width: 220px"
                clearable
                placeholder="标题 / 作者"
                @keyup.enter.native="handleBlogSearch"
              />
              <el-button data-testid="blog-audit-search-submit" size="small" type="primary" @click="handleBlogSearch">查询</el-button>
            </div>
          </div>

          <el-table data-testid="blog-audit-table" :data="blogList" v-loading="blogLoading" stripe>
            <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
            <el-table-column label="作者" width="140">
              <template slot-scope="{ row }">
                {{ getAuthorName(row.author) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="110">
              <template slot-scope="{ row }">
                <el-tag size="small" :type="getBlogStatusType(row.status)">
                  {{ getBlogStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reportCount" label="举报数" width="90" />
            <el-table-column prop="updatedAt" label="更新时间" width="180">
              <template slot-scope="{ row }">{{ formatDate(row.updatedAt || row.auditTime || row.publishTime || row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="260" fixed="right">
              <template slot-scope="{ row }">
                <el-button :data-testid="`blog-audit-view-${row.id}`" type="text" size="mini" @click="openBlogDetail(row)">查看</el-button>
                <el-button :data-testid="`blog-audit-approve-${row.id}`" v-if="row.status === 'pending'" type="text" size="mini" style="color:#67c23a" @click="handleApprove(row)">通过</el-button>
                <el-button :data-testid="`blog-audit-reject-${row.id}`" v-if="row.status === 'pending'" type="text" size="mini" style="color:#f56c6c" @click="openRejectDialog(row)">拒绝</el-button>
                <el-button :data-testid="`blog-audit-report-${row.id}`" v-if="(row.reportCount || 0) > 0" type="text" size="mini" style="color:#e6a23c" @click="openReportManagerByBlog(row)">举报记录</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-box">
            <el-pagination
              :current-page="blogPage.current"
              :page-size="blogPage.size"
              :total="blogPage.total"
              layout="total, prev, pager, next"
              @current-change="handleBlogPageChange"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="举报处理" name="reportAudit">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-select v-model="reportFilter.status" size="small" style="width: 140px" clearable @change="handleReportFilterChange">
                <el-option label="待处理" value="pending" />
                <el-option label="举报成立" value="processed" />
                <el-option label="举报驳回" value="ignored" />
              </el-select>
              <el-input
                v-model.trim="reportFilter.keyword"
                size="small"
                style="width: 220px"
                clearable
                placeholder="博客标题 / 举报人"
                @keyup.enter.native="handleReportSearch"
              />
              <el-button size="small" type="primary" @click="handleReportSearch">查询</el-button>
            </div>
          </div>

          <el-table :data="reportList" v-loading="reportLoading" stripe>
            <el-table-column prop="targetTitle" label="博客标题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="reporterName" label="举报人" width="120" />
            <el-table-column prop="reason" label="举报原因" min-width="180" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="110">
              <template slot-scope="{ row }">
                <el-tag size="small" :type="getReportStatusType(row.status)">
                  {{ getReportStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="举报时间" width="180">
              <template slot-scope="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="260" fixed="right">
              <template slot-scope="{ row }">
                <el-button type="text" size="mini" @click="openReportDetail(row)">查看</el-button>
                <el-button v-if="isPendingReport(row)" type="text" size="mini" style="color:#f56c6c" @click="handleProcessReport(row, 'approved')">举报成立</el-button>
                <el-button v-if="isPendingReport(row)" type="text" size="mini" style="color:#409eff" @click="handleProcessReport(row, 'rejected')">驳回举报</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-box">
            <el-pagination
              :current-page="reportPage.current"
              :page-size="reportPage.size"
              :total="reportPage.total"
              layout="total, prev, pager, next"
              @current-change="handleReportPageChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog title="拒绝博客" :visible.sync="rejectDialogVisible" width="480px">
      <el-input
        v-model.trim="rejectForm.reason"
        type="textarea"
        :rows="4"
        maxlength="200"
        show-word-limit
        placeholder="请输入拒绝原因"
      />
      <span slot="footer">
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitReject">确认拒绝</el-button>
      </span>
    </el-dialog>

    <el-dialog title="博客详情" :visible.sync="blogDetailVisible" width="76%" top="5vh">
      <div v-if="currentBlog" class="blog-detail-box">
        <div class="detail-meta">
          <div><strong>标题：</strong>{{ currentBlog.title }}</div>
          <div><strong>作者：</strong>{{ getAuthorName(currentBlog.author) }}</div>
          <div><strong>状态：</strong>{{ getBlogStatusText(currentBlog.status) }}</div>
          <div v-if="currentBlog.rejectReason"><strong>拒绝原因：</strong>{{ currentBlog.rejectReason }}</div>
        </div>
        <el-divider />
        <div class="rich-content" v-html="currentBlog.content || '<p>暂无内容</p>'"></div>
      </div>
      <span slot="footer">
        <el-button @click="blogDetailVisible = false">关闭</el-button>
      </span>
    </el-dialog>

    <el-dialog title="举报详情" :visible.sync="reportDetailVisible" width="80%" top="4vh">
      <div v-if="currentReport" class="report-detail-box">
        <div class="report-summary">
          <div><strong>博客标题：</strong>{{ currentReport.targetTitle }}</div>
          <div><strong>举报人：</strong>{{ currentReport.reporterName }}</div>
          <div><strong>举报原因：</strong>{{ currentReport.reason }}</div>
          <div><strong>状态：</strong>{{ getReportStatusText(currentReport.status) }}</div>
        </div>
        <el-divider />
        <div class="rich-content" v-html="currentReportBlog.content || '<p>暂无内容</p>'"></div>
      </div>
      <span slot="footer">
        <el-button @click="reportDetailVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  approveBlog,
  blogStatusTagType,
  blogStatusText,
  fetchBlogDetail,
  fetchBlogReviewList,
  fetchReportReviewList,
  processReport,
  rejectBlog
} from '@/api/blog'

function normalizeSearchKeyword(value) {
  return String(value || '').trim().toLowerCase()
}

export default {
  name: 'BlogAuditManage',
  layout: 'manage',
  data() {
    return {
      activeTab: 'blogAudit',

      blogLoading: false,
      blogList: [],
      blogPage: { current: 1, size: 10, total: 0 },
      blogFilter: { status: 'pending', keyword: '' },

      reportLoading: false,
      reportList: [],
      reportPage: { current: 1, size: 10, total: 0 },
      reportFilter: { status: 'pending', keyword: '' },

      rejectDialogVisible: false,
      rejectForm: { blogId: null, reason: '' },

      blogDetailVisible: false,
      reportDetailVisible: false,
      currentBlog: null,
      currentReport: null,
      currentReportBlog: {}
    }
  },
  mounted() {
    this.loadBlogList()
  },
  methods: {
    handleTabChange() {
      this.refreshCurrentTab()
    },
    refreshCurrentTab() {
      if (this.activeTab === 'blogAudit') this.loadBlogList()
      if (this.activeTab === 'reportAudit') this.loadReportList()
    },
    handleBlogFilterChange() {
      this.blogPage.current = 1
      this.loadBlogList()
    },
    handleBlogSearch() {
      this.blogPage.current = 1
      this.loadBlogList()
    },
    handleReportFilterChange() {
      this.reportPage.current = 1
      this.loadReportList()
    },
    handleReportSearch() {
      this.reportPage.current = 1
      this.loadReportList()
    },

    async loadBlogList() {
      this.blogLoading = true
      try {
        const result = await fetchBlogReviewList({
          status: this.blogFilter.status || 'pending',
          keyword: this.blogFilter.keyword,
          page: this.blogPage.current,
          pageSize: this.blogPage.size
        })
        this.blogList = result.list || []
        this.blogPage.total = result.total || 0
        if (result.page && result.page !== this.blogPage.current) {
          this.blogPage.current = result.page
        }
      } catch (e) {
        console.error('加载博客审核列表失败', e)
        this.blogList = []
        this.blogPage.total = 0
        this.$message.error(e?.response?.data?.message || '加载博客审核列表失败')
      } finally {
        this.blogLoading = false
      }
    },

    async loadReportList() {
      this.reportLoading = true
      try {
        const result = await fetchReportReviewList({
          status: this.reportFilter.status || '',
          page: this.reportPage.current,
          pageSize: this.reportPage.size
        })
        const keyword = normalizeSearchKeyword(this.reportFilter.keyword)
        const filtered = keyword
          ? (result.list || []).filter(item => {
            const title = normalizeSearchKeyword(item.targetTitle)
            const reporter = normalizeSearchKeyword(item.reporterName)
            return title.includes(keyword) || reporter.includes(keyword)
          })
          : (result.list || [])

        this.reportList = filtered
        this.reportPage.total = keyword ? filtered.length : (result.total || 0)
        if (!keyword && result.page && result.page !== this.reportPage.current) {
          this.reportPage.current = result.page
        }
      } catch (e) {
        console.error('加载举报列表失败', e)
        this.reportList = []
        this.reportPage.total = 0
        this.$message.error(e?.response?.data?.message || '加载举报列表失败')
      } finally {
        this.reportLoading = false
      }
    },

    async fetchBlogSilently(id) {
      if (!id) return null
      try {
        return await fetchBlogDetail(id)
      } catch (e) {
        return null
      }
    },

    async openBlogDetail(row) {
      const blog = await this.fetchBlogSilently(row.id)
      this.currentBlog = blog || row
      this.blogDetailVisible = true
    },

    async openReportDetail(row) {
      this.currentReport = row
      this.currentReportBlog = await this.fetchBlogSilently(row.targetId) || {}
      this.reportDetailVisible = true
    },

    openReportManagerByBlog(row) {
      this.activeTab = 'reportAudit'
      this.reportFilter.keyword = row.title || ''
      this.reportPage.current = 1
      this.loadReportList()
    },

    openRejectDialog(row) {
      this.rejectForm.blogId = row.id
      this.rejectForm.reason = ''
      this.rejectDialogVisible = true
    },

    async submitReject() {
      if (!this.rejectForm.blogId) return
      if (!this.rejectForm.reason) {
        this.$message.warning('请输入拒绝原因')
        return
      }
      try {
        await rejectBlog(this.rejectForm.blogId, this.rejectForm.reason)
        this.$message.success('已拒绝该博客')
        this.rejectDialogVisible = false
        this.loadBlogList()
      } catch (e) {
        console.error('拒绝博客失败', e)
        this.$message.error(e?.response?.data?.message || '拒绝博客失败')
      }
    },

    async handleApprove(row) {
      try {
        await this.$confirm('确认通过该博客吗？', '提示', { type: 'warning' })
        await approveBlog(row.id)
        this.$message.success('审核通过')
        this.loadBlogList()
      } catch (e) {
        if (e !== 'cancel') {
          console.error('通过博客失败', e)
          this.$message.error('通过博客失败')
        }
      }
    },

    async handleProcessReport(row, action) {
      const actionText = action === 'approved' ? '举报成立' : '驳回举报'
      try {
        await this.$confirm(`确认执行“${actionText}”吗？`, '提示', { type: 'warning' })
        await processReport(row.id, action)
        this.$message.success(`${actionText}成功`)
        await Promise.all([
          this.loadReportList(),
          this.loadBlogList()
        ])
      } catch (e) {
        if (e !== 'cancel') {
          console.error('处理举报失败', e)
          this.$message.error(e?.response?.data?.message || '处理举报失败')
        }
      }
    },

    handleBlogPageChange(page) {
      this.blogPage.current = page
      this.loadBlogList()
    },
    handleReportPageChange(page) {
      this.reportPage.current = page
      this.loadReportList()
    },

    isPendingReport(row) {
      return String(row.status || '').toLowerCase() === 'pending'
    },

    getAuthorName(author) {
      if (!author) return '未知作者'
      return author.nickname || author.displayName || author.username || '未知作者'
    },
    getBlogStatusType(status) {
      return blogStatusTagType(status)
    },
    getBlogStatusText(status) {
      return blogStatusText(status)
    },
    getReportStatusType(status) {
      const s = String(status || '').toLowerCase()
      if (s === 'pending') return 'warning'
      if (s === 'processed' || s === 'approved') return 'danger'
      if (s === 'ignored' || s === 'rejected') return 'info'
      return 'info'
    },
    getReportStatusText(status) {
      const s = String(status || '').toLowerCase()
      if (s === 'pending') return '待处理'
      if (s === 'processed' || s === 'approved') return '举报成立'
      if (s === 'ignored' || s === 'rejected') return '举报驳回'
      return '未知'
    },
    formatDate(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return '-'
      return date.toLocaleString('zh-CN')
    }
  }
}
</script>

<style scoped>
.blog-audit-page { padding: 20px; }
.page-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; }
.page-header h1 { margin:0; font-size:24px; color:#303133; }
.page-header p { margin:6px 0 0; color:#909399; font-size:13px; }
.tab-card { border-radius: 10px; }
.toolbar { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; }
.toolbar-left { display:flex; gap:10px; align-items:center; flex-wrap:wrap; }
.pagination-box { display:flex; justify-content:flex-end; margin-top:16px; }
.detail-meta, .report-summary { display:grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap:10px 20px; }
.rich-content { min-height:240px; max-height:62vh; overflow:auto; border:1px solid #ebeef5; border-radius:8px; padding:16px; background:#fff; }
.rich-content >>> img { max-width:100%; }
.rich-content >>> pre { white-space:pre-wrap; word-break:break-word; }
@media (max-width: 900px) {
  .detail-meta, .report-summary { grid-template-columns: 1fr; }
}
</style>
