<template>
  <div class="project-merge-conflict-page">
    <div v-if="pageError" class="page-error-shell">
      <el-card shadow="never" class="page-error-card">
        <el-empty :description="pageError" :image-size="92">
          <div class="page-error-actions">
            <el-button type="primary" @click="reloadAll">重试</el-button>
            <el-button plain @click="goBack">返回上一页</el-button>
          </div>
        </el-empty>
      </el-card>
    </div>

    <div v-else-if="pageEmpty" class="page-empty-shell">
      <el-card shadow="never" class="page-empty-card">
        <el-empty description="请先通过审核中心打开一个 MR，或者在 URL 中补充 projectId 与 mergeRequestId" :image-size="92">
          <div class="page-empty-actions">
            <el-button type="primary" @click="goBack">返回审核中心</el-button>
          </div>
        </el-empty>
      </el-card>
    </div>

    <div v-else class="page-shell" v-loading="pageLoading">
      <el-card shadow="never" class="page-hero-card">
        <div class="hero-copy">
          <div class="hero-eyebrow">Merge Request</div>
          <div class="hero-title">冲突处理中心</div>
          <div class="hero-subtitle">
            先看最新 merge-check 摘要，再从左侧挑选冲突项，中间保留后续 Monaco / diff 线程插槽，右侧用于 pre-merge 门禁与合并操作。
          </div>
          <div class="hero-meta">
            <el-tag size="mini" effect="plain">Project #{{ projectId || '-' }}</el-tag>
            <el-tag size="mini" effect="plain">MR #{{ mergeRequestId || '-' }}</el-tag>
            <el-tag size="mini" :type="tabTone" effect="plain">{{ activeTabLabel }}</el-tag>
            <el-tag v-if="latestCheck.checkedAt" size="mini" type="info" effect="plain">
              {{ latestCheck.checkedAt }}
            </el-tag>
          </div>
        </div>

        <div class="hero-actions">
          <el-button size="small" plain @click="goBack">返回审核中心</el-button>
          <el-button size="small" type="primary" :loading="pageLoading" @click="reloadAll">刷新页面</el-button>
        </div>
      </el-card>

      <el-tabs v-model="activeTab" class="page-tabs">
        <el-tab-pane label="总览" name="summary" />
        <el-tab-pane label="冲突列表" name="conflicts" />
        <el-tab-pane label="合并门禁" name="gate" />
      </el-tabs>

      <MergeCheckSummary
        :cards="summaryCards"
        :loading="latestCheckLoading"
        :error-text="latestCheckError"
        :recheck-loading="recheckLoading"
        :status-label="latestStatusLabel"
        :status-type="latestStatusTone"
        :summary-subtitle="summarySubtitle"
        @refresh="reloadAll"
        @recheck="handleRecheck"
      />

      <el-row :gutter="16" class="main-grid">
        <el-col :xs="24" :xl="7">
          <ConflictSidebar
            :conflicts="conflicts"
            :active-conflict-id="selectedConflictId"
            :loading="latestCheckLoading"
            :error-text="latestCheckError"
            :empty-text="conflictEmptyText"
            :subtitle="sidebarSubtitle"
            @select="handleConflictSelect"
          />
        </el-col>

        <el-col :xs="24" :xl="10">
          <el-card shadow="never" class="detail-card" :class="`focus-${activeTab}`">
            <div slot="header" class="detail-header">
              <div class="detail-title-group">
                <div class="detail-title">冲突详情</div>
                <div class="detail-subtitle">内容冲突可在线对比和编辑，其他类型暂不在此线程做专项 UI。</div>
              </div>
              <div class="detail-type-chip">
                <i :class="selectedConflictTypeIcon" class="detail-type-icon"></i>
                <el-tag size="mini" effect="plain" :type="selectedConflictTypeTone">{{ selectedConflictTypeLabel }}</el-tag>
              </div>
            </div>

            <div v-if="detailLoading" class="detail-loading">
              <el-skeleton :rows="6" animated />
            </div>

            <div v-else-if="!selectedConflict" class="detail-empty">
              <el-empty description="请先在左侧选择一个冲突项" :image-size="80">
                <div class="detail-empty-note">后续线程会在这里接入冲突对比和编辑器组件位。</div>
              </el-empty>
            </div>

            <div v-else class="detail-body">
              <div class="detail-info-grid">
                <div class="detail-info-card">
                  <div class="detail-info-label">文件路径</div>
                  <div class="detail-info-value">{{ selectedConflict.displayPath }}</div>
                </div>
                <div class="detail-info-card">
                  <div class="detail-info-label">冲突类型</div>
                  <div class="detail-info-value">{{ selectedConflictTypeLabel }}</div>
                </div>
                <div class="detail-info-card">
                  <div class="detail-info-label">源路径</div>
                  <div class="detail-info-value">{{ selectedConflict.sourcePath || '-' }}</div>
                </div>
                <div class="detail-info-card">
                  <div class="detail-info-label">目标路径</div>
                  <div class="detail-info-value">{{ selectedConflict.targetPath || '-' }}</div>
                </div>
              </div>

              <div class="detail-note-box">
                <div class="detail-note-title">当前摘要</div>
                <div class="detail-note-text">{{ selectedConflict.displaySummary }}</div>
              </div>

              <ConflictDiffEditor
                v-if="isSelectedContentConflict"
                :conflict="selectedConflict"
                :detail="contentConflictDetail"
                :loading="contentConflictLoading"
                :saving="contentConflictSaving"
                :error-text="contentConflictError"
                @refresh="loadSelectedContentConflict(true)"
                @save="handleContentConflictSave"
              />

              <PathConflictPanel
                v-else-if="isSelectedPathConflict || isSelectedStaleBranch"
                :conflict="selectedConflict"
                :related-content-conflict="relatedContentConflict"
                :loading="pathConflictApplying"
                :resolving="pathConflictApplying"
                :error-text="pathConflictError"
                :custom-path-supported="false"
                :can-recheck="true"
                @use-source="handlePathConflictResolve('source')"
                @use-target="handlePathConflictResolve('target')"
                @open-related-content="jumpToRelatedContentConflict"
                @recheck="handleRecheck"
              />

              <div v-else-if="!isSelectedPathConflict" class="detail-slot-box">
                <div class="detail-slot-head">
                  <span>当前类型暂不支持在线内容编辑</span>
                  <el-tag size="mini" effect="plain">非内容冲突</el-tag>
                </div>
                <div class="detail-slot-body">
                  <div class="detail-slot-copy">
                    本线程只处理内容冲突编辑器。重命名、移动、路径占用等结构化冲突不在这里展开专项 UI。
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :xl="7">
          <PreMergeGate
            :merge-request-id="mergeRequestId"
            :loading="preMergeCheckLoading"
            :error-text="preMergeCheckError"
            :mergeable="preMergeCheck.mergeable"
            :reason-text="gateReasonText"
            :blocking-reasons="preMergeCheck.blockingReasons || []"
            :status-label="gateStatusLabel"
            :status-type="gateStatusTone"
            :merge-disabled="mergeDisabled"
            :pre-merge-loading="preMergeCheckLoading"
            :recheck-loading="recheckLoading"
            :merge-loading="mergeLoading"
            @pre-merge-check="handlePreMergeCheck"
            @recheck="handleRecheck"
            @merge="handleMerge"
          />
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import MergeCheckSummary from '@/components/project-merge/MergeCheckSummary.vue'
import ConflictSidebar from '@/components/project-merge/ConflictSidebar.vue'
import ConflictDiffEditor from '@/components/project-merge/ConflictDiffEditor.vue'
import PathConflictPanel from '@/components/project-merge/PathConflictPanel.vue'
import PreMergeGate from '@/components/project-merge/PreMergeGate.vue'
import {
  buildMergeCheckSummaryCards,
  buildPathConflictResolutionOption,
  getConflictPathCandidates,
  getMergeCheckStatusLabel,
  normalizeMergeCheckDetail,
  isPathConflictType,
  sortMergeConflicts
} from '@/utils/projectMergeConflictAdapter'
import {
  getProjectMergeContentConflict,
  getProjectMergeCheckLatest,
  getProjectPreMergeCheck,
  mergeProjectMergeRequest,
  recheckProjectMerge,
  resolveProjectMergeContentConflict,
  resolveProjectMergeConflicts
} from '@/api/projectMergeRequest'

const unwrap = res => {
  const raw = res && Object.prototype.hasOwnProperty.call(res, 'data') ? res.data : res
  return raw && typeof raw === 'object' && Object.prototype.hasOwnProperty.call(raw, 'code') ? raw.data : raw
}

export default {
  name: 'ProjectMergeConflictCenterPage',
  layout: 'project',
  components: {
    ConflictDiffEditor,
    ConflictSidebar,
    PathConflictPanel,
    MergeCheckSummary,
    PreMergeGate
  },
  data() {
    return {
      projectId: '',
      mergeRequestId: '',
      activeTab: 'summary',
      routeSyncing: false,
      pageLoading: false,
      pageError: '',
      latestCheckLoading: false,
      latestCheckError: '',
      preMergeCheckLoading: false,
      preMergeCheckError: '',
      recheckLoading: false,
      mergeLoading: false,
      contentConflictDetail: null,
      contentConflictError: '',
      contentConflictLoadingId: null,
      contentConflictSavingId: null,
      pathConflictApplyingId: null,
      pathConflictError: '',
      latestCheck: {},
      preMergeCheck: {},
      selectedConflictId: ''
    }
  },
  computed: {
    pageEmpty() {
      return !this.projectId || !this.mergeRequestId
    },
    conflicts() {
      return sortMergeConflicts(this.latestCheck.conflicts || [])
    },
    selectedConflict() {
      if (!this.conflicts.length) return null
      return this.conflicts.find(item => String(this.conflictKey(item) || '') === String(this.selectedConflictId || '')) || this.conflicts[0]
    },
    selectedConflictKey() {
      return String(this.conflictKey(this.selectedConflict) || '')
    },
    selectedConflictType() {
      return this.conflictType(this.selectedConflict)
    },
    isSelectedContentConflict() {
      return this.selectedConflictType === 'CONTENT_CONFLICT'
    },
    selectedConflictTypeLabel() {
      return this.selectedConflict ? this.selectedConflict.typeLabel : '冲突'
    },
    selectedConflictTypeTone() {
      return this.selectedConflict ? this.selectedConflict.typeTone : 'info'
    },
    selectedConflictTypeIcon() {
      return this.selectedConflict ? this.selectedConflict.typeIcon : 'el-icon-document'
    },
    isSelectedPathConflict() {
      return isPathConflictType(this.selectedConflictType, this.selectedConflict)
    },
    isSelectedStaleBranch() {
      return this.selectedConflictType === 'STALE_BRANCH'
    },
    relatedContentConflict() {
      if (!this.isSelectedPathConflict || !this.selectedConflict) return null
      const relatedPaths = getConflictPathCandidates(this.selectedConflict)
      if (!relatedPaths.length) return null
      return this.conflicts.find(item => {
        if (!item || this.conflictType(item) !== 'CONTENT_CONFLICT') return false
        const candidates = getConflictPathCandidates(item)
        return candidates.some(path => relatedPaths.includes(path))
      }) || null
    },
    summaryCards() {
      return buildMergeCheckSummaryCards(this.latestCheck)
    },
    summarySubtitle() {
      if (this.latestCheckError) return this.latestCheckError
      if (!this.mergeRequestId) return '请先提供 mergeRequestId'
      if (this.latestCheck.summary) return this.latestCheck.summary
      return '基于最新 merge-check 摘要显示当前状态'
    },
    sidebarSubtitle() {
      if (this.conflicts.length) {
        return `共 ${this.conflicts.length} 项，按内容、路径、重命名、移动、过期分支分类`
      }
      return '目前没有结构化冲突，或者最新检查尚未返回冲突列表'
    },
    conflictEmptyText() {
      return this.latestCheckError ? '无法获取冲突列表' : '当前没有冲突项'
    },
    latestStatusLabel() {
      return getMergeCheckStatusLabel(this.latestCheck)
    },
    latestStatusTone() {
      if (this.latestCheck.mergeable === true) return 'success'
      if (this.latestCheck.mergeable === false) return 'warning'
      return this.latestCheck.conflictCount ? 'danger' : 'info'
    },
    gateStatusLabel() {
      if (this.preMergeCheckError) return '门禁异常'
      if (this.preMergeCheck.mergeable === true) return '可合并'
      if (this.preMergeCheck.mergeable === false) return '受阻'
      return '待检查'
    },
    gateStatusTone() {
      if (this.preMergeCheckError) return 'danger'
      if (this.preMergeCheck.mergeable === true) return 'success'
      if (this.preMergeCheck.mergeable === false) return 'warning'
      return 'info'
    },
    gateReasonText() {
      if (this.preMergeCheckError) return this.preMergeCheckError
      if (this.preMergeCheck.suggestedAction) return this.preMergeCheck.suggestedAction
      if (this.preMergeCheck.summary) return this.preMergeCheck.summary
      if (this.preMergeCheck.mergeable === true) return '预合并检查通过'
      if (this.preMergeCheck.mergeable === false) return '请先处理阻塞项'
      return '等待预合并检查结果'
    },
    mergeDisabled() {
      return !!(this.preMergeCheckError || this.mergeLoading || this.preMergeCheckLoading || this.latestCheckLoading || !this.mergeRequestId || this.preMergeCheck.mergeable !== true)
    },
    activeTabLabel() {
      const map = {
        summary: '总览',
        conflicts: '冲突列表',
        gate: '合并门禁'
      }
      return map[this.activeTab] || '总览'
    },
    tabTone() {
      return {
        summary: 'info',
        conflicts: 'warning',
        gate: 'success'
      }[this.activeTab] || 'info'
    },
    detailLoading() {
      return this.latestCheckLoading && !this.selectedConflict
    },
    contentConflictLoading() {
      return String(this.contentConflictLoadingId || '') === String(this.selectedConflictKey || '')
    },
    contentConflictSaving() {
      return String(this.contentConflictSavingId || '') === String(this.selectedConflictKey || '')
    },
    pathConflictApplying() {
      return String(this.pathConflictApplyingId || '') === String(this.selectedConflictKey || '')
    }
  },
  watch: {
    '$route.query': {
      immediate: true,
      handler(query) {
        this.applyRouteState(query || {})
      }
    },
    activeTab(val) {
      if (this.routeSyncing) return
      this.syncRouteState({ tab: val })
    },
    selectedConflictId() {
      this.clearContentConflictState()
      this.pathConflictError = ''
      this.$nextTick(() => {
        this.loadSelectedContentConflict(false)
      })
    }
  },
  methods: {
    applyRouteState(query = {}) {
      this.routeSyncing = true
      this.projectId = this.normalizeId(query.projectId)
      this.mergeRequestId = this.normalizeId(query.mergeRequestId)
      this.activeTab = this.normalizeTab(query.tab)
      if (query.conflictId) {
        this.selectedConflictId = String(query.conflictId)
      }
      this.$nextTick(() => {
        this.routeSyncing = false
      })
      if (this.projectId && this.mergeRequestId) {
        this.reloadAll()
      } else {
        this.pageLoading = false
        this.latestCheck = {}
        this.preMergeCheck = {}
        this.latestCheckError = ''
        this.preMergeCheckError = ''
        this.pathConflictError = ''
        this.pathConflictApplyingId = null
      }
    },
    normalizeId(value) {
      const raw = String(value || '').trim()
      return raw ? raw : ''
    },
    normalizeTab(value) {
      const raw = String(value || 'summary')
      return ['summary', 'conflicts', 'gate'].includes(raw) ? raw : 'summary'
    },
    syncRouteState(extra = {}) {
      if (!this.projectId) return
      const query = {
        ...this.$route.query,
        projectId: String(this.projectId)
      }
      if (this.mergeRequestId) {
        query.mergeRequestId = String(this.mergeRequestId)
      }
      query.tab = this.activeTab
      Object.keys(extra || {}).forEach(key => {
        const value = extra[key]
        if (value === undefined || value === null || value === '') {
          delete query[key]
        } else {
          query[key] = String(value)
        }
      })
      this.routeSyncing = true
      this.$router.replace({ path: this.$route.path, query }).catch(() => {}).finally(() => {
        this.routeSyncing = false
      })
    },
    async reloadAll() {
      if (!this.projectId || !this.mergeRequestId) return
      this.pageLoading = true
      this.latestCheckLoading = true
      this.preMergeCheckLoading = true
      this.pageError = ''
      this.latestCheckError = ''
      this.preMergeCheckError = ''

      try {
        const [latestResult, preMergeResult] = await Promise.allSettled([
          getProjectMergeCheckLatest(this.mergeRequestId),
          getProjectPreMergeCheck(this.mergeRequestId)
        ])

        if (latestResult.status !== 'fulfilled') {
          throw latestResult.reason
        }

        this.latestCheck = normalizeMergeCheckDetail(latestResult.value, {
          projectId: this.projectId,
          mergeRequestId: this.mergeRequestId
        })

        if (preMergeResult.status === 'fulfilled') {
          this.preMergeCheck = normalizeMergeCheckDetail(preMergeResult.value, {
            projectId: this.projectId,
            mergeRequestId: this.mergeRequestId
          })
        } else {
          this.preMergeCheck = {}
          this.preMergeCheckError = this.extractErrorMessage(preMergeResult.reason, '预合并检查失败')
        }

        if (!this.selectedConflictId) {
          this.selectedConflictId = this.conflicts.length ? String(this.conflictKey(this.conflicts[0])) : ''
        } else if (this.conflicts.length && !this.conflicts.some(item => String(this.conflictKey(item)) === String(this.selectedConflictId))) {
          this.selectedConflictId = String(this.conflictKey(this.conflicts[0]))
        }
        this.clearContentConflictState()
        this.$nextTick(() => {
          this.loadSelectedContentConflict(false)
        })
      } catch (error) {
        this.pageError = this.extractErrorMessage(error, '冲突处理中心加载失败')
      } finally {
        this.pageLoading = false
        this.latestCheckLoading = false
        this.preMergeCheckLoading = false
      }
    },
    async handleRecheck() {
      if (!this.mergeRequestId || this.recheckLoading) return
      this.recheckLoading = true
      this.pageError = ''
      try {
        const latestResult = await recheckProjectMerge(this.mergeRequestId)
        this.latestCheck = normalizeMergeCheckDetail(latestResult, {
          projectId: this.projectId,
          mergeRequestId: this.mergeRequestId
        })
        await this.handlePreMergeCheck({ silent: true })
        if (!this.selectedConflictId && this.conflicts.length) {
          this.selectedConflictId = String(this.conflictKey(this.conflicts[0]))
        } else if (this.selectedConflictId && !this.conflicts.some(item => String(this.conflictKey(item)) === String(this.selectedConflictId))) {
          this.selectedConflictId = this.conflicts.length ? String(this.conflictKey(this.conflicts[0])) : ''
        }
        this.clearContentConflictState()
        this.$nextTick(() => {
          this.loadSelectedContentConflict(true)
        })
        this.syncRouteState()
        this.$message.success('已重新检查合并状态')
      } catch (error) {
        this.$message.error(this.extractErrorMessage(error, '重新检查失败'))
      } finally {
        this.recheckLoading = false
      }
    },
    async handlePreMergeCheck(options = {}) {
      if (!this.mergeRequestId || this.preMergeCheckLoading) return
      const silent = !!(options && options.silent)
      this.preMergeCheckLoading = true
      this.preMergeCheckError = ''
      try {
        const result = await getProjectPreMergeCheck(this.mergeRequestId)
        this.preMergeCheck = normalizeMergeCheckDetail(result, {
          projectId: this.projectId,
          mergeRequestId: this.mergeRequestId
        })
        this.syncRouteState()
        if (!silent) {
          this.$message.success('预合并检查已完成')
        }
      } catch (error) {
        this.preMergeCheckError = this.extractErrorMessage(error, '预合并检查失败')
        if (!silent) {
          this.$message.error(this.preMergeCheckError)
        }
      } finally {
        this.preMergeCheckLoading = false
      }
    },
    async handleMerge() {
      if (!this.mergeRequestId || this.mergeLoading) return
      if (this.preMergeCheck.mergeable !== true) {
        await this.handlePreMergeCheck({ silent: true })
      }
      if (this.preMergeCheck.mergeable !== true) {
        this.$message.warning(this.gateReasonText || '请先通过预合并检查')
        this.activeTab = 'gate'
        return
      }

      try {
        await this.$confirm('确认继续合并当前 MR？', '合并确认', {
          type: 'warning'
        })
      } catch (error) {
        return
      }

      this.mergeLoading = true
      try {
        await mergeProjectMergeRequest(this.mergeRequestId)
        this.$message.success('MR 已合并')
        await this.reloadAll()
      } catch (error) {
        this.$message.error(this.extractErrorMessage(error, 'MR 合并失败'))
      } finally {
        this.mergeLoading = false
      }
    },
    handleConflictSelect(conflict) {
      if (!conflict) return
      this.selectedConflictId = String(this.conflictKey(conflict) || '')
      this.activeTab = 'conflicts'
      this.pathConflictError = ''
      this.syncRouteState({
        conflictId: this.selectedConflictId,
        tab: this.activeTab
      })
    },
    jumpToRelatedContentConflict() {
      if (!this.relatedContentConflict) return
      this.selectedConflictId = String(this.conflictKey(this.relatedContentConflict) || '')
      this.activeTab = 'conflicts'
      this.syncRouteState({
        conflictId: this.selectedConflictId,
        tab: this.activeTab
      })
    },
    async handlePathConflictResolve(choice) {
      if (!this.isSelectedPathConflict || !this.selectedConflict || !this.mergeRequestId) return
      if (this.pathConflictApplying) return

      const option = buildPathConflictResolutionOption(this.selectedConflict, choice)
      if (!option) {
        this.$message.warning('当前路径冲突不支持该处理方式')
        return
      }

      this.pathConflictApplyingId = option.conflictId
      this.pathConflictError = ''
      try {
        const result = unwrap(await resolveProjectMergeConflicts(this.mergeRequestId, { options: [option] })) || {}
        const unresolvedIds = Array.isArray(result.unresolvedConflictIds) ? result.unresolvedConflictIds.map(String) : []
        await this.reloadAll()
        this.selectConflictAfterUpdate(option.conflictId, unresolvedIds)
        this.syncRouteState({
          conflictId: this.selectedConflictId,
          tab: 'conflicts'
        })
        if (result.resolved === false && unresolvedIds.length) {
          this.$message.warning(`仍有 ${unresolvedIds.length} 个冲突未解决`)
        } else if (result.supplementalCommitId) {
          this.$message.success(`路径冲突已应用（提交 ${result.supplementalCommitId}）`)
        } else {
          this.$message.success('路径冲突已应用')
        }
      } catch (error) {
        this.pathConflictError = this.extractErrorMessage(error, '应用路径冲突处理方式失败')
        this.$message.error(this.pathConflictError)
      } finally {
        if (String(this.pathConflictApplyingId || '') === String(option.conflictId || '')) {
          this.pathConflictApplyingId = null
        }
      }
    },
    conflictKey(item) {
      if (!item) return ''
      return item.conflictId || item.id || item.filePath || item.path || item.oldPath || item.newPath || ''
    },
    conflictType(item) {
      return String((item && (item.conflictType || item.type)) || '').toUpperCase()
    },
    normalizeTextValue(value) {
      return typeof value === 'string' ? value : ''
    },
    hasTextValue(value) {
      return typeof value === 'string'
    },
    joinBlockLines(lines) {
      return Array.isArray(lines) ? lines.map(item => (item == null ? '' : String(item))).join('\n') : ''
    },
    buildResolvedContentFromBlocks(blocks, fallbackText = '') {
      const rows = Array.isArray(blocks) ? blocks : []
      if (!rows.length) return fallbackText
      const merged = rows.map(block => {
        if (!block || typeof block !== 'object') return ''
        if (typeof block.resolvedContent === 'string') return block.resolvedContent
        if (typeof block.finalContent === 'string') return block.finalContent
        if (typeof block.sourceContent === 'string') return block.sourceContent
        if (typeof block.targetContent === 'string') return block.targetContent
        if (typeof block.baseContent === 'string') return block.baseContent
        if (typeof block.content === 'string') return block.content
        if (Array.isArray(block.sourceLines)) return this.joinBlockLines(block.sourceLines)
        if (Array.isArray(block.targetLines)) return this.joinBlockLines(block.targetLines)
        if (Array.isArray(block.baseLines)) return this.joinBlockLines(block.baseLines)
        return ''
      }).join('')
      return merged || fallbackText
    },
    normalizeContentConflictDetail(payload, conflict) {
      const source = payload && typeof payload === 'object' ? payload : {}
      const metadata = source.metadata && typeof source.metadata === 'object' ? { ...source.metadata } : {}
      const conflictId = String(source.conflictId || this.conflictKey(conflict) || '')
      const baseContent = this.normalizeTextValue(source.baseContent)
      const sourceContent = this.normalizeTextValue(source.sourceContent)
      const targetContent = this.normalizeTextValue(source.targetContent)
      const blocks = Array.isArray(source.blocks)
        ? source.blocks.map(item => ({ ...(item && typeof item === 'object' ? item : {}) }))
        : []
      const fallbackResolved = this.hasTextValue(source.sourceContent)
        ? sourceContent
        : this.hasTextValue(source.targetContent)
          ? targetContent
          : this.hasTextValue(source.baseContent)
            ? baseContent
            : ''
      const resolvedContent = typeof source.resolvedContent === 'string'
        ? source.resolvedContent
        : this.buildResolvedContentFromBlocks(blocks, fallbackResolved)

      if (!metadata.filePath) {
        metadata.filePath = (conflict && (conflict.filePath || conflict.path || conflict.sourcePath || conflict.targetPath || conflict.basePath)) || ''
      }
      if (source.binary || source.isBinary || source.binaryFile) {
        metadata.binary = true
      }
      if (!metadata.encoding && source.encoding) {
        metadata.encoding = source.encoding
      }

      return {
        conflictId,
        baseContent,
        sourceContent,
        targetContent,
        blocks,
        metadata,
        resolvedContent
      }
    },
    clearContentConflictState() {
      this.contentConflictDetail = null
      this.contentConflictError = ''
      this.contentConflictLoadingId = null
    },
    async loadSelectedContentConflict(force = false) {
      if (!this.isSelectedContentConflict || !this.selectedConflict || !this.mergeRequestId) return
      await this.loadContentConflict(this.selectedConflict, force)
    },
    async loadContentConflict(conflict, force = false) {
      const conflictId = String(this.conflictKey(conflict) || '').trim()
      if (!conflictId || !this.mergeRequestId) return
      if (!force && this.contentConflictDetail && String(this.contentConflictDetail.conflictId || '') === conflictId) return

      this.contentConflictError = ''
      this.contentConflictLoadingId = conflictId
      try {
        const result = unwrap(await getProjectMergeContentConflict(this.mergeRequestId, conflictId))
        this.contentConflictDetail = this.normalizeContentConflictDetail(result, conflict)
      } catch (error) {
        this.contentConflictError = this.extractErrorMessage(error, '加载内容冲突详情失败')
      } finally {
        if (String(this.contentConflictLoadingId || '') === conflictId) {
          this.contentConflictLoadingId = null
        }
      }
    },
    async handleContentConflictSave(payload) {
      const conflict = payload && payload.conflict ? payload.conflict : this.selectedConflict
      const conflictId = String((payload && payload.conflictId) || this.conflictKey(conflict) || '').trim()
      if (!this.mergeRequestId || !conflictId || this.contentConflictSaving) return

      this.contentConflictSavingId = conflictId
      try {
        const result = unwrap(await resolveProjectMergeContentConflict(this.mergeRequestId, {
          conflictId,
          resolvedContent: this.normalizeTextValue(payload && payload.resolvedContent)
        })) || {}
        const unresolvedIds = Array.isArray(result.unresolvedConflictIds) ? result.unresolvedConflictIds.map(String) : []
        await this.reloadAll()
        this.selectConflictAfterUpdate(conflictId, unresolvedIds)
        this.syncRouteState({
          conflictId: this.selectedConflictId,
          tab: 'conflicts'
        })
        if (this.isSelectedContentConflict) {
          await this.loadSelectedContentConflict(true)
        }
        if (result.supplementalCommitId) {
          this.$message.success(`内容冲突已保存（提交 ${result.supplementalCommitId}）`)
        } else {
          this.$message.success('内容冲突已保存')
        }
      } catch (error) {
        this.contentConflictError = this.extractErrorMessage(error, '保存内容冲突失败')
        this.$message.error(this.contentConflictError)
      } finally {
        if (String(this.contentConflictSavingId || '') === conflictId) {
          this.contentConflictSavingId = null
        }
      }
    },
    selectConflictAfterUpdate(preferredConflictId, unresolvedIds = []) {
      const conflicts = Array.isArray(this.conflicts) ? this.conflicts : []
      if (!conflicts.length) {
        this.selectedConflictId = ''
        return
      }
      const unresolvedSet = new Set((Array.isArray(unresolvedIds) ? unresolvedIds : []).map(String))
      const unresolvedFirst = conflicts.find(item => unresolvedSet.has(String(this.conflictKey(item))))
      const preferred = conflicts.find(item => String(this.conflictKey(item)) === String(preferredConflictId || ''))
      this.selectedConflictId = String(this.conflictKey(unresolvedFirst || preferred || conflicts[0]) || '')
    },
    extractErrorMessage(error, fallback = '') {
      const responseData = error && error.response && error.response.data
      const message = (responseData && (responseData.msg || responseData.message || responseData.error)) ||
        (error && error.message) ||
        fallback
      return String(message || fallback || '')
    },
    goBack() {
      if (!this.projectId) {
        this.$router.push('/projectmanage')
        return
      }
      this.$router.push({
        path: '/projectmanage',
        query: {
          projectId: String(this.projectId),
          tab: this.$route.query.fromTab || 'audit-manage'
        }
      })
    }
  }
}
</script>

<style scoped>
.project-merge-conflict-page {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-hero-card,
.page-error-card,
.page-empty-card,
.detail-card {
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid #e5ecf6;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
}

.page-hero-card {
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
}

.page-error-shell,
.page-empty-shell {
  max-width: 1080px;
}

.page-error-actions,
.page-empty-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hero-eyebrow {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #2563eb;
  font-weight: 700;
}

.hero-title {
  font-size: 26px;
  line-height: 1.35;
  color: #0f172a;
  font-weight: 700;
}

.hero-subtitle {
  max-width: 980px;
  font-size: 13px;
  line-height: 1.8;
  color: #64748b;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-actions {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
  margin-top: 10px;
}

.page-tabs {
  margin-top: -4px;
}

.main-grid {
  align-items: stretch;
}

.detail-card {
  height: 100%;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.detail-type-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.detail-type-icon {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  flex: 0 0 auto;
}

.detail-title-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.detail-subtitle {
  font-size: 12px;
  line-height: 1.7;
  color: #64748b;
}

.detail-loading,
.detail-empty {
  min-height: 420px;
}

.detail-empty-note {
  margin-top: 8px;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.7;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.detail-info-card,
.detail-note-box,
.detail-slot-box {
  border-radius: 14px;
  border: 1px solid #dbe7f7;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  padding: 14px;
}

.detail-info-label {
  font-size: 12px;
  color: #64748b;
}

.detail-info-value {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #0f172a;
  word-break: break-word;
}

.detail-note-title {
  font-size: 12px;
  font-weight: 700;
  color: #1f2937;
}

.detail-note-text {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.8;
  color: #475569;
}

.detail-slot-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #1f2937;
}

.detail-slot-body {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-slot-copy {
  font-size: 12px;
  line-height: 1.8;
  color: #64748b;
}

.detail-slot-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.focus-summary {
  box-shadow: 0 14px 34px rgba(37, 99, 235, 0.08);
}

.focus-conflicts {
  box-shadow: 0 14px 34px rgba(245, 158, 11, 0.08);
}

.focus-gate {
  box-shadow: 0 14px 34px rgba(16, 185, 129, 0.08);
}

@media (max-width: 1200px) {
  .detail-info-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .project-merge-conflict-page {
    padding: 16px;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .hero-title {
    font-size: 22px;
  }
}
</style>
