<template>
  <div data-testid="project-governance-page" class="project-governance-page">
    <front-page-shell
      class="project-governance-shell"
      layout="three"
      :max-width="'100%'"
      :left-width="240"
      :right-width="300"
      :gap="12"
      :padding-top="0"
      :padding-bottom="0"
      :show-background="false"
      content-class="project-governance-main"
    >
      <template #top>
        <front-hero-panel
          class="project-governance-hero"
          compact
          badge="Project Discovery"
          title="项目治理推荐页"
          :subtitle="heroSubtitle"
          :stats="heroStats"
          :stats-columns="4"
        >
          <template #actions>
            <div class="hero-actions">
              <el-tag v-if="activeTechFilter" size="small" type="success" effect="plain">技术栈：{{ activeTechFilter }}</el-tag>
              <el-tag v-if="routeKeyword" size="small" type="primary" effect="plain">关键词：{{ routeKeyword }}</el-tag>
              <el-tag v-if="currentAuthor" size="small" type="warning" effect="plain">作者：{{ currentAuthor }}</el-tag>
              <el-tag v-if="activeJoinableFilter !== 'all'" size="small" type="info" effect="plain">
                可加入：{{ activeJoinableFilter === 'yes' ? '是' : '否' }}
              </el-tag>
              <el-tag v-if="activeUpdatedFilter !== 'all'" size="small" type="info" effect="plain">
                更新时间：{{ updatedFilterLabel(activeUpdatedFilter) }}
              </el-tag>
              <el-button v-if="hasListQuery" size="small" icon="el-icon-refresh-left" @click="clearFilters">清空筛选</el-button>
            </div>
          </template>
        </front-hero-panel>
      </template>

      <template #left>
        <aside class="project-filter-rail">
          <section class="left-panel">
            <h3>技术栈筛选</h3>
            <front-tag-cloud
              title=""
              compact
              :tags="techCloudItems"
              :active-values="[activeTechTabValue]"
              :multiple="false"
              :show-count="true"
              empty-text="暂无技术栈"
              @tag-click="handleTechCloudSelect"
            />
          </section>

          <section class="left-panel">
            <h3>是否可加入</h3>
            <el-radio-group
              :value="activeJoinableFilter"
              size="small"
              class="filter-radio-group"
              @change="setJoinableFilter"
            >
              <el-radio-button label="all">全部</el-radio-button>
              <el-radio-button label="yes">可加入</el-radio-button>
              <el-radio-button label="no">不可加入</el-radio-button>
            </el-radio-group>
          </section>

          <section class="left-panel">
            <h3>更新时间</h3>
            <el-radio-group
              :value="activeUpdatedFilter"
              size="small"
              class="filter-radio-group"
              @change="setUpdatedFilter"
            >
              <el-radio-button v-for="item in updatedFilters" :key="item.value" :label="item.value">
                {{ item.label }}
              </el-radio-button>
            </el-radio-group>
          </section>
        </aside>
      </template>

      <div class="project-main-content">
        <front-feed-toolbar
          class="project-toolbar"
          :tabs="techTabs"
          :active-tab="activeTechTabValue"
          :sort-options="sortOptions"
          :sort-value="sortType"
          sort-label="排序"
          :filters="activeFilters"
          filter-label="筛选中"
          clear-text="清空筛选"
          @tab-change="handleTechTabChange"
          @sort-change="handleSortChange"
          @clear-filters="clearFilters"
          @remove-filter="handleFilterRemove"
        >
          <template #leading>
            <span class="toolbar-summary">当前页展示 {{ governanceProjects.length }} 项（总计 {{ total || governanceProjects.length }} 项）</span>
          </template>
          <template #extra>
            <span class="toolbar-current-filter">{{ currentFilterText }}</span>
          </template>
        </front-feed-toolbar>

        <div
          v-loading="loading || permissionLoading"
          element-loading-text="正在加载项目..."
          class="project-list-zone"
        >
          <div v-if="!loading && governanceProjects.length > 0" data-testid="project-governance-list" class="governance-list">
            <el-card
              v-for="project in governanceProjects"
              :key="project.id"
              class="governance-card"
              shadow="hover"
              @click.native="goToDetail(project.id)"
            >
              <div class="governance-card__header">
                <div class="governance-card__headline">
                  <h3 class="governance-card__title">{{ project.name || project.title || '未命名项目' }}</h3>
                  <div class="governance-card__badges">
                    <el-tag
                      v-if="project._role"
                      size="mini"
                      :type="getRoleTagType(project._role)"
                      effect="plain"
                    >
                      {{ formatRole(project._role) }}
                    </el-tag>
                    <el-tag size="mini" :type="project._joinable ? 'success' : 'info'" effect="plain">
                      {{ project._joinableText }}
                    </el-tag>
                  </div>
                </div>

                <p class="governance-card__description">{{ formatDescription(project.description) }}</p>
              </div>

              <div class="governance-card__meta-grid">
                <div class="meta-item">
                  <span class="meta-item__label">技术栈</span>
                  <div class="meta-item__value tag-list">
                    <el-tag
                      v-for="tech in project._tags.slice(0, 6)"
                      :key="`${project.id}-tech-${tech}`"
                      size="mini"
                      class="tech-tag"
                      @click.stop="filterByTech(tech)"
                    >
                      {{ tech }}
                    </el-tag>
                    <span v-if="project._tags.length === 0" class="meta-item__placeholder">未标注</span>
                  </div>
                </div>

                <div class="meta-item">
                  <span class="meta-item__label">项目类型</span>
                  <div class="meta-item__value">
                    <el-tag size="mini" :type="getProjectTypeTag(project.category || project.type)" effect="plain">
                      {{ formatProjectType(project.category || project.type) }}
                    </el-tag>
                  </div>
                </div>

                <div class="meta-item">
                  <span class="meta-item__label">作者</span>
                  <div class="meta-item__value author-cell">
                    <el-avatar :size="24" :src="getAuthorAvatar(project)"></el-avatar>
                    <span>{{ getAuthorName(project) }}</span>
                  </div>
                </div>

                <div class="meta-item">
                  <span class="meta-item__label">最近更新</span>
                  <div class="meta-item__value">{{ formatDateTime(project._updatedRaw) || '-' }}</div>
                </div>
              </div>

              <div class="governance-card__signals">
                <span class="signal-item"><i class="el-icon-star-off"></i>{{ project._starCount }}</span>
                <span class="signal-item"><i class="el-icon-download"></i>{{ project._downloadCount }}</span>
                <span class="signal-item"><i class="el-icon-view"></i>{{ project._viewCount }}</span>
              </div>

              <div class="governance-card__reasons">
                <span class="reason-label">推荐理由</span>
                <el-tag
                  v-for="reason in project._reasons"
                  :key="`${project.id}-reason-${reason}`"
                  size="mini"
                  class="reason-tag"
                  effect="plain"
                >
                  {{ reason }}
                </el-tag>
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
            </el-card>
          </div>

          <front-empty-state
            v-if="!loading && governanceProjects.length === 0"
            data-testid="project-governance-empty"
            icon="el-icon-folder-opened"
            title="当前筛选下暂无项目"
            description="可以切换技术栈、加入条件或更新时间范围，查看更多治理推荐项目。"
            size="lg"
          />
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

      <template #right>
        <front-right-rail
          title="治理榜"
          subtitle="活跃项目、新发布项目与高收藏项目"
          data-testid="project-governance-right-rail"
        >
          <section class="front-right-rail__section rank-section">
            <h4 class="rank-title">活跃项目</h4>
            <ul v-if="activeRankList.length" class="rank-list">
              <li v-for="(item, index) in activeRankList" :key="`active-${item.id}`">
                <button type="button" class="rank-item" @click="goToDetail(item.id)">
                  <span class="rank-item__index">{{ index + 1 }}</span>
                  <span class="rank-item__copy">
                    <strong>{{ item.name || item.title || '未命名项目' }}</strong>
                    <em>最近更新 {{ formatDate(item._updatedRaw) || '-' }}</em>
                  </span>
                </button>
              </li>
            </ul>
            <p v-else class="rail-empty-tip">暂无活跃项目信号</p>
          </section>

          <section class="front-right-rail__section rank-section">
            <h4 class="rank-title">新发布项目</h4>
            <p v-if="releaseSignalLoading" class="rail-loading-tip">正在同步发布信号...</p>
            <ul v-if="newReleaseRankList.length" class="rank-list">
              <li v-for="(item, index) in newReleaseRankList" :key="`release-${item.id}`">
                <button type="button" class="rank-item" @click="goToDetail(item.id)">
                  <span class="rank-item__index">{{ index + 1 }}</span>
                  <span class="rank-item__copy">
                    <strong>{{ item.name || item.title || '未命名项目' }}</strong>
                    <em>{{ formatDate(item._releaseRaw) || formatDate(item.createdAt) || '-' }}</em>
                  </span>
                </button>
              </li>
            </ul>
            <p v-else class="rail-empty-tip">暂无发布记录</p>
          </section>

          <section class="front-right-rail__section rank-section">
            <h4 class="rank-title">高收藏项目</h4>
            <ul v-if="starRankList.length" class="rank-list">
              <li v-for="(item, index) in starRankList" :key="`star-${item.id}`">
                <button type="button" class="rank-item" @click="goToDetail(item.id)">
                  <span class="rank-item__index">{{ index + 1 }}</span>
                  <span class="rank-item__copy">
                    <strong>{{ item.name || item.title || '未命名项目' }}</strong>
                    <em>{{ item._starCount }} 收藏 · {{ item._downloadCount }} 下载</em>
                  </span>
                </button>
              </li>
            </ul>
            <p v-else class="rail-empty-tip">暂无收藏数据</p>
          </section>
        </front-right-rail>
      </template>
    </front-page-shell>

    <ProjectInvitationSidebarNotice />
  </div>
</template>

<script>
import { pageProjects, getMyProjects, getParticipatedProjects } from '@/api/project'
import { getLatestProjectRelease } from '@/api/projectRelease'
import ProjectInvitationSidebarNotice from '../components/ProjectInvitationSidebarNotice.vue'
import {
  FrontEmptyState,
  FrontFeedToolbar,
  FrontHeroPanel,
  FrontPageShell,
  FrontRightRail,
  FrontTagCloud
} from '@/components/front'
import { getCurrentUser, getToken } from '@/utils/auth'
import { pickAvatarUrl } from '@/utils/avatar'

const DAY_MS = 24 * 60 * 60 * 1000
const UPDATED_FILTER_OPTIONS = [
  { value: 'all', label: '全部时间' },
  { value: '7d', label: '近 7 天' },
  { value: '30d', label: '近 30 天' },
  { value: '90d', label: '近 90 天' }
]

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

function toTimestamp(value) {
  if (!value) return 0
  const date = new Date(value)
  const time = date.getTime()
  return Number.isNaN(time) ? 0 : time
}

function readNumber(...values) {
  for (let i = 0; i < values.length; i += 1) {
    const n = Number(values[i])
    if (!Number.isNaN(n) && Number.isFinite(n)) return n
  }
  return 0
}

function withTimeout(promise, timeoutMs = 1200) {
  return Promise.race([
    promise,
    new Promise((_, reject) => setTimeout(() => reject(new Error('timeout')), timeoutMs))
  ])
}

async function runWithConcurrency(items, limit, worker) {
  if (!Array.isArray(items) || !items.length) return []
  const max = Math.max(1, Math.min(Number(limit) || 1, items.length))
  const results = new Array(items.length)
  let cursor = 0

  const runners = Array.from({ length: max }).map(async () => {
    while (cursor < items.length) {
      const current = cursor
      cursor += 1
      try {
        results[current] = await worker(items[current], current)
      } catch (error) {
        results[current] = null
      }
    }
  })
  await Promise.all(runners)
  return results
}

export default {
  layout: 'project',
  components: {
    FrontEmptyState,
    FrontFeedToolbar,
    FrontHeroPanel,
    FrontPageShell,
    FrontRightRail,
    FrontTagCloud,
    ProjectInvitationSidebarNotice
  },
  data() {
    return {
      clientHydrated: false,
      projects: [],
      total: 0,
      pageSize: 12,
      loading: false,
      permissionLoading: false,
      releaseSignalLoading: false,
      sortType: 'time_desc',
      authorMap: {},
      userProjectRoleMap: {},
      releaseSignalMap: {},
      releaseSignalRequestToken: 0
    }
  },
  computed: {
    currentPage: {
      get() {
        return parseInt(this.$route.query.page, 10) || 1
      },
      set(page) {
        const nextQuery = { ...this.$route.query }
        if (page > 1) nextQuery.page = page
        else delete nextQuery.page
        this.$router.push({ path: '/projectlist', query: nextQuery })
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
    activeJoinableFilter() {
      const value = String(this.$route.query.joinable || 'all').trim()
      return ['all', 'yes', 'no'].includes(value) ? value : 'all'
    },
    activeUpdatedFilter() {
      const value = String(this.$route.query.updated || 'all').trim()
      return ['all', '7d', '30d', '90d'].includes(value) ? value : 'all'
    },
    activeTechTabValue() {
      return this.activeTechFilter || 'all'
    },
    isLoggedIn() {
      return !!readStoredToken() || !!readCurrentUser()
    },
    hasListQuery() {
      return !!(
        this.routeKeyword ||
        this.currentAuthor ||
        this.activeTechFilter ||
        this.activeJoinableFilter !== 'all' ||
        this.activeUpdatedFilter !== 'all'
      )
    },
    updatedFilters() {
      return UPDATED_FILTER_OPTIONS
    },
    sortOptions() {
      return [
        { id: 'hot', label: '治理推荐', value: 'hot', icon: 'el-icon-fire' },
        { id: 'latest', label: '最新更新', value: 'time_desc', icon: 'el-icon-time' },
        { id: 'earliest', label: '最早更新', value: 'time_asc', icon: 'el-icon-date' }
      ]
    },
    heroSubtitle() {
      return '基于技术栈、活跃度、热度与可参与性，为你推荐更值得投入的项目。'
    },
    projectCards() {
      const now = Date.now()
      return (this.projects || []).map((project, index) => {
        const tags = parseTags(project.tags)
        const role = this.projectUserRole(project)
        const updatedRaw =
          project.updatedAt ||
          project.updateTime ||
          project.modifiedAt ||
          project.gmtModified ||
          project.lastActiveAt ||
          project.createdAt
        const updatedTs = toTimestamp(updatedRaw)
        const starCount = readNumber(project.stars, project.starCount, project.favoriteCount, project.collectCount, 0)
        const downloadCount = readNumber(project.downloads, project.downloadCount, project.downloadTimes, 0)
        const viewCount = readNumber(project.views, project.viewCount, project.readCount, 0)
        const releaseSignal = this.releaseSignalMap[Number(project.id)] || null
        const releaseRaw =
          (releaseSignal &&
            (releaseSignal.publishedAt ||
              releaseSignal.publishTime ||
              releaseSignal.createdAt ||
              releaseSignal.releaseTime ||
              releaseSignal.latestAt)) ||
          project.latestReleaseAt ||
          project.releaseTime ||
          project.publishedAt
        const releaseTs = toTimestamp(releaseRaw)
        const joinable = this.resolveJoinable(project, role)
        const joinableText = role ? '已加入' : (joinable ? '可加入' : '不可加入')

        const diffDays = updatedTs ? Math.floor((now - updatedTs) / DAY_MS) : 999
        const recencyScore = Math.max(0, 30 - diffDays)
        const popularityScore = starCount * 6 + downloadCount * 4 + viewCount * 0.18
        const completeSignal = this.hasCompleteSignal(project, tags)
        const techMatched = this.techMatched(tags, this.activeTechFilter)
        const recommendationScore =
          recencyScore * 2.4 +
          popularityScore +
          (techMatched ? 18 : 0) +
          (role ? 12 : 0) +
          (completeSignal ? 8 : 0)

        const reasons = this.buildRecommendationReasons({
          role,
          popularityScore,
          updatedTs,
          completeSignal,
          techMatched
        })

        return {
          ...project,
          _index: index,
          _tags: tags,
          _role: role,
          _updatedRaw: updatedRaw,
          _updatedTs: updatedTs,
          _releaseRaw: releaseRaw,
          _releaseTs: releaseTs,
          _joinable: joinable,
          _joinableText: joinableText,
          _starCount: Math.max(0, starCount),
          _downloadCount: Math.max(0, downloadCount),
          _viewCount: Math.max(0, viewCount),
          _activeScore: recencyScore * 5 + viewCount * 0.2 + downloadCount * 2,
          _recommendationScore: recommendationScore,
          _reasons: reasons
        }
      })
    },
    governanceProjects() {
      const days = this.resolveUpdatedDays(this.activeUpdatedFilter)
      const now = Date.now()
      const filtered = this.projectCards.filter(item => {
        if (this.activeTechFilter && !this.techMatched(item._tags, this.activeTechFilter)) return false
        if (this.activeJoinableFilter === 'yes' && !item._joinable && !item._role) return false
        if (this.activeJoinableFilter === 'no' && (item._joinable || item._role)) return false
        if (days > 0) {
          if (!item._updatedTs) return false
          if (now - item._updatedTs > days * DAY_MS) return false
        }
        return true
      })

      return filtered.sort((left, right) => {
        if (this.sortType === 'time_asc') {
          return (left._updatedTs || 0) - (right._updatedTs || 0)
        }
        if (this.sortType === 'time_desc') {
          return (right._updatedTs || 0) - (left._updatedTs || 0)
        }
        return (right._recommendationScore || 0) - (left._recommendationScore || 0)
      })
    },
    heroStats() {
      const active7dCount = this.projectCards.filter(item => item._updatedTs && Date.now() - item._updatedTs <= 7 * DAY_MS).length
      const joinableCount = this.projectCards.filter(item => item._joinable || !!item._role).length
      const workbenchCount = this.projectCards.filter(item => !!item._role).length
      return [
        { id: 'displayed', label: '当前页推荐', value: String(this.governanceProjects.length), hint: '经过治理信号筛选' },
        { id: 'joinable', label: '可加入项目', value: String(joinableCount), hint: '可直接加入或已加入' },
        { id: 'active7d', label: '近 7 天活跃', value: String(active7dCount), hint: '最近有更新动作' },
        { id: 'workbench', label: '可进入工作台', value: String(workbenchCount), hint: '当前账号有项目角色' }
      ]
    },
    techTabs() {
      const map = {}
      this.projectCards.forEach(item => {
        item._tags.forEach(tag => {
          const key = String(tag || '').trim()
          if (!key) return
          map[key] = (map[key] || 0) + 1
        })
      })
      const tabList = Object.keys(map)
        .sort((a, b) => map[b] - map[a] || a.localeCompare(b))
        .slice(0, 10)
        .map(tag => ({ id: `tech-tab-${tag}`, label: tag, value: tag, count: map[tag] }))
      return [{ id: 'tech-tab-all', label: '全部技术栈', value: 'all', count: this.total || this.projectCards.length }, ...tabList]
    },
    techCloudItems() {
      return this.techTabs.map(item => ({
        id: `cloud-${item.value}`,
        label: item.label,
        value: item.value,
        count: item.count
      }))
    },
    activeFilters() {
      const filters = []
      if (this.activeTechFilter) filters.push({ id: 'tech', key: 'tech', value: this.activeTechFilter, text: `技术栈：${this.activeTechFilter}` })
      if (this.routeKeyword) filters.push({ id: 'keyword', key: 'keyword', value: this.routeKeyword, text: `关键词：${this.routeKeyword}` })
      if (this.currentAuthor) filters.push({ id: 'author', key: 'author', value: this.currentAuthor, text: `作者：${this.currentAuthor}` })
      if (this.activeJoinableFilter !== 'all') {
        filters.push({
          id: 'joinable',
          key: 'joinable',
          value: this.activeJoinableFilter,
          text: `可加入：${this.activeJoinableFilter === 'yes' ? '是' : '否'}`
        })
      }
      if (this.activeUpdatedFilter !== 'all') {
        filters.push({
          id: 'updated',
          key: 'updated',
          value: this.activeUpdatedFilter,
          text: `更新时间：${this.updatedFilterLabel(this.activeUpdatedFilter)}`
        })
      }
      return filters
    },
    currentFilterText() {
      if (!this.activeFilters.length) return '当前筛选：全部项目'
      return `当前筛选：${this.activeFilters.map(item => item.text).join('，')}`
    },
    activeRankList() {
      return [...this.projectCards]
        .sort((a, b) => b._activeScore - a._activeScore || b._updatedTs - a._updatedTs)
        .slice(0, 5)
    },
    newReleaseRankList() {
      return [...this.projectCards]
        .filter(item => item._releaseTs > 0 || toTimestamp(item.createdAt) > 0)
        .sort((a, b) => {
          const rightTs = b._releaseTs || toTimestamp(b.createdAt)
          const leftTs = a._releaseTs || toTimestamp(a.createdAt)
          return rightTs - leftTs
        })
        .slice(0, 5)
    },
    starRankList() {
      return [...this.projectCards]
        .sort((a, b) => b._starCount - a._starCount || b._downloadCount - a._downloadCount)
        .slice(0, 5)
    }
  },
  watch: {
    '$route.query': {
      immediate: true,
      handler(nextQuery, prevQuery) {
        if (!prevQuery) {
          this.fetchProjects()
          return
        }
        const remoteKeys = ['page', 'keyword', 'author', 'tech']
        const shouldFetch = remoteKeys.some(key => String(nextQuery[key] || '') !== String(prevQuery[key] || ''))
        if (shouldFetch) this.fetchProjects()
      }
    }
  },
  mounted() {
    this.clientHydrated = true
    if (this.projects.length) this.hydratePermissionMap()
  },
  methods: {
    normalizeTags: parseTags,
    mapSortType(sortType) {
      const map = { hot: 'hot', time_desc: 'latest', time_asc: 'earliest' }
      return map[sortType] || 'latest'
    },
    normalizeProjectStatus(status) {
      const value = String(status || '').trim().toLowerCase()
      const map = {
        draft: 'draft',
        pending: 'pending',
        published: 'published',
        rejected: 'rejected',
        archived: 'archived',
        active: 'published',
        ongoing: 'published',
        completed: 'archived',
        'in-progress': 'published',
        in_progress: 'published',
        '草稿': 'draft',
        '待审核': 'pending',
        '已发布': 'published',
        '已拒绝': 'rejected',
        '已归档': 'archived'
      }
      return map[value] || value || 'draft'
    },
    updatedFilterLabel(value) {
      const target = UPDATED_FILTER_OPTIONS.find(item => item.value === value)
      return target ? target.label : '全部时间'
    },
    resolveUpdatedDays(value) {
      return { all: 0, '7d': 7, '30d': 30, '90d': 90 }[value] || 0
    },
    updateQuery(patch = {}, options = {}) {
      const { resetPage = true } = options
      const nextQuery = { ...this.$route.query, ...patch }
      if (resetPage) nextQuery.page = 1
      delete nextQuery.status

      Object.keys(nextQuery).forEach(key => {
        const value = nextQuery[key]
        if (value === undefined || value === null || value === '' || value === 'all') {
          delete nextQuery[key]
        }
      })
      this.$router.push({ path: '/projectlist', query: nextQuery })
    },
    handleTechTabChange(value) {
      this.updateQuery({ tech: value === 'all' ? '' : value })
    },
    handleTechCloudSelect(payload) {
      const value = payload && payload.value ? payload.value : 'all'
      this.handleTechTabChange(value)
    },
    setJoinableFilter(value) {
      this.updateQuery({ joinable: value })
    },
    setUpdatedFilter(value) {
      this.updateQuery({ updated: value })
    },
    handleSortChange(value) {
      this.sortType = value
      if (this.currentPage !== 1) {
        this.currentPage = 1
        return
      }
      this.fetchProjects()
    },
    handleFilterRemove(payload) {
      const item = payload && payload.item ? payload.item : null
      if (!item || !item.key) return
      const patch = {}
      patch[item.key] = ''
      this.updateQuery(patch)
    },
    handlePageChange(page) {
      this.currentPage = page
    },
    clearFilters() {
      this.$router.push({ path: '/projectlist', query: {} })
    },
    filterByTech(tech) {
      this.updateQuery({ tech: tech || '' })
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
        const list = Array.isArray(response?.data?.list) ? response.data.list : []
        this.projects = list.map(item => ({ ...item, tags: parseTags(item.tags) }))
        this.total = Number(response?.data?.total) || 0
        await this.hydratePermissionMap()
        this.prefetchReleaseSignals(this.projects)
      } catch (error) {
        console.error('获取项目列表失败:', error)
        this.projects = []
        this.total = 0
        this.userProjectRoleMap = {}
        this.releaseSignalMap = {}
        this.$message.error(error.response?.data?.message || '获取项目列表失败')
      } finally {
        this.loading = false
      }
    },
    async hydratePermissionMap() {
      if (!this.clientHydrated || !this.isLoggedIn) {
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
    async prefetchReleaseSignals(list) {
      const candidates = (Array.isArray(list) ? list : []).filter(item => Number(item.id)).slice(0, 12)
      const requestToken = ++this.releaseSignalRequestToken

      if (!candidates.length) {
        this.releaseSignalMap = {}
        this.releaseSignalLoading = false
        return
      }

      this.releaseSignalLoading = true
      const nextMap = {}
      await runWithConcurrency(candidates, 3, async item => {
        const projectId = Number(item.id)
        if (!projectId) return null
        try {
          const response = await withTimeout(getLatestProjectRelease(projectId), 1200)
          const release = response?.data
          if (release && typeof release === 'object' && Object.keys(release).length > 0) {
            nextMap[projectId] = release
          }
        } catch (error) {
          return null
        }
        return null
      })

      if (requestToken !== this.releaseSignalRequestToken) return
      this.releaseSignalMap = nextMap
      this.releaseSignalLoading = false
    },
    projectUserRole(project) {
      if (!this.clientHydrated) return ''
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
    resolveJoinable(project, role) {
      if (role) return true
      if (project.joinable !== undefined) return !!project.joinable
      if (project.canJoin !== undefined) return !!project.canJoin
      if (project.allowJoin !== undefined) return !!project.allowJoin
      if (project.joinEnabled !== undefined) return !!project.joinEnabled

      const policy = String(project.joinPolicy || project.joinType || project.joinMode || '').trim().toLowerCase()
      if (['open', 'public', 'auto', 'free'].includes(policy)) return true
      if (['closed', 'private', 'invite_only', 'invite-only', 'disabled', 'none'].includes(policy)) return false
      if (String(project.visibility || '').toLowerCase() === 'private') return false
      if (['rejected', 'archived'].includes(this.normalizeProjectStatus(project.status))) return false
      return true
    },
    techMatched(tags, activeTech) {
      if (!activeTech) return false
      const target = String(activeTech).trim().toLowerCase()
      return (Array.isArray(tags) ? tags : []).some(tag => String(tag).trim().toLowerCase() === target)
    },
    hasCompleteSignal(project, tags) {
      const description = String(project.description || '').trim()
      return description.length >= 40 || (description.length >= 18 && Array.isArray(tags) && tags.length >= 2)
    },
    buildRecommendationReasons(payload) {
      const reasons = []
      const now = Date.now()
      const recentDays = payload.updatedTs ? Math.floor((now - payload.updatedTs) / DAY_MS) : 999
      if (payload.techMatched) reasons.push('技术栈匹配')
      if (recentDays <= 7) reasons.push('近期活跃')
      if (payload.popularityScore >= 40) reasons.push('热度较高')
      if (payload.role) reasons.push('当前用户可进入工作台')
      if (payload.completeSignal) reasons.push('文档/描述完整')
      if (!reasons.length) reasons.push('基础信息完整')
      return reasons.slice(0, 4)
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
    getAuthorAvatar(project) {
      const author = this.authorMap[project.authorId] || {}
      return pickAvatarUrl(project.authorAvatarUrl, project.authorAvatar, author.avatarUrl, author.avatar)
    },
    getAuthorName(project) {
      const author = this.authorMap[project.authorId] || {}
      return project.authorName || author.nickname || author.username || '未知作者'
    },
    formatDescription(desc) {
      const text = String(desc || '').trim()
      if (!text) return '暂无描述'
      return text.length > 160 ? `${text.slice(0, 160)}...` : text
    },
    formatDate(value) {
      const ts = toTimestamp(value)
      if (!ts) return ''
      return new Date(ts).toLocaleDateString('zh-CN')
    },
    formatDateTime(value) {
      const ts = toTimestamp(value)
      if (!ts) return ''
      return new Date(ts).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
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
    getProjectTypeTag(type) {
      const typeMap = {
        frontend: 'primary',
        backend: 'success',
        fullstack: 'warning',
        mobile: 'success',
        ai: 'danger',
        tools: 'info'
      }
      return typeMap[type] || 'info'
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
    }
  }
}
</script>

<style scoped>
.project-governance-page {
  --project-radius-card: 8px;
  --project-radius-control: 8px;
  --project-border: var(--it-border, #dbe2ea);
  --project-border-strong: var(--it-border-strong, #b6c3d1);
  --project-surface: var(--it-surface, #ffffff);
  --project-page-bg: var(--it-page-bg, #f5f8fc);
  --project-text: var(--it-text, #0f172a);
  --project-muted: var(--it-text-muted, #64748b);
  --project-shadow: var(--it-shadow, 0 6px 18px rgba(15, 23, 42, 0.06));

  min-height: 100vh;
  background: var(--project-page-bg);
  padding: 12px 0 20px;
}

.project-governance-hero :deep(.front-hero-panel) {
  border-radius: var(--project-radius-card);
  border-color: var(--project-border);
  box-shadow: var(--project-shadow);
}

.project-governance-hero :deep(.front-hero-panel__title) {
  font-size: 30px;
  color: var(--project-text);
}

.project-governance-hero :deep(.front-hero-panel__subtitle),
.project-governance-hero :deep(.front-hero-panel__stat-label),
.project-governance-hero :deep(.front-hero-panel__stat-hint) {
  color: var(--project-muted);
}

.project-governance-hero :deep(.front-hero-panel__stat-card) {
  border-radius: var(--project-radius-control);
  border-color: var(--project-border);
  background: color-mix(in srgb, var(--project-surface) 88%, var(--it-accent-soft, #e8f1ff));
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.project-governance-shell :deep(.front-page-shell__main.project-governance-main) {
  border: none;
  box-shadow: none;
  background: transparent;
  padding: 0;
}

.project-filter-rail {
  display: grid;
  gap: 10px;
}

.left-panel {
  border: 1px solid var(--project-border);
  border-radius: var(--project-radius-card);
  background: var(--project-surface);
  box-shadow: var(--project-shadow);
  padding: 12px;
}

.left-panel h3 {
  margin: 0 0 10px;
  font-size: 14px;
  color: var(--project-text);
}

.left-panel :deep(.front-tag-cloud) {
  border: none;
  box-shadow: none;
  padding: 0;
  background: transparent;
}

.left-panel :deep(.front-tag-cloud__item) {
  border-color: var(--project-border);
}

.filter-radio-group {
  width: 100%;
  display: grid;
  gap: 8px;
}

.filter-radio-group :deep(.el-radio-button) {
  width: 100%;
}

.filter-radio-group :deep(.el-radio-button__inner) {
  width: 100%;
  border-radius: var(--project-radius-control);
  border-color: var(--project-border);
  color: var(--project-muted);
}

.project-main-content {
  display: grid;
  gap: 12px;
}

.project-toolbar :deep(.front-feed-toolbar) {
  border-radius: var(--project-radius-card);
  border-color: var(--project-border);
  box-shadow: var(--project-shadow);
}

.project-toolbar :deep(.front-feed-toolbar__controls) {
  justify-content: flex-start;
}

.project-toolbar :deep(.front-feed-toolbar__filter-chip) {
  border-radius: 999px;
  border-color: var(--project-border);
}

.toolbar-summary,
.toolbar-current-filter {
  font-size: 12px;
  color: var(--project-muted);
}

.project-list-zone {
  min-height: 360px;
}

.governance-list {
  display: grid;
  gap: 12px;
}

.governance-card {
  border-radius: var(--project-radius-card);
  border: 1px solid var(--project-border);
  box-shadow: var(--project-shadow);
  cursor: pointer;
}

.governance-card:hover {
  border-color: var(--project-border-strong);
}

.governance-card__header {
  margin-bottom: 10px;
}

.governance-card__headline {
  display: grid;
  gap: 8px;
}

.governance-card__title {
  margin: 0;
  color: var(--project-text);
  font-size: 21px;
  line-height: 1.35;
}

.governance-card__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.governance-card__description {
  margin: 10px 0 0;
  color: var(--project-muted);
  line-height: 1.72;
  font-size: 14px;
}

.governance-card__meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 10px;
}

.meta-item {
  border: 1px solid var(--project-border);
  border-radius: var(--project-radius-control);
  background: color-mix(in srgb, var(--project-surface) 94%, var(--it-accent-soft, #e8f1ff));
  padding: 8px 10px;
  min-width: 0;
}

.meta-item__label {
  display: block;
  font-size: 12px;
  color: var(--project-muted);
  margin-bottom: 6px;
}

.meta-item__value {
  color: var(--project-text);
  font-size: 13px;
  line-height: 1.5;
  min-height: 20px;
}

.meta-item__placeholder {
  color: var(--project-muted);
  font-size: 12px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tech-tag {
  border-radius: 999px;
  border-color: var(--project-border);
  cursor: pointer;
}

.author-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.governance-card__signals {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  padding: 8px 0 10px;
  border-top: 1px solid var(--project-border);
}

.signal-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--project-muted);
  font-size: 12px;
}

.governance-card__reasons {
  border-top: 1px solid var(--project-border);
  padding-top: 10px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.reason-label {
  color: var(--project-muted);
  font-size: 12px;
}

.reason-tag {
  border-radius: 999px;
  border-color: var(--project-border);
}

.project-actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--project-border);
}

.rank-section {
  display: grid;
  gap: 8px;
}

.rank-title {
  margin: 0;
  color: var(--project-text);
  font-size: 14px;
}

.rank-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 8px;
}

.rank-item {
  width: 100%;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  border: 1px solid var(--project-border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--project-surface) 94%, var(--it-accent-soft, #e8f1ff));
  padding: 8px 10px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.rank-item:hover {
  border-color: var(--project-border-strong);
}

.rank-item__index {
  width: 20px;
  flex: 0 0 20px;
  color: var(--it-accent, #2563eb);
  font-size: 13px;
  font-weight: 700;
  line-height: 1.5;
}

.rank-item__copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.rank-item__copy strong {
  color: var(--project-text);
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
}

.rank-item__copy em {
  font-style: normal;
  color: var(--project-muted);
  font-size: 12px;
}

.rail-empty-tip,
.rail-loading-tip {
  margin: 0;
  color: var(--project-muted);
  font-size: 12px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 4px;
  padding-bottom: 4px;
}

.project-governance-page :deep(.el-pagination.is-background .btn-prev),
.project-governance-page :deep(.el-pagination.is-background .btn-next),
.project-governance-page :deep(.el-pagination.is-background .el-pager li) {
  background: var(--project-surface);
  border-radius: 8px;
  border: 1px solid var(--project-border);
}

.project-governance-page :deep(.el-pagination.is-background .el-pager li:not(.disabled).active) {
  border-color: transparent;
  color: #ffffff;
  background: var(--it-primary-gradient, linear-gradient(135deg, #2563eb, #3b82f6));
}

@media screen and (max-width: 1024px) {
  .project-governance-hero :deep(.front-hero-panel__stats) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .governance-card__meta-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .hero-actions {
    justify-content: flex-start;
  }
}

@media screen and (max-width: 768px) {
  .project-governance-page {
    padding: 10px 0 16px;
  }

  .project-governance-hero :deep(.front-hero-panel__title) {
    font-size: 24px;
  }

  .project-governance-hero :deep(.front-hero-panel__stats) {
    grid-template-columns: minmax(0, 1fr);
  }

  .governance-card__title {
    font-size: 18px;
  }

  .project-actions-row {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
