<template>
  <el-card v-if="pageAccessResolved && canSeeTaskCollaboration" shadow="never" class="section-card task-board-card">
    <div slot="header" class="section-header section-header-flex">
      <span>任务看板</span>
      <div class="task-board-header-actions">
        <el-button size="mini" type="text" :loading="taskBoardLoading" @click="fetchProjectTasks">刷新</el-button>
        <el-button size="mini" type="primary" plain icon="el-icon-s-operation" @click="handleTaskManageClick">进入任务协作</el-button>
      </div>
    </div>

    <div class="task-board-summary">
      <div class="task-mini-stat">
        <div class="task-mini-stat-value">{{ taskSummary.total }}</div>
        <div class="task-mini-stat-label">总任务</div>
      </div>
      <div class="task-mini-stat">
        <div class="task-mini-stat-value">{{ taskSummary.todo }}</div>
        <div class="task-mini-stat-label">待处理</div>
      </div>
      <div class="task-mini-stat">
        <div class="task-mini-stat-value">{{ taskSummary.inProgress }}</div>
        <div class="task-mini-stat-label">进行中</div>
      </div>
      <div class="task-mini-stat">
        <div class="task-mini-stat-value">{{ taskSummary.done }}</div>
        <div class="task-mini-stat-label">已完成</div>
      </div>
      <div class="task-mini-stat danger">
        <div class="task-mini-stat-value">{{ taskSummary.overdue }}</div>
        <div class="task-mini-stat-label">已逾期</div>
      </div>
    </div>

    <div v-loading="taskBoardLoading" class="task-board-grid">
      <div class="task-compact-panel">
        <div class="task-compact-panel-head">
          <span>最近任务</span>
          <el-tag size="mini" effect="plain">{{ recentTasks.length }}</el-tag>
        </div>
        <div v-if="recentTasks.length" class="task-compact-list">
          <div v-for="task in recentTasks" :key="'recent-' + task.id" class="task-compact-item" :class="{ 'is-overdue': isTaskOverdue(task) }">
            <div class="task-compact-main">
              <div class="task-compact-top">
                <div class="task-compact-title">{{ task.title || '未命名任务' }}</div>
                <div class="task-compact-tags">
                  <el-tag size="mini" effect="plain" :type="getTaskPriorityType(task.priority)">{{ getTaskPriorityText(task.priority) }}</el-tag>
                  <el-tag size="mini" :type="getTaskStatusType(task.status)">{{ getTaskStatusText(task.status) }}</el-tag>
                </div>
              </div>
              <div class="task-compact-meta">
                <div class="task-assignee-inline">
                  <el-avatar :size="26" :src="task.assigneeAvatar || ''">{{ getTaskAssigneeName(task).slice(0, 1) }}</el-avatar>
                  <span>{{ getTaskAssigneeName(task) }}</span>
                </div>
                <span>{{ getTaskTimeLabel(task) }}</span>
              </div>
            </div>
            <div class="task-compact-side">
              <el-select :value="task.status" size="mini" placeholder="状态" :disabled="taskQuickUpdatingId === task.id" @change="handleQuickTaskStatusChange(task, $event)">
                <el-option label="待处理" value="todo" />
                <el-option label="进行中" value="in_progress" />
                <el-option label="已完成" value="done" />
              </el-select>
              <el-button size="mini" type="text" class="task-collab-entry-btn" @click="openTaskCollabDrawer(task, 'comment')">协作详情</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无最近任务" :image-size="60" />
      </div>

      <div class="task-compact-panel">
        <div class="task-compact-panel-head">
          <span>逾期任务</span>
          <el-tag size="mini" type="danger" effect="plain">{{ overdueTasks.length }}</el-tag>
        </div>
        <div v-if="overdueTasks.length" class="task-compact-list">
          <div v-for="task in overdueTasks" :key="'overdue-' + task.id" class="task-compact-item is-overdue">
            <div class="task-compact-main">
              <div class="task-compact-top">
                <div class="task-compact-title">{{ task.title || '未命名任务' }}</div>
                <div class="task-compact-tags">
                  <el-tag size="mini" effect="plain" :type="getTaskPriorityType(task.priority)">{{ getTaskPriorityText(task.priority) }}</el-tag>
                  <el-tag size="mini" type="danger">逾期</el-tag>
                </div>
              </div>
              <div class="task-compact-meta">
                <div class="task-assignee-inline">
                  <el-avatar :size="26" :src="task.assigneeAvatar || ''">{{ getTaskAssigneeName(task).slice(0, 1) }}</el-avatar>
                  <span>{{ getTaskAssigneeName(task) }}</span>
                </div>
                <span>{{ getTaskDueLabel(task) }}</span>
              </div>
            </div>
            <div class="task-compact-side">
              <el-button size="mini" type="success" plain :loading="taskQuickUpdatingId === task.id" @click="handleQuickTaskStatusChange(task, 'done')">标记完成</el-button>
              <el-button size="mini" type="text" class="task-collab-entry-btn" @click="openTaskCollabDrawer(task, 'dependency')">协作详情</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无逾期任务" :image-size="60" />
      </div>
    </div>
  </el-card>
</template>

<script>
export default {
  name: 'ProjectTaskBoardPreview',
  props: {
    pageAccessResolved: {
      type: Boolean,
      default: false
    },
    canSeeTaskCollaboration: {
      type: Boolean,
      default: false
    },
    taskBoardLoading: {
      type: Boolean,
      default: false
    },
    taskSummary: {
      type: Object,
      default: () => ({ total: 0, todo: 0, inProgress: 0, done: 0, overdue: 0 })
    },
    recentTasks: {
      type: Array,
      default: () => []
    },
    overdueTasks: {
      type: Array,
      default: () => []
    },
    taskQuickUpdatingId: {
      type: [Number, String],
      default: null
    },
    fetchProjectTasks: {
      type: Function,
      default: () => {}
    },
    handleTaskManageClick: {
      type: Function,
      default: () => {}
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
    getTaskStatusType: {
      type: Function,
      default: () => 'info'
    },
    getTaskStatusText: {
      type: Function,
      default: () => ''
    },
    getTaskAssigneeName: {
      type: Function,
      default: () => ''
    },
    getTaskTimeLabel: {
      type: Function,
      default: () => ''
    },
    getTaskDueLabel: {
      type: Function,
      default: () => ''
    },
    handleQuickTaskStatusChange: {
      type: Function,
      default: () => {}
    },
    openTaskCollabDrawer: {
      type: Function,
      default: () => {}
    }
  }
}
</script>
