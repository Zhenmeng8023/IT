<template>
  <div class="task-log-timeline">
    <div class="timeline-header-card">
      <div>
        <div class="timeline-eyebrow">任务动态</div>
        <div class="timeline-title">操作时间线</div>
      </div>
      <el-button size="mini" type="text" @click="resetAndLoad">刷新</el-button>
    </div>

    <div class="timeline-filter-card">
      <el-select v-model="filters.action" size="small" clearable placeholder="动作类型" @change="loadLogs">
        <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
      </el-select>

      <el-select v-model="filters.operatorId" size="small" clearable filterable placeholder="操作人" @change="loadLogs">
        <el-option
          v-for="item in operatorOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        ></el-option>
      </el-select>

      <el-date-picker
        v-model="filters.dateRange"
        size="small"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        value-format="yyyy-MM-dd HH:mm:ss"
        @change="loadLogs"
      ></el-date-picker>

      <el-button size="small" @click="clearFilters">清空筛选</el-button>
    </div>

    <div v-if="loading" class="panel-card loading-wrap">
      <i class="el-icon-loading"></i>
      <span>日志加载中...</span>
    </div>

    <div v-else-if="!groupedLogs.length" class="panel-card empty-wrap">
      <el-empty description="暂无动态" />
    </div>

    <div v-else class="timeline-shell">
      <div v-for="group in groupedLogs" :key="group.day" class="timeline-day-group">
        <div class="timeline-day-header">
          <span class="timeline-day-title">{{ group.label }}</span>
          <span class="timeline-day-count">{{ group.items.length }} 条</span>
        </div>

        <div v-for="(item, index) in group.items" :key="item.id || index" class="timeline-item">
          <div class="timeline-axis">
            <div class="timeline-dot" :class="resolveActionClass(item)">
              <i :class="resolveActionIcon(item)"></i>
            </div>
            <div v-if="index !== group.items.length - 1" class="timeline-line"></div>
          </div>

          <div class="timeline-card">
            <div class="timeline-card-top">
              <div class="timeline-title-group">
                <div class="timeline-action">{{ item.actionLabel || formatAction(item.action) }}</div>
                <span class="timeline-badge" :class="resolveActionClass(item)">
                  {{ formatActionTag(item.action) }}
                </span>
              </div>
              <div class="timeline-time">{{ formatDate(item.createdAt) }}</div>
            </div>

            <div class="timeline-operator-row">
              <img
                v-if="item.operatorAvatar"
                :src="item.operatorAvatar"
                alt="avatar"
                class="timeline-operator-avatar is-image"
              />
              <div v-else class="timeline-operator-avatar">
                {{ firstChar(formatOperator(item)) }}
              </div>
              <div class="timeline-operator-text">
                <div class="timeline-operator-name">{{ formatOperator(item) }}</div>
                <div class="timeline-operator-desc">对当前任务产生了一次协作变更</div>
              </div>
            </div>

            <div v-if="hasChange(item)" class="timeline-change-box">
              <div class="timeline-change-title">变更详情</div>
              <div class="timeline-change-grid">
                <div v-if="item.fieldLabel || item.fieldName" class="timeline-change-chip">
                  <span class="chip-label">字段</span>
                  <span class="chip-value">{{ item.fieldLabel || item.fieldName }}</span>
                </div>
                <div
                  v-if="item.oldValueDisplay !== undefined && item.oldValueDisplay !== null && item.oldValueDisplay !== ''"
                  class="timeline-change-chip"
                >
                  <span class="chip-label">旧值</span>
                  <span class="chip-value">{{ item.oldValueDisplay }}</span>
                </div>
                <div
                  v-if="item.newValueDisplay !== undefined && item.newValueDisplay !== null && item.newValueDisplay !== ''"
                  class="timeline-change-chip is-strong"
                >
                  <span class="chip-label">新值</span>
                  <span class="chip-value">{{ item.newValueDisplay }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script>
import { getTaskLogs } from '@/api/project'

export default {
  name: 'TaskLogTimeline',
  props: {
    taskId: {
      type: Number,
      default: null
    }
  },
  data() {
    return {
      loading: false,
      logs: [],
      filters: {
        action: '',
        operatorId: null,
        dateRange: []
      },
      actionOptions: [
        { label: '创建', value: 'create' },
        { label: '更新', value: 'update' },
        { label: '负责人', value: 'assign' },
        { label: '状态', value: 'change_status' },
        { label: '优先级', value: 'change_priority' },
        { label: '评论', value: 'comment' },
        { label: '附件', value: 'attach' },
        { label: '完成', value: 'complete' },
        { label: '重开', value: 'reopen' },
        { label: '删除', value: 'delete' }
      ]
    }
  },
  computed: {
    operatorOptions() {
      const map = new Map()
      this.logs.forEach(item => {
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
    groupedLogs() {
      const groups = []
      const bucket = {}
      this.logs.forEach(item => {
        const day = item.groupDay || this.formatDay(item.createdAt)
        if (!bucket[day]) {
          bucket[day] = []
          groups.push({ day, label: this.formatDayLabel(day), items: bucket[day] })
        }
        bucket[day].push(item)
      })
      return groups
    }
  },
  watch: {
    taskId: {
      immediate: true,
      handler(val) {
        if (val) this.resetAndLoad()
        else this.logs = []
      }
    }
  },
  methods: {
    buildParams() {
      const params = {}
      if (this.filters.action) {
        params.action = this.filters.action
      }
      if (this.filters.operatorId !== null && this.filters.operatorId !== undefined && this.filters.operatorId !== '') {
        params.operatorId = this.filters.operatorId
      }
      if (Array.isArray(this.filters.dateRange) && this.filters.dateRange.length === 2) {
        params.startTime = this.filters.dateRange[0]
        params.endTime = this.filters.dateRange[1]
      }
      return params
    },
    async loadLogs() {
      if (!this.taskId) return
      try {
        this.loading = true
        const res = await getTaskLogs(this.taskId, this.buildParams())
        this.logs = Array.isArray(res && res.data) ? res.data : []
      } catch (e) {
        const m = e.response?.data?.message || e.response?.data?.msg || '加载任务日志失败'
        this.$message.error(m)
        this.logs = []
      } finally {
        this.loading = false
      }
    },
    resetAndLoad() {
      this.loadLogs()
    },
    clearFilters() {
      this.filters = {
        action: '',
        operatorId: null,
        dateRange: []
      }
      this.loadLogs()
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
    },
    formatActionTag(action) {
      const map = {
        create: '创建',
        update: '更新',
        assign: '指派',
        change_status: '状态',
        change_priority: '优先级',
        comment: '评论',
        attach: '附件',
        complete: '完成',
        reopen: '重开',
        delete: '删除'
      }
      return map[action] || '动态'
    },
    resolveActionClass(item) {
      const action = item && item.action
      const map = {
        create: 'is-create',
        update: 'is-update',
        assign: 'is-assign',
        change_status: 'is-status',
        change_priority: 'is-priority',
        comment: 'is-comment',
        attach: 'is-attach',
        complete: 'is-complete',
        reopen: 'is-reopen',
        delete: 'is-delete'
      }
      return map[action] || 'is-default'
    },
    resolveActionIcon(item) {
      const action = item && item.action
      const map = {
        create: 'el-icon-circle-plus-outline',
        update: 'el-icon-edit-outline',
        assign: 'el-icon-user',
        change_status: 'el-icon-refresh',
        change_priority: 'el-icon-warning-outline',
        comment: 'el-icon-chat-dot-round',
        attach: 'el-icon-paperclip',
        complete: 'el-icon-check',
        reopen: 'el-icon-refresh-left',
        delete: 'el-icon-delete'
      }
      return map[action] || 'el-icon-time'
    },
    formatOperator(item) {
      return item.operatorName || item.operatorNickname || ('用户#' + (item.operatorId || '-'))
    },
    hasChange(item) {
      return !!(item && ((item.fieldLabel || item.fieldName) || item.oldValueDisplay || item.newValueDisplay))
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
.task-log-timeline {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.timeline-header-card,
.timeline-filter-card,
.panel-card,
.timeline-card {
  border: 1px solid var(--it-border);
  border-radius: 16px;
  background: var(--it-surface);
  box-shadow: var(--it-shadow-soft);
}

.timeline-header-card {
  padding: 16px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: var(--it-surface);
}

.timeline-filter-card {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.timeline-eyebrow {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--it-text-subtle);
  font-weight: 700;
}

.timeline-title {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 700;
  color: var(--it-text);
}

.timeline-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
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
  color: var(--it-text-muted);
}

.timeline-day-count {
  font-size: 12px;
  color: var(--it-text-subtle);
}

.timeline-item {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  gap: 14px;
  align-items: stretch;
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
  box-shadow: 0 12px 24px rgba(59, 130, 246, 0.18);
}

.timeline-line {
  flex: 1;
  width: 2px;
  margin-top: 8px;
  background: var(--it-surface-muted);
}

.timeline-card {
  padding: 16px 18px;
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
  color: var(--it-text);
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

.timeline-time {
  font-size: 12px;
  color: var(--it-text-subtle);
  white-space: nowrap;
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
  background: var(--it-accent-soft);
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

.timeline-operator-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--it-text-muted);
}

.timeline-operator-desc {
  margin-top: 3px;
  font-size: 12px;
  color: var(--it-text-subtle);
}

.timeline-change-box {
  margin-top: 14px;
  padding: 14px;
  border-radius: 14px;
  background: var(--it-surface);
  border: 1px solid var(--it-border);
}

.timeline-change-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--it-text-muted);
}

.timeline-change-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.timeline-change-chip {
  min-width: 160px;
  max-width: 100%;
  padding: 10px 12px;
  border-radius: 12px;
  background: var(--it-surface);
  border: 1px solid var(--it-border);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.timeline-change-chip.is-strong {
  border-color: var(--it-border-strong);
  box-shadow: 0 8px 20px color-mix(in srgb, var(--it-accent) 14%, transparent);
}

.chip-label {
  font-size: 12px;
  color: var(--it-text-subtle);
}

.chip-value {
  font-size: 13px;
  line-height: 1.7;
  color: var(--it-text-muted);
  word-break: break-word;
}

.empty-wrap {
  padding: 24px 12px;
}

.loading-wrap {
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--it-text-muted);
}

.is-create {
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-success) 78%, white), var(--it-success) 100%);
}

.is-update,
.is-default {
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-accent) 72%, white), var(--it-accent));
}

.is-assign {
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-tone-cyan-text) 82%, white), var(--it-accent));
}

.is-status,
.is-complete,
.is-reopen {
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-success) 76%, white), var(--it-success));
}

.is-priority {
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-warning) 80%, white), var(--it-warning));
}

.is-comment {
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-tone-purple-text) 82%, white), var(--it-tone-purple-text));
}

.is-attach {
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-tone-info-text) 80%, white), var(--it-tone-info-text));
}

.is-delete {
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-danger) 76%, white), var(--it-danger));
}

.timeline-badge.is-create {
  color: var(--it-tone-success-text);
  background: var(--it-success-soft);
  border-color: color-mix(in srgb, var(--it-success) 28%, var(--it-border));
}

.timeline-badge.is-update,
.timeline-badge.is-default {
  color: var(--it-accent);
  background: var(--it-tone-info-soft);
  border-color: var(--it-tone-info-border);
}

.timeline-badge.is-assign {
  color: var(--it-tone-cyan-text);
  background: var(--it-tone-cyan-soft);
  border-color: var(--it-tone-cyan-border);
}

.timeline-badge.is-status,
.timeline-badge.is-complete,
.timeline-badge.is-reopen {
  color: var(--it-tone-success-text);
  background: var(--it-success-soft);
  border-color: color-mix(in srgb, var(--it-success) 28%, var(--it-border));
}

.timeline-badge.is-priority {
  color: var(--it-tone-warning-text);
  background: var(--it-warning-soft);
  border-color: color-mix(in srgb, var(--it-warning) 28%, var(--it-border));
}

.timeline-badge.is-comment {
  color: var(--it-tone-purple-text);
  background: var(--it-tone-purple-soft);
  border-color: var(--it-tone-purple-border);
}

.timeline-badge.is-attach {
  color: var(--it-tone-info-text);
  background: var(--it-tone-info-soft);
  border-color: var(--it-tone-info-border);
}

.timeline-badge.is-delete {
  color: var(--it-tone-danger-text);
  background: var(--it-danger-soft);
  border-color: color-mix(in srgb, var(--it-danger) 28%, var(--it-border));
}

@media (max-width: 768px) {
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

  .timeline-change-chip {
    min-width: 100%;
  }
}


.timeline-header-card,
.timeline-filter-card,
.panel-card,
.timeline-card,
.timeline-change-box,
.timeline-change-chip,
.timeline-operator-avatar.is-image {
  border-color: var(--it-border);
  background: var(--it-panel-bg-strong);
  box-shadow: var(--it-shadow-soft);
}
.timeline-eyebrow,
.timeline-day-count,
.timeline-time,
.chip-label,
.timeline-operator-desc { color: var(--it-text-subtle); }
.timeline-title,
.timeline-action,
.timeline-operator-name,
.chip-value { color: var(--it-text); }
.timeline-day-title,
.timeline-change-title,
.loading-wrap { color: var(--it-text-muted); }
.timeline-operator-avatar { color: var(--it-accent); }
.timeline-line { background: var(--it-border); }
.timeline-change-chip.is-strong {
  border-color: var(--it-border-strong);
  box-shadow: 0 8px 20px color-mix(in srgb, var(--it-accent) 14%, transparent);
}
.is-create { background: linear-gradient(135deg, color-mix(in srgb, var(--it-success) 78%, white), var(--it-success) 100%); }
.is-update,
.is-default { background: linear-gradient(135deg, color-mix(in srgb, var(--it-accent) 72%, white), var(--it-accent)); }
.is-assign { background: linear-gradient(135deg, color-mix(in srgb, var(--it-tone-cyan-text) 82%, white), var(--it-accent)); }
.is-status,
.is-complete,
.is-reopen { background: linear-gradient(135deg, color-mix(in srgb, var(--it-success) 76%, white), var(--it-success)); }
.is-priority { background: linear-gradient(135deg, color-mix(in srgb, var(--it-warning) 80%, white), var(--it-warning)); }
.is-comment { background: linear-gradient(135deg, color-mix(in srgb, var(--it-tone-purple-text) 82%, white), var(--it-tone-purple-text)); }
.is-attach { background: linear-gradient(135deg, color-mix(in srgb, var(--it-tone-info-text) 80%, white), var(--it-tone-info-text)); }
.is-delete { background: linear-gradient(135deg, color-mix(in srgb, var(--it-danger) 76%, white), var(--it-danger)); }
.timeline-badge.is-create { color: var(--it-tone-success-text); background: var(--it-success-soft); border-color: color-mix(in srgb, var(--it-success) 28%, var(--it-border)); }
.timeline-badge.is-update,
.timeline-badge.is-default { color: var(--it-tone-info-text); background: var(--it-tone-info-soft); border-color: var(--it-tone-info-border); }
.timeline-badge.is-assign { color: var(--it-tone-cyan-text); background: var(--it-tone-cyan-soft); border-color: var(--it-tone-cyan-border); }
.timeline-badge.is-status,
.timeline-badge.is-complete,
.timeline-badge.is-reopen { color: var(--it-tone-success-text); background: var(--it-success-soft); border-color: color-mix(in srgb, var(--it-success) 28%, var(--it-border)); }
.timeline-badge.is-priority { color: var(--it-tone-warning-text); background: var(--it-warning-soft); border-color: color-mix(in srgb, var(--it-warning) 28%, var(--it-border)); }
.timeline-badge.is-comment { color: var(--it-tone-purple-text); background: var(--it-tone-purple-soft); border-color: var(--it-tone-purple-border); }
.timeline-badge.is-attach { color: var(--it-tone-info-text); background: var(--it-tone-info-soft); border-color: var(--it-tone-info-border); }
.timeline-badge.is-delete { color: var(--it-tone-danger-text); background: var(--it-danger-soft); border-color: color-mix(in srgb, var(--it-danger) 28%, var(--it-border)); }

</style>