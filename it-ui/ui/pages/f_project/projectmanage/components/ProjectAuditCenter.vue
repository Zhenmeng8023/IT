<template>
  <div class="project-audit-center">
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      class="audit-tip"
      title="主线保护流程：文件改动进入工作区后先提交到分支，再通过合并请求、评审与检查进入主线。"
    />

    <div class="audit-lane">
      <div v-for="item in auditStages" :key="item.key" class="audit-lane-item">
        <div class="audit-lane-step">{{ item.order }}</div>
        <div class="audit-lane-body">
          <div class="audit-lane-title">{{ item.title }}</div>
          <div class="audit-lane-desc">{{ item.desc }}</div>
        </div>
      </div>
    </div>

    <el-row :gutter="16" class="summary-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="summary-card">
          <div class="summary-value">{{ branchList.length }}</div>
          <div class="summary-label">分支总数</div>
          <div class="summary-note">建议长期保留 main / dev，功能开发走 feature 分支。</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="summary-card">
          <div class="summary-value">{{ protectedBranchCount }}</div>
          <div class="summary-label">受保护分支</div>
          <div class="summary-note">主线建议关闭直提，让合并入口只保留 MR。</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="summary-card">
          <div class="summary-value">{{ openMrCount }}</div>
          <div class="summary-label">打开中的 MR</div>
          <div class="summary-note">这里聚合待评审、待检查和待合并的主线入口。</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="summary-card danger">
          <div class="summary-value">{{ failedMrCount }}</div>
          <div class="summary-label">存在失败检查</div>
          <div class="summary-note">有失败检查时先修复分支，再推进合并更稳妥。</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="audit-card">
          <div slot="header" class="card-header">
            <span>分支保护</span>
            <el-button size="mini" type="text" @click="loadBranches">刷新</el-button>
          </div>

          <el-table :data="branchList" size="small" border v-loading="branchLoading">
            <el-table-column prop="name" label="分支" min-width="110" />
            <el-table-column prop="branchType" label="类型" width="90" />
            <el-table-column prop="headCommitId" label="Head" width="88" />
            <el-table-column label="保护" width="88">
              <template slot-scope="{ row }">
                <el-switch
                  :value="!!row.protectedFlag"
                  :disabled="!canManageProject || branchSavingId === row.id"
                  @change="value => updateBranchProtection(row, { protectedFlag: value })"
                />
              </template>
            </el-table-column>
            <el-table-column label="直提" width="88">
              <template slot-scope="{ row }">
                <el-switch
                  :value="!!row.allowDirectCommitFlag"
                  :disabled="!canManageProject || branchSavingId === row.id"
                  @change="value => updateBranchProtection(row, { allowDirectCommitFlag: value })"
                />
              </template>
            </el-table-column>
          </el-table>

          <div class="branch-help">
            <div>建议把 `main` 保持为受保护分支，并关闭直接提交。</div>
            <div>开发分支可以允许直接提交，但最终仍通过 MR 合入主线。</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="audit-card">
          <div slot="header" class="card-header card-header-wrap">
            <div class="card-header-left">
              <span>审核中心</span>
              <span class="header-desc">统一查看合并请求、评审结果和检查状态。</span>
            </div>
            <div class="toolbar-actions">
              <el-select v-model="status" size="small" clearable placeholder="状态筛选" class="toolbar-select" @change="loadMergeRequests">
                <el-option label="open" value="open" />
                <el-option label="merged" value="merged" />
                <el-option label="closed" value="closed" />
              </el-select>
              <el-button size="small" @click="refreshAll">刷新</el-button>
              <el-button v-if="canManageProject" type="primary" size="small" @click="openCreateDialog">创建 MR</el-button>
            </div>
          </div>

          <el-table :data="mergeRequests" border size="small" v-loading="loading">
            <el-table-column label="标题" min-width="220">
              <template slot-scope="{ row }">
                <div class="mr-title-cell">
                  <div class="mr-title-main">{{ row.title || '未命名合并请求' }}</div>
                  <div class="mr-title-tags">
                    <el-tag size="mini" effect="plain">{{ branchName(row.sourceBranchId) }}</el-tag>
                    <span class="mr-title-arrow">-></span>
                    <el-tag size="mini" type="success" effect="plain">{{ branchName(row.targetBranchId) }}</el-tag>
                    <el-tag v-if="row.status === 'open'" size="mini" type="warning" effect="plain">待处理</el-tag>
                  </div>
                  <div class="mr-title-sub">
                    #{{ row.id || '-' }} · 创建于 {{ formatTime(row.createdAt) || '-' }}
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="提交" width="140">
              <template slot-scope="{ row }">
                <div class="mr-meta-line">源：#{{ row.sourceHeadCommitId || '-' }}</div>
                <div class="mr-meta-line">目标：#{{ row.targetHeadCommitId || '-' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="评审" width="140">
              <template slot-scope="{ row }">
                <div class="mr-meta-line success-text">通过 {{ approvalCount(row) }}</div>
                <div class="mr-meta-line danger-text">拒绝 {{ rejectCount(row) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="检查" width="160">
              <template slot-scope="{ row }">
                <div class="mr-meta-line">总数 {{ checkCount(row) }}</div>
                <div class="mr-meta-line" :class="failedCheckCount(row) ? 'danger-text' : 'success-text'">
                  {{ failedCheckCount(row) ? ('失败 ' + failedCheckCount(row)) : '全部通过/未运行' }}
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="110">
              <template slot-scope="{ row }">
                <el-tag size="mini" :type="mrStatusType(row.status)">{{ row.status || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="170">
              <template slot-scope="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" min-width="280" fixed="right">
              <template slot-scope="{ row }">
                <el-button size="mini" @click="openReviewDialog(row)">评审</el-button>
                <el-button size="mini" type="warning" plain @click="openCheckDialog(row)">检查</el-button>
                <el-button
                  size="mini"
                  type="success"
                  :disabled="!canManageProject || row.status !== 'open'"
                  @click="mergeRow(row)"
                >
                  合并
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!loading && !mergeRequests.length" description="当前没有合并请求" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog title="创建合并请求" :visible.sync="createDialogVisible" width="520px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="源分支">
          <el-select v-model="createForm.sourceBranchId" style="width: 100%">
            <el-option v-for="item in branchList" :key="'src-' + item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标分支">
          <el-select v-model="createForm.targetBranchId" style="width: 100%">
            <el-option v-for="item in branchList" :key="'target-' + item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="createForm.title" maxlength="120" show-word-limit />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="createForm.description" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitCreate">创建</el-button>
      </span>
    </el-dialog>

    <el-dialog title="提交评审" :visible.sync="reviewDialogVisible" width="480px">
      <el-form :model="reviewForm" label-width="90px">
        <el-form-item label="评审结果">
          <el-select v-model="reviewForm.reviewResult" style="width: 100%">
            <el-option label="approve" value="approve" />
            <el-option label="comment" value="comment" />
            <el-option label="reject" value="reject" />
          </el-select>
        </el-form-item>
        <el-form-item label="评审说明">
          <el-input v-model="reviewForm.reviewComment" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewLoading" @click="submitReview">提交</el-button>
      </span>
    </el-dialog>

    <el-dialog title="登记检查结果" :visible.sync="checkDialogVisible" width="480px">
      <el-form :model="checkForm" label-width="90px">
        <el-form-item label="检查类型">
          <el-select v-model="checkForm.checkType" style="width: 100%">
            <el-option label="custom" value="custom" />
            <el-option label="ci" value="ci" />
            <el-option label="security" value="security" />
            <el-option label="release" value="release" />
          </el-select>
        </el-form-item>
        <el-form-item label="检查状态">
          <el-select v-model="checkForm.checkStatus" style="width: 100%">
            <el-option label="success" value="success" />
            <el-option label="failed" value="failed" />
            <el-option label="running" value="running" />
            <el-option label="queued" value="queued" />
          </el-select>
        </el-form-item>
        <el-form-item label="检查说明">
          <el-input v-model="checkForm.summary" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="checkDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="checkLoading" @click="submitCheck">提交</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listProjectBranches, protectProjectBranch } from '@/api/projectBranch'
import {
  createProjectMergeRequest,
  listProjectMergeRequests,
  mergeProjectMergeRequest,
  reviewProjectMergeRequest,
  runProjectCheck
} from '@/api/projectMergeRequest'

function unwrap(res) {
  const raw = res && Object.prototype.hasOwnProperty.call(res, 'data') ? res.data : res
  if (raw && typeof raw === 'object' && Object.prototype.hasOwnProperty.call(raw, 'code')) {
    return raw.data
  }
  return raw
}

export default {
  name: 'ProjectAuditCenter',
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
      branchSavingId: null,
      status: 'open',
      branchList: [],
      mergeRequests: [],
      createDialogVisible: false,
      reviewDialogVisible: false,
      checkDialogVisible: false,
      reviewTarget: null,
      checkTarget: null,
      createForm: this.createDefaultCreateForm(),
      reviewForm: this.createDefaultReviewForm(),
      checkForm: this.createDefaultCheckForm()
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
      return this.mergeRequests.filter(item => this.failedCheckCount(item) > 0).length
    },
    auditStages() {
      return [
        { key: 'commit', order: '01', title: '开发分支提交', desc: `当前可见 ${this.branchList.length} 个分支，先把改动稳定留在分支内。` },
        { key: 'mr', order: '02', title: '创建 MR', desc: `现在有 ${this.openMrCount} 个打开中的 MR，作为进入主线的统一入口。` },
        { key: 'review', order: '03', title: '评审与检查', desc: `受保护分支 ${this.protectedBranchCount} 个，评审和检查会一起成为合并门槛。` },
        { key: 'merge', order: '04', title: '主线合并', desc: this.failedMrCount ? `当前有 ${this.failedMrCount} 个 MR 需要先修复检查。` : '当前没有失败检查，合并节奏更清晰。'}
      ]
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler() {
        if (this.projectId) {
          this.refreshAll()
        }
      }
    }
  },
  methods: {
    createDefaultCreateForm() {
      return {
        sourceBranchId: null,
        targetBranchId: null,
        title: '',
        description: ''
      }
    },
    createDefaultReviewForm() {
      return {
        reviewResult: 'approve',
        reviewComment: ''
      }
    },
    createDefaultCheckForm() {
      return {
        checkType: 'custom',
        checkStatus: 'success',
        summary: ''
      }
    },
    async refreshAll() {
      await Promise.all([this.loadBranches(), this.loadMergeRequests()])
    },
    async loadBranches() {
      this.branchLoading = true
      try {
        const response = await listProjectBranches(this.projectId)
        const rows = unwrap(response)
        this.branchList = Array.isArray(rows) ? rows : []
        if (!this.createForm.sourceBranchId && this.branchList.length) {
          const devBranch = this.branchList.find(item => item.branchType === 'dev') || this.branchList.find(item => item.name === 'dev')
          const mainBranch = this.branchList.find(item => item.branchType === 'main') || this.branchList.find(item => item.name === 'main')
          this.createForm.sourceBranchId = devBranch ? devBranch.id : this.branchList[0].id
          this.createForm.targetBranchId = mainBranch ? mainBranch.id : this.branchList[0].id
        }
      } catch (error) {
        this.$message.error(error.response?.data?.message || '加载分支失败')
      } finally {
        this.branchLoading = false
      }
    },
    async loadMergeRequests() {
      this.loading = true
      try {
        const response = await listProjectMergeRequests(this.projectId, this.status)
        const rows = unwrap(response)
        this.mergeRequests = Array.isArray(rows) ? rows : []
      } catch (error) {
        this.$message.error(error.response?.data?.message || '加载合并请求失败')
      } finally {
        this.loading = false
      }
    },
    branchName(branchId) {
      const matched = this.branchList.find(item => String(item.id) === String(branchId))
      return matched ? matched.name : `#${branchId || '-'}`
    },
    mrStatusType(status) {
      return { open: 'warning', merged: 'success', closed: 'info' }[status] || 'info'
    },
    approvalCount(row) {
      return this.readReviews(row).filter(item => item.reviewResult === 'approve').length
    },
    rejectCount(row) {
      return this.readReviews(row).filter(item => item.reviewResult === 'reject').length
    },
    checkCount(row) {
      return this.readChecks(row).length
    },
    failedCheckCount(row) {
      return this.readChecks(row).filter(item => item.checkStatus === 'failed').length
    },
    readReviews(row) {
      return Array.isArray(row && row.reviews) ? row.reviews : []
    },
    readChecks(row) {
      return Array.isArray(row && row.checks) ? row.checks : []
    },
    formatTime(value) {
      if (!value) return ''
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return `${date.toLocaleDateString('zh-CN')} ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
    },
    async updateBranchProtection(row, payload) {
      if (!this.canManageProject) return
      this.branchSavingId = row.id
      try {
        await protectProjectBranch(row.id, payload)
        this.$message.success('分支保护已更新')
        await this.loadBranches()
      } catch (error) {
        this.$message.error(error.response?.data?.message || '分支保护更新失败')
      } finally {
        this.branchSavingId = null
      }
    },
    openCreateDialog() {
      this.createForm = {
        ...this.createDefaultCreateForm(),
        sourceBranchId: this.createForm.sourceBranchId || (this.branchList[0] && this.branchList[0].id) || null,
        targetBranchId: this.createForm.targetBranchId || (this.branchList[0] && this.branchList[0].id) || null
      }
      this.createDialogVisible = true
    },
    async submitCreate() {
      if (!this.createForm.sourceBranchId || !this.createForm.targetBranchId) {
        this.$message.warning('请选择源分支和目标分支')
        return
      }
      this.createLoading = true
      try {
        await createProjectMergeRequest({
          projectId: Number(this.projectId),
          sourceBranchId: Number(this.createForm.sourceBranchId),
          targetBranchId: Number(this.createForm.targetBranchId),
          title: this.createForm.title,
          description: this.createForm.description
        })
        this.$message.success('合并请求已创建')
        this.createDialogVisible = false
        this.createForm = this.createDefaultCreateForm()
        await this.loadMergeRequests()
      } catch (error) {
        this.$message.error(error.response?.data?.message || '创建合并请求失败')
      } finally {
        this.createLoading = false
      }
    },
    openReviewDialog(row) {
      this.reviewTarget = row
      this.reviewForm = this.createDefaultReviewForm()
      this.reviewDialogVisible = true
    },
    async submitReview() {
      if (!this.reviewTarget || !this.reviewTarget.id) return
      this.reviewLoading = true
      try {
        await reviewProjectMergeRequest(this.reviewTarget.id, this.reviewForm)
        this.$message.success('评审已提交')
        this.reviewDialogVisible = false
        await this.loadMergeRequests()
      } catch (error) {
        this.$message.error(error.response?.data?.message || '评审提交失败')
      } finally {
        this.reviewLoading = false
      }
    },
    openCheckDialog(row) {
      this.checkTarget = row
      this.checkForm = this.createDefaultCheckForm()
      this.checkDialogVisible = true
    },
    async submitCheck() {
      if (!this.checkTarget || !this.checkTarget.id) return
      this.checkLoading = true
      try {
        await runProjectCheck({
          projectId: Number(this.projectId),
          mergeRequestId: Number(this.checkTarget.id),
          commitId: this.checkTarget.sourceHeadCommitId ? Number(this.checkTarget.sourceHeadCommitId) : undefined,
          checkType: this.checkForm.checkType,
          checkStatus: this.checkForm.checkStatus,
          summary: this.checkForm.summary
        })
        this.$message.success('检查结果已登记')
        this.checkDialogVisible = false
        await this.loadMergeRequests()
      } catch (error) {
        this.$message.error(error.response?.data?.message || '登记检查失败')
      } finally {
        this.checkLoading = false
      }
    },
    async mergeRow(row) {
      try {
        await this.$confirm(`确认把 ${this.branchName(row.sourceBranchId)} 合并到 ${this.branchName(row.targetBranchId)} 吗？`, '提示', {
          type: 'warning'
        })
      } catch (error) {
        return
      }
      try {
        await mergeProjectMergeRequest(row.id)
        this.$message.success('合并完成')
        await this.loadMergeRequests()
        await this.loadBranches()
      } catch (error) {
        this.$message.error(error.response?.data?.message || '合并失败')
      }
    }
  }
}
</script>

<style scoped>
.project-audit-center {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.audit-tip {
  border-radius: 12px;
}

.audit-lane {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.audit-lane-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid #dbe7f7;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.04);
}

.audit-lane-step {
  flex: 0 0 38px;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: #eff6ff;
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.audit-lane-body {
  min-width: 0;
}

.audit-lane-title {
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
}

.audit-lane-desc {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.7;
}

.summary-row {
  margin-bottom: 0;
}

.summary-card {
  height: 100%;
  border-radius: 16px;
  border: 1px solid #e5ecf6;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  padding: 18px;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.04);
}

.summary-card.danger {
  background: #fff7f7;
  border-color: #f3c0c0;
}

.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.summary-label {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}

.summary-note {
  margin-top: 10px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.7;
}

.audit-card {
  border-radius: 18px;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-header-wrap {
  align-items: flex-start;
}

.card-header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-desc {
  font-size: 12px;
  color: #909399;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-select {
  width: 140px;
}

.branch-help {
  margin-top: 12px;
  color: #909399;
  font-size: 12px;
  line-height: 1.8;
}

.mr-title-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mr-title-main {
  font-weight: 600;
  color: #303133;
}

.mr-title-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.mr-title-arrow {
  color: #94a3b8;
  font-size: 12px;
}

.mr-title-sub,
.mr-meta-line {
  font-size: 12px;
  color: #909399;
}

.success-text {
  color: #67c23a;
}

.danger-text {
  color: #f56c6c;
}

@media (max-width: 768px) {
  .audit-lane {
    grid-template-columns: 1fr;
  }

  .toolbar-select {
    width: 100%;
  }
}
</style>
