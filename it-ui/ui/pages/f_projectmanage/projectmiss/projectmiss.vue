<template>
  <div class="project-miss">
    <AdminPageHeader
      title="项目下架管理"
      description="管理已下架项目，支持筛选、恢复、永久删除和批量处理。">
      <template slot="extra">
        <el-input
          v-model.trim="projectIdInput"
          size="small"
          placeholder="可选：绑定项目 ID"
          class="project-id-input"
          @keyup.enter.native="applyProjectId" />
        <el-button size="small" @click="applyProjectId">绑定项目</el-button>
      </template>
    </AdminPageHeader>

    <ProjectManageEntryHub
      current-key="miss"
      title="管理侧统一入口"
      subtitle="在项目管理、审核中心和下架管理之间切换时，尽量保留同一项目上下文。"
      :project-id="projectId"
      :entries="manageEntries"
      :status-cards="statusCards" />

    <AdminToolbarCard class="filter-card">
      <div class="filter-toolbar">
        <div class="filter-left">
          <el-select v-model="filterForm.status" clearable placeholder="下架状态" style="width: 120px">
            <el-option label="已下架" value="removed" />
            <el-option label="已恢复" value="restored" />
            <el-option label="已删除" value="deleted" />
          </el-select>

          <el-input
            v-model.trim="filterForm.type"
            clearable
            placeholder="项目类型"
            style="width: 140px; margin-left: 10px" />

          <el-date-picker
            v-model="filterForm.removeDateRange"
            type="daterange"
            value-format="yyyy-MM-dd"
            range-separator="至"
            start-placeholder="下架开始日期"
            end-placeholder="下架结束日期"
            style="width: 260px; margin-left: 10px" />
        </div>

        <div class="filter-right">
          <el-input
            v-model.trim="filterForm.keyword"
            clearable
            placeholder="搜索项目名称或下架人"
            style="width: 260px"
            prefix-icon="el-icon-search"
            @input="handleSearch" />
        </div>
      </div>
    </AdminToolbarCard>

    <AdminToolbarCard class="toolbar-card">
      <div class="toolbar">
        <el-button
          type="success"
          icon="el-icon-refresh-left"
          :disabled="selectedProjects.length === 0"
          @click="handleBatchRestore">
          批量恢复
        </el-button>
        <el-button
          type="danger"
          icon="el-icon-delete"
          :disabled="selectedProjects.length === 0"
          @click="handleBatchDelete">
          批量永久删除
        </el-button>
        <el-button icon="el-icon-refresh" @click="refreshData">刷新</el-button>
      </div>
    </AdminToolbarCard>

    <AdminTableCard class="table-card">
      <el-table
        :data="projectList"
        v-loading="loading"
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />

        <el-table-column prop="name" label="项目名称" min-width="220">
          <template slot-scope="scope">
            <div class="project-name">
              <span class="name-text">{{ scope.row.name }}</span>
              <el-tag v-if="scope.row.isRecommended" size="mini" type="warning" style="margin-left: 8px">推荐</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="applicant" label="申请人" width="140">
          <template slot-scope="scope">
            <el-avatar :size="24" :src="scope.row.avatar" style="vertical-align: middle; margin-right: 6px" />
            {{ scope.row.applicant || '-' }}
          </template>
        </el-table-column>

        <el-table-column prop="type" label="项目类型" width="120" align="center">
          <template slot-scope="scope">
            <StatusTag :value="scope.row.type" :type-map="projectTypeTagMap" default-type="info" size="small" />
          </template>
        </el-table-column>

        <el-table-column prop="budget" label="预算(元)" width="140" align="right">
          <template slot-scope="scope">
            {{ formatBudget(scope.row.budget) }}
          </template>
        </el-table-column>

        <el-table-column prop="removeTime" label="下架时间" width="170" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.removeTime) }}
          </template>
        </el-table-column>

        <el-table-column prop="remover" label="下架人" width="120" align="center" />

        <el-table-column prop="removeReason" label="下架原因" min-width="170">
          <template slot-scope="scope">
            <el-tooltip
              v-if="toText(scope.row.removeReason).length > 20"
              :content="toText(scope.row.removeReason)"
              placement="top">
              <span>{{ shortText(scope.row.removeReason, 20) }}</span>
            </el-tooltip>
            <span v-else>{{ toText(scope.row.removeReason) || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="110" align="center">
          <template slot-scope="scope">
            <StatusTag
              :value="scope.row.status"
              :text-map="statusTextMap"
              :type-map="statusTagMap"
              size="small" />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="250" fixed="right" align="center">
          <template slot-scope="scope">
            <AdminActionGroup>
              <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)">查看</el-button>
              <el-button
                v-if="scope.row.status === 'removed'"
                size="mini"
                type="text"
                icon="el-icon-refresh-left"
                style="color: #67c23a"
                @click="handleRestore(scope.row)">
                恢复
              </el-button>
              <el-button
                v-if="scope.row.status === 'removed'"
                size="mini"
                type="text"
                icon="el-icon-delete"
                style="color: #f56c6c"
                @click="handleDelete(scope.row)">
                永久删除
              </el-button>
            </AdminActionGroup>
          </template>
        </el-table-column>
      </el-table>

      <template slot="pagination">
        <el-pagination
          :current-page="pagination.currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </template>
    </AdminTableCard>

    <AdminFormDialog
      :title="currentProject ? `${currentProject.name} - 下架详情` : '项目下架详情'"
      :visible.sync="detailDialogVisible"
      width="68%"
      :loading="detailLoading">
      <div v-if="currentProject" class="project-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="项目名称">{{ currentProject.name }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ currentProject.applicant || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目类型">{{ currentProject.type || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预算">{{ formatBudget(currentProject.budget) }} 元</el-descriptions-item>
          <el-descriptions-item label="下架时间">{{ formatDate(currentProject.removeTime) }}</el-descriptions-item>
          <el-descriptions-item label="下架人">{{ currentProject.remover || '-' }}</el-descriptions-item>
          <el-descriptions-item label="下架原因" :span="2">{{ toText(currentProject.removeReason) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目描述" :span="2">{{ toText(currentProject.description) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="技术栈" :span="2">{{ toText(currentProject.techStack) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="团队成员" :span="2">{{ toText(currentProject.teamMembers) || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template slot="footer">
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="currentProject && currentProject.status === 'removed'"
          type="success"
          @click="handleRestore(currentProject)">
          恢复项目
        </el-button>
        <el-button
          v-if="currentProject && currentProject.status === 'removed'"
          type="danger"
          @click="handleDelete(currentProject)">
          永久删除
        </el-button>
      </template>
    </AdminFormDialog>

    <AdminFormDialog
      title="永久删除确认"
      :visible.sync="deleteDialogVisible"
      width="440px"
      :loading="deleteLoading"
      :confirm-text="'确认删除'"
      :cancel-text="'取消'"
      @confirm="confirmDelete">
      <div class="delete-warning">
        <el-alert
          title="警告"
          type="error"
          description="该操作将永久删除项目数据，删除后不可恢复，请谨慎操作。"
          show-icon
          :closable="false" />
        <div style="margin-top: 15px;">
          <p>
            请输入项目名称确认删除：
            <strong>{{ currentProject ? currentProject.name : '' }}</strong>
          </p>
          <el-input
            v-model.trim="deleteConfirmText"
            placeholder="输入项目名称确认"
            style="margin-top: 10px" />
        </div>
      </div>
      <template slot="footer">
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button
          type="danger"
          :loading="deleteLoading"
          :disabled="!canConfirmDelete"
          @click="confirmDelete">
          确认删除
        </el-button>
      </template>
    </AdminFormDialog>
  </div>
</template>

<script>
import ProjectManageEntryHub from '../../f_project/projectmanage/components/ProjectManageEntryHub.vue'
import {
  getArchivedProjectsPage,
  restoreArchivedProject,
  deleteArchivedProject,
  batchRestoreArchivedProjects,
  batchDeleteArchivedProjects
} from '@/api/adminProject'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminToolbarCard from '@/components/admin/AdminToolbarCard.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminActionGroup from '@/components/admin/AdminActionGroup.vue'
import StatusTag from '@/components/admin/StatusTag.vue'
import AdminFormDialog from '@/components/admin/AdminFormDialog.vue'

export default {
  name: 'ProjectMiss',
  layout: 'manage',
  components: {
    ProjectManageEntryHub,
    AdminPageHeader,
    AdminToolbarCard,
    AdminTableCard,
    AdminActionGroup,
    StatusTag,
    AdminFormDialog
  },
  data() {
    const queryProjectId = this.$route && this.$route.query ? this.$route.query.projectId : ''
    return {
      projectId: queryProjectId ? Number(queryProjectId) : null,
      projectIdInput: queryProjectId ? String(queryProjectId) : '',
      filterForm: {
        status: '',
        type: '',
        removeDateRange: [],
        keyword: ''
      },
      projectList: [],
      selectedProjects: [],
      loading: false,
      detailLoading: false,
      deleteLoading: false,
      pagination: {
        currentPage: 1,
        pageSize: 20,
        total: 0
      },
      detailDialogVisible: false,
      deleteDialogVisible: false,
      currentProject: null,
      deleteConfirmText: '',
      statusTextMap: {
        removed: '已下架',
        restored: '已恢复',
        deleted: '已删除'
      },
      statusTagMap: {
        removed: 'warning',
        restored: 'success',
        deleted: 'danger'
      },
      projectTypeTagMap: {
        tech: 'primary',
        design: 'success',
        operation: 'warning',
        other: 'info'
      }
    }
  },
  computed: {
    canConfirmDelete() {
      if (!this.currentProject) return false
      return Boolean(this.deleteConfirmText && this.deleteConfirmText === this.currentProject.name)
    },
    returnManageTab() {
      const raw = String((this.$route.query && this.$route.query.fromTab) || 'overview')
      const allow = [
        'overview',
        'repo-workbench',
        'audit-manage',
        'milestone-manage',
        'release-manage',
        'stat-manage',
        'task-manage',
        'member-manage',
        'file-manage',
        'doc-manage',
        'activity-manage',
        'sprint-manage',
        'download-manage',
        'settings'
      ]
      return allow.includes(raw) ? raw : 'overview'
    },
    removedCount() {
      return this.projectList.filter(item => item.status === 'removed').length
    },
    restoredCount() {
      return this.projectList.filter(item => item.status === 'restored').length
    },
    deletedCount() {
      return this.projectList.filter(item => item.status === 'deleted').length
    },
    manageEntries() {
      return [
        {
          key: 'manage',
          title: '项目管理',
          desc: '进入项目总览、任务、成员、仓库和设置的统一管理入口。',
          path: '/projectmanage',
          query: this.projectId ? { projectId: String(this.projectId), tab: this.returnManageTab } : undefined,
          requiresProjectId: true,
          disabled: !this.projectId,
          tone: 'blue'
        },
        {
          key: 'audit',
          title: '审核中心',
          desc: '进入 MR、评审、检查和主线保护的统一治理入口。',
          path: '/projectaudit',
          query: this.projectId ? { projectId: String(this.projectId), fromTab: this.returnManageTab } : undefined,
          requiresProjectId: true,
          disabled: !this.projectId,
          tone: 'cyan'
        },
        {
          key: 'miss',
          title: '下架管理',
          desc: '处理已下架项目的恢复、删除和状态复核。',
          path: '/projectmiss',
          query: this.projectId ? { projectId: String(this.projectId), fromTab: this.returnManageTab } : undefined,
          tone: 'orange'
        }
      ]
    },
    statusCards() {
      return [
        {
          key: 'context',
          label: '项目上下文',
          value: this.projectId ? `#${this.projectId}` : '全局治理',
          desc: this.projectId ? '当前跳转到项目管理或审核中心时会沿用同一项目上下文。' : '当前从全局视角查看下架治理记录。',
          tone: 'blue'
        },
        {
          key: 'total',
          label: '当前列表',
          value: this.projectList.length,
          desc: '当前页面已加载的下架治理记录数量。',
          tone: 'cyan'
        },
        {
          key: 'removed',
          label: '待处理下架',
          value: this.removedCount,
          desc: this.removedCount ? '这些记录还可以继续恢复或永久删除。' : '当前没有待处理的下架记录。',
          tone: this.removedCount ? 'orange' : 'purple'
        },
        {
          key: 'selection',
          label: '当前选中',
          value: this.selectedProjects.length,
          desc: this.selectedProjects.length ? '已可执行批量恢复或批量删除。' : '先勾选记录，再进行批量治理。',
          tone: this.selectedProjects.length ? 'danger' : 'purple'
        }
      ]
    }
  },
  watch: {
    '$route.query.projectId': {
      immediate: false,
      handler(value) {
        this.projectId = value ? Number(value) : null
        this.projectIdInput = value ? String(value) : ''
      }
    }
  },
  mounted() {
    this.loadProjectList()
  },
  methods: {
    unwrapResponse(raw) {
      if (!raw) return {}
      if (raw.data !== undefined && raw.status !== undefined) {
        return raw.data || {}
      }
      return raw
    },
    unwrapData(raw) {
      const body = this.unwrapResponse(raw)
      if (body && body.data !== undefined) {
        return body.data
      }
      return body
    },
    toText(value) {
      if (value === null || value === undefined) return ''
      if (typeof value === 'string') return value
      if (typeof value === 'number' || typeof value === 'boolean') return String(value)
      try {
        return JSON.stringify(value, null, 2)
      } catch (error) {
        return String(value)
      }
    },
    shortText(value, maxLength) {
      const text = this.toText(value)
      if (text.length <= maxLength) return text
      return `${text.slice(0, maxLength)}...`
    },
    normalizeStatus(rawStatus) {
      const status = String(rawStatus || '').toLowerCase()
      if (['restored', 'published', 'active'].includes(status)) return 'restored'
      if (['deleted', 'removed_deleted'].includes(status)) return 'deleted'
      return 'removed'
    },
    normalizeProjectItem(item) {
      return {
        id: item.id || item.projectId,
        name: item.name || item.title || item.projectName || `项目#${item.id || '-'}`,
        applicant: item.applicant || item.authorName || item.creatorName || item.ownerName || '',
        avatar: item.avatar || item.authorAvatar || item.creatorAvatar || '',
        type: item.type || item.category || item.projectType || 'other',
        budget: Number(item.budget || item.amount || 0),
        removeTime: item.removeTime || item.removedAt || item.archivedAt || item.updatedAt || item.createTime,
        remover: item.remover || item.removedByName || item.updaterName || item.updatedByName || '',
        removeReason: item.removeReason || item.reason || item.rejectReason || item.archiveReason || '',
        status: this.normalizeStatus(item.status),
        isRecommended: Boolean(item.isRecommended || item.recommended),
        description: item.description || item.summary || '',
        techStack: item.techStack || item.stack || '',
        teamMembers: item.teamMembers || item.members || '',
        raw: item
      }
    },
    mapStatusToApi(status) {
      const map = {
        removed: 'archived',
        restored: 'published',
        deleted: 'deleted'
      }
      return map[status] || undefined
    },
    buildQueryParams() {
      const params = {
        page: this.pagination.currentPage,
        size: this.pagination.pageSize,
        keyword: this.filterForm.keyword || undefined,
        type: this.filterForm.type || undefined,
        status: this.mapStatusToApi(this.filterForm.status)
      }
      if (this.projectId) {
        params.projectId = this.projectId
      }
      const range = this.filterForm.removeDateRange || []
      if (Array.isArray(range) && range.length === 2) {
        params.startDate = range[0]
        params.endDate = range[1]
        params.startTime = range[0]
        params.endTime = range[1]
      }
      return params
    },
    applyProjectId() {
      if (!this.projectIdInput) {
        this.projectId = null
        this.$router.replace({
          path: this.$route.path,
          query: Object.keys(this.$route.query || {}).reduce((result, key) => {
            if (key !== 'projectId') {
              result[key] = this.$route.query[key]
            }
            return result
          }, {})
        })
        this.refreshData()
        return
      }
      const nextId = Number(this.projectIdInput)
      if (!nextId) {
        this.$message.warning('请输入有效的项目 ID')
        return
      }
      this.projectId = nextId
      this.$router.replace({
        path: this.$route.path,
        query: {
          ...this.$route.query,
          projectId: String(nextId)
        }
      })
      this.refreshData()
    },
    async loadProjectList() {
      this.loading = true
      try {
        const response = await getArchivedProjectsPage(this.buildQueryParams())
        const data = this.unwrapData(response) || {}
        const list =
          (Array.isArray(data.list) && data.list) ||
          (Array.isArray(data.records) && data.records) ||
          (Array.isArray(data.items) && data.items) ||
          (Array.isArray(data) && data) ||
          []

        this.projectList = list.map(item => this.normalizeProjectItem(item))
        const total = Number(data.total || data.count || data.totalCount || this.projectList.length)
        this.pagination.total = Number.isFinite(total) ? total : this.projectList.length
      } catch (error) {
        console.error('load archived projects failed:', error)
        this.$message.error(error?.response?.data?.message || error.message || '加载下架项目列表失败')
        this.projectList = []
        this.pagination.total = 0
      } finally {
        this.loading = false
      }
    },
    handleSelectionChange(selection) {
      this.selectedProjects = selection
    },
    handleSearch() {
      this.pagination.currentPage = 1
      this.loadProjectList()
    },
    refreshData() {
      this.pagination.currentPage = 1
      this.loadProjectList()
    },
    handleSizeChange(size) {
      this.pagination.pageSize = size
      this.pagination.currentPage = 1
      this.loadProjectList()
    },
    handleCurrentChange(page) {
      this.pagination.currentPage = page
      this.loadProjectList()
    },
    handleView(project) {
      this.currentProject = project
      this.detailDialogVisible = true
    },
    handleDelete(project) {
      this.currentProject = project
      this.deleteConfirmText = ''
      this.deleteDialogVisible = true
    },
    async handleRestore(project) {
      try {
        await this.$confirm('确认恢复该项目吗？', '提示', {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await restoreArchivedProject(project.id)
        this.$message.success('项目恢复成功')
        this.detailDialogVisible = false
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('restore archived project failed:', error)
          this.$message.error(error?.response?.data?.message || error.message || '恢复项目失败')
        }
      }
    },
    async confirmDelete() {
      if (!this.currentProject || !this.canConfirmDelete) return
      this.deleteLoading = true
      try {
        await deleteArchivedProject(this.currentProject.id)
        this.$message.success('项目永久删除成功')
        this.deleteDialogVisible = false
        this.detailDialogVisible = false
        this.refreshData()
      } catch (error) {
        console.error('delete archived project failed:', error)
        this.$message.error(error?.response?.data?.message || error.message || '永久删除项目失败')
      } finally {
        this.deleteLoading = false
      }
    },
    async handleBatchRestore() {
      const projectIds = this.selectedProjects.map(item => item.id).filter(Boolean)
      if (!projectIds.length) return
      try {
        await this.$confirm(`确认批量恢复 ${projectIds.length} 个项目吗？`, '提示', {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await batchRestoreArchivedProjects(projectIds)
        this.$message.success(`批量恢复 ${projectIds.length} 个项目成功`)
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('batch restore archived projects failed:', error)
          this.$message.error(error?.response?.data?.message || error.message || '批量恢复失败')
        }
      }
    },
    async handleBatchDelete() {
      const projectIds = this.selectedProjects.map(item => item.id).filter(Boolean)
      if (!projectIds.length) return
      try {
        await this.$confirm(`确认批量永久删除 ${projectIds.length} 个项目吗？该操作不可恢复。`, '警告', {
          confirmButtonText: '确认删除',
          cancelButtonText: '取消',
          type: 'error',
          confirmButtonClass: 'el-button--danger'
        })
        await batchDeleteArchivedProjects(projectIds)
        this.$message.success(`批量永久删除 ${projectIds.length} 个项目成功`)
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('batch delete archived projects failed:', error)
          this.$message.error(error?.response?.data?.message || error.message || '批量删除失败')
        }
      }
    },
    formatDate(date) {
      if (!date) return '-'
      const dateObj = new Date(date)
      if (Number.isNaN(dateObj.getTime())) return String(date)
      return dateObj.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    },
    formatBudget(budget) {
      const amount = Number(budget || 0)
      if (!Number.isFinite(amount)) return '0'
      return amount.toLocaleString('zh-CN')
    }
  }
}
</script>

<style scoped>
.project-miss {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.project-id-input {
  width: 180px;
}

.filter-card,
.toolbar-card {
  margin-bottom: 0;
}

.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.filter-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.project-name {
  display: flex;
  align-items: center;
}

.name-text {
  font-weight: 500;
}

.project-detail {
  max-height: 420px;
  overflow-y: auto;
}

.delete-warning {
  padding: 8px 0;
}

@media (max-width: 768px) {
  .project-miss {
    padding: 16px;
  }

  .project-id-input {
    width: 100%;
  }

  .filter-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-left {
    width: 100%;
    gap: 10px;
  }

  .filter-left :deep(.el-select),
  .filter-left :deep(.el-date-editor),
  .filter-right :deep(.el-input) {
    width: 100% !important;
    margin-left: 0 !important;
  }
}
</style>
