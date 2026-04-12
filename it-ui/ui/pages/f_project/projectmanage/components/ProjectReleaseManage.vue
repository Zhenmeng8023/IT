<template>
  <div class="feature-page">
    <el-row :gutter="16" class="top-row">
      <el-col :xs="24" :sm="12">
        <div class="mini-card">
          <div class="mini-label">最新发布</div>
          <div class="mini-value small">{{ latest.version || '暂无' }}</div>
          <div class="mini-desc">{{ latest.title || '当前还没有已发布版本' }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12">
        <div class="mini-card">
          <div class="mini-label">追溯锚点</div>
          <div class="mini-value">{{ latestTraceTitle }}</div>
          <div class="mini-desc">{{ latestTraceDesc }}</div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <div slot="header" class="bar">
        <span>发布记录管理</span>
        <div class="bar-actions">
          <el-select v-model="status" size="small" clearable placeholder="状态筛选" class="w140" @change="loadList">
            <el-option label="draft" value="draft" />
            <el-option label="published" value="published" />
            <el-option label="archived" value="archived" />
          </el-select>
          <el-button size="small" @click="loadAll">刷新</el-button>
          <el-button v-if="canManageProject" type="primary" size="small" @click="openCreate">新建 Release</el-button>
        </div>
      </div>

      <el-table :data="list" border v-loading="loading">
        <el-table-column prop="version" label="版本号" width="120" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="releaseType" label="类型" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template slot-scope="scope">
            <el-tag size="mini" :type="statusType(scope.row.status)">{{ scope.row.status || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分支" min-width="120">
          <template slot-scope="scope">{{ branchLabel(scope.row.branchId) }}</template>
        </el-table-column>
        <el-table-column label="基线 Commit" min-width="210">
          <template slot-scope="scope">{{ commitLabel(scope.row.basedCommitId) }}</template>
        </el-table-column>
        <el-table-column label="里程碑" min-width="140">
          <template slot-scope="scope">{{ milestoneLabel(scope.row.basedMilestoneId) }}</template>
        </el-table-column>
        <el-table-column prop="frozenAt" label="冻结时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.frozenAt) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.publishedAt) }}</template>
        </el-table-column>
        <el-table-column prop="releaseNotes" label="说明" min-width="260" />
        <el-table-column v-if="canManageProject" label="操作" width="360" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" @click="openEdit(scope.row)">编辑</el-button>
            <el-button size="mini" type="primary" plain @click="openBindFiles(scope.row)">绑定文件</el-button>
            <el-button size="mini" type="success" :disabled="scope.row.status === 'published'" @click="publishRow(scope.row)">发布</el-button>
            <el-button size="mini" type="warning" :disabled="scope.row.status === 'archived'" @click="archiveRow(scope.row)">归档</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !list.length" description="暂无发布记录" />
    </el-card>

    <el-dialog :title="form.id ? '编辑 Release' : '新建 Release'" :visible.sync="visible" width="680px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="版本号">
          <el-input v-model="form.version" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.releaseType" style="width:100%">
            <el-option label="draft" value="draft" />
            <el-option label="beta" value="beta" />
            <el-option label="stable" value="stable" />
            <el-option label="hotfix" value="hotfix" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="draft" value="draft" />
            <el-option label="published" value="published" />
            <el-option label="archived" value="archived" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题说明">
          <el-input v-model="form.description" />
        </el-form-item>
        <el-form-item label="发布说明">
          <el-input v-model="form.releaseNotes" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="分支">
          <el-select v-model="form.branchId" style="width:100%" placeholder="请选择分支" filterable @change="handleBranchChange">
            <el-option v-for="item in branchOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="基线 Commit">
          <el-select v-model="form.basedCommitId" style="width:100%" placeholder="请选择基线 Commit" filterable @change="handleBasedCommitChange">
            <el-option v-for="item in commitOptions" :key="item.id" :label="commitOptionLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="里程碑">
          <el-select v-model="form.basedMilestoneId" clearable style="width:100%" placeholder="可选，绑定某个里程碑">
            <el-option v-for="item in milestoneOptions" :key="item.id" :label="milestoneOptionLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="推荐版本">
          <el-switch v-model="form.recommendedFlag" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog title="绑定发布文件" :visible.sync="bindVisible" width="620px">
      <el-form label-width="90px">
        <el-form-item label="Release">
          <div class="release-name">{{ activeRelease.version }} · {{ activeRelease.title }}</div>
        </el-form-item>
        <el-form-item label="基线 Commit">
          <div class="release-name">{{ commitLabel(activeRelease.basedCommitId) }}</div>
        </el-form-item>
        <el-form-item label="选择文件">
          <el-select v-model="selectedFileIds" multiple filterable style="width:100%" placeholder="请选择要挂到本次发布的项目文件">
            <el-option v-for="item in bindableFileOptions" :key="item.projectFileId" :label="bindableFileLabel(item)" :value="item.projectFileId" />
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="binding" @click="submitBind">绑定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  listProjectReleases,
  getLatestProjectRelease,
  createProjectRelease,
  updateProjectRelease,
  publishProjectRelease,
  archiveProjectRelease,
  bindProjectReleaseFiles,
  listProjectReleaseBindableFiles
} from '@/api/projectRelease'
import { listProjectBranches } from '@/api/projectBranch'
import { listProjectCommits } from '@/api/projectCommit'
import { listProjectMilestones } from '@/api/projectMilestone'

function p(r) {
  if (r && r.data !== undefined) return r.data
  return r || {}
}

export default {
  name: 'ProjectReleaseManage',
  props: {
    projectId: { type: [String, Number], required: true },
    canManageProject: { type: Boolean, default: true }
  },
  data() {
    return {
      loading: false,
      saving: false,
      binding: false,
      visible: false,
      bindVisible: false,
      status: '',
      list: [],
      latest: {},
      branchOptions: [],
      milestoneOptions: [],
      commitMap: {},
      commitOptionsByBranch: {},
      commitOptions: [],
      bindableFileOptions: [],
      selectedFileIds: [],
      activeRelease: {},
      form: this.emptyForm()
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler() {
        if (this.projectId) this.loadAll()
      }
    }
  },
  computed: {
    latestTraceTitle() {
      if (!this.latest || !this.latest.id) return '未冻结'
      return `${this.branchLabel(this.latest.branchId)} · ${this.commitLabel(this.latest.basedCommitId)}`
    },
    latestTraceDesc() {
      if (!this.latest || !this.latest.id) return '当前还没有可追溯的发布基线'
      const milestone = this.latest.basedMilestoneId ? this.milestoneLabel(this.latest.basedMilestoneId) : '未绑定里程碑'
      const frozenAt = this.latest.frozenAt ? this.formatTime(this.latest.frozenAt) : '未记录冻结时间'
      return `${milestone} · ${frozenAt}`
    }
  },
  methods: {
    emptyForm() {
      return {
        id: null,
        projectId: this.projectId,
        version: '',
        title: '',
        description: '',
        releaseNotes: '',
        releaseType: 'draft',
        status: 'draft',
        branchId: '',
        basedCommitId: '',
        basedMilestoneId: '',
        recommendedFlag: false
      }
    },
    statusType(v) {
      return { draft: 'info', published: 'success', archived: 'warning' }[v] || 'info'
    },
    formatTime(v) {
      if (!v) return ''
      const d = new Date(v)
      if (Number.isNaN(d.getTime())) return v
      return d.toLocaleString('zh-CN')
    },
    async loadAll() {
      await Promise.all([this.loadLatest(), this.loadReferenceData()])
      await Promise.all([this.loadList(), this.loadCommitReferenceData()])
    },
    async loadLatest() {
      const r = await getLatestProjectRelease(this.projectId).catch(() => ({}))
      const summary = p(r)
      this.latest = summary && summary.latest ? summary.latest : {}
    },
    async loadList() {
      this.loading = true
      try {
        const r = await listProjectReleases(this.projectId, this.status ? { status: this.status } : {})
        this.list = Array.isArray(p(r)) ? p(r) : []
      } finally {
        this.loading = false
      }
    },
    async loadReferenceData() {
      await Promise.all([this.loadBranches(), this.loadMilestones()])
    },
    async loadBranches() {
      const r = await listProjectBranches(this.projectId).catch(() => ({}))
      const d = p(r)
      this.branchOptions = Array.isArray(d) ? d : []
    },
    async loadMilestones() {
      const r = await listProjectMilestones(this.projectId).catch(() => ({}))
      const d = p(r)
      this.milestoneOptions = Array.isArray(d) ? d : []
    },
    async loadCommitReferenceData() {
      const branchIds = Array.from(new Set((this.branchOptions || []).map(item => item && item.id).filter(Boolean)))
      await Promise.all(branchIds.map(branchId => this.loadBranchCommits(branchId)))
    },
    mergeCommitMap(commits = []) {
      const next = { ...this.commitMap }
      ;(commits || []).forEach(item => {
        if (item && item.id !== undefined && item.id !== null) {
          next[String(item.id)] = item
        }
      })
      this.commitMap = next
    },
    async loadBranchCommits(branchId, force = false) {
      if (!branchId) return []
      const key = String(branchId)
      if (!force && Array.isArray(this.commitOptionsByBranch[key])) {
        return this.commitOptionsByBranch[key]
      }
      const r = await listProjectCommits(this.projectId, branchId).catch(() => ({}))
      const d = p(r)
      const commits = Array.isArray(d) ? d : []
      this.$set(this.commitOptionsByBranch, key, commits)
      this.mergeCommitMap(commits)
      return commits
    },
    async loadCommits(branchId) {
      if (!branchId) {
        this.commitOptions = []
        return
      }
      this.commitOptions = await this.loadBranchCommits(branchId)
    },
    async loadBindableFiles(commitId) {
      if (!commitId) {
        this.bindableFileOptions = []
        return
      }
      const r = await listProjectReleaseBindableFiles(this.projectId, commitId).catch(() => ({}))
      const d = p(r)
      this.bindableFileOptions = Array.isArray(d) ? d : []
    },
    async openCreate() {
      this.form = this.emptyForm()
      await this.prepareFormContext()
      this.visible = true
    },
    async openEdit(row) {
      this.form = {
        id: row.id,
        projectId: row.projectId,
        version: row.version || '',
        title: row.title || '',
        description: row.description || '',
        releaseNotes: row.releaseNotes || '',
        releaseType: row.releaseType || 'draft',
        status: row.status || 'draft',
        branchId: row.branchId || '',
        basedCommitId: row.basedCommitId || '',
        basedMilestoneId: row.basedMilestoneId || '',
        recommendedFlag: !!row.recommendedFlag
      }
      await this.prepareFormContext(this.form.branchId, this.form.basedCommitId)
      this.visible = true
    },
    async prepareFormContext(preferredBranchId = '', preferredCommitId = '') {
      await this.loadReferenceData()
      const branchId = preferredBranchId || this.form.branchId || (this.branchOptions[0] && this.branchOptions[0].id) || ''
      this.form.branchId = branchId || ''
      await this.loadCommits(this.form.branchId)
      const commitId = preferredCommitId || this.form.basedCommitId || ''
      this.form.basedCommitId = commitId || ''
      await this.loadBindableFiles(this.form.basedCommitId)
    },
    async handleBranchChange(value) {
      this.form.branchId = value || ''
      this.form.basedCommitId = ''
      this.bindableFileOptions = []
      await this.loadCommits(this.form.branchId)
    },
    async handleBasedCommitChange(value) {
      this.form.basedCommitId = value || ''
      await this.loadBindableFiles(this.form.basedCommitId)
    },
    async save() {
      if (!this.form.version || !this.form.title) {
        this.$message.warning('请填写版本号和标题')
        return
      }
      if (!this.form.branchId || !this.form.basedCommitId) {
        this.$message.warning('请先选择分支和基线 Commit')
        return
      }
      this.saving = true
      try {
        const d = {
          ...this.form,
          projectId: Number(this.projectId),
          branchId: this.form.branchId ? Number(this.form.branchId) : undefined,
          basedCommitId: this.form.basedCommitId ? Number(this.form.basedCommitId) : undefined,
          basedMilestoneId: this.form.basedMilestoneId ? Number(this.form.basedMilestoneId) : undefined
        }
        if (d.id) {
          await updateProjectRelease(d.id, d)
          this.$message.success('Release 已更新')
        } else {
          await createProjectRelease(d)
          this.$message.success('Release 已创建')
        }
        this.visible = false
        await this.loadAll()
      } catch (e) {
        this.$message.error(e.response?.data?.message || '保存失败')
      } finally {
        this.saving = false
      }
    },
    async openBindFiles(row) {
      this.activeRelease = row || {}
      this.selectedFileIds = []
      await this.loadBindableFiles(row && row.basedCommitId)
      this.bindVisible = true
    },
    async submitBind() {
      if (!this.activeRelease.id || !this.selectedFileIds.length) {
        this.$message.warning('请先选择要绑定的文件')
        return
      }
      this.binding = true
      try {
        await bindProjectReleaseFiles(this.activeRelease.id, this.selectedFileIds)
        this.$message.success('发布文件已绑定')
        this.bindVisible = false
      } catch (e) {
        this.$message.error(e.response?.data?.message || '绑定失败')
      } finally {
        this.binding = false
      }
    },
    async publishRow(row) {
      try {
        await publishProjectRelease(row.id)
        this.$message.success('已发布')
        await this.loadAll()
      } catch (e) {
        this.$message.error(e.response?.data?.message || '发布失败')
      }
    },
    async archiveRow(row) {
      try {
        await archiveProjectRelease(row.id)
        this.$message.success('已归档')
        await this.loadAll()
      } catch (e) {
        this.$message.error(e.response?.data?.message || '归档失败')
      }
    },
    branchLabel(id) {
      const matched = this.branchOptions.find(item => String(item.id) === String(id))
      return matched ? matched.name : (id ? `#${id}` : '-')
    },
    milestoneLabel(id) {
      const matched = this.milestoneOptions.find(item => String(item.id) === String(id))
      return matched ? matched.name : (id ? `#${id}` : '-')
    },
    milestoneOptionLabel(item) {
      if (!item) return ''
      return `${item.name}${item.status ? ` · ${item.status}` : ''}`
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
    commitOptionLabel(item) {
      if (!item) return ''
      const no = item.commitNo != null ? `#${item.commitNo}` : '#-'
      const sha = item.displaySha || '-'
      const msg = this.commitPrimaryText(item)
      return `${no} ${sha} ${msg}`
    },
    commitLabel(id) {
      const matched = this.commitMap[String(id)] || this.commitOptions.find(item => String(item.id) === String(id))
      return matched ? this.commitOptionLabel(matched) : (id ? `#${id}` : '-')
    },
    bindableFileLabel(item) {
      if (!item) return ''
      const version = item.version ? ` · ${item.version}` : ''
      const note = item.commitMessage ? ` · ${this.commitPrimaryText({ message: item.commitMessage })}` : ''
      return `${item.canonicalPath || item.fileName || '-'}${version}${note}`
    }
  }
}
</script>

<style scoped>
.feature-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.top-row {
  margin-bottom: 0;
}
.mini-card {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fff;
  padding: 16px;
  min-height: 118px;
}
.mini-label {
  color: #909399;
  font-size: 13px;
}
.mini-value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
  color: #303133;
}
.mini-value.small {
  font-size: 22px;
}
.mini-desc {
  margin-top: 8px;
  color: #7b8794;
  font-size: 12px;
  line-height: 1.7;
}
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.bar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.w140 {
  width: 140px;
}
.release-name {
  color: #303133;
  font-weight: 600;
}
</style>
