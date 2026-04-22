import useKnowledgeBasePage from '@/pages/ai/composables/useKnowledgeBasePage'

function normalizeProjectId(raw) {
  if (raw === undefined || raw === null || raw === '') return null
  const firstValue = Array.isArray(raw) ? raw[0] : raw
  const text = String(firstValue || '').trim()
  if (!text) return null
  const numeric = Number(text)
  return Number.isFinite(numeric) && numeric > 0 ? numeric : text
}

function parseRouteProjectId(route) {
  if (!route) return null
  return normalizeProjectId((route.query && route.query.projectId) || (route.params && route.params.projectId))
}

function isSameProjectId(left, right) {
  if (left === undefined || left === null || left === '') {
    return right === undefined || right === null || right === ''
  }
  if (right === undefined || right === null || right === '') {
    return false
  }
  return String(left) === String(right)
}

function createEmptyEmbeddingStatus() {
  return { totalChunkCount: 0, embeddedChunkCount: 0, createdEmbeddingCount: 0 }
}

export default {
  mixins: [useKnowledgeBasePage],
  computed: {
    canCreateKnowledgeBase() {
      return !!this.routeProjectId
    },

    canManageCurrentMembers() {
      return false
    }
  },
  methods: {
    initRouteContext() {
      const previousRouteProjectId = this.routeProjectId
      const routeProjectId = parseRouteProjectId(this.$route)
      const changed = !isSameProjectId(previousRouteProjectId, routeProjectId)

      this.routeProjectId = routeProjectId
      this.projectId = routeProjectId
      this.listMode = 'project'
      this.chatForm.projectId = routeProjectId
      this.chatForm.bizType = routeProjectId ? 'PROJECT' : 'GENERAL'
      this.chatForm.sceneCode = routeProjectId ? 'project.knowledge-base' : 'knowledge.base'
      return changed
    },

    ensureCanCreateKnowledgeBase() {
      if (this.canCreateKnowledgeBase) return true
      this.$message.warning('请先绑定项目上下文，再创建项目知识库')
      return false
    },

    handleListModeChange() {
      if (this.listMode !== 'project') {
        this.listMode = 'project'
      }
      if (!isSameProjectId(this.projectId, this.routeProjectId)) {
        this.projectId = this.routeProjectId
      }
      this.pagination.page = 0
      this.loadKnowledgeBases()
    },

    canEditKnowledgeBaseItem() {
      return false
    },

    async loadMembers() {
      this.members = []
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
      this.$message.info('前台项目知识库中心不提供索引任务治理入口')
    },

    viewIndexTasks() {
      this.$message.info('前台项目知识库中心不提供索引任务治理入口')
    },

    reindexKnowledgeBase() {
      this.$message.info('前台项目知识库中心不提供重建索引能力')
    },

    reindexDocument() {
      this.$message.info('前台项目知识库中心不提供重建索引能力')
    },

    manualRefreshEmbeddingStatus() {
      this.$message.info('前台项目知识库中心不提供 Embedding 治理能力')
    },

    async backfillCurrentKnowledgeBaseEmbeddings() {
      this.$message.info('前台项目知识库中心不提供 Embedding 回填能力')
    },

    async backfillDocumentVector() {
      this.$message.info('前台项目知识库中心不提供 Embedding 回填能力')
    },

    async runChatDebugSearch() {
      this.$message.info('Front project knowledge base center does not provide debug search')
    },

    async openRetrievalDrawerByMessage() {
      this.$message.info('Front project knowledge base center does not provide retrieval logs')
    },

    async openRetrievalDrawer() {
      this.$message.info('Front project knowledge base center does not provide retrieval logs')
    }
  }
}
