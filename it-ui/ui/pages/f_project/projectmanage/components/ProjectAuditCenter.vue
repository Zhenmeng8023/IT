<template>
  <div class="audit">
    <el-row :gutter="16">
      <el-col :xs="24" :lg="8">
        <el-card shadow="never">
          <div slot="header">分支保护</div>
          <el-table :data="branchList" border size="small" v-loading="branchLoading">
            <el-table-column prop="name" label="分支" min-width="120" />
            <el-table-column label="受保护" width="96">
              <template slot-scope="{ row }">
                <el-switch
                  :value="!!row.protectedFlag"
                  :disabled="!canManageProject || branchSavingId === row.id"
                  @change="value => updateBranchProtection(row, { protectedFlag: value })"
                />
              </template>
            </el-table-column>
            <el-table-column label="允许直提" width="112">
              <template slot-scope="{ row }">
                <el-switch
                  :value="!!row.allowDirectCommitFlag"
                  :disabled="!canManageProject || branchSavingId === row.id"
                  @change="value => updateBranchProtection(row, { allowDirectCommitFlag: value })"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template slot-scope="{ row }">
                <el-button
                  size="mini"
                  type="danger"
                  plain
                  :disabled="!canDeleteBranch(row)"
                  :loading="branchDeletingId === row.id"
                  @click="deleteBranch(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="16">
        <el-card shadow="never">
          <div slot="header" class="toolbar">
            <span>审核中心</span>
            <div class="toolbar-actions">
              <el-select
                v-model="status"
                size="small"
                clearable
                placeholder="状态"
                @change="loadMergeRequests"
              >
                <el-option label="进行中" value="open" />
                <el-option label="已合并" value="merged" />
                <el-option label="已关闭" value="closed" />
              </el-select>
              <el-button size="small" @click="refreshAll">刷新</el-button>
              <el-button
                v-if="canManageProject"
                size="small"
                type="primary"
                @click="openCreateDialog"
              >
                新建 MR
              </el-button>
            </div>
          </div>

          <el-table :data="mergeRequests" border size="small" v-loading="loading">
            <el-table-column label="标题" min-width="240">
              <template slot-scope="{ row }">
                <div>{{ row.title || '未命名 MR' }}</div>
                <div class="minor">
                  <el-tag size="mini" effect="plain">{{ branchName(row.sourceBranchId) }}</el-tag>
                  <span>-></span>
                  <el-tag size="mini" type="success" effect="plain">{{ branchName(row.targetBranchId) }}</el-tag>
                  <ConflictBadge
                    :has-conflict="hasConflict(row)"
                    :conflict-count="getConflictCount(row)"
                    :state="mergeCheckState(row)"
                    @click="openConflictDrawer(row, true)"
                  />
                </div>
              </template>
            </el-table-column>

            <el-table-column label="评审" width="120">
              <template slot-scope="{ row }">
                <div class="minor">通过 {{ approvalCount(row) }}</div>
                <div class="minor">拒绝 {{ rejectCount(row) }}</div>
              </template>
            </el-table-column>

            <el-table-column label="检查" width="220">
              <template slot-scope="{ row }">
                <div class="minor">当前 {{ checkCount(row) }}</div>
                <div class="minor">{{ failedCheckCount(row) ? `失败 ${failedCheckCount(row)}` : '通过' }}</div>
              </template>
            </el-table-column>

            <el-table-column prop="status" label="状态" width="100">
              <template slot-scope="{ row }">
                <el-tag size="mini" :type="mrStatusType(row.status)">{{ mrStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>

            <el-table-column label="操作" min-width="280" fixed="right">
              <template slot-scope="{ row }">
                <el-button size="mini" @click="openReviewDialog(row)">评审</el-button>
                <el-button size="mini" type="warning" plain @click="openCheckDialog(row)">记录检查</el-button>
                <el-button size="mini" plain @click="openConflictDrawer(row, true)">查看门禁明细</el-button>
                <el-button size="mini" type="primary" plain @click="openConflictCenter(row)">进入冲突处理中心</el-button>
                <el-button
                  size="mini"
                  type="success"
                  :loading="mergeLoadingId === row.id"
                  :disabled="!!mergeDisabledReason(row)"
                  @click="mergeRow(row)"
                >
                  合并 MR
                </el-button>
                <div v-if="mergeDisabledReason(row)" class="minor">{{ mergeDisabledReason(row) }}</div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog title="新建 MR" :visible.sync="createDialogVisible" width="520px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="源分支">
          <el-select v-model="createForm.sourceBranchId" style="width:100%">
            <el-option
              v-for="item in branchList"
              :key="'src-' + item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标分支">
          <el-select v-model="createForm.targetBranchId" style="width:100%">
            <el-option
              v-for="item in branchList"
              :key="'target-' + item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="createForm.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitCreate">创建</el-button>
      </span>
    </el-dialog>

    <el-dialog title="评审 MR" :visible.sync="reviewDialogVisible" width="460px">
      <el-form :model="reviewForm" label-width="90px">
        <el-form-item label="结论">
          <el-select v-model="reviewForm.reviewResult" style="width:100%">
            <el-option label="通过" value="approve" />
            <el-option label="评论" value="comment" />
            <el-option label="拒绝" value="reject" />
          </el-select>
        </el-form-item>
        <el-form-item label="评论">
          <el-input v-model="reviewForm.reviewComment" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewLoading" @click="submitReview">提交</el-button>
      </span>
    </el-dialog>

    <el-dialog title="记录检查" :visible.sync="checkDialogVisible" width="460px">
      <el-form :model="checkForm" label-width="90px">
        <el-form-item label="类型">
          <el-select v-model="checkForm.checkType" style="width:100%">
            <el-option label="自定义" value="custom" />
            <el-option label="CI" value="ci" />
            <el-option label="安全" value="security" />
            <el-option label="发布" value="release" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="checkForm.checkStatus" style="width:100%">
            <el-option label="成功" value="success" />
            <el-option label="失败" value="failed" />
            <el-option label="运行中" value="running" />
            <el-option label="排队中" value="queued" />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="checkForm.summary" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="checkDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="checkLoading" @click="submitCheck">提交</el-button>
      </span>
    </el-dialog>

    <ConflictDetailDrawer
      :visible.sync="conflictDrawerVisible"
      :loading="conflictDrawerLoading"
      :detail="conflictDetail"
      :conflicts="conflictDetail.conflicts || []"
      :selected-conflict-id="selectedConflictId"
      :can-merge="!mergeDisabledReason(conflictTargetRow || conflictDetail)"
      :can-recheck="!!(conflictTargetRow && conflictTargetRow.id)"
      :merge-disabled-reason="mergeDisabledReason(conflictTargetRow || conflictDetail)"
      :merge-loading="mergeLoadingId === (conflictTargetRow && conflictTargetRow.id)"
      :recheck-loading="recheckLoadingId === (conflictTargetRow && conflictTargetRow.id)"
      :resolution-map="currentConflictResolutionMap"
      :save-resolution-loading="saveResolutionLoading"
      :apply-resolution-loading="applyResolutionLoading"
      :unresolved-conflict-ids="unresolvedConflictIds"
      :content-conflict-detail="contentConflictDetail"
      :content-conflict-loading="contentConflictLoading"
      :content-conflict-applying="contentConflictApplying"
      @select-conflict="handleConflictSelect"
      @refresh="openConflictDrawer(conflictTargetRow, true)"
      @recheck="handleConflictRecheck"
      @update-source="handleConflictSyncSource"
      @open-conflict-center="handleDrawerOpenConflictCenter"
      @merge="mergeRow(conflictTargetRow)"
      @resolution-change="handleConflictResolutionChange"
      @save-resolution="handleConflictResolutionSave"
      @apply-resolution="handleConflictResolutionApply"
      @load-content-conflict="handleLoadContentConflict"
      @save-content-conflict="handleSaveContentConflict"
    />
  </div>
</template>
<script>
import ConflictBadge from './ConflictBadge.vue'
import ConflictDetailDrawer from './ConflictDetailDrawer.vue'
import { deleteProjectBranch, listProjectBranches, protectProjectBranch } from '@/api/projectBranch'
import {
  createProjectMergeRequest,
  getProjectMergeContentConflict,
  getProjectMergeCheckLatest,
  getProjectPreMergeCheck,
  listProjectMergeRequests,
  mergeProjectMergeRequest,
  recheckProjectMerge,
  resolveProjectMergeContentConflict,
  resolveProjectMergeConflicts,
  reviewProjectMergeRequest,
  runProjectCheck
} from '@/api/projectMergeRequest'

const STRUCTURED_CONFLICT_TYPES = new Set([
  'STALE_BRANCH',
  'DELETE_MODIFY_CONFLICT',
  'RENAME_CONFLICT',
  'MOVE_CONFLICT',
  'TARGET_PATH_OCCUPIED'
])

const CONFLICT_TYPE_STRATEGIES = {
  STALE_BRANCH: new Set(['SYNC_SOURCE_WITH_TARGET']),
  DELETE_MODIFY_CONFLICT: new Set(['KEEP_SOURCE', 'KEEP_TARGET']),
  RENAME_CONFLICT: new Set(['USE_SOURCE_PATH', 'USE_TARGET_PATH', 'SET_TARGET_PATH']),
  MOVE_CONFLICT: new Set(['USE_SOURCE_PATH', 'USE_TARGET_PATH', 'SET_TARGET_PATH']),
  TARGET_PATH_OCCUPIED: new Set(['KEEP_SOURCE', 'KEEP_TARGET', 'SET_TARGET_PATH'])
}

const unwrap = res => {
  const raw = res && Object.prototype.hasOwnProperty.call(res, 'data') ? res.data : res
  return raw && typeof raw === 'object' && Object.prototype.hasOwnProperty.call(raw, 'code') ? raw.data : raw
}

export default {
  name: 'ProjectAuditCenter',
  components: {
    ConflictBadge,
    ConflictDetailDrawer
  },
  props: {
    projectId: {
      type: [String, Number],
      required: true
    },
    canManageProject: {
      type: Boolean,
      default: true
    },
    defaultBranchId: {
      type: [String, Number],
      default: null
    }
  },
  data() {
    return {
      loading: false,
      branchLoading: false,
      createLoading: false,
      reviewLoading: false,
        checkLoading: false,
        mergeLoadingId: null,
        recheckLoadingId: null,
        branchSavingId: null,
        branchDeletingId: null,
        status: 'open',
      branchList: [],
      mergeRequests: [],
      mergeRequestConflictCache: {},
      mergeCheckLoadingIds: {},
      conflictDrawerVisible: false,
      conflictDrawerLoading: false,
      conflictTargetRow: null,
      conflictDetail: {},
      selectedConflictId: null,
      conflictResolutionDraftsByMr: {},
      unresolvedConflictIds: [],
      saveResolutionLoadingId: null,
      applyResolutionLoadingId: null,
      contentConflictDetail: null,
      contentConflictLoadingId: null,
      contentConflictApplyingId: null,
      createDialogVisible: false,
      reviewDialogVisible: false,
      checkDialogVisible: false,
      reviewTarget: null,
      checkTarget: null,
      createForm: {
        sourceBranchId: null,
        targetBranchId: null,
        title: '',
        description: ''
      },
      reviewForm: {
        reviewResult: 'approve',
        reviewComment: ''
      },
      checkForm: {
        checkType: 'custom',
        checkStatus: 'success',
        summary: ''
      }
    }
  },
  computed: {
    protectedBranchCount() {
      return this.branchList.filter(item => !!item.protectedFlag).length
    },
    openMrCount() {
      return this.mergeRequests.filter(item => item.status === 'open').length
    },
    failedMrCount() {
      return this.mergeRequests.filter(item => !!this.mergeDisabledReason(item)).length
    },
    currentConflictResolutionMap() {
      const key = this.cacheKey(this.conflictTargetRow)
      return this.conflictResolutionDraftsByMr[key] || {}
    },
    saveResolutionLoading() {
      return this.saveResolutionLoadingId === (this.conflictTargetRow && this.conflictTargetRow.id)
    },
    applyResolutionLoading() {
      return this.applyResolutionLoadingId === (this.conflictTargetRow && this.conflictTargetRow.id)
    },
    contentConflictLoading() {
      return String(this.contentConflictLoadingId || '') === String(this.selectedConflictId || '')
    },
    contentConflictApplying() {
      return String(this.contentConflictApplyingId || '') === String(this.selectedConflictId || '')
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler() {
        this.mergeRequestConflictCache = {}
        this.mergeCheckLoadingIds = {}
        this.conflictTargetRow = null
        this.conflictDetail = {}
        this.selectedConflictId = null
        this.conflictResolutionDraftsByMr = {}
        this.unresolvedConflictIds = []
        this.contentConflictDetail = null
        this.contentConflictLoadingId = null
        this.contentConflictApplyingId = null
        if (this.projectId) {
          this.refreshAll()
        }
      }
    }
  },
  methods: {
    cacheKey(value) {
      return String(value && value.id ? value.id : value || '')
    },
    conflictKey(conflict) {
      if (!conflict) return null
      return conflict.conflictId || conflict.id || conflict.filePath || conflict.path || null
    },
    conflictType(conflict) {
      return String((conflict && (conflict.conflictType || conflict.type)) || '').toUpperCase()
    },
    conflictResolutionKeyByParts(conflictId, conflictType) {
      return `${String(conflictId || '')}::${String(conflictType || '').toUpperCase()}`
    },
    conflictResolutionKey(conflict) {
      return this.conflictResolutionKeyByParts(this.conflictKey(conflict), this.conflictType(conflict))
    },
    isStructuredConflictType(conflictType) {
      return STRUCTURED_CONFLICT_TYPES.has(String(conflictType || '').toUpperCase())
    },
    isStrategyAllowed(conflictType, strategy) {
      const key = String(conflictType || '').toUpperCase()
      const allowed = CONFLICT_TYPE_STRATEGIES[key]
      if (!allowed) return false
      return allowed.has(String(strategy || '').toUpperCase())
    },
    normalizeResolutionOption(option) {
      if (!option || typeof option !== 'object') return null
      const conflictId = String(option.conflictId || '').trim()
      const conflictType = String(option.conflictType || '').toUpperCase()
      const resolutionStrategy = String(option.resolutionStrategy || '').toUpperCase()
      if (!conflictId || !this.isStructuredConflictType(conflictType)) return null
      if (!this.isStrategyAllowed(conflictType, resolutionStrategy)) return null

      const normalized = { conflictId, conflictType, resolutionStrategy }
      if (resolutionStrategy === 'SET_TARGET_PATH') {
        const targetPath = String(option.targetPath || '').trim()
        if (!targetPath) return null
        normalized.targetPath = targetPath
      }
      return normalized
    },
    isSameResolutionOption(left, right) {
      if (!left || !right) return false
      return String(left.conflictId || '') === String(right.conflictId || '') &&
        String(left.conflictType || '').toUpperCase() === String(right.conflictType || '').toUpperCase() &&
        String(left.resolutionStrategy || '').toUpperCase() === String(right.resolutionStrategy || '').toUpperCase() &&
        String(left.targetPath || '') === String(right.targetPath || '')
    },
    setResolutionDraft(option) {
      const mrKey = this.cacheKey(this.conflictTargetRow)
      if (!mrKey) return false

      const normalized = this.normalizeResolutionOption(option)
      if (!normalized) return false
      const entryKey = this.conflictResolutionKeyByParts(normalized.conflictId, normalized.conflictType)
      const bucket = { ...(this.conflictResolutionDraftsByMr[mrKey] || {}) }
      if (this.isSameResolutionOption(bucket[entryKey], normalized)) {
        return true
      }
      bucket[entryKey] = normalized
      this.$set(this.conflictResolutionDraftsByMr, mrKey, bucket)
      return true
    },
    collectResolutionOptions() {
      const conflicts = Array.isArray(this.conflictDetail && this.conflictDetail.conflicts) ? this.conflictDetail.conflicts : []
      const mrKey = this.cacheKey(this.conflictTargetRow)
      const map = this.conflictResolutionDraftsByMr[mrKey] || {}
      const options = []
      conflicts.forEach(conflict => {
        const conflictType = this.conflictType(conflict)
        if (!this.isStructuredConflictType(conflictType)) return
        const key = this.conflictResolutionKey(conflict)
        const normalized = this.normalizeResolutionOption(map[key])
        if (normalized) {
          const item = {
            conflictId: normalized.conflictId,
            resolutionStrategy: normalized.resolutionStrategy
          }
          if (normalized.targetPath) item.targetPath = normalized.targetPath
          options.push(item)
        }
      })
      return options
    },
    extractErrorMessage(error, fallback = '') {
      const responseData = error && error.response && error.response.data
      const message = (responseData && (responseData.msg || responseData.message || responseData.error)) ||
        (error && error.message) ||
        fallback
      return String(message || fallback || '')
    },
    isOutdatedMergeCheckError(error) {
      const message = this.extractErrorMessage(error).toLowerCase()
      return message.includes('merge check is outdated')
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
      const merged = rows.map(item => {
        if (!item || typeof item !== 'object') return ''
        if (typeof item.resolvedContent === 'string') return item.resolvedContent
        if (typeof item.finalContent === 'string') return item.finalContent
        if (typeof item.sourceContent === 'string') return item.sourceContent
        if (typeof item.targetContent === 'string') return item.targetContent
        if (typeof item.baseContent === 'string') return item.baseContent
        if (typeof item.content === 'string') return item.content
        if (Array.isArray(item.sourceLines)) return this.joinBlockLines(item.sourceLines)
        if (Array.isArray(item.targetLines)) return this.joinBlockLines(item.targetLines)
        if (Array.isArray(item.baseLines)) return this.joinBlockLines(item.baseLines)
        return ''
      }).join('')
      return merged || fallbackText
    },
    normalizeContentConflictDetail(payload, conflict) {
      const source = payload && typeof payload === 'object' ? payload : {}
      const conflictId = String(source.conflictId || this.conflictKey(conflict) || '')
      const baseContent = this.normalizeTextValue(source.baseContent)
      const sourceContent = this.normalizeTextValue(source.sourceContent)
      const targetContent = this.normalizeTextValue(source.targetContent)
      const blocks = Array.isArray(source.blocks) ? source.blocks.map(item => ({
        ...(item && typeof item === 'object' ? item : {})
      })) : []
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
      return {
        conflictId,
        baseContent,
        sourceContent,
        targetContent,
        blocks,
        resolvedContent
      }
    },
    buildConflictLocator(conflict) {
      const item = conflict && typeof conflict === 'object' ? conflict : {}
      return {
        conflictId: String(this.conflictKey(item) || ''),
        conflictType: this.conflictType(item),
        basePath: String(item.basePath || item.oldPath || ''),
        sourcePath: String(item.sourcePath || item.newPath || ''),
        targetPath: String(item.targetPath || item.path || ''),
        filePath: String(item.filePath || item.path || item.newPath || item.sourcePath || item.targetPath || item.oldPath || '')
      }
    },
    matchConflictByLocator(conflict, locator) {
      if (!conflict || !locator) return false
      const candidateId = String(this.conflictKey(conflict) || '')
      if (locator.conflictId && candidateId === locator.conflictId) return true
      if (this.conflictType(conflict) !== 'CONTENT_CONFLICT') return false

      const candidatePaths = [
        String(conflict.basePath || conflict.oldPath || ''),
        String(conflict.sourcePath || conflict.newPath || ''),
        String(conflict.targetPath || conflict.path || ''),
        String(conflict.filePath || conflict.path || conflict.newPath || conflict.sourcePath || conflict.targetPath || conflict.oldPath || '')
      ].filter(Boolean)
      const expectedPaths = [
        String(locator.basePath || ''),
        String(locator.sourcePath || ''),
        String(locator.targetPath || ''),
        String(locator.filePath || '')
      ].filter(Boolean)
      return expectedPaths.some(path => candidatePaths.includes(path))
    },
    selectConflictAfterUpdate(preferredConflictId, unresolvedIds = []) {
      const conflicts = Array.isArray(this.conflictDetail && this.conflictDetail.conflicts) ? this.conflictDetail.conflicts : []
      if (!conflicts.length) {
        this.selectedConflictId = null
        return
      }
      const unresolvedSet = new Set((Array.isArray(unresolvedIds) ? unresolvedIds : []).map(item => String(item)))
      const unresolvedFirst = conflicts.find(item => unresolvedSet.has(String(this.conflictKey(item))))
      const preferred = conflicts.find(item => String(this.conflictKey(item)) === String(preferredConflictId || ''))
      this.selectedConflictId = this.conflictKey(unresolvedFirst || preferred || conflicts[0])
    },
    clearContentConflictState() {
      this.contentConflictDetail = null
      this.contentConflictLoadingId = null
      this.contentConflictApplyingId = null
    },
    async recheckAndRecoverContentConflict(preferredConflict) {
      await this.handleConflictRecheck(true)
      const conflicts = Array.isArray(this.conflictDetail && this.conflictDetail.conflicts) ? this.conflictDetail.conflicts : []
      const locator = typeof preferredConflict === 'string'
        ? { conflictId: String(preferredConflict || '') }
        : this.buildConflictLocator(preferredConflict)
      let matched = conflicts.find(item => this.matchConflictByLocator(item, locator))
      if (!matched) {
        matched = conflicts.find(item => this.conflictType(item) === 'CONTENT_CONFLICT') || null
      }
      if (matched) {
        this.selectedConflictId = this.conflictKey(matched)
        return matched
      }
      this.$message.warning('Content conflict was rechecked, but the matching editor entry could not be found')
      return null
    },
    branchName(id) {
      const matched = this.branchList.find(item => String(item.id) === String(id))
      return matched ? matched.name : `#${id || '-'}`
    },
    mrStatusType(status) {
      return {
        open: 'warning',
        merged: 'success',
        closed: 'info'
      }[status] || 'info'
    },
    mrStatusLabel(status) {
      return {
        open: '进行中',
        merged: '已合并',
        closed: '已关闭'
      }[status] || (status || '-')
    },
    approvalCount(row) {
      return (Array.isArray(row && row.reviews) ? row.reviews : []).filter(item => item.reviewResult === 'approve').length
    },
    rejectCount(row) {
      return (Array.isArray(row && row.reviews) ? row.reviews : []).filter(item => item.reviewResult === 'reject').length
    },
    currentChecks(row) {
      const checksFromDetail = Array.isArray(row && row.effectiveChecks) ? row.effectiveChecks : []
      const sourceChecks = checksFromDetail.length ? checksFromDetail : (Array.isArray(row && row.checks) ? row.checks : [])
      const commitId = row && row.sourceHeadCommitId
      const latest = new Map()
      sourceChecks.forEach(rawItem => {
        const item = this.normalizeCheckItem(rawItem)
        if (!item) return
        if (String(item && item.commitId) !== String(commitId)) return
        const type = (item && item.checkType ? String(item.checkType) : 'custom').trim().toLowerCase() || 'custom'
        if (!latest.has(type)) {
          latest.set(type, item)
        }
      })
      return Array.from(latest.values())
    },
    normalizeCheckItem(rawItem) {
      if (!rawItem || typeof rawItem !== 'object') return null
      return {
        id: rawItem.id,
        checkType: String(rawItem.checkType || 'custom'),
        checkStatus: String(rawItem.checkStatus || ''),
        commitId: rawItem.commitId,
        mergeRequestId: rawItem.mergeRequestId,
        summary: String(rawItem.summary || ''),
        createdAt: rawItem.createdAt || '',
        blockingMerge: !!rawItem.blockingMerge,
        systemInternal: !!rawItem.systemInternal
      }
    },
    blockingFailedChecks(row) {
      const checks = this.currentChecks(row)
      return checks.filter(item => item.checkStatus === 'failed' && (item.blockingMerge || !item.systemInternal))
    },
    diagnosticFailedChecks(row) {
      return this.currentChecks(row).filter(item => item.checkStatus === 'failed' && item.systemInternal)
    },
    failedActionableCount(row) {
      return this.blockingFailedChecks(row).length
    },
    formatCheckBrief(item) {
      if (!item) return ''
      const checkType = String(item.checkType || 'custom').trim().toLowerCase() || 'custom'
      const summary = String(item.summary || '').trim()
      return summary ? `${checkType}: ${summary}` : `${checkType}: failed`
    },
    failedCheckSummary(row) {
      const failedChecks = this.blockingFailedChecks(row)
      if (!failedChecks.length) return ''
      return failedChecks.slice(0, 2).map(this.formatCheckBrief).join(' | ')
    },
    diagnosticCheckSummary(row) {
      const diagnosticChecks = this.diagnosticFailedChecks(row)
      if (!diagnosticChecks.length) return ''
      return `诊断失败 ${diagnosticChecks.slice(0, 2).map(this.formatCheckBrief).join(' | ')}`
    },
    checkCount(row) {
      return this.currentChecks(row).length
    },
    failedCheckCount(row) {
      const summary = this.failedCheckSummary(row)
      if (summary) return summary
      const diagnostic = this.diagnosticFailedChecks(row)
      if (diagnostic.length) {
        return `[诊断] ${diagnostic.slice(0, 2).map(this.formatCheckBrief).join(' | ')}`
      }
      return 0
    },
    mergeCheckState(row) {
      const key = this.cacheKey(row)
      return this.mergeCheckLoadingIds[key] || !this.mergeRequestConflictCache[key] ? 'unknown' : 'known'
    },
    detail(row) {
      return this.mergeRequestConflictCache[this.cacheKey(row)] || {}
    },
    getConflictCount(row) {
      const detail = this.detail(row)
      const count = Number(detail.conflictCount)
      return Number.isFinite(count) ? count : 0
    },
    hasConflict(row) {
      return this.getConflictCount(row) > 0
    },
    withNextStep(message, stepText) {
      const summary = String(message || '').trim()
      if (!summary) return `下一步：${stepText}`
      if (summary.includes('下一步')) return summary
      return `${summary}。下一步：${stepText}`
    },
    mergeDisabledReason(row) {
      if (!this.canManageProject) return '没有合并权限'
      if (!row || row.status !== 'open') return 'MR 不是进行中状态'
      if (!row.sourceHeadCommitId) return '源分支缺少头提交'

      const key = this.cacheKey(row)
      if (this.mergeCheckLoadingIds[key]) return '正在加载合并检查'

      const detail = this.detail(row)
      if (detail && detail.requiresBranchUpdate) {
        return this.withNextStep(detail.suggestedAction || detail.summary, '更新源分支，或进入冲突处理中心')
      }
      if (detail && Number(detail.conflictCount) > 0) {
        return this.withNextStep(detail.suggestedAction || detail.summary, '进入冲突处理中心')
      }
      if (detail && detail.requiresRecheck) {
        return this.withNextStep(detail.suggestedAction || detail.summary, '重新检查')
      }

      const target = this.branchList.find(item => String(item.id) === String(row.targetBranchId))
      if (target && target.protectedFlag && this.approvalCount(row) < 1) {
        return '至少需要 1 条通过评审'
      }
      if (target && target.protectedFlag && this.failedActionableCount(row) > 0) {
        return `Failed checks: ${this.failedCheckSummary(row) || 'failed checks'}. Actions: re-run checks; add a newer success check with the same type; or fix CI before merging.`
      }
      return ''
    },
    normalizeConflict(conflict) {
      const normalized = conflict && typeof conflict === 'object' ? { ...conflict } : {}
      normalized.id = this.conflictKey(normalized)
      normalized.filePath = normalized.filePath || normalized.newPath || normalized.sourcePath || normalized.targetPath || normalized.oldPath || ''
      return normalized
    },
    normalizeDetail(payload, row) {
      const source = payload && typeof payload === 'object' ? payload : {}
      const conflicts = (Array.isArray(source.conflicts) ? source.conflicts : []).map(this.normalizeConflict)
      const conflictCount = Number(source.totalConflicts ?? source.conflictCount)
      const effectiveChecks = (Array.isArray(source.effectiveChecks) ? source.effectiveChecks : [])
        .map(this.normalizeCheckItem)
        .filter(Boolean)
      const blockingChecks = (Array.isArray(source.blockingChecks) ? source.blockingChecks : [])
        .map(this.normalizeCheckItem)
        .filter(Boolean)
      return {
        id: source.mergeRequestId || (row && row.id) || null,
        title: (row && row.title) || '',
        sourceBranchId: source.sourceBranchId ?? (row && row.sourceBranchId),
        targetBranchId: source.targetBranchId ?? (row && row.targetBranchId),
        sourceBranchName: this.branchName(source.sourceBranchId || (row && row.sourceBranchId)),
        targetBranchName: this.branchName(source.targetBranchId || (row && row.targetBranchId)),
        sourceHeadCommitId: source.sourceCommitId || (row && row.sourceHeadCommitId) || '',
        targetHeadCommitId: source.targetCommitId || (row && row.targetHeadCommitId) || '',
        baseCommitId: source.baseCommitId || '',
        summary: source.summary || '',
        suggestedAction: source.suggestedAction || '',
        conflictCount: Number.isFinite(conflictCount) ? conflictCount : conflicts.length,
        conflicts,
        effectiveChecks,
        blockingChecks,
        blockingReasons: Array.isArray(source.blockingReasons) ? source.blockingReasons : [],
        requiresBranchUpdate: !!source.requiresBranchUpdate,
        requiresRecheck: !!source.requiresRecheck,
        mergeable: !!source.mergeable,
        hasConflict: conflicts.length > 0
      }
    },
    pickDefaultBranches() {
      if (!this.branchList.length) {
        return {
          sourceBranchId: null,
          targetBranchId: null
        }
      }

      const byName = name => this.branchList.find(item => String(item.name || '').toLowerCase() === name)
      const protectedBranch = this.branchList.find(item => !!item.protectedFlag)
      const target = byName('master') || byName('main') || protectedBranch || this.branchList[0]
      const source = byName('develop') || this.branchList.find(item => item.id !== target.id) || target

      return {
        sourceBranchId: source ? source.id : null,
        targetBranchId: target ? target.id : null
      }
    },
    async fetchMergeCheck(row, mode = 'latest', force = false) {
      if (!row || !row.id) return null
      const key = this.cacheKey(row)
      if (!force && mode === 'latest' && this.mergeRequestConflictCache[key]) {
        return this.mergeRequestConflictCache[key]
      }

      this.$set(this.mergeCheckLoadingIds, key, true)
      try {
        const res = mode === 'recheck'
          ? await recheckProjectMerge(row.id)
          : mode === 'premerge'
            ? await getProjectPreMergeCheck(row.id)
            : await getProjectMergeCheckLatest(row.id)
        const detail = this.normalizeDetail(unwrap(res), row)
        this.$set(this.mergeRequestConflictCache, key, detail)
        return detail
      } finally {
        this.$delete(this.mergeCheckLoadingIds, key)
      }
    },
    async loadMergeRequests() {
      this.loading = true
      try {
        const rows = unwrap(await listProjectMergeRequests(this.projectId, this.status))
        this.mergeRequests = Array.isArray(rows) ? rows : []
        await Promise.all(
          this.mergeRequests
            .filter(item => item && item.status === 'open')
            .map(item => this.fetchMergeCheck(item).catch(() => null))
        )
      } finally {
        this.loading = false
        this.$emit('summary-change', {
          branchCount: this.branchList.length,
          protectedBranchCount: this.protectedBranchCount,
          openMrCount: this.openMrCount,
          failedMrCount: this.failedMrCount
        })
      }
    },
    async loadBranches() {
      this.branchLoading = true
      try {
        const rows = unwrap(await listProjectBranches(this.projectId))
        this.branchList = Array.isArray(rows) ? rows : []
        if (!this.createForm.sourceBranchId || !this.createForm.targetBranchId) {
          const defaults = this.pickDefaultBranches()
          this.createForm.sourceBranchId = defaults.sourceBranchId
          this.createForm.targetBranchId = defaults.targetBranchId
        }
      } finally {
        this.branchLoading = false
      }
    },
    async refreshAll() {
      await Promise.all([this.loadBranches(), this.loadMergeRequests()])
    },
    async updateBranchProtection(row, payload) {
      this.branchSavingId = row.id
      try {
        await protectProjectBranch(row.id, payload)
        this.$message.success('分支规则已更新')
        await this.loadBranches()
      } catch (error) {
        this.$message.error((error && error.message) || '更新分支规则失败')
      } finally {
        this.branchSavingId = null
      }
    },
    canDeleteBranch(row) {
      if (!this.canManageProject || !row || !row.id) return false
      if (this.defaultBranchId != null && String(this.defaultBranchId) === String(row.id)) return false
      if (row.protectedFlag) return false
      return true
    },
    async deleteBranch(row) {
      if (!this.canDeleteBranch(row)) {
        this.$message.warning('当前分支不允许删除')
        return
      }
      try {
        await this.$confirm(`确认删除分支 ${row.name || row.id} 吗？`, '提示', {
          type: 'warning'
        })
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error && error.message ? error.message : '删除分支失败')
        }
        return
      }

      this.branchDeletingId = row.id
      try {
        await deleteProjectBranch(row.id)
        this.$message.success('分支已删除')
        await Promise.all([this.loadBranches(), this.loadMergeRequests()])
      } catch (error) {
        this.$message.error((error && error.response && error.response.data && error.response.data.message) || (error && error.message) || '删除分支失败')
      } finally {
        this.branchDeletingId = null
      }
    },
    openCreateDialog() {
      this.createDialogVisible = true
    },
    async submitCreate() {
      if (!this.createForm.sourceBranchId || !this.createForm.targetBranchId) {
        this.$message.warning('请选择源分支和目标分支')
        return
      }
      if (String(this.createForm.sourceBranchId) === String(this.createForm.targetBranchId)) {
        this.$message.warning('源分支和目标分支不能相同')
        return
      }

      this.createLoading = true
      try {
        await createProjectMergeRequest({
          projectId: Number(this.projectId),
          sourceBranchId: Number(this.createForm.sourceBranchId),
          targetBranchId: Number(this.createForm.targetBranchId),
          title: this.createForm.title || '',
          description: this.createForm.description || ''
        })
        this.$message.success('MR 创建成功')
        this.createDialogVisible = false
        this.createForm.title = ''
        this.createForm.description = ''
        await this.loadMergeRequests()
      } catch (error) {
        this.$message.error((error && error.message) || 'MR 创建失败')
      } finally {
        this.createLoading = false
      }
    },
    openReviewDialog(row) {
      this.reviewTarget = row
      this.reviewForm.reviewResult = 'approve'
      this.reviewForm.reviewComment = ''
      this.reviewDialogVisible = true
    },
    async submitReview() {
      if (!this.reviewTarget || !this.reviewTarget.id) return

      this.reviewLoading = true
      try {
        await reviewProjectMergeRequest(this.reviewTarget.id, this.reviewForm)
        this.$message.success('评审提交成功')
        this.reviewDialogVisible = false
        await this.loadMergeRequests()
      } catch (error) {
        this.$message.error((error && error.message) || '评审提交失败')
      } finally {
        this.reviewLoading = false
      }
    },
    openCheckDialog(row) {
      this.checkTarget = row
      this.checkForm.checkType = 'custom'
      this.checkForm.checkStatus = 'success'
      this.checkForm.summary = ''
      this.checkDialogVisible = true
    },
    async submitCheck() {
      if (!this.checkTarget || !this.checkTarget.id) return

      this.checkLoading = true
      try {
        await runProjectCheck({
          projectId: Number(this.projectId),
          mergeRequestId: Number(this.checkTarget.id),
          checkType: this.checkForm.checkType,
          checkStatus: this.checkForm.checkStatus,
          summary: this.checkForm.summary
        })
        this.$message.success('检查结果已记录')
        this.checkDialogVisible = false
        await this.loadMergeRequests()
        const refreshedRow = this.mergeRequests.find(item => String(item && item.id) === String(this.checkTarget.id))
        if (refreshedRow) {
          const key = this.cacheKey(refreshedRow)
          this.$delete(this.mergeRequestConflictCache, key)
          await this.fetchMergeCheck(refreshedRow, 'latest', true).catch(() => null)
          if (this.conflictTargetRow && String(this.conflictTargetRow.id) === String(refreshedRow.id) && this.conflictDrawerVisible) {
            this.conflictTargetRow = refreshedRow
            this.conflictDetail = this.detail(refreshedRow)
          }
        }
      } catch (error) {
        this.$message.error((error && error.message) || '记录检查结果失败')
      } finally {
        this.checkLoading = false
      }
    },
    async openConflictDrawer(row, force = false) {
      if (!row || !row.id) return

      this.conflictTargetRow = row
      this.conflictDrawerVisible = true
      this.conflictDrawerLoading = true
      this.unresolvedConflictIds = []
      this.clearContentConflictState()
      try {
        this.conflictDetail = (await this.fetchMergeCheck(row, 'latest', force)) || this.detail(row)
        const firstConflict = this.conflictDetail.conflicts && this.conflictDetail.conflicts[0]
        this.selectedConflictId = this.conflictKey(firstConflict)
      } finally {
        this.conflictDrawerLoading = false
      }
    },
    handleConflictSelect(conflict) {
      this.selectedConflictId = this.conflictKey(conflict)
      this.clearContentConflictState()
    },
    async handleLoadContentConflict(payload, allowRetry = true) {
      if (!this.conflictTargetRow || !this.conflictTargetRow.id) return
      const conflict = payload && payload.conflict ? payload.conflict : payload
      const force = !!(payload && payload.force)
      if (!conflict || this.conflictType(conflict) !== 'CONTENT_CONFLICT') return

      const conflictId = String(this.conflictKey(conflict) || '').trim()
      if (!conflictId) return
      if (!force && this.contentConflictDetail && String(this.contentConflictDetail.conflictId || '') === conflictId) return

      this.contentConflictLoadingId = conflictId
      try {
        const res = await getProjectMergeContentConflict(this.conflictTargetRow.id, conflictId)
        this.contentConflictDetail = this.normalizeContentConflictDetail(unwrap(res), conflict)
      } catch (error) {
        if (allowRetry && this.isOutdatedMergeCheckError(error)) {
          this.$message.warning('合并检查已过期，正在先重检后再打开编辑器')
          const recovered = await this.recheckAndRecoverContentConflict(conflict)
          if (recovered) {
            await this.handleLoadContentConflict({ conflict: recovered, force: true }, false)
          }
          return
        }
        this.$message.error(this.extractErrorMessage(error, '加载内容冲突详情失败'))
      } finally {
        if (String(this.contentConflictLoadingId || '') === conflictId) {
          this.contentConflictLoadingId = null
        }
      }
    },
    async handleSaveContentConflict(payload, allowRetry = true) {
      if (!this.conflictTargetRow || !this.conflictTargetRow.id) return
      const conflict = payload && payload.conflict
      const conflictId = String((payload && payload.conflictId) || this.conflictKey(conflict) || '').trim()
      if (!conflictId) {
        this.$message.warning('缺少 conflictId')
        return
      }

      this.contentConflictApplyingId = conflictId
      try {
        const result = unwrap(await resolveProjectMergeContentConflict(this.conflictTargetRow.id, {
          conflictId,
          resolvedContent: this.normalizeTextValue(payload && payload.resolvedContent)
        })) || {}
        this.unresolvedConflictIds = Array.isArray(result.unresolvedConflictIds) ? result.unresolvedConflictIds.map(String) : []

        if (result.latestMergeCheck) {
          this.conflictDetail = this.normalizeDetail(result.latestMergeCheck, this.conflictTargetRow)
          this.$set(this.mergeRequestConflictCache, this.cacheKey(this.conflictTargetRow), this.conflictDetail)
        } else {
          this.conflictDetail = await this.fetchMergeCheck(this.conflictTargetRow, 'latest', true)
        }

        this.selectConflictAfterUpdate(conflictId, this.unresolvedConflictIds)
        this.contentConflictDetail = null
        if (result.supplementalCommitId) {
          this.$message.success(`内容冲突已应用（提交 ${result.supplementalCommitId}）`)
        } else {
          this.$message.success('内容冲突已应用')
        }
        await this.loadMergeRequests()
      } catch (error) {
        if (allowRetry && this.isOutdatedMergeCheckError(error)) {
          this.$message.warning('合并检查已过期，正在先重检后再打开编辑器')
          const recovered = await this.recheckAndRecoverContentConflict(conflict)
          if (recovered) {
            await this.handleLoadContentConflict({ conflict: recovered, force: true }, false)
          }
          return
        }
        this.$message.error(this.extractErrorMessage(error, '保存内容冲突失败'))
      } finally {
        if (String(this.contentConflictApplyingId || '') === conflictId) {
          this.contentConflictApplyingId = null
        }
      }
    },
    handleConflictResolutionChange(payload) {
      const option = payload && payload.option
      if (!option) return
      this.setResolutionDraft(option)
    },
    async handleConflictResolutionSave(option) {
      if (!this.conflictTargetRow || !this.conflictTargetRow.id) return
      this.saveResolutionLoadingId = this.conflictTargetRow.id
      try {
        const success = this.setResolutionDraft(option)
        if (!success) {
          this.$message.warning('请选择有效的处理策略')
          return
        }
        this.$message.success('策略草稿已保存')
      } finally {
        this.saveResolutionLoadingId = null
      }
    },
    async handleConflictResolutionApply(option) {
      if (!this.conflictTargetRow || !this.conflictTargetRow.id) return
      const saved = this.setResolutionDraft(option)
      if (!saved) {
        this.$message.warning('请选择有效的处理策略')
        return
      }

      const options = this.collectResolutionOptions()
      if (!options.length) {
        this.$message.warning('没有可提交的可处理冲突')
        return
      }

      this.applyResolutionLoadingId = this.conflictTargetRow.id
      try {
        const result = unwrap(await resolveProjectMergeConflicts(this.conflictTargetRow.id, { options })) || {}
        this.unresolvedConflictIds = Array.isArray(result.unresolvedConflictIds) ? result.unresolvedConflictIds.map(String) : []

        if (result.latestMergeCheck) {
          this.conflictDetail = this.normalizeDetail(result.latestMergeCheck, this.conflictTargetRow)
          this.$set(this.mergeRequestConflictCache, this.cacheKey(this.conflictTargetRow), this.conflictDetail)
        } else {
          this.conflictDetail = await this.fetchMergeCheck(this.conflictTargetRow, 'latest', true)
        }

        this.selectConflictAfterUpdate(option && option.conflictId, this.unresolvedConflictIds)
        this.clearContentConflictState()

        if (result.resolved === false && this.unresolvedConflictIds.length) {
          this.$message.warning(`仍有 ${this.unresolvedConflictIds.length} 个冲突未解决`)
        } else {
          this.$message.success('冲突策略已应用')
        }
        await this.loadMergeRequests()
      } catch (error) {
        this.$message.error((error && error.message) || '应用冲突策略失败')
      } finally {
        this.applyResolutionLoadingId = null
      }
    },
    async handleConflictRecheck(silent = false) {
      if (!this.conflictTargetRow || !this.conflictTargetRow.id) return

      this.recheckLoadingId = this.conflictTargetRow.id
      try {
        this.conflictDetail = await this.fetchMergeCheck(this.conflictTargetRow, 'recheck', true)
        const firstConflict = this.conflictDetail.conflicts && this.conflictDetail.conflicts[0]
        this.selectedConflictId = this.conflictKey(firstConflict)
        this.unresolvedConflictIds = []
        this.clearContentConflictState()
        if (!silent) {
          this.$message.success('合并检查已重新执行')
        }
        await this.loadMergeRequests()
      } catch (error) {
        if (!silent) {
          this.$message.error(this.extractErrorMessage(error, '重新执行合并检查失败'))
          return
        }
        throw error
      } finally {
        this.recheckLoadingId = null
      }
    },
    findStaleBranchConflict(detail = this.conflictDetail) {
      const conflicts = Array.isArray(detail && detail.conflicts) ? detail.conflicts : []
      return conflicts.find(item => this.conflictType(item) === 'STALE_BRANCH') || null
    },
    async handleConflictSyncSource() {
      if (!this.conflictTargetRow || !this.conflictTargetRow.id) return
      const staleConflict = this.findStaleBranchConflict(this.conflictDetail)
      if (!staleConflict) {
        this.$message.warning('当前没有可更新的分支落后项，请先重新检查。')
        return
      }

      const option = {
        conflictId: String(this.conflictKey(staleConflict) || ''),
        resolutionStrategy: 'SYNC_SOURCE_WITH_TARGET'
      }
      if (!option.conflictId) {
        this.$message.warning('缺少冲突标识，无法更新源分支。')
        return
      }

      this.applyResolutionLoadingId = this.conflictTargetRow.id
      try {
        const result = unwrap(await resolveProjectMergeConflicts(this.conflictTargetRow.id, { options: [option] })) || {}
        this.unresolvedConflictIds = Array.isArray(result.unresolvedConflictIds) ? result.unresolvedConflictIds.map(String) : []

        if (result.latestMergeCheck) {
          this.conflictDetail = this.normalizeDetail(result.latestMergeCheck, this.conflictTargetRow)
          this.$set(this.mergeRequestConflictCache, this.cacheKey(this.conflictTargetRow), this.conflictDetail)
        } else {
          this.conflictDetail = await this.fetchMergeCheck(this.conflictTargetRow, 'latest', true)
        }

        const firstConflict = this.conflictDetail.conflicts && this.conflictDetail.conflicts[0]
        this.selectedConflictId = this.conflictKey(firstConflict)
        this.clearContentConflictState()

        if (result.supplementalCommitId) {
          this.$message.success(`源分支已更新（提交 ${result.supplementalCommitId}）`)
        } else {
          this.$message.success('源分支已更新')
        }
        await this.loadMergeRequests()
      } catch (error) {
        this.$message.error(this.extractErrorMessage(error, '更新源分支失败'))
      } finally {
        this.applyResolutionLoadingId = null
      }
    },
    async mergeRow(row) {
      if (!row) return

      const disabledReason = this.mergeDisabledReason(row)
      if (disabledReason) {
        this.$message.warning(disabledReason)
        return
      }

      try {
        const preMerge = await this.fetchMergeCheck(row, 'premerge', true)
        if (!preMerge || !preMerge.mergeable) {
          this.conflictTargetRow = row
          this.conflictDetail = preMerge || {}
          this.conflictDrawerVisible = true
          const firstConflict = this.conflictDetail.conflicts && this.conflictDetail.conflicts[0]
          this.selectedConflictId = this.conflictKey(firstConflict)
          this.$message.warning((preMerge && (preMerge.suggestedAction || preMerge.summary)) || '合并被阻塞')
          return
        }

        this.mergeLoadingId = row.id
        await mergeProjectMergeRequest(row.id)
        this.$message.success('MR 已合并')
        await this.loadMergeRequests()
        await this.loadBranches()
      } catch (error) {
        this.$message.error((error && error.message) || 'MR 合并失败')
      } finally {
        this.mergeLoadingId = null
      }
    },
    handleDrawerOpenConflictCenter(payload) {
      if (!this.conflictTargetRow || !this.conflictTargetRow.id) return
      const conflictId = payload && payload.conflictId ? String(payload.conflictId) : ''
      this.openConflictCenter(this.conflictTargetRow, conflictId)
    },
    openConflictCenter(row, conflictId = '') {
      if (!row || !row.id) return
      const normalizedConflictId = String(conflictId || '').trim()
      this.$router.push({
        path: '/projectmergeconflict',
        query: {
          projectId: String(this.projectId),
          mergeRequestId: String(row.id),
          tab: 'summary',
          conflictId: normalizedConflictId || undefined,
          fromTab: 'audit-manage'
        }
      })
    }
  }
}
</script>

<style scoped>
.audit {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.minor {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>







