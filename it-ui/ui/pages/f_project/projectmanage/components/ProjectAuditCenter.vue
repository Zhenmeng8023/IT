<template>
  <div class="audit">
    <el-row :gutter="16">
      <el-col :xs="24" :lg="8">
        <el-card shadow="never">
          <div slot="header">Branch Protection</div>
          <el-table :data="branchList" border size="small" v-loading="branchLoading">
            <el-table-column prop="name" label="Branch" min-width="120" />
            <el-table-column label="Protected" width="96">
              <template slot-scope="{ row }">
                <el-switch
                  :value="!!row.protectedFlag"
                  :disabled="!canManageProject || branchSavingId === row.id"
                  @change="value => updateBranchProtection(row, { protectedFlag: value })"
                />
              </template>
            </el-table-column>
            <el-table-column label="Direct Commit" width="112">
              <template slot-scope="{ row }">
                <el-switch
                  :value="!!row.allowDirectCommitFlag"
                  :disabled="!canManageProject || branchSavingId === row.id"
                  @change="value => updateBranchProtection(row, { allowDirectCommitFlag: value })"
                />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="16">
        <el-card shadow="never">
          <div slot="header" class="toolbar">
            <span>Audit Center</span>
            <div class="toolbar-actions">
              <el-select v-model="status" size="small" clearable placeholder="status" @change="loadMergeRequests">
                <el-option label="open" value="open" />
                <el-option label="merged" value="merged" />
                <el-option label="closed" value="closed" />
              </el-select>
              <el-button size="small" @click="refreshAll">Refresh</el-button>
              <el-button v-if="canManageProject" size="small" type="primary" @click="openCreateDialog">Create MR</el-button>
            </div>
          </div>

          <el-table :data="mergeRequests" border size="small" v-loading="loading">
            <el-table-column label="Title" min-width="240">
              <template slot-scope="{ row }">
                <div>{{ row.title || 'Untitled MR' }}</div>
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

            <el-table-column label="Review" width="120">
              <template slot-scope="{ row }">
                <div class="minor">approve {{ approvalCount(row) }}</div>
                <div class="minor">reject {{ rejectCount(row) }}</div>
              </template>
            </el-table-column>

            <el-table-column label="Checks" width="140">
              <template slot-scope="{ row }">
                <div class="minor">current {{ checkCount(row) }}</div>
                <div class="minor">{{ failedCheckCount(row) ? `failed ${failedCheckCount(row)}` : 'passed' }}</div>
              </template>
            </el-table-column>

            <el-table-column prop="status" label="Status" width="100">
              <template slot-scope="{ row }">
                <el-tag size="mini" :type="mrStatusType(row.status)">{{ row.status || '-' }}</el-tag>
              </template>
            </el-table-column>

            <el-table-column label="Actions" min-width="280" fixed="right">
              <template slot-scope="{ row }">
                <el-button size="mini" @click="openReviewDialog(row)">Review</el-button>
                <el-button size="mini" type="warning" plain @click="openCheckDialog(row)">Check</el-button>
                <el-button size="mini" plain @click="openConflictDrawer(row, true)">Conflicts</el-button>
                <el-button
                  size="mini"
                  type="success"
                  :loading="mergeLoadingId === row.id"
                  :disabled="!!mergeDisabledReason(row)"
                  @click="mergeRow(row)"
                >
                  Merge
                </el-button>
                <div v-if="mergeDisabledReason(row)" class="minor">{{ mergeDisabledReason(row) }}</div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog title="Create MR" :visible.sync="createDialogVisible" width="520px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="Source">
          <el-select v-model="createForm.sourceBranchId" style="width:100%">
            <el-option v-for="item in branchList" :key="'src-' + item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Target">
          <el-select v-model="createForm.targetBranchId" style="width:100%">
            <el-option v-for="item in branchList" :key="'target-' + item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Title">
          <el-input v-model="createForm.title" />
        </el-form-item>
        <el-form-item label="Desc">
          <el-input v-model="createForm.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="createDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitCreate">Create</el-button>
      </span>
    </el-dialog>

    <el-dialog title="Review MR" :visible.sync="reviewDialogVisible" width="460px">
      <el-form :model="reviewForm" label-width="90px">
        <el-form-item label="Result">
          <el-select v-model="reviewForm.reviewResult" style="width:100%">
            <el-option label="approve" value="approve" />
            <el-option label="comment" value="comment" />
            <el-option label="reject" value="reject" />
          </el-select>
        </el-form-item>
        <el-form-item label="Comment">
          <el-input v-model="reviewForm.reviewComment" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="reviewDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="reviewLoading" @click="submitReview">Submit</el-button>
      </span>
    </el-dialog>

    <el-dialog title="Record Check" :visible.sync="checkDialogVisible" width="460px">
      <el-form :model="checkForm" label-width="90px">
        <el-form-item label="Type">
          <el-select v-model="checkForm.checkType" style="width:100%">
            <el-option label="custom" value="custom" />
            <el-option label="ci" value="ci" />
            <el-option label="security" value="security" />
            <el-option label="release" value="release" />
          </el-select>
        </el-form-item>
        <el-form-item label="Status">
          <el-select v-model="checkForm.checkStatus" style="width:100%">
            <el-option label="success" value="success" />
            <el-option label="failed" value="failed" />
            <el-option label="running" value="running" />
            <el-option label="queued" value="queued" />
          </el-select>
        </el-form-item>
        <el-form-item label="Summary">
          <el-input v-model="checkForm.summary" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="checkDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="checkLoading" @click="submitCheck">Submit</el-button>
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
      @select-conflict="handleConflictSelect"
      @refresh="openConflictDrawer(conflictTargetRow, true)"
      @recheck="handleConflictRecheck"
      @merge="mergeRow(conflictTargetRow)"
    />
  </div>
</template>

<script>
import ConflictBadge from './ConflictBadge.vue'
import ConflictDetailDrawer from './ConflictDetailDrawer.vue'
import { listProjectBranches, protectProjectBranch } from '@/api/projectBranch'
import {
  createProjectMergeRequest,
  getProjectMergeCheckLatest,
  getProjectPreMergeCheck,
  listProjectMergeRequests,
  mergeProjectMergeRequest,
  recheckProjectMerge,
  reviewProjectMergeRequest,
  runProjectCheck
} from '@/api/projectMergeRequest'

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
    approvalCount(row) {
      return (Array.isArray(row && row.reviews) ? row.reviews : []).filter(item => item.reviewResult === 'approve').length
    },
    rejectCount(row) {
      return (Array.isArray(row && row.reviews) ? row.reviews : []).filter(item => item.reviewResult === 'reject').length
    },
    currentChecks(row) {
      const commitId = row && row.sourceHeadCommitId
      const latest = new Map()
      ;(Array.isArray(row && row.checks) ? row.checks : []).forEach(item => {
        if (String(item && item.commitId) !== String(commitId)) return
        const type = (item && item.checkType ? String(item.checkType) : 'custom').trim().toLowerCase() || 'custom'
        if (!latest.has(type)) {
          latest.set(type, item)
        }
      })
      return Array.from(latest.values())
    },
    checkCount(row) {
      return this.currentChecks(row).length
    },
    failedCheckCount(row) {
      return this.currentChecks(row).filter(item => item.checkStatus === 'failed').length
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
    mergeDisabledReason(row) {
      if (!this.canManageProject) return 'No merge permission'
      if (!row || row.status !== 'open') return 'MR is not open'
      if (!row.sourceHeadCommitId) return 'Source head is missing'

      const key = this.cacheKey(row)
      if (this.mergeCheckLoadingIds[key]) return 'Loading merge check'

      const detail = this.detail(row)
      if (detail && (detail.requiresRecheck || detail.requiresBranchUpdate || Number(detail.conflictCount) > 0)) {
        return detail.suggestedAction || detail.summary || 'Resolve conflicts first'
      }

      const target = this.branchList.find(item => String(item.id) === String(row.targetBranchId))
      if (target && target.protectedFlag && this.approvalCount(row) < 1) {
        return 'Need one approval'
      }
      if (target && target.protectedFlag && this.failedCheckCount(row) > 0) {
        return 'There are failed checks'
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
        this.$message.success('Branch rule updated')
        await this.loadBranches()
      } catch (error) {
        this.$message.error((error && error.message) || 'Failed to update branch rule')
      } finally {
        this.branchSavingId = null
      }
    },
    openCreateDialog() {
      this.createDialogVisible = true
    },
    async submitCreate() {
      if (!this.createForm.sourceBranchId || !this.createForm.targetBranchId) {
        this.$message.warning('Select source and target branch')
        return
      }
      if (String(this.createForm.sourceBranchId) === String(this.createForm.targetBranchId)) {
        this.$message.warning('Source and target must be different')
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
        this.$message.success('MR created')
        this.createDialogVisible = false
        this.createForm.title = ''
        this.createForm.description = ''
        await this.loadMergeRequests()
      } catch (error) {
        this.$message.error((error && error.message) || 'Failed to create MR')
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
        this.$message.success('Review submitted')
        this.reviewDialogVisible = false
        await this.loadMergeRequests()
      } catch (error) {
        this.$message.error((error && error.message) || 'Failed to submit review')
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
        this.$message.success('Check recorded')
        this.checkDialogVisible = false
        await this.loadMergeRequests()
      } catch (error) {
        this.$message.error((error && error.message) || 'Failed to record check')
      } finally {
        this.checkLoading = false
      }
    },
    async openConflictDrawer(row, force = false) {
      if (!row || !row.id) return

      this.conflictTargetRow = row
      this.conflictDrawerVisible = true
      this.conflictDrawerLoading = true
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
    },
    async handleConflictRecheck() {
      if (!this.conflictTargetRow || !this.conflictTargetRow.id) return

      this.recheckLoadingId = this.conflictTargetRow.id
      try {
        this.conflictDetail = await this.fetchMergeCheck(this.conflictTargetRow, 'recheck', true)
        const firstConflict = this.conflictDetail.conflicts && this.conflictDetail.conflicts[0]
        this.selectedConflictId = this.conflictKey(firstConflict)
        this.$message.success('Merge check re-run completed')
        await this.loadMergeRequests()
      } catch (error) {
        this.$message.error((error && error.message) || 'Failed to re-run merge check')
      } finally {
        this.recheckLoadingId = null
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
          this.$message.warning((preMerge && (preMerge.suggestedAction || preMerge.summary)) || 'Merge is blocked')
          return
        }

        this.mergeLoadingId = row.id
        await mergeProjectMergeRequest(row.id)
        this.$message.success('MR merged')
        await this.loadMergeRequests()
        await this.loadBranches()
      } catch (error) {
        this.$message.error((error && error.message) || 'Failed to merge MR')
      } finally {
        this.mergeLoadingId = null
      }
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
