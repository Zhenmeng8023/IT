<template>
  <div class="project-container">
    <div class="project-header project-header-row">
      <div>
        <h1 class="page-title">项目列表</h1>
        <p class="page-subtitle">探索精彩的技术项目，发现创新的解决方案</p>
      </div>
      <div class="header-actions">
        <el-tag v-if="activeTechFilter" size="small" type="success" effect="plain">技术栈：{{ activeTechFilter }}</el-tag>
        <el-tag v-if="routeKeyword" size="small" type="primary" effect="plain">关键词：{{ routeKeyword }}</el-tag>
        <el-tag v-if="currentAuthor" size="small" type="warning" effect="plain">作者：{{ currentAuthor }}</el-tag>
        <el-button v-if="hasListQuery" size="small" icon="el-icon-refresh-left" @click="clearFilters">清空筛选</el-button>
      </div>
    </div>

    <div class="sort-toolbar">
      <div class="sort-wrapper">
        <span class="sort-label">排序：</span>
        <el-radio-group v-model="sortType" size="small" @change="handleSortChange" class="sort-group">
          <el-radio-button label="hot">
            <i class="el-icon-fire"></i> 热门
          </el-radio-button>
          <el-radio-button label="time_desc">
            <i class="el-icon-download"></i> 最新
          </el-radio-button>
          <el-radio-button label="time_asc">
            <i class="el-icon-upload"></i> 最早
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div v-loading="loading || permissionLoading" element-loading-text="加载中..." class="loading-container">
      <div v-if="!loading && projects.length > 0" class="project-grid">
        <el-card
          v-for="project in projects"
          :key="project.id"
          class="project-card"
          shadow="hover"
          @click.native="goToDetail(project.id)"
        >
          <div class="card-content">
            <div class="project-title-row">
              <h3 class="project-title">{{ project.name || project.title }}</h3>
              <el-tag
                v-if="projectUserRole(project)"
                size="mini"
                :type="getRoleTagType(projectUserRole(project))"
                effect="plain"
              >
                {{ formatRole(projectUserRole(project)) }}
              </el-tag>
            </div>

            <div class="project-meta">
              <el-tag :type="getProjectTypeTag(project.category || project.type)" size="small" class="type-tag">
                {{ formatProjectType(project.category || project.type) }}
              </el-tag>
              <el-tag :type="getStatusTag(project.status)" size="small" class="status-tag">
                {{ formatProjectStatus(project.status) }}
              </el-tag>
            </div>

            <p class="project-description">{{ formatDescription(project.description) }}</p>

            <div class="author-info">
              <el-avatar :size="24" :src="getAuthorAvatar(project)" class="author-avatar"></el-avatar>
              <span class="author-name">{{ getAuthorName(project) }}</span>
            </div>

            <div class="tech-stack" v-if="normalizeTags(project.tags).length > 0">
              <el-tag
                v-for="tech in normalizeTags(project.tags).slice(0, 3)"
                :key="tech"
                size="mini"
                class="tech-tag"
                @click.stop="filterByTech(tech)"
              >
                {{ tech }}
              </el-tag>
              <span v-if="normalizeTags(project.tags).length > 3" class="more-tech">+{{ normalizeTags(project.tags).length - 3 }}更多</span>
            </div>

            <div class="project-stats">
              <span class="stat-item"><i class="el-icon-star-off"></i>{{ project.stars || project.starCount || 0 }}</span>
              <span class="stat-item"><i class="el-icon-download"></i>{{ project.downloads || project.downloadCount || 0 }}</span>
              <span class="stat-item"><i class="el-icon-view"></i>{{ project.views || project.viewCount || 0 }}</span>
              <span class="stat-item" v-if="project.updatedAt || project.updateTime"><i class="el-icon-time"></i>{{ formatTime(project.updatedAt || project.updateTime) }}</span>
            </div>

            <div class="project-actions-row">
              <el-button size="mini" @click.stop="goToDetail(project.id)">查看详情</el-button>
              <el-button
                v-if="canEnterWorkbench(project)"
                type="primary"
                size="mini"
                @click.stop="goToWorkbench(project)"
              >
                {{ getWorkbenchButtonText(project) }}
              </el-button>
            </div>
          </div>
        </el-card>
      </div>

      <div v-if="!loading && projects.length === 0" class="empty-state">
        <div class="empty-icon"><i class="el-icon-folder-opened"></i></div>
        <h3 class="empty-title">暂无项目</h3>
        <p class="empty-desc">当前筛选条件下没有找到项目，试试切换排序或标签。</p>
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
    <ProjectInvitationSidebarNotice />
  </div>
</template>

<script>
import { pageProjects, getMyProjects, getParticipatedProjects } from '@/api/project'
import ProjectInvitationSidebarNotice from '../components/ProjectInvitationSidebarNotice.vue'
import { getCurrentUser, getToken } from '@/utils/auth'

function parseTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags.filter(Boolean)
  if (typeof tags === 'string') {
    try {
      const parsed = JSON.parse(tags)
      if (Array.isArray(parsed)) return parsed.filter(Boolean)
    } catch (e) {}
    return tags.split(',').map(item => item.trim()).filter(Boolean)
  }
  return []
}

function readStoredToken() {
  return getToken() || ''
}

function readCurrentUser() {
  return getCurrentUser()
}

function normalizeRole(role) {
  const value = String(role || '').trim().toLowerCase()
  if (['owner', 'admin', 'member', 'viewer'].includes(value)) return value
  return ''
}

function roleLevel(role) {
  return { viewer: 1, member: 2, admin: 3, owner: 4 }[normalizeRole(role)] || 0
}

function mergeRole(map, projectId, role) {
  const id = Number(projectId)
  const normalizedRole = normalizeRole(role)
  if (!id || !normalizedRole) return
  const currentRole = map[id]
  if (!currentRole || roleLevel(normalizedRole) > roleLevel(currentRole)) {
    map[id] = normalizedRole
  }
}

export default {
  layout: 'project',
  components: {
    ProjectInvitationSidebarNotice
  },
  data() {
    return {
      projects: [],
      total: 0,
      pageSize: 8,
      loading: false,
      permissionLoading: false,
      sortType: 'time_desc',
      authorMap: {},
      userProjectRoleMap: {}
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
    },
    routeKeyword() {
      return String(this.$route.query.keyword || '').trim()
    },
    currentAuthor() {
      return String(this.$route.query.author || '').trim()
    },
    activeTechFilter() {
      return String(this.$route.query.tech || '').trim()
    },
    isLoggedIn() {
      return !!readStoredToken() || !!readCurrentUser()
    },
    hasListQuery() {
      return !!(this.routeKeyword || this.currentAuthor || this.activeTechFilter)
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
    normalizeTags: parseTags,
    mapSortType(sortType) {
      const map = { hot: 'hot', time_desc: 'latest', time_asc: 'earliest' }
      return map[sortType] || 'latest'
    },
    async fetchProjects() {
      this.loading = true
      try {
        const response = await pageProjects({
          page: this.currentPage,
          size: this.pageSize,
          sortBy: this.mapSortType(this.sortType),
          tag: this.activeTechFilter || undefined,
          keyword: this.routeKeyword || undefined,
          author: this.currentAuthor || undefined
        })
        this.projects = (response.data?.list || []).map(item => ({
          ...item,
          tags: parseTags(item.tags)
        }))
        this.total = response.data?.total || 0
        await this.hydratePermissionMap()
      } catch (error) {
        console.error('获取项目列表失败:', error)
        this.projects = []
        this.total = 0
        this.userProjectRoleMap = {}
        this.$message.error(error.response?.data?.message || '获取项目列表失败')
      } finally {
        this.loading = false
      }
    },
    async hydratePermissionMap() {
      if (!this.isLoggedIn) {
        this.userProjectRoleMap = {}
        return
      }
      this.permissionLoading = true
      try {
        const map = {}
        const [mineRes, joinedRes] = await Promise.allSettled([
          getMyProjects({ page: 1, size: 200 }),
          getParticipatedProjects({ page: 1, size: 200 })
        ])

        if (mineRes.status === 'fulfilled') {
          const list = Array.isArray(mineRes.value?.data?.list) ? mineRes.value.data.list : []
          list.forEach(item => mergeRole(map, item.id || item.projectId, 'owner'))
        }

        if (joinedRes.status === 'fulfilled') {
          const list = Array.isArray(joinedRes.value?.data?.list) ? joinedRes.value.data.list : []
          list.forEach(item => {
            const role =
              item.currentUserRole ||
              item.role ||
              item.memberRole ||
              item.projectRole ||
              item.currentRole ||
              'member'
            mergeRole(map, item.id || item.projectId, role)
          })
        }

        this.userProjectRoleMap = map
      } catch (error) {
        console.error('加载项目权限信息失败:', error)
        this.userProjectRoleMap = {}
      } finally {
        this.permissionLoading = false
      }
    },
    projectUserRole(project) {
      return this.userProjectRoleMap[Number(project.id)] || ''
    },
    canManageProject(project) {
      return ['owner', 'admin'].includes(this.projectUserRole(project))
    },
    canEnterWorkbench(project) {
      return !!this.projectUserRole(project)
    },
    getWorkbenchButtonText(project) {
      return this.canManageProject(project) ? '项目管理' : '进入项目'
    },
    getRoleTagType(role) {
      return {
        owner: 'danger',
        admin: 'warning',
        member: 'success',
        viewer: 'info'
      }[normalizeRole(role)] || 'info'
    },
    formatRole(role) {
      return {
        owner: '所有者',
        admin: '管理员',
        member: '成员',
        viewer: '查看者'
      }[normalizeRole(role)] || ''
    },
    handleSortChange() {
      this.currentPage = 1
      this.fetchProjects()
    },
    handlePageChange(page) {
      this.currentPage = page
    },
    goToDetail(projectId) {
      this.$router.push(`/projectdetail?projectId=${projectId}`)
    },
    goToWorkbench(project) {
      if (!this.canEnterWorkbench(project)) {
        this.$message.warning('只有项目成员才能进入项目工作台')
        return
      }
      this.$router.push(`/projectmanage?projectId=${project.id}`)
    },
    getAuthorAvatar(project) {
      const author = this.authorMap[project.authorId] || {}
      return project.authorAvatar || author.avatar || ''
    },
    getAuthorName(project) {
      const author = this.authorMap[project.authorId] || {}
      return project.authorName || author.nickname || author.username || '未知作者'
    },
    filterByTech(tech) {
      const query = { ...this.$route.query, tech, page: 1 }
      delete query.keyword
      delete query.author
      this.$router.push({ path: '/projectlist', query })
    },
    clearFilters() {
      this.$router.push({ path: '/projectlist', query: {} })
    },
    formatDescription(desc) {
      if (!desc) return '暂无描述'
      return desc.length > 100 ? `${desc.slice(0, 100)}...` : desc
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
/* ========== 容器样式 ========== */
.project-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  min-height: 100vh;
}

/* ========== 页面头部 ========== */
.project-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 10px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.page-subtitle {
  font-size: 16px;
  color: #64748b;
  margin: 0;
}

/* ========== 排序工具栏 ========== */
.sort-toolbar {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
}

.sort-wrapper {
  display: flex;
  align-items: center;
  gap: 15px;
  background: white;
  padding: 12px 20px;
  border-radius: 25px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.03);
}

.sort-label {
  font-size: 14px;
  color: #475569;
  font-weight: 500;
}

.sort-group :deep(.el-radio-button__inner) {
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  transition: all 0.3s ease;
}

.sort-group :deep(.el-radio-button__inner i) {
  margin-right: 4px;
  font-size: 12px;
}

.sort-group :deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.2);
}

.sort-group :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 20px 0 0 20px;
}

.sort-group :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 0 20px 20px 0;
}

/* ========== 项目网格 ========== */
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 24px;
  margin-bottom: 30px;
}

.project-card {
  border: 1px solid rgba(0, 0, 0, 0.03);
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
  background: white;
}

.project-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.05);
  border-color: rgba(59, 130, 246, 0.2);
}

.card-content {
  padding: 24px;
}

/* 项目标题 */
.project-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 12px;
  line-height: 1.4;
  transition: color 0.2s ease;
}

.project-card:hover .project-title {
  color: #3b82f6;
}

/* 项目元信息 */
.project-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.type-tag, .status-tag {
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

/* 作者信息 */
.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.author-avatar {
  transition: border-color 0.3s ease;
}

.project-card:hover .author-avatar {
  border-color: #3b82f6;
}

.author-name {
  font-size: 13px;
  color: #475569;
  font-weight: 500;
}

/* 技术栈 */
.tech-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
  align-items: center;
}

.tech-tag {
  background: #f1f5f9;
  border: none;
  color: #475569;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tech-tag:hover {
  background: #3b82f6;
  color: white;
  transform: translateY(-1px);
}

.more-tech {
  font-size: 11px;
  color: #94a3b8;
  margin-left: 4px;
}

/* 项目描述 */
.project-description {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  margin: 0 0 15px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 统计信息 */
.project-stats {
  display: flex;
  gap: 15px;
  padding-top: 15px;
  border-top: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.stat-item i {
  font-size: 14px;
  color: #94a3b8;
  transition: color 0.2s ease;
}

.stat-item:hover i {
  color: #3b82f6;
}

/* ========== 空状态 ========== */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 24px;
  border: 1px solid rgba(0, 0, 0, 0.03);
  max-width: 400px;
  margin: 50px auto;
}

.empty-icon {
  color: #cbd5e1;
  margin-bottom: 16px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

/* ========== 分页 ========== */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding: 20px 0;
}

.pagination-wrapper :deep(.el-pagination) {
  font-weight: 400;
}

.pagination-wrapper :deep(.el-pagination.is-background .el-pager li:not(.disabled).active) {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.2);
}

.pagination-wrapper :deep(.el-pagination.is-background .el-pager li) {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.pagination-wrapper :deep(.el-pagination.is-background .el-pager li:hover) {
  color: #3b82f6;
  background: #f1f5f9;
}

/* ========== 加载状态 ========== */
.loading-container {
  min-height: 400px;
}

/* ========== 响应式 ========== */
@media screen and (max-width: 768px) {
  .project-container {
    padding: 20px 15px;
  }

  .page-title {
    font-size: 28px;
  }

  .project-grid {
    grid-template-columns: 1fr;
  }

  .sort-wrapper {
    flex-direction: column;
    align-items: flex-start;
  }

  .sort-group {
    width: 100%;
  }

  .sort-group :deep(.el-radio-group) {
    display: flex;
    width: 100%;
  }

  .sort-group :deep(.el-radio-button) {
    flex: 1;
  }

  .sort-group :deep(.el-radio-button__inner) {
    width: 100%;
    text-align: center;
  }
}

@media screen and (max-width: 480px) {
  .project-stats {
    flex-wrap: wrap;
    gap: 10px;
  }

  .project-title {
    font-size: 16px;
  }

  .card-content {
    padding: 16px;
  }
}

.project-header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.project-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.project-actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #eef2f7;
}

</style>

<style scoped>
.project-container {
  background: var(--it-page-bg) !important;
  color: var(--it-text) !important;
}

.project-header,
.sort-toolbar,
.project-card,
.empty-state {
  background: var(--it-surface) !important;
  border: 1px solid var(--it-border) !important;
  border-radius: var(--it-radius-card) !important;
  box-shadow: var(--it-shadow) !important;
}

.page-title,
.project-title,
.empty-title,
.sort-label {
  color: var(--it-text) !important;
}

.page-title {
  background: var(--it-primary-gradient) !important;
  -webkit-background-clip: text !important;
  -webkit-text-fill-color: transparent !important;
}

.page-subtitle,
.sort-group :deep(.el-radio-button__inner),
.project-description,
.author-name,
.project-stats,
.empty-desc,
.more-tech {
  color: var(--it-text-muted) !important;
}

.tech-tag,
.type-tag,
.status-tag,
.meta-chip {
  background: var(--it-accent-soft) !important;
  border-color: var(--it-border) !important;
  color: var(--it-accent) !important;
  border-radius: var(--it-radius-control) !important;
}

.project-stats,
.project-actions-row {
  border-color: var(--it-border) !important;
}

.project-card:hover {
  border-color: var(--it-border-strong) !important;
  box-shadow: var(--it-shadow-strong) !important;
}

.sort-group :deep(.el-radio-button__inner),
.pagination-wrapper :deep(.el-pagination.is-background .el-pager li),
.pagination-wrapper :deep(.el-pagination.is-background .btn-prev),
.pagination-wrapper :deep(.el-pagination.is-background .btn-next) {
  background: var(--it-surface-solid) !important;
  border-color: var(--it-border) !important;
  border-radius: var(--it-radius-control) !important;
}

.sort-group :deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner),
.pagination-wrapper :deep(.el-pagination.is-background .el-pager li:not(.disabled).active) {
  background: var(--it-primary-gradient) !important;
  color: #ffffff !important;
  border-color: transparent !important;
}
</style>
