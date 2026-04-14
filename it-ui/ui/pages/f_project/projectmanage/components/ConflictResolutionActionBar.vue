<template>
  <div class="conflict-action-bar">
    <div class="status-block">
      <el-tag size="mini" :type="statusType" effect="dark">{{ statusLabel }}</el-tag>
      <span class="status-text">{{ statusHint }}</span>
    </div>

    <div class="action-block">
      <el-button size="mini" plain @click="$emit('refresh')">刷新结果</el-button>
      <el-button
        size="mini"
        type="warning"
        :disabled="!canRecheck"
        :loading="recheckLoading"
        @click="$emit('recheck')"
      >
        重新检查
      </el-button>
      <el-button
        size="mini"
        type="success"
        :disabled="!canMerge || !!mergeDisabledReason"
        :loading="mergeLoading"
        @click="$emit('merge')"
      >
        继续合并
      </el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ConflictResolutionActionBar',
  props: {
    hasConflict: {
      type: Boolean,
      default: false
    },
    canMerge: {
      type: Boolean,
      default: true
    },
    canRecheck: {
      type: Boolean,
      default: true
    },
    mergeDisabledReason: {
      type: String,
      default: ''
    },
    statusSummary: {
      type: String,
      default: ''
    },
    recheckLoading: {
      type: Boolean,
      default: false
    },
    mergeLoading: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    statusType() {
      if (!this.hasConflict && this.canMerge) return 'success'
      if (!this.hasConflict) return 'warning'
      return 'danger'
    },
    statusLabel() {
      if (!this.hasConflict && this.canMerge) return '可合并'
      if (!this.hasConflict) return '待处理'
      return '存在冲突'
    },
    statusHint() {
      if (this.statusSummary) return this.statusSummary
      if (!this.hasConflict && this.canMerge) return '当前 MR 已通过冲突检查，可以继续合并。'
      if (!this.hasConflict) return '当前没有结构化冲突，但仍有其他合并前阻断项。'
      return '请先处理冲突，再重新检查或继续合并。'
    }
  }
}
</script>

<style scoped>
.conflict-action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--it-surface-muted);
  border: 1px solid var(--it-border);
}

.status-block {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.status-text {
  color: var(--it-text-subtle);
  font-size: 12px;
  line-height: 1.5;
}

.action-block {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .conflict-action-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .status-block,
  .action-block {
    width: 100%;
  }

  .action-block {
    justify-content: flex-start;
  }
}
</style>
