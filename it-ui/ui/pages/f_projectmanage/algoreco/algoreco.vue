<template>
  <div class="project-algoreco-page">
    <AdminPageHeader
      title="项目算法推荐"
      description="管理项目推荐任务，支持筛选、手动触发、失败排查与结果详情查看。">
      <template slot="extra">
        <el-select v-model="recommendSize" size="small" class="size-select">
          <el-option v-for="item in recommendSizeOptions" :key="item" :label="`推荐条数 ${item}`" :value="item" />
        </el-select>
        <el-button size="small" icon="el-icon-refresh" :loading="listLoading" @click="loadTaskSeeds">刷新列表</el-button>
        <el-button
          size="small"
          type="primary"
          icon="el-icon-cpu"
          :loading="batchRunning"
          :disabled="listLoading || pagedTaskRows.length === 0"
          @click="runBatchOnCurrentPage">
          触发当前页推荐
        </el-button>
      </template>
    </AdminPageHeader>

    <AdminToolbarCard class="alert-card">
      <el-alert
        title="当前页面基于“项目列表 + 推荐接口”构建推荐任务视图；项目推荐任务列表暂未独立接口。"
        type="info"
        :closable="false"
        show-icon />
      <el-alert
        v-if="preferredEndpointMissing"
        title="检测到 /project/{id}/recommendations 不可用，已自动回退到 /project/{id}/related。"
        type="warning"
        :closable="false"
        show-icon
        class="mt-10" />
      <el-alert
        v-if="listError"
        :title="`任务列表加载失败：${listError}`"
        type="error"
        :closable="false"
        show-icon
        class="mt-10" />
    </AdminToolbarCard>

    <AdminToolbarCard class="filter-card">
      <div class="filter-toolbar">
        <div class="filter-left">
          <el-select
            v-model="filters.projectId"
            clearable
            filterable
            placeholder="项目"
            style="width: 260px">
            <el-option
              v-for="item in projectOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value" />
          </el-select>

          <el-select
            v-model="filters.status"
            clearable
            placeholder="任务状态"
            style="width: 140px; margin-left: 10px">
            <el-option v-for="item in taskStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>

          <el-select
            v-model="filters.type"
            clearable
            placeholder="项目类型"
            style="width: 160px; margin-left: 10px">
            <el-option v-for="item in projectTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>

          <el-date-picker
            v-model="filters.timeRange"
            type="datetimerange"
            value-format="yyyy-MM-dd HH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 340px; margin-left: 10px" />
        </div>

        <div class="filter-right">
          <el-button type="primary" @click="handleFilterSubmit">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </div>
    </AdminToolbarCard>

    <AdminTableCard class="table-card">
      <el-table
        :data="pagedTaskRows"
        v-loading="listLoading"
        border
        stripe
        row-key="id"
        style="width: 100%">
        <el-table-column prop="id" label="项目ID" width="90" />
        <el-table-column prop="name" label="项目名称" min-width="220" show-overflow-tooltip />

        <el-table-column label="类型" width="130" align="center">
          <template slot-scope="{ row }">
            <StatusTag
              :value="normalizeTypeKey(row.type)"
              :text-map="projectTypeTextMap"
              :type-map="projectTypeTagMap"
              default-type="info"
              size="small" />
          </template>
        </el-table-column>

        <el-table-column label="项目状态" width="120" align="center">
          <template slot-scope="{ row }">
            <StatusTag
              :value="row.status"
              :text-map="projectStatusTextMap"
              :type-map="projectStatusTagMap"
              default-type="info"
              size="small" />
          </template>
        </el-table-column>

        <el-table-column label="场景" width="120" align="center">
          <template slot-scope="{ row }">
            <el-tag type="info" size="mini">{{ formatSceneLabel(row.scene) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="任务状态" width="110" align="center">
          <template slot-scope="{ row }">
            <StatusTag
              :value="row.task.status"
              :text-map="taskStatusTextMap"
              :type-map="taskStatusTagMap"
              default-type="info"
              size="small" />
          </template>
        </el-table-column>

        <el-table-column label="推荐数" width="90" align="center">
          <template slot-scope="{ row }">
            {{ getResultCountText(row.task) }}
          </template>
        </el-table-column>

        <el-table-column label="推荐来源" width="120" align="center">
          <template slot-scope="{ row }">
            {{ getSourceText(row.task.source) }}
          </template>
        </el-table-column>

        <el-table-column label="生成时间" width="170" align="center">
          <template slot-scope="{ row }">
            {{ formatDateTime(row.task.generatedAt) }}
          </template>
        </el-table-column>

        <el-table-column label="最近执行" width="170" align="center">
          <template slot-scope="{ row }">
            {{ formatDateTime(row.task.lastRunAt || row.updatedAt || row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="失败信息" min-width="170">
          <template slot-scope="{ row }">
            <el-tooltip v-if="row.task.errorMessage" :content="row.task.errorMessage" placement="top">
              <span class="ellipsis-text error-text">{{ row.task.errorMessage }}</span>
            </el-tooltip>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template slot-scope="{ row }">
            <AdminActionGroup>
              <el-button type="text" size="mini" :loading="row.task.loading" @click="runTask(row)">
                {{ getTriggerButtonText(row.task.status) }}
              </el-button>
              <el-button
                type="text"
                size="mini"
                :disabled="row.task.loading || row.task.status !== 'failed'"
                @click="retryTask(row)">
                重试
              </el-button>
              <el-button type="text" size="mini" @click="openTaskDetail(row)">详情</el-button>
              <el-button type="text" size="mini" @click="openProjectDetail(row.id)">项目</el-button>
            </AdminActionGroup>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!listLoading && filteredTaskRows.length === 0" class="table-empty">
        <el-empty :description="emptyDescription" :image-size="80" />
      </div>

      <template slot="pagination">
        <el-pagination
          :current-page="page.current"
          :page-sizes="[10, 20, 50]"
          :page-size="page.size"
          :total="filteredTaskRows.length"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange" />
      </template>
    </AdminTableCard>

    <AdminFormDialog
      :title="detailRow ? `${detailRow.name} - 推荐任务详情` : '推荐任务详情'"
      :visible.sync="detailVisible"
      width="82%">
      <div v-if="detailRow && detailTask" class="detail-wrap">
        <el-descriptions :column="2" border class="detail-desc">
          <el-descriptions-item label="项目ID">{{ detailRow.id }}</el-descriptions-item>
          <el-descriptions-item label="项目名称">{{ detailRow.name }}</el-descriptions-item>
          <el-descriptions-item label="任务状态">{{ taskStatusTextMap[detailTask.status] || '未执行' }}</el-descriptions-item>
          <el-descriptions-item label="场景">{{ formatSceneLabel(detailRow.scene) }}</el-descriptions-item>
          <el-descriptions-item label="推荐来源">{{ getSourceText(detailTask.source) }}</el-descriptions-item>
          <el-descriptions-item label="推荐条数">{{ getResultCountText(detailTask) }}</el-descriptions-item>
          <el-descriptions-item label="算法版本">{{ detailTask.algorithmVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="生成时间">{{ formatDateTime(detailTask.generatedAt) }}</el-descriptions-item>
          <el-descriptions-item label="最近执行">{{ formatDateTime(detailTask.lastRunAt) }}</el-descriptions-item>
          <el-descriptions-item label="项目更新时间">{{ formatDateTime(detailRow.updatedAt || detailRow.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="detailTask.errorMessage"
          :title="detailTask.errorMessage"
          type="error"
          :closable="false"
          show-icon
          class="detail-alert" />

        <el-alert
          v-if="detailTask.forceRefreshUnsupported"
          title="已发起重试，但返回时间戳未变化，可能是推荐结果本身未发生变化。"
          type="warning"
          :closable="false"
          show-icon
          class="detail-alert" />

        <el-divider content-position="left">推荐结果</el-divider>
        <el-table
          v-if="detailTask.resultItems && detailTask.resultItems.length"
          :data="detailTask.resultItems"
          border
          stripe
          size="mini"
          style="width: 100%">
          <el-table-column prop="id" label="项目ID" width="90" />
          <el-table-column prop="name" label="项目名称" min-width="180" show-overflow-tooltip />
          <el-table-column label="类型" width="120" align="center">
            <template slot-scope="{ row }">
              <StatusTag
                :value="normalizeTypeKey(row.type)"
                :text-map="projectTypeTextMap"
                :type-map="projectTypeTagMap"
                default-type="info"
                size="small" />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template slot-scope="{ row }">
              <StatusTag
                :value="row.status"
                :text-map="projectStatusTextMap"
                :type-map="projectStatusTagMap"
                default-type="info"
                size="small" />
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="170" align="center">
            <template slot-scope="{ row }">
              {{ formatDateTime(row.updatedAt || row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="description" label="项目描述" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="80" align="center">
            <template slot-scope="{ row }">
              <el-button type="text" size="mini" @click="openProjectDetail(row.id)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="detail-empty">
          <el-empty description="暂无推荐结果" :image-size="70" />
        </div>

        <el-divider content-position="left">项目信息</el-divider>
        <div v-if="detailProjectLoading" class="detail-loading">正在加载项目详情...</div>
        <el-descriptions v-else-if="detailProject" :column="2" border class="detail-desc">
          <el-descriptions-item label="项目名称">{{ detailProject.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="作者">{{ detailProject.authorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目状态">{{ projectStatusTextMap[detailProject.status] || detailProject.status || '-' }}</el-descriptions-item>
          <el-descriptions-item label="可见性">{{ detailProject.visibility || '-' }}</el-descriptions-item>
          <el-descriptions-item label="技术标签" :span="2">
            {{ detailProject.tags && detailProject.tags.length ? detailProject.tags.join(' / ') : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="项目描述" :span="2">{{ detailProject.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <template slot="footer">
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="detailRow"
          type="warning"
          :loading="detailTask && detailTask.loading"
          @click="retryTask(detailRow)">
          重试推荐
        </el-button>
        <el-button
          v-if="detailRow"
          type="primary"
          :loading="detailTask && detailTask.loading"
          @click="runTask(detailRow)">
          {{ detailTask ? getTriggerButtonText(detailTask.status) : '触发推荐' }}
        </el-button>
      </template>
    </AdminFormDialog>
  </div>
</template>

<script>
import AdminActionGroup from '@/components/admin/AdminActionGroup.vue'
import AdminFormDialog from '@/components/admin/AdminFormDialog.vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminToolbarCard from '@/components/admin/AdminToolbarCard.vue'
import StatusTag from '@/components/admin/StatusTag.vue'
import {
  DEFAULT_RECO_SIZE,
  MAX_RECO_SIZE,
  RECO_SCENE_CODE,
  fetchProjectRecoDetail,
  fetchProjectRecoTaskSeeds,
  resolveRecoErrorMessage,
  triggerProjectRecoTask
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

function normalizeText(value, fallback = '') {
  if (value === null || value === undefined) return fallback
  const text = String(value).trim()
  return text || fallback
}

export default {
  name: 'ProjectAlgoreco',
  layout: 'manage',
  components: {
    AdminActionGroup,
    AdminFormDialog,
    AdminPageHeader,
    AdminTableCard,
    AdminToolbarCard,
    StatusTag
  },
  data() {
    const routeProjectId = Number(this.$route?.query?.projectId || 0)
    return {
      listLoading: false,
      batchRunning: false,
      listError: '',
      preferredEndpointMissing: false,

      taskSeedList: [],
      taskStateMap: {},

      recommendSize: DEFAULT_RECO_SIZE,
      recommendSizeOptions: [DEFAULT_RECO_SIZE, 8, MAX_RECO_SIZE],

      filters: {
        projectId: routeProjectId > 0 ? routeProjectId : '',
        status: '',
        type: '',
        timeRange: []
      },
      taskStatusOptions: [
        { label: '全部状态', value: '' },
        { label: '未执行', value: 'idle' },
        { label: '执行中', value: 'running' },
        { label: '成功', value: 'success' },
        { label: '无结果', value: 'empty' },
        { label: '失败', value: 'failed' }
      ],
      taskStatusTextMap: {
        idle: '未执行',
        running: '执行中',
        success: '成功',
        empty: '无结果',
        failed: '失败'
      },
      taskStatusTagMap: {
        idle: 'info',
        running: 'warning',
        success: 'success',
        empty: 'info',
        failed: 'danger'
      },
      projectTypeTextMap: {
        frontend: '前端项目',
        backend: '后端项目',
        fullstack: '全栈项目',
        mobile: '移动应用',
        ai: 'AI 项目',
        tools: '工具项目',
        other: '其他'
      },
      projectTypeTagMap: {
        frontend: 'primary',
        backend: 'success',
        fullstack: 'warning',
        mobile: 'success',
        ai: 'danger',
        tools: 'info',
        other: 'info'
      },
      projectStatusTextMap: {
        draft: '草稿',
        pending: '待审核',
        published: '已发布',
        rejected: '已拒绝',
        archived: '已归档',
        unknown: '未知'
      },
      projectStatusTagMap: {
        draft: 'info',
        pending: 'warning',
        published: 'success',
        rejected: 'danger',
        archived: 'info',
        unknown: 'info'
      },

      page: {
        current: 1,
        size: 10
      },

      detailVisible: false,
      detailProjectId: null,
      detailProjectLoading: false,
      detailProject: null
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
    projectOptions() {
      return this.taskSeedList.map(item => ({
        label: `${item.name} (#${item.id})`,
        value: item.id
      }))
    },
    projectTypeOptions() {
      const map = new Map()
      this.taskSeedList.forEach(item => {
        const typeKey = this.normalizeTypeKey(item.type)
        if (!typeKey || map.has(typeKey)) return
        map.set(typeKey, {
          value: typeKey,
          label: this.projectTypeTextMap[typeKey] || normalizeText(item.type, '其他')
        })
      })
      return [{ value: '', label: '全部类型' }, ...Array.from(map.values())]
    },
    filteredTaskRows() {
      const statusFilter = normalizeText(this.filters.status).toLowerCase()
      const projectIdFilter = Number(this.filters.projectId || 0)
      const typeFilter = normalizeText(this.filters.type).toLowerCase()
      const [timeStartRaw, timeEndRaw] = Array.isArray(this.filters.timeRange) ? this.filters.timeRange : []
      const timeStart = parseDateValue(timeStartRaw)
      const timeEnd = parseDateValue(timeEndRaw)

      return this.taskRows.filter(row => {
        if (projectIdFilter && Number(row.id) !== projectIdFilter) return false
        if (statusFilter && normalizeText(row.task.status).toLowerCase() !== statusFilter) return false
        if (typeFilter && this.normalizeTypeKey(row.type) !== typeFilter) return false

        if (timeStart !== null && timeEnd !== null) {
          const anchor = parseDateValue(
            row.task.lastRunAt ||
            row.task.generatedAt ||
            row.updatedAt ||
            row.createdAt
          )
          if (anchor === null || anchor < timeStart || anchor > timeEnd) return false
        }

        return true
      })
    },
    pagedTaskRows() {
      const start = (this.page.current - 1) * this.page.size
      return this.filteredTaskRows.slice(start, start + this.page.size)
    },
    detailRow() {
      if (!this.detailProjectId) return null
      return this.taskRows.find(item => String(item.id) === String(this.detailProjectId)) || null
    },
    detailTask() {
      if (!this.detailRow) return null
      return this.detailRow.task
    },
    emptyDescription() {
      if (this.listLoading) return '正在加载推荐任务...'
      if (this.listError) return '任务列表加载失败，请刷新重试'
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
    normalizeTypeKey(type) {
      return normalizeText(type, 'other').toLowerCase()
    },
    formatSceneLabel(scene) {
      if (scene === RECO_SCENE_CODE) return '项目相关推荐'
      return '项目推荐'
    },
    formatDateTime(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return '-'
      return date.toLocaleString('zh-CN')
    },
    getSourceText(source) {
      const value = normalizeText(source).toLowerCase()
      if (value === 'algorithm') return '算法结果'
      if (value === 'mixed') return '算法 + 补充'
      if (value === 'fallback') return '回退结果'
      if (value === 'related') return '相关项目'
      return '-'
    },
    getResultCountText(task) {
      if (!task || task.status === 'idle') return '-'
      return Number(task.resultTotal || 0)
    },
    getTriggerButtonText(status) {
      const value = normalizeText(status).toLowerCase()
      if (value === 'running') return '执行中'
      if (value === 'success' || value === 'empty') return '再次触发'
      if (value === 'failed') return '重新触发'
      return '触发推荐'
    },
    ensureCurrentPageInRange() {
      const total = this.filteredTaskRows.length
      const maxPage = Math.max(1, Math.ceil(total / this.page.size))
      if (this.page.current > maxPage) {
        this.page.current = maxPage
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
    handleFilterSubmit() {
      this.page.current = 1
    },
    resetFilters() {
      this.filters.projectId = ''
      this.filters.status = ''
      this.filters.type = ''
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
    async loadTaskSeeds() {
      this.listLoading = true
      this.listError = ''
      try {
        const list = await fetchProjectRecoTaskSeeds()
        this.taskSeedList = Array.isArray(list) ? list : []
        this.ensureTaskStateMap(this.taskSeedList)
      } catch (error) {
        this.taskSeedList = []
        this.listError = resolveRecoErrorMessage(error)
        this.$message.error(`加载推荐任务失败：${this.listError}`)
      } finally {
        this.listLoading = false
      }
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
        const result = await triggerProjectRecoTask(row.id, {
          size: this.recommendSize,
          forceRefresh
        })
        task.source = result.source || 'related'
        task.algorithmVersion = result.algorithmVersion || ''
        task.generatedAt = result.generatedAt || ''
        task.resultItems = Array.isArray(result.items) ? result.items : []
        task.rawResult = result.raw || null
        task.resultTotal = Number(result.total || task.resultItems.length || 0)
        task.status = task.resultTotal > 0 ? 'success' : 'empty'
        task.lastRunAt = new Date().toISOString()

        if (result.preferredEndpointMissing) {
          this.preferredEndpointMissing = true
        }

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
            this.$message.warning('触发成功，但暂无推荐结果')
          } else {
            this.$message.success(`推荐完成，返回 ${task.resultTotal} 条结果`)
          }
        }
      } catch (error) {
        task.status = 'failed'
        task.resultItems = []
        task.resultTotal = 0
        task.rawResult = null
        task.errorMessage = resolveRecoErrorMessage(error)
        task.lastRunAt = new Date().toISOString()

        if (!silent) {
          this.$message.error(`推荐执行失败：${task.errorMessage}`)
        }
      } finally {
        task.loading = false
      }

      if (this.detailVisible && String(this.detailProjectId) === String(row.id)) {
        this.$forceUpdate()
      }
    },
    async retryTask(row) {
      if (!row || row.task.loading) return
      try {
        await this.$confirm('将重新触发推荐任务，是否继续？', '重试确认', { type: 'warning' })
        await this.runTask(row, { forceRefresh: true })
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(resolveRecoErrorMessage(error))
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
    async openTaskDetail(row) {
      if (!row || !row.id) return
      this.detailProjectId = row.id
      this.detailVisible = true
      await this.loadDetailProject(row.id)
    },
    async loadDetailProject(projectId) {
      if (!projectId) return
      this.detailProjectLoading = true
      try {
        this.detailProject = await fetchProjectRecoDetail(projectId)
      } catch (error) {
        this.detailProject = null
        this.$message.warning(`加载项目详情失败：${resolveRecoErrorMessage(error)}`)
      } finally {
        this.detailProjectLoading = false
      }
    },
    openProjectDetail(projectId) {
      if (!projectId) return
      this.$router.push(`/projectdetail?projectId=${projectId}`)
    }
  }
}
</script>

<style scoped>
.project-algoreco-page {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.alert-card {
  margin-bottom: 18px;
}

.filter-card {
  margin-bottom: 18px;
}

.size-select {
  width: 136px;
}

.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.filter-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.filter-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-card {
  margin-bottom: 0;
}

.table-empty {
  padding: 18px 0 8px;
}

.ellipsis-text {
  display: inline-block;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.error-text {
  color: #f56c6c;
}

.detail-wrap {
  color: var(--it-text);
}

.detail-desc {
  margin-bottom: 12px;
}

.detail-alert {
  margin-bottom: 12px;
}

.detail-empty {
  padding: 10px 0;
}

.detail-loading {
  color: var(--it-text-subtle);
  padding: 12px 0;
}

.mt-10 {
  margin-top: 10px;
}

@media (max-width: 768px) {
  .project-algoreco-page {
    padding: 16px;
  }

  .filter-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-left {
    width: 100%;
    gap: 10px;
  }

  .filter-right {
    width: 100%;
    justify-content: flex-end;
  }

  .filter-left :deep(.el-select),
  .filter-left :deep(.el-date-editor) {
    width: 100% !important;
    margin-left: 0 !important;
  }
}
</style>
