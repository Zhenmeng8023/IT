<template>
  <div class="project-container">
    <div class="project-header">
      <h1 class="page-title">我参与的项目</h1>
      <p class="page-subtitle">查看您正在协作的项目，并可直接退出协作</p>
    </div>

    <div v-loading="loading" element-loading-text="加载中..." class="loading-container">
      <div v-if="!loading && collectedProjects.length > 0" class="project-grid">
        <el-card
          v-for="project in collectedProjects"
          :key="project.id"
          class="project-card"
          shadow="hover"
          @click.native="goToDetail(project.id)"
        >
          <div class="card-content">
            <h3 class="project-title">{{ project.name || project.title }}</h3>

            <div class="project-meta">
              <el-tag :type="getProjectTypeTag(project.type || project.category)" size="small" class="type-tag">
                {{ formatProjectType(project.type || project.category) }}
              </el-tag>
              <el-tag :type="getStatusTag(project.status)" size="small" class="status-tag">
                {{ formatProjectStatus(project.status) }}
              </el-tag>
            </div>

            <p class="project-description">{{ formatDescription(project.description) }}</p>

            <div class="project-stats">
              <span v-if="project.stars !== undefined" class="stat-item">
                <i class="el-icon-star-off"></i>
                <span>{{ project.stars }}</span>
              </span>
              <span v-if="project.downloads !== undefined" class="stat-item">
                <i class="el-icon-download"></i>
                <span>{{ project.downloads }}</span>
              </span>
              <span v-if="project.views !== undefined" class="stat-item">
                <i class="el-icon-view"></i>
                <span>{{ project.views }}</span>
              </span>
              <span v-if="project.updatedAt || project.updateTime" class="stat-item">
                <i class="el-icon-time"></i>
                <span>{{ formatTime(project.updatedAt || project.updateTime) }}</span>
              </span>
            </div>

            <div class="collection-actions">
              <el-button
                size="mini"
                type="danger"
                plain
                @click.stop="quitParticipatedProject(project.id)"
                class="remove-btn"
              >
                <i class="el-icon-switch-button"></i>
                退出项目
              </el-button>
            </div>
          </div>
        </el-card>
      </div>

      <div v-if="!loading && collectedProjects.length === 0" class="empty-state">
        <div class="empty-icon">
          <i class="el-icon-user-solid"></i>
        </div>
        <h3 class="empty-title">暂无参与项目</h3>
        <p class="empty-desc">您当前还没有参与任何项目，去项目列表看看吧。</p>
        <el-button type="primary" @click="goToProjectList">去项目列表</el-button>
      </div>
    </div>

    <div v-if="!loading && total > pageSize" class="pagination-wrapper">
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
</template>

<script>
import { getParticipatedProjects, quitProject } from '@/api/project'

export default {
  layout: 'project',
  data() {
    return {
      collectedProjects: [],
      total: 0,
      pageSize: 8,
      loading: false
    }
  },
  computed: {
    currentPage: {
      get() {
        return parseInt(this.$route.query.page, 10) || 1
      },
      set(page) {
        const newQuery = { ...this.$route.query }
        if (page > 1) newQuery.page = page
        else delete newQuery.page
        this.$router.push({ query: newQuery })
      }
    }
  },
  watch: {
    '$route.query': {
      handler() {
        this.fetchCollections()
      },
      immediate: true
    }
  },
  methods: {
    async fetchCollections() {
      this.loading = true
      try {
        const response = await getParticipatedProjects({
          page: this.currentPage,
          size: this.pageSize
        })
        this.collectedProjects = response.data?.list || []
        this.total = response.data?.total || 0
      } catch (error) {
        console.error('获取参与项目失败:', error)
        this.$message.error(error.response?.data?.message || '获取参与项目失败')
      } finally {
        this.loading = false
      }
    },
    async quitParticipatedProject(projectId) {
      try {
        await this.$confirm('确定要退出该项目吗？退出后将无法继续协作。', '提示', { type: 'warning' })
        await quitProject(projectId)
        this.$message.success('退出项目成功')
        await this.fetchCollections()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('退出项目失败:', error)
          this.$message.error(error.response?.data?.message || '退出项目失败')
        }
      }
    },
    goToDetail(projectId) {
      this.$router.push(`/projectdetail?projectId=${projectId}`)
    },
    goToProjectList() {
      this.$router.push('/projectlist')
    },
    handlePageChange(page) {
      this.currentPage = page
    },
    formatDescription(desc) {
      if (!desc) return '暂无描述'
      return desc.length > 100 ? `${desc.substring(0, 100)}...` : desc
    },
    formatTime(timeStr) {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      return Number.isNaN(date.getTime()) ? '' : date.toLocaleDateString('zh-CN')
    },
    formatProjectType(type) {
      const map = {
        frontend: '前端项目',
        backend: '后端项目',
        fullstack: '全栈项目',
        mobile: '移动应用',
        ai: 'AI 项目',
        tools: '工具项目'
      }
      return map[type] || type || '未分类'
    },
    formatProjectStatus(status) {
      const map = {
        draft: '草稿',
        pending: '待审核',
        published: '已发布',
        rejected: '已拒绝',
        archived: '已归档'
      }
      return map[status] || status || '未知状态'
    },
    getProjectTypeTag(type) {
      const typeMap = {
        frontend: 'primary',
        backend: 'success',
        fullstack: 'warning',
        mobile: 'success',
        ai: 'danger',
        tools: 'info',
        '前端项目': 'primary',
        '后端项目': 'success',
        '全栈项目': 'warning',
        '移动应用': 'success',
        'AI 项目': 'danger',
        '工具项目': 'info'
      }
      return typeMap[type] || 'info'
    },
    getStatusTag(status) {
      const statusMap = {
        draft: 'info',
        pending: 'warning',
        published: 'success',
        rejected: 'danger',
        archived: 'info',
        '草稿': 'info',
        '待审核': 'warning',
        '已发布': 'success',
        '已拒绝': 'danger',
        '已归档': 'info'
      }
      return statusMap[status] || 'info'
    }
  }
}
</script>

<style scoped>
.project-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.project-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.page-subtitle {
  font-size: 16px;
  color: #909399;
  margin: 0;
}

.loading-container {
  min-height: 400px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.project-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
}

.project-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.card-content {
  padding: 16px;
}

.project-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  line-height: 1.4;
}

.project-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.author-avatar {
  background-color: #409EFF;
}

.author-name {
  font-size: 14px;
  color: #606266;
}

.tech-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.tech-tag {
  cursor: pointer;
  transition: all 0.2s ease;
}

.tech-tag:hover {
  transform: scale(1.05);
}

.more-tech {
  font-size: 12px;
  color: #909399;
  align-self: center;
}

.project-description {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 16px;
  min-height: 42px;
}

.project-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.collection-actions {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}

.remove-btn {
  font-size: 12px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 64px;
  color: #DCDFE6;
  margin-bottom: 20px;
}

.empty-title {
  font-size: 20px;
  color: #606266;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: #909399;
  margin-bottom: 20px;
}

.empty-action {
  margin-top: 10px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

@media (max-width: 768px) {
  .project-container {
    padding: 16px;
  }
  
  .project-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .page-title {
    font-size: 24px;
  }
}
</style>