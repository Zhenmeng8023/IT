<template>
  <div class="my-projects-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <i class="el-icon-s-management"></i>
          我的项目
        </h1>
        <p class="page-subtitle">管理您创建的所有项目</p>
      </div>
      <div class="header-actions">
        <el-button 
          type="primary" 
          icon="el-icon-plus" 
          @click="handleCreateProject"
        >
          新建项目
        </el-button>
      </div>
    </div>

    <!-- 统计信息卡片 -->
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
      
      <!-- <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon active">
            <i class="el-icon-success"></i>
          </div>
          <div class="stat-info">
            <div class="stat-number">{{ stats.activeProjects }}</div>
            <div class="stat-label">进行中</div>
          </div>
        </div>
      </el-card>
      
      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon completed">
            <i class="el-icon-finished"></i>
          </div>
          <div class="stat-info">
            <div class="stat-number">{{ stats.completedProjects }}</div>
            <div class="stat-label">已完成</div>
          </div>
        </div>
      </el-card> -->
      
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

    <!-- 项目列表区域 -->
    <div class="projects-section">
      <!-- 筛选工具栏 -->
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
          <!-- <el-select
            v-model="filterStatus"
            placeholder="项目状态"
            clearable
            @change="handleFilterChange"
            style="width: 120px;"
          >
            <el-option label="全部" value=""></el-option>
            <el-option label="草稿" value="draft"></el-option>
            <el-option label="进行中" value="active"></el-option>
            <el-option label="已完成" value="completed"></el-option>
            <el-option label="已归档" value="archived"></el-option>
          </el-select> -->
          
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
              :value="tag.id"
            ></el-option>
          </el-select>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-loading="loading" element-loading-text="加载中..." class="loading-container">
        <!-- 项目列表 -->
        <div v-if="!loading && filteredProjects.length > 0" class="projects-grid">
          <el-card
            v-for="project in filteredProjects"
            :key="project.id"
            class="project-card"
            shadow="hover"
          >
            <div class="card-content" @click="goToDetail(project.id)">
              <!-- 项目头部 -->
              <div class="project-header">
                <h3 class="project-title">
                  {{ project.name }}
                </h3>
                <div class="project-actions">
                  <el-button 
                    type="text" 
                    icon="el-icon-edit" 
                    size="mini"
                    @click.stop="handleEdit(project)"
                  >
                    编辑
                  </el-button>
                  <el-button 
                    type="text" 
                    icon="el-icon-delete" 
                    size="mini"
                    @click.stop="handleDelete(project)"
                  >
                    删除
                  </el-button>
                </div>
              </div>
              
              <!-- 项目类型和状态 -->
              <div class="project-meta">
                <el-tag 
                  :type="getStatusTag(project.status)" 
                  size="small"
                  class="status-tag"
                >
                  {{ project.status }}
                </el-tag>
                <span class="create-time">{{ formatTime(project.createTime) }}</span>
              </div>
              
              <!-- 项目描述 -->
              <p class="project-description">{{ formatDescription(project.description) }}</p>
              
              <!-- 技术栈标签 -->
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
              
              <!-- 统计信息 -->
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

        <!-- 空状态 -->
        <div v-if="!loading && filteredProjects.length === 0" class="empty-state">
          <div class="empty-icon">
            <i class="el-icon-s-management"></i>
          </div>
          <h3 class="empty-title">暂无项目</h3>
          <p class="empty-desc">您还没有创建任何项目，快去创建第一个项目吧</p>
          <el-button 
            type="primary" 
            icon="el-icon-plus" 
            @click="handleCreateProject"
            class="empty-action"
          >
            创建第一个项目
          </el-button>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page.sync="currentPage"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 使用自定义模态框替代 Element UI Dialog -->
    <transition name="dialog-fade">
      <div v-show="showEditDialog" class="custom-dialog-overlay" @click.self="closeDialog">
        <div class="custom-dialog" v-show="showEditDialog" :style="{ width: '600px' }">
          <div class="custom-dialog-header">
            <span class="custom-dialog-title">{{ editDialogTitle }}</span>
            <button class="custom-dialog-close" @click="closeDialog">×</button>
          </div>
          <div class="custom-dialog-body">
            <el-form :model="projectForm" :rules="projectRules" ref="projectFormRef" label-width="100px">
              <el-form-item label="项目名称" prop="name" required>
                <el-input v-model="projectForm.name" placeholder="请输入项目名称"></el-input>
              </el-form-item>
              <el-form-item label="项目描述" prop="description">
                <el-input type="textarea" v-model="projectForm.description" rows="3" placeholder="请输入项目描述"></el-input>
              </el-form-item>
              <el-form-item label="项目分类" prop="category">
                <el-input v-model="projectForm.category" placeholder="例如：后端开发、前端框架"></el-input>
              </el-form-item>
              <!-- <el-form-item label="项目状态" prop="status">
                <el-select v-model="projectForm.status" placeholder="请选择状态">
                  <el-option
                    v-for="option in statusOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  ></el-option>
                </el-select>
              </el-form-item> -->
              <el-form-item label="可见性" prop="visibility">
                <el-select v-model="projectForm.visibility" placeholder="请选择可见性">
                  <el-option
                    v-for="option in visibilityOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  ></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="技术栈" prop="tags">
                <el-select
                  v-model="projectForm.tags"
                  multiple
                  placeholder="请选择技术栈"
                  style="width: 100%"
                >
                  <el-option
                    v-for="tag in tags"
                    :key="tag.id"
                    :label="tag.name"
                    :value="tag.id"
                  ></el-option>
                </el-select>
                <span class="form-tip">可多选技术栈</span>
              </el-form-item>
              <!-- <el-form-item label="模板ID" prop="templateId">
                <el-input-number v-model="projectForm.templateId" :min="0" :step="1" placeholder="可选"></el-input-number>
              </el-form-item> -->
            </el-form>
          </div>
          <div class="custom-dialog-footer">
            <el-button @click="closeDialog">取消</el-button>
            <el-button type="primary" @click="submitProjectForm">确定</el-button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 自定义删除确认对话框 -->
    <div v-if="showDeleteDialog" class="custom-delete-dialog-overlay">
      <div class="custom-delete-dialog">
        <div class="custom-dialog-header">
          <h3>确认删除</h3>
        </div>
        <div class="custom-dialog-body">
          <p>确定要删除项目 "{{ deletingProject?.name }}" 吗？</p>
          <p class="delete-warning">此操作不可恢复，请谨慎操作！</p>
        </div>
        <div class="custom-dialog-footer">
          <button @click="showDeleteDialog = false" class="cancel-button">取消</button>
          <button 
            @click="confirmDelete" 
            class="delete-button" 
            :disabled="deleteLoading"
          >
            {{ deleteLoading ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
// 导入项目相关的API接口
import { getMyProjects, createProject, updateProject, deleteProject } from '@/api/project'
import { GetAllTags } from '@/api/index'

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
      statusOptions: [
        { label: '草稿', value: 'draft' },
        { label: '待审核', value: 'pending' },
        { label: '已发布', value: 'published' },
        { label: '已拒绝', value: 'rejected' },
        { label: '已归档', value: 'archived' }
      ],
      visibilityOptions: [
        { label: '公开', value: 'public' },
        { label: '私有', value: 'private' }
      ],
      projectForm: {
        name: '',
        description: '',
        category: '',
        status: 'draft',
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
    }
  },
  watch: {
    '$route.query': {
      handler() {
        this.fetchProjects()
      },
      immediate: true
    }
  },
  mounted() {
    this.fetchTags()
  },
  methods: {
    // 获取我的项目列表
    async fetchProjects() {
      console.log('Fetching projects with keyword:', this.searchKeyword)
      this.loading = true
      try {
        // 调用API获取项目列表
        const response = await getMyProjects({
          page: this.currentPage,
          size: this.pageSize,
          status: this.filterStatus,
          keyword: this.searchKeyword
        })
        console.log('API response:', response)

        // 先处理项目中的标签数据
        const processedProjects = response.data.list.map(project => {
          // 处理tags字段，将JSON字符串转换为数组
          if (project.tags) {
            try {
              project.tags = JSON.parse(project.tags)
            } catch (e) {
              project.tags = []
            }
          } else {
            project.tags = []
          }
          return project
        })

        // 将处理后的项目赋值给 this.projects
        this.projects = processedProjects

        // 前端筛选项目
        console.log('Before filtering - projects count:', this.projects.length, 'keyword:', this.searchKeyword)
                this.filteredProjects = this.projects.filter(project => {
                  // 按关键词筛选
                  if (this.searchKeyword) {
                    const keyword = this.searchKeyword.toLowerCase()
                    const matchesKeyword = project.name.toLowerCase().includes(keyword) ||
                                          (project.description && project.description.toLowerCase().includes(keyword))
                    if (!matchesKeyword) return false
                  }
                  
                  // 按状态筛选
                  if (this.filterStatus) {
                    if (project.status !== this.filterStatus) return false
                  }
                  
                  // 按标签筛选
                  if (this.filterTag) {
                    if (!project.tags || !project.tags.includes(this.filterTag)) return false
                  }
                  
                  return true
                })
                console.log('After filtering - filteredProjects count:', this.filteredProjects.length)

        this.total = response.data.total || 0

        // 计算统计数据
        this.stats = {
          totalProjects: this.projects.length,
          activeProjects: this.projects.filter(p => ['pending', 'published'].includes(p.status)).length,
          completedProjects: this.projects.filter(p => p.status === 'archived').length,
          totalStars: this.projects.reduce((sum, project) => sum + (project.stars || 0), 0)
        }
      } catch (error) {
        this.$message.error('获取项目列表失败')
        console.error('Error fetching projects:', error)
      } finally {
        this.loading = false
      }
    },

    // 获取标签列表
    async fetchTags() {
      try {
        const response = await GetAllTags()
        this.tags = response.data || []
      } catch (error) {
        console.error('Error fetching tags:', error)
        // 如果获取失败，使用默认标签
        this.tags = [
          { id: 1, name: '前端' },
          { id: 2, name: '后端' },
          { id: 3, name: '移动开发' },
          { id: 4, name: 'DevOps' }
        ]
      }
    },

    // 搜索项目
    async handleSearch() {
      this.currentPage = 1
      await this.fetchProjects()
    },

    // 筛选项目
    async handleFilterChange() {
      this.currentPage = 1
      await this.fetchProjects()
    },

    // 新建项目
    handleCreateProject() {
      console.log('Creating new project...')
      this.isEditing = false
      this.resetProjectForm()
      
      // 直接显示自定义对话框
      this.showEditDialog = true
      console.log('Custom dialog visibility set to:', this.showEditDialog)
    }, 
    
    // 关闭对话框
    closeDialog() {
      this.showEditDialog = false
    },
    
    // 重置项目表单
    resetProjectForm() {
      this.projectForm = {
        name: '',
        description: '',
        category: '',
        status: 'draft',
        visibility: 'public',
        tags: [],
        templateId: null
      }
      console.log('Form reset completed')
      this.$nextTick(() => {
        if (this.$refs.projectFormRef) {
          this.$refs.projectFormRef.clearValidate()
          console.log('Validation cleared')
        }
      })
    },
    
    // 提交项目表单
    async submitProjectForm() {
      try {
        await this.$refs.projectFormRef.validate()
        
        // 构建请求数据
        const requestData = {
          name: this.projectForm.name,
          description: this.projectForm.description || '',
          category: this.projectForm.category || '',
          status: this.projectForm.status || '',
          visibility: this.projectForm.visibility || 'public',
          templateId: this.projectForm.templateId || null,
          tags: JSON.stringify(this.projectForm.tags || [])
        }

        console.log('Request data:', requestData)
        this.handleEditSuccess(requestData)
      } catch (error) {
        if (error !== 'cancel') {
          console.error('表单验证失败', error)
        }
      }
    },

    // 编辑项目
    handleEdit(project) {
      this.isEditing = true
      // 处理标签数据，如果是字符串则解析为数组
      let tags = []
      if (project.tags) {
        if (typeof project.tags === 'string') {
          try {
            tags = JSON.parse(project.tags)
          } catch (e) {
            tags = []
          }
        } else {
          tags = project.tags
        }
      }
      
      this.projectForm = {
        name: project.name || '',
        description: project.description || '',
        category: project.category || '',
        status: project.status || 'draft',
        visibility: project.visibility || 'public',
        tags: tags,
        templateId: project.templateId || null
      }
      this.editingProject = { ...project }
      this.showEditDialog = true
    },

    // 删除项目
    handleDelete(project) {
      console.log('Delete button clicked, project:', project)
      this.deletingProject = project
      console.log('Setting showDeleteDialog to true')
      this.showDeleteDialog = true
      console.log('showDeleteDialog value:', this.showDeleteDialog)
    },

    // 确认删除
    async confirmDelete() {
      this.deleteLoading = true
      try {
        // 调用API删除项目
        await deleteProject(this.deletingProject.id)
        
        // 更新本地数据
        this.projects = this.projects.filter(p => p.id !== this.deletingProject.id)
        this.filteredProjects = this.filteredProjects.filter(p => p.id !== this.deletingProject.id)
        
        // 更新统计信息
        this.stats.totalProjects = this.projects.length
        this.stats.activeProjects = this.projects.filter(p => ['pending', 'published'].includes(p.status)).length
        this.stats.completedProjects = this.projects.filter(p => p.status === 'archived').length
        this.stats.totalStars = this.projects.reduce((sum, project) => sum + (project.starCount || 0), 0)
        
        this.$message.success('项目删除成功')
        this.showDeleteDialog = false
      } catch (error) {
        this.$message.error('删除失败')
        console.error('Delete error:', error)
      } finally {
        this.deleteLoading = false
      }
    },

    // 编辑成功回调
    async handleEditSuccess(projectData) {
      this.closeDialog() // 关闭自定义对话框
      
      if (this.isEditing) {
        // 调用API更新项目
        await updateProject(this.editingProject.id, projectData)
        
        // 更新现有项目
        const index = this.projects.findIndex(p => p.id === this.editingProject.id)
        if (index !== -1) {
          const updatedProject = { ...this.editingProject, ...projectData }
          this.projects.splice(index, 1, updatedProject)
        }
        this.$message.success('项目更新成功')
      } else {
        // 调用API创建项目
        const response = await createProject(projectData)
        const newProject = response.data
        
        // 添加新项目
        this.projects.unshift(newProject)
        this.$message.success('项目创建成功')
        
        // 跳转到新项目详情页
        this.$nextTick(() => {
          this.goToDetail(newProject.id)
        })
      }
      
      // 更新筛选后的项目列表
      this.filteredProjects = this.projects.filter(project => {
        // 按关键词筛选
        if (this.searchKeyword) {
          const keyword = this.searchKeyword.toLowerCase()
          const matchesKeyword = project.name.toLowerCase().includes(keyword) ||
                                (project.description && project.description.toLowerCase().includes(keyword))
          if (!matchesKeyword) return false
        }
        
        // 按状态筛选
        if (this.filterStatus) {
          if (project.status !== this.filterStatus) return false
        }
        
        // 按标签筛选
        if (this.filterTag) {
          if (!project.tags || !project.tags.includes(this.filterTag)) return false
        }
        
        return true
      })
      
      // 更新统计信息
      this.stats.totalProjects = this.projects.length
      this.stats.activeProjects = this.projects.filter(p => ['pending', 'published'].includes(p.status)).length
      this.stats.completedProjects = this.projects.filter(p => p.status === 'archived').length
      this.stats.totalStars = this.projects.reduce((sum, project) => sum + (project.starCount || 0), 0)
    },

    // 分页变化
    handlePageChange(page) {
      this.currentPage = page
      this.fetchProjects()
    },

    // 跳转到项目详情
goToDetail(id) {
  console.log('goToDetail called with id:', id)
  console.log('Current router:', this.$router)
  try {
    this.$router.push(`/projectdetail?projectId=${id}`)
    console.log('Router push successful')
  } catch (error) {
    console.error('Router push error:', error)
  }
},

    // 工具方法 - 与现有项目页面保持一致
    getProjectTypeTag(type) {
      const typeMap = {
        'Web应用': 'primary',
        '移动应用': 'success',
        '桌面应用': 'warning',
        '工具库': 'info'
      }
      return typeMap[type] || 'info'
    },

    getStatusTag(status) {
      const statusMap = {
        'completed': 'success',
        '已完成': 'success',
        'active': 'warning',
        '开发中': 'warning',
        '维护中': 'info',
        'archived': 'danger',
        '已归档': 'danger',
        'draft': 'info'
      }
      return statusMap[status] || 'info'
    },

    // 根据标签ID获取标签名称
    getTagName(tagId) {
      const tag = this.tags.find(t => t.id === tagId)
      return tag ? tag.name : `标签${tagId}`
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
</style>