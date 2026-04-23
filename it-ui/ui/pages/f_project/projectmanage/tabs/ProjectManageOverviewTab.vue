<template>
  <div>
    <div class="overview-summary">
      <div>
        <div class="overview-kicker">项目操作中心</div>
        <div class="overview-title">{{ project.title || '当前项目' }}</div>
        <div class="overview-desc">
          工作台按当前项目组织任务、文件、文档、仓库、审核、统计和设置；具体操作通过上方页签进入。
        </div>
      </div>
      <div class="overview-meta">
        <div class="overview-meta-item">
          <span>项目状态</span>
          <el-tag size="mini" :type="getProjectStatusType(project.status)">{{ project.statusText || '-' }}</el-tag>
        </div>
        <div class="overview-meta-item">
          <span>更新时间</span>
          <strong>{{ formatTime(project.updateTime) || '-' }}</strong>
        </div>
        <div class="overview-meta-item">
          <span>项目成员</span>
          <strong>{{ members.length }}</strong>
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
              class="activity-item"
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
        { key: 'files', label: '项目文件', value: this.fileCount, chip: '资产', hint: '项目文件和版本资料集中在文件页签查看。' },
        { key: 'activities', label: '项目活动', value: this.activityTotal, chip: '动态', hint: '用于追踪最近提交、协作和操作痕迹。' }
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
.overview-summary {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 24px;
  border: 1px solid var(--it-border);
  border-radius: 18px;
  background: var(--it-panel-bg-strong);
  box-shadow: var(--it-shadow-soft);
}
.overview-kicker {
  color: var(--it-text-muted);
  font-size: 12px;
}
.overview-title {
  margin-top: 8px;
  color: var(--it-text);
  font-size: 24px;
  font-weight: 700;
}
.overview-desc {
  margin-top: 8px;
  max-width: 720px;
  color: var(--it-text-muted);
  font-size: 13px;
  line-height: 1.8;
}
.overview-meta {
  min-width: 240px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.overview-meta-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--it-text-muted);
  font-size: 13px;
}
.overview-meta-item strong {
  color: var(--it-text);
}
.stats-row { margin: 16px 0; }
.stat-card {
  position: relative;
  overflow: hidden;
  background: var(--it-panel-bg);
  border: 1px solid var(--it-border);
  border-radius: 14px;
  padding: 18px 20px;
  box-shadow: var(--it-shadow-soft);
}
.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--it-primary-gradient);
}
.stat-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.stat-chip { padding: 4px 10px; border-radius: 999px; background: var(--it-tag-bg); color: var(--it-tag-text); font-size: 12px; }
.stat-number { font-size: 28px; font-weight: 700; color: var(--it-text); }
.stat-label { color: var(--it-text-muted); font-size: 13px; }
.stat-hint { margin-top: 8px; color: var(--it-text-muted); font-size: 12px; line-height: 1.7; }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.side-card { margin-bottom: 16px; }
.info-list { display: flex; flex-direction: column; gap: 12px; }
.info-item { display: flex; justify-content: space-between; gap: 12px; color: var(--it-text-muted); }
.contributor-list { display: flex; flex-direction: column; gap: 12px; }
.contributor-item { display: flex; align-items: center; gap: 12px; }
.contributor-text { display: flex; flex-direction: column; }
.contributor-name { color: var(--it-text); font-weight: 600; }
.contributor-role { color: var(--it-text-muted); font-size: 12px; }
.activity-list { display: flex; flex-direction: column; gap: 14px; }
.activity-item {
  display: grid;
  grid-template-columns: 40px 1fr auto;
  gap: 12px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--it-border);
}
.activity-item:last-child { border-bottom: none; }
.activity-title { color: var(--it-text); font-weight: 600; }
.activity-desc, .activity-time { color: var(--it-text-muted); font-size: 12px; }
.activity-time { white-space: nowrap; }

@media (max-width: 768px) {
  .overview-summary { flex-direction: column; padding: 18px; }
  .overview-meta { min-width: 0; }
  .activity-item { grid-template-columns: 40px 1fr; }
  .activity-time { grid-column: 2 / 3; }
}
</style>
