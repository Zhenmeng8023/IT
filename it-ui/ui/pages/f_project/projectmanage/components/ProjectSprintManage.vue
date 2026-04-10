<template>
  <div class="feature-page">
    <el-card shadow="never">
      <div slot="header" class="bar">
        <span>Sprint 管理</span>
        <div class="bar-actions">
          <el-select v-model="status" size="small" clearable placeholder="状态筛选" class="w140" @change="loadList">
            <el-option label="planned" value="planned" />
            <el-option label="active" value="active" />
            <el-option label="closed" value="closed" />
          </el-select>
          <el-button size="small" @click="loadAll">刷新</el-button>
          <el-button v-if="canManageProject" type="primary" size="small" @click="openCreate">新建 Sprint</el-button>
        </div>
      </div>

      <el-row :gutter="16" class="top-row">
        <el-col :xs="24" :sm="12">
          <div class="mini-card">
            <div class="mini-label">当前 Sprint</div>
            <div class="mini-value small">{{ current.name || '暂无' }}</div>
            <div class="mini-desc">{{ current.goal || '当前还没有 active Sprint' }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12">
          <div class="mini-card">
            <div class="mini-label">状态</div>
            <div class="mini-value">{{ current.status || 'planned' }}</div>
            <div class="mini-desc">{{ current.startDate || '-' }} ~ {{ current.endDate || '-' }}</div>
          </div>
        </el-col>
      </el-row>

      <el-table :data="list" border v-loading="loading">
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="goal" label="目标" min-width="260" />
        <el-table-column prop="status" label="状态" width="120">
          <template slot-scope="scope">
            <el-tag size="mini" :type="statusType(scope.row.status)">{{ scope.row.status || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column v-if="canManageProject" label="操作" width="260" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" @click="openEdit(scope.row)">编辑</el-button>
            <el-dropdown @command="v => changeStatus(scope.row, v)">
              <el-button size="mini" type="primary" plain>状态</el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="planned">planned</el-dropdown-item>
                <el-dropdown-item command="active">active</el-dropdown-item>
                <el-dropdown-item command="closed">closed</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !list.length" description="暂无 Sprint" />
    </el-card>

    <el-dialog :title="form.id ? '编辑 Sprint' : '新建 Sprint'" :visible.sync="visible" width="620px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="目标">
          <el-input v-model="form.goal" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="planned" value="planned" />
            <el-option label="active" value="active" />
            <el-option label="closed" value="closed" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="yyyy-MM-dd" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="yyyy-MM-dd" style="width:100%" />
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
  listProjectSprints,
  getCurrentProjectSprint,
  createProjectSprint,
  updateProjectSprint,
  changeProjectSprintStatus
} from '@/api/projectSprint'

function p(r) {
  if (r && r.data !== undefined) return r.data
  return r || {}
}

export default {
  name: 'ProjectSprintManage',
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
      current: {},
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
        goal: '',
        status: 'planned',
        startDate: '',
        endDate: ''
      }
    },
    statusType(v) {
      return { planned: 'info', active: 'warning', closed: 'success' }[v] || 'info'
    },
    async loadAll() {
      await Promise.all([this.loadCurrent(), this.loadList()])
    },
    async loadCurrent() {
      const r = await getCurrentProjectSprint(this.projectId).catch(() => ({}))
      this.current = p(r)
    },
    async loadList() {
      this.loading = true
      try {
        const r = await listProjectSprints(this.projectId, this.status ? { status: this.status } : {})
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
        goal: row.goal || '',
        status: row.status || 'planned',
        startDate: row.startDate || '',
        endDate: row.endDate || ''
      }
      this.visible = true
    },
    async save() {
      if (!this.form.name) {
        this.$message.warning('请填写 Sprint 名称')
        return
      }
      this.saving = true
      try {
        const d = { ...this.form, projectId: Number(this.projectId) }
        if (d.id) {
          await updateProjectSprint(d.id, d)
          this.$message.success('Sprint 已更新')
        } else {
          await createProjectSprint(d)
          this.$message.success('Sprint 已创建')
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
        await changeProjectSprintStatus(row.id, v)
        this.$message.success('状态已更新')
        await this.loadAll()
      } catch (e) {
        this.$message.error(e.response?.data?.message || '状态更新失败')
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
  margin-bottom: 16px;
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
</style>
