import useKnowledgeBasePage from '@/pages/ai/composables/useKnowledgeBasePage'

function createEmptyEmbeddingStatus() {
  return { totalChunkCount: 0, embeddedChunkCount: 0, createdEmbeddingCount: 0 }
}

export default {
  mixins: [useKnowledgeBasePage],
  methods: {
    initRouteContext() {
      const changed =
        this.routeProjectId !== null ||
        this.projectId !== null ||
        this.listMode !== 'owner' ||
        this.chatForm.projectId !== null ||
        this.chatForm.bizType !== 'GENERAL' ||
        this.chatForm.sceneCode !== 'knowledge.base'

      this.routeProjectId = null
      this.projectId = null
      this.listMode = 'owner'
      this.chatForm.projectId = null
      this.chatForm.bizType = 'GENERAL'
      this.chatForm.sceneCode = 'knowledge.base'
      return changed
    },

    handleListModeChange() {
      if (this.listMode !== 'owner') {
        this.listMode = 'owner'
      }
      this.pagination.page = 0
      this.loadKnowledgeBases()
    },

    canEditKnowledgeBaseItem() {
      return false
    },

    async loadIndexTasks() {
      this.indexTasks = []
      this.taskDrawerVisible = false
      this.stopTaskPolling()
    },

    async loadKnowledgeBaseEmbeddingStatus() {
      this.kbEmbeddingStatus = createEmptyEmbeddingStatus()
    },

    async refreshEmbeddingRuntimeState() {
      this.embeddingBackfillRunning = false
      this.embeddingRunningTaskId = null
      this.stopEmbeddingPolling()
      if (!this.documentEmbeddingStatusMap || typeof this.documentEmbeddingStatusMap !== 'object') {
        this.documentEmbeddingStatusMap = {}
      }
    },

    openKnowledgeBaseTasks() {
      this.$message.info('前台个人知识库中心不提供索引任务入口')
    },

    viewIndexTasks() {
      this.$message.info('前台个人知识库中心不提供索引任务入口')
    },

    reindexKnowledgeBase() {
      this.$message.info('前台个人知识库中心不提供重建索引能力')
    },

    reindexDocument() {
      this.$message.info('前台个人知识库中心不提供重建索引能力')
    },

    manualRefreshEmbeddingStatus() {
      this.$message.info('前台个人知识库中心不提供 Embedding 治理能力')
    },

    async backfillCurrentKnowledgeBaseEmbeddings() {
      this.$message.info('前台个人知识库中心不提供 Embedding 回填能力')
    },

    async backfillDocumentVector() {
      this.$message.info('前台个人知识库中心不提供 Embedding 回填能力')
    },

    async runChatDebugSearch() {
      this.$message.info('前台个人知识库中心不提供调试检索能力')
    },

    async openRetrievalDrawerByMessage() {
      this.$message.info('前台个人知识库中心不提供检索日志查看能力')
    },

    async openRetrievalDrawer() {
      this.$message.info('前台个人知识库中心不提供检索日志查看能力')
    }
  }
}
