<template>
  <div class="project-main-tabs">
    <ProjectReadmeDocPanel
      :project-name="project.name"
      :can-manage-project="canManageProject"
      :loading="projectDocsLoading"
      :docs="projectDocs"
      :primary-doc="primaryProjectDoc"
      :active-doc="activeProjectDoc"
      :primary-doc-html="primaryProjectDocHtml"
      :active-doc-html="activeProjectDocHtml"
      :fallback-html="projectDocFallbackHtml"
      :fallback-has-content="projectDocFallbackHasContent"
      :fallback-lead-text="readmeLeadText"
      :drawer-visible.sync="projectDocDrawerVisibleProxy"
      @open-doc-manage="handleOpenDocManage"
      @select-doc="handleSelectProjectDoc"
      @refresh-docs="handleRefreshProjectDocs"
    />

    <el-card v-if="hasAiResult" shadow="never" class="section-card ai-result-card">
      <div slot="header" class="section-header section-header-flex">
        <span>AI 项目辅助结果</span>
        <div class="ai-result-header-actions">
          <el-tag size="mini" type="success" effect="plain">{{ lastAiModelLabel || currentAiModelLabel }}</el-tag>
          <el-button size="mini" type="text" @click="clearAiResult">清空</el-button>
        </div>
      </div>
      <el-tabs v-model="aiActiveTabProxy" class="ai-result-tabs">
        <el-tab-pane label="项目总结" name="summary">
          <div v-if="aiSummaryCard.overview" class="ai-struct-panel">
            <div class="ai-hero-overview">{{ aiSummaryCard.overview }}</div>
            <div class="ai-struct-grid">
              <div class="ai-struct-item">
                <div class="ai-struct-title">目标用户 / 场景</div>
                <ul class="ai-struct-list">
                  <li v-for="(item, index) in aiSummaryCard.scenarios" :key="'scene-' + index">{{ item }}</li>
                </ul>
              </div>
              <div class="ai-struct-item">
                <div class="ai-struct-title">核心功能</div>
                <ul class="ai-struct-list">
                  <li v-for="(item, index) in aiSummaryCard.features" :key="'feature-' + index">{{ item }}</li>
                </ul>
              </div>
              <div class="ai-struct-item">
                <div class="ai-struct-title">风险与待办</div>
                <ul class="ai-struct-list ai-struct-list-warn">
                  <li v-for="(item, index) in aiSummaryCard.risks" :key="'risk-' + index">{{ item }}</li>
                </ul>
              </div>
              <div class="ai-struct-item">
                <div class="ai-struct-title">下一步建议</div>
                <ul class="ai-struct-list">
                  <li v-for="(item, index) in aiSummaryCard.nextActions" :key="'next-' + index">{{ item }}</li>
                </ul>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无项目总结" :image-size="70" />
        </el-tab-pane>

        <el-tab-pane label="任务拆解" name="tasks">
          <div v-if="aiTaskCard.phases && aiTaskCard.phases.length" class="ai-struct-panel">
            <div v-for="(phase, phaseIndex) in aiTaskCard.phases" :key="'phase-' + phaseIndex" class="ai-phase-card">
              <div class="ai-phase-title">{{ phase.name }}</div>
              <div class="ai-task-list">
                <div v-for="(task, taskIndex) in phase.tasks" :key="'task-' + phaseIndex + '-' + taskIndex" class="ai-task-item">
                  <div class="ai-task-top">
                    <span class="ai-task-name">{{ task.title }}</span>
                    <el-tag size="mini" effect="plain">{{ task.priority || 'P2' }}</el-tag>
                  </div>
                  <div class="ai-task-meta">目标：{{ task.goal || '-' }}</div>
                  <div class="ai-task-meta">产出：{{ task.deliverable || '-' }}</div>
                  <div class="ai-task-meta">预计耗时：{{ task.estimate || '-' }}</div>
                </div>
              </div>
            </div>

            <div v-if="aiTaskCard.executionOrder && aiTaskCard.executionOrder.length" class="ai-struct-item">
              <div class="ai-struct-title">建议执行顺序</div>
              <ol class="ai-struct-list ai-ordered-list">
                <li v-for="(item, index) in aiTaskCard.executionOrder" :key="'order-' + index">{{ item }}</li>
              </ol>
            </div>

            <div v-if="aiTaskCard.risks && aiTaskCard.risks.length" class="ai-struct-item">
              <div class="ai-struct-title">依赖 / 阻塞点</div>
              <ul class="ai-struct-list ai-struct-list-warn">
                <li v-for="(item, index) in aiTaskCard.risks" :key="'task-risk-' + index">{{ item }}</li>
              </ul>
            </div>
          </div>
          <el-empty v-else description="暂无任务拆解" :image-size="70" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import ProjectReadmeDocPanel from './ProjectReadmeDocPanel.vue'

export default {
  name: 'ProjectDetailMainTabs',
  components: {
    ProjectReadmeDocPanel
  },
  props: {
    project: {
      type: Object,
      default: () => ({})
    },
    canManageProject: {
      type: Boolean,
      default: false
    },
    projectDocsLoading: {
      type: Boolean,
      default: false
    },
    projectDocs: {
      type: Array,
      default: () => []
    },
    primaryProjectDoc: {
      type: Object,
      default: null
    },
    activeProjectDoc: {
      type: Object,
      default: null
    },
    primaryProjectDocHtml: {
      type: String,
      default: ''
    },
    activeProjectDocHtml: {
      type: String,
      default: ''
    },
    projectDocFallbackHtml: {
      type: String,
      default: ''
    },
    projectDocFallbackHasContent: {
      type: Boolean,
      default: false
    },
    readmeLeadText: {
      type: String,
      default: ''
    },
    projectDocDrawerVisible: {
      type: Boolean,
      default: false
    },
    handleOpenDocManage: {
      type: Function,
      default: () => {}
    },
    handleSelectProjectDoc: {
      type: Function,
      default: () => {}
    },
    handleRefreshProjectDocs: {
      type: Function,
      default: () => {}
    },
    hasAiResult: {
      type: Boolean,
      default: false
    },
    lastAiModelLabel: {
      type: String,
      default: ''
    },
    currentAiModelLabel: {
      type: String,
      default: ''
    },
    clearAiResult: {
      type: Function,
      default: () => {}
    },
    aiActiveTab: {
      type: String,
      default: 'summary'
    },
    aiSummaryCard: {
      type: Object,
      default: () => ({ overview: '', scenarios: [], features: [], risks: [], nextActions: [] })
    },
    aiTaskCard: {
      type: Object,
      default: () => ({ phases: [], executionOrder: [], risks: [] })
    }
  },
  computed: {
    projectDocDrawerVisibleProxy: {
      get() {
        return this.projectDocDrawerVisible
      },
      set(value) {
        this.$emit('update:projectDocDrawerVisible', value)
      }
    },
    aiActiveTabProxy: {
      get() {
        return this.aiActiveTab
      },
      set(value) {
        this.$emit('update:aiActiveTab', value)
      }
    }
  }
}
</script>
