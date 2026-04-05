<template>
  <el-drawer
    :visible.sync="innerVisible"
    size="960px"
    append-to-body
    custom-class="task-collab-drawer"
    @closed="handleClosed"
  >
    <div slot="title" class="task-collab-drawer-title">
      <div class="task-collab-drawer-heading">任务协作中心</div>
      <div v-if="taskData" class="task-collab-drawer-subtitle">{{ taskData.title || '未命名任务' }}</div>
    </div>

    <div v-if="taskData" class="task-collab-drawer-shell">
      <div class="task-collab-hero">
        <div class="task-collab-hero-main">
          <div class="task-collab-eyebrow">任务详情</div>

          <div class="task-collab-title-row">
            <div class="task-collab-title-text">{{ taskData.title || '未命名任务' }}</div>
            <div class="task-collab-title-tags">
              <el-tag size="mini" effect="plain" :type="getTaskPriorityType(taskData.priority)">
                {{ getTaskPriorityText(taskData.priority) }}
              </el-tag>
              <el-tag size="mini" :type="getTaskStatusType(taskData.status)">
                {{ getTaskStatusText(taskData.status) }}
              </el-tag>
              <el-tag v-if="isTaskOverdue(taskData)" size="mini" type="danger">已逾期</el-tag>
            </div>
          </div>

          <div class="task-collab-desc">
            {{ taskData.description || '这条任务还没有补充详细描述，可以先在任务管理页补充背景、目标和验收标准。' }}
          </div>

          <div class="task-collab-meta-grid">
            <div class="task-collab-meta-card">
              <div class="task-collab-meta-label">负责人</div>
              <div class="task-collab-meta-value">{{ getTaskAssigneeName(taskData) }}</div>
            </div>
            <div class="task-collab-meta-card">
              <div class="task-collab-meta-label">截止时间</div>
              <div class="task-collab-meta-value">{{ getTaskDueLabel(taskData) }}</div>
            </div>
            <div class="task-collab-meta-card">
              <div class="task-collab-meta-label">创建人</div>
              <div class="task-collab-meta-value">{{ getTaskCreatorName(taskData) }}</div>
            </div>
            <div class="task-collab-meta-card">
              <div class="task-collab-meta-label">最近更新</div>
              <div class="task-collab-meta-value">{{ formatTaskShortTime(taskData.updatedAt || taskData.createdAt) }}</div>
            </div>
          </div>
        </div>

        <div class="task-collab-side-box">
          <div class="task-collab-side-title">快捷操作</div>

          <el-select
            v-model="quickStatus"
            size="mini"
            class="task-collab-status-select"
            :disabled="statusUpdating"
            @change="handleQuickStatusChange"
          >
            <el-option label="待处理" value="todo" />
            <el-option label="进行中" value="in_progress" />
            <el-option label="已完成" value="done" />
          </el-select>

          <div v-if="taskData.status === 'done' && !canManageProject" class="task-collab-reopen-tip">
            <i class="el-icon-info"></i>
            <span>已完成任务改回未完成时，会先判断是不是上一入组周期完成的旧任务；不是旧任务时才允许提交重开申请。</span>
          </div>

          <div v-if="latestReopenRequest" class="task-collab-reopen-banner" :class="`is-${latestReopenRequest.status || 'pending'}`">
            <div class="task-collab-reopen-banner-top">
              <div class="task-collab-reopen-banner-title">重开申请{{ pendingReopenCount ? `（待处理 ${pendingReopenCount}）` : '' }}</div>
              <el-tag size="mini" :type="getReopenStatusType(latestReopenRequest.status)">{{ getReopenStatusText(latestReopenRequest.status) }}</el-tag>
            </div>
            <div class="task-collab-reopen-banner-desc">
              {{ reopenBannerText }}
            </div>
            <div class="task-collab-reopen-banner-actions">
              <el-button size="mini" plain @click="setTab('reopen')">去处理</el-button>
              <template v-if="canManageProject && latestPendingReopenRequest">
                <el-button size="mini" type="success" plain :loading="reviewLoadingId === latestPendingReopenRequest.id" @click="handleApproveReopen(latestPendingReopenRequest)">通过</el-button>
                <el-button size="mini" type="danger" plain :loading="reviewLoadingId === latestPendingReopenRequest.id" @click="handleRejectReopen(latestPendingReopenRequest)">驳回</el-button>
              </template>
            </div>
          </div>

          <div class="task-collab-shortcuts">
            <el-button size="mini" plain @click="setTab('overview')">看概览</el-button>
            <el-button size="mini" plain @click="setTab('reopen')">看重开</el-button>
            <el-button size="mini" plain @click="setTab('comment')">去评论</el-button>
            <el-button size="mini" plain @click="setTab('attachment')">看附件</el-button>
            <el-button size="mini" plain @click="setTab('checklist')">看清单</el-button>
            <el-button size="mini" plain @click="setTab('dependency')">看依赖</el-button>
            <el-button size="mini" plain @click="setTab('log')">看时间线</el-button>
          </div>

          <el-button size="mini" type="text" :loading="overviewLoading || reopenLoading" @click="refreshAll">
            刷新概览
          </el-button>
        </div>
      </div>

      <div class="task-collab-tabs-card">
        <el-tabs v-model="innerActiveTab" class="task-collab-tabs">
          <el-tab-pane label="概览" name="overview">
            <div class="task-overview-panel" v-loading="overviewLoading">
              <div class="task-overview-stats">
                <div class="task-overview-stat">
                  <div class="task-overview-stat-value">{{ overview.commentCount }}</div>
                  <div class="task-overview-stat-label">评论</div>
                </div>
                <div class="task-overview-stat">
                  <div class="task-overview-stat-value">{{ overview.attachmentCount }}</div>
                  <div class="task-overview-stat-label">附件</div>
                </div>
                <div class="task-overview-stat">
                  <div class="task-overview-stat-value">{{ overview.dependencyCount }}</div>
                  <div class="task-overview-stat-label">依赖</div>
                </div>
                <div class="task-overview-stat">
                  <div class="task-overview-stat-value">{{ overview.logCount }}</div>
                  <div class="task-overview-stat-label">日志</div>
                </div>
              </div>

              <div class="task-overview-grid">
                <div class="task-overview-card">
                  <div class="task-overview-card-title">Checklist 进度</div>
                  <div class="task-overview-progress-row">
                    <el-progress
                      :percentage="overview.checklistPercent"
                      :status="overview.checklistPercent >= 100 ? 'success' : ''"
                      :stroke-width="10"
                    />
                  </div>
                  <div class="task-overview-card-desc">
                    已完成 {{ overview.checklistDone }} / {{ overview.checklistTotal }} 项
                  </div>
                </div>

                <div class="task-overview-card">
                  <div class="task-overview-card-title">任务基础信息</div>
                  <div class="task-overview-info-list">
                    <div class="task-overview-info-item">
                      <span>任务 ID</span>
                      <span>{{ taskData.id || '-' }}</span>
                    </div>
                    <div class="task-overview-info-item">
                      <span>项目 ID</span>
                      <span>{{ projectId || taskData.projectId || '-' }}</span>
                    </div>
                    <div class="task-overview-info-item">
                      <span>创建时间</span>
                      <span>{{ formatTaskShortTime(taskData.createdAt) }}</span>
                    </div>
                    <div class="task-overview-info-item">
                      <span>完成时间</span>
                      <span>{{ formatTaskShortTime(taskData.completedAt) }}</span>
                    </div>
                  </div>
                </div>

                <div class="task-overview-card">
                  <div class="task-overview-card-title">最近日志</div>
                  <div v-if="overview.latestLogs.length" class="task-overview-log-list">
                    <div
                      v-for="item in overview.latestLogs"
                      :key="item.id"
                      class="task-overview-log-item"
                    >
                      <div class="task-overview-log-top">
                        <span class="task-overview-log-action">{{ item.actionLabel || formatAction(item.action) }}</span>
                        <span class="task-overview-log-time">{{ formatTaskShortTime(item.createdAt) }}</span>
                      </div>
                      <div class="task-overview-log-desc">
                        {{ item.operatorName || '未知用户' }}
                        <span v-if="item.fieldLabel || item.fieldName"> · {{ item.fieldLabel || item.fieldName }}</span>
                      </div>
                    </div>
                  </div>
                  <el-empty v-else description="暂无日志" :image-size="50" />
                </div>

                <div class="task-overview-card task-overview-card-full">
                  <div class="task-overview-card-title">重开申请</div>
                  <div v-loading="reopenLoading">
                    <div v-if="reopenRequests.length" class="task-reopen-list">
                      <div v-for="item in reopenRequests" :key="item.id" class="task-reopen-item">
                        <div class="task-reopen-top">
                          <div class="task-reopen-title">
                            <span>{{ item.applicantName || '未知用户' }}</span>
                            <el-tag size="mini" :type="getReopenStatusType(item.status)">{{ getReopenStatusText(item.status) }}</el-tag>
                          </div>
                          <div class="task-reopen-time">{{ formatTaskShortTime(item.createdAt) }}</div>
                        </div>
                        <div class="task-reopen-desc">目标状态：{{ getTaskStatusText(item.targetStatus) }}</div>
                        <div class="task-reopen-desc">原因：{{ item.reason || '-' }}</div>
                        <div v-if="item.reviewRemark" class="task-reopen-desc">处理备注：{{ item.reviewRemark }}</div>
                        <div v-if="canManageProject && item.status === 'pending'" class="task-reopen-actions">
                          <el-button size="mini" type="success" plain :loading="reviewLoadingId === item.id" @click="handleApproveReopen(item)">通过</el-button>
                          <el-button size="mini" type="danger" plain :loading="reviewLoadingId === item.id" @click="handleRejectReopen(item)">驳回</el-button>
                        </div>
                      </div>
                    </div>
                    <el-empty v-else description="暂无重开申请" :image-size="50" />
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane name="reopen">
            <span slot="label">
              重开申请
              <span v-if="pendingReopenCount" class="task-tab-badge">{{ pendingReopenCount }}</span>
            </span>
            <div class="task-reopen-panel" v-loading="reopenLoading">
              <div class="task-reopen-panel-head">
                <div>
                  <div class="task-reopen-panel-title">任务重开流转</div>
                  <div class="task-reopen-panel-subtitle">这里集中处理“已完成任务改回未完成”的申请、审批和结果。</div>
                </div>
                <el-button size="mini" type="text" :loading="reopenLoading" @click="loadReopenRequests">刷新</el-button>
              </div>

              <div class="task-reopen-panel-summary">
                <div class="task-reopen-summary-card">
                  <div class="task-reopen-summary-value">{{ reopenRequests.length }}</div>
                  <div class="task-reopen-summary-label">全部申请</div>
                </div>
                <div class="task-reopen-summary-card warning">
                  <div class="task-reopen-summary-value">{{ pendingReopenCount }}</div>
                  <div class="task-reopen-summary-label">待处理</div>
                </div>
                <div class="task-reopen-summary-card success">
                  <div class="task-reopen-summary-value">{{ approvedReopenCount }}</div>
                  <div class="task-reopen-summary-label">已通过</div>
                </div>
                <div class="task-reopen-summary-card danger">
                  <div class="task-reopen-summary-value">{{ rejectedReopenCount }}</div>
                  <div class="task-reopen-summary-label">已驳回</div>
                </div>
              </div>

              <div v-if="reopenRequests.length" class="task-reopen-list task-reopen-list-detailed">
                <div v-for="item in reopenRequests" :key="item.id" class="task-reopen-item">
                  <div class="task-reopen-top">
                    <div class="task-reopen-title">
                      <span>{{ item.applicantName || '未知用户' }}</span>
                      <el-tag size="mini" :type="getReopenStatusType(item.status)">{{ getReopenStatusText(item.status) }}</el-tag>
                    </div>
                    <div class="task-reopen-time">{{ formatTaskShortTime(item.createdAt) }}</div>
                  </div>
                  <div class="task-reopen-grid">
                    <div class="task-reopen-field">
                      <span>原状态</span>
                      <strong>{{ getTaskStatusText(item.fromStatus) }}</strong>
                    </div>
                    <div class="task-reopen-field">
                      <span>目标状态</span>
                      <strong>{{ getTaskStatusText(item.targetStatus) }}</strong>
                    </div>
                    <div class="task-reopen-field">
                      <span>申请人</span>
                      <strong>{{ item.applicantName || '未知用户' }}</strong>
                    </div>
                    <div class="task-reopen-field">
                      <span>处理时间</span>
                      <strong>{{ formatTaskShortTime(item.reviewedAt) }}</strong>
                    </div>
                  </div>
                  <div class="task-reopen-desc">原因：{{ item.reason || '-' }}</div>
                  <div v-if="item.reviewRemark" class="task-reopen-desc">处理备注：{{ item.reviewRemark }}</div>
                  <div v-if="canManageProject && item.status === 'pending'" class="task-reopen-actions">
                    <el-button size="mini" type="success" plain :loading="reviewLoadingId === item.id" @click="handleApproveReopen(item)">通过申请</el-button>
                    <el-button size="mini" type="danger" plain :loading="reviewLoadingId === item.id" @click="handleRejectReopen(item)">驳回申请</el-button>
                  </div>
                </div>
              </div>
              <el-empty v-else description="当前任务还没有重开申请" :image-size="70" />
            </div>
          </el-tab-pane>

          <el-tab-pane label="评论讨论" name="comment">
            <TaskCommentPanel
              :key="panelKey('comment')"
              :task-id="taskData.id"
              @changed="handleChildChanged"
            />
          </el-tab-pane>

          <el-tab-pane label="任务附件" name="attachment">
            <TaskAttachmentPanel
              :key="panelKey('attachment')"
              :task-id="taskData.id"
              @changed="handleChildChanged"
            />
          </el-tab-pane>

          <el-tab-pane label="Checklist" name="checklist">
            <TaskChecklist
              :key="panelKey('checklist')"
              :task-id="taskData.id"
              :task="taskData"
              @changed="handleChildChanged"
            />
          </el-tab-pane>

          <el-tab-pane label="依赖关系" name="dependency">
            <TaskDependencyPanel
              :key="panelKey('dependency')"
              :task-id="taskData.id"
              :project-id="projectId"
              @changed="handleChildChanged"
            />
          </el-tab-pane>

          <el-tab-pane label="操作时间线" name="log">
            <TaskLogTimeline
              :key="panelKey('log')"
              :task-id="taskData.id"
            />
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <div v-else class="task-collab-drawer-empty">
      <el-empty description="请选择任务后查看协作详情" />
    </div>
  </el-drawer>
</template>

<script>
import request from '@/utils/request'
import {
  getTaskComments,
  getTaskChecklist,
  getTaskAttachments,
  getTaskDependencies,
  getTaskLogs,
  updateTaskStatus
} from '@/api/project'
import TaskCommentPanel from '../projectdetail/components/TaskCommentPanel.vue'
import TaskAttachmentPanel from '../projectdetail/components/TaskAttachmentPanel.vue'
import TaskChecklist from '../projectdetail/components/TaskChecklist.vue'
import TaskDependencyPanel from '../projectdetail/components/TaskDependencyPanel.vue'
import TaskLogTimeline from '../projectdetail/components/TaskLogTimeline.vue'

function createEmptyOverview() {
  return {
    commentCount: 0,
    attachmentCount: 0,
    dependencyCount: 0,
    logCount: 0,
    checklistTotal: 0,
    checklistDone: 0,
    checklistPercent: 0,
    latestLogs: []
  }
}

function extractApiData(res) {
  if (res == null) return null
  const payload = res.data !== undefined ? res.data : res
  if (payload && typeof payload === 'object' && payload.data !== undefined && (payload.code !== undefined || payload.success !== undefined || payload.message !== undefined)) {
    return payload.data
  }
  return payload
}

export default {
  name: 'ProjectTaskCollabDrawer',
  components: {
    TaskCommentPanel,
    TaskAttachmentPanel,
    TaskChecklist,
    TaskDependencyPanel,
    TaskLogTimeline
  },
  props: {
    visible: { type: Boolean, default: false },
    task: { type: Object, default: null },
    projectId: { type: [Number, String], default: null },
    currentUserId: { type: [Number, String], default: null },
    currentMemberJoinedAt: { type: [String, Number, Date], default: '' },
    canManageProject: { type: Boolean, default: false },
    activeTab: { type: String, default: 'overview' },
    refreshSeed: { type: Number, default: 0 }
  },
  data() {
    return {
      overviewLoading: false,
      overview: createEmptyOverview(),
      statusUpdating: false,
      quickStatus: 'todo',
      reopenLoading: false,
      reopenRequests: [],
      reviewLoadingId: null
    }
  },
  computed: {
    innerVisible: {
      get() { return this.visible },
      set(v) { this.$emit('update:visible', v) }
    },
    innerActiveTab: {
      get() { return this.activeTab || 'overview' },
      set(v) { this.$emit('update:activeTab', v) }
    },
    taskData() {
      return this.task || null
    },
    pendingReopenCount() {
      return this.reopenRequests.filter(item => item && item.status === 'pending').length
    },
    approvedReopenCount() {
      return this.reopenRequests.filter(item => item && item.status === 'approved').length
    },
    rejectedReopenCount() {
      return this.reopenRequests.filter(item => item && item.status === 'rejected').length
    },
    latestReopenRequest() {
      return this.reopenRequests.length ? this.reopenRequests[0] : null
    },
    latestPendingReopenRequest() {
      return this.reopenRequests.find(item => item && item.status === 'pending') || null
    },
    reopenBannerText() {
      const item = this.latestPendingReopenRequest || this.latestReopenRequest
      if (!item) return ''
      if (item.status === 'pending') {
        if (this.canManageProject) {
          return `当前有待你确认的重开申请：${item.applicantName || '未知用户'} 申请把任务改回 ${this.getTaskStatusText(item.targetStatus)}。`
        }
        return `你提交的重开申请正在等待管理员或所有者确认，目标状态是 ${this.getTaskStatusText(item.targetStatus)}。`
      }
      if (item.status === 'approved') {
        return `最近一次重开申请已通过，任务会回退到 ${this.getTaskStatusText(item.targetStatus)}。`
      }
      if (item.status === 'rejected') {
        return `最近一次重开申请已被驳回${item.reviewRemark ? `：${item.reviewRemark}` : '。'}`
      }
      return '当前任务存在重开申请记录，可点击进入查看。'
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(v) {
        if (v && this.taskData && this.taskData.id) {
          this.syncQuickStatus()
          this.refreshAll()
        }
      }
    },
    task: {
      deep: true,
      immediate: true,
      handler() {
        this.syncQuickStatus()
        if (this.visible && this.taskData && this.taskData.id) {
          this.refreshAll()
        } else if (!this.taskData) {
          this.overview = createEmptyOverview()
          this.reopenRequests = []
        }
      }
    },
    refreshSeed() {
      if (this.visible && this.taskData && this.taskData.id) {
        this.refreshAll()
      }
    }
  },
  methods: {
    syncQuickStatus() {
      this.quickStatus = (this.taskData && this.taskData.status) || 'todo'
    },
    setTab(tab) {
      this.innerActiveTab = tab
      this.$emit('tab-change', tab)
    },
    handleClosed() {
      this.$emit('close')
    },
    panelKey(name) {
      const taskId = this.taskData && this.taskData.id ? this.taskData.id : 'empty'
      return `${name}-${taskId}-${this.refreshSeed}`
    },
    async refreshAll() {
      await Promise.all([this.loadOverview(), this.loadReopenRequests()])
    },
    async loadOverview() {
      if (!this.taskData || !this.taskData.id) {
        this.overview = createEmptyOverview()
        return
      }
      try {
        this.overviewLoading = true
        const [commentsRes, checklistRes, attachmentsRes, dependenciesRes, logsRes] = await Promise.all([
          getTaskComments(this.taskData.id).catch(() => ({ data: [] })),
          getTaskChecklist(this.taskData.id).catch(() => ({ data: [] })),
          getTaskAttachments(this.taskData.id).catch(() => ({ data: [] })),
          getTaskDependencies(this.taskData.id).catch(() => ({ data: [] })),
          getTaskLogs(this.taskData.id).catch(() => ({ data: [] }))
        ])
        const comments = Array.isArray(extractApiData(commentsRes)) ? extractApiData(commentsRes) : []
        const checklist = Array.isArray(extractApiData(checklistRes)) ? extractApiData(checklistRes) : []
        const attachments = Array.isArray(extractApiData(attachmentsRes)) ? extractApiData(attachmentsRes) : []
        const dependencies = Array.isArray(extractApiData(dependenciesRes)) ? extractApiData(dependenciesRes) : []
        const logs = Array.isArray(extractApiData(logsRes)) ? extractApiData(logsRes) : []

        const checklistTotal = checklist.length
        const checklistDone = checklist.filter(item => item && (item.checked === true || item.checked === 1)).length
        const checklistPercent = checklistTotal ? Math.round((checklistDone / checklistTotal) * 100) : 0

        this.overview = {
          commentCount: comments.length,
          attachmentCount: attachments.length,
          dependencyCount: dependencies.length,
          logCount: logs.length,
          checklistTotal,
          checklistDone,
          checklistPercent,
          latestLogs: logs.slice(0, 4)
        }
      } catch (e) {
        const m = e.response?.data?.message || e.response?.data?.msg || '加载任务概览失败'
        this.$message.error(m)
        this.overview = createEmptyOverview()
      } finally {
        this.overviewLoading = false
      }
    },
    async loadReopenRequests() {
      if (!this.taskData || !this.taskData.id) {
        this.reopenRequests = []
        return
      }
      try {
        this.reopenLoading = true
        const res = await request({
          url: `/project/task/${this.taskData.id}/reopen-requests`,
          method: 'get'
        })
        const list = extractApiData(res)
        this.reopenRequests = Array.isArray(list) ? list : []
      } catch (e) {
        this.reopenRequests = []
      } finally {
        this.reopenLoading = false
      }
    },
    getComparableTaskCycleTime(value) {
      if (!value) return 0
      const time = new Date(value).getTime()
      return Number.isNaN(time) ? 0 : time
    },
    isHistoricalDoneTask(task) {
      if (!task || task.status !== 'done' || this.canManageProject) return false
      if (this.currentUserId === null || this.currentUserId === undefined || this.currentUserId === '') return false
      if (Number(task.assigneeId) !== Number(this.currentUserId)) return false
      const completedCycle = this.getComparableTaskCycleTime(task.completedMemberJoinedAt)
      const currentCycle = this.getComparableTaskCycleTime(this.currentMemberJoinedAt)
      if (!completedCycle || !currentCycle) return false
      return completedCycle !== currentCycle
    },
    async handleQuickStatusChange(value) {
      if (!this.taskData || !this.taskData.id || !value || value === this.taskData.status) {
        return
      }
      if (!this.canManageProject && this.taskData.status === 'done' && value !== 'done') {
        if (this.isHistoricalDoneTask(this.taskData)) {
          this.quickStatus = (this.taskData && this.taskData.status) || 'done'
          this.$message.error('你不能修改上一入组周期已完成的任务，请联系项目管理员或所有者处理')
          return
        }
        if (Number(this.taskData.assigneeId) !== Number(this.currentUserId)) {
          this.quickStatus = (this.taskData && this.taskData.status) || 'done'
          this.$message.error('只有当前负责人本人可以提交重开申请')
          return
        }
        await this.handleApplyReopen(value)
        this.quickStatus = (this.taskData && this.taskData.status) || 'done'
        return
      }
      await this.submitDirectStatusChange(value)
    },
    async submitDirectStatusChange(value) {
      try {
        this.statusUpdating = true
        const res = await updateTaskStatus(this.taskData.id, { status: value })
        const data = extractApiData(res) || { status: value }
        this.$message.success('任务状态已更新')
        this.$emit('status-updated', { taskId: this.taskData.id, status: data.status || value })
        this.$emit('changed', { taskId: this.taskData.id, reason: 'status-updated', status: data.status || value, partial: true })
        await this.refreshAll()
      } catch (e) {
        this.quickStatus = (this.taskData && this.taskData.status) || 'todo'
        const m = e.response?.data?.message || e.response?.data?.msg || '更新任务状态失败'
        this.$message.error(m)
      } finally {
        this.statusUpdating = false
      }
    },
    async handleApplyReopen(targetStatus) {
      try {
        const { value } = await this.$prompt('请填写重开原因，提交后需要管理员或所有者确认。', '提交重开申请', {
          confirmButtonText: '提交申请',
          cancelButtonText: '取消',
          inputType: 'textarea',
          inputPlaceholder: '例如：验收发现遗漏、联调失败、完成结论需要撤回等',
          inputValidator: (inputValue) => {
            if (!String(inputValue || '').trim()) return '请填写重开原因'
            if (String(inputValue || '').trim().length < 2) return '重开原因至少写 2 个字'
            return true
          }
        })
        await request({
          url: `/project/task/${this.taskData.id}/reopen-requests`,
          method: 'post',
          data: {
            targetStatus,
            reason: String(value || '').trim()
          }
        })
        this.$message.success('已提交重开申请，请等待管理员或所有者确认')
        await this.refreshAll()
        this.setTab('reopen')
        this.$emit('changed', { taskId: this.taskData.id, reason: 'reopen-request-created', partial: true })
      } catch (e) {
        if (e === 'cancel' || e === 'close') return
        const m = e.response?.data?.message || e.response?.data?.msg || '提交重开申请失败'
        this.$message.error(m)
      }
    },
    async handleApproveReopen(item) {
      if (!item || !item.id) return
      try {
        const { value } = await this.$prompt('可选填写审批备注。通过后任务会回退到申请里的目标状态。', '通过重开申请', {
          confirmButtonText: '通过',
          cancelButtonText: '取消',
          inputType: 'textarea',
          inputPlaceholder: '可选填写审批备注',
          inputValidator: () => true
        })
        this.reviewLoadingId = item.id
        const res = await request({
          url: `/project/task/${this.taskData.id}/reopen-requests/${item.id}/approve`,
          method: 'post',
          data: {
            reviewRemark: String(value || '').trim(),
            approvedTargetStatus: item.targetStatus
          }
        })
        const task = extractApiData(res) || {}
        this.$message.success('已通过重开申请')
        this.setTab('reopen')
        this.$emit('status-updated', { taskId: this.taskData.id, status: task.status || item.targetStatus })
        this.$emit('changed', { taskId: this.taskData.id, reason: 'reopen-approved', status: task.status || item.targetStatus })
        await this.refreshAll()
      } catch (e) {
        if (e === 'cancel' || e === 'close') return
        const m = e.response?.data?.message || e.response?.data?.msg || '通过重开申请失败'
        this.$message.error(m)
      } finally {
        this.reviewLoadingId = null
      }
    },
    async handleRejectReopen(item) {
      if (!item || !item.id) return
      try {
        const { value } = await this.$prompt('请填写驳回原因，申请人会看到这条备注。', '驳回重开申请', {
          confirmButtonText: '驳回',
          cancelButtonText: '取消',
          inputType: 'textarea',
          inputPlaceholder: '例如：当前验收结论仍成立，不同意重开',
          inputValidator: (inputValue) => {
            if (!String(inputValue || '').trim()) return '请填写驳回原因'
            return true
          }
        })
        this.reviewLoadingId = item.id
        await request({
          url: `/project/task/${this.taskData.id}/reopen-requests/${item.id}/reject`,
          method: 'post',
          data: {
            reviewRemark: String(value || '').trim()
          }
        })
        this.$message.success('已驳回重开申请')
        this.setTab('reopen')
        await this.refreshAll()
        this.$emit('changed', { taskId: this.taskData.id, reason: 'reopen-rejected', partial: true })
      } catch (e) {
        if (e === 'cancel' || e === 'close') return
        const m = e.response?.data?.message || e.response?.data?.msg || '驳回重开申请失败'
        this.$message.error(m)
      } finally {
        this.reviewLoadingId = null
      }
    },
    async handleChildChanged() {
      await this.refreshAll()
      this.$emit('changed', { taskId: this.taskData && this.taskData.id, reason: 'child-changed' })
    },
    parseDateTime(value) {
      if (!value) return 0
      const time = new Date(value).getTime()
      return Number.isNaN(time) ? 0 : time
    },
    isTaskOverdue(task) {
      if (!task || task.status === 'done' || !task.dueDate) return false
      const dueTime = this.parseDateTime(task.dueDate)
      if (!dueTime) return false
      return dueTime < Date.now()
    },
    getTaskAssigneeName(task) {
      return (task && task.assigneeName) || '未分配'
    },
    getTaskCreatorName(task) {
      return (task && task.creatorName) || '未知创建人'
    },
    getTaskDueLabel(task) {
      if (task && task.dueDate) return `截止 ${this.formatTaskShortTime(task.dueDate)}`
      return '未设置截止时间'
    },
    formatTaskShortTime(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return `${date.toLocaleDateString('zh-CN')} ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
    },
    getTaskStatusType(status) {
      return { todo: 'info', in_progress: 'warning', done: 'success' }[status] || 'info'
    },
    getTaskStatusText(status) {
      return { todo: '待处理', in_progress: '进行中', done: '已完成' }[status] || status || '未知状态'
    },
    getTaskPriorityType(priority) {
      return { low: 'info', medium: '', high: 'warning', urgent: 'danger' }[priority] || 'info'
    },
    getTaskPriorityText(priority) {
      return { low: '低', medium: '中', high: '高', urgent: '紧急' }[priority] || priority || '中'
    },
    getReopenStatusType(status) {
      return { pending: 'warning', approved: 'success', rejected: 'danger', cancelled: 'info' }[status] || 'info'
    },
    getReopenStatusText(status) {
      return { pending: '待确认', approved: '已通过', rejected: '已驳回', cancelled: '已取消' }[status] || status || '未知状态'
    },
    formatAction(action) {
      const map = {
        create: '创建任务',
        update: '更新任务',
        assign: '变更负责人',
        change_status: '修改状态',
        change_priority: '修改优先级',
        comment: '发表评论',
        attach: '上传附件',
        complete: '完成任务',
        reopen: '重新打开任务',
        reopen_request: '提交重开申请',
        reopen_approve: '通过重开申请',
        reopen_reject: '驳回重开申请',
        delete: '删除内容'
      }
      return map[action] || action || '任务动态'
    }
  }
}
</script>

<style scoped>
.task-collab-drawer-title { display: flex; flex-direction: column; gap: 4px; }
.task-collab-drawer-heading { font-size: 12px; letter-spacing: 0.08em; text-transform: uppercase; color: #8a94a6; font-weight: 700; }
.task-collab-drawer-subtitle { font-size: 18px; line-height: 1.4; color: #1f2937; font-weight: 700; max-width: 720px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
::v-deep(.task-collab-drawer .el-drawer__header) { margin-bottom: 0; padding: 18px 22px 16px; border-bottom: 1px solid #ebf1f7; background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%); }
::v-deep(.task-collab-drawer .el-drawer__body) { padding: 0; background: linear-gradient(180deg, #f6f8fc 0%, #f2f5fa 100%); overflow: auto; }
.task-collab-drawer-shell { padding: 18px; display: flex; flex-direction: column; gap: 16px; }
.task-collab-hero { display: grid; grid-template-columns: minmax(0, 1fr) 240px; gap: 16px; padding: 22px; border-radius: 20px; background: linear-gradient(135deg, #f7fbff 0%, #eef5ff 45%, #f8fcff 100%); border: 1px solid #dfeaf8; box-shadow: 0 16px 32px rgba(15, 23, 42, 0.06); }
.task-collab-eyebrow { display: inline-flex; align-items: center; height: 26px; padding: 0 10px; border-radius: 999px; background: rgba(59, 130, 246, 0.1); color: #2563eb; font-size: 12px; font-weight: 700; }
.task-collab-title-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-top: 12px; }
.task-collab-title-text { font-size: 24px; line-height: 1.4; font-weight: 700; color: #1e293b; }
.task-collab-title-tags { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }
.task-collab-desc { margin-top: 12px; font-size: 14px; line-height: 1.85; color: #607089; white-space: pre-wrap; }
.task-collab-meta-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-top: 16px; }
.task-collab-meta-card { padding: 14px 16px; border-radius: 16px; background: rgba(255, 255, 255, 0.82); border: 1px solid rgba(222, 232, 245, 0.95); }
.task-collab-meta-label { font-size: 12px; color: #7b8ba7; }
.task-collab-meta-value { margin-top: 8px; font-size: 14px; line-height: 1.6; color: #223248; font-weight: 600; word-break: break-word; }
.task-collab-side-box { display: flex; flex-direction: column; gap: 12px; padding: 18px; border-radius: 18px; background: rgba(255, 255, 255, 0.86); border: 1px solid rgba(222, 232, 245, 0.95); }
.task-collab-side-title { font-size: 14px; font-weight: 700; color: #1f2937; }
.task-collab-status-select { width: 100%; }
.task-collab-reopen-tip { display: flex; gap: 8px; align-items: flex-start; padding: 10px 12px; border-radius: 12px; background: #fff7ed; color: #9a3412; font-size: 12px; line-height: 1.7; border: 1px solid #fed7aa; }
.task-collab-reopen-banner { display: flex; flex-direction: column; gap: 8px; padding: 12px; border-radius: 14px; border: 1px solid #e8eef7; background: #f8fbff; }
.task-collab-reopen-banner.is-pending { background: #fffbeb; border-color: #fcd34d; }
.task-collab-reopen-banner.is-approved { background: #f0fdf4; border-color: #86efac; }
.task-collab-reopen-banner.is-rejected { background: #fef2f2; border-color: #fca5a5; }
.task-collab-reopen-banner-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.task-collab-reopen-banner-title { font-size: 13px; font-weight: 700; color: #1f2937; }
.task-collab-reopen-banner-desc { font-size: 12px; color: #475569; line-height: 1.7; }
.task-collab-reopen-banner-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.task-collab-shortcuts { display: flex; flex-direction: column; gap: 8px; }
.task-collab-tabs-card { border-radius: 20px; background: #fff; border: 1px solid #e8eef7; box-shadow: 0 16px 32px rgba(15, 23, 42, 0.05); overflow: hidden; }
::v-deep(.task-collab-tabs .el-tabs__header) { margin: 0; padding: 0 18px; border-bottom: 1px solid #edf2f8; background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%); }
::v-deep(.task-collab-tabs .el-tabs__nav-wrap::after) { display: none; }
::v-deep(.task-collab-tabs .el-tabs__item) { height: 54px; line-height: 54px; font-weight: 600; }
::v-deep(.task-collab-tabs .el-tabs__content) { padding: 18px; background: linear-gradient(180deg, #fbfcff 0%, #ffffff 100%); }
.task-tab-badge { display: inline-flex; align-items: center; justify-content: center; min-width: 18px; height: 18px; margin-left: 6px; padding: 0 5px; border-radius: 999px; background: #ef4444; color: #fff; font-size: 11px; line-height: 1; }
.task-overview-panel { display: flex; flex-direction: column; gap: 16px; }
.task-overview-stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.task-overview-stat { padding: 14px 16px; border-radius: 14px; background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%); border: 1px solid #e8eef7; }
.task-overview-stat-value { font-size: 22px; font-weight: 700; color: #1f2937; }
.task-overview-stat-label { margin-top: 6px; font-size: 12px; color: #94a3b8; }
.task-overview-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.task-overview-card { padding: 16px; border-radius: 16px; border: 1px solid #e8eef7; background: #fff; }
.task-overview-card-full { grid-column: 1 / -1; }
.task-overview-card-title { font-size: 14px; font-weight: 700; color: #1f2937; margin-bottom: 12px; }
.task-overview-progress-row { margin-bottom: 10px; }
.task-overview-card-desc { font-size: 13px; color: #64748b; }
.task-overview-info-list { display: flex; flex-direction: column; gap: 10px; }
.task-overview-info-item { display: flex; justify-content: space-between; gap: 10px; font-size: 13px; color: #475569; }
.task-overview-log-list { display: flex; flex-direction: column; gap: 10px; }
.task-overview-log-item { padding: 10px 12px; border-radius: 12px; background: #f8fafc; border: 1px solid #e8eef7; }
.task-overview-log-top { display: flex; justify-content: space-between; gap: 8px; font-size: 13px; }
.task-overview-log-action { color: #1f2937; font-weight: 600; }
.task-overview-log-time { color: #94a3b8; }
.task-overview-log-desc { margin-top: 6px; font-size: 12px; color: #64748b; }
.task-reopen-panel { display: flex; flex-direction: column; gap: 16px; }
.task-reopen-panel-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.task-reopen-panel-title { font-size: 16px; font-weight: 700; color: #1f2937; }
.task-reopen-panel-subtitle { margin-top: 4px; font-size: 12px; color: #64748b; }
.task-reopen-panel-summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.task-reopen-summary-card { padding: 14px 16px; border-radius: 14px; border: 1px solid #e8eef7; background: #fff; }
.task-reopen-summary-card.warning { background: #fffbeb; border-color: #fcd34d; }
.task-reopen-summary-card.success { background: #f0fdf4; border-color: #86efac; }
.task-reopen-summary-card.danger { background: #fef2f2; border-color: #fca5a5; }
.task-reopen-summary-value { font-size: 22px; font-weight: 700; color: #1f2937; }
.task-reopen-summary-label { margin-top: 6px; font-size: 12px; color: #64748b; }
.task-reopen-list { display: flex; flex-direction: column; gap: 12px; }
.task-reopen-list-detailed .task-reopen-item { background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%); }
.task-reopen-item { padding: 12px 14px; border-radius: 12px; border: 1px solid #e8eef7; background: #f8fbff; }
.task-reopen-top { display: flex; justify-content: space-between; gap: 12px; align-items: center; }
.task-reopen-title { display: flex; gap: 8px; align-items: center; font-size: 13px; font-weight: 600; color: #1f2937; }
.task-reopen-time { font-size: 12px; color: #94a3b8; }
.task-reopen-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-top: 10px; }
.task-reopen-field { padding: 10px 12px; border-radius: 12px; background: #f8fafc; border: 1px solid #e8eef7; display: flex; flex-direction: column; gap: 6px; }
.task-reopen-field span { font-size: 12px; color: #94a3b8; }
.task-reopen-field strong { font-size: 13px; color: #1f2937; }
.task-reopen-desc { margin-top: 8px; font-size: 13px; color: #475569; line-height: 1.7; }
.task-reopen-actions { margin-top: 10px; display: flex; gap: 8px; flex-wrap: wrap; }
.task-collab-drawer-empty { display: flex; align-items: center; justify-content: center; min-height: 320px; }
@media (max-width: 1100px) {
  .task-collab-hero { grid-template-columns: 1fr; }
  .task-collab-meta-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .task-overview-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .task-overview-grid { grid-template-columns: 1fr; }
  .task-reopen-panel-summary { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .task-reopen-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 768px) {
  .task-collab-drawer-shell { padding: 12px; }
  .task-collab-title-row { flex-direction: column; }
  .task-collab-title-text { font-size: 20px; }
  .task-collab-meta-grid { grid-template-columns: 1fr; }
  .task-overview-stats { grid-template-columns: 1fr 1fr; }
  .task-reopen-panel-summary { grid-template-columns: 1fr; }
  .task-reopen-grid { grid-template-columns: 1fr; }
  .task-reopen-top { flex-direction: column; align-items: flex-start; }
}
</style>
