<template>
  <el-card shadow="never" class="role-flow-card" :class="{ compact }">
    <div slot="header" class="role-flow-header">
      <div class="role-flow-heading">
        <div class="role-flow-title">{{ resolvedTitle }}</div>
        <div class="role-flow-subtitle">{{ resolvedSubtitle }}</div>
      </div>
      <div class="role-flow-legend">
        <span v-for="stage in stageHeaders" :key="stage" class="role-flow-legend-item">{{ stage }}</span>
      </div>
    </div>

    <div class="role-flow-stage-row">
      <div class="role-flow-stage-spacer"></div>
      <div class="role-flow-stage-grid">
        <div v-for="stage in stageHeaders" :key="'grid-' + stage" class="role-flow-stage">
          {{ stage }}
        </div>
      </div>
    </div>

    <div class="role-flow-lanes">
      <div
        v-for="lane in lanes"
        :key="lane.key"
        class="role-flow-lane"
        :class="'tone-' + lane.tone"
      >
        <div class="role-flow-role">
          <div class="role-flow-role-name">{{ lane.role }}</div>
          <div class="role-flow-role-desc">{{ lane.desc }}</div>
        </div>

        <div class="role-flow-track">
          <div
            v-for="(step, index) in lane.steps"
            :key="lane.key + '-' + step.key"
            class="role-flow-step"
            :class="[step.status, { 'is-focus': step.focus }]"
          >
            <div class="role-flow-step-order">{{ String(index + 1).padStart(2, '0') }}</div>
            <div class="role-flow-step-title">{{ step.title }}</div>
            <div class="role-flow-step-desc">{{ step.desc }}</div>
            <div v-if="step.meta" class="role-flow-step-meta">{{ step.meta }}</div>
          </div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script>
export default {
  name: 'ProjectRoleFlowLane',
  props: {
    mode: {
      type: String,
      default: 'audit'
    },
    compact: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: ''
    },
    subtitle: {
      type: String,
      default: ''
    },
    taskCount: {
      type: Number,
      default: 0
    },
    memberCount: {
      type: Number,
      default: 0
    },
    fileCount: {
      type: Number,
      default: 0
    },
    activityCount: {
      type: Number,
      default: 0
    },
    branchCount: {
      type: Number,
      default: 0
    },
    protectedBranchCount: {
      type: Number,
      default: 0
    },
    openMrCount: {
      type: Number,
      default: 0
    },
    failedMrCount: {
      type: Number,
      default: 0
    }
  },
  computed: {
    resolvedTitle() {
      if (this.title) return this.title
      return this.mode === 'overview' ? '不同角色活动流' : '不同角色活动泳道图'
    },
    resolvedSubtitle() {
      if (this.subtitle) return this.subtitle
      if (this.mode === 'overview') {
        return '从上传到交付，不同角色分别在工作区、提交、审核和发布阶段参与同一条主线。'
      }
      return '审核中心按角色把“开发 -> 评审 -> 合主线 -> 交付”串成可视化泳道，方便统一理解每个人该做什么。'
    },
    stageHeaders() {
      return ['工作区入口', '提交与分支', '评审与检查', '主线交付']
    },
    lanes() {
      const protectedText = this.branchCount
        ? `当前 ${this.protectedBranchCount}/${this.branchCount} 个分支已开启保护。`
        : '负责定义主线保护与交付规则。'
      const reviewerText = this.openMrCount
        ? `当前待处理 MR ${this.openMrCount} 个。`
        : '当前没有待处理 MR，主线入口比较空闲。'
      const developerText = this.fileCount
        ? `当前项目已沉淀 ${this.fileCount} 个文件资产。`
        : '开发成员先在工作区整理一次真实改动。'
      const viewerText = this.activityCount
        ? `最近记录到 ${this.activityCount} 条项目活动。`
        : '查看者主要关心阶段进度、发布和趋势结果。'

      return [
        {
          key: 'admin',
          role: '项目管理员',
          desc: protectedText,
          tone: 'blue',
          steps: [
            {
              key: 'protect',
              title: '配置分支保护',
              desc: '把 main 设为受保护分支，并决定是否允许直提。',
              meta: this.protectedBranchCount ? `受保护分支 ${this.protectedBranchCount} 个` : '建议优先保护主线',
              status: this.protectedBranchCount ? 'success' : 'warning',
              focus: this.mode === 'audit'
            },
            {
              key: 'strategy',
              title: '维护分支策略',
              desc: '约定 main / dev / feature 的职责，让改动先留在正确分支里。',
              meta: this.branchCount ? `当前分支 ${this.branchCount} 个` : '分支策略越清晰，主线越稳定',
              status: 'info',
              focus: false
            },
            {
              key: 'merge',
              title: '只合并通过的 MR',
              desc: '把评审通过且检查正常的分支并入主线，不绕过审核中心。',
              meta: this.openMrCount ? `当前打开中的 MR ${this.openMrCount} 个` : '当前没有待合并 MR',
              status: this.openMrCount ? 'warning' : 'success',
              focus: this.mode === 'audit'
            },
            {
              key: 'release',
              title: '绑定里程碑与发布',
              desc: '把阶段完成点和对外交付都绑定到 commit，形成闭环。',
              meta: '主线、里程碑和发布统一可追踪',
              status: 'info',
              focus: false
            }
          ]
        },
        {
          key: 'developer',
          role: '开发成员',
          desc: developerText,
          tone: 'cyan',
          steps: [
            {
              key: 'workspace',
              title: '上传进 Workspace',
              desc: '文件先进入工作区，不直接改掉正式版本。',
              meta: this.fileCount ? `当前文件 ${this.fileCount} 个` : '先把变更放进暂存区',
              status: 'success',
              focus: true
            },
            {
              key: 'commit',
              title: '写 Commit',
              desc: '用一次 Commit 固化一次真实改动，形成最小变更单位。',
              meta: '提交说明越清晰，后续 review 越轻松',
              status: 'info',
              focus: this.mode !== 'overview'
            },
            {
              key: 'branch',
              title: '保留在分支',
              desc: '在 dev / feature 分支继续迭代，不直接冲击主线。',
              meta: this.memberCount ? `当前协作成员 ${this.memberCount} 人` : '先在分支里完成自测',
              status: 'info',
              focus: false
            },
            {
              key: 'mr',
              title: '创建 MR 并跟进',
              desc: '把变更送去评审，按意见补充修改后再申请进入主线。',
              meta: this.taskCount ? `可与 ${this.taskCount} 条任务推进节奏联动` : 'MR 是进入主线的统一入口',
              status: 'warning',
              focus: this.mode === 'audit'
            }
          ]
        },
        {
          key: 'reviewer',
          role: '审核者 / Reviewer',
          desc: reviewerText,
          tone: 'purple',
          steps: [
            {
              key: 'review-change',
              title: '查看 MR 变更',
              desc: '围绕 Commit、分支差异和变更说明检查本次改动。',
              meta: this.openMrCount ? `待评审 ${this.openMrCount} 个` : '暂无待评审 MR',
              status: this.openMrCount ? 'warning' : 'info',
              focus: this.mode === 'audit'
            },
            {
              key: 'review-result',
              title: '提交评审意见',
              desc: '给出 approve / comment / reject，明确本次变更能否继续前进。',
              meta: '评审意见决定开发是否继续补改',
              status: 'info',
              focus: this.mode === 'audit'
            },
            {
              key: 'check-run',
              title: '登记 CheckRun',
              desc: '把 CI、质量、安全或发布检查结果写回审核中心。',
              meta: this.failedMrCount ? `失败检查 ${this.failedMrCount} 个` : '当前没有失败检查',
              status: this.failedMrCount ? 'danger' : 'success',
              focus: true
            },
            {
              key: 'gate',
              title: '决定是否准入主线',
              desc: '只有评审和检查都满足要求，才允许继续进入主线。',
              meta: this.failedMrCount ? '先修复失败项，再考虑合并' : '当前准入门槛清晰可控',
              status: this.failedMrCount ? 'danger' : 'success',
              focus: this.mode === 'audit'
            }
          ]
        },
        {
          key: 'viewer',
          role: '查看者 / 业务侧',
          desc: viewerText,
          tone: 'orange',
          steps: [
            {
              key: 'detail',
              title: '查看项目详情',
              desc: '通过项目详情页理解当前项目、文件和协作背景。',
              meta: '先看内容，再看治理进度',
              status: 'info',
              focus: this.mode === 'overview'
            },
            {
              key: 'milestone',
              title: '跟踪 Milestone / Sprint',
              desc: '关注阶段目标、时间周期和当前完成情况。',
              meta: '阶段点决定项目是否达到预期',
              status: 'info',
              focus: false
            },
            {
              key: 'release-view',
              title: '查看 Release',
              desc: '看哪些提交已经被整理成对外交付版本。',
              meta: 'Release 是真正对外可交付的版本',
              status: 'success',
              focus: false
            },
            {
              key: 'stat',
              title: '查看趋势与审计',
              desc: '从图表、表格和审计入口了解项目活跃度与主线健康度。',
              meta: this.activityCount ? `最近活动 ${this.activityCount} 条` : '适合复盘项目节奏',
              status: 'info',
              focus: this.mode !== 'audit'
            }
          ]
        }
      ]
    }
  }
}
</script>

<style scoped>
.role-flow-card {
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid #e5ecf6;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
}

.role-flow-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.role-flow-heading {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.role-flow-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.role-flow-subtitle {
  font-size: 13px;
  line-height: 1.8;
  color: #64748b;
  max-width: 760px;
}

.role-flow-legend {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.role-flow-legend-item {
  padding: 6px 10px;
  border-radius: 999px;
  background: #f8fafc;
  border: 1px solid #e5ecf6;
  color: #475569;
  font-size: 12px;
}

.role-flow-stage-row {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 12px;
  margin-bottom: 14px;
}

.role-flow-stage-spacer {
  min-height: 1px;
}

.role-flow-stage-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.role-flow-stage {
  padding: 10px 12px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px dashed #d9e3f0;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
}

.role-flow-lanes {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.role-flow-lane {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 12px;
}

.role-flow-role {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid #e5ecf6;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.role-flow-role-name {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.role-flow-role-desc {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.8;
  color: #64748b;
}

.role-flow-track {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.role-flow-step {
  position: relative;
  min-height: 132px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid #e5ecf6;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.role-flow-step:not(:last-child)::after {
  content: '';
  position: absolute;
  top: 34px;
  right: -12px;
  width: 12px;
  height: 2px;
  background: #d9e3f0;
}

.role-flow-step-order {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.role-flow-step-title {
  margin-top: 12px;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.role-flow-step-desc {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.75;
  color: #64748b;
}

.role-flow-step-meta {
  margin-top: 10px;
  font-size: 12px;
  color: #94a3b8;
}

.role-flow-step.info {
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.role-flow-step.success {
  background: linear-gradient(180deg, #effcf6 0%, #ffffff 100%);
  border-color: #bbf7d0;
}

.role-flow-step.warning {
  background: linear-gradient(180deg, #fffaf0 0%, #ffffff 100%);
  border-color: #fde68a;
}

.role-flow-step.danger {
  background: linear-gradient(180deg, #fff5f5 0%, #ffffff 100%);
  border-color: #fecaca;
}

.role-flow-step.is-focus {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(37, 99, 235, 0.08);
}

.tone-blue .role-flow-role {
  background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%);
  border-color: #bfdbfe;
}

.tone-cyan .role-flow-role {
  background: linear-gradient(180deg, #ecfeff 0%, #ffffff 100%);
  border-color: #bae6fd;
}

.tone-purple .role-flow-role {
  background: linear-gradient(180deg, #f5f3ff 0%, #ffffff 100%);
  border-color: #ddd6fe;
}

.tone-orange .role-flow-role {
  background: linear-gradient(180deg, #fff7ed 0%, #ffffff 100%);
  border-color: #fed7aa;
}

.compact .role-flow-title {
  font-size: 16px;
}

.compact .role-flow-subtitle {
  font-size: 12px;
}

@media (max-width: 1200px) {
  .role-flow-stage-row,
  .role-flow-lane {
    grid-template-columns: 1fr;
  }

  .role-flow-stage-spacer {
    display: none;
  }
}

@media (max-width: 768px) {
  .role-flow-header {
    flex-direction: column;
  }

  .role-flow-stage-grid,
  .role-flow-track {
    grid-template-columns: 1fr;
  }

  .role-flow-step:not(:last-child)::after {
    top: auto;
    right: auto;
    left: 50%;
    bottom: -12px;
    width: 2px;
    height: 12px;
    transform: translateX(-50%);
  }
}


.role-flow-card { border-color: var(--it-border); box-shadow: var(--it-shadow-soft); background: var(--it-panel-bg-strong); }
.role-flow-title,
.role-flow-node-title,
.role-flow-stage-label,
.role-flow-node-role { color: var(--it-text); }
.role-flow-subtitle,
.role-flow-node-desc,
.role-flow-node-meta,
.role-flow-legend-item,
.role-flow-stage,
.role-flow-stage-desc { color: var(--it-text-muted); }
.role-flow-legend-item,
.role-flow-stage,
.role-flow-node { background: var(--it-panel-bg); border-color: var(--it-border); }
.role-flow-stage { border-style: dashed; }
.role-flow-node:hover,
.role-flow-node.is-active { border-color: var(--it-border-strong); box-shadow: var(--it-shadow-hover); }
.role-flow-node.is-owner,
.role-flow-node.is-admin { background: linear-gradient(180deg, var(--it-tone-info-soft) 0%, var(--it-panel-bg-strong) 100%); }
.role-flow-node.is-manager { background: linear-gradient(180deg, var(--it-tone-purple-soft) 0%, var(--it-panel-bg-strong) 100%); }
.role-flow-node.is-member { background: linear-gradient(180deg, var(--it-tone-cyan-soft) 0%, var(--it-panel-bg-strong) 100%); }
.role-flow-node.is-viewer { background: linear-gradient(180deg, var(--it-surface-muted) 0%, var(--it-panel-bg-strong) 100%); }
.role-flow-node-badge { background: var(--it-tag-bg); color: var(--it-tag-text); }

</style>


<style scoped>
/* round11-roleflow-unify */
.role-flow-card {
  border-color: var(--it-border) !important;
  box-shadow: var(--it-shadow-soft) !important;
  background: var(--it-panel-bg-strong) !important;
}
.role-flow-title,
.role-flow-role-name,
.role-flow-step-title { color: var(--it-text) !important; }
.role-flow-subtitle,
.role-flow-role-desc,
.role-flow-step-desc,
.role-flow-step-meta,
.role-flow-stage,
.role-flow-legend-item { color: var(--it-text-muted) !important; }
.role-flow-legend-item,
.role-flow-stage,
.role-flow-step,
.role-flow-role {
  border-color: var(--it-border) !important;
  box-shadow: var(--it-shadow-soft) !important;
}
.role-flow-legend-item,
.role-flow-stage { background: var(--it-panel-bg) !important; }
.role-flow-stage { border-style: dashed !important; }
.role-flow-role,
.role-flow-step { background: var(--it-panel-bg) !important; }
.role-flow-step:not(:last-child)::after { background: var(--it-border-strong) !important; }
.role-flow-step-order {
  background: var(--it-tag-bg) !important;
  color: var(--it-tag-text) !important;
}
.role-flow-step.info { background: linear-gradient(180deg, var(--it-tone-info-soft) 0%, var(--it-panel-bg-strong) 100%) !important; }
.role-flow-step.success {
  background: linear-gradient(180deg, var(--it-success-soft) 0%, var(--it-panel-bg-strong) 100%) !important;
  border-color: color-mix(in srgb, var(--it-success) 28%, var(--it-border)) !important;
}
.role-flow-step.warning {
  background: linear-gradient(180deg, var(--it-warning-soft) 0%, var(--it-panel-bg-strong) 100%) !important;
  border-color: color-mix(in srgb, var(--it-warning) 28%, var(--it-border)) !important;
}
.role-flow-step.danger {
  background: linear-gradient(180deg, var(--it-danger-soft) 0%, var(--it-panel-bg-strong) 100%) !important;
  border-color: color-mix(in srgb, var(--it-danger) 28%, var(--it-border)) !important;
}
.role-flow-step.is-focus {
  box-shadow: 0 18px 36px color-mix(in srgb, var(--it-accent) 18%, transparent) !important;
}
.tone-blue .role-flow-role {
  background: linear-gradient(180deg, var(--it-tone-info-soft) 0%, var(--it-panel-bg-strong) 100%) !important;
  border-color: color-mix(in srgb, var(--it-accent) 22%, var(--it-border)) !important;
}
.tone-cyan .role-flow-role {
  background: linear-gradient(180deg, var(--it-tone-cyan-soft) 0%, var(--it-panel-bg-strong) 100%) !important;
  border-color: color-mix(in srgb, var(--it-tone-cyan-text) 18%, var(--it-border)) !important;
}
.tone-purple .role-flow-role {
  background: linear-gradient(180deg, var(--it-tone-purple-soft) 0%, var(--it-panel-bg-strong) 100%) !important;
  border-color: color-mix(in srgb, var(--it-tone-purple-text) 18%, var(--it-border)) !important;
}
.tone-orange .role-flow-role {
  background: linear-gradient(180deg, var(--it-warning-soft) 0%, var(--it-panel-bg-strong) 100%) !important;
  border-color: color-mix(in srgb, var(--it-warning) 22%, var(--it-border)) !important;
}
</style>

