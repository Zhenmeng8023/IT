<template>
  <div class="project-repo-workbench">
    <el-alert
      type="success"
      :closable="false"
      show-icon
      class="repo-tip"
      title="这里是按你当前后端已实现接口对齐的仓库工作台。当前后端已支持：仓库初始化、仓库详情、分支列表/创建、单文件暂存、路径删除暂存、工作区提交、提交历史、提交详情、提交比较、回退提交。"
    />

    <el-card shadow="never" class="repo-header-card">
      <div class="repo-header">
        <div class="repo-header-left">
          <div class="repo-title">项目仓库工作台</div>
          <div class="repo-meta">
            <span>仓库：{{ repository ? '已初始化' : '未初始化' }}</span>
            <span>当前分支：{{ currentBranchName }}</span>
            <span>工作区变更：{{ workspaceItems.length }}</span>
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

    <el-row :gutter="16" class="repo-main-row">
      <el-col :span="7">
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
            title="你当前这份后端还没有实现 ZIP 暂存接口，所以这里先保留说明，不发请求。"
          />

          <div class="repo-help section-gap-lg">
            <div>1. 先初始化仓库，再创建或选择分支。</div>
            <div>2. 当前后端的工作区只记录：单文件暂存、路径删除、提交工作区。</div>
            <div>3. 提交后会生成新提交历史，不会直接覆盖正式版本。</div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never" class="work-card diff-card">
          <div slot="header" class="card-header">工作区变更</div>
          <el-table
            :data="workspaceItems"
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
            <el-table-column prop="canonicalPath" label="路径" min-width="180" show-overflow-tooltip />
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
            <div class="commit-meta">当前工作区：{{ workspace ? '已创建' : '未创建' }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="9">
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
            <el-table-column prop="message" label="提交说明" min-width="180" show-overflow-tooltip />
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

    <el-row :gutter="16" class="repo-bottom-row">
      <el-col :span="12">
        <el-card shadow="never" class="work-card detail-card">
          <div slot="header" class="card-header">当前提交详情</div>
          <div v-if="selectedCommitDetail" class="detail-box">
            <div class="detail-item"><span>提交号：</span>{{ selectedCommitDetail.commitNo || '-' }}</div>
            <div class="detail-item"><span>SHA：</span>{{ selectedCommitDetail.displaySha || '-' }}</div>
            <div class="detail-item"><span>说明：</span>{{ selectedCommitDetail.message || '-' }}</div>
            <div class="detail-item"><span>类型：</span>{{ selectedCommitDetail.commitType || '-' }}</div>
            <div class="detail-item"><span>提交人：</span>{{ selectedCommitDetail.operatorId || '-' }}</div>
            <div class="detail-item"><span>时间：</span>{{ selectedCommitDetail.createdAt || '-' }}</div>
            <div class="detail-item"><span>变更文件数：</span>{{ selectedCommitDetail.changedFileCount || 0 }}</div>
          </div>
          <el-empty v-else description="点击右侧提交历史查看详情" :image-size="90" />
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never" class="work-card compare-card">
          <div slot="header" class="card-header">提交变更摘要</div>
          <div v-if="compareResult" class="compare-box">
            <pre>{{ prettyJson(compareResult) }}</pre>
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
      commitList: [],
      selectedCommitDetail: null,
      compareFromCommitId: null,
      compareToCommitId: null,
      compareResult: null,
      stageForm: {
        canonicalPath: ''
      },
      deletePath: '',
      pendingFile: null,
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
      deletePathLoading: false,
      commitLoading: false,
      commitListLoading: false
    }
  },
  computed: {
    currentBranchName() {
      const item = this.branchList.find(v => String(v.id) === String(this.currentBranchId))
      return item ? item.name : '未选择'
    },
    canStageFile() {
      return !!this.projectId && !!this.currentBranchId && !!this.stageForm.canonicalPath && !!this.pendingFile
    },
    canDeletePath() {
      return !!this.projectId && !!this.currentBranchId && !!this.deletePath
    },
    canCommit() {
      return !!this.projectId && !!this.currentBranchId && !!this.workspace && this.workspaceItems.length > 0 && !!this.commitForm.message
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
      return fallback || '请求失败'
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
        this.workspace = this.unwrapResponse(wsRes)
        const itemsRes = await getWorkspaceItems(this.projectId, this.currentBranchId)
        const items = this.unwrapResponse(itemsRes)
        this.workspaceItems = Array.isArray(items) ? items : []
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
      await this.loadWorkspace()
      await this.loadCommitList()
    },
    async handleBranchChange() {
      this.selectedCommitDetail = null
      this.compareResult = null
      await this.loadWorkspace()
      await this.loadCommitList()
    },
    preventAutoUpload() {
      return false
    },
    handleFileChange(file) {
      this.pendingFile = file && file.raw ? file.raw : null
    },
    async handleStageFile() {
      if (!this.canStageFile) {
        this.$message.warning('请先填写规范路径并选择文件')
        return
      }
      this.stageFileLoading = true
      try {
        await stageWorkspaceFile(this.projectId, this.currentBranchId, this.stageForm.canonicalPath, this.pendingFile)
        this.$message.success('已加入工作区')
        this.stageForm.canonicalPath = ''
        this.pendingFile = null
        await this.loadWorkspace()
      } catch (e) {
        this.$message.error(this.getResponseMessage(e, '加入工作区失败'))
      } finally {
        this.stageFileLoading = false
      }
    },
    async handleStageDelete() {
      if (!this.canDeletePath) {
        this.$message.warning('请先填写要删除的规范路径')
        return
      }
      this.deletePathLoading = true
      try {
        await stageWorkspaceDelete(this.projectId, this.currentBranchId, this.deletePath)
        this.$message.success('删除路径已加入工作区')
        this.deletePath = ''
        await this.loadWorkspace()
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
        await commitWorkspace({
          projectId: Number(this.projectId),
          branchId: Number(this.currentBranchId),
          message: this.commitForm.message
        })
        this.$message.success('提交成功')
        this.commitForm.message = ''
        this.selectedCommitDetail = null
        this.compareResult = null
        await this.refreshAll()
      } catch (e) {
        this.$message.error(this.getResponseMessage(e, '提交失败'))
      } finally {
        this.commitLoading = false
      }
    },
    async handleSelectCommit(row) {
      if (!row || !row.id) return
      try {
        const res = await getProjectCommitDetail(row.id)
        this.selectedCommitDetail = this.unwrapResponse(res)
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
        await rollbackToCommit(row.id)
        this.$message.success('回退成功')
        this.selectedCommitDetail = null
        this.compareResult = null
        await this.refreshAll()
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
      const msg = item.message || '-'
      return `${no} ${sha} ${msg}`
    },
    prettyJson(v) {
      try {
        return JSON.stringify(v, null, 2)
      } catch (e) {
        return String(v || '')
      }
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
  border-radius: 8px;
}

.repo-header-card {
  margin-bottom: 16px;
}

.repo-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.repo-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.repo-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
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

.repo-help {
  font-size: 12px;
  color: #909399;
  line-height: 1.9;
}

.commit-box {
  margin-top: 14px;
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
</style>
