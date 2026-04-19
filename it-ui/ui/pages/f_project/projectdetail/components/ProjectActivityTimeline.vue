<template>
  <div class="project-activity-timeline">
    <div class="timeline-header-card">
      <div>
        <div class="timeline-eyebrow">项目活动</div>
        <div class="timeline-title">项目级时间线</div>
      </div>
      <div class="timeline-header-actions">
        <span class="timeline-auto-tip">{{ autoRefreshLabel }}</span>
        <el-button size="mini" type="text" @click="resetAndLoad">刷新</el-button>
        <el-button v-if="showViewAll" size="mini" type="text" @click="goToAllActivities">查看全部活动</el-button>
      </div>
    </div>

    <div v-if="showFilters" class="timeline-filter-card">
      <el-select v-model="filters.action" size="small" clearable placeholder="动作类型" @change="loadActivities">
        <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>

      <el-select v-model="filters.targetType" size="small" clearable placeholder="对象类型" @change="loadActivities">
        <el-option label="任务" value="task" />
        <el-option label="文档" value="doc" />
        <el-option label="文件" value="file" />
        <el-option label="成员" value="member" />
        <el-option label="邀请" value="invitation" />
        <el-option label="加入申请" value="join_request" />
        <el-option label="合并请求" value="merge_request" />
        <el-option label="项目" value="project" />
      </el-select>

      <el-select v-model="filters.operatorId" size="small" clearable filterable placeholder="操作人" @change="loadActivities">
        <el-option v-for="item in operatorOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>

      <el-date-picker
        v-model="filters.dateRange"
        size="small"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        value-format="yyyy-MM-dd HH:mm:ss"
        @change="loadActivities"
      />

      <el-button size="small" @click="clearFilters">清空筛选</el-button>
    </div>

    <div class="timeline-body-card">
      <div v-if="loading" class="loading-wrap">
        <i class="el-icon-loading" />
        <span>活动流加载中...</span>
      </div>

      <div v-else-if="!displayGroupedActivities.length" class="empty-wrap">
        <el-empty description="暂无项目活动" />
      </div>

      <div v-else class="timeline-shell">
        <div v-for="group in displayGroupedActivities" :key="group.day" class="timeline-day-group">
          <div class="timeline-day-header">
            <span class="timeline-day-title">{{ group.label }}</span>
            <span class="timeline-day-count">{{ group.items.length }} 条</span>
          </div>

          <div v-for="(item, index) in group.items" :key="item.id || index" class="timeline-item" @click="handleActivityClick(item)">
            <div class="timeline-axis">
              <div class="timeline-dot" :class="resolveActionClass(item)">
                <i :class="resolveActionIcon(item)" />
              </div>
              <div v-if="index !== group.items.length - 1" class="timeline-line" />
            </div>

            <div class="timeline-card">
              <div class="timeline-card-top">
                <div class="timeline-title-group">
                  <div class="timeline-action">{{ item.actionLabel || item.action || '项目动态' }}</div>
                  <span class="timeline-badge" :class="resolveActionClass(item)">{{ formatActionTag(item.action) }}</span>
                </div>
                <div class="timeline-time">{{ formatDate(item.createdAt) }}</div>
              </div>

              <div class="timeline-operator-row">
                <img v-if="item.operatorAvatar" :src="item.operatorAvatar" alt="avatar" class="timeline-operator-avatar is-image" />
                <div v-else class="timeline-operator-avatar">{{ firstChar(formatOperator(item)) }}</div>
                <div class="timeline-operator-text">
                  <div class="timeline-operator-name">{{ formatOperator(item) }}</div>
                  <div class="timeline-operator-desc">{{ formatTargetType(item.targetType) }}</div>
                </div>
              </div>

              <div class="timeline-change-box">
                <div class="timeline-change-title">动态内容</div>
                <div class="timeline-content">{{ item.content || '暂无描述' }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
   </div>
  </div>
</template>

<script>
import { getProjectActivities } from '@/api/projectActivity'

export default {
  name: 'ProjectActivityTimeline',
  props: {
    projectId: {
      type: [Number, String],
      default: null
    },
    compact: {
      type: Boolean,
      default: false
    },
    showFilters: {
      type: Boolean,
      default: true
    },
    showViewAll: {
      type: Boolean,
      default: false
    },
    maxCount: {
      type: Number,
      default: 5
    },
    disableAutoRefresh: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      loading: false,
      activities: [],
      autoRefreshTimer: null,
      lastLoadedAt: 0,
      filters: {
        action: '',
        targetType: '',
        operatorId: null,
        dateRange: []
      },
      actionOptions: [
        { label: '创建项目', value: 'create_project' },
        { label: '编辑项目', value: 'update_project' },
        { label: '新增成员', value: 'add_member' },
        { label: '移除成员', value: 'remove_member' },
        { label: '退出项目', value: 'quit_project' },
        { label: '保存模板', value: 'save_as_template' },
        { label: '套用模板', value: 'apply_template' },
        { label: '发起邀请', value: 'create_invite' },
        { label: '接受邀请', value: 'accept_invite' },
        { label: '取消邀请', value: 'cancel_invite' },
        { label: '提交加入申请', value: 'submit_join_request' },
        { label: '通过加入申请', value: 'approve_join_request' },
        { label: '拒绝加入申请', value: 'reject_join_request' },
        { label: '上传文件', value: 'upload_file' },
        { label: '删除文件', value: 'delete_file' },
        { label: '设主文件', value: 'set_main_file' },
        { label: '新建文档', value: 'create_doc' },
        { label: '更新文档', value: 'update_doc' },
        { label: '回滚文档', value: 'rollback_doc' },
        { label: '设为主文档', value: 'set_primary_doc' },
        { label: '删除文档', value: 'delete_doc' },
        { label: '创建任务', value: 'create_task' },
        { label: '更新任务', value: 'update_task' },
        { label: '修改任务状态', value: 'change_task_status' },
        { label: '删除任务', value: 'delete_task' },
        { label: '冲突处理记录', value: 'mr_conflict_resolve' },
        { label: '冲突解决开始', value: 'mr_conflict_resolve_start' },
        { label: '冲突策略已应用', value: 'mr_conflict_resolve_apply' },
        { label: '冲突自动重检', value: 'mr_conflict_resolve_recheck' },
        { label: '冲突解决失败', value: 'mr_conflict_resolve_fail' }
      ]
    }
  },
  computed: {
    operatorOptions() {
      const map = new Map()
      this.activities.forEach(item => {
        if (!item || item.operatorId == null) return
        if (!map.has(item.operatorId)) {
          map.set(item.operatorId, {
            value: item.operatorId,
            label: this.formatOperator(item)
          })
        }
      })
      return Array.from(map.values())
    },
    displayActivities() {
      if (!this.compact) return this.activities
      return this.activities.slice(0, this.maxCount)
    },
    displayGroupedActivities() {
      const groups = []
      const bucket = {}
      this.displayActivities.forEach(item => {
        const day = item.groupDay || this.formatDay(item.createdAt)
        if (!bucket[day]) {
          bucket[day] = []
          groups.push({ day, label: this.formatDayLabel(day), items: bucket[day] })
        }
        bucket[day].push(item)
      })
      return groups
    },
    autoRefreshLabel() {
      if (this.disableAutoRefresh) return '自动更新已关闭'
      if (!this.lastLoadedAt) return '自动更新已开启'
      return '自动更新中'
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler(val) {
        if (val) {
          this.resetAndLoad()
          if (!this.disableAutoRefresh) this.startAutoRefresh()
        } else {
          this.activities = []
          this.stopAutoRefresh()
        }
      }
    }
  },
  mounted() {
    document.addEventListener('visibilitychange', this.handleVisibilityChange)
    window.addEventListener('focus', this.handleWindowFocus)
    if (!this.disableAutoRefresh) this.startAutoRefresh()
  },
  beforeDestroy() {
    document.removeEventListener('visibilitychange', this.handleVisibilityChange)
    window.removeEventListener('focus', this.handleWindowFocus)
    this.stopAutoRefresh()
  },
  methods: {
    buildParams() {
      const params = { page: 1, size: this.compact ? this.maxCount : 50 }
      if (this.filters.action) params.action = this.filters.action
      if (this.filters.targetType) params.targetType = this.filters.targetType
      if (this.filters.operatorId !== null && this.filters.operatorId !== undefined && this.filters.operatorId !== '') params.operatorId = this.filters.operatorId
      if (Array.isArray(this.filters.dateRange) && this.filters.dateRange.length === 2) {
        params.startTime = this.filters.dateRange[0]
        params.endTime = this.filters.dateRange[1]
      }
      return params
    },
    extractList(res) {
      const payload = res && res.data ? res.data : res
      if (payload && Array.isArray(payload.list)) return payload.list
      if (Array.isArray(payload)) return payload
      return []
    },
    async loadActivities(silent = false) {
      if (!this.projectId) return
      try {
        if (!silent) this.loading = true
        const res = await getProjectActivities(this.projectId, this.buildParams())
        this.activities = this.extractList(res)
        this.lastLoadedAt = Date.now()
      } catch (e) {
        if (!silent) {
          const m = e.response?.data?.message || e.response?.data?.msg || '加载项目活动失败'
          this.$message.error(m)
        }
        this.activities = []
      } finally {
        if (!silent) this.loading = false
      }
    },
    resetAndLoad() {
      this.loadActivities(false)
    },
    clearFilters() {
      this.filters = { action: '', targetType: '', operatorId: null, dateRange: [] }
      this.loadActivities(false)
    },
    startAutoRefresh() {
      this.stopAutoRefresh()
      if (!this.projectId || this.disableAutoRefresh) return
      this.autoRefreshTimer = setInterval(() => {
        if (document.hidden) return
        this.loadActivities(true)
      }, 8000)
    },
    stopAutoRefresh() {
      if (this.autoRefreshTimer) {
        clearInterval(this.autoRefreshTimer)
        this.autoRefreshTimer = null
      }
    },
    handleVisibilityChange() {
      if (!document.hidden && this.projectId && !this.disableAutoRefresh) this.loadActivities(true)
    },
    handleWindowFocus() {
      if (this.projectId && !this.disableAutoRefresh) this.loadActivities(true)
    },
    buildBaseManageQuery() {
      const query = {
        projectId: String(this.projectId),
        tab: 'activity-manage'
      }
      if (this.filters.action) query.action = this.filters.action
      if (this.filters.targetType) query.targetType = this.filters.targetType
      if (this.filters.operatorId !== null && this.filters.operatorId !== undefined && this.filters.operatorId !== '') query.operatorId = String(this.filters.operatorId)
      if (Array.isArray(this.filters.dateRange) && this.filters.dateRange.length === 2) {
        query.startTime = this.filters.dateRange[0]
        query.endTime = this.filters.dateRange[1]
      }
      return query
    },
    buildManageQuery(item) {
      return {
        ...this.buildBaseManageQuery(),
        activityId: String(item.id)
      }
    },
    goToAllActivities() {
      this.$router.push({
        path: '/projectmanage',
        query: this.buildBaseManageQuery()
      })
    },
    goToManageActivity(item) {
      if (!item || !item.id) return
      this.$router.push({ path: '/projectmanage', query: this.buildManageQuery(item) })
    },
    handleActivityClick(item) {
      this.goToManageActivity(item)
    },
    formatOperator(item) {
      return item.operatorName || ('用户#' + (item.operatorId || '-'))
    },
    formatTargetType(value) {
      const map = { task: '任务动态', doc: '文档动态', file: '文件动态', member: '成员动态', project: '项目动态', invitation: '邀请动态', join_request: '加入申请动态', merge_request: '合并请求动态' }
      return map[value] || '项目动态'
    },
    formatActionTag(action) {
      const map = {
        create_project: '创建', update_project: '更新', add_member: '成员', remove_member: '成员', quit_project: '成员',
        upload_file: '文件', delete_file: '文件', set_main_file: '主文件', create_doc: '文档', update_doc: '文档',
        rollback_doc: '回滚', set_primary_doc: '主文档', delete_doc: '文档', create_task: '任务', update_task: '任务',
        change_task_status: '状态', delete_task: '任务', save_as_template: '模板', apply_template: '模板',
        mr_conflict_resolve: '冲突', mr_conflict_resolve_start: '冲突', mr_conflict_resolve_apply: '冲突', mr_conflict_resolve_recheck: '重检',
        mr_conflict_resolve_fail: '失败'
      }
      return map[action] || '动态'
    },
    resolveActionClass(item) {
      const action = item && item.action
      const map = {
        create_project: 'is-create', create_doc: 'is-create', create_task: 'is-create', add_member: 'is-create', upload_file: 'is-create',
        update_project: 'is-update', update_doc: 'is-update', update_task: 'is-update', set_main_file: 'is-priority',
        change_task_status: 'is-status', rollback_doc: 'is-status', set_primary_doc: 'is-priority',
        remove_member: 'is-delete', quit_project: 'is-delete', delete_file: 'is-delete', delete_doc: 'is-delete', delete_task: 'is-delete',
        mr_conflict_resolve: 'is-create', mr_conflict_resolve_start: 'is-status', mr_conflict_resolve_apply: 'is-create', mr_conflict_resolve_recheck: 'is-status',
        mr_conflict_resolve_fail: 'is-delete'
      }
      return map[action] || 'is-default'
    },
    resolveActionIcon(item) {
      const action = item && item.action
      const map = {
        create_project: 'el-icon-circle-plus-outline', create_doc: 'el-icon-document-add', create_task: 'el-icon-s-claim', add_member: 'el-icon-user-solid',
        upload_file: 'el-icon-upload', update_project: 'el-icon-edit-outline', update_doc: 'el-icon-edit', update_task: 'el-icon-edit',
        set_main_file: 'el-icon-star-on', change_task_status: 'el-icon-refresh', rollback_doc: 'el-icon-refresh-left', set_primary_doc: 'el-icon-star-on',
        remove_member: 'el-icon-remove', quit_project: 'el-icon-close', delete_file: 'el-icon-delete', delete_doc: 'el-icon-delete', delete_task: 'el-icon-delete',
        mr_conflict_resolve: 'el-icon-document-checked', mr_conflict_resolve_start: 'el-icon-warning-outline', mr_conflict_resolve_apply: 'el-icon-check',
        mr_conflict_resolve_recheck: 'el-icon-refresh', mr_conflict_resolve_fail: 'el-icon-close'
      }
      return map[action] || 'el-icon-time'
    },
    formatDate(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return date.toLocaleString()
    },
    formatDay(value) {
      if (!value) return 'unknown'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return String(value).slice(0, 10)
      const p = n => String(n).padStart(2, '0')
      return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}`
    },
    formatDayLabel(day) {
      if (!day || day === 'unknown') return '未知日期'
      const today = this.formatDay(new Date())
      const yesterdayDate = new Date()
      yesterdayDate.setDate(yesterdayDate.getDate() - 1)
      const yesterday = this.formatDay(yesterdayDate)
      if (day === today) return '今天'
      if (day === yesterday) return '昨天'
      return day
    },
    firstChar(value) {
      return value ? String(value).slice(0, 1) : 'U'
    }
  }
}
</script>

<style scoped>
.project-activity-timeline {
  display: flex;
  flex-direction: column;
  gap: 14px;
  height: 900px;
  max-height: 900px;
}

.timeline-header-card,
.timeline-filter-card,
.timeline-body-card,
.timeline-card {
  border: 1px solid var(--it-border);
  border-radius: 18px;
  background: var(--it-panel-bg-strong);
  box-shadow: var(--it-shadow-soft);
}

.timeline-header-card {
  padding: 16px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-accent) 7%, var(--it-surface-elevated)), var(--it-surface-solid));
}

.timeline-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.timeline-auto-tip,
.timeline-day-count,
.timeline-time,
.timeline-operator-desc {
  font-size: 12px;
  color: var(--it-text-subtle);
}

.timeline-filter-card {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.timeline-body-card {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: 16px 14px 16px 16px;
}

.timeline-eyebrow {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--it-accent);
  font-weight: 700;
}

.timeline-title,
.timeline-action,
.timeline-day-title,
.timeline-operator-name {
  color: var(--it-text);
}

.timeline-title {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 700;
}

.timeline-shell {
  height: 100%;
  overflow-y: auto;
  padding-right: 4px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.timeline-shell::-webkit-scrollbar {
  width: 8px;
}

.timeline-shell::-webkit-scrollbar-thumb {
  background: var(--it-scrollbar);
  border-radius: 999px;
}

.timeline-day-group {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.timeline-day-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 2px;
}

.timeline-day-title {
  font-size: 14px;
  font-weight: 700;
}

.timeline-item {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  gap: 14px;
  align-items: stretch;
  cursor: pointer;
}

.timeline-axis {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.timeline-dot {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  box-shadow: var(--it-button-shadow);
}

.timeline-line {
  flex: 1;
  width: 2px;
  margin-top: 8px;
  background: linear-gradient(180deg, color-mix(in srgb, var(--it-accent) 24%, transparent) 0%, transparent 100%);
}

.timeline-card {
  padding: 16px 18px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-surface-elevated) 94%, transparent), var(--it-surface-solid));
  transition: all .25s ease;
}

.timeline-item:hover .timeline-card {
  border-color: var(--it-border-strong);
  box-shadow: var(--it-shadow-hover);
  transform: translateY(-1px);
}

.timeline-card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.timeline-title-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.timeline-action {
  font-size: 16px;
  font-weight: 700;
}

.timeline-badge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  border: 1px solid transparent;
}

.timeline-operator-row {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.timeline-operator-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-accent) 14%, var(--it-surface-elevated)) 0%, var(--it-surface-solid) 100%);
  color: var(--it-accent);
  font-weight: 700;
  flex-shrink: 0;
  object-fit: cover;
}

.timeline-operator-avatar.is-image {
  padding: 0;
  background: var(--it-surface-muted);
  border: 1px solid var(--it-border);
}

.timeline-change-box {
  margin-top: 14px;
  padding: 14px;
  border-radius: 14px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-accent) 5%, var(--it-surface-elevated)), var(--it-surface-solid));
  border: 1px solid var(--it-border);
}

.timeline-change-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--it-text-muted);
}

.timeline-content,
.loading-wrap {
  color: var(--it-text-muted);
}

.timeline-content {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.8;
  word-break: break-word;
}

.empty-wrap,
.loading-wrap {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-wrap {
  gap: 8px;
}

.is-create { background: linear-gradient(135deg, color-mix(in srgb, var(--it-success) 74%, white) 0%, var(--it-success) 100%); }
.is-update,
.is-default { background: var(--it-primary-gradient); }
.is-status { background: linear-gradient(135deg, color-mix(in srgb, var(--it-success) 62%, var(--it-accent)) 0%, var(--it-success) 100%); }
.is-priority { background: linear-gradient(135deg, color-mix(in srgb, var(--it-warning) 74%, white) 0%, var(--it-warning) 100%); }
.is-delete { background: linear-gradient(135deg, color-mix(in srgb, var(--it-danger) 76%, white) 0%, var(--it-danger) 100%); }

.timeline-badge.is-create {
  color: var(--it-tone-success-text);
  background: var(--it-success-soft);
  border-color: color-mix(in srgb, var(--it-success) 24%, var(--it-border));
}

.timeline-badge.is-update,
.timeline-badge.is-default {
  color: var(--it-accent);
  background: var(--it-accent-soft);
  border-color: color-mix(in srgb, var(--it-accent) 24%, var(--it-border));
}

.timeline-badge.is-status {
  color: var(--it-tone-success-text);
  background: color-mix(in srgb, var(--it-success) 16%, transparent);
  border-color: color-mix(in srgb, var(--it-success) 26%, var(--it-border));
}

.timeline-badge.is-priority {
  color: var(--it-tone-warning-text);
  background: var(--it-warning-soft);
  border-color: color-mix(in srgb, var(--it-warning) 26%, var(--it-border));
}

.timeline-badge.is-delete {
  color: var(--it-tone-danger-text);
  background: var(--it-danger-soft);
  border-color: color-mix(in srgb, var(--it-danger) 26%, var(--it-border));
}

@media (max-width: 768px) {
  .project-activity-timeline {
    height: 760px;
    max-height: 760px;
  }

  .timeline-filter-card {
    align-items: stretch;
  }

  .timeline-filter-card > * {
    width: 100%;
  }

  .timeline-item {
    grid-template-columns: 32px minmax(0, 1fr);
    gap: 12px;
  }

  .timeline-dot {
    width: 30px;
    height: 30px;
    font-size: 14px;
  }

  .timeline-card-top {
    flex-direction: column;
  }

  .timeline-time {
    white-space: normal;
  }
}
</style>

