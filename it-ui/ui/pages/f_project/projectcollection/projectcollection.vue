<template>
  <div class="project-collection-container">
    <div class="project-header">
      <h1 class="page-title">{{ pageTitle }}</h1>
      <p class="page-subtitle">{{ pageSubtitle }}</p>
    </div>

    <div class="tab-toolbar">
      <el-radio-group v-model="activeTab" size="small" class="collection-tab-group" @change="handleTabChange">
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
                size="small"
                type="warning"
                plain
                @click.stop="removeStar(project.id)"
                class="remove-btn collection-action-btn"
              >
                <i class="el-icon-star-off"></i>
                取消收藏
              </el-button>

              <el-button
                v-else
                size="small"
                type="danger"
                plain
                @click.stop="quitParticipatedProject(project.id)"
                class="remove-btn collection-action-btn"
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
        <el-button size="small" type="primary" class="go-project-btn" @click="goToProjectList">去项目列表</el-button>
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

function normalizeProjectStatus(status) {
  const value = String(status || '').trim().toLowerCase()
  if (value === 'draft' || value === '草稿') return 'draft'
  return 'published'
}

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
        const normalizedList = list.map(item => ({
          ...item,
          status: normalizeProjectStatus(item.status)
        }))

        if (this.activeTab === 'starred') {
          this.starredProjects = normalizedList
        } else {
          this.participatedProjects = normalizedList
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
      return normalizeProjectStatus(status) === 'draft' ? '草稿' : '已发布'
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
      return normalizeProjectStatus(status) === 'draft' ? 'info' : 'success'
    }
  }
}
</script>

<style scoped>
.project-collection-container {
  --pc-card-radius: 8px;
  --pc-control-radius: 8px;
  --pc-border: var(--it-border, #dbe2ea);
  --pc-border-strong: var(--it-border-strong, #b6c3d1);
  --pc-surface: var(--it-surface, #ffffff);
  --pc-surface-solid: var(--it-surface-solid, #f8fafc);
  --pc-page-bg: var(--it-page-bg, #f5f8fc);
  --pc-text: var(--it-text, #0f172a);
  --pc-muted: var(--it-text-muted, #64748b);
  --pc-accent: var(--it-accent, #2563eb);
  --pc-accent-soft: var(--it-accent-soft, #e8f1ff);
  --pc-shadow: var(--it-shadow, 0 6px 18px rgba(15, 23, 42, 0.06));
  --pc-shadow-strong: var(--it-shadow-strong, 0 10px 26px rgba(15, 23, 42, 0.12));

  width: 100% !important;
  max-width: none !important;
  margin: 0 !important;
  padding: 16px 12px 36px !important;
  background: var(--pc-page-bg);
  color: var(--pc-text);
}

.project-header,
.tab-toolbar,
.project-card,
.empty-state {
  background: var(--pc-surface);
  border: 1px solid var(--pc-border);
  border-radius: var(--pc-card-radius);
  box-shadow: var(--pc-shadow);
}

.project-header {
  text-align: left;
  margin-bottom: 12px;
  padding: 14px 16px;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--pc-text);
  margin: 0 0 6px;
  line-height: 1.2;
}

.page-subtitle {
  font-size: 13px;
  line-height: 1.72;
  color: var(--pc-muted);
  margin: 0;
  max-width: 760px;
}

.tab-toolbar {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 12px;
  padding: 10px 12px;
}

.collection-tab-group {
  display: flex;
  flex-wrap: wrap;
}

.tab-toolbar :deep(.el-radio-button__inner) {
  height: 34px;
  line-height: 32px;
  min-width: 128px;
  padding: 0 14px;
  border-radius: var(--pc-control-radius);
  border-color: var(--pc-border);
  background: var(--pc-surface-solid);
  color: var(--pc-muted);
  font-size: 13px;
  font-weight: 600;
}

.tab-toolbar :deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
  color: #ffffff;
  border-color: transparent;
  background: var(--it-primary-gradient, linear-gradient(135deg, #2563eb, #3b82f6));
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.24);
}

.loading-container {
  min-height: 320px;
  width: 100% !important;
}

.project-grid {
  display: grid;
  width: 100% !important;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.project-card {
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
  height: 100%;
}

.project-card:hover {
  transform: translateY(-1px);
  border-color: var(--pc-border-strong);
  box-shadow: var(--pc-shadow-strong);
}

.project-card :deep(.el-card__body) {
  padding: 0;
  height: 100%;
}

.card-content {
  padding: 16px;
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.project-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--pc-text);
  margin: 0 0 10px;
  line-height: 1.4;
}

.project-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.type-tag,
.status-tag {
  border-radius: var(--pc-control-radius);
}

.project-description {
  font-size: 13px;
  color: var(--pc-muted);
  line-height: 1.66;
  margin: 0 0 12px;
  min-height: 44px;
  flex-grow: 1;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.author-name {
  font-size: 13px;
  color: var(--pc-muted);
}

.project-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: var(--pc-muted);
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid var(--pc-border);
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.collection-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.remove-btn {
  width: 100%;
}

.collection-actions :deep(.collection-action-btn.el-button) {
  height: 32px;
  border-radius: var(--pc-control-radius);
  font-size: 12px;
  font-weight: 600;
  padding: 0 12px;
}

.empty-state {
  text-align: center;
  padding: 44px 20px;
}

.empty-icon {
  font-size: 48px;
  color: var(--pc-muted);
  margin-bottom: 12px;
}

.empty-title {
  font-size: 22px;
  color: var(--pc-text);
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 13px;
  color: var(--pc-muted);
  margin: 0 0 16px;
  line-height: 1.7;
}

.go-project-btn {
  height: 34px;
  padding: 0 14px;
  border-radius: var(--pc-control-radius);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 4px;
}

.project-collection-container :deep(.el-pagination.is-background .btn-prev),
.project-collection-container :deep(.el-pagination.is-background .btn-next),
.project-collection-container :deep(.el-pagination.is-background .el-pager li) {
  background: var(--pc-surface);
  border-radius: var(--pc-control-radius);
  border: 1px solid var(--pc-border);
}

.project-collection-container :deep(.el-pagination.is-background .el-pager li:not(.disabled).active) {
  color: #ffffff;
  border-color: transparent;
  background: var(--it-primary-gradient, linear-gradient(135deg, #2563eb, #3b82f6));
}

@media (max-width: 900px) {
  .project-collection-container {
    padding: 10px 10px 28px !important;
  }

  .project-grid {
    grid-template-columns: 1fr;
  }

  .tab-toolbar {
    padding: 10px;
  }

  .collection-tab-group {
    width: 100%;
  }

  .tab-toolbar :deep(.el-radio-button) {
    flex: 1;
  }

  .tab-toolbar :deep(.el-radio-button__inner) {
    width: 100%;
    min-width: 0;
  }
}
</style>
