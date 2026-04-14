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
          <div class="drawer-title">合并冲突详情</div>
          <div class="drawer-subtitle">查看冲突项、阻塞原因与处理动作。</div>
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
                <span>冲突项</span>
                <el-tag size="mini" effect="plain">{{ currentConflictTypeLabel }}</el-tag>
              </div>

              <el-skeleton v-if="loading" :rows="6" animated />
              <div v-else>
                <el-empty v-if="!currentConflict" description="请先选择一个冲突项" />
                <div v-else class="detail-grid">
                  <div class="detail-item">
                    <span class="detail-label">基线路径</span>
                    <span class="detail-value">{{ currentConflict.basePath || detail.basePath || '-' }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">源分支路径</span>
                    <span class="detail-value">{{ currentConflict.sourcePath || detail.sourcePath || '-' }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">目标分支路径</span>
                    <span class="detail-value">{{ currentConflict.targetPath || detail.targetPath || '-' }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">源分支变更</span>
                    <span class="detail-value">{{ currentConflict.sourceChangeType || '-' }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">目标分支变更</span>
                    <span class="detail-value">{{ currentConflict.targetChangeType || '-' }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">严重级别</span>
                    <span class="detail-value">{{ currentConflict.severity || '-' }}</span>
                  </div>
                  <div class="detail-item full">
                    <span class="detail-label">摘要</span>
                    <span class="detail-value">{{ currentConflict.summary || detail.summary || '-' }}</span>
                  </div>
                  <div class="detail-item full">
                    <span class="detail-label">建议处理</span>
                    <span class="detail-value">{{ currentConflict.suggestedAction || detail.suggestedAction || '-' }}</span>
                  </div>
                </div>
              </div>
            </el-card>

            <el-card shadow="never" class="detail-card">
              <div slot="header" class="detail-header-row">
                <span>处理区</span>
                <el-tag size="mini" effect="plain" :type="resolutionTagType">
                  {{ resolutionTagLabel }}
                </el-tag>
              </div>

              <el-empty v-if="!currentConflict" description="请选择要处理的冲突" />

              <div v-else-if="isContentConflict" class="content-resolution">
                <div class="content-entry-bar">
                  <el-button
                    size="mini"
                    type="primary"
                    plain
                    :loading="contentConflictLoading"
                    @click="openContentEditor"
                  >
                    打开内容编辑器
                  </el-button>
                  <span class="content-entry-tip">仅内容冲突支持在线编辑。</span>
                </div>

                <div v-if="contentEditorExpanded" class="content-editor-shell">
                  <el-alert
                    title="编辑器会基于基线/源分支/目标分支内容初始化"
                    type="info"
                    :closable="false"
                    show-icon
                    class="content-editor-hint"
                  />

                  <el-skeleton v-if="contentConflictLoading && !contentDetailReady" :rows="6" animated />
                  <el-empty
                    v-else-if="!contentDetailReady"
                    description="尚未加载内容，请点击“打开内容编辑器”。"
                  />
                  <div v-else>
                    <div class="content-meta-row">
                      <el-tag size="mini" effect="plain">冲突块：{{ contentBlocks.length }}</el-tag>
                      <el-button size="mini" plain :loading="contentConflictLoading" @click="refreshContentEditor">
                        重新加载
                      </el-button>
                    </div>

                    <div class="content-compare-grid">
                      <div class="content-pane">
                        <div class="content-pane-title">基线版本</div>
                        <el-input
                          :value="contentDetailReady.baseContent"
                          type="textarea"
                          :autosize="{ minRows: 5, maxRows: 10 }"
                          readonly
                        />
                      </div>
                      <div class="content-pane">
                        <div class="content-pane-title">源分支版本</div>
                        <el-input
                          :value="contentDetailReady.sourceContent"
                          type="textarea"
                          :autosize="{ minRows: 5, maxRows: 10 }"
                          readonly
                        />
                      </div>
                      <div class="content-pane">
                        <div class="content-pane-title">目标分支版本</div>
                        <el-input
                          :value="contentDetailReady.targetContent"
                          type="textarea"
                          :autosize="{ minRows: 5, maxRows: 10 }"
                          readonly
                        />
                      </div>
                    </div>

                    <div class="content-final-box">
                      <div class="content-final-head">
                        <span>最终内容</span>
                        <div class="content-final-actions">
                          <el-button size="mini" plain @click="useSourceContent">使用源分支</el-button>
                          <el-button size="mini" plain @click="useTargetContent">使用目标分支</el-button>
                        </div>
                      </div>

                      <el-input
                        :value="currentContentDraft"
                        type="textarea"
                        :autosize="{ minRows: 8, maxRows: 16 }"
                        @input="handleContentDraftInput"
                      />
                    </div>

                    <div class="resolution-actions">
                      <el-button
                        size="mini"
                        type="primary"
                        :loading="contentConflictApplying"
                        :disabled="!canSaveContentConflict"
                        @click="emitSaveContentConflict"
                      >
                        保存并应用
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>

              <div v-else-if="!isStructuredConflict" class="resolution-unsupported">
                当前冲突类型暂不支持在线处理。
              </div>

              <div v-else>
                <el-form label-width="96px" size="small">
                  <el-form-item label="类型">
                    <span class="resolution-meta">{{ currentConflictTypeLabel }}</span>
                  </el-form-item>
                  <el-form-item label="策略">
                    <el-radio-group v-model="editingDraft.resolutionStrategy" @change="handleDraftChange">
                      <el-radio
                        v-for="item in strategyOptions"
                        :key="item.value"
                        :label="item.value"
                      >
                        {{ item.label }}
                      </el-radio>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item v-if="needTargetPath" label="新路径">
                    <el-input
                      v-model.trim="editingDraft.targetPath"
                      placeholder="请输入目标路径"
                      @input="handleDraftChange"
                    />
                  </el-form-item>
                </el-form>

                <div v-if="unresolvedConflictIds.length" class="unresolved-box">
                  <div class="unresolved-title">应用后仍未解决的冲突</div>
                  <div class="unresolved-list">
                    <el-tag
                      v-for="id in unresolvedConflictIds"
                      :key="id"
                      size="mini"
                      type="danger"
                      effect="plain"
                    >
                      {{ id }}
                    </el-tag>
                  </div>
                </div>

                <div class="resolution-actions">
                  <el-button
                    size="mini"
                    plain
                    :loading="saveResolutionLoading"
                    :disabled="!canSaveCurrentDraft"
                    @click="emitSaveResolution"
                  >
                    保存草稿
                  </el-button>
                  <el-button
                    size="mini"
                    type="primary"
                    :loading="applyResolutionLoading"
                    :disabled="!canApplyWithCurrentDraft"
                    @click="emitApplyResolution"
                  >
                    应用并重检
                  </el-button>
                </div>
              </div>
            </el-card>

            <el-card shadow="never" class="detail-card">
              <div slot="header" class="detail-header-row">
                <span>合并检查概览</span>
                <el-tag size="mini" :type="canMerge ? 'success' : 'warning'" effect="plain">
                  {{ canMerge ? '可合并' : '阻塞中' }}
                </el-tag>
              </div>
              <div class="detail-grid">
                <div class="detail-item">
                  <span class="detail-label">MR 标题</span>
                  <span class="detail-value">{{ detail.title || '-' }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">源分支</span>
                  <span class="detail-value">{{ detail.sourceBranchName || detail.sourceBranchId || '-' }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">目标分支</span>
                  <span class="detail-value">{{ detail.targetBranchName || detail.targetBranchId || '-' }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">冲突数量</span>
                  <span class="detail-value">{{ conflictCount }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">源分支提交</span>
                  <span class="detail-value">{{ detail.sourceHeadCommitId || detail.sourceCommitId || '-' }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">目标分支提交</span>
                  <span class="detail-value">{{ detail.targetHeadCommitId || detail.targetCommitId || '-' }}</span>
                </div>
                <div class="detail-item full">
                  <span class="detail-label">阻塞原因</span>
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

const TYPE_LABELS = {
  CONTENT_CONFLICT: '内容冲突',
  DELETE_MODIFY_CONFLICT: '删除/修改冲突',
  RENAME_CONFLICT: '重命名冲突',
  MOVE_CONFLICT: '移动冲突',
  TARGET_PATH_OCCUPIED: '目标路径被占用',
  MISSING_BASE: '缺少基线',
  STALE_BRANCH: '分支落后'
}

const STRATEGY_OPTIONS = {
  STALE_BRANCH: [{ value: 'SYNC_SOURCE_WITH_TARGET', label: '同步源分支到目标分支' }],
  DELETE_MODIFY_CONFLICT: [
    { value: 'KEEP_SOURCE', label: '保留源分支（删除）' },
    { value: 'KEEP_TARGET', label: '保留目标分支（修改）' }
  ],
  RENAME_CONFLICT: [
    { value: 'USE_SOURCE_PATH', label: '使用源分支路径' },
    { value: 'USE_TARGET_PATH', label: '使用目标分支路径' },
    { value: 'SET_TARGET_PATH', label: '设置新目标路径' }
  ],
  MOVE_CONFLICT: [
    { value: 'USE_SOURCE_PATH', label: '使用源分支移动结果' },
    { value: 'USE_TARGET_PATH', label: '使用目标分支移动结果' },
    { value: 'SET_TARGET_PATH', label: '设置新目标路径' }
  ],
  TARGET_PATH_OCCUPIED: [
    { value: 'KEEP_SOURCE', label: '保留源分支路径' },
    { value: 'KEEP_TARGET', label: '保留目标分支路径' },
    { value: 'SET_TARGET_PATH', label: '设置新目标路径' }
  ]
}

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
    },
    resolutionMap: {
      type: Object,
      default() {
        return {}
      }
    },
    saveResolutionLoading: {
      type: Boolean,
      default: false
    },
    applyResolutionLoading: {
      type: Boolean,
      default: false
    },
    unresolvedConflictIds: {
      type: Array,
      default() {
        return []
      }
    },
    contentConflictDetail: {
      type: Object,
      default: null
    },
    contentConflictLoading: {
      type: Boolean,
      default: false
    },
    contentConflictApplying: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      editingDraft: {
        conflictId: '',
        conflictType: '',
        resolutionStrategy: '',
        targetPath: ''
      },
      syncingDraft: false,
      contentEditorExpanded: false,
      contentDraft: '',
      contentDraftConflictId: ''
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
    currentConflict() {
      if (!this.conflicts.length) return null
      return this.conflicts.find(item => String(this.conflictKey(item) || '') === String(this.selectedConflictId || '')) || this.conflicts[0]
    },
    currentConflictType() {
      return String((this.currentConflict && (this.currentConflict.conflictType || this.currentConflict.type)) || '').toUpperCase()
    },
    currentConflictTypeLabel() {
      return TYPE_LABELS[this.currentConflictType] || '冲突'
    },
    currentConflictId() {
      return String(this.conflictKey(this.currentConflict) || '')
    },
    isContentConflict() {
      return this.currentConflictType === 'CONTENT_CONFLICT'
    },
    strategyOptions() {
      return STRATEGY_OPTIONS[this.currentConflictType] || []
    },
    isStructuredConflict() {
      return !!this.strategyOptions.length
    },
    resolutionTagType() {
      if (this.isContentConflict) return 'success'
      if (this.isStructuredConflict) return 'warning'
      return 'info'
    },
    resolutionTagLabel() {
      if (this.isContentConflict) return '在线编辑'
      if (this.isStructuredConflict) return '结构化策略'
      return '暂不支持'
    },
    detailState() {
      return this.loading ? 'unknown' : 'known'
    },
    blockingReasonText() {
      const reasons = Array.isArray(this.detail.blockingReasons) ? this.detail.blockingReasons : []
      if (!reasons.length) {
        return this.canMerge ? '-' : (this.detail.summary || '-')
      }
      return reasons.join(' / ')
    },
    needTargetPath() {
      return this.editingDraft.resolutionStrategy === 'SET_TARGET_PATH'
    },
    canSaveCurrentDraft() {
      return !!this.normalizedCurrentDraft
    },
    canApplyWithCurrentDraft() {
      return !!this.normalizedCurrentDraft
    },
    normalizedCurrentDraft() {
      if (!this.currentConflict || !this.isStructuredConflict) return null
      const conflictId = this.conflictKey(this.currentConflict)
      const resolutionStrategy = this.editingDraft.resolutionStrategy
      if (!conflictId || !resolutionStrategy) return null
      if (!this.strategyOptions.find(item => item.value === resolutionStrategy)) return null

      const option = {
        conflictId: String(conflictId),
        conflictType: this.currentConflictType,
        resolutionStrategy
      }
      if (resolutionStrategy === 'SET_TARGET_PATH') {
        const targetPath = (this.editingDraft.targetPath || '').trim()
        if (!targetPath) return null
        option.targetPath = targetPath
      }
      return option
    },
    contentDetailReady() {
      if (!this.contentConflictDetail || !this.isContentConflict) return null
      if (String(this.contentConflictDetail.conflictId || '') !== this.currentConflictId) return null
      return this.contentConflictDetail
    },
    contentBlocks() {
      return this.contentDetailReady && Array.isArray(this.contentDetailReady.blocks)
        ? this.contentDetailReady.blocks
        : []
    },
    currentContentDraft() {
      if (this.contentDraftConflictId === this.currentConflictId) {
        return this.contentDraft
      }
      return ''
    },
    canSaveContentConflict() {
      if (!this.contentEditorExpanded || !this.contentDetailReady) return false
      return !!this.currentConflictId && !this.contentConflictApplying
    }
  },
  watch: {
    selectedConflictId: {
      immediate: true,
      handler() {
        this.syncEditingDraft()
        this.resetContentEditorState()
      }
    },
    currentConflictType() {
      this.syncEditingDraft()
      if (!this.isContentConflict) {
        this.resetContentEditorState()
      }
    },
    resolutionMap: {
      deep: true,
      handler() {
        this.syncEditingDraft()
      }
    },
    contentConflictDetail: {
      deep: false,
      handler() {
        this.syncContentDraftFromDetail()
      }
    }
  },
  methods: {
    conflictKey(conflict) {
      return conflict && (conflict.conflictId || conflict.id || conflict.filePath || conflict.path || conflict.oldPath || conflict.newPath)
    },
    conflictType(conflict) {
      return String((conflict && (conflict.conflictType || conflict.type)) || '').toUpperCase()
    },
    resolutionKey(conflict) {
      const id = this.conflictKey(conflict)
      const type = this.conflictType(conflict)
      return `${String(id || '')}::${type}`
    },
    defaultDraftFor(conflict) {
      const type = this.conflictType(conflict)
      const options = STRATEGY_OPTIONS[type] || []
      return {
        conflictId: String(this.conflictKey(conflict) || ''),
        conflictType: type,
        resolutionStrategy: options[0] ? options[0].value : '',
        targetPath: conflict && (conflict.targetPath || conflict.newPath || '') ? String(conflict.targetPath || conflict.newPath || '') : ''
      }
    },
    syncEditingDraft() {
      if (!this.currentConflict) {
        this.syncingDraft = false
        this.editingDraft = {
          conflictId: '',
          conflictType: '',
          resolutionStrategy: '',
          targetPath: ''
        }
        return
      }

      const key = this.resolutionKey(this.currentConflict)
      const fromCache = this.resolutionMap[key]
      const draft = fromCache && typeof fromCache === 'object'
        ? {
            conflictId: String(fromCache.conflictId || this.conflictKey(this.currentConflict) || ''),
            conflictType: this.currentConflictType,
            resolutionStrategy: fromCache.resolutionStrategy || '',
            targetPath: fromCache.targetPath || ''
          }
        : this.defaultDraftFor(this.currentConflict)

      if (!this.strategyOptions.find(item => item.value === draft.resolutionStrategy) && this.strategyOptions.length) {
        draft.resolutionStrategy = this.strategyOptions[0].value
      }

      this.syncingDraft = true
      this.editingDraft = draft
      this.$nextTick(() => {
        this.syncingDraft = false
      })
    },
    handleDraftChange() {
      if (this.syncingDraft) return
      if (!this.currentConflict || !this.isStructuredConflict) return
      const option = this.normalizedCurrentDraft || {
        conflictId: String(this.conflictKey(this.currentConflict) || ''),
        conflictType: this.currentConflictType,
        resolutionStrategy: this.editingDraft.resolutionStrategy || '',
        targetPath: this.editingDraft.targetPath || ''
      }
      this.$emit('resolution-change', {
        key: this.resolutionKey(this.currentConflict),
        option
      })
    },
    emitSaveResolution() {
      if (!this.normalizedCurrentDraft) return
      this.$emit('save-resolution', this.normalizedCurrentDraft)
    },
    emitApplyResolution() {
      if (!this.normalizedCurrentDraft) return
      this.$emit('apply-resolution', this.normalizedCurrentDraft)
    },
    resetContentEditorState() {
      this.contentEditorExpanded = false
      this.contentDraft = ''
      this.contentDraftConflictId = ''
    },
    openContentEditor() {
      if (!this.currentConflict || !this.isContentConflict) return
      this.contentEditorExpanded = true
      this.$emit('load-content-conflict', {
        conflict: this.currentConflict,
        force: false
      })
    },
    refreshContentEditor() {
      if (!this.currentConflict || !this.isContentConflict) return
      this.$emit('load-content-conflict', {
        conflict: this.currentConflict,
        force: true
      })
    },
    initialResolvedContent(detail) {
      if (!detail || typeof detail !== 'object') return ''
      if (typeof detail.resolvedContent === 'string') return detail.resolvedContent
      const blocks = Array.isArray(detail.blocks) ? detail.blocks : []
      if (blocks.length) {
        const merged = blocks.map(block => {
          if (!block || typeof block !== 'object') return ''
          if (typeof block.resolvedContent === 'string') return block.resolvedContent
          if (typeof block.finalContent === 'string') return block.finalContent
          if (typeof block.sourceContent === 'string') return block.sourceContent
          if (typeof block.targetContent === 'string') return block.targetContent
          if (typeof block.baseContent === 'string') return block.baseContent
          if (typeof block.content === 'string') return block.content
          if (Array.isArray(block.sourceLines)) return block.sourceLines.map(item => (item == null ? '' : String(item))).join('\n')
          if (Array.isArray(block.targetLines)) return block.targetLines.map(item => (item == null ? '' : String(item))).join('\n')
          if (Array.isArray(block.baseLines)) return block.baseLines.map(item => (item == null ? '' : String(item))).join('\n')
          return ''
        }).join('')
        if (merged) return merged
      }
      if (typeof detail.sourceContent === 'string') return detail.sourceContent
      if (typeof detail.targetContent === 'string') return detail.targetContent
      if (typeof detail.baseContent === 'string') return detail.baseContent
      return ''
    },
    syncContentDraftFromDetail() {
      if (!this.contentDetailReady) return
      if (this.contentDraftConflictId === this.currentConflictId) return
      this.contentDraft = this.initialResolvedContent(this.contentDetailReady)
      this.contentDraftConflictId = this.currentConflictId
    },
    handleContentDraftInput(value) {
      this.contentDraft = String(value || '')
      this.contentDraftConflictId = this.currentConflictId
    },
    useSourceContent() {
      if (!this.contentDetailReady) return
      this.contentDraft = String(this.contentDetailReady.sourceContent || '')
      this.contentDraftConflictId = this.currentConflictId
    },
    useTargetContent() {
      if (!this.contentDetailReady) return
      this.contentDraft = String(this.contentDetailReady.targetContent || '')
      this.contentDraftConflictId = this.currentConflictId
    },
    emitSaveContentConflict() {
      if (!this.canSaveContentConflict) return
      this.$emit('save-content-conflict', {
        conflict: this.currentConflict,
        conflictId: this.currentConflictId,
        resolvedContent: this.currentContentDraft
      })
    },
    handleClose() {
      this.$emit('close')
    }
  }
}
</script>

<style scoped>
/deep/ .conflict-detail-drawer {
  background: var(--it-surface-solid);
  color: var(--it-text);
}

/deep/ .conflict-detail-drawer .el-drawer__body {
  background: var(--it-surface-solid);
  color: var(--it-text);
}

/deep/ .conflict-detail-drawer .el-card {
  border-color: var(--it-border);
  background: var(--it-surface-solid);
}

/deep/ .conflict-detail-drawer .el-card__header {
  border-bottom-color: var(--it-border);
}

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
  color: var(--it-text);
}

.drawer-subtitle {
  margin-top: 4px;
  color: var(--it-text-subtle);
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
  background: var(--it-surface-muted);
  border: 1px solid var(--it-border);
}

.detail-item.full {
  grid-column: 1 / -1;
}

.detail-label {
  color: var(--it-text-subtle);
  font-size: 12px;
}

.detail-value {
  color: var(--it-text);
  font-size: 13px;
  line-height: 1.7;
  word-break: break-word;
}

.resolution-meta {
  color: var(--it-text);
  font-size: 13px;
}

.resolution-unsupported {
  color: var(--it-text-subtle);
  font-size: 12px;
  line-height: 1.7;
  padding: 4px 0;
}

.content-resolution {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.content-entry-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.content-entry-tip {
  color: var(--it-text-subtle);
  font-size: 12px;
}

.content-editor-shell {
  border: 1px solid var(--it-border);
  border-radius: 12px;
  background: var(--it-surface-muted);
  padding: 12px;
}

.content-editor-hint {
  margin-bottom: 12px;
}

.content-meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.content-compare-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: 1fr;
}

.content-pane {
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
  border-radius: 10px;
  padding: 10px;
}

.content-pane-title {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  color: var(--it-text);
}

.content-final-box {
  margin-top: 10px;
  border: 1px solid var(--it-border-strong);
  background: var(--it-surface-solid);
  border-radius: 10px;
  padding: 10px;
}

.content-final-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  color: var(--it-text);
}

.content-final-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.resolution-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.unresolved-box {
  border: 1px solid var(--it-danger);
  background: var(--it-danger-soft);
  border-radius: 10px;
  padding: 10px 12px;
  margin: 8px 0 12px;
}

.unresolved-title {
  color: var(--it-danger);
  font-size: 12px;
  margin-bottom: 8px;
}

.unresolved-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

@media (max-width: 768px) {
  .drawer-shell {
    padding: 14px;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .content-final-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .resolution-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
