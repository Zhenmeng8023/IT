<template>
  <el-card shadow="never" class="project-kb-entry">
    <div class="entry-content">
      <div class="entry-main">
        <div class="entry-title">项目知识库中心</div>
        <div class="entry-desc">
          面向当前项目的知识库入口，可创建项目知识库、上传资料、ZIP 导入项目文档，并直接发起知识库问答。
        </div>
      </div>

      <div class="entry-actions">
        <el-tag size="small" effect="plain" type="info">项目 #{{ projectId || '-' }}</el-tag>
        <el-button type="primary" size="small" :disabled="!projectId" @click="handleEnter">
          进入知识库中心
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script>
export default {
  name: 'ProjectKnowledgeBaseCenterEntry',
  props: {
    projectId: {
      type: [String, Number],
      default: null
    },
    entryPath: {
      type: String,
      default: '/project-knowledge-base'
    },
    autoNavigate: {
      type: Boolean,
      default: true
    }
  },
  methods: {
    buildTargetLocation() {
      return {
        path: this.entryPath,
        query: this.projectId ? { projectId: String(this.projectId) } : undefined
      }
    },

    handleEnter() {
      if (!this.projectId) {
        this.$message.warning('请先绑定项目上下文，再进入项目知识库中心')
        return
      }

      const location = this.buildTargetLocation()
      this.$emit('enter', location)
      if (!this.autoNavigate) return
      this.$router.push(location).catch(() => {})
    }
  }
}
</script>

<style scoped>
.project-kb-entry {
  border-radius: 18px;
  border: 1px solid var(--it-border);
  background: var(--it-panel-bg-strong);
  box-shadow: var(--it-shadow-soft);
}

.entry-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.entry-main {
  min-width: 0;
}

.entry-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--it-text);
}

.entry-desc {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--it-text-subtle);
}

.entry-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .entry-content {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
