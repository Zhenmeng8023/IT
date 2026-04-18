<template>
  <div class="content-side">
    <ProjectActivityTimeline
      :key="activityTimelineKey"
      :project-id="projectId"
      :compact="true"
      :show-filters="false"
      :show-view-all="true"
      :max-count="5"
    />

    <el-card shadow="never" class="section-card side-card">
      <div slot="header" class="section-header">
        <span>AI 项目助手</span>
      </div>
      <div class="ai-assistant-box">
        <div class="ai-model-field">
          <div class="ai-model-label">当前模型</div>
          <el-select
            v-model="selectedAiModelIdProxy"
            size="small"
            clearable
            filterable
            :loading="aiModelsLoading"
            placeholder="请选择 AI 模型"
            style="width: 100%"
            @change="handleAiModelChange"
          >
            <el-option
              v-for="item in aiModels"
              :key="item.id"
              :label="formatAiModelOption(item)"
              :value="item.id"
            />
          </el-select>
        </div>
        <div class="ai-model-tag-row">
          <el-tag size="mini" type="success" effect="plain">{{ currentAiModelLabel }}</el-tag>
          <el-tag v-if="currentAiProviderLabel" size="mini" type="info" effect="plain">{{ currentAiProviderLabel }}</el-tag>
        </div>
        <div class="ai-helper-actions">
          <el-button size="small" type="success" plain :loading="aiSummaryLoading" @click="handleAiSummarizeProject">生成项目总结</el-button>
          <el-button size="small" type="warning" plain :loading="aiTaskLoading" @click="handleAiSplitProjectTasks">生成任务拆解</el-button>
          <el-button size="small" plain :loading="aiRiskLoading" @click="handleAiIdentifyRisks">识别风险</el-button>
          <el-button size="small" plain :loading="aiNextStepsLoading" @click="handleAiNextSteps">下一步建议</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="section-card side-card">
      <div slot="header" class="section-header">
        <span>项目信息</span>
      </div>
      <div class="info-list">
        <div class="info-item"><span class="info-label">项目 ID</span><span class="info-value">{{ project.id || '-' }}</span></div>
        <div class="info-item"><span class="info-label">状态</span><span class="info-value">{{ statusLabel }}</span></div>
        <div class="info-item"><span class="info-label">分类</span><span class="info-value">{{ categoryLabel }}</span></div>
        <div class="info-item"><span class="info-label">最后更新</span><span class="info-value">{{ formatTime(project.updatedAt) }}</span></div>
        <div class="info-item"><span class="info-label">可见性</span><span class="info-value">{{ visibilityLabel }}</span></div>
      </div>
    </el-card>

    <el-card v-if="pageAccessResolved && canSeeTaskCollaboration" shadow="never" class="section-card side-card">
      <div slot="header" class="section-header section-header-flex">
        <span>我的待办</span>
        <div class="side-task-header-actions">
          <el-tag size="mini" type="warning" effect="plain">{{ myTodoTasks.length }}</el-tag>
          <el-button size="mini" type="text" :loading="myTasksLoading" @click="fetchMyTasks">刷新</el-button>
        </div>
      </div>
      <div v-loading="myTasksLoading" class="side-task-card-body">
        <div class="side-task-summary">
          <div class="side-task-summary-value">{{ myTodoPendingCount }}</div>
          <div class="side-task-summary-label">未完成</div>
        </div>
        <div v-if="myTodoTasks.length" class="side-task-list">
          <div v-for="task in myTodoTasks" :key="'my-' + task.id" class="side-task-item" :class="{ 'is-overdue': isTaskOverdue(task) }">
            <div class="side-task-main">
              <div class="side-task-title">{{ task.title || '未命名任务' }}</div>
              <div class="side-task-meta">
                <el-tag size="mini" effect="plain" :type="getTaskPriorityType(task.priority)">{{ getTaskPriorityText(task.priority) }}</el-tag>
                <span>{{ getTaskDueLabel(task) }}</span>
              </div>
            </div>
            <div class="side-task-actions">
              <el-button size="mini" type="success" plain :loading="taskQuickUpdatingId === task.id" @click="handleQuickTaskStatusChange(task, 'done')">完成</el-button>
              <el-button size="mini" type="text" class="side-task-link" @click="openTaskCollabDrawer(task, 'comment')">协作详情</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无我的待办" :image-size="60" />
      </div>
    </el-card>

    <el-card v-if="pageAccessResolved && canSeeTaskCollaboration" shadow="never" class="section-card side-card">
      <div slot="header" class="section-header section-header-flex">
        <span>今日到期提醒</span>
        <div class="side-task-header-actions">
          <el-tag size="mini" type="danger" effect="plain">{{ todayDueTasks.length }}</el-tag>
          <el-button size="mini" type="text" :loading="taskBoardLoading" @click="fetchProjectTasks">刷新</el-button>
        </div>
      </div>
      <div v-loading="taskBoardLoading" class="side-task-card-body">
        <div class="side-task-summary danger">
          <div class="side-task-summary-value">{{ todayDueTasks.length }}</div>
          <div class="side-task-summary-label">今日到期</div>
        </div>
        <div v-if="todayDueTasks.length" class="side-task-list">
          <div v-for="task in todayDueTasks" :key="'due-' + task.id" class="side-task-item side-task-item-due">
            <div class="side-task-main">
              <div class="side-task-title">{{ task.title || '未命名任务' }}</div>
              <div class="side-task-meta">
                <span>{{ getTaskAssigneeName(task) }}</span>
                <span>{{ formatTaskDueClock(task.dueDate) }}</span>
              </div>
            </div>
            <div class="side-task-actions">
              <el-select
                :value="task.status"
                size="mini"
                placeholder="状态"
                class="side-task-status-select"
                :disabled="taskQuickUpdatingId === task.id"
                @change="handleQuickTaskStatusChange(task, $event)"
              >
                <el-option label="待处理" value="todo" />
                <el-option label="进行中" value="in_progress" />
                <el-option label="已完成" value="done" />
              </el-select>
              <el-button size="mini" type="text" class="side-task-link" @click="openTaskCollabDrawer(task, 'log')">协作详情</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="今天暂无到期任务" :image-size="60" />
      </div>
    </el-card>

    <el-card shadow="never" class="section-card side-card">
      <div slot="header" class="section-header">
        <span>贡献者</span>
      </div>
      <div v-if="contributors.length" class="contributors-list">
        <div v-for="contributor in contributors" :key="contributor.id || contributor.userId" class="contributor-item">
          <el-avatar :size="34" :src="contributor.avatar || ''">{{ contributor.displayName.slice(0, 1) }}</el-avatar>
          <div class="contributor-text">
            <div class="contributor-name">{{ contributor.displayName }}</div>
            <div class="contributor-role">{{ roleLabel(contributor.role) }}</div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无贡献者数据" :image-size="70" />
    </el-card>

    <el-card shadow="never" class="section-card side-card">
      <div slot="header" class="section-header">
        <span>相关项目</span>
      </div>
      <div v-if="relatedProjects.length" class="related-list">
        <div v-for="item in relatedProjects" :key="item.id" class="related-item" @click="goToDetail(item.id)">
          <div class="related-title">{{ item.name || item.title || '未命名项目' }}</div>
          <div class="related-desc">{{ item.description || '暂无项目描述' }}</div>
          <div class="related-meta">{{ mapCategory(item.category) }} · {{ item.stars || 0 }} 收藏</div>
        </div>
      </div>
      <el-empty v-else description="暂无相关推荐" :image-size="70" />
    </el-card>
  </div>
</template>

<script>
import ProjectActivityTimeline from './ProjectActivityTimeline.vue'

export default {
  name: 'ProjectDetailSidebar',
  components: {
    ProjectActivityTimeline
  },
  props: {
    activityTimelineKey: {
      type: [Number, String],
      default: 0
    },
    projectId: {
      type: [Number, String],
      default: null
    },
    selectedAiModelId: {
      type: [Number, String],
      default: null
    },
    aiModelsLoading: {
      type: Boolean,
      default: false
    },
    aiModels: {
      type: Array,
      default: () => []
    },
    formatAiModelOption: {
      type: Function,
      default: () => ''
    },
    handleAiModelChange: {
      type: Function,
      default: () => {}
    },
    currentAiModelLabel: {
      type: String,
      default: ''
    },
    currentAiProviderLabel: {
      type: String,
      default: ''
    },
    aiSummaryLoading: {
      type: Boolean,
      default: false
    },
    handleAiSummarizeProject: {
      type: Function,
      default: () => {}
    },
    aiTaskLoading: {
      type: Boolean,
      default: false
    },
    handleAiSplitProjectTasks: {
      type: Function,
      default: () => {}
    },
    aiRiskLoading: {
      type: Boolean,
      default: false
    },
    handleAiIdentifyRisks: {
      type: Function,
      default: () => {}
    },
    aiNextStepsLoading: {
      type: Boolean,
      default: false
    },
    handleAiNextSteps: {
      type: Function,
      default: () => {}
    },
    project: {
      type: Object,
      default: () => ({})
    },
    statusLabel: {
      type: String,
      default: ''
    },
    categoryLabel: {
      type: String,
      default: ''
    },
    formatTime: {
      type: Function,
      default: () => '-'
    },
    visibilityLabel: {
      type: String,
      default: ''
    },
    pageAccessResolved: {
      type: Boolean,
      default: false
    },
    canSeeTaskCollaboration: {
      type: Boolean,
      default: false
    },
    myTodoTasks: {
      type: Array,
      default: () => []
    },
    myTasksLoading: {
      type: Boolean,
      default: false
    },
    fetchMyTasks: {
      type: Function,
      default: () => {}
    },
    myTodoPendingCount: {
      type: Number,
      default: 0
    },
    isTaskOverdue: {
      type: Function,
      default: () => false
    },
    getTaskPriorityType: {
      type: Function,
      default: () => 'info'
    },
    getTaskPriorityText: {
      type: Function,
      default: () => ''
    },
    getTaskDueLabel: {
      type: Function,
      default: () => ''
    },
    taskQuickUpdatingId: {
      type: [Number, String],
      default: null
    },
    handleQuickTaskStatusChange: {
      type: Function,
      default: () => {}
    },
    openTaskCollabDrawer: {
      type: Function,
      default: () => {}
    },
    todayDueTasks: {
      type: Array,
      default: () => []
    },
    taskBoardLoading: {
      type: Boolean,
      default: false
    },
    fetchProjectTasks: {
      type: Function,
      default: () => {}
    },
    getTaskAssigneeName: {
      type: Function,
      default: () => ''
    },
    formatTaskDueClock: {
      type: Function,
      default: () => ''
    },
    contributors: {
      type: Array,
      default: () => []
    },
    roleLabel: {
      type: Function,
      default: () => ''
    },
    relatedProjects: {
      type: Array,
      default: () => []
    },
    goToDetail: {
      type: Function,
      default: () => {}
    },
    mapCategory: {
      type: Function,
      default: () => ''
    }
  },
  computed: {
    selectedAiModelIdProxy: {
      get() {
        return this.selectedAiModelId
      },
      set(value) {
        this.$emit('update:selectedAiModelId', value)
      }
    }
  }
}
</script>
