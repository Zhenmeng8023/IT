<template>
  <div class="task-log-timeline">
    <div class="timeline-header-card">
      <div>
        <div class="timeline-eyebrow">任务动态</div>
        <div class="timeline-title">操作时间线</div>
      </div>
      <el-button size="mini" type="text" @click="loadLogs">刷新</el-button>
    </div>

    <div v-if="!logs.length" class="panel-card empty-wrap">
      <el-empty description="暂无动态" />
    </div>

    <div v-else class="timeline-shell">
      <div v-for="(item, index) in logs" :key="item.id || index" class="timeline-item">
        <div class="timeline-axis">
          <div class="timeline-dot" :class="resolveActionClass(item)">
            <i :class="resolveActionIcon(item)"></i>
          </div>
          <div v-if="index !== logs.length - 1" class="timeline-line"></div>
        </div>

        <div class="timeline-card">
          <div class="timeline-card-top">
            <div class="timeline-title-group">
              <div class="timeline-action">{{ formatAction(item) }}</div>
              <span class="timeline-badge" :class="resolveActionClass(item)">{{ formatActionTag(item) }}</span>
            </div>
            <div class="timeline-time">{{ formatDate(item.createdAt) }}</div>
          </div>

          <div class="timeline-operator-row">
            <div class="timeline-operator-avatar">{{ firstChar(formatOperator(item)) }}</div>
            <div class="timeline-operator-text">
              <div class="timeline-operator-name">{{ formatOperator(item) }}</div>
              <div class="timeline-operator-desc">对当前任务产生了一次协作变更</div>
            </div>
          </div>

          <div v-if="hasChange(item)" class="timeline-change-box">
            <div class="timeline-change-title">变更详情</div>
            <div class="timeline-change-grid">
              <div v-if="item.fieldName" class="timeline-change-chip">
                <span class="chip-label">字段</span>
                <span class="chip-value">{{ item.fieldName }}</span>
              </div>
              <div v-if="item.oldValue !== undefined && item.oldValue !== null && item.oldValue !== ''" class="timeline-change-chip">
                <span class="chip-label">旧值</span>
                <span class="chip-value">{{ formatValue(item.oldValue) }}</span>
              </div>
              <div v-if="item.newValue !== undefined && item.newValue !== null && item.newValue !== ''" class="timeline-change-chip is-strong">
                <span class="chip-label">新值</span>
                <span class="chip-value">{{ formatValue(item.newValue) }}</span>
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
      logs: []
    }
  },
  watch: {
    taskId: {
      immediate: true,
      handler(val) {
        if (val) this.loadLogs()
        else this.logs = []
      }
    }
  },
  methods: {
    async loadLogs() {
      if (!this.taskId) return
      const res = await getTaskLogs(this.taskId)
      this.logs = Array.isArray(res && res.data) ? res.data : []
    },
    formatAction(item) {
      const action = item && item.action
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
    formatActionTag(item) {
      const action = item && item.action
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
    formatValue(value) {
      if (value === null || value === undefined || value === '') return '—'
      if (typeof value === 'object') {
        try {
          return JSON.stringify(value)
        } catch (e) {
          return String(value)
        }
      }
      return String(value)
    },
    hasChange(item) {
      return !!(item && (item.fieldName || item.oldValue !== undefined || item.newValue !== undefined))
    },
    formatDate(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return date.toLocaleString()
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
  gap: 16px;
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
