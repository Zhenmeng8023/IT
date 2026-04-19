<template>
  <div>
    <div class="overview-hero">
      <div class="overview-hero-main">
        <div class="overview-kicker">Repository Flow</div>
        <div class="overview-hero-title">文件改动先进入工作区，再经过 Commit、MR 和发布进入主线</div>
        <div class="overview-hero-desc">
          现在这套项目管理页已经按“工作区 -> 提交 -> 审核 -> 交付”的主线组织。上传不会直接改正式版本，主线也不会被随手覆盖。
        </div>
        <div class="overview-flow-grid">
          <div
            v-for="step in repoFlowSteps"
            :key="step.key"
            class="overview-flow-item"
            @click="$emit('switch-tab', step.tab)"
          >
            <div class="overview-flow-order">{{ step.order }}</div>
            <div class="overview-flow-body">
              <div class="overview-flow-title">{{ step.title }}</div>
              <div class="overview-flow-desc">{{ step.desc }}</div>
            </div>
            <i class="el-icon-arrow-right overview-flow-arrow"></i>
          </div>
        </div>
      </div>

      <div class="overview-hero-side">
        <div class="hero-side-card primary">
          <div class="hero-side-label">推荐入口</div>
          <div class="hero-side-title">仓库工作台</div>
          <div class="hero-side-desc">上传文件、暂存删除、编写提交说明都集中在这里，适合日常开发改动。</div>
          <el-button type="primary" size="small" @click="$emit('switch-tab', 'repo-workbench')">打开仓库工作台</el-button>
        </div>

        <div class="hero-side-card">
          <div class="hero-side-label">主线保护</div>
          <div class="hero-side-title">审核中心</div>
          <div class="hero-side-desc">合并请求、评审和检查统一收到审核中心，主线保护更清晰。</div>
          <el-button plain size="small" @click="$emit('switch-tab', 'audit-manage')">打开审核中心</el-button>
        </div>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col v-for="card in overviewStats" :key="card.key" :xs="24" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-head">
            <div class="stat-label">{{ card.label }}</div>
            <span class="stat-chip">{{ card.chip }}</span>
          </div>
          <div class="stat-number">{{ card.value }}</div>
          <div class="stat-hint">{{ card.hint }}</div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never" class="feature-entry-card">
      <div slot="header" class="card-header">
        <span>主链路入口</span>
        <span class="feature-entry-tip">主界面优先围绕仓库工作区、Commit 历史、MR、里程碑、发布和统计分析组织。</span>
      </div>
      <div class="feature-entry-grid">
        <button
          v-for="card in primaryManageCards"
          :key="card.key"
          type="button"
          class="feature-entry-item"
          :class="{ 'is-primary': card.emphasis === 'primary', 'is-secondary': card.emphasis === 'secondary' }"
          @click="$emit('switch-tab', card.tab)"
        >
          <div v-if="card.badge" class="feature-entry-badge">{{ card.badge }}</div>
          <div class="feature-entry-title">{{ card.title }}</div>
          <div class="feature-entry-desc">{{ card.desc }}</div>
        </button>
      </div>
    </el-card>

    <el-card shadow="never" class="support-entry-card">
      <div slot="header" class="card-header">
        <span>辅助治理入口</span>
        <span class="feature-entry-tip">任务、成员、文件、文档、活动、下载和设置继续可用，但不再堆到主链路中心。</span>
      </div>
      <div class="support-entry-grid">
        <button
          v-for="card in supportManageCards"
          :key="card.key"
          type="button"
          class="support-entry-item"
          @click="$emit('switch-tab', card.tab)"
        >
          <div class="support-entry-title">{{ card.title }}</div>
          <div class="support-entry-desc">{{ card.desc }}</div>
        </button>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never">
          <div slot="header" class="card-header">
            <span>最近活动</span>
            <el-button type="text" @click="loadOverviewData">刷新</el-button>
          </div>
          <div v-if="recentActivities.length > 0" class="activity-list">
            <div
              v-for="item in recentActivities"
              :key="item.id"
              class="activity-item is-clickable"
              @click="$emit('open-activity', item)"
            >
              <div class="activity-left">
                <el-avatar :size="32" :src="getActivityAvatar(item)"></el-avatar>
              </div>
              <div class="activity-right">
                <div class="activity-title">{{ item.actionLabel || item.title }}</div>
                <div class="activity-desc">{{ item.content || item.description }}</div>
              </div>
              <div class="activity-time">{{ formatTime(item.createdAt || item.time) }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无最近活动"></el-empty>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="side-card">
          <div slot="header" class="card-header"><span>项目信息</span></div>
          <div class="info-list">
            <div class="info-item"><span>状态</span><el-tag size="mini" :type="getProjectStatusType(project.status)">{{ project.statusText || '-' }}</el-tag></div>
            <div class="info-item"><span>可见性</span><span>{{ project.visibility || '-' }}</span></div>
            <div class="info-item"><span>分类</span><span>{{ project.category || '-' }}</span></div>
            <div class="info-item"><span>更新时间</span><span>{{ formatTime(project.updateTime) }}</span></div>
            <div class="info-item"><span>标签</span><span>{{ (project.tags || []).join('、') || '-' }}</span></div>
          </div>
        </el-card>

        <el-card shadow="never" class="side-card">
          <div slot="header" class="card-header"><span>贡献者</span></div>
          <div v-if="contributors.length > 0" class="contributor-list">
            <div v-for="member in contributors" :key="member.id" class="contributor-item">
              <el-avatar :size="30" :src="member.avatar"></el-avatar>
              <div class="contributor-text">
                <div class="contributor-name">{{ member.name }}</div>
                <div class="contributor-role">{{ getMemberRoleText(member.role) }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无成员"></el-empty>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getProjectActivities } from '@/api/projectActivity'
import { listProjectFiles, listProjectTasks } from '@/api/project'
import {
  buildOwnerRow,
  formatTime,
  getMemberRoleText,
  getProjectStatusType,
  normalizeActivityPayload
} from '../services/projectManageShared'
import { pickAvatarUrl } from '@/utils/avatar'

export default {
  name: 'ProjectManageOverviewTab',
  props: {
    projectId: {
      type: [String, Number],
      default: null
    },
    project: {
      type: Object,
      default: () => ({})
    },
    members: {
      type: Array,
      default: () => []
    },
    canSeeTaskCollaboration: {
      type: Boolean,
      default: false
    },
    refreshSeed: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      taskCount: 0,
      fileCount: 0,
      activityTotal: 0,
      recentActivities: []
    }
  },
  computed: {
    contributors() {
      const owner = buildOwnerRow(this.project)
      const memberRows = (this.members || []).filter(item => item && !item.isOwner)
      return [owner, ...memberRows].filter(Boolean).slice(0, 8)
    },
    overviewStats() {
      return [
        { key: 'tasks', label: '项目任务', value: this.taskCount, chip: '协作面', hint: '看当前任务体量与推进节奏。' },
        { key: 'members', label: '项目成员', value: this.members.length, chip: '团队', hint: '成员、权限和协作分工都在这里收口。' },
        { key: 'files', label: '项目文件', value: this.fileCount, chip: '资产', hint: '旧上传入口已经改成先进入工作区。' },
        { key: 'activities', label: '项目活动', value: this.activityTotal, chip: '动态', hint: '用于追踪最近提交、协作和操作痕迹。' }
      ]
    },
    primaryManageCards() {
      return [
        {
          key: 'workspace',
          title: '仓库工作区 / Commit 历史',
          desc: '上传进入工作区、查看差异、提交 commit、回退和 compare 都从这里进入。',
          tab: 'repo-workbench',
          badge: '推荐',
          emphasis: 'primary'
        },
        {
          key: 'audit',
          title: 'MR / 审核中心',
          desc: '分支保护、合并请求、review 和检查统一收口，保证主线不被简单覆盖。',
          tab: 'audit-manage',
          badge: '保护主线',
          emphasis: 'secondary'
        },
        {
          key: 'milestone',
          title: '里程碑',
          desc: '把阶段目标和 commit 节点对齐，方便按阶段回看交付进展。',
          tab: 'milestone-manage'
        },
        {
          key: 'release',
          title: '发布 / 交付',
          desc: '把 release、里程碑和 commit 绑到同一条可追溯链路上。',
          tab: 'release-manage'
        },
        {
          key: 'stat',
          title: '统计分析',
          desc: '从提交、合并、发布和下载等指标看项目主线运行状态。',
          tab: 'stat-manage'
        }
      ]
    },
    supportManageCards() {
      const cards = [
        { key: 'task', title: '任务协作', desc: '任务推进、协作抽屉和负责人更新。', tab: 'task-manage', hidden: !this.canSeeTaskCollaboration },
        { key: 'member', title: '成员管理', desc: '成员角色、邀请和加入申请。', tab: 'member-manage' },
        { key: 'file', title: '文件管理', desc: '项目文件浏览、主文件和版本入口。', tab: 'file-manage' },
        { key: 'doc', title: '项目文档', desc: '项目文档、历史和主入口文档。', tab: 'doc-manage' },
        { key: 'activity', title: '活动流', desc: '活动时间线和操作回看。', tab: 'activity-manage' },
        { key: 'sprint', title: 'Sprint', desc: '迭代目标和时间窗口管理。', tab: 'sprint-manage' },
        { key: 'download', title: '下载记录', desc: '下载摘要和明细追踪。', tab: 'download-manage' },
        { key: 'settings', title: '项目设置', desc: '基础设置、模板和治理配置。', tab: 'settings' }
      ]
      return cards.filter(item => !item.hidden)
    },
    repoFlowSteps() {
      return [
        { key: 'workspace', order: '01', title: '上传进入工作区', desc: '先暂存，不直接改正式版本。', tab: 'repo-workbench' },
        { key: 'commit', order: '02', title: '提交 Commit', desc: '把一次真实改动固定为最小变更单位。', tab: 'repo-workbench' },
        { key: 'audit', order: '03', title: '创建 MR 并审核', desc: '评审、检查和主线保护都放在审核中心。', tab: 'audit-manage' },
        { key: 'release', order: '04', title: '里程碑 / 发布', desc: '把阶段完成点和交付版本绑定到提交。', tab: 'release-manage' }
      ]
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler() {
        this.loadOverviewData()
      }
    },
    refreshSeed() {
      this.loadOverviewData()
    },
    canSeeTaskCollaboration() {
      this.loadOverviewData()
    }
  },
  methods: {
    getActivityAvatar(item) {
      return pickAvatarUrl(item.operatorAvatarUrl, item.operatorAvatar, item.avatarUrl, item.avatar)
    },
    formatTime,
    getMemberRoleText,
    getProjectStatusType,
    async loadOverviewData() {
      if (!this.projectId) return
      const requests = [
        this.canSeeTaskCollaboration ? listProjectTasks(this.projectId) : Promise.resolve({ data: [] }),
        listProjectFiles(this.projectId),
        getProjectActivities(this.projectId, { page: 1, size: 8 })
      ]
      const [taskResult, fileResult, activityResult] = await Promise.allSettled(requests)

      this.taskCount = taskResult.status === 'fulfilled' && Array.isArray(taskResult.value.data) ? taskResult.value.data.length : 0
      this.fileCount = fileResult.status === 'fulfilled' && Array.isArray(fileResult.value.data) ? fileResult.value.data.length : 0

      if (activityResult.status === 'fulfilled') {
        const pageData = normalizeActivityPayload(activityResult.value, 8)
        this.recentActivities = pageData.list || []
        this.activityTotal = pageData.total || 0
      } else {
        this.recentActivities = []
        this.activityTotal = 0
      }

      this.$emit('summary-change', {
        taskCount: this.taskCount,
        fileCount: this.fileCount,
        activityTotal: this.activityTotal
      })
    }
  }
}
</script>

<style scoped>
.overview-hero { display: grid; grid-template-columns: minmax(0, 1.7fr) minmax(280px, 0.9fr); gap: 16px; padding: 22px; border-radius: 24px; background: linear-gradient(135deg, color-mix(in srgb, var(--it-accent) 84%, #122033) 0%, color-mix(in srgb, var(--it-accent) 72%, var(--it-success) 28%) 52%, color-mix(in srgb, var(--it-accent) 56%, white) 100%); color: #fff; box-shadow: var(--it-shadow-strong); }
.overview-hero-main { min-width: 0; }
.overview-kicker { display: inline-flex; align-items: center; padding: 6px 10px; border-radius: 999px; background: rgba(255, 255, 255, 0.14); font-size: 12px; letter-spacing: .08em; text-transform: uppercase; }
.overview-hero-title { margin-top: 14px; font-size: 30px; line-height: 1.3; font-weight: 700; }
.overview-hero-desc { margin-top: 10px; max-width: 760px; color: rgba(255, 255, 255, 0.82); line-height: 1.85; font-size: 14px; }
.overview-flow-grid { margin-top: 18px; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.overview-flow-item { display: flex; align-items: center; gap: 12px; padding: 14px 16px; border-radius: 18px; background: rgba(255, 255, 255, 0.12); border: 1px solid rgba(255, 255, 255, 0.16); cursor: pointer; transition: transform .2s ease, background .2s ease, border-color .2s ease; }
.overview-flow-item:hover { transform: translateY(-2px); background: rgba(255, 255, 255, 0.18); border-color: rgba(255, 255, 255, 0.28); }
.overview-flow-order { flex: 0 0 42px; width: 42px; height: 42px; border-radius: 14px; display: flex; align-items: center; justify-content: center; background: rgba(255, 255, 255, 0.16); font-size: 13px; font-weight: 700; }
.overview-flow-body { min-width: 0; flex: 1; }
.overview-flow-title { font-size: 15px; font-weight: 700; }
.overview-flow-desc { margin-top: 4px; font-size: 12px; line-height: 1.6; color: rgba(255, 255, 255, 0.76); }
.overview-flow-arrow { font-size: 14px; color: rgba(255, 255, 255, 0.72); }
.overview-hero-side { display: flex; flex-direction: column; gap: 12px; }
.hero-side-card { padding: 18px; border-radius: 20px; background: var(--it-surface); color: var(--it-text); border: 1px solid var(--it-border); box-shadow: var(--it-shadow); }
.hero-side-label { font-size: 12px; color: var(--it-text-muted); }
.hero-side-title { margin-top: 8px; font-size: 20px; font-weight: 700; color: var(--it-text); }
.hero-side-desc { margin: 8px 0 16px; font-size: 13px; line-height: 1.8; color: var(--it-text-muted); }
.stats-row { margin: 16px 0; }
.stat-card { position: relative; overflow: hidden; background: var(--it-surface); border: 1px solid var(--it-border); border-radius: 18px; padding: 18px 20px; box-shadow: var(--it-shadow); }
.stat-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 4px; background: var(--it-primary-gradient); }
.stat-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.stat-chip { padding: 4px 10px; border-radius: 999px; background: var(--it-accent-soft); color: var(--it-accent); font-size: 12px; }
.stat-number { font-size: 28px; font-weight: 700; color: var(--it-text); }
.stat-label { color: var(--it-text-muted); font-size: 13px; }
.stat-hint { margin-top: 8px; color: var(--it-text-subtle); font-size: 12px; line-height: 1.7; }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.feature-entry-card, .support-entry-card { margin-bottom: 16px; border-radius: 22px; overflow: hidden; }
.feature-entry-tip { color: var(--it-text-subtle); font-size: 12px; }
.feature-entry-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; }
.feature-entry-item { position: relative; appearance: none; width: 100%; text-align: left; padding: 18px 16px 16px; border-radius: 18px; border: 1px solid var(--it-border); background: var(--it-surface); cursor: pointer; transition: all .2s ease; }
.feature-entry-item:hover { border-color: var(--it-border-strong); transform: translateY(-2px); box-shadow: var(--it-shadow-soft); }
.feature-entry-item.is-primary { border-color: var(--it-accent); }
.feature-entry-item.is-secondary { background: var(--it-surface-muted); }
.feature-entry-badge { display: inline-flex; margin-bottom: 10px; padding: 4px 10px; border-radius: 999px; background: var(--it-accent-soft); color: var(--it-accent); font-size: 12px; font-weight: 600; }
.feature-entry-title { font-size: 16px; font-weight: 700; color: var(--it-text); }
.feature-entry-desc { margin-top: 10px; font-size: 12px; line-height: 1.7; color: var(--it-text-muted); }
.support-entry-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.support-entry-item { appearance: none; width: 100%; text-align: left; padding: 15px 16px; border-radius: 16px; border: 1px solid var(--it-border); background: var(--it-surface); cursor: pointer; transition: border-color .2s ease, transform .2s ease, box-shadow .2s ease; }
.support-entry-item:hover { border-color: var(--it-border-strong); transform: translateY(-1px); box-shadow: var(--it-shadow-soft); }
.support-entry-title { font-size: 14px; font-weight: 700; color: #1f2937; }
.support-entry-desc { margin-top: 8px; font-size: 12px; line-height: 1.7; color: var(--it-text-muted); }
.side-card { margin-bottom: 16px; }
.info-list { display: flex; flex-direction: column; gap: 12px; }
.info-item { display: flex; justify-content: space-between; gap: 12px; color: var(--it-text-muted); }
.contributor-list { display: flex; flex-direction: column; gap: 12px; }
.contributor-item { display: flex; align-items: center; gap: 12px; }
.contributor-text { display: flex; flex-direction: column; }
.contributor-name { color: var(--it-text); font-weight: 600; }
.contributor-role { color: var(--it-text-subtle); font-size: 12px; }
.activity-list { display: flex; flex-direction: column; gap: 14px; }
.activity-item { display: grid; grid-template-columns: 40px 1fr auto; gap: 12px; align-items: center; padding: 12px 0; border-bottom: 1px solid var(--it-border); }
.activity-item:last-child { border-bottom: none; }
.activity-item.is-clickable { cursor: pointer; transition: all .25s ease; }
.activity-item.is-clickable:hover { background: var(--it-accent-soft); border-radius: 10px; }
.activity-title { color: var(--it-text); font-weight: 600; }
.activity-desc, .activity-time { color: var(--it-text-subtle); font-size: 12px; }
.activity-time { white-space: nowrap; }

@media (max-width: 768px) {
  .overview-hero { grid-template-columns: 1fr; padding: 18px; }
  .overview-hero-title { font-size: 24px; }
  .overview-flow-grid { grid-template-columns: 1fr; }
  .feature-entry-grid, .support-entry-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .activity-item { grid-template-columns: 40px 1fr; }
  .activity-time { grid-column: 2 / 3; }
}

@media (max-width: 480px) {
  .feature-entry-grid, .support-entry-grid { grid-template-columns: 1fr; }
  .overview-flow-item { align-items: flex-start; }
  .overview-flow-arrow { display: none; }
}


.overview-hero,
.feature-entry-card,
.support-entry-card,
.side-card,
.stat-card {
  border-color: var(--it-border);
  box-shadow: var(--it-shadow-soft);
}
.overview-hero {
  background:
    radial-gradient(circle at top right, var(--it-glow-primary) 0%, transparent 28%),
    radial-gradient(circle at bottom left, var(--it-glow-secondary) 0%, transparent 32%),
    var(--it-panel-bg-strong);
}
.overview-hero-title,
.hero-side-title,
.stat-number,
.feature-entry-title,
.support-entry-title,
.contributor-name,
.activity-title { color: var(--it-text); }
.overview-hero-desc,
.hero-side-desc,
.stat-label,
.stat-hint,
.feature-entry-desc,
.support-entry-desc,
.info-item,
.contributor-role,
.activity-desc,
.activity-time,
.feature-entry-tip { color: var(--it-text-muted); }
.stat-card,
.feature-entry-item,
.support-entry-item { background: var(--it-panel-bg); }
.stat-chip,
.feature-entry-badge { background: var(--it-tag-bg); color: var(--it-tag-text); }
.feature-entry-item:hover,
.support-entry-item:hover,
.activity-item.is-clickable:hover { border-color: var(--it-border-strong); box-shadow: var(--it-shadow-hover); }
.feature-entry-item:hover { background: linear-gradient(180deg, var(--it-tone-info-soft) 0%, var(--it-panel-bg-strong) 100%); }
.feature-entry-item.is-secondary { background: var(--it-soft-gradient); }
.activity-item { border-bottom-color: var(--it-border); }



.overview-hero {
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-accent) 84%, #122033) 0%, color-mix(in srgb, var(--it-accent) 72%, var(--it-success) 28%) 52%, color-mix(in srgb, var(--it-accent) 56%, white) 100%);
  box-shadow: var(--it-shadow-strong);
}
.hero-side-label,
.stat-label,
.stat-hint,
.feature-entry-tip,
.feature-entry-desc,
.support-entry-desc,
.info-item,
.contributor-role,
.activity-desc,
.activity-time { color: var(--it-text-muted); }
.hero-side-title,
.stat-number,
.feature-entry-title,
.support-entry-title,
.contributor-name,
.activity-title { color: var(--it-text); }
.feature-entry-item:hover,
.support-entry-item:hover {
  border-color: var(--it-border-strong);
  box-shadow: var(--it-shadow-soft);
}
.activity-item { border-bottom-color: var(--it-border); }

</style>
