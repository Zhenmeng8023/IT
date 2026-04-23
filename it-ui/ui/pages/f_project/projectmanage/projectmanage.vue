<template>
  <div class="project-manage-page-shell">
    <div v-if="pageReady && projectId && resolvedCanAccessWorkbench" class="project-manage-page">
      <ProjectManageHeader
        :project="project"
        :current-role-text="currentUserRoleText"
        @back="goToDetail"
        @refresh="refreshAll"
      />

      <el-tabs v-model="activeTab" class="manage-tabs">
        <el-tab-pane
          v-for="tab in visibleManageTabs"
          :key="tab.name"
          :label="tab.label"
          :name="tab.name"
        ></el-tab-pane>
      </el-tabs>

      <div v-if="activeTab === 'overview'" class="tab-panel">
        <ProjectManageOverviewTab
          :project-id="projectId"
          :project="project"
          :members="members"
          :can-see-task-collaboration="resolvedCanSeeTaskCollaboration"
          :refresh-seed="refreshSeed"
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
        <ProjectAuditCenter
          :project-id="projectId"
          :can-manage-project="resolvedCanManageProject"
          :default-branch-id="defaultBranchId"
        />
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

      <div v-if="activeTab === 'knowledge'" class="tab-panel">
        <ProjectManageKnowledgeTab :project-id="projectId" />
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

    <div v-else-if="pageReady && projectId && accessResolved && !resolvedCanAccessWorkbench" class="project-manage-fallback">
      <el-card shadow="never" class="project-manage-fallback-card">
        <el-empty description="Current account has no permission to access the project workbench.">
          <el-button type="primary" @click="goToDetail">返回项目详情</el-button>
        </el-empty>
      </el-card>
    </div>

    <div v-else class="project-manage-fallback" v-loading="(!pageReady || !accessResolved) && !!projectId && !pageLoadError">
      <el-card shadow="never" class="project-manage-fallback-card">
        <el-empty :description="pageLoadError || 'Missing project context. Please open the workbench from My Projects or Project List.'">
          <el-button type="primary" @click="goToMyProjects">返回我的项目</el-button>
        </el-empty>
      </el-card>
    </div>
  </div>
</template>

<script>
import ProjectAuditCenter from './components/ProjectAuditCenter.vue'
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
import ProjectManageKnowledgeTab from './tabs/ProjectManageKnowledgeTab.vue'
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
    ProjectManageFileTab,
    ProjectManageHeader,
    ProjectManageMemberTab,
    ProjectManageOverviewTab,
    ProjectManageSettingsTab,
    ProjectManageTaskTab,
    ProjectManageKnowledgeTab,
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
    currentUserRole() {
      if (!this.accessResolved) return ''
      if (this.currentUserId === null || this.currentUserId === undefined) return 'visitor'
      if (sameId(this.project.authorId, this.currentUserId)) return 'owner'
      const role = this.currentMemberRecord && this.currentMemberRecord.role ? String(this.currentMemberRecord.role).toLowerCase() : ''
      return role || 'visitor'
    },
    currentUserRoleText() {
      const map = {
        owner: 'Owner',
        admin: 'Admin',
        member: '成员',
        visitor: '访客'
      }
      return map[this.currentUserRole] || this.currentUserRole || '-'
    },
    canAccessWorkbench() {
      return ['owner', 'admin', 'member'].includes(this.currentUserRole)
    },
    canSeeTaskCollaboration() {
      return this.canAccessWorkbench
    },
    canManageProject() {
      return this.currentUserRole === 'owner' || this.currentUserRole === 'admin'
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
    resolvedCanAccessWorkbench() {
      return this.accessResolved && this.canAccessWorkbench
    },
    resolvedCanSeeTaskCollaboration() {
      return this.accessResolved && this.canSeeTaskCollaboration
    },
    manageTabs() {
      return [
        { name: 'overview', label: '概览', roles: ['owner', 'admin', 'member'] },
        { name: 'task-manage', label: `任务协作 (${this.taskCount})`, roles: ['owner', 'admin', 'member'] },
        { name: 'member-manage', label: `成员 (${this.members.length})`, roles: ['owner', 'admin'] },
        { name: 'file-manage', label: `文件 (${this.fileCount})`, roles: ['owner', 'admin', 'member'] },
        { name: 'doc-manage', label: `文档 (${this.docCount})`, roles: ['owner', 'admin', 'member'] },
        { name: 'repo-workbench', label: '仓库', roles: ['owner', 'admin'] },
        { name: 'milestone-manage', label: 'Milestone', roles: ['owner', 'admin'] },
        { name: 'sprint-manage', label: 'Sprint', roles: ['owner', 'admin'] },
        { name: 'release-manage', label: '发布', roles: ['owner', 'admin'] },
        { name: 'audit-manage', label: '审核', roles: ['owner', 'admin'] },
        { name: 'activity-manage', label: `活动 (${this.activityTotal})`, roles: ['owner', 'admin'] },
        { name: 'download-manage', label: '下载记录', roles: ['owner', 'admin'] },
        { name: 'stat-manage', label: '统计', roles: ['owner', 'admin'] },
        { name: 'settings', label: '设置', roles: ['owner', 'admin'] },
        { name: 'knowledge', label: '项目知识库', roles: ['owner', 'admin', 'member'] }
      ]
    },
    visibleManageTabs() {
      if (!this.resolvedCanAccessWorkbench) return []
      return this.manageTabs.filter(tab => tab.roles.includes(this.currentUserRole))
    },
    visibleManageTabNames() {
      return this.visibleManageTabs.map(tab => tab.name)
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
            this.pageLoadError = 'Missing project ID. Please open the workbench from My Projects or Project List.'
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
          this.pageLoadError = 'Missing project ID. Please open the workbench from My Projects or Project List.'
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
      if (this.accessResolved && !this.ensureVisibleTab(val, true)) return
      this.syncRouteTab(val)
    },
    currentUserRole() {
      this.ensureVisibleTab(this.activeTab, false)
    },
    visibleManageTabNames() {
      this.ensureVisibleTab(this.activeTab, false)
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
        activity: 'overview',
        tasks: 'task-manage',
        members: 'member-manage',
        files: 'file-manage',
        docs: 'doc-manage',
        repository: 'repo-workbench',
        repo: 'repo-workbench',
        audit: 'audit-manage',
        stat: 'stat-manage',
        stats: 'stat-manage',
        milestone: 'milestone-manage',
        sprint: 'sprint-manage',
        release: 'release-manage',
        download: 'download-manage'
      }
      const next = map[raw] || raw
      const allow = ['overview', 'task-manage', 'member-manage', 'file-manage', 'doc-manage', 'repo-workbench', 'milestone-manage', 'sprint-manage', 'release-manage', 'audit-manage', 'activity-manage', 'download-manage', 'stat-manage', 'settings', 'knowledge']
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
      if (!this.ensureVisibleTab(nextTab, true)) return
      this.activeTab = nextTab
    },
    openSaveAsTemplate() {
      this.saveTemplateDialogVisible = true
    },
    async handleTemplateSaved() {
      this.$message.success('模板保存成功')
      await this.refreshAll()
    },
    ensureVisibleTab(tab = this.activeTab, showFeedback = false) {
      if (!this.accessResolved) return true
      if (!this.canAccessWorkbench) return false
      const normalizedTab = this.normalizeManageTab(tab)
      if (this.visibleManageTabNames.includes(normalizedTab)) return true
      const fallbackTab = this.visibleManageTabNames[0] || 'overview'
      if (showFeedback) {
        this.$message.closeAll()
        this.$message.warning('当前角色无权查看该工作台页签，已切回可见页签')
      }
      if (this.activeTab !== fallbackTab) {
        this.activeTab = fallbackTab
      } else {
        this.syncRouteTab(fallbackTab)
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
        if (this.accessResolved && !this.canAccessWorkbench) {
          this.refreshSeed += 1
          return
        }
        this.ensureVisibleTab(this.activeTab, false)
        const summary = await fetchProjectManageSummary(this.projectId, { canSeeTaskCollaboration: this.canSeeTaskCollaboration })
        this.handleSummaryChange(summary)
        this.refreshSeed += 1
      } catch (error) {
        this.pageLoadError = error.response?.data?.message || error.message || 'Failed to initialize project manage page. Please refresh and retry.'
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
    },
    goToMyProjects() {
      this.$router.push('/myproject')
    }
  },
  mounted() {
    this.clientHydrated = true
    this.$nextTick(() => {
      this.ensureVisibleTab(this.activeTab, false)
    })
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


