<template>
  <div class="conflict-action-bar">
    <div class="status-block">
      <el-tag size="mini" :type="statusType" effect="dark">{{ statusLabel }}</el-tag>
      <span class="status-text">{{ statusHint }}</span>
    </div>

    <div class="action-block">
      <el-button size="mini" plain @click="$emit('refresh')">Refresh</el-button>
      <el-button
        size="mini"
        type="warning"
        :disabled="!canRecheck"
        :loading="recheckLoading"
        @click="$emit('recheck')"
      >
        Recheck
      </el-button>
      <el-button
        size="mini"
        type="success"
        :disabled="!canMerge || !!mergeDisabledReason"
        :loading="mergeLoading"
        @click="$emit('merge')"
      >
        Merge
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
      if (!this.hasConflict && this.canMerge) return 'Mergeable'
      if (!this.hasConflict) return 'Pending'
      return 'Conflict'
    },
    statusHint() {
      if (this.statusSummary) return this.statusSummary
      if (!this.hasConflict && this.canMerge) return 'Merge checks passed and merge can continue.'
      if (!this.hasConflict) return 'No structured conflict exists, but merge is still blocked by other gates.'
      return 'Resolve the conflict, push follow-up commits if needed, then run merge check again.'
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
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}

.status-block {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.status-text {
  color: #64748b;
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
