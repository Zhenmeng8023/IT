<template>
  <div class="algoreco-page">
    <div class="page-header">
      <div>
        <h1>算法推荐管理</h1>
        <p>维护博客推荐任务，支持筛选、手动触发、失败排查与结果查看。</p>
      </div>
      <div class="header-actions">
        <el-select v-model="recommendSize" size="small" class="size-select">
          <el-option v-for="item in recommendSizeOptions" :key="item" :label="`推荐条数 ${item}`" :value="item" />
        </el-select>
        <el-button size="small" icon="el-icon-refresh" :loading="listLoading" @click="loadTaskSeeds">刷新列表</el-button>
        <el-button
          size="small"
          type="primary"
          icon="el-icon-cpu"
          :loading="batchRunning"
          :disabled="pagedTaskRows.length === 0 || listLoading"
          @click="runBatchOnCurrentPage"
        >
          触发当前页推荐
        </el-button>
      </div>
    </div>

    <el-alert
      title="当前页面基于“博客列表 + 推荐接口”构建推荐任务视图，支持筛选、手动触发与重新生成。"
      type="info"
      :closable="false"
      show-icon
      class="page-alert"
    />

    <el-alert
      v-if="recommendEndpointMissing"
      title="推荐接口可能未就绪（检测到 404）。请确认后端已提供 /api/blogs/{id}/recommendations。"
      type="warning"
      :closable="false"
      show-icon
      class="page-alert"
    />

    <el-alert
      v-if="listError"
      :title="`任务列表加载失败：${listError}`"
      type="error"
      :closable="false"
      show-icon
      class="page-alert"
    />

    <el-card shadow="never" class="tab-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model.trim="filters.keyword"
            size="small"
            clearable
            style="width: 220px"
            placeholder="关键词：博客标题 / 作者 / ID"
            @keyup.enter.native="handleFilterSubmit"
          />
          <el-select v-model="filters.status" size="small" clearable style="width: 120px" placeholder="任务状态">
            <el-option v-for="item in taskStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="filters.scene" size="small" clearable style="width: 170px" placeholder="推荐场景">
            <el-option v-for="item in sceneOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-date-picker
            v-model="filters.timeRange"
            size="small"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 330px"
          />
          <el-button size="small" type="primary" @click="handleFilterSubmit">查询</el-button>
          <el-button size="small" @click="resetFilters">重置</el-button>
        </div>
      </div>

      <el-table
        data-testid="algoreco-table"
        :data="pagedTaskRows"
        v-loading="listLoading"
        border
        stripe
        row-key="id"
        style="width: 100%"
      >
        <el-table-column prop="id" label="博客ID" width="88" />
        <el-table-column prop="title" label="博客标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="作者" width="130">
          <template slot-scope="{ row }">
            {{ getAuthorName(row.author) }}
          </template>
        </el-table-column>
        <el-table-column label="场景" width="140">
          <template slot-scope="{ row }">
            <el-tag size="mini" type="info">{{ formatSceneLabel(row.scene) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="任务状态" width="110">
          <template slot-scope="{ row }">
            <el-tag size="small" :type="getTaskStatusType(row.task.status)">
              {{ getTaskStatusText(row.task.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="推荐来源" width="140">
          <template slot-scope="{ row }">
            {{ getSourceText(row.task.source) }}
          </template>
        </el-table-column>
        <el-table-column label="推荐数" width="90">
          <template slot-scope="{ row }">
            {{ getResultCountText(row.task) }}
          </template>
        </el-table-column>
        <el-table-column label="生成时间" width="170">
          <template slot-scope="{ row }">
            {{ formatDateTime(row.task.generatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="最近执行" width="170">
          <template slot-scope="{ row }">
            {{ formatDateTime(row.task.lastRunAt || row.updatedAt || row.publishTime || row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="错误信息" min-width="180">
          <template slot-scope="{ row }">
            <el-tooltip v-if="row.task.errorMessage" :content="row.task.errorMessage" placement="top">
              <span class="ellipsis-text error-text">{{ row.task.errorMessage }}</span>
            </el-tooltip>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template slot-scope="{ row }">
            <el-button
              type="text"
              size="mini"
              :loading="row.task.loading"
              @click="runTask(row)"
            >
              {{ getTriggerButtonText(row.task.status) }}
            </el-button>
            <el-button
              type="text"
              size="mini"
              :disabled="row.task.loading || row.task.status === 'idle'"
              @click="rerunTask(row)"
            >
              重新生成
            </el-button>
            <el-button type="text" size="mini" @click="openTaskDetail(row)">详情</el-button>
            <el-button type="text" size="mini" @click="openBlogDetail(row)">博客</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!listLoading && filteredTaskRows.length === 0" class="table-empty">
        <el-empty :description="emptyDescription" :image-size="76" />
      </div>

      <div class="pagination-box">
        <el-pagination
          :current-page="page.current"
          :page-size="page.size"
          :page-sizes="[10, 20, 50]"
          :total="filteredTaskRows.length"
          background
          layout="total, sizes, prev, pager, next"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      title="推荐任务详情"
      :visible.sync="detailVisible"
      width="80%"
      top="4vh"
    >
      <div v-if="detailRow && detailTask" class="detail-wrap">
        <el-descriptions :column="2" border class="detail-desc">
          <el-descriptions-item label="博客ID">{{ detailRow.id }}</el-descriptions-item>
          <el-descriptions-item label="博客标题">{{ detailRow.title || '-' }}</el-descriptions-item>
          <el-descriptions-item label="任务状态">{{ getTaskStatusText(detailTask.status) }}</el-descriptions-item>
          <el-descriptions-item label="场景">{{ formatSceneLabel(detailRow.scene) }}</el-descriptions-item>
          <el-descriptions-item label="推荐来源">{{ getSourceText(detailTask.source) }}</el-descriptions-item>
          <el-descriptions-item label="推荐条数">{{ getResultCountText(detailTask) }}</el-descriptions-item>
          <el-descriptions-item label="算法版本">{{ detailTask.algorithmVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="生成时间">{{ formatDateTime(detailTask.generatedAt) }}</el-descriptions-item>
          <el-descriptions-item label="最近执行">{{ formatDateTime(detailTask.lastRunAt) }}</el-descriptions-item>
          <el-descriptions-item label="博客更新时间">{{ formatDateTime(detailRow.updatedAt || detailRow.publishTime || detailRow.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="detailTask.errorMessage"
          :title="detailTask.errorMessage"
          type="error"
          show-icon
          :closable="false"
          class="detail-alert"
        />

        <el-alert
          v-if="detailTask.forceRefreshUnsupported"
          title="已尝试重新生成，但返回时间戳未变化，可能是推荐结果本身未发生变化。"
          type="warning"
          show-icon
          :closable="false"
          class="detail-alert"
        />

        <el-divider content-position="left">推荐结果</el-divider>
        <el-table
          v-if="detailTask.resultItems && detailTask.resultItems.length"
          :data="detailTask.resultItems"
          border
          stripe
          size="mini"
          style="width: 100%"
        >
          <el-table-column prop="id" label="博客ID" width="88" />
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column label="作者" width="130">
            <template slot-scope="{ row }">
              {{ getAuthorName(row.author) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template slot-scope="{ row }">
              <el-tag size="mini" :type="getBlogStatusType(row.status)">
                {{ row.statusLabel || row.status || '-' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="170">
            <template slot-scope="{ row }">
              {{ formatDateTime(row.updatedAt || row.publishTime || row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="摘要" min-width="220" show-overflow-tooltip>
            <template slot-scope="{ row }">
              {{ row.summary || '-' }}
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="detail-empty">
          <el-empty description="暂无推荐结果" :image-size="68" />
        </div>

        <el-divider content-position="left">博客信息</el-divider>
        <div v-if="detailBlogLoading" class="detail-loading">正在加载博客详情...</div>
        <div v-else-if="detailBlog" class="detail-blog-meta">
          <div><strong>作者：</strong>{{ getAuthorName(detailBlog.author) }}</div>
          <div><strong>状态：</strong>{{ detailBlog.statusLabel || detailBlog.status || '-' }}</div>
          <div><strong>标签：</strong>{{ (detailBlog.tags && detailBlog.tags.join(' / ')) || '-' }}</div>
          <div><strong>摘要：</strong>{{ detailBlog.summary || '-' }}</div>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="detailRow"
          type="primary"
          :loading="detailTask && detailTask.loading"
          @click="runTask(detailRow)"
        >
          {{ detailTask ? getTriggerButtonText(detailTask.status) : '触发推荐' }}
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  DEFAULT_RECO_SIZE,
  MAX_RECO_SIZE,
  RECO_SCENE_CODE,
  fetchAlgoRecoBlogDetail,
  fetchAlgoRecoTaskSeeds,
  triggerAlgoRecoTask
} from './algoreco.service'

function createDefaultTaskState() {
  return {
    status: 'idle',
    loading: false,
    source: '',
    algorithmVersion: '',
    generatedAt: '',
    lastRunAt: '',
    errorMessage: '',
    resultTotal: 0,
    resultItems: [],
    rawResult: null,
    forceRefreshUnsupported: false
  }
}

function parseDateValue(value) {
  if (!value) return null
  const date = value instanceof Date ? value : new Date(value)
  const timestamp = date.getTime()
  if (Number.isNaN(timestamp)) return null
  return timestamp
}

function normalizeKeyword(value) {
  return String(value || '').trim().toLowerCase()
}

function resolveErrorMessage(error) {
  return (
    error?.response?.data?.message ||
    error?.response?.data?.msg ||
    error?.message ||
    '请求失败'
  )
}

export default {
  name: 'Algoreco',
  layout: 'manage',
  data() {
    return {
      listLoading: false,
      batchRunning: false,
      listError: '',
      recommendEndpointMissing: false,

      taskSeedList: [],
      taskStateMap: {},

      recommendSize: DEFAULT_RECO_SIZE,
      recommendSizeOptions: [DEFAULT_RECO_SIZE, 8, MAX_RECO_SIZE],

      filters: {
        keyword: '',
        status: '',
        scene: '',
        timeRange: []
      },
      sceneOptions: [
        { label: '全部场景', value: '' },
        { label: '博客详情推荐', value: RECO_SCENE_CODE }
      ],
      taskStatusOptions: [
        { label: '全部状态', value: '' },
        { label: '未执行', value: 'idle' },
        { label: '执行中', value: 'running' },
        { label: '成功', value: 'success' },
        { label: '无结果', value: 'empty' },
        { label: '失败', value: 'failed' }
      ],

      page: {
        current: 1,
        size: 10
      },

      detailVisible: false,
      detailBlogId: null,
      detailBlog: null,
      detailBlogLoading: false
    }
  },
  computed: {
    taskRows() {
      return this.taskSeedList.map(item => {
        const task = this.taskStateMap[item.id] || createDefaultTaskState()
        return {
          ...item,
          scene: RECO_SCENE_CODE,
          task
        }
      })
    },
    filteredTaskRows() {
      const keyword = normalizeKeyword(this.filters.keyword)
      const statusFilter = String(this.filters.status || '').trim().toLowerCase()
      const sceneFilter = String(this.filters.scene || '').trim()
      const [timeStartRaw, timeEndRaw] = Array.isArray(this.filters.timeRange) ? this.filters.timeRange : []
      const timeStart = parseDateValue(timeStartRaw)
      const timeEnd = parseDateValue(timeEndRaw)

      return this.taskRows.filter(row => {
        if (sceneFilter && row.scene !== sceneFilter) return false

        if (statusFilter && String(row.task.status || '').toLowerCase() !== statusFilter) {
          return false
        }

        if (keyword) {
          const title = normalizeKeyword(row.title)
          const summary = normalizeKeyword(row.summary)
          const author = normalizeKeyword(this.getAuthorName(row.author))
          const blogId = String(row.id || '')
          if (
            !title.includes(keyword) &&
            !summary.includes(keyword) &&
            !author.includes(keyword) &&
            !blogId.includes(keyword)
          ) {
            return false
          }
        }

        if (timeStart !== null && timeEnd !== null) {
          const anchor = parseDateValue(
            row.task.lastRunAt ||
            row.task.generatedAt ||
            row.updatedAt ||
            row.publishTime ||
            row.createdAt
          )
          if (anchor === null || anchor < timeStart || anchor > timeEnd) {
            return false
          }
        }

        return true
      })
    },
    pagedTaskRows() {
      const start = (this.page.current - 1) * this.page.size
      return this.filteredTaskRows.slice(start, start + this.page.size)
    },
    detailRow() {
      if (!this.detailBlogId) return null
      return this.taskRows.find(item => String(item.id) === String(this.detailBlogId)) || null
    },
    detailTask() {
      if (!this.detailRow) return null
      return this.detailRow.task
    },
    emptyDescription() {
      if (this.listLoading) return '正在加载推荐任务...'
      if (this.listError) return '列表加载失败，请刷新后重试'
      return '暂无符合条件的推荐任务'
    }
  },
  watch: {
    filteredTaskRows() {
      this.ensureCurrentPageInRange()
    }
  },
  mounted() {
    this.loadTaskSeeds()
  },
  methods: {
    async loadTaskSeeds() {
      this.listLoading = true
      this.listError = ''
      try {
        const list = await fetchAlgoRecoTaskSeeds()
        this.taskSeedList = Array.isArray(list) ? list : []
        this.ensureTaskStateMap(this.taskSeedList)
      } catch (error) {
        this.taskSeedList = []
        this.listError = resolveErrorMessage(error)
        this.$message.error(`加载推荐任务失败：${this.listError}`)
      } finally {
        this.listLoading = false
      }
    },

    ensureTaskStateMap(seedList) {
      ;(Array.isArray(seedList) ? seedList : []).forEach(item => {
        if (!item || item.id === null || item.id === undefined) return
        if (!this.taskStateMap[item.id]) {
          this.$set(this.taskStateMap, item.id, createDefaultTaskState())
        }
      })
    },

    ensureCurrentPageInRange() {
      const total = this.filteredTaskRows.length
      const maxPage = Math.max(1, Math.ceil(total / this.page.size))
      if (this.page.current > maxPage) {
        this.page.current = maxPage
      }
    },

    handleFilterSubmit() {
      this.page.current = 1
    },
    resetFilters() {
      this.filters.keyword = ''
      this.filters.status = ''
      this.filters.scene = ''
      this.filters.timeRange = []
      this.page.current = 1
    },
    handlePageChange(page) {
      this.page.current = page
    },
    handleSizeChange(size) {
      this.page.size = size
      this.page.current = 1
    },

    getAuthorName(author) {
      if (!author) return '未知作者'
      if (typeof author === 'string') return author
      return author.displayName || author.nickname || author.username || '未知作者'
    },
    getBlogStatusType(status) {
      const s = String(status || '').toLowerCase()
      if (s === 'published') return 'success'
      if (s === 'pending') return 'warning'
      if (s === 'rejected') return 'danger'
      return 'info'
    },
    getTaskStatusType(status) {
      const s = String(status || '').toLowerCase()
      if (s === 'success') return 'success'
      if (s === 'running') return 'warning'
      if (s === 'empty') return 'info'
      if (s === 'failed') return 'danger'
      return ''
    },
    getTaskStatusText(status) {
      const s = String(status || '').toLowerCase()
      if (s === 'success') return '成功'
      if (s === 'running') return '执行中'
      if (s === 'empty') return '无结果'
      if (s === 'failed') return '失败'
      return '未执行'
    },
    getSourceText(source) {
      const s = String(source || '').toLowerCase()
      if (s === 'algorithm') return '算法结果'
      if (s === 'mixed') return '算法 + 补充'
      if (s === 'fallback') return '回退结果'
      return '-'
    },
    formatSceneLabel(scene) {
      if (scene === RECO_SCENE_CODE) return '博客详情推荐'
      return '未知场景'
    },
    getResultCountText(task) {
      if (!task) return '-'
      if (task.status === 'idle') return '-'
      return Number(task.resultTotal || 0)
    },
    getTriggerButtonText(status) {
      const s = String(status || '').toLowerCase()
      if (s === 'success' || s === 'empty') return '再次触发'
      if (s === 'running') return '执行中'
      return '触发推荐'
    },
    formatDateTime(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return '-'
      return date.toLocaleString('zh-CN')
    },

    async runTask(row, { forceRefresh = false, silent = false } = {}) {
      if (!row || row.id === null || row.id === undefined) return
      const task = this.taskStateMap[row.id]
      if (!task || task.loading) return

      const previousGeneratedAt = task.generatedAt
      task.status = 'running'
      task.loading = true
      task.errorMessage = ''
      task.forceRefreshUnsupported = false
      task.lastRunAt = new Date().toISOString()

      try {
        const result = await triggerAlgoRecoTask(row.id, {
          size: this.recommendSize,
          forceRefresh
        })
        task.source = result.source || 'fallback'
        task.algorithmVersion = result.algorithmVersion || ''
        task.generatedAt = result.generatedAt || ''
        task.resultItems = Array.isArray(result.items) ? result.items : []
        task.rawResult = result.raw || null
        task.resultTotal = Number(result.total || task.resultItems.length || 0)
        task.status = task.resultTotal > 0 ? 'success' : 'empty'
        task.lastRunAt = new Date().toISOString()

        if (
          forceRefresh &&
          previousGeneratedAt &&
          task.generatedAt &&
          String(previousGeneratedAt) === String(task.generatedAt)
        ) {
          task.forceRefreshUnsupported = true
        }

        if (!silent) {
          if (task.status === 'empty') {
            this.$message.warning('触发成功，但当前没有推荐结果')
          } else {
            this.$message.success(`推荐完成，返回 ${task.resultTotal} 条结果`)
          }
        }
      } catch (error) {
        task.status = 'failed'
        task.resultItems = []
        task.resultTotal = 0
        task.rawResult = null
        task.errorMessage = resolveErrorMessage(error)
        task.lastRunAt = new Date().toISOString()

        if (Number(error?.response?.status) === 404) {
          this.recommendEndpointMissing = true
        }

        if (!silent) {
          this.$message.error(`推荐执行失败：${task.errorMessage}`)
        }
      } finally {
        task.loading = false
      }

      if (this.detailVisible && String(this.detailBlogId) === String(row.id)) {
        this.$forceUpdate()
      }
    },

    async rerunTask(row) {
      if (!row || row.task.loading) return
      try {
        await this.$confirm('将重新生成推荐结果，是否继续？', '重新生成确认', { type: 'warning' })
        await this.runTask(row, { forceRefresh: true })
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(resolveErrorMessage(error))
        }
      }
    },

    async runBatchOnCurrentPage() {
      const rows = [...this.pagedTaskRows]
      if (!rows.length) return

      this.batchRunning = true
      let failedCount = 0
      for (const row of rows) {
        await this.runTask(row, { forceRefresh: false, silent: true })
        if ((this.taskStateMap[row.id] || {}).status === 'failed') {
          failedCount += 1
        }
      }
      this.batchRunning = false

      if (failedCount > 0) {
        this.$message.warning(`批量触发完成，失败 ${failedCount} 项`)
      } else {
        this.$message.success('批量触发完成')
      }
    },

    openBlogDetail(row) {
      if (!row || !row.id) return
      this.$router.push(`/blog/${row.id}`)
    },

    async openTaskDetail(row) {
      if (!row || !row.id) return
      this.detailBlogId = row.id
      this.detailVisible = true
      await this.loadDetailBlog(row.id)
    },

    async loadDetailBlog(blogId) {
      if (!blogId) return
      this.detailBlogLoading = true
      try {
        this.detailBlog = await fetchAlgoRecoBlogDetail(blogId)
      } catch (error) {
        this.detailBlog = null
        this.$message.warning(`加载博客详情失败：${resolveErrorMessage(error)}`)
      } finally {
        this.detailBlogLoading = false
      }
    }
  }
}
</script>

<style scoped>
.algoreco-page { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; gap: 16px; }
.page-header h1 { margin: 0; font-size: 24px; color: var(--it-text); }
.page-header p { margin: 6px 0 0; color: var(--it-text-subtle); font-size: 13px; }
.header-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.size-select { width: 124px; }
.page-alert { margin-bottom: 12px; }
.tab-card { border-radius: 18px; border: 1px solid var(--it-border); background: var(--it-panel-bg); box-shadow: var(--it-shadow-soft); }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.toolbar-left { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.pagination-box { display: flex; justify-content: flex-end; margin-top: 16px; }
.ellipsis-text { display: inline-block; max-width: 100%; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.error-text { color: #f56c6c; }
.table-empty { padding: 20px 0 8px; }
.detail-wrap { color: var(--it-text); }
.detail-desc { margin-bottom: 12px; }
.detail-alert { margin-bottom: 12px; }
.detail-empty { padding: 16px 0; }
.detail-loading { color: var(--it-text-subtle); padding: 12px 0; }
.detail-blog-meta { display: grid; gap: 8px; color: var(--it-text); }

@media (max-width: 1024px) {
  .page-header { flex-direction: column; align-items: flex-start; }
}
</style>
