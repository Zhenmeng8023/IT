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
          <div class="mini-label">发布状态</div>
          <div class="mini-value">{{ latest.status || 'draft' }}</div>
          <div class="mini-desc">{{ latest.publishedAt ? formatTime(latest.publishedAt) : '未发布' }}</div>
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
        <el-table-column prop="basedCommitId" label="基线 Commit" width="120" />
        <el-table-column prop="basedMilestoneId" label="里程碑" width="100" />
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
        <el-form-item label="分支 ID">
          <el-input v-model="form.branchId" placeholder="可选，例如 1" />
        </el-form-item>
        <el-form-item label="基线 Commit">
          <el-input v-model="form.basedCommitId" placeholder="可选，例如 36" />
        </el-form-item>
        <el-form-item label="里程碑 ID">
          <el-input v-model="form.basedMilestoneId" placeholder="可选，例如 5" />
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
        <el-form-item label="选择文件">
          <el-select v-model="selectedFileIds" multiple filterable style="width:100%" placeholder="请选择要挂到本次发布的项目文件">
            <el-option v-for="item in fileOptions" :key="item.id" :label="item.fileName" :value="item.id" />
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
  bindProjectReleaseFiles
} from '@/api/projectRelease'
import { listProjectFiles } from '@/api/project'

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
      fileOptions: [],
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
      await Promise.all([this.loadLatest(), this.loadList(), this.loadFiles()])
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
    async loadFiles() {
      const r = await listProjectFiles(this.projectId).catch(() => ({}))
      const d = p(r)
      this.fileOptions = Array.isArray(d) ? d : []
    },
    openCreate() {
      this.form = this.emptyForm()
      this.visible = true
    },
    openEdit(row) {
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
      this.visible = true
    },
    async save() {
      if (!this.form.version || !this.form.title) {
        this.$message.warning('请填写版本号和标题')
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
    openBindFiles(row) {
      this.activeRelease = row || {}
      this.selectedFileIds = []
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
