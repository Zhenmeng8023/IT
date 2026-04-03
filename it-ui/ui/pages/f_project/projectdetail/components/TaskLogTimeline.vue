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
  border: 1px solid #e8eef7;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
}

.timeline-header-card {
  padding: 16px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
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
  color: #94a3b8;
  font-weight: 700;
}

.timeline-title {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
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
  color: #334155;
}

.timeline-day-count {
  font-size: 12px;
  color: #94a3b8;
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
  background: linear-gradient(180deg, rgba(148, 163, 184, 0.38) 0%, rgba(148, 163, 184, 0.08) 100%);
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
  color: #1f2937;
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
  color: #94a3b8;
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
  background: linear-gradient(135deg, #dbeafe 0%, #eff6ff 100%);
  color: #2563eb;
  font-weight: 700;
  flex-shrink: 0;
  object-fit: cover;
}

.timeline-operator-avatar.is-image {
  padding: 0;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.timeline-operator-name {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.timeline-operator-desc {
  margin-top: 3px;
  font-size: 12px;
  color: #94a3b8;
}

.timeline-change-box {
  margin-top: 14px;
  padding: 14px;
  border-radius: 14px;
  background: linear-gradient(180deg, #f8fbff 0%, #fdfefe 100%);
  border: 1px solid #eaf1f8;
}

.timeline-change-title {
  font-size: 13px;
  font-weight: 700;
  color: #475569;
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
  background: #fff;
  border: 1px solid #e5edf6;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.timeline-change-chip.is-strong {
  border-color: rgba(59, 130, 246, 0.22);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.08);
}

.chip-label {
  font-size: 12px;
  color: #94a3b8;
}

.chip-value {
  font-size: 13px;
  line-height: 1.7;
  color: #334155;
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
  color: #64748b;
}

.is-create {
  background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
}

.is-update,
.is-default {
  background: linear-gradient(135deg, #60a5fa 0%, #2563eb 100%);
}

.is-assign {
  background: linear-gradient(135deg, #38bdf8 0%, #0284c7 100%);
}

.is-status,
.is-complete,
.is-reopen {
  background: linear-gradient(135deg, #34d399 0%, #059669 100%);
}

.is-priority {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.is-comment {
  background: linear-gradient(135deg, #a78bfa 0%, #7c3aed 100%);
}

.is-attach {
  background: linear-gradient(135deg, #818cf8 0%, #4f46e5 100%);
}

.is-delete {
  background: linear-gradient(135deg, #f87171 0%, #dc2626 100%);
}

.timeline-badge.is-create {
  color: #15803d;
  background: rgba(34, 197, 94, 0.08);
  border-color: rgba(34, 197, 94, 0.18);
}

.timeline-badge.is-update,
.timeline-badge.is-default {
  color: #2563eb;
  background: rgba(37, 99, 235, 0.08);
  border-color: rgba(37, 99, 235, 0.18);
}

.timeline-badge.is-assign {
  color: #0284c7;
  background: rgba(2, 132, 199, 0.08);
  border-color: rgba(2, 132, 199, 0.18);
}

.timeline-badge.is-status,
.timeline-badge.is-complete,
.timeline-badge.is-reopen {
  color: #059669;
  background: rgba(5, 150, 105, 0.08);
  border-color: rgba(5, 150, 105, 0.18);
}

.timeline-badge.is-priority {
  color: #b45309;
  background: rgba(245, 158, 11, 0.1);
  border-color: rgba(245, 158, 11, 0.2);
}

.timeline-badge.is-comment {
  color: #7c3aed;
  background: rgba(124, 58, 237, 0.08);
  border-color: rgba(124, 58, 237, 0.18);
}

.timeline-badge.is-attach {
  color: #4338ca;
  background: rgba(79, 70, 229, 0.08);
  border-color: rgba(79, 70, 229, 0.18);
}

.timeline-badge.is-delete {
  color: #dc2626;
  background: rgba(220, 38, 38, 0.08);
  border-color: rgba(220, 38, 38, 0.18);
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
</style>
