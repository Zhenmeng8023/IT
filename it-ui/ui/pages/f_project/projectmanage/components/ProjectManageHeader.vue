<template>
  <div class="manage-header">
    <div>
      <div class="header-top">
        <el-button type="text" icon="el-icon-arrow-left" @click="$emit('back')">返回项目详情</el-button>
      </div>
      <h1 class="page-title">{{ project.title || '项目管理' }}</h1>
      <p class="page-subtitle">{{ project.description || '在这里统一管理任务、成员、文件、活动流与项目设置。' }}</p>
    </div>

    <div class="header-actions">
      <el-button icon="el-icon-refresh" @click="$emit('refresh')">刷新</el-button>
      <el-button icon="el-icon-setting" @click="$emit('open-settings')">项目设置</el-button>
      <el-button type="primary" icon="el-icon-folder-add" @click="$emit('open-save-template')">保存为模板</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ProjectManageHeader',
  props: {
    project: {
      type: Object,
      default: () => ({})
    }
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

.page-subtitle {
  margin: 8px 0 0;
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
