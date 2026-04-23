import {
  readBrowserPermissionCodes,
  extractResponseData as readResponseData,
  extractResponseMessage as readResponseMessage,
  extractPageData as readPageData,
  extractListData as readListData,
  normalizeKnowledgeBase,
  normalizeDocument,
  normalizeTask,
  normalizeSources,
  normalizeEmbeddingStatus,
  getEmbeddingCompletionRate,
  formatTime,
  kbStatusTagType,
  docStatusTagType,
  taskStatusTagType,
  readStoredUserInfo
} from '@/pages/ai/services/knowledgeBaseDomain'
import { adminKnowledgeGovernanceService } from '@/pages/ai/services/adminKnowledgeGovernanceService'

function parseRouteProjectId(route) {
  if (!route) return null
  const queryId = route.query && route.query.projectId
  const paramsId = route.params && route.params.projectId
  const raw = queryId !== undefined && queryId !== null && queryId !== '' ? queryId : paramsId
  if (raw === undefined || raw === null) return null

  const firstValue = Array.isArray(raw) ? raw[0] : raw
  const text = String(firstValue || '').trim()
  if (!text) return null

  const numeric = Number(text)
  if (!Number.isNaN(numeric) && Number.isFinite(numeric)) {
    return numeric
  }
  return text
}

function parseRouteKnowledgeBaseId(route) {
  if (!route) return null
  const raw =
    (route.query && (route.query.kbId || route.query.knowledgeBaseId)) ||
    (route.params && (route.params.kbId || route.params.knowledgeBaseId)) ||
    null
  const firstValue = Array.isArray(raw) ? raw[0] : raw
  const text = String(firstValue || '').trim()
  if (!text) return null
  const numeric = Number(text)
  return Number.isFinite(numeric) && numeric > 0 ? numeric : text
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

function pickPreferredKnowledgeBase(list = [], routeProjectId) {
  if (!Array.isArray(list) || !list.length) return null
  if (routeProjectId === undefined || routeProjectId === null || routeProjectId === '') {
    return list[0]
  }
  return list.find(item => isSameProjectId(item && item.projectId, routeProjectId)) || list[0]
}

export default {
  data() {
    return {
      permissionCodes: [],
      listMode: 'all',
      keyword: '',
      ownerId: null,
      projectId: null,
      routeProjectId: null,

      loading: {
        kbList: false,
        documents: false,
        tasks: false,
        retrievals: false,
        embeddingStatus: false,
        debugSearch: false,
        governanceAction: false
      },

      pagination: {
        page: 0,
        size: 10,
        total: 0
      },

      documentPagination: {
        page: 0,
        size: 10,
        total: 0
      },

      knowledgeBases: [],
      currentKnowledgeBase: null,
      documents: [],
      indexTasks: [],
      currentTaskDocument: null,

      taskDrawerVisible: false,
      taskDrawerScope: 'knowledgeBase',
      taskPollTimer: null,

      retrievalDrawerVisible: false,
      retrievalLogs: [],
      currentRetrievalMeta: '',

      kbEmbeddingStatus: { totalChunkCount: 0, embeddedChunkCount: 0, createdEmbeddingCount: 0 },
      documentEmbeddingStatusMap: {},
      embeddingPollTimer: null,
      embeddingBackfillRunning: false,
      embeddingBackfillSubmitting: false,
      embeddingRunningTaskId: null,

      debugForm: {
        query: '',
        topK: 5
      },

      governancePlaceholders: [
        { key: 'freeze-kb', label: 'Freeze knowledge base (pending)' },
        { key: 'archive-kb', label: 'Archive knowledge base (pending)' }, { key: 'delete-kb', label: 'Delete knowledge base (pending)' }, { key: 'operation-audit', label: 'Operation audit (pending)' }
      ]
    }
  },

  computed: {
    canGovernCurrentKnowledgeBase() {
      return !!this.currentKnowledgeBase
    },

    filteredKnowledgeBases() {
      if (!this.keyword) return this.knowledgeBases
      const key = String(this.keyword).toLowerCase()
      return this.knowledgeBases.filter(item => {
        if (!item) return false
        return (
          String(item.name || '').toLowerCase().includes(key) ||
          String(item.description || '').toLowerCase().includes(key) ||
          String(item.id || '').includes(key)
        )
      })
    },

    documentStats() {
      const rows = Array.isArray(this.documents) ? this.documents : []
      let indexedCount = 0
      let failedCount = 0
      let processingCount = 0
      let embeddedReadyCount = 0

      rows.forEach(item => {
        const status = String(item && item.status ? item.status : '').toUpperCase()
        if (status === 'INDEXED' || status === 'READY') indexedCount += 1
        if (status === 'FAILED' || status === 'ERROR') failedCount += 1
        if (status === 'PENDING' || status === 'PROCESSING' || status === 'UPLOADING') processingCount += 1

        const embedding = this.documentEmbeddingStatusMap[item && item.id]
        if (!embedding) return
        const total = Number(embedding.totalChunkCount || 0)
        const embedded = Number(embedding.embeddedChunkCount || 0)
        if (total > 0 && embedded >= total) {
          embeddedReadyCount += 1
        }
      })

      return {
        totalCount: rows.length,
        indexedCount,
        failedCount,
        processingCount,
        embeddedReadyCount
      }
    },

    embeddingCompletionRate() {
      return getEmbeddingCompletionRate(this.kbEmbeddingStatus)
    },

    embeddingButtonText() {
      if (this.embeddingBackfillRunning) {
        return `Backfilling ${this.embeddingCompletionRate}%`
      }
      return 'Backfill Embedding'
    },

    taskDrawerTitle() {
      return this.taskDrawerScope === 'document' ? 'Document Index Tasks' : 'Knowledge Base Index Tasks'
    },

    taskDrawerSubtitle() {
      if (this.taskDrawerScope === 'document' && this.currentTaskDocument) {
        return this.currentTaskDocument.title || `文档 #${this.currentTaskDocument.id}`
      }
      return this.currentKnowledgeBase ? this.currentKnowledgeBase.name : 'Current Knowledge Base'
    }
  },

  watch: {
    taskDrawerVisible(value) {
      if (value) {
        this.startTaskPolling()
      } else {
        this.stopTaskPolling()
      }
    },

    '$route.fullPath'() {
      const changed = this.initRouteContext()
      if (!changed) return
      this.pagination.page = 0
      this.loadKnowledgeBases()
    }
  },

  mounted() {
    this.initPermissionCodes()
    this.initRouteContext()
    this.initUserId()
    this.loadKnowledgeBases()
  },

  beforeDestroy() {
    this.stopTaskPolling()
    this.stopEmbeddingPolling()
  },

  methods: {
    formatTime,
    kbStatusTagType,
    docStatusTagType,
    taskStatusTagType,

    extractResponseData(res) {
      return readResponseData(res)
    },

    extractResponseMessage(res, fallback) {
      return readResponseMessage(res, fallback)
    },

    extractPageData(res) {
      return readPageData(res)
    },

    extractListData(res) {
      return readListData(res)
    },

    documentEmbeddingLabel(row) {
      const status = this.documentEmbeddingStatusMap[row && row.id]
      if (!status) return 'Not counted'
      const total = Number(status.totalChunkCount || 0)
      const embedded = Number(status.embeddedChunkCount || 0)
      if (!total) return 'No chunks'
      if (embedded >= total) return `Done ${embedded}/${total}`
      return `Pending ${embedded}/${total}`
    },

    documentEmbeddingTagType(row) {
      const status = this.documentEmbeddingStatusMap[row && row.id]
      if (!status) return 'info'
      const total = Number(status.totalChunkCount || 0)
      const embedded = Number(status.embeddedChunkCount || 0)
      if (!total) return 'info'
      return embedded >= total ? 'success' : 'warning'
    },

    canEditKnowledgeBaseItem() {
      return false
    },

    initPermissionCodes() {
      this.permissionCodes = readBrowserPermissionCodes()
    },

    hasAuthority(code) {
      if (!code) return true
      if (this.permissionCodes.includes(code)) return true
      const routePermissions = (((this.$route || {}).meta || {}).permissions) || []
      return Array.isArray(routePermissions) && routePermissions.includes(code)
    },

    initUserId() {
      const user = readStoredUserInfo()
      const uid = user && (user.id || user.userId)
      if (!uid) return
      this.ownerId = uid
    },

    initRouteContext() {
      const previousRouteProjectId = this.routeProjectId
      const routeProjectId = parseRouteProjectId(this.$route)
      const changed = !isSameProjectId(previousRouteProjectId, routeProjectId)
      if (!routeProjectId) {
        this.routeProjectId = null
        if (
          this.listMode === 'project' &&
          previousRouteProjectId &&
          isSameProjectId(this.projectId, previousRouteProjectId)
        ) {
          this.projectId = null
          this.listMode = 'all'
        }
        return changed
      }

      this.routeProjectId = routeProjectId
      this.projectId = routeProjectId
      this.listMode = 'project'
      return changed
    },

    handleListModeChange() {
      this.pagination.page = 0
      this.loadKnowledgeBases()
    },

    handleKbPageChange(page) {
      this.pagination.page = page - 1
      this.loadKnowledgeBases()
    },

    handleDocumentPageChange(page) {
      this.documentPagination.page = page - 1
      this.loadDocuments()
    },

    async loadKnowledgeBases() {
      this.loading.kbList = true
      try {
        if (this.listMode === 'project' && !this.projectId) {
          this.knowledgeBases = []
          this.pagination.total = 0
          this.resetCurrentKnowledgeBase()
          return
        }
        // all mode does not require owner filter






        const res = await adminKnowledgeGovernanceService.fetchKnowledgeBases({
          listMode: this.listMode,
          ownerId: this.ownerId,
          projectId: this.projectId,
          page: this.pagination.page,
          size: this.pagination.size
        })

        const pageData = this.extractPageData(res)
        this.knowledgeBases = (pageData.content || []).map(item => normalizeKnowledgeBase(item))
        this.pagination.total = pageData.total || 0

        const currentId = this.currentKnowledgeBase && this.currentKnowledgeBase.id
        let target = null
        if (currentId) {
          target = this.knowledgeBases.find(item => item.id === currentId) || null
        }

        const routeKnowledgeBaseId = parseRouteKnowledgeBaseId(this.$route)
        if (!target && routeKnowledgeBaseId) {
          target = this.knowledgeBases.find(item => String(item.id) === String(routeKnowledgeBaseId)) || null
        }
        if (!target && routeKnowledgeBaseId) {
          try {
            const detailRes = await adminKnowledgeGovernanceService.fetchKnowledgeBaseDetail(routeKnowledgeBaseId)
            target = normalizeKnowledgeBase(this.extractResponseData(detailRes) || {})
          } catch (error) {
            target = null
          }
        }
        if (!target && this.knowledgeBases.length) {
          target = pickPreferredKnowledgeBase(
            this.knowledgeBases,
            this.listMode === 'project' ? this.routeProjectId || this.projectId : null
          )
        }

        if (target) {
          await this.selectKnowledgeBase(target, { reloadBase: false })
        } else {
          this.resetCurrentKnowledgeBase()
        }
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, 'Failed to load knowledge bases'))
      } finally {
        this.loading.kbList = false
      }
    },

    resetCurrentKnowledgeBase() {
      this.currentKnowledgeBase = null
      this.documents = []
      this.indexTasks = []
      this.currentTaskDocument = null
      this.documentPagination.total = 0
      this.retrievalLogs = []
      this.kbEmbeddingStatus = { totalChunkCount: 0, embeddedChunkCount: 0, createdEmbeddingCount: 0 }
      this.documentEmbeddingStatusMap = {}
      this.embeddingBackfillRunning = false
      this.embeddingRunningTaskId = null
      this.stopEmbeddingPolling()
    },

    async selectKnowledgeBase(item, options = {}) {
      if (!item || !item.id) return
      const reloadBase = options.reloadBase !== false
      try {
        if (reloadBase) {
          const res = await adminKnowledgeGovernanceService.fetchKnowledgeBaseDetail(item.id)
          this.currentKnowledgeBase = normalizeKnowledgeBase(this.extractResponseData(res) || item)
        } else {
          this.currentKnowledgeBase = normalizeKnowledgeBase(item)
        }

        this.documentPagination.page = 0
        this.debugForm.topK = Number(this.currentKnowledgeBase.defaultTopK || 5) || 5

        await this.loadDocuments()
        await this.loadIndexTasks('knowledgeBase', { silent: true, background: true })
        await this.refreshEmbeddingRuntimeState(false, true)
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, 'Failed to load knowledge base detail'))
      }
    },

    async loadDocuments() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      this.loading.documents = true
      try {
        const res = await adminKnowledgeGovernanceService.fetchDocuments(
          this.currentKnowledgeBase.id,
          this.documentPagination.page,
          this.documentPagination.size
        )
        const pageData = this.extractPageData(res)
        this.documents = (pageData.content || []).map(item => normalizeDocument(item))
        this.documentPagination.total = pageData.total || 0
        await this.loadDocumentEmbeddingStatuses()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '加载文档失败'))
      } finally {
        this.loading.documents = false
      }
    },

    async loadDocumentEmbeddingStatuses() {
      const docs = Array.isArray(this.documents) ? this.documents.slice() : []
      if (!docs.length) {
        this.documentEmbeddingStatusMap = {}
        return
      }

      const entries = await Promise.all(docs.map(async item => {
        try {
          const res = await adminKnowledgeGovernanceService.fetchDocumentEmbeddingStatus(item.id)
          return [item.id, normalizeEmbeddingStatus(this.extractResponseData(res) || {})]
        } catch (error) {
          return [item.id, null]
        }
      }))

      const map = {}
      entries.forEach(([id, value]) => {
        if (id) map[id] = value
      })
      this.documentEmbeddingStatusMap = map
    },

    isKnowledgeBaseEmbeddingConfigured(row) {
      if (!row) return false
      return !!String(row.embeddingProvider || '').trim() && !!String(row.embeddingModel || '').trim()
    },

    async reindexKnowledgeBase(row) {
      const kb = row || this.currentKnowledgeBase
      if (!kb || !kb.id) return
      this.loading.governanceAction = true
      try {
        await adminKnowledgeGovernanceService.createIndexTask(kb.id, {})
        this.$message.success('Knowledge base reindex task submitted')
        this.openKnowledgeBaseTasks()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, 'Failed to submit knowledge base reindex task'))
      } finally {
        this.loading.governanceAction = false
      }
    },

    async reindexDocument(row) {
      if (!row || !row.id || !this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      this.loading.governanceAction = true
      try {
        await adminKnowledgeGovernanceService.createIndexTask(this.currentKnowledgeBase.id, { documentId: row.id })
        this.$message.success('Document reindex task submitted')
        this.viewIndexTasks(row)
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '提交文档索引任务失败'))
      } finally {
        this.loading.governanceAction = false
      }
    },

    async backfillDocumentVector(row) {
      if (!row || !row.id || !this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      if (!this.isKnowledgeBaseEmbeddingConfigured(this.currentKnowledgeBase)) {
        this.$message.warning('Embedding provider/model is not configured for current knowledge base')
        return
      }
      this.loading.governanceAction = true
      try {
        await adminKnowledgeGovernanceService.backfillDocumentEmbeddings(row.id, {
          provider: this.currentKnowledgeBase.embeddingProvider,
          modelName: this.currentKnowledgeBase.embeddingModel
        })
        this.$message.success('Document embedding backfill completed')
        await this.loadKnowledgeBaseEmbeddingStatus(false)
        await this.loadDocumentEmbeddingStatuses()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '文档向量回填失败'))
      } finally {
        this.loading.governanceAction = false
      }
    },

    async loadIndexTasks(scope, options = {}) {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      const silent = !!options.silent
      const background = !!options.background
      if (!background) {
        this.loading.tasks = true
      }

      try {
        let res = null
        if (scope === 'document' && this.currentTaskDocument && this.currentTaskDocument.id) {
          res = await adminKnowledgeGovernanceService.fetchDocumentTasks(this.currentTaskDocument.id)
        } else {
          res = await adminKnowledgeGovernanceService.fetchKnowledgeBaseTasks(this.currentKnowledgeBase.id)
        }
        this.indexTasks = this.extractListData(res).map(item => normalizeTask(item))
        if (scope !== 'document') {
          this.syncEmbeddingTaskState(this.indexTasks)
        }
      } catch (error) {
        if (!silent) {
          this.$message.error(this.extractResponseMessage(error, '加载索引任务失败'))
        }
      } finally {
        if (!background) {
          this.loading.tasks = false
        }
      }
    },

    openKnowledgeBaseTasks() {
      this.taskDrawerScope = 'knowledgeBase'
      this.currentTaskDocument = null
      this.taskDrawerVisible = true
      this.loadIndexTasks('knowledgeBase')
    },

    viewIndexTasks(row) {
      this.taskDrawerScope = 'document'
      this.currentTaskDocument = row
      this.taskDrawerVisible = true
      this.loadIndexTasks('document')
    },

    startTaskPolling() {
      this.stopTaskPolling()
      this.taskPollTimer = setInterval(() => {
        const scope = this.taskDrawerScope === 'document' ? 'document' : 'knowledgeBase'
        this.loadIndexTasks(scope, { silent: true, background: true })
      }, 4000)
    },

    stopTaskPolling() {
      if (this.taskPollTimer) {
        clearInterval(this.taskPollTimer)
        this.taskPollTimer = null
      }
    },

    isRunningEmbeddingTask(task) {
      if (!task) return false
      const type = String(task.taskType || '').toUpperCase()
      const status = String(task.status || '').toUpperCase()
      const embeddingTypes = ['EMBEDDING', 'EMBEDDING_BACKFILL', 'VECTOR_BACKFILL', 'VECTOR']
      const runningStatuses = ['RUNNING', 'PROCESSING', 'INDEXING', 'PENDING']
      return embeddingTypes.includes(type) && runningStatuses.includes(status)
    },

    syncEmbeddingTaskState(tasks = []) {
      const runningTask = (tasks || []).find(item => this.isRunningEmbeddingTask(item)) || null
      if (runningTask) {
        this.embeddingRunningTaskId = runningTask.id
        this.embeddingBackfillRunning = true
        this.startEmbeddingPolling()
        return
      }
      if (this.embeddingBackfillSubmitting || (this.embeddingBackfillRunning && this.embeddingCompletionRate < 100)) {
        this.startEmbeddingPolling()
        return
      }
      this.embeddingRunningTaskId = null
      this.embeddingBackfillRunning = false
      this.stopEmbeddingPolling()
    },

    startEmbeddingPolling() {
      if (this.embeddingPollTimer) return
      this.embeddingPollTimer = setInterval(() => {
        this.refreshEmbeddingRuntimeState(false, true)
      }, 1500)
    },

    stopEmbeddingPolling() {
      if (this.embeddingPollTimer) {
        clearInterval(this.embeddingPollTimer)
        this.embeddingPollTimer = null
      }
    },

    async loadKnowledgeBaseEmbeddingStatus(showError = false, background = false) {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      if (!background) {
        this.loading.embeddingStatus = true
      }
      try {
        const res = await adminKnowledgeGovernanceService.fetchKnowledgeBaseEmbeddingStatus(this.currentKnowledgeBase.id)
        this.kbEmbeddingStatus = normalizeEmbeddingStatus(this.extractResponseData(res) || {})
      } catch (error) {
        if (showError) {
          this.$message.error(this.extractResponseMessage(error, 'Failed to load knowledge base embedding status'))
        }
      } finally {
        if (!background) {
          this.loading.embeddingStatus = false
        }
      }
    },

    async refreshEmbeddingRuntimeState(showError = false, background = true) {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      await this.loadKnowledgeBaseEmbeddingStatus(showError, background)
      try {
        await this.loadIndexTasks('knowledgeBase', { silent: true, background: true })
      } catch (error) {
      }
      if (this.embeddingCompletionRate >= 100) {
        this.embeddingBackfillRunning = false
        this.embeddingRunningTaskId = null
        this.stopEmbeddingPolling()
      }
    },

    manualRefreshEmbeddingStatus() {
      this.refreshEmbeddingRuntimeState(true, false)
    },

    async backfillCurrentKnowledgeBaseEmbeddings() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('Please select a knowledge base first')
        return
      }
      if (this.embeddingBackfillRunning) {
        this.$message.warning('Embedding backfill is running; please try later')
        this.startEmbeddingPolling()
        return
      }
      if (!this.isKnowledgeBaseEmbeddingConfigured(this.currentKnowledgeBase)) {
        this.$message.warning('Embedding provider/model is not configured for current knowledge base')
        return
      }

      this.embeddingBackfillSubmitting = true
      this.embeddingBackfillRunning = true
      this.startEmbeddingPolling()

      try {
        await adminKnowledgeGovernanceService.backfillKnowledgeBaseEmbeddings(this.currentKnowledgeBase.id, {
          provider: this.currentKnowledgeBase.embeddingProvider,
          modelName: this.currentKnowledgeBase.embeddingModel
        })
        await this.refreshEmbeddingRuntimeState(false, true)
        await this.loadDocumentEmbeddingStatuses()
        this.$message.success('Knowledge base embedding backfill completed')
      } catch (error) {
        const status = error && error.response ? Number(error.response.status || 0) : 0
        if (status === 409) {
          this.$message.warning(this.extractResponseMessage(error, 'Knowledge base embedding backfill task is already running'))
          await this.refreshEmbeddingRuntimeState(false, true)
          return
        }
        this.embeddingBackfillRunning = false
        this.stopEmbeddingPolling()
        this.$message.error(this.extractResponseMessage(error, 'Knowledge base embedding backfill failed'))
      } finally {
        this.embeddingBackfillSubmitting = false
        if (!this.embeddingBackfillRunning) {
          this.stopEmbeddingPolling()
        }
      }
    },

    async runDebugSearch() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('Please select a knowledge base first')
        return
      }
      const query = String((this.debugForm && this.debugForm.query) || '').trim()
      if (!query) {
        this.$message.warning('Please input debug query')
        return
      }

      this.retrievalDrawerVisible = true
      this.currentRetrievalMeta = `Debug retrieval: ${query}`
      this.loading.debugSearch = true
      try {
        const res = await adminKnowledgeGovernanceService.debugSearch(this.currentKnowledgeBase.id, {
          query,
          topK: Number(this.debugForm.topK || this.currentKnowledgeBase.defaultTopK || 5) || 5
        })
        const payload = this.extractResponseData(res) || {}
        this.retrievalLogs = normalizeSources(payload.hits || [], {
          knowledgeBaseId: this.currentKnowledgeBase.id
        })
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, 'Debug search failed'))
      } finally {
        this.loading.debugSearch = false
      }
    },

    async openRetrievalDrawer(callLogId, title) {
      if (!callLogId) {
        this.$message.warning('缺少 callLogId')
        return
      }
      this.retrievalDrawerVisible = true
      this.currentRetrievalMeta = title || `Retrieval logs #${callLogId}`
      this.loading.retrievals = true
      try {
        const res = await adminKnowledgeGovernanceService.fetchRetrievals(callLogId)
        this.retrievalLogs = normalizeSources(this.extractListData(res), { callLogId })
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, 'Failed to load retrieval logs'))
      } finally {
        this.loading.retrievals = false
      }
    },

    handleGovernancePlaceholder(action) {
      const label = action && action.label ? action.label : 'Governance action'
      this.$message.info(`${label} is pending backend wiring`)
    }
  }
}

