<template>
  <div class="project-manage-page-shell">
    <div v-if="pageReady && projectId" class="project-manage-page">
      <ProjectManageHeader
        :project="project"
        @back="goToDetail"
        @refresh="refreshAll"
        @open-settings="goToSettingsTab"
        @open-save-template="openSaveAsTemplate"
      />

      <ProjectManageEntryHub
        current-key="manage"
        title="管理侧统一入口"
        subtitle="这里统一承接项目治理总览，再按需要切到独立审核页或下架管理页，同时尽量保持同一个项目上下文。"
        :project-id="projectId"
        :entries="manageEntries"
        :status-cards="manageStatusCards"
      />

      <el-tabs v-model="activeTab" class="manage-tabs">
        <el-tab-pane label="概览" name="overview"></el-tab-pane>
        <el-tab-pane label="仓库工作区 / Commit 历史" name="repo-workbench"></el-tab-pane>
        <el-tab-pane label="MR / 审核中心" name="audit-manage"></el-tab-pane>
        <el-tab-pane label="里程碑" name="milestone-manage"></el-tab-pane>
        <el-tab-pane label="发布 / 交付" name="release-manage"></el-tab-pane>
        <el-tab-pane label="统计分析" name="stat-manage"></el-tab-pane>
        <el-tab-pane v-if="resolvedCanSeeTaskCollaboration" :label="`任务协作 (${taskCount})`" name="task-manage"></el-tab-pane>
        <el-tab-pane :label="`成员 (${members.length})`" name="member-manage"></el-tab-pane>
        <el-tab-pane :label="`文件 (${fileCount})`" name="file-manage"></el-tab-pane>
        <el-tab-pane :label="`文档 (${docCount})`" name="doc-manage"></el-tab-pane>
        <el-tab-pane :label="`活动流 (${activityTotal})`" name="activity-manage"></el-tab-pane>
        <el-tab-pane label="Sprint" name="sprint-manage"></el-tab-pane>
        <el-tab-pane label="下载记录" name="download-manage"></el-tab-pane>
        <el-tab-pane label="设置" name="settings"></el-tab-pane>
      </el-tabs>

      <div v-if="activeTab === 'overview'" class="tab-panel">
        <ProjectManageOverviewTab
          :project-id="projectId"
          :project="project"
          :members="members"
          :can-see-task-collaboration="resolvedCanSeeTaskCollaboration"
          :refresh-seed="refreshSeed"
          @switch-tab="switchManageTab"
          @open-activity="goToActivityManage"
          @summary-change="handleSummaryChange"
        />
      </div>

      <div v-if="activeTab === 'task-manage'" class="tab-panel">
        <ProjectManageTaskTab
          :project-id="projectId"
          :members="members"
          :current-user-id="resolvedCurrentUserId"
          :current-member-record="resolvedCurrentMemberRecord"
          :can-manage-project="resolvedCanManageProject"
          :can-see-task-collaboration="resolvedCanSeeTaskCollaboration"
          :refresh-seed="refreshSeed"
          :initial-mine-only="$route.query.mineOnly === '1' || $route.query.tab === 'my-tasks'"
          @task-count-change="taskCount = $event"
          @request-refresh="refreshAll"
        />
      </div>

      <div v-if="activeTab === 'member-manage'" class="tab-panel">
        <ProjectManageMemberTab
          :project-id="projectId"
          :members="members"
          :current-user-id="resolvedCurrentUserId"
          :current-member-record="resolvedCurrentMemberRecord"
          :can-manage-project="resolvedCanManageProject"
          :refresh-seed="refreshSeed"
          @request-refresh="refreshAll"
        />
      </div>

      <div v-if="activeTab === 'file-manage'" class="tab-panel">
        <ProjectManageFileTab
          :project-id="projectId"
          :branch-list="branchList"
          :current-branch-id="currentBranchId"
          :default-branch-id="defaultBranchId"
          :refresh-seed="refreshSeed"
          @file-count-change="fileCount = $event"
          @switch-tab="switchManageTab"
          @request-refresh="refreshAll"
        />
      </div>

      <div v-if="activeTab === 'doc-manage'" class="tab-panel">
        <ProjectManageDocTab
          :project-id="projectId"
          :initial-doc-id="$route.query.docId || null"
          :initial-mode="$route.query.mode || 'view'"
          :refresh-seed="refreshSeed"
          @doc-count-change="docCount = $event"
          @request-refresh="refreshAll"
        />
      </div>

      <div v-if="activeTab === 'activity-manage'" class="tab-panel">
        <ProjectManageActivityTab
          :project-id="projectId"
          :initial-activity-id="$route.query.activityId ? Number($route.query.activityId) : null"
          :member-options="members.filter(item => item && item.userId)"
          :refresh-seed="refreshSeed"
          @activity-total-change="activityTotal = $event"
        />
      </div>

      <div v-if="activeTab === 'milestone-manage'" class="tab-panel">
        <ProjectMilestoneManage :project-id="projectId" :can-manage-project="resolvedCanManageProject" />
      </div>

      <div v-if="activeTab === 'sprint-manage'" class="tab-panel">
        <ProjectSprintManage :project-id="projectId" :can-manage-project="resolvedCanManageProject" />
      </div>

      <div v-if="activeTab === 'release-manage'" class="tab-panel">
        <ProjectReleaseManage :project-id="projectId" :can-manage-project="resolvedCanManageProject" />
      </div>

      <div v-if="activeTab === 'audit-manage'" class="tab-panel">
        <ProjectAuditCenter :project-id="projectId" :can-manage-project="resolvedCanManageProject" />
      </div>

      <div v-if="activeTab === 'download-manage'" class="tab-panel">
        <ProjectManageDownloadTab :project-id="projectId" :refresh-seed="refreshSeed" />
      </div>

      <div v-if="activeTab === 'stat-manage'" class="tab-panel">
        <ProjectStatManage :project-id="projectId" />
      </div>

      <div v-if="activeTab === 'repo-workbench'" class="tab-panel">
        <ProjectRepoWorkbench
          :project-id="projectId"
          :project="project"
          :can-manage-project="resolvedCanManageProject"
          :managed-branch-list="branchList"
          :managed-current-branch-id="currentBranchId"
          @update:branch-list="handleBranchListUpdate"
          @update:current-branch-id="handleCurrentBranchChange"
          @update:default-branch-id="handleDefaultBranchIdChange"
        />
      </div>

      <div v-if="activeTab === 'settings'" class="tab-panel">
        <ProjectManageSettingsTab
          :project-id="projectId"
          :project="project"
          :can-manage-project="resolvedCanManageProject"
          @open-save-template="openSaveAsTemplate"
          @request-refresh="refreshAll"
        />
      </div>

      <ProjectTemplateSaveDialog
        :visible.sync="saveTemplateDialogVisible"
        :project-id="projectId"
        :default-name="project.title || project.name || ''"
        :default-category="project.category || ''"
        :default-description="project.description || ''"
        @saved="handleTemplateSaved"
      />
    </div>

    <div v-else class="project-manage-fallback" v-loading="!pageReady && !!projectId && !pageLoadError">
      <el-card shadow="never" class="project-manage-fallback-card">
        <el-empty :description="pageLoadError || '当前缺少项目上下文，请从项目列表或项目详情进入管理页。'">
          <el-button type="primary" @click="$router.push('/projectlist')">返回项目列表</el-button>
        </el-empty>
      </el-card>
    </div>
  </div>
</template>

<script>
import ProjectAuditCenter from './components/ProjectAuditCenter.vue'
import ProjectManageEntryHub from './components/ProjectManageEntryHub.vue'
import ProjectMilestoneManage from './components/ProjectMilestoneManage.vue'
import ProjectReleaseManage from './components/ProjectReleaseManage.vue'
import ProjectRepoWorkbench from './components/ProjectRepoWorkbench.vue'
import ProjectSprintManage from './components/ProjectSprintManage.vue'
import ProjectStatManage from './components/ProjectStatManage.vue'
import ProjectTemplateSaveDialog from './components/ProjectTemplateSaveDialog.vue'
import ProjectManageHeader from './components/ProjectManageHeader.vue'
import ProjectManageActivityTab from './tabs/ProjectManageActivityTab.vue'
import ProjectManageDocTab from './tabs/ProjectManageDocTab.vue'
import ProjectManageDownloadTab from './tabs/ProjectManageDownloadTab.vue'
import ProjectManageFileTab from './tabs/ProjectManageFileTab.vue'
import ProjectManageMemberTab from './tabs/ProjectManageMemberTab.vue'
import ProjectManageOverviewTab from './tabs/ProjectManageOverviewTab.vue'
import ProjectManageSettingsTab from './tabs/ProjectManageSettingsTab.vue'
import ProjectManageTaskTab from './tabs/ProjectManageTaskTab.vue'
import { listProjectBranches } from '@/api/projectBranch'
import { getProjectRepository } from '@/api/projectRepository'
import {
  fetchProjectManageContext,
  fetchProjectManageSummary,
  readCurrentUserId,
  sameId
} from './services/projectManageShared'

export default {
  layout: 'project',
  components: {
    ProjectAuditCenter,
    ProjectManageActivityTab,
    ProjectManageDocTab,
    ProjectManageDownloadTab,
    ProjectManageEntryHub,
    ProjectManageFileTab,
    ProjectManageHeader,
    ProjectManageMemberTab,
    ProjectManageOverviewTab,
    ProjectManageSettingsTab,
    ProjectManageTaskTab,
    ProjectMilestoneManage,
    ProjectReleaseManage,
    ProjectRepoWorkbench,
    ProjectSprintManage,
    ProjectStatManage,
    ProjectTemplateSaveDialog
  },
  data() {
    return {
      projectId: null,
      activeTab: 'overview',
      routeSyncing: false,
      clientHydrated: false,
      pageReady: false,
      pageLoadError: '',
      project: {},
      members: [],
      taskCount: 0,
      fileCount: 0,
      docCount: 0,
      activityTotal: 0,
      branchList: [],
      currentBranchId: null,
      defaultBranchId: null,
      refreshSeed: 0,
      saveTemplateDialogVisible: false
    }
  },
  computed: {
    currentUserId() {
      if (!this.clientHydrated) return null
      return readCurrentUserId()
    },
    canSeeTaskCollaboration() {
      if (this.currentUserId === null || this.currentUserId === undefined) return false
      if (sameId(this.project.authorId, this.currentUserId)) return true
      return (this.members || []).some(member => {
        if (!member || member.isOwner) return false
        return sameId(member.userId, this.currentUserId)
      })
    },
    currentMemberRecord() {
      if (this.currentUserId === null || this.currentUserId === undefined) return null
      if (sameId(this.project.authorId, this.currentUserId)) {
        return { userId: this.currentUserId, role: 'owner', joinedAt: '' }
      }
      return (this.members || []).find(member => {
        if (!member || member.isOwner) return false
        return sameId(member.userId, this.currentUserId)
      }) || null
    },
    canManageProject() {
      if (this.currentUserId === null || this.currentUserId === undefined) return false
      if (sameId(this.project.authorId, this.currentUserId)) return true
      const role = this.currentMemberRecord && this.currentMemberRecord.role ? String(this.currentMemberRecord.role).toLowerCase() : ''
      return role === 'owner' || role === 'admin'
    },
    accessResolved() {
      return this.clientHydrated
    },
    resolvedCurrentUserId() {
      return this.accessResolved ? this.currentUserId : null
    },
    resolvedCurrentMemberRecord() {
      return this.accessResolved ? this.currentMemberRecord : null
    },
    resolvedCanManageProject() {
      return this.accessResolved && this.canManageProject
    },
    resolvedCanSeeTaskCollaboration() {
      return this.accessResolved && this.canSeeTaskCollaboration
    },
    activeTabDisplayText() {
      const labelMap = {
        overview: '概览',
        'task-manage': '任务协作',
        'member-manage': '成员管理',
        'file-manage': '文件管理',
        'doc-manage': '项目文档',
        'activity-manage': '活动流',
        'milestone-manage': '里程碑',
        'sprint-manage': 'Sprint',
        'release-manage': '发布记录',
        'audit-manage': '审核中心',
        'download-manage': '下载记录',
        'stat-manage': '统计分析',
        'repo-workbench': '仓库工作台',
        settings: '设置'
      }
      return labelMap[this.activeTab] || '概览'
    },
    manageEntries() {
      return [
        {
          key: 'manage',
          title: '项目管理',
          desc: '查看总览、任务、成员、文件、仓库和设置，作为统一治理主入口。',
          path: '/projectmanage',
          query: this.projectId ? { projectId: String(this.projectId), tab: this.activeTab || 'overview' } : undefined,
          requiresProjectId: true,
          disabled: !this.projectId,
          tone: 'blue'
        },
        {
          key: 'audit',
          title: '审核中心',
          desc: '切到独立审核页查看 MR、评审、检查和主线保护状态。',
          path: '/projectaudit',
          query: this.projectId ? { projectId: String(this.projectId), fromTab: this.activeTab || 'overview' } : undefined,
          requiresProjectId: true,
          disabled: !this.projectId,
          tone: 'cyan'
        },
        {
          key: 'miss',
          title: '下架管理',
          desc: '处理已下架项目的恢复、删除和后续治理动作。',
          path: '/projectmiss',
          query: this.projectId ? { projectId: String(this.projectId), fromTab: this.activeTab || 'overview' } : undefined,
          tone: 'orange'
        }
      ]
    },
    manageStatusCards() {
      return [
        {
          key: 'context',
          label: '项目上下文',
          value: this.projectId ? `#${this.projectId}` : '未绑定',
          desc: this.project && this.project.title ? this.project.title : '当前管理页会把项目上下文继续传给审核和下架入口。',
          tone: 'blue'
        },
        {
          key: 'tab',
          label: '当前入口',
          value: this.activeTabDisplayText,
          desc: '切换 tab 时，顶部治理入口和状态卡保持不变。',
          tone: 'cyan'
        },
        {
          key: 'collab',
          label: '协作规模',
          value: `${this.members.length} / ${this.taskCount}`,
          desc: '成员数 / 任务数，用来快速感知当前协作体量。',
          tone: 'purple'
        },
        {
          key: 'asset',
          label: '资产与动态',
          value: `${this.fileCount} / ${this.activityTotal}`,
          desc: '文件数 / 最近活动总量，帮助判断项目当前活跃度。',
          tone: 'orange'
        }
      ]
    }
  },
  watch: {
    '$route.query.projectId': {
      immediate: true,
      async handler(value) {
        const nextProjectId = value || this.$route.params.id || null
        if (String(nextProjectId || '') === String(this.projectId || '')) {
          if (!nextProjectId && !this.pageReady) {
            this.pageReady = true
            this.pageLoadError = '当前缺少项目 ID，请从项目列表或项目详情进入管理页。'
          }
          return
        }
        this.projectId = nextProjectId
        this.branchList = []
        this.defaultBranchId = null
        if (!this.$route.query.branchId) {
          this.currentBranchId = null
        }
        this.pageLoadError = ''
        if (!this.projectId) {
          this.pageReady = true
          this.pageLoadError = '当前缺少项目 ID，请从项目列表或项目详情进入管理页。'
          return
        }
        this.pageReady = false
        try {
          await this.refreshAll()
        } finally {
          this.pageReady = true
        }
      }
    },
    '$route.query': {
      immediate: true,
      handler(query) {
        this.applyRouteState(query || {})
      }
    },
    activeTab(val) {
      if (this.routeSyncing) return
      if (val === 'task-manage' && this.accessResolved && !this.ensureTaskCollaborationAccess(true, true)) return
      this.syncRouteTab(val)
    },
    currentBranchId(val, oldVal) {
      if (this.routeSyncing) return
      if (String(val || '') === String(oldVal || '')) return
      this.syncRouteTab(this.activeTab, { branchId: val || undefined })
    }
  },
  methods: {
    unwrapApiResponse(response) {
      const raw = response && Object.prototype.hasOwnProperty.call(response, 'data') ? response.data : response
      if (raw && typeof raw === 'object' && Object.prototype.hasOwnProperty.call(raw, 'code')) {
        return raw.data
      }
      return raw
    },
    normalizeBranchId(value) {
      if (value === undefined || value === null || value === '') return null
      const num = Number(value)
      if (Number.isFinite(num) && num > 0) return num
      return null
    },
    async refreshBranchContext() {
      if (!this.projectId) {
        this.branchList = []
        this.currentBranchId = null
        this.defaultBranchId = null
        return
      }
      let nextDefaultBranchId = null
      try {
        const repository = this.unwrapApiResponse(await getProjectRepository(this.projectId))
        nextDefaultBranchId = this.normalizeBranchId(repository && repository.defaultBranchId)
      } catch (error) {
        nextDefaultBranchId = null
      }
      this.defaultBranchId = nextDefaultBranchId

      try {
        const branchResponse = await listProjectBranches(this.projectId)
        const list = this.unwrapApiResponse(branchResponse)
        this.branchList = Array.isArray(list) ? list : []
      } catch (error) {
        this.branchList = []
      }

      if (this.currentBranchId && !this.branchList.some(item => String(item.id) === String(this.currentBranchId))) {
        this.currentBranchId = null
      }
      if (!this.currentBranchId) {
        if (this.defaultBranchId && this.branchList.some(item => String(item.id) === String(this.defaultBranchId))) {
          this.currentBranchId = this.defaultBranchId
        } else if (this.branchList.length > 0) {
          this.currentBranchId = this.normalizeBranchId(this.branchList[0].id)
        }
      }
    },
    handleBranchListUpdate(list) {
      this.branchList = Array.isArray(list) ? list : []
      if (this.currentBranchId && !this.branchList.some(item => String(item.id) === String(this.currentBranchId))) {
        this.currentBranchId = null
      }
    },
    handleCurrentBranchChange(branchId) {
      this.currentBranchId = this.normalizeBranchId(branchId)
    },
    handleDefaultBranchIdChange(branchId) {
      this.defaultBranchId = this.normalizeBranchId(branchId)
    },
    normalizeManageTab(tab) {
      const raw = String(tab || 'overview')
      const map = {
        'my-tasks': 'task-manage',
        'template-manage': 'settings',
        activity: 'activity-manage',
        tasks: 'task-manage',
        members: 'member-manage',
        files: 'file-manage',
        docs: 'doc-manage'
      }
      const next = map[raw] || raw
      const allow = ['overview', 'task-manage', 'member-manage', 'file-manage', 'doc-manage', 'activity-manage', 'settings', 'milestone-manage', 'sprint-manage', 'release-manage', 'audit-manage', 'download-manage', 'stat-manage', 'repo-workbench']
      return allow.includes(next) ? next : 'overview'
    },
    applyRouteState(query = {}) {
      this.routeSyncing = true
      this.activeTab = this.normalizeManageTab(query.tab)
      if (Object.prototype.hasOwnProperty.call(query, 'branchId')) {
        this.currentBranchId = this.normalizeBranchId(query.branchId)
      }
      if (query.tab === 'template-manage') {
        this.saveTemplateDialogVisible = true
      }
      this.$nextTick(() => {
        this.routeSyncing = false
      })
    },
    syncRouteTab(tab, extraQuery = {}) {
      if (!this.projectId) return
      const query = {
        ...this.$route.query,
        projectId: String(this.projectId),
        tab
      }
      if (this.currentBranchId) query.branchId = String(this.currentBranchId)
      else delete query.branchId
      delete query.fromTab
      if (tab !== 'task-manage') {
        delete query.mineOnly
        delete query.taskId
      }
      if (tab !== 'activity-manage') {
        delete query.activityId
        delete query.action
        delete query.targetType
        delete query.operatorId
        delete query.startTime
        delete query.endTime
      }
      Object.keys(extraQuery).forEach(key => {
        const value = extraQuery[key]
        if (value === undefined || value === null || value === '') delete query[key]
        else query[key] = String(value)
      })
      this.routeSyncing = true
      this.$router.replace({ path: '/projectmanage', query }).finally(() => {
        this.routeSyncing = false
      })
    },
    switchManageTab(tab) {
      const nextTab = this.normalizeManageTab(tab)
      if (nextTab === 'task-manage' && !this.ensureTaskCollaborationAccess(true, true)) return
      this.activeTab = nextTab
    },
    goToSettingsTab() {
      this.activeTab = 'settings'
    },
    goToActivityManage(item, extra = {}) {
      if (!item || !item.id) return
      this.$router.push({
        path: '/projectmanage',
        query: {
          projectId: String(this.projectId),
          tab: 'activity-manage',
          activityId: String(item.id),
          branchId: this.currentBranchId ? String(this.currentBranchId) : undefined,
          ...extra
        }
      })
    },
    openSaveAsTemplate() {
      this.saveTemplateDialogVisible = true
    },
    async handleTemplateSaved() {
      this.$message.success('模板保存成功')
      await this.refreshAll()
    },
    ensureTaskCollaborationAccess(redirect = false, showFeedback = false) {
      if (!this.accessResolved) return true
      if (this.canSeeTaskCollaboration) return true
      if (this.activeTab === 'task-manage') {
        this.activeTab = 'overview'
      }
      if (redirect && this.projectId) {
        if (showFeedback) {
          this.$message.closeAll()
          this.$message.warning('只有已加入项目的成员才能进入任务协作，其他用户仅可在项目详情页查看贡献者列表')
        }
        this.$router.replace(`/projectdetail?projectId=${this.projectId}`)
      }
      return false
    },
    async refreshAll() {
      if (!this.projectId) return
      try {
        const { project, members } = await fetchProjectManageContext(this.projectId)
        this.project = project
        this.members = members
        await this.refreshBranchContext()
        if (this.activeTab === 'task-manage' && !this.ensureTaskCollaborationAccess(true, false)) {
          return
        }
        const summary = await fetchProjectManageSummary(this.projectId, { canSeeTaskCollaboration: this.canSeeTaskCollaboration })
        this.handleSummaryChange(summary)
        this.refreshSeed += 1
      } catch (error) {
        this.pageLoadError = error.response?.data?.message || error.message || '项目管理页初始化失败，请刷新后重试。'
        this.$message.error(this.pageLoadError)
      }
    },
    handleSummaryChange(summary = {}) {
      if (summary.taskCount !== undefined) this.taskCount = Number(summary.taskCount || 0)
      if (summary.fileCount !== undefined) this.fileCount = Number(summary.fileCount || 0)
      if (summary.activityTotal !== undefined) this.activityTotal = Number(summary.activityTotal || 0)
    },
    goToDetail() {
      this.$router.push(`/projectdetail?projectId=${this.projectId}`)
    }
  },
  mounted() {
    this.clientHydrated = true
  }
}
</script>

<style scoped>
.project-manage-page-shell { min-height: 100%; }
.project-manage-page { max-width: 1360px; margin: 0 auto; padding: 24px; background: var(--it-page-bg); }
.project-manage-fallback { max-width: 980px; margin: 0 auto; padding: 24px; }
.project-manage-fallback-card { border-radius: 24px; }
.manage-tabs { margin-bottom: 16px; }
.tab-panel { display: flex; flex-direction: column; gap: 16px; }

@media (max-width: 768px) {
  .project-manage-fallback,
  .project-manage-page { padding: 16px; }
}
</style>
