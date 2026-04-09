<template>
  <div class="project-templates-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">项目模板中心</h1>
        <p class="page-subtitle">统一浏览模板、预览模板内容，并直接从模板创建新项目。</p>
      </div>
      <div class="page-actions">
        <el-button icon="el-icon-arrow-left" @click="goBackMyProjects">返回我的项目</el-button>
        <el-button type="primary" icon="el-icon-plus" @click="goBlankCreate">新建空白项目</el-button>
      </div>
    </div>

    <ProjectTemplateCenter @template-applied="handleTemplateApplied" />
  </div>
</template>

<script>
import ProjectTemplateCenter from '../projectmanage/components/ProjectTemplateCenter.vue'

export default {
  name: 'ProjectTemplatesPage',
  layout: 'project',
  components: {
    ProjectTemplateCenter
  },
  methods: {
    goBackMyProjects() {
      this.$router.push({ path: '/myproject' })
    },
    goBlankCreate() {
      this.$router.push({ path: '/myproject', query: { openCreate: '1' } })
    },
    handleTemplateApplied(payload) {
      const projectId = payload?.id || payload?.projectId || payload?.data?.id || payload?.data?.projectId
      if (!projectId) return
      this.$router.push({
        path: '/projectdetail',
        query: { projectId: String(projectId) }
      })
    }
  }
}
</script>

<style scoped>
.project-templates-page {
  max-width: 1360px;
  margin: 0 auto;
  padding: 24px;
}
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
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
.page-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
@media (max-width: 768px) {
  .project-templates-page { padding: 16px; }
  .page-header { flex-direction: column; }
}
</style>
