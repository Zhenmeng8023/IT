<template>
  <div class="detail-header">
    <div class="breadcrumb-wrap">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/projectlist' }">项目列表</el-breadcrumb-item>
        <el-breadcrumb-item>{{ project.name || '项目详情' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="header-actions">
      <ProjectJoinRequestPanel
        v-if="pageAccessResolved && currentUserId && !canSeeTaskCollaboration"
        :project-id="projectId"
        :current-user-id="currentUserId"
        :can-manage-project="canManageProject"
        @changed="handleProjectSocialChanged"
      />
      <el-button
        v-if="pageAccessResolved && (canManageProject || canSeeTaskCollaboration)"
        size="small"
        icon="el-icon-s-platform"
        @click="handleProjectManageClick"
      >
        进入工作台
      </el-button>
      <el-button
        type="success"
        size="small"
        icon="el-icon-document"
        :loading="aiSummaryLoading"
        @click="handleAiSummarizeProject"
      >
        AI 项目总结
      </el-button>
      <el-button
        v-if="pageAccessResolved && canManageProject"
        type="warning"
        size="small"
        icon="el-icon-s-operation"
        :loading="aiTaskLoading"
        @click="handleAiSplitProjectTasks"
      >
        AI 拆任务
      </el-button>
      <el-button
        type="primary"
        size="small"
        icon="el-icon-star-off"
        :loading="starLoading"
        @click="toggleStar"
      >
        {{ project.starred ? '取消收藏' : '收藏项目' }}
      </el-button>
      <el-button size="small" icon="el-icon-download" @click="downloadMainFile">
        下载主文件
      </el-button>
    </div>
  </div>
</template>

<script>
import ProjectJoinRequestPanel from './ProjectJoinRequestPanel.vue'

export default {
  name: 'ProjectDetailHeader',
  components: {
    ProjectJoinRequestPanel
  },
  props: {
    project: {
      type: Object,
      default: () => ({})
    },
    pageAccessResolved: {
      type: Boolean,
      default: false
    },
    currentUserId: {
      type: [Number, String],
      default: null
    },
    canSeeTaskCollaboration: {
      type: Boolean,
      default: false
    },
    projectId: {
      type: [Number, String],
      default: null
    },
    canManageProject: {
      type: Boolean,
      default: false
    },
    aiSummaryLoading: {
      type: Boolean,
      default: false
    },
    aiTaskLoading: {
      type: Boolean,
      default: false
    },
    starLoading: {
      type: Boolean,
      default: false
    },
    handleProjectSocialChanged: {
      type: Function,
      default: () => {}
    },
    handleProjectManageClick: {
      type: Function,
      default: () => {}
    },
    handleAiSummarizeProject: {
      type: Function,
      default: () => {}
    },
    handleAiSplitProjectTasks: {
      type: Function,
      default: () => {}
    },
    toggleStar: {
      type: Function,
      default: () => {}
    },
    downloadMainFile: {
      type: Function,
      default: () => {}
    }
  }
}
</script>
