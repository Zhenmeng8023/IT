<template>
  <div class="project-audit-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">项目审核中心</h1>
        <p class="page-subtitle">围绕分支保护、合并请求、评审和检查统一收口项目主线治理。</p>
      </div>
      <div class="header-actions">
        <el-input
          v-model.trim="projectIdInput"
          size="small"
          placeholder="输入项目 ID"
          class="project-id-input"
          @keyup.enter.native="applyProjectId"
        />
        <el-button size="small" @click="applyProjectId">加载项目</el-button>
        <el-button v-if="projectId" size="small" type="primary" @click="goToProjectManage">返回项目工作台</el-button>
      </div>
    </div>

    <ProjectAuditCenter
      v-if="projectId"
      :project-id="projectId"
      :can-manage-project="true"
    />

    <el-empty v-else description="先从 query 或输入框指定 projectId，再进入审核中心" />
  </div>
</template>

<script>
import ProjectAuditCenter from '../../f_project/projectmanage/components/ProjectAuditCenter.vue'

export default {
  name: 'ProjectAuditPage',
  layout: 'manage',
  components: {
    ProjectAuditCenter
  },
  data() {
    const queryProjectId = this.$route && this.$route.query ? this.$route.query.projectId : ''
    return {
      projectId: queryProjectId ? Number(queryProjectId) : null,
      projectIdInput: queryProjectId ? String(queryProjectId) : ''
    }
  },
  watch: {
    '$route.query.projectId': {
      immediate: false,
      handler(value) {
        this.projectId = value ? Number(value) : null
        this.projectIdInput = value ? String(value) : ''
      }
    }
  },
  methods: {
    applyProjectId() {
      const nextId = Number(this.projectIdInput)
      if (!nextId) {
        this.$message.warning('请输入有效的项目 ID')
        return
      }
      this.projectId = nextId
      this.$router.replace({
        path: this.$route.path,
        query: {
          ...this.$route.query,
          projectId: String(nextId)
        }
      })
    },
    goToProjectManage() {
      if (!this.projectId) return
      this.$router.push({
        path: '/projectmanage',
        query: {
          projectId: String(this.projectId),
          tab: 'audit-manage'
        }
      })
    }
  }
}
</script>

<style scoped>
.project-audit-page {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  color: #303133;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #909399;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.project-id-input {
  width: 180px;
}

@media (max-width: 768px) {
  .project-audit-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
  }

  .project-id-input {
    width: 100%;
  }
}
</style>
