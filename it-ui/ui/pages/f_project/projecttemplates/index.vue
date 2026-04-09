<template>
  <div class="project-templates-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">项目模板库</h1>
        <p class="page-subtitle">这里作为隐藏的模板资产页保留，用于集中浏览、预览和维护项目模板，不再承担新建项目主入口。</p>
      </div>
      <div class="page-actions">
        <el-button icon="el-icon-arrow-left" @click="goBackMyProjects">返回我的项目</el-button>
      </div>
    </div>

    <ProjectTemplateCenter />
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
  line-height: 1.8;
  max-width: 920px;
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
