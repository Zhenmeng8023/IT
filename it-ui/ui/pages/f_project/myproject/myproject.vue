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
      
      <el-card class="stat-card" shadow="hover">
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
            style="width: 300px;"
          />
        </div>
        <div class="filter-right">
          <el-select
            v-model="filterStatus"
            placeholder="项目状态"
            clearable
            @change="handleFilterChange"
            style="width: 120px;"
          >
            <el-option label="全部" value=""></el-option>
            <el-option label="进行中" value="开发中"></el-option>
            <el-option label="已完成" value="已完成"></el-option>
            <el-option label="维护中" value="维护中"></el-option>
            <el-option label="已归档" value="已归档"></el-option>
          </el-select>
          
          <el-select
            v-model="filterType"
            placeholder="项目类型"
            clearable
            @change="handleFilterChange"
            style="width: 120px; margin-left: 10px;"
          >
            <el-option label="全部" value=""></el-option>
            <el-option label="Web应用" value="Web应用"></el-option>
            <el-option label="移动应用" value="移动应用"></el-option>
            <el-option label="桌面应用" value="桌面应用"></el-option>
            <el-option label="工具库" value="工具库"></el-option>
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
            <div class="card-content">
              <!-- 项目头部 -->
              <div class="project-header">
                <h3 class="project-title" @click="goToDetail(project.id)">
                  {{ project.title }}
                </h3>
                <div class="project-actions">
                  <el-button 
                    type="text" 
                    icon="el-icon-edit" 
                    size="mini"
                    @click="handleEdit(project)"
                  >
                    编辑
                  </el-button>
                  <el-button 
                    type="text" 
                    icon="el-icon-delete" 
                    size="mini"
                    @click="handleDelete(project)"
                  >
                    删除
                  </el-button>
                </div>
              </div>
              
              <!-- 项目类型和状态 -->
              <div class="project-meta">
                <el-tag 
                  :type="getProjectTypeTag(project.type)" 
                  size="small"
                  class="type-tag"
                >
                  {{ project.type }}
                </el-tag>
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
              <div class="tech-stack" v-if="project.technologies && project.technologies.length > 0">
                <el-tag
                  v-for="tech in project.technologies.slice(0, 4)"
                  :key="tech"
                  size="small"
                  class="tech-tag"
                >
                  {{ tech }}
                </el-tag>
                <span v-if="project.technologies.length > 4" class="more-tech">
                  +{{ project.technologies.length - 4 }}更多
                </span>
              </div>
              
              <!-- 统计信息 -->
              <div class="project-stats">
                <span class="stat-item">
                  <i class="el-icon-star-off"></i>
                  <span>{{ project.starCount || 0 }}</span>
                </span>
                <span class="stat-item">
                  <i class="el-icon-share"></i>
                  <span>{{ project.forkCount || 0 }}</span>
                </span>
                <span class="stat-item">
                  <i class="el-icon-view"></i>
                  <span>{{ project.viewCount || 0 }}</span>
                </span>
                <span class="stat-item">
                  <i class="el-icon-chat-dot-round"></i>
                  <span>{{ project.issueCount || 0 }}</span>
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

    <!-- 创建/编辑项目对话框 -->
    <el-dialog
      :title="editDialogTitle"
      v-model="showEditDialog"
      width="600px"
    >
      <project-edit-form 
        :project="editingProject"
        :is-edit="isEditing"
        @success="handleEditSuccess"
        @cancel="showEditDialog = false"
      />
    </el-dialog>

    <!-- 删除确认对话框 -->
    <el-dialog
      title="确认删除"
      v-model="showDeleteDialog"
      width="400px"
    >
      <div class="delete-confirm">
        <p>确定要删除项目 "{{ deletingProject?.title }}" 吗？</p>
        <p class="delete-warning">此操作不可恢复，请谨慎操作！</p>
      </div>
      <template #footer>
        <el-button @click="showDeleteDialog = false">取消</el-button>
        <el-button type="danger" @click="confirmDelete" :loading="deleteLoading">
          确认删除
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// 预留所有API接口
import { 
  GetMyProjects,           // 获取我的项目列表
  GetMyProjectsStats,      // 获取我的项目统计
  CreateProject,           // 创建项目
  UpdateProject,           // 更新项目
  DeleteProject,           // 删除项目
  SearchMyProjects,        // 搜索我的项目
  FilterMyProjects,        // 筛选我的项目
  ExportMyProjects         // 导出我的项目
} from '@/api/index'

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
      filterType: '',
      showEditDialog: false,
      showDeleteDialog: false,
      editingProject: null,
      deletingProject: null,
      isEditing: false,
      deleteLoading: false
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
  methods: {
    // 获取我的项目列表
    async fetchProjects() {
      this.loading = true
      try {
        // 预留API调用 - 获取项目列表
        // const response = await GetMyProjects({
        //   page: this.currentPage,
        //   pageSize: this.pageSize
        // })
        // this.projects = response.data.items
        // this.total = response.data.total
        
        // 预留API调用 - 获取项目统计
        // const statsResponse = await GetMyProjectsStats()
        // this.stats = statsResponse.data
        
        // 模拟数据 - 实际使用时请替换为API调用
        await this.$nextTick()
        this.projects = [
          {
            id: 1,
            title: '博客管理系统',
            type: 'Web应用',
            status: '已完成',
            description: '一个基于Vue.js和Node.js的现代化博客管理系统，支持多用户、标签管理、评论系统等功能。',
            technologies: ['Vue.js', 'Node.js', 'MongoDB', 'Express'],
            starCount: 45,
            forkCount: 12,
            viewCount: 234,
            issueCount: 3,
            createTime: '2024-01-15T10:00:00Z',
            updateTime: '2024-03-20T14:30:00Z'
          },
          {
            id: 2,
            title: '在线文档编辑器',
            type: 'Web应用',
            status: '开发中',
            description: '支持多人协作的在线文档编辑器，实时同步，版本控制。',
            technologies: ['React', 'Node.js', 'WebSocket', 'MongoDB'],
            starCount: 23,
            forkCount: 8,
            viewCount: 156,
            issueCount: 5,
            createTime: '2024-02-10T14:20:00Z',
            updateTime: '2024-03-25T09:15:00Z'
          },
          {
            id: 3,
            title: '移动端任务管理应用',
            type: '移动应用',
            status: '维护中',
            description: '跨平台移动端任务管理应用，支持iOS和Android。',
            technologies: ['React Native', 'Redux', 'Firebase'],
            starCount: 18,
            forkCount: 5,
            viewCount: 89,
            issueCount: 2,
            createTime: '2024-01-20T16:45:00Z',
            updateTime: '2024-03-18T11:30:00Z'
          }
        ]
        this.filteredProjects = this.projects
        this.total = this.projects.length
        
        // 模拟统计数据
        this.stats = {
          totalProjects: this.projects.length,
          activeProjects: this.projects.filter(p => p.status === '开发中').length,
          completedProjects: this.projects.filter(p => p.status === '已完成').length,
          totalStars: this.projects.reduce((sum, project) => sum + (project.starCount || 0), 0)
        }
      } catch (error) {
        this.$message.error('获取项目列表失败')
      } finally {
        this.loading = false
      }
    },

    // 搜索项目
    async handleSearch() {
      if (this.searchKeyword) {
        try {
          // 预留API调用 - 搜索项目
          // const response = await SearchMyProjects({
          //   keyword: this.searchKeyword,
          //   page: this.currentPage,
          //   pageSize: this.pageSize
          // })
          // this.filteredProjects = response.data.items
          // this.total = response.data.total
          
          // 模拟搜索逻辑
          this.filteredProjects = this.projects.filter(project => 
            project.title.toLowerCase().includes(this.searchKeyword.toLowerCase()) ||
            project.description.toLowerCase().includes(this.searchKeyword.toLowerCase())
          )
        } catch (error) {
          this.$message.error('搜索失败')
        }
      } else {
        this.filteredProjects = this.projects
      }
    },

    // 筛选项目
    async handleFilterChange() {
      try {
        // 预留API调用 - 筛选项目
        // const response = await FilterMyProjects({
        //   status: this.filterStatus,
        //   type: this.filterType,
        //   page: this.currentPage,
        //   pageSize: this.pageSize
        // })
        // this.filteredProjects = response.data.items
        // this.total = response.data.total
        
        // 模拟筛选逻辑
        this.filteredProjects = this.projects.filter(project => {
          const statusMatch = !this.filterStatus || project.status === this.filterStatus
          const typeMatch = !this.filterType || project.type === this.filterType
          return statusMatch && typeMatch
        })
      } catch (error) {
        this.$message.error('筛选失败')
      }
    },

    // 新建项目
    handleCreateProject() {
      this.isEditing = false
      this.editingProject = null
      this.showEditDialog = true
    },

    // 编辑项目
    handleEdit(project) {
      this.isEditing = true
      this.editingProject = { ...project }
      this.showEditDialog = true
    },

    // 删除项目
    handleDelete(project) {
      this.deletingProject = project
      this.showDeleteDialog = true
    },

    // 确认删除
    async confirmDelete() {
      this.deleteLoading = true
      try {
        // 预留API调用 - 删除项目
        // await DeleteProject(this.deletingProject.id)
        
        // 模拟删除逻辑
        this.projects = this.projects.filter(p => p.id !== this.deletingProject.id)
        this.filteredProjects = this.filteredProjects.filter(p => p.id !== this.deletingProject.id)
        
        // 更新统计信息
        this.stats.totalProjects = this.projects.length
        this.stats.activeProjects = this.projects.filter(p => p.status === '开发中').length
        this.stats.completedProjects = this.projects.filter(p => p.status === '已完成').length
        this.stats.totalStars = this.projects.reduce((sum, project) => sum + (project.starCount || 0), 0)
        
        this.$message.success('项目删除成功')
        this.showDeleteDialog = false
      } catch (error) {
        this.$message.error('删除失败')
      } finally {
        this.deleteLoading = false
      }
    },

    // 编辑成功回调
    async handleEditSuccess(project) {
      this.showEditDialog = false
      
      if (this.isEditing) {
        // 预留API调用 - 更新项目
        // await UpdateProject(project)
        
        // 更新现有项目
        const index = this.projects.findIndex(p => p.id === project.id)
        if (index !== -1) {
          this.projects.splice(index, 1, project)
        }
        this.$message.success('项目更新成功')
      } else {
        // 预留API调用 - 创建项目
        // const response = await CreateProject(project)
        // const newProject = response.data
        
        // 添加新项目
        const newProject = {
          ...project,
          id: Math.max(...this.projects.map(p => p.id)) + 1,
          starCount: 0,
          forkCount: 0,
          viewCount: 0,
          issueCount: 0,
          createTime: new Date().toISOString(),
          updateTime: new Date().toISOString()
        }
        this.projects.unshift(newProject)
        this.$message.success('项目创建成功')
      }
      
      // 更新筛选后的项目列表
      this.filteredProjects = [...this.projects]
      
      // 更新统计信息
      this.stats.totalProjects = this.projects.length
      this.stats.activeProjects = this.projects.filter(p => p.status === '开发中').length
      this.stats.completedProjects = this.projects.filter(p => p.status === '已完成').length
      this.stats.totalStars = this.projects.reduce((sum, project) => sum + (project.starCount || 0), 0)
    },

    // 分页变化
    handlePageChange(page) {
      this.currentPage = page
      this.fetchProjects()
    },

    // 跳转到项目详情
    goToDetail(id) {
      this.$router.push(`/projectdetail`)
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
        '已完成': 'success',
        '开发中': 'warning',
        '维护中': 'info',
        '已归档': 'danger'
      }
      return statusMap[status] || 'info'
    },

    formatTime(time) {
      if (!time) return ''
      return new Date(time).toLocaleDateString('zh-CN')
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
}
</style>