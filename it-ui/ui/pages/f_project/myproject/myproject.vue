<template>
  <div class="my-projects-container">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <i class="el-icon-s-management"></i>
          我的项目
        </h1>
        <p class="page-subtitle">管理你创建的项目，快速进入详情与工作台。</p>
      </div>
      <div class="header-actions">
        <el-button
          v-if="resolvedIsLoggedIn"
          size="small"
          type="primary"
          icon="el-icon-plus"
          class="header-primary-btn"
          @click="handleCreateProject"
        >
          新建项目
        </el-button>
        <el-button
          v-else
          size="small"
          type="primary"
          plain
          icon="el-icon-user"
          class="header-secondary-btn"
          @click="goToLogin"
        >
          登录后创建
        </el-button>
      </div>
    </div>

    <div v-if="!resolvedIsLoggedIn" class="auth-empty-card">
      <div class="auth-empty-icon"><i class="el-icon-lock"></i></div>
      <h3>请先登录后查看我的项目</h3>
      <p>“我的项目”仅展示你创建的项目，登录后才可创建、编辑、删除与进入管理页。</p>
      <div class="auth-empty-actions">
        <el-button type="primary" @click="goToLogin">去登录</el-button>
        <el-button plain @click="goToRegister">去注册</el-button>
      </div>
    </div>

    <template v-else>
      <div class="stats-cards">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon total">
              <i class="el-icon-s-management"></i>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.totalProjects }}</div>
              <div class="stat-label">总项目数</div>
            </div>
          </div>
        </el-card>

        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon stars">
              <i class="el-icon-star-off"></i>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.totalStars }}</div>
              <div class="stat-label">总收藏数</div>
            </div>
          </div>
        </el-card>
      </div>

      <div class="projects-section">
        <div class="filter-toolbar">
          <div class="filter-left">
            <el-input
              v-model="searchKeyword"
              class="search-input"
              placeholder="搜索项目名称或描述..."
              prefix-icon="el-icon-search"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
              @input="handleSearch"
            />
            <el-button
              size="small"
              type="primary"
              icon="el-icon-search"
              class="search-btn"
              @click="handleSearch"
            >
              搜索
            </el-button>
          </div>

          <div class="filter-right">
            <el-select
              v-model="filterStatus"
              class="filter-select status-filter"
              placeholder="项目状态"
              clearable
              @change="handleFilterChange"
            >
              <el-option label="全部" value=""></el-option>
              <el-option label="草稿" value="draft"></el-option>
              <el-option label="已发布" value="published"></el-option>
            </el-select>

            <el-select
              v-model="filterTag"
              class="filter-select tag-filter"
              placeholder="项目标签"
              clearable
              @change="handleFilterChange"
            >
              <el-option label="全部" value=""></el-option>
              <el-option
                v-for="tag in tags"
                :key="tag.id"
                :label="tag.name"
                :value="tag.name"
              />
            </el-select>
          </div>
        </div>

        <div v-loading="loading" element-loading-text="加载中..." class="loading-container">
          <div v-if="!loading && filteredProjects.length > 0" class="projects-grid">
            <el-card
              v-for="project in filteredProjects"
              :key="project.id"
              class="project-card"
              shadow="hover"
            >
              <div class="card-content" @click="goToDetail(project.id)">
                <div class="project-header">
                  <div class="project-title-row">
                    <h3 class="project-title">{{ project.name }}</h3>
                    <el-tag size="mini" type="danger" effect="plain">所有者</el-tag>
                  </div>
                  <div class="project-actions">
                    <el-button type="text" icon="el-icon-view" size="mini" class="card-action-btn" @click.stop="goToDetail(project.id)">详情</el-button>
                    <el-button type="text" icon="el-icon-s-tools" size="mini" class="card-action-btn" @click.stop="goToManage(project.id)">工作台</el-button>
                    <el-button type="text" icon="el-icon-edit" size="mini" class="card-action-btn" @click.stop="handleEdit(project)">编辑</el-button>
                    <el-button type="text" icon="el-icon-delete" size="mini" class="card-action-btn" @click.stop="handleDelete(project)">删除</el-button>
                  </div>
                </div>

                <div class="project-meta">
                  <el-tag
                    :type="getStatusTag(project.status)"
                    size="small"
                    class="status-tag"
                  >
                    {{ formatProjectStatus(project.status) }}
                  </el-tag>
                  <span class="create-time">{{ formatTime(project.createdAt || project.createTime || project.updatedAt || project.updateTime) }}</span>
                </div>

                <p class="project-description">{{ formatDescription(project.description) }}</p>

                <div class="tech-stack" v-if="project.tags && project.tags.length > 0">
                  <el-tag
                    v-for="tagId in project.tags.slice(0, 4)"
                    :key="tagId"
                    size="small"
                    class="tech-tag"
                  >
                    {{ getTagName(tagId) }}
                  </el-tag>
                  <span v-if="project.tags.length > 4" class="more-tech">+{{ project.tags.length - 4 }}更多</span>
                </div>

                <div class="project-stats">
                  <span class="stat-item"><i class="el-icon-star-off"></i><span>{{ project.stars || 0 }}</span></span>
                  <span class="stat-item"><i class="el-icon-download"></i><span>{{ project.downloads || 0 }}</span></span>
                  <span class="stat-item"><i class="el-icon-view"></i><span>{{ project.views || 0 }}</span></span>
                  <span class="stat-item"><i class="el-icon-time"></i><span>{{ formatTime(project.createdAt) }}</span></span>
                </div>
              </div>
            </el-card>
          </div>

          <div v-if="!loading && filteredProjects.length === 0" class="empty-state">
            <div class="empty-icon"><i class="el-icon-folder-opened"></i></div>
            <h3 class="empty-title">暂无项目</h3>
            <p class="empty-desc">还没有创建项目，点击“立即新建项目”开始。</p>
            <div class="empty-action-row">
              <el-button type="primary" icon="el-icon-plus" @click="handleCreateProject">立即新建项目</el-button>
            </div>
          </div>
        </div>

        <div v-if="!loading && total > pageSize" class="pagination-wrapper">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page="currentPage"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </template>

    <ProjectInvitationSidebarNotice @accepted="fetchProjects" />

    <ProjectCreateDialog
      :visible.sync="createDialogVisible"
      :initial-mode="createDialogMode"
      :tags="tags"
      @created="handleProjectCreated"
    />

    <el-dialog :visible.sync="showEditDialog" :title="editDialogTitle" width="680px" append-to-body>
      <el-form ref="projectFormRef" :model="projectForm" :rules="projectRules" label-width="90px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="projectForm.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="projectForm.description" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="项目分类">
          <el-input v-model="projectForm.category" />
        </el-form-item>
        <el-form-item label="可见范围">
          <el-select v-model="projectForm.visibility" style="width: 100%">
            <el-option v-for="item in visibilityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目标签">
          <el-select v-model="projectForm.tags" multiple filterable allow-create default-first-option style="width: 100%">
            <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.name" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="submitProjectForm">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog :visible.sync="showDeleteDialog" width="420px" append-to-body>
      <div class="custom-delete-dialog">
        <div class="delete-icon-wrapper"><i class="el-icon-warning-outline"></i></div>
        <h3>确认删除</h3>
        <p>确定要删除项目“{{ deletingProject && deletingProject.name }}”吗？删除后无法恢复。</p>
        <div class="dialog-footer delete-footer">
          <el-button @click="showDeleteDialog = false">取消</el-button>
          <el-button type="danger" :loading="deleteLoading" @click="confirmDelete">
            {{ deleteLoading ? '删除中...' : '确认删除' }}
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getMyProjects, createProject, updateProject, deleteProject } from '@/api/project'
import { GetAllTags } from '@/api/index'
import ProjectCreateDialog from './components/ProjectCreateDialog.vue'
import ProjectInvitationSidebarNotice from '../components/ProjectInvitationSidebarNotice.vue'
import { getCurrentUser, getToken } from '@/utils/auth'

function parseProjectTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) {
    return tags.map(item => (typeof item === 'string' ? item.trim() : String(item))).filter(Boolean)
  }
  if (typeof tags === 'string') {
    try {
      const parsed = JSON.parse(tags)
      if (Array.isArray(parsed)) {
        return parsed.map(item => (typeof item === 'string' ? item.trim() : String(item))).filter(Boolean)
      }
    } catch (e) {}
    return tags.split(',').map(item => item.trim()).filter(Boolean)
  }
  return []
}

function normalizeProjectStatus(status) {
  const value = String(status || '').trim().toLowerCase()
  if (value === 'draft' || value === '草稿') return 'draft'
  return 'published'
}

function readStoredToken() {
  return getToken() || ''
}

function readCurrentUser() {
  return getCurrentUser()
}

export default {
  layout: 'project',
  components: {
    ProjectCreateDialog,
    ProjectInvitationSidebarNotice
  },
  data() {
    return {
      clientHydrated: false,
      projects: [],
      filteredProjects: [],
      stats: {
        totalProjects: 0,
        activeProjects: 0,
        completedProjects: 0,
        totalStars: 0
      },
      total: 0,
      pageSize: 8,
      currentPage: 1,
      loading: false,
      searchKeyword: '',
      filterStatus: '',
      filterTag: '',
      showEditDialog: false,
      createDialogVisible: false,
      createDialogMode: 'blank',
      showDeleteDialog: false,
      editingProject: null,
      deletingProject: null,
      isEditing: false,
      deleteLoading: false,
      tags: [],
      visibilityOptions: [
        { label: '公开', value: 'public' },
        { label: '私有', value: 'private' }
      ],
      projectForm: {
        name: '',
        description: '',
        category: '',
        status: 'published',
        visibility: 'public',
        tags: [],
        templateId: null
      },
      projectRules: {
        name: [
          { required: true, message: '请输入项目名称', trigger: 'blur' },
          { min: 1, max: 100, message: '长度在 1 到 100 个字符', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    editDialogTitle() {
      return this.isEditing ? '编辑项目' : '新建项目'
    },
    isLoggedIn() {
      return !!readStoredToken() || !!readCurrentUser()
    },
    resolvedIsLoggedIn() {
      return this.clientHydrated && this.isLoggedIn
    }
  },
  watch: {
    '$route.query': {
      async handler() {
        await this.fetchProjects()
        this.handleRouteCreateTrigger()
      },
      immediate: true
    }
  },
  mounted() {
    this.clientHydrated = true
    this.fetchTags()
  },
  methods: {
    normalizeProject(project = {}) {
      const normalizedStatus = normalizeProjectStatus(project.status)
      return {
        ...project,
        status: normalizedStatus,
        _backendStatus: project.status || normalizedStatus,
        tags: parseProjectTags(project.tags),
        stars: Number(project.stars || project.starCount || 0),
        downloads: Number(project.downloads || project.downloadCount || 0),
        views: Number(project.views || project.viewCount || 0)
      }
    },
    ensureLoggedIn(actionText = '执行该操作') {
      if (this.isLoggedIn) return true
      this.$confirm(`需要登录后才能${actionText}，是否前往登录页？`, '未登录', {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$router.push('/login')
      }).catch(() => {})
      return false
    },
    handleRouteCreateTrigger() {
      const flag = this.$route.query.create === '1' || this.$route.query.openCreate === '1'
      if (!this.isLoggedIn || !flag) return
      if (!this.createDialogVisible) this.handleCreateProject()
      const query = { ...this.$route.query }
      delete query.create
      delete query.openCreate
      this.$router.replace({ query }).catch(() => {})
    },
    applyProjectFilters() {
      this.filteredProjects = this.projects.filter(project => {
        if (this.searchKeyword) {
          const keyword = this.searchKeyword.toLowerCase()
          const name = (project.name || '').toLowerCase()
          const description = (project.description || '').toLowerCase()
          if (!name.includes(keyword) && !description.includes(keyword)) return false
        }

        if (this.filterStatus && project.status !== this.filterStatus) return false
        if (this.filterTag && (!project.tags || !project.tags.includes(this.filterTag))) return false
        return true
      })
    },
    updateStats() {
      this.stats = {
        totalProjects: this.projects.length,
        activeProjects: this.projects.filter(p => p.status === 'published').length,
        completedProjects: this.projects.filter(p => p.status === 'draft').length,
        totalStars: this.projects.reduce((sum, project) => sum + Number(project.stars || 0), 0)
      }
    },
    async fetchProjects() {
      if (!this.isLoggedIn) {
        this.projects = []
        this.filteredProjects = []
        this.total = 0
        this.updateStats()
        return
      }
      this.loading = true
      try {
        const response = await getMyProjects({
          page: this.currentPage,
          size: this.pageSize,
          status: this.filterStatus,
          keyword: this.searchKeyword
        })

        const list = Array.isArray(response?.data?.list) ? response.data.list : []
        this.projects = list.map(project => this.normalizeProject(project))
        this.applyProjectFilters()
        this.total = Number(response?.data?.total || 0)
        this.updateStats()
      } catch (error) {
        this.$message.error('获取项目列表失败')
        console.error('Error fetching projects:', error)
        this.projects = []
        this.filteredProjects = []
        this.total = 0
        this.updateStats()
      } finally {
        this.loading = false
      }
    },
    async fetchTags() {
      try {
        const response = await GetAllTags()
        this.tags = Array.isArray(response?.data) ? response.data : []
      } catch (error) {
        console.error('Error fetching tags:', error)
        this.tags = [
          { id: 1, name: '前端' },
          { id: 2, name: '后端' },
          { id: 3, name: '移动开发' },
          { id: 4, name: 'DevOps' }
        ]
      }
    },
    async handleSearch() {
      this.currentPage = 1
      await this.fetchProjects()
    },
    async handleFilterChange() {
      this.currentPage = 1
      await this.fetchProjects()
    },
    handleCreateProject() {
      if (!this.ensureLoggedIn('创建项目')) return
      this.createDialogMode = 'blank'
      this.createDialogVisible = true
    },
    goToLogin() {
      this.$router.push('/login')
    },
    goToRegister() {
      this.$router.push('/registe')
    },
    goToManage(id) {
      if (!this.ensureLoggedIn('进入项目管理页')) return
      this.$router.push({
        path: '/projectmanage',
        query: { projectId: String(id), tab: 'overview' }
      })
    },
    async handleProjectCreated(payload) {
      this.createDialogVisible = false
      await this.fetchProjects()
      const projectId = payload?.id || payload?.projectId || payload?.data?.id || payload?.data?.projectId
      if (projectId) this.$router.push(`/projectdetail?projectId=${projectId}`)
    },
    closeDialog() {
      this.showEditDialog = false
    },
    resetProjectForm() {
      this.projectForm = {
        name: '',
        description: '',
        category: '',
        status: 'published',
        visibility: 'public',
        tags: [],
        templateId: null
      }
      this.$nextTick(() => {
        if (this.$refs.projectFormRef) this.$refs.projectFormRef.clearValidate()
      })
    },
    async submitProjectForm() {
      if (!this.ensureLoggedIn(this.isEditing ? '编辑项目' : '创建项目')) return
      try {
        await this.$refs.projectFormRef.validate()
        const requestData = {
          name: this.projectForm.name,
          description: this.projectForm.description || '',
          category: this.projectForm.category || '',
          status: this.isEditing ? (this.projectForm.status || '') : 'published',
          visibility: this.projectForm.visibility || 'public',
          templateId: this.projectForm.templateId || null,
          tags: JSON.stringify(this.projectForm.tags || [])
        }
        await this.handleEditSuccess(requestData)
      } catch (error) {
        if (error !== 'cancel') console.error('表单验证失败', error)
      }
    },
    handleEdit(project) {
      if (!this.ensureLoggedIn('编辑项目')) return
      this.isEditing = true
      const normalizedProject = this.normalizeProject(project)
      this.projectForm = {
        name: normalizedProject.name || '',
        description: normalizedProject.description || '',
        category: normalizedProject.category || '',
        status: normalizedProject._backendStatus || normalizedProject.status || 'published',
        visibility: normalizedProject.visibility || 'public',
        tags: normalizedProject.tags,
        templateId: normalizedProject.templateId || null
      }
      this.editingProject = { ...normalizedProject }
      this.showEditDialog = true
    },
    handleDelete(project) {
      if (!this.ensureLoggedIn('删除项目')) return
      this.deletingProject = project
      this.showDeleteDialog = true
    },
    async confirmDelete() {
      if (!this.ensureLoggedIn('删除项目')) return
      this.deleteLoading = true
      try {
        await deleteProject(this.deletingProject.id)
        this.projects = this.projects.filter(p => p.id !== this.deletingProject.id)
        this.applyProjectFilters()
        this.updateStats()
        this.total = Math.max(0, this.total - 1)
        this.$message.success('项目删除成功')
        this.showDeleteDialog = false
      } catch (error) {
        this.$message.error('删除失败')
        console.error('Delete error:', error)
      } finally {
        this.deleteLoading = false
      }
    },
    async handleEditSuccess(projectData) {
      this.closeDialog()
      if (this.isEditing) {
        await updateProject(this.editingProject.id, projectData)
        const index = this.projects.findIndex(p => p.id === this.editingProject.id)
        if (index !== -1) {
          const updatedProject = this.normalizeProject({ ...this.editingProject, ...projectData })
          this.projects.splice(index, 1, updatedProject)
        }
        this.$message.success('项目更新成功')
      } else {
        const response = await createProject(projectData)
        const newProject = this.normalizeProject(response?.data || {})
        this.projects.unshift(newProject)
        this.total += 1
        this.$message.success('项目创建成功')
        this.$nextTick(() => this.goToDetail(newProject.id))
      }
      this.applyProjectFilters()
      this.updateStats()
    },
    handlePageChange(page) {
      this.currentPage = page
      this.fetchProjects()
    },
    goToDetail(id) {
      this.$router.push(`/projectdetail?projectId=${id}`)
    },
    formatProjectStatus(status) {
      return normalizeProjectStatus(status) === 'draft' ? '草稿' : '已发布'
    },
    getStatusTag(status) {
      return normalizeProjectStatus(status) === 'draft' ? 'info' : 'success'
    },
    getTagName(tagValue) {
      if (!tagValue) return ''
      if (typeof tagValue === 'string') return tagValue
      const tag = this.tags.find(t => String(t.id) === String(tagValue) || t.name === tagValue)
      return tag ? tag.name : String(tagValue)
    },
    formatTime(time) {
      if (!time) return ''
      return new Date(time).toLocaleString('zh-CN')
    },
    formatDescription(desc) {
      if (!desc) return ''
      return desc.length > 100 ? `${desc.substring(0, 100)}...` : desc
    }
  }
}
</script>

<style scoped>
.my-projects-container {
  --my-card-radius: 8px;
  --my-control-radius: 8px;
  --my-border: var(--it-border, #dbe2ea);
  --my-border-strong: var(--it-border-strong, #b6c3d1);
  --my-surface: var(--it-surface, #ffffff);
  --my-surface-solid: var(--it-surface-solid, #f8fafc);
  --my-page-bg: var(--it-page-bg, #f5f8fc);
  --my-text: var(--it-text, #0f172a);
  --my-muted: var(--it-text-muted, #64748b);
  --my-accent: var(--it-accent, #2563eb);
  --my-accent-soft: var(--it-accent-soft, #e8f1ff);
  --my-shadow: var(--it-shadow, 0 6px 18px rgba(15, 23, 42, 0.06));
  --my-shadow-strong: var(--it-shadow-strong, 0 10px 26px rgba(15, 23, 42, 0.12));

  width: 100% !important;
  max-width: none !important;
  margin: 0 !important;
  padding: 16px 12px 36px !important;
  background: var(--my-page-bg);
}

.page-header,
.auth-empty-card,
.filter-toolbar,
.stat-card,
.project-card,
.empty-state {
  background: var(--my-surface);
  border: 1px solid var(--my-border);
  box-shadow: var(--my-shadow);
  border-radius: var(--my-card-radius);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  margin-bottom: 14px;
}

.header-content {
  min-width: 0;
}

.page-title {
  margin: 0 0 6px;
  font-size: 26px;
  color: var(--my-text);
  line-height: 1.2;
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-title i {
  color: var(--my-accent);
}

.page-subtitle {
  margin: 0;
  color: var(--my-muted);
  font-size: 13px;
  line-height: 1.72;
  max-width: 780px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-actions :deep(.el-button) {
  height: 34px;
  padding: 0 14px;
  border-radius: var(--my-control-radius);
  font-size: 13px;
  font-weight: 600;
}

.header-primary-btn {
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.24);
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.stat-card :deep(.el-card__body) {
  padding: 14px 16px;
}

.stat-card {
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.stat-card:hover {
  border-color: var(--my-border-strong);
  box-shadow: var(--my-shadow-strong);
  transform: translateY(-1px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: var(--my-control-radius);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: var(--my-accent-soft);
  color: var(--my-accent);
}

.stat-number {
  font-size: 22px;
  font-weight: 700;
  color: var(--my-text);
  line-height: 1.2;
}

.stat-label {
  color: var(--my-muted);
  font-size: 12px;
  margin-top: 2px;
}

.projects-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px;
}

.filter-left,
.filter-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  width: 320px;
}

.filter-select {
  width: 136px;
}

.status-filter,
.tag-filter {
  min-width: 132px;
}

.filter-toolbar :deep(.el-input__inner),
.filter-toolbar :deep(.el-select .el-input__inner) {
  height: 34px;
  line-height: 34px;
  border-radius: var(--my-control-radius);
  border-color: var(--my-border);
  background: var(--my-surface-solid);
  color: var(--my-text);
  font-size: 13px;
}

.filter-toolbar :deep(.el-input__inner:focus),
.filter-toolbar :deep(.el-select .el-input__inner:focus) {
  border-color: color-mix(in srgb, var(--my-accent) 50%, var(--my-border));
}

.filter-toolbar :deep(.el-input-group__append .el-button),
.search-btn {
  height: 34px;
  border-radius: var(--my-control-radius);
  padding: 0 12px;
}

.search-btn {
  margin-left: 0;
  min-width: 84px;
  font-weight: 600;
}

.loading-container {
  min-height: 240px;
}

.projects-grid {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 14px;
}

.project-card {
  overflow: hidden;
  height: 100%;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.project-card:hover {
  border-color: var(--my-border-strong);
  box-shadow: var(--my-shadow-strong);
  transform: translateY(-1px);
}

.project-card :deep(.el-card__body) {
  padding: 16px;
  height: 100%;
}

.card-content {
  cursor: pointer;
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.project-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  margin-bottom: 10px;
}

.project-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.project-title {
  margin: 0;
  font-size: 18px;
  color: var(--my-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-title-row :deep(.el-tag) {
  border-radius: var(--my-control-radius);
}

.project-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.project-actions :deep(.card-action-btn.el-button--text) {
  margin-left: 0;
  height: 28px;
  line-height: 26px;
  padding: 0 10px;
  border: 1px solid var(--my-border);
  border-radius: var(--my-control-radius);
  background: var(--my-accent-soft);
  color: var(--my-accent);
  font-size: 12px;
}

.project-actions :deep(.card-action-btn.el-button--text:hover) {
  border-color: var(--my-border-strong);
  background: color-mix(in srgb, var(--my-accent-soft) 78%, #ffffff);
}

.project-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.project-meta :deep(.el-tag) {
  font-weight: 600;
}

.status-tag,
.tech-tag {
  border-radius: var(--my-control-radius);
}

.create-time {
  margin-left: auto;
  font-size: 12px;
  color: var(--my-muted);
}

.project-description {
  margin: 0 0 12px;
  color: var(--my-muted);
  line-height: 1.66;
  font-size: 13px;
  flex-grow: 1;
}

.tech-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.more-tech {
  font-size: 12px;
  color: var(--my-muted);
}

.project-stats {
  margin-top: auto;
  border-top: 1px solid var(--my-border);
  padding-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--my-muted);
  line-height: 1.4;
}

.empty-state {
  text-align: center;
  padding: 44px 20px;
}

.empty-icon {
  font-size: 48px;
  color: var(--my-muted);
  margin-bottom: 12px;
}

.empty-title {
  margin: 0 0 8px;
  color: var(--my-text);
}

.empty-desc {
  margin: 0 0 14px;
  color: var(--my-muted);
  font-size: 13px;
}

.empty-action-row :deep(.el-button) {
  height: 34px;
  padding: 0 14px;
  border-radius: var(--my-control-radius);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 2px;
}

.auth-empty-card {
  text-align: center;
  padding: 30px 18px;
}

.auth-empty-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  background: var(--my-accent-soft);
  color: var(--my-accent);
  font-size: 24px;
}

.auth-empty-card h3 {
  margin: 0 0 8px;
  color: var(--my-text);
}

.auth-empty-card p {
  margin: 0;
  color: var(--my-muted);
  font-size: 13px;
  line-height: 1.7;
}

.auth-empty-actions {
  margin-top: 14px;
  display: flex;
  justify-content: center;
  gap: 10px;
}

.auth-empty-actions :deep(.el-button) {
  height: 34px;
  padding: 0 14px;
  border-radius: var(--my-control-radius);
}

@media (max-width: 900px) {
  .my-projects-container {
    padding: 10px 10px 28px !important;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions :deep(.el-button) {
    flex: 1;
  }

  .filter-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-left,
  .filter-right {
    width: 100%;
    flex-wrap: wrap;
  }

  .search-input,
  .filter-select {
    width: 100%;
  }

  .search-btn {
    width: 100%;
  }

  .projects-grid {
    grid-template-columns: 1fr;
  }

  .project-header {
    grid-template-columns: minmax(0, 1fr);
  }

  .project-actions {
    justify-content: flex-start;
  }
}
</style>
