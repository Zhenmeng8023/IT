<template>
  <div class="conflict-list-panel">
    <div class="panel-head">
      <div class="panel-title">Conflict List</div>
      <div class="panel-subtitle">{{ subtitle }}</div>
    </div>

    <el-empty v-if="!loading && !conflicts.length" :description="emptyText" />

    <div v-else class="conflict-list">
      <button
        v-for="item in conflicts"
        :key="conflictKey(item)"
        type="button"
        class="conflict-item"
        :class="{ active: isSelected(item) }"
        @click="$emit('select', item)"
      >
        <div class="conflict-item__top">
          <div class="conflict-path">{{ conflictPath(item) }}</div>
          <div class="conflict-tags">
            <el-tag
              size="mini"
              effect="plain"
              class="type-tag"
              :class="typeTagClass(item.conflictType || item.type)"
            >
              {{ typeLabel(item.conflictType || item.type) }}
            </el-tag>
            <el-tag size="mini" effect="plain" :type="severityType(item.severity)">
              {{ severityLabel(item.severity) }}
            </el-tag>
          </div>
        </div>

        <div v-if="item.summary" class="conflict-summary">{{ item.summary }}</div>
        <div v-if="item.suggestedAction" class="conflict-action">Action: {{ item.suggestedAction }}</div>
      </button>
    </div>

    <div v-if="loading" class="loading-box">
      <i class="el-icon-loading"></i>
      <span>Loading conflicts...</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ConflictListPanel',
  props: {
    conflicts: {
      type: Array,
      default() {
        return []
      }
    },
    loading: {
      type: Boolean,
      default: false
    },
    selectedConflictId: {
      type: [Number, String],
      default: null
    },
    emptyText: {
      type: String,
      default: 'No conflicts'
    },
    subtitle: {
      type: String,
      default: 'Browse structured conflict items and suggested actions'
    }
  },
  methods: {
    conflictKey(item) {
      return item && (item.conflictId || item.id || item.filePath || item.path || item.oldPath || item.newPath)
    },
    conflictPath(item) {
      if (!item) return '-'
      return item.filePath || item.newPath || item.sourcePath || item.targetPath || item.oldPath || item.path || item.name || '-'
    },
    isSelected(item) {
      return String(this.conflictKey(item) || '') === String(this.selectedConflictId || '')
    },
    severityLabel(value) {
      return {
        info: 'Info',
        low: 'Low',
        medium: 'Medium',
        high: 'High',
        critical: 'Critical',
        warning: 'Warning',
        error: 'Error'
      }[String(value || '').toLowerCase()] || String(value || 'Medium')
    },
    severityType(value) {
      return {
        info: 'info',
        low: 'info',
        medium: 'warning',
        warning: 'warning',
        high: 'danger',
        critical: 'danger',
        error: 'danger'
      }[String(value || '').toLowerCase()] || 'warning'
    },
    typeLabel(value) {
      const normalized = String(value || '').toUpperCase()
      return {
        CONTENT_CONFLICT: 'Content',
        DELETE_MODIFY_CONFLICT: 'Delete/Modify',
        RENAME_CONFLICT: 'Rename',
        MOVE_CONFLICT: 'Move',
        TARGET_PATH_OCCUPIED: 'Path Occupied',
        MISSING_BASE: 'Missing Base',
        STALE_BRANCH: 'Stale Branch'
      }[normalized] || normalized || 'Conflict'
    },
    typeTagClass(value) {
      const normalized = String(value || '').toUpperCase()
      return {
        RENAME_CONFLICT: 'is-special is-rename',
        MOVE_CONFLICT: 'is-special is-move',
        STALE_BRANCH: 'is-special is-warning'
      }[normalized] || ''
    }
  }
}
</script>

<style scoped>
.conflict-list-panel {
  position: relative;
  min-height: 220px;
}

.panel-head {
  margin-bottom: 12px;
}

.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
}

.panel-subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.conflict-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.conflict-item {
  appearance: none;
  width: 100%;
  text-align: left;
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  background: #fff;
  padding: 14px;
  cursor: pointer;
  transition: all .18s ease;
}

.conflict-item:hover,
.conflict-item.active {
  border-color: #409eff;
  box-shadow: 0 8px 16px rgba(64, 158, 255, 0.08);
}

.conflict-item__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.conflict-path {
  font-size: 13px;
  font-weight: 700;
  color: #303133;
  word-break: break-all;
}

.conflict-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.conflict-summary,
.conflict-action {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}

.type-tag.is-special {
  border-width: 1px;
}

.type-tag.is-rename {
  background: #f0f5ff;
  color: #2f54eb;
  border-color: #adc6ff;
}

.type-tag.is-move {
  background: #f6ffed;
  color: #389e0d;
  border-color: #b7eb8f;
}

.type-tag.is-warning {
  background: #fff7e6;
  color: #d48806;
  border-color: #ffd591;
}

.loading-box {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #409eff;
}
</style>
