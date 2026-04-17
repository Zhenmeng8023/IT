<template>
  <div class="conflict-action-bar">
    <div class="status-block">
      <el-tag size="mini" :type="statusType" effect="dark">{{ statusLabel }}</el-tag>
      <span class="status-text">{{ statusHint }}</span>
    </div>

    <div class="action-block">
      <el-button size="mini" plain @click="$emit('refresh')">查看门禁明细</el-button>
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
        v-if="canUpdateSource"
        size="mini"
        type="warning"
        plain
        :disabled="recheckLoading"
        @click="$emit('update-source')"
      >
        更新源分支
      </el-button>
      <el-button
        size="mini"
        type="primary"
        plain
        :disabled="!canOpenConflictCenter"
        @click="$emit('open-conflict-center')"
      >
        进入冲突处理中心
      </el-button>
      <el-button
        size="mini"
        type="success"
        :disabled="!canMerge || !!mergeDisabledReason"
        :loading="mergeLoading"
        @click="$emit('merge')"
      >
        合并 MR
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
    canUpdateSource: {
      type: Boolean,
      default: false
    },
    canOpenConflictCenter: {
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
      if (!this.hasConflict && this.canMerge) return '当前 MR 已通过冲突检查，抽屉仅做概览，可直接合并 MR。'
      if (this.canUpdateSource) return '这是分支基线落后，请先更新源分支，再重新检查。'
      if (!this.hasConflict) return '当前没有结构化冲突，但仍有其他阻塞项。'
      return '冲突处理请进入冲突处理中心，抽屉仅保留概览和快速跳转。'
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
