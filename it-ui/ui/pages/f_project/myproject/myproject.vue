<template>
  <div class="my-projects-container">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <i class="el-icon-s-management"></i>
          我的项目
        </h1>
        <p class="page-subtitle">管理你创建的项目，快速进入详情、工作台或从模板创建新项目</p>
      </div>
      <div class="header-actions">
        <el-button
          v-if="isLoggedIn"
          type="primary"
          icon="el-icon-plus"
          @click="handleCreateProject"
        >
          新建项目
        </el-button>
        <el-button
          v-if="isLoggedIn"
          icon="el-icon-copy-document"
          @click="goToTemplateCenter"
        >
          从模板创建
        </el-button>
        <el-button
          v-if="isLoggedIn"
          icon="el-icon-collection"
          @click="goToTemplateCenter"
        >
          模板中心
        </el-button>
        <el-button
          v-else
          type="primary"
          plain
          icon="el-icon-user"
          @click="goToLogin"
        >
          登录后创建
        </el-button>
      </div>
    </div>

    <div v-if="!isLoggedIn" class="auth-empty-card">
      <div class="auth-empty-icon"><i class="el-icon-lock"></i></div>
      <h3>请先登录后查看我的项目</h3>
      <p>“我的项目”仅展示你创建的项目，登录后才能进行创建、编辑、删除和进入管理台。</p>
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
              placeholder="搜索项目名称或描述..."
              prefix-icon="el-icon-search"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
              @input="handleSearch"
              style="width: 300px;"
            />
            <el-button
              type="primary"
              icon="el-icon-search"
              @click="handleSearch"
              style="margin-left: 10px;"
            >
              搜索
            </el-button>
          </div>
          <div class="filter-right">
            <el-select
              v-model="filterTag"
              placeholder="项目标签"
              clearable
              @change="handleFilterChange"
              style="width: 120px; margin-left: 10px;"
            >
              <el-option label="全部" value=""></el-option>
              <el-option
                v-for="tag in tags"
                :key="tag.id"
                :label="tag.name"
                :value="tag.name"
              ></el-option>
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
                    <el-button type="text" icon="el-icon-view" size="mini" @click.stop="goToDetail(project.id)">
                      详情
                    </el-button>
                    <el-button type="text" icon="el-icon-s-tools" size="mini" @click.stop="goToManage(project.id)">
                      工作台
                    </el-button>
                    <el-button type="text" icon="el-icon-edit" size="mini" @click.stop="handleEdit(project)">
                      编辑
                    </el-button>
                    <el-button type="text" icon="el-icon-delete" size="mini" @click.stop="handleDelete(project)">
                      删除
                    </el-button>
                  </div>
                </div>

                <div class="project-meta">
                  <el-tag
                    :type="getStatusTag(project.status)"
                    size="small"
                    class="status-tag"
                  >
                    {{ project.status }}
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
                  <span v-if="project.tags.length > 4" class="more-tech">
                    +{{ project.tags.length - 4 }}更多
                  </span>
                </div>

                <div class="project-stats">
                  <span class="stat-item">
                    <i class="el-icon-star-off"></i>
                    <span>{{ project.stars || 0 }}</span>
                  </span>
                  <span class="stat-item">
                    <i class="el-icon-download"></i>
                    <span>{{ project.downloads || 0 }}</span>
                  </span>
                  <span class="stat-item">
                    <i class="el-icon-view"></i>
                    <span>{{ project.views || 0 }}</span>
                  </span>
                  <span class="stat-item">
                    <i class="el-icon-time"></i>
                    <span>{{ formatTime(project.createdAt) }}</span>
                  </span>
                </div>
              </div>
            </el-card>
          </div>

          <div v-if="!loading && filteredProjects.length === 0" class="empty-state">
            <div class="empty-icon">
              <i class="el-icon-folder-opened"></i>
            </div>
            <h3 class="empty-title">暂无项目</h3>
            <p class="empty-desc">还没有创建项目，你可以直接新建，也可以从模板快速创建。</p>
            <div class="empty-action-row">
              <el-button type="primary" icon="el-icon-plus" @click="handleCreateProject">立即新建项目</el-button>
              <el-button icon="el-icon-copy-document" @click="goToTemplateCenter">从模板创建项目</el-button>
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

    <el-dialog :visible.sync="showEditDialog" :title="editDialogTitle" width="680px" append-to-body>
      <el-form ref="projectFormRef" :model="projectForm" :rules="projectRules" label-width="90px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="projectForm.name" maxlength="100" show-word-limit></el-input>
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="projectForm.description" type="textarea" :rows="4" maxlength="1000" show-word-limit></el-input>
        </el-form-item>
        <el-form-item label="项目分类">
          <el-input v-model="projectForm.category"></el-input>
        </el-form-item>
        <el-form-item label="可见范围">
          <el-select v-model="projectForm.visibility" style="width: 100%">
            <el-option v-for="item in visibilityOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="项目标签">
          <el-select v-model="projectForm.tags" multiple filterable allow-create default-first-option style="width: 100%">
            <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.name"></el-option>
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
        <p>确定要删除项目 “{{ deletingProject && deletingProject.name }}” 吗？删除后无法恢复。</p>
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

function readStoredToken() {
  if (!process.client) return ''
  try {
    return localStorage.getItem('token') || localStorage.getItem('userToken') || ''
  } catch (e) {
    return ''
  }
}

function parseJwtPayload(token) {
  if (!token || token.split('.').length < 2) return null
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const decoded = decodeURIComponent(
      atob(payload)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(decoded)
  } catch (e) {
    return null
  }
}

function readCurrentUser() {
  if (!process.client) return null
  try {
    const raw = localStorage.getItem('userInfo')
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed && typeof parsed === 'object') return parsed
    }
  } catch (e) {}
  const payload = parseJwtPayload(readStoredToken())
  return payload && typeof payload === 'object' ? payload : null
}

export default {
  layout: 'project',
  data() {
    return {
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
    this.fetchTags()
  },
  methods: {
    normalizeProject(project = {}) {
      return {
        ...project,
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
      if (!this.showEditDialog) {
        this.handleCreateProject()
      }
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
          if (!name.includes(keyword) && !description.includes(keyword)) {
            return false
          }
        }

        if (this.filterStatus && project.status !== this.filterStatus) {
          return false
        }

        if (this.filterTag && (!project.tags || !project.tags.includes(this.filterTag))) {
          return false
        }

        return true
      })
    },
    updateStats() {
      this.stats = {
        totalProjects: this.projects.length,
        activeProjects: this.projects.filter(p => ['pending', 'published'].includes(p.status)).length,
        completedProjects: this.projects.filter(p => p.status === 'archived').length,
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
      this.isEditing = false
      this.editingProject = null
      this.resetProjectForm()
      this.showEditDialog = true
    },
    goToLogin() {
      this.$router.push('/login')
    },
    goToRegister() {
      this.$router.push('/registe')
    },
    goToManage(id) {
      if (!this.ensureLoggedIn('进入项目管理台')) return
      this.$router.push({
        path: '/projectmanage',
        query: { projectId: String(id), tab: 'overview' }
      })
    },
    goToTemplateCenter() {
      if (!this.ensureLoggedIn('从模板创建项目')) return
      this.$router.push({ path: '/projecttemplates', query: { from: 'myproject' } })
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
        if (this.$refs.projectFormRef) {
          this.$refs.projectFormRef.clearValidate()
        }
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
        if (error !== 'cancel') {
          console.error('表单验证失败', error)
        }
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
        status: normalizedProject.status || 'published',
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
        this.$nextTick(() => {
          this.goToDetail(newProject.id)
        })
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
    getStatusTag(status) {
      const statusMap = {
        completed: 'success',
        '已完成': 'success',
        active: 'warning',
        '开发中': 'warning',
        '维护中': 'info',
        archived: 'danger',
        '已归档': 'danger',
        draft: 'info',
        published: 'success',
        pending: 'warning',
        rejected: 'danger'
      }
      return statusMap[status] || 'info'
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
      return desc.length > 100 ? desc.substring(0, 100) + '...' : desc
    }
  }
}
</script>
<style scoped>
.my-projects-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  position: relative;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 30px;
}

.header-content .page-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.header-content .page-subtitle {
  font-size: 16px;
  color: #666;
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.stat-icon.total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.active {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.completed {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.stars {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-info .stat-number {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.stat-info .stat-label {
  font-size: 14px;
  color: #666;
}

/* 筛选工具栏 */
.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.filter-left, .filter-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 项目网格 */
.projects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.project-card {
  border-radius: 8px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.project-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-content {
  padding: 0;
}

/* 项目头部 */
.project-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.project-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
  cursor: pointer;
  transition: color 0.3s;
}

.project-title:hover {
  color: #409eff;
}

.project-actions {
  display: flex;
  gap: 4px;
}

/* 项目元信息 */
.project-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.create-time {
  font-size: 12px;
  color: #999;
  margin-left: auto;
}

/* 项目描述 */
.project-description {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 12px;
}

/* 技术栈 */
.tech-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.tech-tag {
  cursor: default;
}

.more-tech {
  font-size: 12px;
  color: #999;
}

/* 统计信息 */
.project-stats {
  display: flex;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 64px;
  color: #dcdfe6;
  margin-bottom: 20px;
}

.empty-title {
  font-size: 18px;
  color: #666;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: #999;
  margin-bottom: 20px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

/* 删除确认 */
.delete-confirm {
  text-align: center;
}

.delete-warning {
    color: #f56c6c;
    font-size: 14px;
    margin-top: 8px;
  }

  /* 自定义删除确认对话框样式 */
  .custom-delete-dialog-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
  }

  .custom-delete-dialog {
    background-color: white;
    border-radius: 4px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    width: 400px;
  }

  .custom-dialog-header {
    padding: 15px 20px;
    border-bottom: 1px solid #e4e7ed;
  }

  .custom-dialog-header h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 500;
  }

  .custom-dialog-body {
    padding: 20px;
  }

  .custom-dialog-footer {
    padding: 15px 20px;
    border-top: 1px solid #e4e7ed;
    display: flex;
    justify-content: flex-end;
  }

  .cancel-button {
    padding: 8px 16px;
    margin-right: 10px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    background-color: white;
    cursor: pointer;
  }

  .cancel-button:hover {
    color: #409eff;
    border-color: #c6e2ff;
  }

  .delete-button {
    padding: 8px 16px;
    border: 1px solid #f56c6c;
    border-radius: 4px;
    background-color: #f56c6c;
    color: white;
    cursor: pointer;
  }

  .delete-button:hover {
    background-color: #f78989;
    border-color: #f78989;
  }

  .delete-button:disabled {
    background-color: #f56c6c;
    border-color: #f56c6c;
    opacity: 0.6;
    cursor: not-allowed;
  }

/* 自定义对话框样式 */
.custom-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.custom-dialog {
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  max-height: 90vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.custom-dialog-header {
  padding: 20px 24px 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
}

.custom-dialog-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.custom-dialog-close {
  font-size: 24px;
  font-weight: bold;
  color: #909399;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.custom-dialog-close:hover {
  color: #606266;
}

.custom-dialog-body {
  padding: 20px 24px;
  flex: 1;
}

.custom-dialog-footer {
  padding: 15px 24px 20px;
  text-align: right;
  border-top: 1px solid #ebeef5;
}

/* 对话框过渡动画 */
.dialog-fade-enter-active, .dialog-fade-leave-active {
  transition: opacity 0.3s;
}

.dialog-fade-enter, .dialog-fade-leave-to {
  opacity: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .filter-toolbar {
    flex-direction: column;
    gap: 16px;
  }
  
  .filter-left, .filter-right {
    width: 100%;
  }
  
  .filter-left :deep(.el-input) {
    width: 100%;
  }
  
  .projects-grid {
    grid-template-columns: 1fr;
  }
  
  .custom-dialog {
    width: 90% !important;
    margin: 0 5%;
  }
}

.auth-empty-card {
  background: #fff;
  border-radius: 16px;
  padding: 48px 24px;
  text-align: center;
  box-shadow: 0 6px 24px rgba(15, 23, 42, 0.06);
  border: 1px solid #e5e7eb;
}

.auth-empty-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eff6ff;
  color: #3b82f6;
  font-size: 32px;
}

.auth-empty-card h3 {
  margin: 0 0 10px;
  color: #1f2937;
}

.auth-empty-card p {
  margin: 0;
  color: #6b7280;
}

.auth-empty-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.project-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

</style>