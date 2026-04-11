<template>
  <div class="feature-page">
    <el-row :gutter="16" class="top-row">
      <el-col :xs="24" :sm="8">
        <div class="mini-card">
          <div class="mini-label">总里程碑</div>
          <div class="mini-value">{{ overview.total || overview.totalCount || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="mini-card">
          <div class="mini-label">进行中</div>
          <div class="mini-value">{{ overview.activeCount || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="mini-card">
          <div class="mini-label">已完成</div>
          <div class="mini-value">{{ overview.completedCount || 0 }}</div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <div slot="header" class="bar">
        <span>里程碑管理</span>
        <div class="bar-actions">
          <el-select v-model="status" size="small" clearable placeholder="状态筛选" class="w140" @change="loadList">
            <el-option label="planned" value="planned" />
            <el-option label="active" value="active" />
            <el-option label="completed" value="completed" />
            <el-option label="cancelled" value="cancelled" />
          </el-select>
          <el-button size="small" @click="loadAll">刷新</el-button>
          <el-button v-if="canManageProject" type="primary" size="small" @click="openCreate">新建里程碑</el-button>
        </div>
      </div>

      <el-table :data="list" border v-loading="loading">
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="description" label="说明" min-width="220" />
        <el-table-column prop="status" label="状态" width="120">
          <template slot-scope="scope">
            <el-tag size="mini" :type="statusType(scope.row.status)">{{ scope.row.status || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="dueDate" label="截止日期" width="120" />
        <el-table-column prop="anchorCommitId" label="锚点 Commit" width="110" />
        <el-table-column prop="fromCommitId" label="起始 Commit" width="110" />
        <el-table-column prop="toCommitId" label="结束 Commit" width="110" />
        <el-table-column prop="completedAt" label="完成时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.completedAt) }}</template>
        </el-table-column>
        <el-table-column v-if="canManageProject" label="操作" width="300" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" @click="openEdit(scope.row)">编辑</el-button>
            <el-dropdown @command="v => changeStatus(scope.row, v)">
              <el-button size="mini" type="primary" plain>状态</el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="planned">planned</el-dropdown-item>
                <el-dropdown-item command="active">active</el-dropdown-item>
                <el-dropdown-item command="completed">completed</el-dropdown-item>
                <el-dropdown-item command="cancelled">cancelled</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
            <el-button size="mini" type="danger" @click="removeRow(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !list.length" description="暂无里程碑" />
    </el-card>

    <el-dialog :title="form.id ? '编辑里程碑' : '新建里程碑'" :visible.sync="visible" width="620px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="planned" value="planned" />
            <el-option label="active" value="active" />
            <el-option label="completed" value="completed" />
            <el-option label="cancelled" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="yyyy-MM-dd" style="width:100%" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="form.dueDate" type="date" value-format="yyyy-MM-dd" style="width:100%" />
        </el-form-item>
        <el-form-item label="分支 ID">
          <el-input v-model="form.branchId" placeholder="可选，例如 1" />
        </el-form-item>
        <el-form-item label="锚点 Commit">
          <el-input v-model="form.anchorCommitId" placeholder="可选，例如 12" />
        </el-form-item>
        <el-form-item label="起始 Commit">
          <el-input v-model="form.fromCommitId" placeholder="可选，例如 10" />
        </el-form-item>
        <el-form-item label="结束 Commit">
          <el-input v-model="form.toCommitId" placeholder="可选，例如 15" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  listProjectMilestones,
  getProjectMilestoneOverview,
  createProjectMilestone,
  updateProjectMilestone,
  changeProjectMilestoneStatus,
  deleteProjectMilestone
} from '@/api/projectMilestone'

function p(r) {
  if (r && r.data !== undefined) return r.data
  return r || {}
}

export default {
  name: 'ProjectMilestoneManage',
  props: {
    projectId: { type: [String, Number], required: true },
    canManageProject: { type: Boolean, default: true }
  },
  data() {
    return {
      loading: false,
      saving: false,
      visible: false,
      status: '',
      overview: {},
      list: [],
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
        name: '',
        description: '',
        status: 'planned',
        startDate: '',
        dueDate: '',
        branchId: '',
        anchorCommitId: '',
        fromCommitId: '',
        toCommitId: ''
      }
    },
    statusType(v) {
      return { planned: 'info', active: 'warning', completed: 'success', cancelled: 'danger' }[v] || 'info'
    },
    formatTime(v) {
      if (!v) return ''
      const d = new Date(v)
      if (Number.isNaN(d.getTime())) return v
      return d.toLocaleString('zh-CN')
    },
    async loadAll() {
      await Promise.all([this.loadOverview(), this.loadList()])
    },
    async loadOverview() {
      const r = await getProjectMilestoneOverview(this.projectId).catch(() => ({}))
      this.overview = p(r)
    },
    async loadList() {
      this.loading = true
      try {
        const r = await listProjectMilestones(this.projectId, this.status ? { status: this.status } : {})
        this.list = Array.isArray(p(r)) ? p(r) : []
      } finally {
        this.loading = false
      }
    },
    openCreate() {
      this.form = this.emptyForm()
      this.visible = true
    },
    openEdit(row) {
      this.form = {
        id: row.id,
        projectId: row.projectId,
        name: row.name || '',
        description: row.description || '',
        status: row.status || 'planned',
        startDate: row.startDate || '',
        dueDate: row.dueDate || '',
        branchId: row.branchId || '',
        anchorCommitId: row.anchorCommitId || '',
        fromCommitId: row.fromCommitId || '',
        toCommitId: row.toCommitId || ''
      }
      this.visible = true
    },
    async save() {
      if (!this.form.name) {
        this.$message.warning('请填写里程碑名称')
        return
      }
      this.saving = true
      try {
        const d = {
          ...this.form,
          projectId: Number(this.projectId),
          branchId: this.form.branchId ? Number(this.form.branchId) : undefined,
          anchorCommitId: this.form.anchorCommitId ? Number(this.form.anchorCommitId) : undefined,
          fromCommitId: this.form.fromCommitId ? Number(this.form.fromCommitId) : undefined,
          toCommitId: this.form.toCommitId ? Number(this.form.toCommitId) : undefined
        }
        if (d.id) {
          await updateProjectMilestone(d.id, d)
          this.$message.success('里程碑已更新')
        } else {
          await createProjectMilestone(d)
          this.$message.success('里程碑已创建')
        }
        this.visible = false
        await this.loadAll()
      } catch (e) {
        this.$message.error(e.response?.data?.message || '保存失败')
      } finally {
        this.saving = false
      }
    },
    async changeStatus(row, v) {
      try {
        await changeProjectMilestoneStatus(row.id, v)
        this.$message.success('状态已更新')
        await this.loadAll()
      } catch (e) {
        this.$message.error(e.response?.data?.message || '状态更新失败')
      }
    },
    async removeRow(row) {
      try {
        await this.$confirm(`确定删除里程碑“${row.name}”吗？`, '提示', { type: 'warning' })
        await deleteProjectMilestone(row.id)
        this.$message.success('删除成功')
        await this.loadAll()
      } catch (e) {
        if (e !== 'cancel') this.$message.error(e.response?.data?.message || '删除失败')
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
</style>
