<template>
  <div class="project-container">
    <div class="project-header">
      <h1 class="page-title">{{ pageTitle }}</h1>
      <p class="page-subtitle">{{ pageSubtitle }}</p>
    </div>

    <div class="tab-toolbar">
      <el-radio-group v-model="activeTab" size="small" @change="handleTabChange">
        <el-radio-button label="starred">
          <i class="el-icon-star-on"></i>
          我的收藏
        </el-radio-button>
        <el-radio-button label="participated">
          <i class="el-icon-user-solid"></i>
          我参与的项目
        </el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" element-loading-text="加载中..." class="loading-container">
      <div v-if="!loading && displayedProjects.length > 0" class="project-grid">
        <el-card
          v-for="project in displayedProjects"
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

            <div class="author-info" v-if="project.authorName || project.authorAvatar">
              <el-avatar :size="24" :src="project.authorAvatar || ''" class="author-avatar"></el-avatar>
              <span class="author-name">{{ project.authorName || '未知作者' }}</span>
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
              <span v-if="project.updatedAt || project.updateTime" class="stat-item">
                <i class="el-icon-time"></i>
                <span>{{ formatTime(project.updatedAt || project.updateTime) }}</span>
              </span>
            </div>

            <div class="collection-actions">
              <el-button
                v-if="activeTab === 'starred'"
                size="mini"
                type="warning"
                plain
                @click.stop="removeStar(project.id)"
                class="remove-btn"
              >
                <i class="el-icon-star-off"></i>
                取消收藏
              </el-button>

              <el-button
                v-else
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

      <div v-if="!loading && displayedProjects.length === 0" class="empty-state">
        <div class="empty-icon">
          <i :class="activeTab === 'starred' ? 'el-icon-star-on' : 'el-icon-user-solid'"></i>
        </div>
        <h3 class="empty-title">{{ emptyTitle }}</h3>
        <p class="empty-desc">{{ emptyDesc }}</p>
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
import { getMyStarredProjects, getParticipatedProjects, unstarProject, quitProject } from '@/api/project'

export default {
  layout: 'project',
  data() {
    return {
      activeTab: 'starred',
      starredProjects: [],
      participatedProjects: [],
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
        const newQuery = { ...this.$route.query, tab: this.activeTab }
        if (page > 1) newQuery.page = page
        else delete newQuery.page
        this.$router.push({ query: newQuery })
      }
    },
    displayedProjects() {
      return this.activeTab === 'starred' ? this.starredProjects : this.participatedProjects
    },
    pageTitle() {
      return this.activeTab === 'starred' ? '我的收藏' : '我参与的项目'
    },
    pageSubtitle() {
      return this.activeTab === 'starred'
        ? '查看您收藏的项目，并可随时取消收藏'
        : '查看您正在协作的项目，并可直接退出协作'
    },
    emptyTitle() {
      return this.activeTab === 'starred' ? '暂无收藏项目' : '暂无参与项目'
    },
    emptyDesc() {
      return this.activeTab === 'starred'
        ? '您当前还没有收藏任何项目，去项目列表发现感兴趣的内容吧。'
        : '您当前还没有参与任何项目，去项目列表看看吧。'
    }
  },
  watch: {
    '$route.query': {
      handler() {
        this.syncTabFromRoute()
        this.fetchCollections()
      },
      immediate: true
    }
  },
  methods: {
    syncTabFromRoute() {
      const tab = this.$route.query.tab
      this.activeTab = tab === 'participated' ? 'participated' : 'starred'
    },
    async fetchCollections() {
      this.loading = true
      try {
        const params = {
          page: this.currentPage,
          size: this.pageSize
        }
        const response = this.activeTab === 'starred'
          ? await getMyStarredProjects(params)
          : await getParticipatedProjects(params)

        const pageData = response.data || {}
        const list = Array.isArray(pageData.list) ? pageData.list : []

        if (this.activeTab === 'starred') {
          this.starredProjects = list
        } else {
          this.participatedProjects = list
        }

        this.total = pageData.total || 0
      } catch (error) {
        console.error(this.activeTab === 'starred' ? '获取收藏项目失败:' : '获取参与项目失败:', error)
        this.$message.error(error.response?.data?.message || (this.activeTab === 'starred' ? '获取收藏项目失败' : '获取参与项目失败'))
        if (this.activeTab === 'starred') {
          this.starredProjects = []
        } else {
          this.participatedProjects = []
        }
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    handleTabChange(tab) {
      const nextQuery = { tab }
      this.$router.push({ query: nextQuery })
    },
    async removeStar(projectId) {
      try {
        await this.$confirm('确定要取消收藏该项目吗？', '提示', { type: 'warning' })
        await unstarProject(projectId)
        this.$message.success('已取消收藏')
        await this.fetchCollections()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('取消收藏失败:', error)
          this.$message.error(error.response?.data?.message || '取消收藏失败')
        }
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
  margin-bottom: 24px;
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

.tab-toolbar {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
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
  margin: 0 0 12px 0;
  line-height: 1.4;
}

.project-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.project-description {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 16px 0;
  min-height: 44px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.author-name {
  font-size: 13px;
  color: #606266;
}

.project-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 16px;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.collection-actions {
  display: flex;
  justify-content: flex-end;
}

.remove-btn {
  width: 100%;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
}

.empty-icon {
  font-size: 64px;
  color: #dcdfe6;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 24px;
  color: #303133;
  margin: 0 0 8px 0;
}

.empty-desc {
  font-size: 14px;
  color: #909399;
  margin: 0 0 24px 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
}
</style>

<style scoped>
.project-container {
  background: var(--it-page-bg) !important;
  color: var(--it-text) !important;
}

.project-header,
.tab-toolbar,
.project-card,
.empty-state {
  background: var(--it-surface) !important;
  border: 1px solid var(--it-border) !important;
  border-radius: var(--it-radius-card) !important;
  box-shadow: var(--it-shadow) !important;
}

.page-title,
.project-title,
.empty-title {
  color: var(--it-text) !important;
}

.page-subtitle,
.project-description,
.author-name,
.project-stats,
.empty-desc {
  color: var(--it-text-muted) !important;
}

.project-stats,
.collection-actions {
  border-color: var(--it-border) !important;
}

.project-card:hover {
  border-color: var(--it-border-strong) !important;
  box-shadow: var(--it-shadow-strong) !important;
}

.tab-toolbar :deep(.el-radio-button__inner) {
  background: var(--it-surface-solid) !important;
  color: var(--it-text-muted) !important;
  border-color: var(--it-border) !important;
  border-radius: var(--it-radius-control) !important;
}

.tab-toolbar :deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
  background: var(--it-primary-gradient) !important;
  color: #ffffff !important;
  border-color: transparent !important;
}
</style>
