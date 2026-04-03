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

          <div class="task-collab-shortcuts">
            <el-button size="mini" plain @click="setTab('overview')">看概览</el-button>
            <el-button size="mini" plain @click="setTab('comment')">去评论</el-button>
            <el-button size="mini" plain @click="setTab('attachment')">看附件</el-button>
            <el-button size="mini" plain @click="setTab('checklist')">看清单</el-button>
            <el-button size="mini" plain @click="setTab('dependency')">看依赖</el-button>
            <el-button size="mini" plain @click="setTab('log')">看时间线</el-button>
          </div>

          <el-button size="mini" type="text" :loading="overviewLoading" @click="loadOverview">
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
              </div>
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
    visible: {
      type: Boolean,
      default: false
    },
    task: {
      type: Object,
      default: null
    },
    projectId: {
      type: [Number, String],
      default: null
    },
    activeTab: {
      type: String,
      default: 'comment'
    },
    refreshSeed: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      overviewLoading: false,
      overview: createEmptyOverview(),
      statusUpdating: false,
      quickStatus: 'todo'
    }
  },
  computed: {
    innerVisible: {
      get() {
        return this.visible
      },
      set(v) {
        this.$emit('update:visible', v)
      }
    },
    innerActiveTab: {
      get() {
        return this.activeTab || 'overview'
      },
      set(v) {
        this.$emit('update:activeTab', v)
      }
    },
    taskData() {
      return this.task || null
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(v) {
        if (v && this.taskData && this.taskData.id) {
          this.syncQuickStatus()
          this.loadOverview()
        }
      }
    },
    task: {
      deep: true,
      immediate: true,
      handler() {
        this.syncQuickStatus()
        if (this.visible && this.taskData && this.taskData.id) {
          this.loadOverview()
        } else if (!this.taskData) {
          this.overview = createEmptyOverview()
        }
      }
    },
    refreshSeed() {
      if (this.visible && this.taskData && this.taskData.id) {
        this.loadOverview()
      }
    }
  },
  methods: {
    syncQuickStatus() {
      this.quickStatus = (this.taskData && this.taskData.status) || 'todo'
    },
    setTab(tab) {
      this.innerActiveTab = tab
    },
    handleClosed() {
      this.$emit('close')
    },
    panelKey(name) {
      const taskId = this.taskData && this.taskData.id ? this.taskData.id : 'empty'
      return `${name}-${taskId}-${this.refreshSeed}`
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
        const comments = Array.isArray(commentsRes && commentsRes.data) ? commentsRes.data : []
        const checklist = Array.isArray(checklistRes && checklistRes.data) ? checklistRes.data : []
        const attachments = Array.isArray(attachmentsRes && attachmentsRes.data) ? attachmentsRes.data : []
        const dependencies = Array.isArray(dependenciesRes && dependenciesRes.data) ? dependenciesRes.data : []
        const logs = Array.isArray(logsRes && logsRes.data) ? logsRes.data : []

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
    async handleQuickStatusChange(value) {
      if (!this.taskData || !this.taskData.id || !value || value === this.taskData.status) {
        return
      }
      try {
        this.statusUpdating = true
        await updateTaskStatus(this.taskData.id, { status: value })
        this.$message.success('任务状态已更新')
        this.$emit('changed')
      } catch (e) {
        this.quickStatus = (this.taskData && this.taskData.status) || 'todo'
        const m = e.response?.data?.message || e.response?.data?.msg || '更新任务状态失败'
        this.$message.error(m)
      } finally {
        this.statusUpdating = false
      }
    },
    async handleChildChanged() {
      await this.loadOverview()
      this.$emit('changed')
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
        delete: '删除内容'
      }
      return map[action] || action || '任务动态'
    }
  }
}
</script>

<style scoped>
.task-collab-drawer-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.task-collab-drawer-heading {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #8a94a6;
  font-weight: 700;
}
.task-collab-drawer-subtitle {
  font-size: 18px;
  line-height: 1.4;
  color: #1f2937;
  font-weight: 700;
  max-width: 720px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
::v-deep(.task-collab-drawer .el-drawer__header) {
  margin-bottom: 0;
  padding: 18px 22px 16px;
  border-bottom: 1px solid #ebf1f7;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}
::v-deep(.task-collab-drawer .el-drawer__body) {
  padding: 0;
  background: linear-gradient(180deg, #f6f8fc 0%, #f2f5fa 100%);
  overflow: auto;
}
.task-collab-drawer-shell {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.task-collab-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 16px;
  padding: 22px;
  border-radius: 20px;
  background: linear-gradient(135deg, #f7fbff 0%, #eef5ff 45%, #f8fcff 100%);
  border: 1px solid #dfeaf8;
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.06);
}
.task-collab-eyebrow {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}
.task-collab-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}
.task-collab-title-text {
  font-size: 24px;
  line-height: 1.4;
  font-weight: 700;
  color: #1e293b;
}
.task-collab-title-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.task-collab-desc {
  margin-top: 12px;
  font-size: 14px;
  line-height: 1.85;
  color: #607089;
  white-space: pre-wrap;
}
.task-collab-meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}
.task-collab-meta-card {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(222, 232, 245, 0.95);
}
.task-collab-meta-label {
  font-size: 12px;
  color: #7b8ba7;
}
.task-collab-meta-value {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.6;
  color: #223248;
  font-weight: 600;
  word-break: break-word;
}
.task-collab-side-box {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(222, 232, 245, 0.95);
}
.task-collab-side-title {
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
}
.task-collab-status-select {
  width: 100%;
}
.task-collab-shortcuts {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.task-collab-tabs-card {
  border-radius: 20px;
  background: #fff;
  border: 1px solid #e8eef7;
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.05);
  overflow: hidden;
}
::v-deep(.task-collab-tabs .el-tabs__header) {
  margin: 0;
  padding: 0 18px;
  border-bottom: 1px solid #edf2f8;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
}
::v-deep(.task-collab-tabs .el-tabs__nav-wrap::after) {
  display: none;
}
::v-deep(.task-collab-tabs .el-tabs__item) {
  height: 54px;
  line-height: 54px;
  font-weight: 600;
}
::v-deep(.task-collab-tabs .el-tabs__content) {
  padding: 18px;
  background: linear-gradient(180deg, #fbfcff 0%, #ffffff 100%);
}
.task-overview-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.task-overview-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.task-overview-stat {
  padding: 14px 16px;
  border-radius: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid #e8eef7;
}
.task-overview-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
}
.task-overview-stat-label {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}
.task-overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}
.task-overview-card {
  padding: 16px;
  border-radius: 16px;
  border: 1px solid #e8eef7;
  background: #fff;
}
.task-overview-card-title {
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 12px;
}
.task-overview-progress-row {
  margin-bottom: 10px;
}
.task-overview-card-desc {
  font-size: 13px;
  color: #64748b;
}
.task-overview-info-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.task-overview-info-item {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
  color: #475569;
}
.task-overview-log-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.task-overview-log-item {
  padding: 10px 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e8eef7;
}
.task-overview-log-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
}
.task-overview-log-action {
  color: #1f2937;
  font-weight: 600;
}
.task-overview-log-time {
  color: #94a3b8;
}
.task-overview-log-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}
.task-collab-drawer-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 320px;
}
@media (max-width: 1100px) {
  .task-collab-hero {
    grid-template-columns: 1fr;
  }
  .task-collab-meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .task-overview-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .task-overview-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 768px) {
  .task-collab-drawer-shell {
    padding: 12px;
  }
  .task-collab-title-row {
    flex-direction: column;
  }
  .task-collab-title-text {
    font-size: 20px;
  }
  .task-collab-meta-grid {
    grid-template-columns: 1fr;
  }
  .task-overview-stats {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
