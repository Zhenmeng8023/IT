<template>
  <div class="manage-header">
    <div>
      <div class="header-top">
        <el-button type="text" icon="el-icon-arrow-left" @click="$emit('back')">返回项目详情</el-button>
      </div>
      <h1 class="page-title">{{ project.title || '项目管理' }}</h1>
      <div class="project-context-meta">
        <el-tag size="small" :type="getProjectStatusType(project.status)">{{ project.statusText || '-' }}</el-tag>
        <span>当前角色：{{ currentRoleText || '-' }}</span>
      </div>
    </div>

    <div class="header-actions">
      <el-button icon="el-icon-refresh" @click="$emit('refresh')">刷新</el-button>
    </div>
  </div>
</template>

<script>
import { getProjectStatusType } from '../services/projectManageShared'

export default {
  name: 'ProjectManageHeader',
  props: {
    project: {
      type: Object,
      default: () => ({})
    },
    currentRoleText: {
      type: String,
      default: ''
    }
  },
  methods: {
    getProjectStatusType
  }
}
</script>

<style scoped>
.manage-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
  padding: 22px 24px;
  border-radius: 24px;
  background: var(--it-surface);
  border: 1px solid var(--it-border);
  box-shadow: var(--it-shadow);
}

.header-top {
  margin-bottom: 8px;
}

.page-title {
  margin: 0;
  font-size: 30px;
  color: #303133;
}

.project-context-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  color: #909399;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .manage-header {
    flex-direction: column;
    padding: 18px;
  }
}
</style>
