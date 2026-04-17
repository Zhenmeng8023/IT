<template>
  <el-card shadow="never" class="path-conflict-panel" :class="[`tone-${typeTone}`, { 'is-resolving': resolving }]">
    <div slot="header" class="panel-header">
      <div class="header-copy">
        <div class="header-eyebrow">冲突处理</div>
        <div class="header-title-row">
          <i :class="typeIcon" class="header-icon"></i>
          <div class="header-text">
            <div class="header-title">{{ panelTitle }}</div>
            <div class="header-subtitle">{{ panelSubtitle }}</div>
          </div>
        </div>
      </div>

      <el-tag size="mini" effect="plain" :type="typeTone">
        {{ typeLabel }}
      </el-tag>
    </div>

    <div v-if="loading" class="panel-state">
      <el-skeleton :rows="6" animated />
    </div>

    <div v-else class="panel-body">
      <el-alert
        v-if="errorText"
        class="panel-alert"
        :title="errorText"
        type="error"
        :closable="false"
        show-icon
      />

      <el-alert
        v-if="isStaleBranch"
        class="panel-alert"
        title="这是分支基线落后，不是普通内容冲突"
        :description="staleBranchHint"
        type="warning"
        :closable="false"
        show-icon
      />

      <div v-if="isStaleBranch" class="stale-actions">
        <el-button size="small" type="warning" :loading="resolving" @click="$emit('update-source')">
          更新源分支
        </el-button>
        <el-button size="small" type="warning" plain :loading="resolving" @click="$emit('recheck')">
          重新检查
        </el-button>
      </div>

      <template v-if="!isStaleBranch">
        <div class="path-grid">
          <div class="path-card">
            <div class="path-label">基线路径</div>
            <div class="path-value">{{ basePath || '-' }}</div>
          </div>
          <div class="path-card">
            <div class="path-label">源分支路径</div>
            <div class="path-value">{{ sourcePath || '-' }}</div>
          </div>
          <div class="path-card">
            <div class="path-label">目标分支路径</div>
            <div class="path-value">{{ targetPath || '-' }}</div>
          </div>
          <div class="path-card">
            <div class="path-label">当前类型</div>
            <div class="path-value">{{ typeLabel }}</div>
          </div>
          <div class="path-card path-card--wide">
            <div class="path-label">摘要</div>
            <div class="path-value path-value--wrap">{{ summaryText || '-' }}</div>
          </div>
          <div class="path-card path-card--wide">
            <div class="path-label">建议处理</div>
            <div class="path-value path-value--wrap">{{ suggestedActionText || '-' }}</div>
          </div>
        </div>

        <el-alert
          v-if="hasRelatedContentConflict"
          class="panel-alert"
          title="该冲突同时伴随内容变化"
          :description="relatedContentHint"
          type="info"
          :closable="false"
          show-icon
        >
          <template slot="title">
            该冲突同时伴随内容变化
          </template>
          <template slot="default">
            <div class="related-content-actions">
              <el-button size="mini" type="primary" plain :disabled="resolving" @click="$emit('open-related-content')">
                前往内容冲突编辑器
              </el-button>
              <span class="related-content-note">不要把路径冲突伪装成普通文本冲突。</span>
            </div>
          </template>
        </el-alert>

        <div class="action-shell">
          <div class="action-title">处理方式</div>
          <div class="action-row">
            <el-button
              size="small"
              type="primary"
              :loading="resolving"
              :disabled="!canResolvePathConflict"
              @click="$emit('use-source')"
            >
              保留源分支版本
            </el-button>
            <el-button
              size="small"
              type="success"
              plain
              :loading="resolving"
              :disabled="!canResolvePathConflict"
              @click="$emit('use-target')"
            >
              保留目标分支版本
            </el-button>
            <el-button
              size="small"
              plain
              :disabled="true"
            >
              自定义路径暂未开放
            </el-button>
          </div>

          <div v-if="!customPathSupported" class="custom-path-note">
            <el-input
              :value="customPathPlaceholder"
              size="small"
              disabled
            />
            <el-tag size="mini" effect="plain" type="info">已禁用</el-tag>
          </div>
        </div>

        <div v-if="!canResolvePathConflict" class="panel-footnote">
          当前冲突类型只提供提示，不支持在这里直接应用保留策略。
        </div>
      </template>
    </div>
  </el-card>
</template>

<script>
import {
  getConflictPathCandidates,
  getConflictTypeMeta,
  getPathConflictActionMeta,
  hasPathConflictInfo,
  isPathConflictType,
  normalizeConflictType
} from '@/utils/projectMergeConflictAdapter'

export default {
  name: 'PathConflictPanel',
  props: {
    conflict: {
      type: Object,
      default: null
    },
    relatedContentConflict: {
      type: Object,
      default: null
    },
    loading: {
      type: Boolean,
      default: false
    },
    resolving: {
      type: Boolean,
      default: false
    },
    errorText: {
      type: String,
      default: ''
    },
    customPathSupported: {
      type: Boolean,
      default: false
    },
    canRecheck: {
      type: Boolean,
      default: true
    }
  },
  computed: {
    conflictType() {
      return normalizeConflictType(this.conflict && (this.conflict.conflictType || this.conflict.type))
    },
    typeMeta() {
      return getConflictTypeMeta(this.conflictType)
    },
    typeLabel() {
      return this.typeMeta.label
    },
    typeTone() {
      return this.typeMeta.tone || 'info'
    },
    typeIcon() {
      return this.typeMeta.icon || 'el-icon-document'
    },
    hasPathInfo() {
      return hasPathConflictInfo(this.conflict)
    },
    isStaleBranch() {
      return this.conflictType === 'STALE_BRANCH'
    },
    isPathConflict() {
      return isPathConflictType(this.conflictType, this.conflict)
    },
    canResolvePathConflict() {
      return !this.isStaleBranch && this.isPathConflict && getPathConflictActionMeta(this.conflict).length > 0
    },
    basePath() {
      return this.conflict && (this.conflict.basePath || this.conflict.oldPath || '')
    },
    sourcePath() {
      return this.conflict && (this.conflict.sourcePath || this.conflict.newPath || '')
    },
    targetPath() {
      return this.conflict && (this.conflict.targetPath || this.conflict.path || '')
    },
    summaryText() {
      return this.conflict && (this.conflict.summary || this.conflict.displaySummary || '')
    },
    suggestedActionText() {
      return this.conflict && (this.conflict.suggestedAction || '')
    },
    panelTitle() {
      if (this.isStaleBranch) return '分支落后提示'
      return this.isPathConflict ? '路径类冲突' : '非路径冲突'
    },
    panelSubtitle() {
      if (this.isStaleBranch) {
        return this.suggestedActionText || this.summaryText || '请先更新源分支，再重新检查当前 MR。'
      }
      if (this.isPathConflict) {
        return this.suggestedActionText || this.summaryText || '请先确认保留源分支版本还是保留目标分支版本。'
      }
      return '当前项不属于路径冲突，建议切换到内容编辑器。'
    },
    customPathPlaceholder() {
      return '后端暂未开放自定义路径'
    },
    staleBranchHint() {
      const detail = this.summaryText || this.suggestedActionText
      const baseHint = '“更新源分支”会把目标分支最新内容合入源分支，并可能产生新的内容冲突；该操作不会直接改动目标分支。'
      return detail ? `${detail}。${baseHint}` : baseHint
    },
    relatedContentHint() {
      if (!this.relatedContentConflict) {
        return '该路径冲突附近未发现独立的内容冲突项。'
      }
      const pathList = getConflictPathCandidates(this.relatedContentConflict)
      const conflictId = this.relatedContentConflict.conflictId || this.relatedContentConflict.id || '-'
      return `发现关联内容冲突 #${conflictId}，涉及路径：${pathList.join(' / ') || '-'}。优先处理内容差异，再回到这里选择保留版本。`
    }
  }
}
</script>

<style scoped>
.path-conflict-panel {
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid #e5ecf6;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
}

.path-conflict-panel.tone-warning {
  border-color: #fde68a;
}

.path-conflict-panel.tone-danger {
  border-color: #fecaca;
}

.path-conflict-panel.tone-success {
  border-color: #bbf7d0;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.header-copy {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.header-eyebrow {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #2563eb;
  font-weight: 700;
}

.header-title-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.header-icon {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #eff6ff;
  color: #2563eb;
  font-size: 16px;
  flex: 0 0 auto;
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.header-title {
  font-size: 22px;
  line-height: 1.4;
  color: #0f172a;
  font-weight: 700;
}

.header-subtitle {
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
}

.panel-state {
  min-height: 240px;
  display: flex;
  align-items: center;
}

.panel-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.stale-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-alert {
  border-radius: 14px;
}

.path-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.path-card {
  border-radius: 14px;
  border: 1px solid #e5ecf6;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  padding: 14px;
}

.path-card--wide {
  grid-column: span 2;
}

.path-label {
  font-size: 12px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.path-value {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.7;
  color: #0f172a;
  font-weight: 600;
  word-break: break-all;
}

.path-value--wrap {
  white-space: pre-wrap;
  font-weight: 500;
}

.action-shell {
  border-radius: 16px;
  border: 1px solid #dbe7f7;
  background: #f8fbff;
  padding: 16px;
}

.action-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.custom-path-note {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.related-content-actions {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.related-content-note {
  font-size: 12px;
  color: #64748b;
}

.panel-footnote {
  font-size: 12px;
  line-height: 1.7;
  color: #94a3b8;
}

@media (max-width: 768px) {
  .panel-header {
    flex-direction: column;
  }

  .path-grid {
    grid-template-columns: 1fr;
  }

  .path-card--wide {
    grid-column: span 1;
  }

  .custom-path-note {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
