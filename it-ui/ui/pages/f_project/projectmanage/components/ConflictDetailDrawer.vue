<template>
  <el-drawer
    :visible.sync="drawerVisible"
    size="62%"
    custom-class="conflict-detail-drawer"
    :with-header="false"
    @close="handleClose"
  >
    <div class="drawer-shell">
      <div class="drawer-header">
        <div>
          <div class="drawer-title">Merge Conflict Detail</div>
          <div class="drawer-subtitle">{{ subtitle }}</div>
        </div>
        <ConflictBadge
          :has-conflict="hasConflict"
          :conflict-count="conflictCount"
          :state="detailState"
          :clickable="false"
        />
      </div>

      <ConflictResolutionActionBar
        :has-conflict="hasConflict"
        :can-merge="canMerge"
        :can-recheck="canRecheck"
        :merge-disabled-reason="mergeDisabledReason"
        :status-summary="detail.summary || detail.suggestedAction || ''"
        :recheck-loading="recheckLoading"
        :merge-loading="mergeLoading"
        @refresh="$emit('refresh')"
        @recheck="$emit('recheck')"
        @merge="$emit('merge')"
      />

      <div class="drawer-body">
        <el-row :gutter="16">
          <el-col :xs="24" :lg="10">
            <ConflictListPanel
              :conflicts="conflicts"
              :loading="loading"
              :selected-conflict-id="selectedConflictId"
              @select="$emit('select-conflict', $event)"
            />
          </el-col>
          <el-col :xs="24" :lg="14">
            <el-card shadow="never" class="detail-card">
              <div slot="header" class="detail-header-row">
                <span>Conflict Item</span>
                <el-tag size="mini" effect="plain">{{ currentConflictTypeLabel }}</el-tag>
              </div>

              <el-skeleton v-if="loading" :rows="6" animated />
              <div v-else>
                <el-empty v-if="!currentConflict" description="Select a conflict item to inspect details" />
                <div v-else class="detail-grid">
                  <div class="detail-item">
                    <span class="detail-label">Base Path</span>
                    <span class="detail-value">{{ currentConflict.basePath || detail.basePath || '-' }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">Source Path</span>
                    <span class="detail-value">{{ currentConflict.sourcePath || detail.sourcePath || '-' }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">Target Path</span>
                    <span class="detail-value">{{ currentConflict.targetPath || detail.targetPath || '-' }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">Source Change</span>
                    <span class="detail-value">{{ currentConflict.sourceChangeType || '-' }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">Target Change</span>
                    <span class="detail-value">{{ currentConflict.targetChangeType || '-' }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">Severity</span>
                    <span class="detail-value">{{ currentConflict.severity || '-' }}</span>
                  </div>
                  <div class="detail-item full">
                    <span class="detail-label">Summary</span>
                    <span class="detail-value">{{ currentConflict.summary || detail.summary || '-' }}</span>
                  </div>
                  <div class="detail-item full">
                    <span class="detail-label">Suggested Action</span>
                    <span class="detail-value">{{ currentConflict.suggestedAction || detail.suggestedAction || '-' }}</span>
                  </div>
                </div>
              </div>
            </el-card>

            <el-card shadow="never" class="detail-card">
              <div slot="header" class="detail-header-row">
                <span>Merge Check Overview</span>
                <el-tag size="mini" :type="canMerge ? 'success' : 'warning'" effect="plain">
                  {{ canMerge ? 'Mergeable' : 'Blocked' }}
                </el-tag>
              </div>
              <div class="detail-grid">
                <div class="detail-item">
                  <span class="detail-label">MR Title</span>
                  <span class="detail-value">{{ detail.title || '-' }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">Source Branch</span>
                  <span class="detail-value">{{ detail.sourceBranchName || detail.sourceBranchId || '-' }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">Target Branch</span>
                  <span class="detail-value">{{ detail.targetBranchName || detail.targetBranchId || '-' }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">Conflict Count</span>
                  <span class="detail-value">{{ conflictCount }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">Source Commit</span>
                  <span class="detail-value">{{ detail.sourceHeadCommitId || detail.sourceCommitId || '-' }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">Target Commit</span>
                  <span class="detail-value">{{ detail.targetHeadCommitId || detail.targetCommitId || '-' }}</span>
                </div>
                <div class="detail-item full">
                  <span class="detail-label">Blocking Reasons</span>
                  <span class="detail-value">{{ blockingReasonText }}</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>
  </el-drawer>
</template>

<script>
import ConflictBadge from './ConflictBadge.vue'
import ConflictListPanel from './ConflictListPanel.vue'
import ConflictResolutionActionBar from './ConflictResolutionActionBar.vue'

export default {
  name: 'ConflictDetailDrawer',
  components: {
    ConflictBadge,
    ConflictListPanel,
    ConflictResolutionActionBar
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    },
    detail: {
      type: Object,
      default() {
        return {}
      }
    },
    conflicts: {
      type: Array,
      default() {
        return []
      }
    },
    selectedConflictId: {
      type: [Number, String],
      default: null
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
    mergeLoading: {
      type: Boolean,
      default: false
    },
    recheckLoading: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    drawerVisible: {
      get() {
        return this.visible
      },
      set(value) {
        this.$emit('update:visible', value)
      }
    },
    hasConflict() {
      if (typeof this.detail.hasConflict === 'boolean') return this.detail.hasConflict
      return this.conflictCount > 0
    },
    conflictCount() {
      const count = Number(this.detail.conflictCount)
      if (Number.isFinite(count) && count >= 0) return count
      return this.conflicts.length
    },
    subtitle() {
      return 'Structured conflict details, blocking reasons, and next actions'
    },
    currentConflict() {
      if (!this.conflicts.length) return null
      return this.conflicts.find(item => String(this.conflictKey(item) || '') === String(this.selectedConflictId || '')) || this.conflicts[0]
    },
    currentConflictTypeLabel() {
      const type = String((this.currentConflict && (this.currentConflict.conflictType || this.currentConflict.type)) || '').toUpperCase()
      return {
        CONTENT_CONFLICT: 'Content Conflict',
        DELETE_MODIFY_CONFLICT: 'Delete/Modify Conflict',
        RENAME_CONFLICT: 'Rename Conflict',
        MOVE_CONFLICT: 'Move Conflict',
        TARGET_PATH_OCCUPIED: 'Target Path Occupied',
        MISSING_BASE: 'Missing Base',
        STALE_BRANCH: 'Stale Branch'
      }[type] || 'Conflict'
    },
    detailState() {
      return this.loading ? 'unknown' : 'known'
    },
    blockingReasonText() {
      const reasons = Array.isArray(this.detail.blockingReasons) ? this.detail.blockingReasons : []
      if (!reasons.length) {
        return this.canMerge ? 'None' : (this.detail.summary || '-')
      }
      return reasons.join(' / ')
    }
  },
  methods: {
    conflictKey(conflict) {
      return conflict && (conflict.conflictId || conflict.id || conflict.filePath || conflict.path || conflict.oldPath || conflict.newPath)
    },
    handleClose() {
      this.$emit('close')
    }
  }
}
</script>

<style scoped>
.drawer-shell {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px 20px 20px;
  box-sizing: border-box;
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.drawer-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.drawer-subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.drawer-body {
  min-height: 0;
  flex: 1;
}

.detail-card {
  margin-bottom: 14px;
  border-radius: 14px;
}

.detail-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}

.detail-item.full {
  grid-column: 1 / -1;
}

.detail-label {
  color: #64748b;
  font-size: 12px;
}

.detail-value {
  color: #111827;
  font-size: 13px;
  line-height: 1.7;
  word-break: break-word;
}

@media (max-width: 768px) {
  .drawer-shell {
    padding: 14px;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
