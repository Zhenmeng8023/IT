<template>
  <div class="project-audit-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">项目审核中心</h1>
        <p class="page-subtitle">围绕分支保护、合并请求、评审和检查统一收口项目主线治理。</p>
      </div>
      <div class="header-actions">
        <el-input
          v-model.trim="projectIdInput"
          size="small"
          placeholder="输入项目 ID"
          class="project-id-input"
          @keyup.enter.native="applyProjectId"
        />
        <el-button size="small" @click="applyProjectId">加载项目</el-button>
        <el-button v-if="projectId" size="small" type="primary" @click="goToProjectManage">返回项目工作台</el-button>
      </div>
    </div>

    <ProjectManageEntryHub
      current-key="audit"
      title="管理侧统一入口"
      subtitle="从项目管理、审核中心和下架管理之间切换时，尽量保持同一个管理上下文和状态口径。"
      :project-id="projectId"
      :entries="manageEntries"
      :status-cards="statusCards"
    />

    <ProjectAuditCenter
      v-if="projectId"
      :project-id="projectId"
      :can-manage-project="true"
      @summary-change="handleSummaryChange"
    />

    <el-empty v-else description="先从 query 或输入框指定 projectId，再进入审核中心" />
  </div>
</template>

<script>
import ProjectAuditCenter from '../../f_project/projectmanage/components/ProjectAuditCenter.vue'
import ProjectManageEntryHub from '../../f_project/projectmanage/components/ProjectManageEntryHub.vue'

export default {
  name: 'ProjectAuditPage',
  layout: 'manage',
  components: {
    ProjectAuditCenter,
    ProjectManageEntryHub
  },
  data() {
    const queryProjectId = this.$route && this.$route.query ? this.$route.query.projectId : ''
    return {
      projectId: queryProjectId ? Number(queryProjectId) : null,
      projectIdInput: queryProjectId ? String(queryProjectId) : '',
      auditSummary: {
        branchCount: 0,
        protectedBranchCount: 0,
        openMrCount: 0,
        failedMrCount: 0
      }
    }
  },
  computed: {
    returnManageTab() {
      const raw = String((this.$route.query && this.$route.query.fromTab) || 'audit-manage')
      const allow = ['overview', 'repo-workbench', 'audit-manage', 'milestone-manage', 'release-manage', 'stat-manage', 'task-manage', 'member-manage', 'file-manage', 'doc-manage', 'activity-manage', 'sprint-manage', 'download-manage', 'settings']
      return allow.includes(raw) ? raw : 'audit-manage'
    },
    manageEntries() {
      return [
        {
          key: 'manage',
          title: '项目管理',
          desc: '回到项目总览、任务、成员、仓库和设置的统一治理入口。',
          path: '/projectmanage',
          query: this.projectId ? { projectId: String(this.projectId), tab: this.returnManageTab } : undefined,
          requiresProjectId: true,
          disabled: !this.projectId,
          tone: 'blue'
        },
        {
          key: 'audit',
          title: '审核中心',
          desc: '查看分支保护、MR、评审和检查，统一决定主线准入。',
          path: '/projectaudit',
          query: this.projectId ? { projectId: String(this.projectId), fromTab: this.returnManageTab } : undefined,
          requiresProjectId: true,
          disabled: !this.projectId,
          tone: 'cyan'
        },
        {
          key: 'miss',
          title: '下架管理',
          desc: '进入项目下架与恢复治理入口，处理已下架项目的后续动作。',
          path: '/projectmiss',
          query: this.projectId ? { projectId: String(this.projectId), fromTab: this.returnManageTab } : undefined,
          tone: 'orange'
        }
      ]
    },
    statusCards() {
      return [
        {
          key: 'context',
          label: '项目上下文',
          value: this.projectId ? `#${this.projectId}` : '未绑定',
          desc: this.projectId ? '当前审核结果会和该项目管理页共用同一上下文。' : '先指定项目 ID，再完整查看主线治理链路。',
          tone: 'blue'
        },
        {
          key: 'branch',
          label: '分支保护',
          value: `${this.auditSummary.protectedBranchCount}/${this.auditSummary.branchCount || 0}`,
          desc: this.auditSummary.branchCount ? '受保护分支越清晰，主线准入越稳定。' : '等待加载分支状态。',
          tone: 'cyan'
        },
        {
          key: 'mr',
          label: '打开中的 MR',
          value: this.auditSummary.openMrCount || 0,
          desc: '这里聚合待评审、待检查和待合并的主线入口。',
          tone: 'purple'
        },
        {
          key: 'check',
          label: '失败检查',
          value: this.auditSummary.failedMrCount || 0,
          desc: this.auditSummary.failedMrCount ? '存在失败检查时，先修复再推进合并。' : '当前没有失败检查阻塞主线。',
          tone: this.auditSummary.failedMrCount ? 'danger' : 'orange'
        }
      ]
    }
  },
  watch: {
    '$route.query.projectId': {
      immediate: false,
      handler(value) {
        this.projectId = value ? Number(value) : null
        this.projectIdInput = value ? String(value) : ''
        if (!value) {
          this.auditSummary = {
            branchCount: 0,
            protectedBranchCount: 0,
            openMrCount: 0,
            failedMrCount: 0
          }
        }
      }
    }
  },
  methods: {
    applyProjectId() {
      const nextId = Number(this.projectIdInput)
      if (!nextId) {
        this.$message.warning('请输入有效的项目 ID')
        return
      }
      this.projectId = nextId
      this.$router.replace({
        path: this.$route.path,
        query: {
          ...this.$route.query,
          projectId: String(nextId)
        }
      })
    },
    handleSummaryChange(summary) {
      this.auditSummary = {
        branchCount: summary && summary.branchCount ? summary.branchCount : 0,
        protectedBranchCount: summary && summary.protectedBranchCount ? summary.protectedBranchCount : 0,
        openMrCount: summary && summary.openMrCount ? summary.openMrCount : 0,
        failedMrCount: summary && summary.failedMrCount ? summary.failedMrCount : 0
      }
    },
    goToProjectManage() {
      if (!this.projectId) return
      this.$router.push({
        path: '/projectmanage',
        query: {
          projectId: String(this.projectId),
          tab: this.returnManageTab
        }
      })
    }
  }
}
</script>

<style scoped>
.project-audit-page {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  color: #303133;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #909399;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.project-id-input {
  width: 180px;
}

@media (max-width: 768px) {
  .project-audit-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
  }

  .project-id-input {
    width: 100%;
  }
}
</style>
