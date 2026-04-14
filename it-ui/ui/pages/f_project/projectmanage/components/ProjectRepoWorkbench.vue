<template>
  <div class="project-repo-workbench">
    <el-alert
      type="success"
      :closable="false"
      show-icon
      class="repo-tip"
      title="这里是按当前后端接口对齐的仓库工作台。当前已支持：仓库初始化、分支管理、单文件暂存、批量暂存、路径删除暂存、工作区提交、提交历史、提交详情、提交比较、回退提交。ZIP 暂存已在本阶段临时关闭。"
    />

    <el-card shadow="never" class="repo-header-card">
      <div class="repo-header">
        <div class="repo-header-left">
          <div class="repo-title">项目仓库工作台</div>
          <div class="repo-meta">
            <span>仓库：{{ repository ? '已初始化' : '未初始化' }}</span>
            <span>当前分支：{{ currentBranchName }}</span>
            <span>工作区变更：{{ workspaceChangeRows.length }}</span>
            <span>提交数：{{ commitList.length }}</span>
          </div>
        </div>
        <div class="repo-header-right">
          <el-select
            v-model="currentBranchId"
            size="small"
            placeholder="选择分支"
            style="width: 180px"
            @change="handleBranchChange"
          >
            <el-option
              v-for="item in branchList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-button size="small" @click="goToAuditCenter">审核中心</el-button>
          <el-button size="small" @click="refreshAll">刷新</el-button>
          <el-button
            size="small"
            type="primary"
            :disabled="!canManageProject"
            @click="createBranchDialogVisible = true"
          >新建分支</el-button>
        </div>
      </div>
    </el-card>

    <div class="repo-journey-strip">
      <div
        v-for="item in workflowSteps"
        :key="item.key"
        class="repo-journey-item"
        :class="{ active: item.active }"
      >
        <div class="repo-journey-step">{{ item.order }}</div>
        <div class="repo-journey-body">
          <div class="repo-journey-title">{{ item.title }}</div>
          <div class="repo-journey-desc">{{ item.desc }}</div>
        </div>
      </div>
    </div>

    <div class="repo-summary-grid">
      <div
        v-for="card in summaryCards"
        :key="card.key"
        class="repo-summary-card"
        :class="'tone-' + card.tone"
      >
        <div class="repo-summary-label">{{ card.label }}</div>
        <div class="repo-summary-value">{{ card.value }}</div>
        <div class="repo-summary-desc">{{ card.desc }}</div>
      </div>
    </div>

    <div v-if="repoWarnings.length" class="repo-warning-stack">
      <el-alert
        v-for="warning in repoWarnings"
        :key="warning.key"
        :type="warning.type"
        :title="warning.title"
        :description="warning.desc"
        :closable="false"
        show-icon
        class="repo-warning-alert"
      />
    </div>

    <el-row :gutter="16" class="repo-main-row">
      <el-col :xs="24" :md="12" :xl="7">
        <el-card shadow="never" class="work-card upload-card">
          <div slot="header" class="card-header">工作区操作</div>

          <div class="section-title">暂存单个文件</div>
          <el-input
            v-model.trim="stageForm.canonicalPath"
            size="small"
            placeholder="例如：/src/views/Home.vue"
            class="section-gap"
          />
          <el-upload
            class="upload-block"
            drag
            action="#"
            :auto-upload="false"
            :show-file-list="true"
            :on-change="handleFileChange"
            :before-upload="preventAutoUpload"
            :limit="1"
          >
            <i class="el-icon-upload" />
            <div class="el-upload__text">拖拽文件到这里，或<em>点击上传</em></div>
            <div slot="tip" class="el-upload__tip">当前后端只支持单文件暂存，上传后点击“加入工作区”。</div>
          </el-upload>
          <el-button
            class="section-gap"
            size="small"
            type="primary"
            :loading="stageFileLoading"
            :disabled="!canStageFile"
            @click="handleStageFile"
          >加入工作区</el-button>

          <div class="section-title section-gap-lg">批量暂存文件</div>
          <el-input
            v-model.trim="batchStageForm.targetDir"
            size="small"
            placeholder="可选：统一放到哪个目录，例如 /src/views/modules"
          />
          <el-upload
            ref="batchUpload"
            class="upload-block section-gap"
            drag
            action="#"
            :auto-upload="false"
            :show-file-list="true"
            :multiple="true"
            :on-change="handleBatchFileChange"
            :on-remove="handleBatchFileRemove"
            :before-upload="preventAutoUpload"
            @dragover.native.prevent
            @drop.native.prevent.stop="handleBatchDrop"
          >
            <i class="el-icon-folder-opened" />
            <div class="el-upload__text">拖拽多个文件到这里，或<em>点击上传</em></div>
            <div slot="tip" class="el-upload__tip">可一次把多个文件加入工作区；若填写目标目录，会统一追加到该目录下。</div>
          </el-upload>
          <input
            ref="batchFolderInput"
            class="folder-input"
            type="file"
            multiple
            webkitdirectory
            directory
            @change="handleFolderInputChange"
          >
          <div class="batch-upload-actions">
            <el-button size="mini" plain @click="openFolderPicker">选择文件夹</el-button>
            <span>选择或拖入文件夹时会保留内部目录结构</span>
          </div>
          <div class="batch-stage-meta">
            已选 {{ pendingBatchFiles.length }} 个文件
          </div>
          <el-button
            class="section-gap"
            size="small"
            type="primary"
            :loading="stageBatchLoading"
            :disabled="!canStageBatch"
            @click="handleStageBatch"
          >批量加入工作区</el-button>

          <div class="section-title section-gap-lg">暂存删除路径</div>
          <el-input
            v-model.trim="deletePath"
            size="small"
            placeholder="例如：/src/views/OldPage.vue"
          />
          <el-button
            class="section-gap"
            size="small"
            type="danger"
            plain
            :loading="deletePathLoading"
            :disabled="!canDeletePath"
            @click="handleStageDelete"
          >删除路径并加入工作区</el-button>

          <div class="section-title section-gap-lg">ZIP 导入</div>
          <el-alert
            type="warning"
            :closable="false"
            show-icon
            title="ZIP 导入已在 Phase 0 临时关闭"
            description="当前阶段先统一仓库目标路径和工作区规则，避免 ZIP 按压缩包内部路径直接落到错误位置。请先改用单文件或批量上传。"
          />

          <div class="repo-help section-gap-lg">
            <div>1. 先初始化仓库，再创建或选择分支。</div>
            <div>2. 当前工作区支持：单文件暂存、批量暂存、路径删除、提交工作区。</div>
            <div>3. 提交后会生成新提交历史，不会直接覆盖正式版本。</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12" :xl="8">
        <el-card shadow="never" class="work-card diff-card">
          <div slot="header" class="card-header">工作区变更与风险</div>
          <div class="workspace-insight-grid">
            <div
              v-for="card in workspaceInsightCards"
              :key="card.key"
              class="workspace-insight-card"
              :class="card.tone ? 'tone-' + card.tone : ''"
            >
              <div class="workspace-insight-label">{{ card.label }}</div>
              <div class="workspace-insight-value">{{ card.value }}</div>
              <div class="workspace-insight-desc">{{ card.desc }}</div>
            </div>
          </div>
          <el-table
            :data="workspaceChangeRows"
            size="small"
            border
            stripe
            height="280"
            v-loading="workspaceLoading"
          >
            <el-table-column prop="changeType" label="变更类型" width="100">
              <template slot-scope="scope">
                <el-tag size="mini" :type="changeTypeTag(scope.row.changeType)">
                  {{ scope.row.changeType || '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="路径" min-width="180" show-overflow-tooltip>
              <template slot-scope="scope">
                <div class="workspace-path-cell">
                  <div class="workspace-path-main">{{ workspacePath(scope.row) }}</div>
                  <div class="workspace-path-tags">
                    <el-tag v-if="scope.row.conflictFlag" size="mini" type="danger" effect="plain">冲突</el-tag>
                    <el-tag v-else-if="isWorkspaceItemAtRisk(scope.row)" size="mini" type="warning" effect="plain">基线落后</el-tag>
                    <el-tag v-else-if="scope.row.stagedFlag" size="mini" type="success" effect="plain">已在工作区</el-tag>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template slot-scope="scope">
                <el-tag size="mini" :type="workspaceStatusType(scope.row)">
                  {{ workspaceStatusText(scope.row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="detectedMessage" label="说明" min-width="120" show-overflow-tooltip />
          </el-table>

          <div class="commit-box">
            <div class="section-title">提交本次工作区变更</div>
            <el-input
              v-model.trim="commitForm.message"
              size="small"
              placeholder="例如：修复登录页按钮状态问题"
              maxlength="200"
              show-word-limit
            />
            <el-button
              class="section-gap"
              size="small"
              type="primary"
              :loading="commitLoading"
              :disabled="!canCommit"
              @click="handleCommit"
            >提交当前分支</el-button>
            <div class="commit-meta">
              当前工作区：{{ workspace ? '已创建' : '未创建' }}
              <span v-if="workspaceBaseCommitLabel"> · 基线 {{ workspaceBaseCommitLabel }}</span>
              <span v-if="branchHeadCommitLabel"> · 分支 Head {{ branchHeadCommitLabel }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="24" :xl="9">
        <el-card shadow="never" class="work-card history-card">
          <div slot="header" class="card-header">提交历史</div>
          <div class="compare-bar">
            <el-select v-model="compareFromCommitId" size="small" placeholder="选择起始提交" clearable>
              <el-option v-for="item in commitList" :key="'f-' + item.id" :label="commitOptionLabel(item)" :value="item.id" />
            </el-select>
            <el-select v-model="compareToCommitId" size="small" placeholder="选择目标提交" clearable>
              <el-option v-for="item in commitList" :key="'t-' + item.id" :label="commitOptionLabel(item)" :value="item.id" />
            </el-select>
            <el-button size="small" :disabled="!canCompare" @click="handleCompare">比较提交</el-button>
          </div>

          <el-table
            :data="commitList"
            size="small"
            border
            stripe
            height="280"
            v-loading="commitListLoading"
            @row-click="handleSelectCommit"
          >
            <el-table-column prop="commitNo" label="#" width="70" />
            <el-table-column prop="displaySha" label="SHA" width="110" show-overflow-tooltip />
            <el-table-column label="提交说明" min-width="240">
              <template slot-scope="scope">
                <div class="commit-message-cell">
                  <div class="commit-message-main">{{ commitPrimaryText(scope.row) }}</div>
                  <div v-if="commitSecondaryText(scope.row)" class="commit-message-sub">{{ commitSecondaryText(scope.row) }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="170" />
            <el-table-column label="操作" width="100" fixed="right">
              <template slot-scope="scope">
                <el-button type="text" size="mini" @click.stop="handleRollback(scope.row)">回退</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="repo-bottom-row repo-insight-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="work-card detail-card">
          <div slot="header" class="card-header">工作区差异摘要</div>
          <div v-if="workspaceDiffGroups.length" class="workspace-group-list">
            <div
              v-for="group in workspaceDiffGroups"
              :key="group.key"
              class="workspace-group-item"
              :class="group.tone ? 'tone-' + group.tone : ''"
            >
              <div class="workspace-group-head">
                <div class="workspace-group-title">{{ group.label }}</div>
                <el-tag size="mini" effect="plain">{{ group.count }}</el-tag>
              </div>
              <div class="workspace-group-desc">{{ group.desc }}</div>
              <div class="workspace-group-paths">
                <div
                  v-for="path in group.paths"
                  :key="group.key + '-' + path"
                  class="workspace-group-path"
                >
                  {{ path }}
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="当前工作区还没有差异项" :image-size="90" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="work-card compare-card">
          <div slot="header" class="card-header">操作日志</div>
          <div v-if="operationLogs.length" class="repo-log-list">
            <div
              v-for="item in operationLogs"
              :key="item.id"
              class="repo-log-item"
              :class="item.tone ? 'tone-' + item.tone : ''"
            >
              <div class="repo-log-head">
                <div class="repo-log-title">{{ item.title }}</div>
                <div class="repo-log-time">{{ formatOperationTime(item.time) }}</div>
              </div>
              <div class="repo-log-desc">{{ item.desc }}</div>
            </div>
          </div>
          <el-empty v-else description="先执行一次工作区操作，这里会沉淀本次工作台日志" :image-size="90" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="repo-bottom-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="work-card detail-card">
          <div slot="header" class="card-header">当前提交详情</div>
          <div v-if="selectedCommitDetail" class="detail-box">
            <div class="detail-item"><span>提交号：</span>{{ selectedCommitDetail.commitNo || '-' }}</div>
            <div class="detail-item"><span>SHA：</span>{{ selectedCommitDetail.displaySha || '-' }}</div>
            <div class="detail-item"><span>说明：</span>{{ commitPrimaryText(selectedCommitDetail) }}</div>
            <div v-if="selectedCommitDetail.message && selectedCommitDetail.message !== commitPrimaryText(selectedCommitDetail)" class="detail-item"><span>原始说明：</span>{{ selectedCommitDetail.message }}</div>
            <div class="detail-item"><span>类型：</span>{{ commitTypeLabel(selectedCommitDetail.commitType) }}</div>
            <div class="detail-item"><span>提交人：</span>{{ selectedCommitDetail.operatorId || '-' }}</div>
            <div class="detail-item"><span>时间：</span>{{ selectedCommitDetail.createdAt || '-' }}</div>
            <div class="detail-item"><span>变更文件数：</span>{{ selectedCommitDetail.changedFileCount || 0 }}</div>
          </div>
          <el-empty v-else description="点击右侧提交历史查看详情" :image-size="90" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="work-card compare-card">
          <div slot="header" class="card-header">提交变更摘要</div>
          <div v-if="compareResult" class="compare-result-shell">
            <div class="compare-stat-grid">
              <div class="compare-stat-card tone-blue">
                <div class="compare-stat-label">新增</div>
                <div class="compare-stat-value">{{ compareResult.addCount || 0 }}</div>
              </div>
              <div class="compare-stat-card tone-purple">
                <div class="compare-stat-label">修改</div>
                <div class="compare-stat-value">{{ compareResult.modifyCount || 0 }}</div>
              </div>
              <div class="compare-stat-card tone-danger">
                <div class="compare-stat-label">删除</div>
                <div class="compare-stat-value">{{ compareResult.deleteCount || 0 }}</div>
              </div>
            </div>
            <el-table
              v-if="compareFiles.length"
              :data="compareFiles"
              size="small"
              border
              stripe
              class="compare-file-table"
              max-height="220"
            >
              <el-table-column label="类型" width="90">
                <template slot-scope="scope">
                  <el-tag size="mini" :type="changeTypeTag(scope.row.changeType)">
                    {{ scope.row.changeType || '-' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="path" label="路径" min-width="220" show-overflow-tooltip />
              <el-table-column prop="fromBlobId" label="旧 Blob" width="90" />
              <el-table-column prop="toBlobId" label="新 Blob" width="90" />
            </el-table>
            <div class="compare-raw-toggle">
              <el-button type="text" size="mini" @click="showCompareRaw = !showCompareRaw">
                {{ showCompareRaw ? '收起原始结果' : '查看原始结果' }}
              </el-button>
            </div>
            <div v-if="showCompareRaw" class="compare-box">
              <pre>{{ prettyJson(compareResult) }}</pre>
            </div>
          </div>
          <el-empty v-else description="先在上方选择两条提交再比较" :image-size="90" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog title="新建分支" :visible.sync="createBranchDialogVisible" width="420px">
      <el-form label-width="90px" size="small">
        <el-form-item label="分支名称">
          <el-input v-model.trim="createBranchForm.name" placeholder="例如：feature/login-fix" />
        </el-form-item>
        <el-form-item label="来源分支">
          <el-select v-model="createBranchForm.sourceBranchId" placeholder="选择来源分支" style="width: 100%">
            <el-option
              v-for="item in branchList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分支类型">
          <el-select v-model="createBranchForm.branchType" style="width: 100%">
            <el-option label="feature" value="feature" />
            <el-option label="hotfix" value="hotfix" />
            <el-option label="release" value="release" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="createBranchDialogVisible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="createBranchLoading" @click="handleCreateBranch">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  initProjectRepository,
  getProjectRepository
} from '@/api/projectRepository'
import {
  listProjectBranches,
  createProjectBranch
} from '@/api/projectBranch'
import {
  getCurrentWorkspace,
  getWorkspaceItems,
  stageWorkspaceFile,
  stageWorkspaceBatch,
  stageWorkspaceDelete,
  commitWorkspace
} from '@/api/projectWorkspace'
import {
  listProjectCommits,
  getProjectCommitDetail,
  compareProjectCommits,
  rollbackToCommit
} from '@/api/projectCommit'

export default {
  name: 'ProjectRepoWorkbench',
  props: {
    projectId: {
      type: [Number, String],
      required: true
    },
    project: {
      type: Object,
      default: null
    },
    canManageProject: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      initializing: false,
      repository: null,
      branchList: [],
      currentBranchId: null,
      workspace: null,
      workspaceItems: [],
      workspaceChanges: [],
      commitList: [],
      selectedCommitDetail: null,
      compareFromCommitId: null,
      compareToCommitId: null,
      compareResult: null,
      showCompareRaw: false,
      operationLogs: [],
      stageForm: {
        canonicalPath: ''
      },
      batchStageForm: {
        targetDir: ''
      },
      deletePath: '',
      pendingFile: null,
      pendingBatchFiles: [],
      pendingZipFile: null,
      commitForm: {
        message: ''
      },
      createBranchDialogVisible: false,
      createBranchLoading: false,
      createBranchForm: {
        name: '',
        sourceBranchId: null,
        branchType: 'feature'
      },
      workspaceLoading: false,
      stageFileLoading: false,
      stageBatchLoading: false,
      stageZipLoading: false,
      deletePathLoading: false,
      commitLoading: false,
      commitListLoading: false
    }
  },
  computed: {
    currentBranch() {
      return this.branchList.find(v => String(v.id) === String(this.currentBranchId)) || null
    },
    currentBranchName() {
      return this.currentBranch ? this.currentBranch.name : '未选择'
    },
    currentBranchHeadCommitId() {
      return this.currentBranch && this.currentBranch.headCommitId ? this.currentBranch.headCommitId : null
    },
    workspaceChangeRows() {
      if (Array.isArray(this.workspaceChanges) && this.workspaceChanges.length) {
        return this.workspaceChanges
      }
      return Array.isArray(this.workspaceItems) ? this.workspaceItems : []
    },
    workspaceBaseCommitLabel() {
      return this.resolveCommitDisplay(this.workspace && this.workspace.baseCommitId)
    },
    branchHeadCommitLabel() {
      return this.resolveCommitDisplay(this.currentBranchHeadCommitId)
    },
    isDirectCommitBlocked() {
      return !!(this.currentBranch && this.currentBranch.protectedFlag && !this.currentBranch.allowDirectCommitFlag)
    },
    conflictItemCount() {
      return this.workspaceItems.filter(item => item && item.conflictFlag).length
    },
    riskItemCount() {
      return this.workspaceItems.filter(item => item && (item.conflictFlag || this.isWorkspaceItemAtRisk(item))).length
    },
    isWorkspaceBaseBehind() {
      if (!this.workspace || !this.workspace.baseCommitId || !this.currentBranchHeadCommitId || !this.workspaceChangeRows.length) {
        return false
      }
      return String(this.workspace.baseCommitId) !== String(this.currentBranchHeadCommitId)
    },
    repoWarnings() {
      const warnings = []
      if (this.isDirectCommitBlocked) {
        warnings.push({
          key: 'protected-branch',
          type: 'warning',
          title: '当前分支受保护，不允许直接提交',
          desc: '建议先在功能分支整理工作区变更，再去审核中心发起 MR，走检查和合并流程。'
        })
      }
      if (this.conflictItemCount > 0) {
        warnings.push({
          key: 'workspace-conflict',
          type: 'error',
          title: `工作区里有 ${this.conflictItemCount} 项冲突标记`,
          desc: '这些文件需要先处理冲突或重新整理后再提交，避免把不一致内容写入新提交。'
        })
      } else if (this.isWorkspaceBaseBehind) {
        warnings.push({
          key: 'workspace-base-behind',
          type: 'warning',
          title: '工作区基线落后于当前分支 Head',
          desc: '分支最新提交已经前进，当前工作区仍基于旧基线整理，建议先核对差异再继续提交。'
        })
      }
      return warnings
    },
    workspaceInsightCards() {
      const addCount = this.workspaceChangeRows.filter(item => item && item.changeType === 'ADD').length
      const modifyCount = this.workspaceChangeRows.filter(item => item && ['MODIFY', 'MOVE', 'RENAME', 'REVERT'].includes(item.changeType)).length
      const deleteCount = this.workspaceChangeRows.filter(item => item && item.changeType === 'DELETE').length
      return [
        {
          key: 'pending',
          label: '待整理差异',
          value: this.workspaceChangeRows.length,
          desc: this.workspaceChangeRows.length ? '当前工作区已经收集到可提交的文件差异。' : '还没有工作区差异，可以先上传文件或暂存删除路径。',
          tone: 'blue'
        },
        {
          key: 'add',
          label: '新增文件',
          value: addCount,
          desc: addCount ? '这些文件会作为新增内容进入下一次提交。' : '当前没有新增文件。',
          tone: 'cyan'
        },
        {
          key: 'modify',
          label: '修改 / 删除',
          value: modifyCount + deleteCount,
          desc: modifyCount + deleteCount ? `包含 ${modifyCount} 项修改与 ${deleteCount} 项删除。` : '当前没有修改或删除项。',
          tone: 'purple'
        },
        {
          key: 'risk',
          label: '风险提示',
          value: this.riskItemCount,
          desc: this.riskItemCount ? '这里聚合了冲突标记或基线落后带来的风险项。' : '当前没有发现明显风险项。',
          tone: this.riskItemCount ? 'danger' : 'orange'
        }
      ]
    },
    workspaceDiffGroups() {
      const groups = []
      const definitions = [
        {
          key: 'ADD',
          label: '新增文件',
          desc: '这些文件会被作为新内容写入下一次提交。',
          tone: 'blue'
        },
        {
          key: 'MODIFY',
          label: '修改内容',
          desc: '这些文件已有历史版本，本次工作区会覆盖为新内容。',
          tone: 'purple'
        },
        {
          key: 'DELETE',
          label: '删除路径',
          desc: '这些路径会在下一次提交中被标记为删除。',
          tone: 'danger'
        }
      ]
      definitions.forEach(definition => {
        const items = this.workspaceChangeRows.filter(item => {
          if (!item) return false
          if (definition.key === 'MODIFY') {
            return ['MODIFY', 'MOVE', 'RENAME', 'REVERT'].includes(item.changeType)
          }
          return item.changeType === definition.key
        })
        if (!items.length) return
        groups.push({
          key: definition.key,
          label: definition.label,
          desc: definition.desc,
          count: items.length,
          tone: definition.tone,
          paths: items.slice(0, 5).map(item => this.workspacePath(item))
        })
      })
      const riskItems = this.workspaceItems.filter(item => item && (item.conflictFlag || this.isWorkspaceItemAtRisk(item)))
      if (riskItems.length) {
        groups.unshift({
          key: 'RISK',
          label: '需要留意的风险项',
          desc: this.conflictItemCount ? '这些文件已经带有冲突标记或需要重新核对基线。' : '这些文件所在工作区基线已落后，继续提交前建议先复核。',
          count: riskItems.length,
          tone: 'danger',
          paths: riskItems.slice(0, 5).map(item => this.workspacePath(item))
        })
      }
      return groups
    },
    compareFiles() {
      if (!this.compareResult || !Array.isArray(this.compareResult.files)) {
        return []
      }
      return this.compareResult.files
    },
    summaryCards() {
      return [
        {
          key: 'repository',
          label: '仓库状态',
          value: this.repository ? '已初始化' : '待初始化',
          desc: this.repository ? '仓库主线已启用，可以继续分支与提交操作。' : '初始化后才能使用工作区、分支和提交能力。',
          tone: 'blue'
        },
        {
          key: 'branch',
          label: '当前分支',
          value: this.currentBranchName,
          desc: this.currentBranchId ? `已选择分支 #${this.currentBranchId}` : '先选择一个分支作为当前开发上下文。',
          tone: 'cyan'
        },
        {
          key: 'workspace',
          label: '工作区变更',
          value: this.workspaceChangeRows.length,
          desc: this.workspaceChangeRows.length ? '已有变更可整理为一次 Commit。' : '当前工作区为空，可以先上传文件、批量文件或暂存删除路径。',
          tone: 'purple'
        },
        {
          key: 'commit',
          label: '提交历史',
          value: this.commitList.length,
          desc: this.commitList.length ? '当前分支已经有历史提交，可直接查看与比较。' : '当前分支还没有提交记录。',
          tone: 'orange'
        }
      ]
    },
    workflowSteps() {
      return [
        {
          key: 'workspace',
          order: '01',
          title: '上传到工作区',
          desc: '单文件、批量文件和删除路径都先暂存，不直接改正式版本。',
          active: !!this.currentBranchId
        },
        {
          key: 'commit',
          order: '02',
          title: '写 Commit',
          desc: this.workspaceChangeRows.length ? '当前已有工作区变更，可以直接写提交说明。' : '先把文件变更加入工作区，再生成 Commit。',
          active: this.workspaceChangeRows.length > 0 || this.commitList.length > 0
        },
        {
          key: 'branch',
          order: '03',
          title: '保留在分支',
          desc: this.currentBranchId ? `当前开发分支是 ${this.currentBranchName}。` : '选择一个分支承载当前变更。',
          active: !!this.currentBranchId
        },
        {
          key: 'review',
          order: '04',
          title: '送审并合主线',
          desc: '完成提交后前往审核中心创建 MR、评审并合并主线。',
          active: false
        }
      ]
    },
    canStageFile() {
      return !!this.projectId && !!this.currentBranchId && !!this.stageForm.canonicalPath && !!this.pendingFile
    },
    canDeletePath() {
      return !!this.projectId && !!this.currentBranchId && !!this.deletePath
    },
    canStageBatch() {
      return !!this.projectId && !!this.currentBranchId && this.pendingBatchFiles.length > 0
    },
    canStageZip() {
      return false
    },
    canCommit() {
      return !!this.projectId &&
        !!this.currentBranchId &&
        !!this.workspace &&
        this.workspaceChangeRows.length > 0 &&
        !!this.commitForm.message &&
        !this.isDirectCommitBlocked &&
        this.conflictItemCount === 0
    },
    canCompare() {
      return !!this.compareFromCommitId && !!this.compareToCommitId && String(this.compareFromCommitId) !== String(this.compareToCommitId)
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler() {
        this.initWorkbench()
      }
    }
  },
  methods: {
    unwrapResponse(res) {
      const raw = res && Object.prototype.hasOwnProperty.call(res, 'data') ? res.data : res
      if (raw && typeof raw === 'object' && Object.prototype.hasOwnProperty.call(raw, 'code')) {
        return raw.data
      }
      return raw
    },
    getResponseMessage(err, fallback) {
      if (err && err.response && err.response.data) {
        const raw = err.response.data
        if (typeof raw === 'string') {
          return raw
        }
        if (raw.message) {
          return raw.message
        }
      }
      if (err && err.message) {
        return `${fallback || '请求失败'}：${err.message}`
      }
      return fallback || '请求失败'
    },
    describeUploadFileForDebug(file) {
      if (!file) {
        return null
      }
      return {
        name: file.name || '',
        size: file.size || 0,
        type: file.type || '',
        relativePath: this.getBatchRelativePath(file)
      }
    },
    workspaceUploadDebug(step, payload) {
      if (typeof console !== 'undefined' && console.info) {
        console.info('[project-repo-workbench-upload] ' + step, payload)
      }
    },
    workspaceUploadError(step, error) {
      if (typeof console !== 'undefined' && console.error) {
        console.error('[project-repo-workbench-upload] ' + step, {
          message: error && error.message,
          code: error && error.code,
          status: error && error.response && error.response.status,
          response: error && error.response && error.response.data
        })
      }
    },
    async initWorkbench() {
      if (!this.projectId) return
      this.initializing = true
      try {
        await this.ensureRepository()
        await this.loadBranches()
        if (this.currentBranchId) {
          await Promise.all([
            this.loadWorkspace(),
            this.loadCommitList()
          ])
          this.loadOperationLogs()
        } else {
          this.operationLogs = []
        }
      } catch (e) {
        this.$message.error(this.getResponseMessage(e, '仓库工作台初始化失败'))
      } finally {
        this.initializing = false
      }
    },
    async ensureRepository() {
      try {
        const detailRes = await getProjectRepository(this.projectId)
        const repo = this.unwrapResponse(detailRes)
        if (repo && repo.id) {
          this.repository = repo
          return
        }
      } catch (e) {
        if (!this.canManageProject) {
          throw e
        }
      }

      if (!this.canManageProject) {
        return
      }

      await initProjectRepository(this.projectId)
      const detailRes = await getProjectRepository(this.projectId)
      this.repository = this.unwrapResponse(detailRes)
    },
    async loadBranches() {
      const res = await listProjectBranches(this.projectId)
      const list = this.unwrapResponse(res) || []
      this.branchList = Array.isArray(list) ? list : []

      if (this.currentBranchId && !this.branchList.some(item => String(item.id) === String(this.currentBranchId))) {
        this.currentBranchId = null
      }

      if (!this.currentBranchId) {
        if (this.repository && this.repository.defaultBranchId) {
          this.currentBranchId = this.repository.defaultBranchId
        }
        if (!this.currentBranchId && this.branchList.length > 0) {
          this.currentBranchId = this.branchList[0].id
        }
      }

      if (!this.createBranchForm.sourceBranchId && this.currentBranchId) {
        this.createBranchForm.sourceBranchId = this.currentBranchId
      }
    },
    async loadWorkspace() {
      if (!this.projectId || !this.currentBranchId) return
      this.workspaceLoading = true
      try {
        const wsRes = await getCurrentWorkspace(this.projectId, this.currentBranchId)
        const workspace = this.unwrapResponse(wsRes)
        this.workspace = workspace || null
        this.workspaceItems = Array.isArray(workspace && workspace.items) ? workspace.items : []
        this.workspaceChanges = Array.isArray(workspace && workspace.changes) ? workspace.changes : []
        if (!this.workspaceChanges.length) {
          const itemsRes = await getWorkspaceItems(this.projectId, this.currentBranchId)
          const items = this.unwrapResponse(itemsRes)
          this.workspaceChanges = Array.isArray(items) ? items : []
        }
      } finally {
        this.workspaceLoading = false
      }
    },
    async loadCommitList() {
      if (!this.projectId || !this.currentBranchId) return
      this.commitListLoading = true
      try {
        const res = await listProjectCommits(this.projectId, this.currentBranchId)
        const list = this.unwrapResponse(res)
        this.commitList = Array.isArray(list) ? list : []
      } finally {
        this.commitListLoading = false
      }
    },
    async refreshAll() {
      await this.ensureRepository()
      await this.loadBranches()
      if (this.currentBranchId) {
        this.loadOperationLogs()
        await this.loadWorkspace()
        await this.loadCommitList()
      } else {
        this.workspace = null
        this.workspaceItems = []
        this.workspaceChanges = []
        this.commitList = []
        this.operationLogs = []
      }
    },
    async handleBranchChange() {
      this.selectedCommitDetail = null
      this.compareResult = null
      this.showCompareRaw = false
      await this.loadWorkspace()
      await this.loadCommitList()
      this.loadOperationLogs()
      this.appendOperationLog('切换分支', `已切换到 ${this.currentBranchName}，工作区与提交历史已刷新。`, 'info')
    },
    preventAutoUpload() {
      return false
    },
    normalizeBatchRelativePath(value) {
      const parts = String(value || '')
        .replace(/\\/g, '/')
        .replace(/^[A-Za-z]:\/+/, '')
        .replace(/^\/+/, '')
        .split('/')
        .map(item => item.trim())
        .filter(Boolean)
        .filter(item => item !== '.')

      if (parts.some(item => item === '..')) {
        return ''
      }

      return parts.join('/')
    },
    shouldSkipBatchPath(relativePath) {
      const normalizedPath = this.normalizeBatchRelativePath(relativePath)
      if (!normalizedPath) return true

      const ignoredSegments = new Set([
        '.git', '.svn', '.hg',
        '.idea', '.vscode',
        'node_modules', 'target', 'dist', 'build', 'out',
        '.gradle', '.next', '.nuxt', '.cache', 'coverage',
        '__MACOSX'
      ])
      const ignoredFileNames = new Set([
        '.ds_store', 'thumbs.db', 'desktop.ini',
        'npm-debug.log', 'yarn-error.log', 'pnpm-debug.log'
      ])
      const ignoredExtensions = new Set([
        'class', 'pyc', 'pyo', 'o', 'obj', 'tmp', 'swp', 'log', 'iml'
      ])

      const parts = normalizedPath.split('/').filter(Boolean)
      if (parts.some(part => ignoredSegments.has(part.toLowerCase()))) {
        return true
      }

      const fileName = (parts[parts.length - 1] || '').toLowerCase()
      if (ignoredFileNames.has(fileName)) {
        return true
      }

      const extIndex = fileName.lastIndexOf('.')
      const ext = extIndex >= 0 ? fileName.slice(extIndex + 1) : ''
      return ignoredExtensions.has(ext)
    },
    attachBatchRelativePath(file, relativePath) {
      if (!file) return null
      const normalizedPath = this.normalizeBatchRelativePath(relativePath || file.webkitRelativePath || file.relativePath || file.name)
      if (this.shouldSkipBatchPath(normalizedPath)) return null
      if (normalizedPath) {
        Object.defineProperty(file, '__relativePath', {
          value: normalizedPath,
          configurable: true
        })
      }
      return file
    },
    getBatchRelativePath(file) {
      return this.normalizeBatchRelativePath(
        (file && (file.__relativePath || file.webkitRelativePath || file.relativePath || file.name)) || ''
      )
    },
    buildBatchCanonicalPath(targetDir, relativePath) {
      const target = this.normalizeBatchRelativePath(targetDir)
      const relative = this.normalizeBatchRelativePath(relativePath)
      return `/${[target, relative].filter(Boolean).join('/')}`
    },
    getBatchFileDedupKey(file) {
      if (!file) return ''
      const keyPath = this.getBatchRelativePath(file)
      return `${keyPath || file.name || ''}:${file.size || 0}:${file.lastModified || 0}`
    },
    addPendingBatchFiles(files) {
      const current = Array.isArray(this.pendingBatchFiles) ? this.pendingBatchFiles : []
      const merged = current.slice()
      const seen = new Set(current.map(file => {
        return this.getBatchFileDedupKey(file)
      }))

      ;(files || []).forEach(file => {
        if (!file) return
        const normalizedFile = this.attachBatchRelativePath(file, this.getBatchRelativePath(file))
        if (!normalizedFile) return
        const key = this.getBatchFileDedupKey(normalizedFile)
        if (!seen.has(key)) {
          seen.add(key)
          merged.push(normalizedFile)
        }
      })

      this.pendingBatchFiles = merged
    },
    openFolderPicker() {
      if (this.$refs.batchFolderInput) {
        this.$refs.batchFolderInput.value = ''
        this.$refs.batchFolderInput.click()
      }
    },
    handleFolderInputChange(event) {
      const files = Array.from((event && event.target && event.target.files) || [])
        .map(file => this.attachBatchRelativePath(file, file.webkitRelativePath || file.name))
        .filter(Boolean)
      if (files.length) {
        this.addPendingBatchFiles(files)
      }
      if (event && event.target) {
        event.target.value = ''
      }
    },
    async handleBatchDrop(event) {
      if (event && typeof event.preventDefault === 'function') event.preventDefault()
      if (event && typeof event.stopPropagation === 'function') event.stopPropagation()
      const files = await this.collectDroppedFiles(event && event.dataTransfer)
      if (files.length) {
        this.addPendingBatchFiles(files)
      }
      if (this.$refs.batchUpload) {
        this.$refs.batchUpload.clearFiles()
      }
    },
    async collectDroppedFiles(dataTransfer) {
      if (!dataTransfer) return []

      const items = Array.from(dataTransfer.items || [])
      const entries = items
        .map(item => (item && typeof item.webkitGetAsEntry === 'function' ? item.webkitGetAsEntry() : null))
        .filter(Boolean)

      if (entries.length) {
        const grouped = await Promise.all(entries.map(entry => this.collectEntryFiles(entry, '')))
        return grouped.flat().filter(Boolean)
      }

      return Array.from(dataTransfer.files || [])
        .map(file => this.attachBatchRelativePath(file, file.webkitRelativePath || file.name))
        .filter(Boolean)
    },
    async collectEntryFiles(entry, parentPath) {
      if (!entry) return []

      if (entry.isFile) {
        return new Promise(resolve => {
          entry.file(file => {
            const relativePath = parentPath ? `${parentPath}/${file.name}` : file.name
            resolve([this.attachBatchRelativePath(file, relativePath)])
          }, () => resolve([]))
        })
      }

      if (!entry.isDirectory) {
        return []
      }

      const nextParentPath = parentPath ? `${parentPath}/${entry.name}` : entry.name
      const children = await this.readDirectoryEntries(entry.createReader())
      const grouped = await Promise.all(children.map(child => this.collectEntryFiles(child, nextParentPath)))
      return grouped.flat().filter(Boolean)
    },
    readDirectoryEntries(reader) {
      return new Promise(resolve => {
        const entries = []
        const readNext = () => {
          reader.readEntries(batch => {
            if (!batch.length) {
              resolve(entries)
              return
            }
            entries.push(...batch)
            readNext()
          }, () => resolve(entries))
        }
        readNext()
      })
    },
    handleFileChange(file) {
      this.pendingFile = file && file.raw ? file.raw : null
    },
    handleBatchFileChange(file, fileList) {
      const nextFileList = Array.isArray(fileList) && fileList.length ? fileList : (file ? [file] : [])
      const selectedFiles = nextFileList
        .map(item => {
          const rawFile = item && item.raw ? item.raw : null
          if (!rawFile || (rawFile.size === 0 && !String(rawFile.name || '').includes('.'))) {
            return null
          }
          return this.attachBatchRelativePath(rawFile, rawFile && (rawFile.webkitRelativePath || rawFile.name))
        })
        .filter(Boolean)
      if (selectedFiles.length) {
        this.addPendingBatchFiles(selectedFiles)
      }
      this.workspaceUploadDebug('batch-files:selected', {
        projectId: this.projectId,
        branchId: this.currentBranchId,
        fileCount: this.pendingBatchFiles.length,
        files: this.pendingBatchFiles.slice(0, 20).map(file => this.describeUploadFileForDebug(file))
      })
    },
    handleBatchFileRemove(file, fileList) {
      const rawFile = file && file.raw ? file.raw : file
      const normalizedFile = rawFile
        ? this.attachBatchRelativePath(rawFile, rawFile.webkitRelativePath || rawFile.relativePath || rawFile.__relativePath || rawFile.name)
        : null
      const removedKey = this.getBatchFileDedupKey(normalizedFile)
      if (removedKey) {
        this.pendingBatchFiles = (this.pendingBatchFiles || []).filter(item => this.getBatchFileDedupKey(item) !== removedKey)
      }
      this.workspaceUploadDebug('batch-files:removed', {
        projectId: this.projectId,
        branchId: this.currentBranchId,
        fileCount: this.pendingBatchFiles.length,
        files: this.pendingBatchFiles.slice(0, 20).map(item => this.describeUploadFileForDebug(item))
      })
    },
    handleZipChange(file) {
      this.pendingZipFile = file && file.raw ? file.raw : null
    },
    async handleStageFile() {
      if (!this.canStageFile) {
        this.$message.warning('请先填写规范路径并选择文件')
        return
      }
      this.stageFileLoading = true
      try {
        const stagedPath = this.stageForm.canonicalPath
        const fileName = this.pendingFile && this.pendingFile.name ? this.pendingFile.name : '上传文件'
        this.workspaceUploadDebug('stage-file:submit', {
          projectId: this.projectId,
          branchId: this.currentBranchId,
          canonicalPath: stagedPath,
          file: this.describeUploadFileForDebug(this.pendingFile)
        })
        await stageWorkspaceFile(this.projectId, this.currentBranchId, this.stageForm.canonicalPath, this.pendingFile)
        this.workspaceUploadDebug('stage-file:done', {
          canonicalPath: stagedPath,
          fileName
        })
        this.$message.success('已加入工作区')
        this.stageForm.canonicalPath = ''
        this.pendingFile = null
        await this.loadWorkspace()
        this.appendOperationLog('单文件加入工作区', `${fileName} 已按 ${stagedPath} 暂存到当前工作区。`, 'success')
      } catch (e) {
        this.workspaceUploadError('stage-file:failed', e)
        this.appendOperationLog('Single upload failed', this.getResponseMessage(e, 'Single upload failed'), 'danger')
        this.$message.error(this.getResponseMessage(e, '加入工作区失败'))
      } finally {
        this.stageFileLoading = false
      }
    },
    async handleStageBatch() {
      if (!this.canStageBatch) {
        this.$message.warning('请先选择要批量加入工作区的文件')
        return
      }
      this.stageBatchLoading = true
      try {
        const stagedCount = this.pendingBatchFiles.length
        const targetDir = this.batchStageForm.targetDir
        const relativePaths = this.pendingBatchFiles.map(file => this.getBatchRelativePath(file))
        this.workspaceUploadDebug('stage-batch:submit', {
          projectId: this.projectId,
          branchId: this.currentBranchId,
          targetDir,
          fileCount: stagedCount,
          relativePaths: relativePaths.slice(0, 20)
        })
        const response = await stageWorkspaceBatch(
          this.projectId,
          this.currentBranchId,
          this.pendingBatchFiles,
          targetDir,
          relativePaths
        )
        const stagedItems = this.unwrapResponse(response)
        this.workspaceUploadDebug('stage-batch:done', {
          requestedCount: stagedCount,
          returnedCount: Array.isArray(stagedItems) ? stagedItems.length : null,
          result: stagedItems
        })
        this.$message.success(`已批量加入工作区，共 ${stagedCount} 个文件`)
        this.pendingBatchFiles = []
        this.batchStageForm.targetDir = ''
        if (this.$refs.batchUpload) {
          this.$refs.batchUpload.clearFiles()
        }
        await this.loadWorkspace()
        this.appendOperationLog('批量加入工作区', `已批量暂存 ${stagedCount} 个文件${targetDir ? `，目标目录为 ${targetDir}` : ''}。`, 'success')
      } catch (e) {
        this.workspaceUploadError('stage-batch:failed', e)
        this.appendOperationLog('Batch upload failed', this.getResponseMessage(e, 'Batch upload failed'), 'danger')
        this.$message.error(this.getResponseMessage(e, '批量加入工作区失败'))
      } finally {
        this.stageBatchLoading = false
      }
    },
    async handleStageZip() {
      this.$message.warning('ZIP 导入已在当前阶段临时关闭，请改用单文件或批量上传')
    },
    async handleStageDelete() {
      if (!this.canDeletePath) {
        this.$message.warning('请先填写要删除的规范路径')
        return
      }
      this.deletePathLoading = true
      try {
        const deletedPath = this.deletePath
        await stageWorkspaceDelete(this.projectId, this.currentBranchId, this.deletePath)
        this.$message.success('删除路径已加入工作区')
        this.deletePath = ''
        await this.loadWorkspace()
        this.appendOperationLog('暂存删除路径', `${deletedPath} 已加入当前工作区，后续提交会按删除处理。`, 'warning')
      } catch (e) {
        this.$message.error(this.getResponseMessage(e, '删除路径暂存失败'))
      } finally {
        this.deletePathLoading = false
      }
    },
    async handleCommit() {
      if (!this.canCommit) {
        this.$message.warning('请先填写提交说明，并确保工作区存在变更')
        return
      }
      this.commitLoading = true
      try {
        const commitMessage = this.commitForm.message
        await commitWorkspace({
          projectId: Number(this.projectId),
          branchId: Number(this.currentBranchId),
          message: this.commitForm.message
        })
        this.$message.success('提交成功')
        this.commitForm.message = ''
        this.selectedCommitDetail = null
        this.compareResult = null
        this.showCompareRaw = false
        await this.refreshAll()
        this.appendOperationLog('工作区已提交', `已在 ${this.currentBranchName} 提交一次工作区变更：${commitMessage}。`, 'success')
      } catch (e) {
        const errorMessage = this.getResponseMessage(e, '提交失败')
        try {
          await this.loadBranches()
          await this.loadWorkspace()
          await this.loadCommitList()
        } catch (refreshError) {
          // keep the original submit error as the primary feedback
        }
        this.$message.error(errorMessage)
      } finally {
        this.commitLoading = false
      }
    },
    async handleSelectCommit(row) {
      if (!row || !row.id) return
      try {
        const res = await getProjectCommitDetail(row.id)
        this.selectedCommitDetail = this.normalizeCommitDetail(this.unwrapResponse(res))
        this.appendOperationLog('查看提交详情', `已查看提交 ${this.resolveCommitDisplay(row.id) || row.displaySha || '#' + row.id} 的详情。`, 'info')
      } catch (e) {
        this.$message.error(this.getResponseMessage(e, '获取提交详情失败'))
      }
    },
    async handleCompare() {
      if (!this.canCompare) {
        this.$message.warning('请选择两条不同的提交再比较')
        return
      }
      try {
        const res = await compareProjectCommits(this.compareFromCommitId, this.compareToCommitId)
        this.compareResult = this.unwrapResponse(res)
        this.showCompareRaw = false
        this.appendOperationLog(
          '比较提交差异',
          `已比较 ${this.resolveCommitDisplay(this.compareFromCommitId)} 与 ${this.resolveCommitDisplay(this.compareToCommitId)} 的改动。`,
          'info'
        )
      } catch (e) {
        this.$message.error(this.getResponseMessage(e, '提交比较失败'))
      }
    },
    async handleRollback(row) {
      if (!row || !row.id) return
      try {
        await this.$confirm('回退会基于该提交生成新的回退结果，是否继续？', '提示', {
          type: 'warning'
        })
      } catch (e) {
        return
      }

      try {
        const rollbackLabel = this.resolveCommitDisplay(row.id) || row.displaySha || '#' + row.id
        await rollbackToCommit(row.id)
        this.$message.success('回退成功')
        this.selectedCommitDetail = null
        this.compareResult = null
        this.showCompareRaw = false
        await this.refreshAll()
        this.appendOperationLog('生成回退提交', `已基于 ${rollbackLabel} 生成新的回退结果。`, 'warning')
      } catch (e) {
        this.$message.error(this.getResponseMessage(e, '回退失败'))
      }
    },
    async handleCreateBranch() {
      if (!this.createBranchForm.name) {
        this.$message.warning('请先填写分支名称')
        return
      }
      if (!this.createBranchForm.sourceBranchId) {
        this.$message.warning('请先选择来源分支')
        return
      }
      this.createBranchLoading = true
      try {
        const sourceBranch = this.branchList.find(item => String(item.id) === String(this.createBranchForm.sourceBranchId))
        const branchName = this.createBranchForm.name
        await createProjectBranch({
          projectId: Number(this.projectId),
          sourceBranchId: Number(this.createBranchForm.sourceBranchId),
          name: this.createBranchForm.name,
          branchType: this.createBranchForm.branchType || 'feature'
        })
        this.$message.success('分支创建成功')
        this.createBranchDialogVisible = false
        this.createBranchForm.name = ''
        this.createBranchForm.branchType = 'feature'
        await this.loadBranches()
        this.appendOperationLog('新建分支', `已从 ${sourceBranch ? sourceBranch.name : '当前来源分支'} 创建 ${branchName}。`, 'success')
      } catch (e) {
        this.$message.error(this.getResponseMessage(e, '创建分支失败'))
      } finally {
        this.createBranchLoading = false
      }
    },
    changeTypeTag(type) {
      const map = {
        ADD: 'success',
        MODIFY: 'warning',
        DELETE: 'danger',
        RENAME: 'info',
        MOVE: 'info'
      }
      return map[type] || ''
    },
    commitOptionLabel(item) {
      if (!item) return ''
      const no = item.commitNo != null ? `#${item.commitNo}` : '#-'
      const sha = item.displaySha || '-'
      const msg = this.commitPrimaryText(item)
      return `${no} ${sha} ${msg}`
    },
    commitTypeLabel(type) {
      return {
        normal: '普通提交',
        merge: '合并提交',
        revert: '回退提交',
        bootstrap: '仓库初始化'
      }[type] || (type || '-')
    },
    commitPrimaryText(item) {
      const raw = String((item && item.message) || '').trim()
      if (!raw) return '无提交说明'
      if (/^bootstrap repository$/i.test(raw) || raw.includes('初始化仓库并接入现有项目文件')) {
        return '初始化仓库并接入现有项目文件'
      }
      const mergeMatch = raw.match(/^merge branch (.+) into (.+)$/i)
      if (mergeMatch) {
        return `合并分支 ${mergeMatch[1]} -> ${mergeMatch[2]}`
      }
      const rollbackMatch = raw.match(/^rollback to commit\s+(.+)$/i)
      if (rollbackMatch) {
        return `回退到提交 ${rollbackMatch[1]}`
      }
      return raw
    },
    commitSecondaryText(item) {
      if (!item) return ''
      const parts = []
      if (item.commitType && item.commitType !== 'normal') {
        parts.push(this.commitTypeLabel(item.commitType))
      }
      if (item.changedFileCount !== undefined && item.changedFileCount !== null) {
        parts.push(`${item.changedFileCount} 个文件`)
      }
      return parts.join(' · ')
    },
    resolveCommitDisplay(commitId) {
      if (!commitId) return ''
      const matched = this.commitList.find(item => String(item.id) === String(commitId))
      if (matched) {
        const no = matched.commitNo != null ? `#${matched.commitNo}` : `#${commitId}`
        return matched.displaySha ? `${no}/${matched.displaySha}` : no
      }
      return `#${commitId}`
    },
    normalizeCommitDetail(raw) {
      if (!raw) return null
      const commit = raw.commit || raw
      return {
        ...commit,
        parents: Array.isArray(raw.parents) ? raw.parents : [],
        changes: Array.isArray(raw.changes) ? raw.changes : []
      }
    },
    workspacePath(row) {
      if (!row) return '-'
      return row.newPath || row.canonicalPath || row.oldPath || row.path || '-'
    },
    workspaceStatusText(row) {
      if (!row) return '未知'
      if (row.conflictFlag) return '冲突'
      if (this.isWorkspaceItemAtRisk(row)) return '待复核'
      if (row.stagedFlag) return '已暂存'
      return '待处理'
    },
    workspaceStatusType(row) {
      if (!row) return 'info'
      if (row.conflictFlag) return 'danger'
      if (this.isWorkspaceItemAtRisk(row)) return 'warning'
      if (row.stagedFlag) return 'success'
      return 'info'
    },
    isWorkspaceItemAtRisk(row) {
      if (!row || row.conflictFlag) return false
      return this.isWorkspaceBaseBehind
    },
    operationLogStorageKey() {
      if (!this.projectId) return ''
      return `project-repo-workbench-log:${this.projectId}:${this.currentBranchId || 'default'}`
    },
    loadOperationLogs() {
      const key = this.operationLogStorageKey()
      if (!key || typeof window === 'undefined' || !window.localStorage) {
        this.operationLogs = []
        return
      }
      try {
        const raw = window.localStorage.getItem(key)
        const list = raw ? JSON.parse(raw) : []
        this.operationLogs = Array.isArray(list) ? list : []
      } catch (e) {
        this.operationLogs = []
      }
    },
    persistOperationLogs() {
      const key = this.operationLogStorageKey()
      if (!key || typeof window === 'undefined' || !window.localStorage) {
        return
      }
      try {
        window.localStorage.setItem(key, JSON.stringify(this.operationLogs.slice(0, 12)))
      } catch (e) {
        // ignore local cache failure
      }
    },
    appendOperationLog(title, desc, tone) {
      const item = {
        id: `${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
        title,
        desc,
        tone: tone || 'info',
        time: new Date().toISOString()
      }
      this.operationLogs = [item].concat(this.operationLogs || []).slice(0, 12)
      this.persistOperationLogs()
    },
    formatOperationTime(value) {
      const date = value instanceof Date ? value : new Date(value)
      if (Number.isNaN(date.getTime())) {
        return value || '-'
      }
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hour = String(date.getHours()).padStart(2, '0')
      const minute = String(date.getMinutes()).padStart(2, '0')
      const second = String(date.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hour}:${minute}:${second}`
    },
    prettyJson(v) {
      try {
        return JSON.stringify(v, null, 2)
      } catch (e) {
        return String(v || '')
      }
    },
    goToAuditCenter() {
      this.$router.push({
        path: '/projectmanage',
        query: {
          projectId: String(this.projectId),
          tab: 'audit-manage'
        }
      })
    }
  }
}
</script>

<style scoped>
.project-repo-workbench {
  padding: 8px 0 0;
}

.repo-tip,
.repo-header-card,
.work-card {
  border-radius: 18px;
}

.repo-header-card {
  margin-bottom: 16px;
  overflow: hidden;
}

.repo-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.repo-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.repo-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #64748b;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.repo-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.repo-main-row,
.repo-bottom-row {
  margin-top: 16px;
}

.repo-journey-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.repo-journey-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid #dbe7f7;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.repo-journey-item.active {
  border-color: #bfdbfe;
  background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%);
}

.repo-journey-step {
  flex: 0 0 38px;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.repo-journey-body {
  min-width: 0;
}

.repo-journey-title {
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
}

.repo-journey-desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.7;
  color: #64748b;
}

.repo-summary-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.repo-summary-card {
  padding: 16px;
  border-radius: 16px;
  border: 1px solid #e5ecf6;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.repo-summary-card.tone-blue { background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%); }
.repo-summary-card.tone-cyan { background: linear-gradient(180deg, #ecfeff 0%, #ffffff 100%); }
.repo-summary-card.tone-purple { background: linear-gradient(180deg, #f5f3ff 0%, #ffffff 100%); }
.repo-summary-card.tone-orange { background: linear-gradient(180deg, #fff7ed 0%, #ffffff 100%); }

.repo-summary-label {
  color: #64748b;
  font-size: 13px;
}

.repo-summary-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.repo-summary-desc {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.7;
}

.repo-warning-stack {
  margin-top: 16px;
  display: grid;
  gap: 10px;
}

.repo-warning-alert {
  border-radius: 14px;
}

.card-header {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.section-gap {
  margin-top: 10px;
}

.section-gap-lg {
  margin-top: 18px;
}

.upload-block {
  width: 100%;
}

.folder-input {
  display: none;
}

.batch-upload-actions {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.repo-help {
  font-size: 12px;
  color: #909399;
  line-height: 1.9;
}

.batch-stage-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.workspace-insight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.workspace-insight-card {
  padding: 14px;
  border-radius: 14px;
  border: 1px solid #e5ecf6;
  background: #ffffff;
}

.workspace-insight-card.tone-blue { background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%); }
.workspace-insight-card.tone-cyan { background: linear-gradient(180deg, #ecfeff 0%, #ffffff 100%); }
.workspace-insight-card.tone-purple { background: linear-gradient(180deg, #f5f3ff 0%, #ffffff 100%); }
.workspace-insight-card.tone-orange { background: linear-gradient(180deg, #fff7ed 0%, #ffffff 100%); }
.workspace-insight-card.tone-danger { background: linear-gradient(180deg, #fff1f2 0%, #ffffff 100%); }

.workspace-insight-label {
  font-size: 12px;
  color: #64748b;
}

.workspace-insight-value {
  margin-top: 6px;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.workspace-insight-desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.7;
  color: #94a3b8;
}

.workspace-path-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.workspace-path-main {
  color: #334155;
  word-break: break-all;
}

.workspace-path-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.commit-box {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.commit-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.compare-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.compare-bar .el-select {
  flex: 1;
}

.detail-box {
  min-height: 160px;
}

.detail-item {
  line-height: 2;
  font-size: 13px;
  color: #606266;
}

.detail-item span {
  display: inline-block;
  width: 90px;
  color: #909399;
}

.repo-insight-row {
  align-items: stretch;
}

.workspace-group-list,
.repo-log-list {
  display: grid;
  gap: 12px;
}

.workspace-group-item,
.repo-log-item {
  padding: 14px;
  border-radius: 14px;
  border: 1px solid #e5ecf6;
  background: #ffffff;
}

.workspace-group-item.tone-blue { background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%); }
.workspace-group-item.tone-purple { background: linear-gradient(180deg, #f5f3ff 0%, #ffffff 100%); }
.workspace-group-item.tone-danger { background: linear-gradient(180deg, #fff1f2 0%, #ffffff 100%); }
.repo-log-item.tone-success { background: linear-gradient(180deg, #f0fdf4 0%, #ffffff 100%); }
.repo-log-item.tone-info { background: linear-gradient(180deg, #f8fafc 0%, #ffffff 100%); }
.repo-log-item.tone-warning { background: linear-gradient(180deg, #fff7ed 0%, #ffffff 100%); }

.workspace-group-head,
.repo-log-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.workspace-group-title,
.repo-log-title {
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
}

.workspace-group-desc,
.repo-log-desc {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.8;
  color: #64748b;
}

.workspace-group-paths {
  margin-top: 10px;
  display: grid;
  gap: 6px;
}

.workspace-group-path {
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.7);
  color: #334155;
  font-size: 12px;
  word-break: break-all;
}

.repo-log-time {
  flex: 0 0 auto;
  color: #94a3b8;
  font-size: 12px;
}

.compare-result-shell {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.compare-stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.compare-stat-card {
  padding: 14px;
  border-radius: 14px;
  border: 1px solid #e5ecf6;
  background: #ffffff;
}

.compare-stat-card.tone-blue { background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%); }
.compare-stat-card.tone-purple { background: linear-gradient(180deg, #f5f3ff 0%, #ffffff 100%); }
.compare-stat-card.tone-danger { background: linear-gradient(180deg, #fff1f2 0%, #ffffff 100%); }

.compare-stat-label {
  font-size: 12px;
  color: #64748b;
}

.compare-stat-value {
  margin-top: 6px;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.compare-file-table {
  border-radius: 12px;
  overflow: hidden;
}

.compare-raw-toggle {
  align-self: flex-end;
}

.compare-box {
  min-height: 160px;
  max-height: 320px;
  overflow: auto;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
}

.compare-box pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
}

@media (max-width: 1200px) {
  .repo-journey-strip,
  .repo-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workspace-insight-grid,
  .compare-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .repo-header {
    flex-direction: column;
    align-items: stretch;
  }

  .repo-header-right {
    flex-wrap: wrap;
  }

  .repo-journey-strip,
  .repo-summary-grid,
  .workspace-insight-grid,
  .compare-stat-grid {
    grid-template-columns: 1fr;
  }

  .compare-bar {
    flex-direction: column;
  }

  .workspace-group-head,
  .repo-log-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
