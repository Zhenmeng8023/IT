<template>
  <el-card shadow="never" class="project-feature-entry-card">
    <div slot="header" class="entry-header">
      <div class="entry-title-wrap">
        <div class="entry-title">新增能力入口</div>
        <div class="entry-subtitle">按“详情页负责看，工作台负责管”的方式，把里程碑、Sprint、发布、下载记录、统计分析接进现有项目模块。</div>
      </div>
      <el-button size="small" type="primary" @click="$emit('open-manage', 'overview')">进入工作台</el-button>
    </div>

    <div class="entry-grid" v-loading="loading">
      <div class="entry-item">
        <div class="entry-item-head">
          <span>里程碑</span>
          <el-tag size="mini" type="info">{{ milestoneLabel }}</el-tag>
        </div>
        <div class="entry-value">{{ milestoneValue }}</div>
        <div class="entry-desc">{{ milestoneDesc }}</div>
        <div class="entry-actions">
          <el-button size="mini" type="text" @click="$emit('open-manage', 'milestone-manage')">查看里程碑</el-button>
        </div>
      </div>

      <div class="entry-item">
        <div class="entry-item-head">
          <span>当前 Sprint</span>
          <el-tag size="mini" type="warning">{{ sprintLabel }}</el-tag>
        </div>
        <div class="entry-value">{{ sprintValue }}</div>
        <div class="entry-desc">{{ sprintDesc }}</div>
        <div class="entry-actions">
          <el-button size="mini" type="text" @click="$emit('open-manage', 'sprint-manage')">查看 Sprint</el-button>
        </div>
      </div>

      <div class="entry-item">
        <div class="entry-item-head">
          <span>发布记录</span>
          <el-tag size="mini" type="success">{{ releaseLabel }}</el-tag>
        </div>
        <div class="entry-value">{{ releaseValue }}</div>
        <div class="entry-desc">{{ releaseDesc }}</div>
        <div class="entry-actions">
          <el-button size="mini" type="text" @click="$emit('open-manage', 'release-manage')">查看发布</el-button>
        </div>
      </div>

      <div class="entry-item">
        <div class="entry-item-head">
          <span>下载记录</span>
          <el-tag size="mini" type="primary">{{ downloadLabel }}</el-tag>
        </div>
        <div class="entry-value">{{ downloadValue }}</div>
        <div class="entry-desc">{{ downloadDesc }}</div>
        <div class="entry-actions">
          <el-button size="mini" type="text" @click="$emit('open-manage', 'download-manage')">查看下载记录</el-button>
        </div>
      </div>

      <div class="entry-item">
        <div class="entry-item-head">
          <span>统计分析</span>
          <el-tag size="mini" type="danger">{{ statLabel }}</el-tag>
        </div>
        <div class="entry-value">{{ statValue }}</div>
        <div class="entry-desc">{{ statDesc }}</div>
        <div class="entry-actions">
          <el-button size="mini" type="text" @click="$emit('open-manage', 'stat-manage')">查看统计分析</el-button>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script>
import { getProjectMilestoneOverview } from '@/api/projectMilestone'
import { getCurrentProjectSprint } from '@/api/projectSprint'
import { getLatestProjectRelease } from '@/api/projectRelease'
import { getProjectDownloadSummary } from '@/api/projectDownload'
import { getProjectStatOverview } from '@/api/projectStat'

function p(r) {
  if (r && r.data !== undefined) return r.data
  return r || {}
}

export default {
  name: 'ProjectFeatureEntryPanel',
  props: {
    projectId: {
      type: [String, Number],
      required: true
    }
  },
  data() {
    return {
      loading: false,
      milestoneOverview: {},
      sprintSummary: {},
      latestRelease: {},
      downloadSummary: {},
      statOverview: {}
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler() {
        if (this.projectId) {
          this.loadAll()
        }
      }
    }
  },
  computed: {
    milestoneValue() {
      return this.milestoneOverview.totalCount || 0
    },
    milestoneLabel() {
      return `进行中 ${this.milestoneOverview.activeCount || 0}`
    },
    milestoneDesc() {
      if (this.milestoneOverview.nextDueName) {
        return `最近截止：${this.milestoneOverview.nextDueName}`
      }
      return '详情页侧重看进度，工作台再做创建和状态流转。'
    },
    sprintValue() {
      return this.sprintSummary.name || '暂无'
    },
    sprintLabel() {
      return this.sprintSummary.status || 'planned'
    },
    sprintDesc() {
      if (this.sprintSummary.goal) return this.sprintSummary.goal
      return '详情页只看当前 Sprint 摘要，工作台管理完整迭代。'
    },
    releaseValue() {
      return this.latestRelease.version || '暂无'
    },
    releaseLabel() {
      return this.latestRelease.status || 'draft'
    },
    releaseDesc() {
      if (this.latestRelease.title) return this.latestRelease.title
      return '查看最新版本说明，下载入口也从这里延伸。'
    },
    downloadValue() {
      return this.downloadSummary.totalDownloads || 0
    },
    downloadLabel() {
      return `近7天 ${this.downloadSummary.last7DaysDownloads || 0}`
    },
    downloadDesc() {
      return '详情页发生下载动作，管理页查看下载明细。'
    },
    statValue() {
      return this.statOverview.viewCount || this.statOverview.totalViews || 0
    },
    statLabel() {
      return `下载 ${this.statOverview.downloadCount || this.statOverview.totalDownloads || 0}`
    },
    statDesc() {
      return '详情页只看轻量摘要，完整趋势和日报放到工作台。'
    }
  },
  methods: {
    async loadAll() {
      this.loading = true
      try {
        const [a, b, c, d, e] = await Promise.all([
          getProjectMilestoneOverview(this.projectId).catch(() => ({})),
          getCurrentProjectSprint(this.projectId).catch(() => ({})),
          getLatestProjectRelease(this.projectId).catch(() => ({})),
          getProjectDownloadSummary(this.projectId).catch(() => ({})),
          getProjectStatOverview(this.projectId).catch(() => ({}))
        ])
        this.milestoneOverview = p(a)
        this.sprintSummary = p(b)
        this.latestRelease = p(c)
        this.downloadSummary = p(d)
        this.statOverview = p(e)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.project-feature-entry-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.entry-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--it-text);
}

.entry-subtitle {
  margin-top: 8px;
  color: var(--it-text-subtle);
  font-size: 13px;
  line-height: 1.7;
  max-width: 760px;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.entry-item {
  border: 1px solid var(--it-border);
  border-radius: 16px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-accent) 5%, var(--it-surface-elevated)), var(--it-surface-solid));
  box-shadow: var(--it-shadow-soft);
  padding: 16px;
  min-height: 156px;
  display: flex;
  flex-direction: column;
}

.entry-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: var(--it-text);
  font-weight: 700;
}

.entry-value {
  margin-top: 16px;
  font-size: 24px;
  line-height: 1.3;
  font-weight: 700;
  color: var(--it-text);
  word-break: break-word;
}

.entry-desc {
  margin-top: 10px;
  color: var(--it-text-muted);
  font-size: 12px;
  line-height: 1.7;
  flex: 1;
}
</style>

