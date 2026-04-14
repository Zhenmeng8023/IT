<template>
  <div class="conflict-list-panel">
    <div class="panel-head">
      <div class="panel-title">冲突列表</div>
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
        <div v-if="item.suggestedAction" class="conflict-action">建议操作：{{ item.suggestedAction }}</div>
      </button>
    </div>

    <div v-if="loading" class="loading-box">
      <i class="el-icon-loading"></i>
      <span>正在加载冲突列表...</span>
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
      default: '当前没有冲突'
    },
    subtitle: {
      type: String,
      default: '按文件查看冲突类型、严重程度和建议操作'
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
        info: '提示',
        low: '低',
        medium: '中',
        high: '高',
        critical: '严重',
        warning: '警告',
        error: '错误'
      }[String(value || '').toLowerCase()] || String(value || '中')
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
        CONTENT_CONFLICT: '内容冲突',
        DELETE_MODIFY_CONFLICT: '删改冲突',
        RENAME_CONFLICT: '重命名冲突',
        MOVE_CONFLICT: '移动冲突',
        TARGET_PATH_OCCUPIED: '路径被占用',
        MISSING_BASE: '缺少基线',
        STALE_BRANCH: '分支落后'
      }[normalized] || normalized || '冲突'
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
  color: var(--it-text);
}

.panel-subtitle {
  margin-top: 4px;
  color: var(--it-text-subtle);
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
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
  padding: 14px;
  cursor: pointer;
  transition: all .18s ease;
}

.conflict-item:hover,
.conflict-item.active {
  border-color: var(--it-accent);
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
  color: var(--it-text);
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
  color: var(--it-text-subtle);
}

.type-tag.is-special {
  border-width: 1px;
}

.type-tag.is-rename {
  background: var(--it-accent-soft);
  color: var(--it-accent);
  border-color: var(--it-border-strong);
}

.type-tag.is-move {
  background: color-mix(in srgb, var(--it-success) 14%, transparent);
  color: var(--it-success);
  border-color: color-mix(in srgb, var(--it-success) 40%, transparent);
}

.type-tag.is-warning {
  background: var(--it-warning-soft);
  color: var(--it-warning);
  border-color: color-mix(in srgb, var(--it-warning) 36%, transparent);
}

.loading-box {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--it-accent);
}
</style>
