<template>
  <div class="task-log-timeline">
    <div v-if="!logs.length" class="panel-card empty-wrap">
      <el-empty description="暂无动态" />
    </div>
    <div v-else class="log-list">
      <div v-for="item in logs" :key="item.id" class="log-card">
        <div class="log-top">
          <div class="log-action">{{ formatAction(item) }}</div>
          <div class="log-time">{{ formatDate(item.createdAt) }}</div>
        </div>
        <div class="log-operator">操作人：{{ item.operatorName || item.operatorNickname || ('用户#' + (item.operatorId || '-')) }}</div>
        <div v-if="item.fieldName || item.oldValue || item.newValue" class="log-change">
          <span v-if="item.fieldName">字段：{{ item.fieldName }}</span>
          <span v-if="item.oldValue !== undefined && item.oldValue !== null">旧值：{{ item.oldValue }}</span>
          <span v-if="item.newValue !== undefined && item.newValue !== null">新值：{{ item.newValue }}</span>
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
    formatDate(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return date.toLocaleString()
    }
  }
}
</script>

<style scoped>
.task-log-timeline { display: flex; flex-direction: column; gap: 12px; }
.panel-card, .log-card { border: 1px solid #ebeef5; border-radius: 12px; background: #fff; padding: 12px; }
.log-list { display: flex; flex-direction: column; gap: 12px; }
.log-top { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.log-action { font-weight: 600; color: #303133; }
.log-time { font-size: 12px; color: #909399; }
.log-operator { margin-top: 8px; color: #606266; }
.log-change { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 10px; font-size: 13px; color: #909399; }
.empty-wrap { padding: 20px 12px; }
</style>
