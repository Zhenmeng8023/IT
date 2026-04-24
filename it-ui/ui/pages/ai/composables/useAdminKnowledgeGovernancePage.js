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
import { GetOperationLogsPage } from '@/api/adminLog'

const KNOWLEDGE_BASE_SCOPE_LABELS = {
  PLATFORM: '平台',
  PROJECT: '项目',
  PERSONAL: '个人'
}

const KNOWLEDGE_BASE_SOURCE_LABELS = {
  MANUAL: '手动录入',
  PROJECT_DOC: '项目文档',
  UPLOAD: '上传文件',
  ZIP_IMPORT: 'ZIP 导入',
  IMPORT: '导入'
}

const KNOWLEDGE_BASE_VISIBILITY_LABELS = {
  PRIVATE: '私有',
  PUBLIC: '公开',
  INTERNAL: '内部'
}

const KNOWLEDGE_BASE_STATUS_LABELS = {
  ACTIVE: '可用',
  READY: '就绪',
  PENDING: '待处理',
  BUILDING: '构建中',
  INDEXING: '索引中',
  FAILED: '失败',
  DELETED: '已删除'
}

const DOCUMENT_SOURCE_LABELS = {
  MANUAL: '手动录入',
  PROJECT_DOC: '项目文档',
  UPLOAD: '上传文件',
  ZIP_IMPORT: 'ZIP 导入',
  IMPORT: '导入'
}

const DOCUMENT_STATUS_LABELS = {
  READY: '就绪',
  INDEXED: '已索引',
  UPLOADED: '已上传',
  PARSING: '解析中',
  INDEXING: '索引中',
  PENDING: '待处理',
  UPLOADING: '上传中',
  PROCESSING: '处理中',
  FAILED: '失败',
  ERROR: '异常'
}

const TASK_TYPE_LABELS = {
  FULL: '全量索引',
  DOCUMENT: '文档索引',
  REINDEX: '重建索引',
  EMBEDDING: '向量回填',
  EMBEDDING_BACKFILL: '向量回填',
  VECTOR_BACKFILL: '向量回填',
  VECTOR: '向量任务'
}

const TASK_STATUS_LABELS = {
  SUCCESS: '成功',
  DONE: '完成',
  COMPLETED: '完成',
  PENDING: '待处理',
  RUNNING: '执行中',
  FAILED: '失败',
  ERROR: '异常',
  CANCELLED: '已取消'
}

const RETRIEVAL_METHOD_LABELS = {
  KEYWORD: '关键词检索',
  VECTOR: '向量检索',
  HYBRID: '混合检索',
  RERANK: '重排'
}

function formatLabel(value, mapping, fallback = '-') {
  const text = String(value || '').trim()
  if (!text) return fallback
  return mapping[text.toUpperCase()] || text
}

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
        { key: 'freeze-kb', label: '冻结知识库（待实现）' },
        { key: 'archive-kb', label: '归档知识库（待实现）' },
        { key: 'delete-kb', label: '删除知识库（待实现）' },
        { key: 'operation-audit', label: '操作审计（待实现）' }
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
        return `向量回填中 ${this.embeddingCompletionRate}%`
      }
      return '执行向量回填'
    },

    taskDrawerTitle() {
      return this.taskDrawerScope === 'document' ? '文档索引任务' : '知识库索引任务'
    },

    taskDrawerSubtitle() {
      if (this.taskDrawerScope === 'document' && this.currentTaskDocument) {
        return this.currentTaskDocument.title || `文档 #${this.currentTaskDocument.id}`
      }
      return this.currentKnowledgeBase ? this.currentKnowledgeBase.name : '当前知识库'
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

    displayKnowledgeBaseScope(value) {
      return formatLabel(value, KNOWLEDGE_BASE_SCOPE_LABELS)
    },

    displayKnowledgeBaseSourceType(value) {
      return formatLabel(value, KNOWLEDGE_BASE_SOURCE_LABELS)
    },

    displayKnowledgeBaseVisibility(value) {
      return formatLabel(value, KNOWLEDGE_BASE_VISIBILITY_LABELS)
    },

    displayKnowledgeBaseStatus(value) {
      return formatLabel(value, KNOWLEDGE_BASE_STATUS_LABELS)
    },

    displayDocumentSourceType(value) {
      return formatLabel(value, DOCUMENT_SOURCE_LABELS)
    },

    displayDocumentStatus(value) {
      return formatLabel(value, DOCUMENT_STATUS_LABELS, '未知')
    },

    displayTaskType(value) {
      return formatLabel(value, TASK_TYPE_LABELS)
    },

    displayTaskStatus(value) {
      return formatLabel(value, TASK_STATUS_LABELS, '未知')
    },

    displayRetrievalMethod(value) {
      return formatLabel(value, RETRIEVAL_METHOD_LABELS)
    },

    documentEmbeddingLabel(row) {
      const status = this.documentEmbeddingStatusMap[row && row.id]
      if (!status) return '未统计'
      const total = Number(status.totalChunkCount || 0)
      const embedded = Number(status.embeddedChunkCount || 0)
      if (!total) return '暂无切片'
      if (embedded >= total) return `已完成 ${embedded}/${total}`
      return `待补齐 ${embedded}/${total}`
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
        this.$message.error(this.extractResponseMessage(error, '加载知识库列表失败'))
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
        this.$message.error(this.extractResponseMessage(error, '加载知识库详情失败'))
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
        this.$message.success('已提交知识库重建索引任务')
        this.openKnowledgeBaseTasks()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '提交知识库重建索引任务失败'))
      } finally {
        this.loading.governanceAction = false
      }
    },

    async reindexDocument(row) {
      if (!row || !row.id || !this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      this.loading.governanceAction = true
      try {
        await adminKnowledgeGovernanceService.createIndexTask(this.currentKnowledgeBase.id, { documentId: row.id })
        this.$message.success('已提交文档重建索引任务')
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
        this.$message.warning('当前知识库未配置向量提供方和模型')
        return
      }
      this.loading.governanceAction = true
      try {
        await adminKnowledgeGovernanceService.backfillDocumentEmbeddings(row.id, {
          provider: this.currentKnowledgeBase.embeddingProvider,
          modelName: this.currentKnowledgeBase.embeddingModel
        })
        this.$message.success('文档向量回填完成')
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
          this.$message.error(this.extractResponseMessage(error, '加载知识库向量状态失败'))
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
        this.$message.warning('请先选择知识库')
        return
      }
      if (this.embeddingBackfillRunning) {
        this.$message.warning('向量回填正在执行，请稍后重试')
        this.startEmbeddingPolling()
        return
      }
      if (!this.isKnowledgeBaseEmbeddingConfigured(this.currentKnowledgeBase)) {
        this.$message.warning('当前知识库未配置向量提供方和模型')
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
        this.$message.success('知识库向量回填完成')
      } catch (error) {
        const status = error && error.response ? Number(error.response.status || 0) : 0
        if (status === 409) {
          this.$message.warning(this.extractResponseMessage(error, '知识库向量回填任务已在执行中'))
          await this.refreshEmbeddingRuntimeState(false, true)
          return
        }
        this.embeddingBackfillRunning = false
        this.stopEmbeddingPolling()
        this.$message.error(this.extractResponseMessage(error, '知识库向量回填失败'))
      } finally {
        this.embeddingBackfillSubmitting = false
        if (!this.embeddingBackfillRunning) {
          this.stopEmbeddingPolling()
        }
      }
    },

    async runDebugSearch() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }
      const query = String((this.debugForm && this.debugForm.query) || '').trim()
      if (!query) {
        this.$message.warning('请输入调试问题')
        return
      }

      this.retrievalDrawerVisible = true
      this.currentRetrievalMeta = `调试检索：${query}`
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
        this.$message.error(this.extractResponseMessage(error, '调试检索失败'))
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
      this.currentRetrievalMeta = title || `检索日志 #${callLogId}`
      this.loading.retrievals = true
      try {
        const res = await adminKnowledgeGovernanceService.fetchRetrievals(callLogId)
        this.retrievalLogs = normalizeSources(this.extractListData(res), { callLogId })
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '加载检索日志失败'))
      } finally {
        this.loading.retrievals = false
      }
    },

    async freezeCurrentKnowledgeBase() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }
      try {
        await this.$confirm(
          `确认冻结知识库“${this.currentKnowledgeBase.name || this.currentKnowledgeBase.id}”吗？冻结后前台将不再允许继续编辑。`,
          '冻结知识库',
          { type: 'warning' }
        )
      } catch (error) {
        return
      }

      this.loading.governanceAction = true
      try {
        const res = await adminKnowledgeGovernanceService.freezeKnowledgeBase(this.currentKnowledgeBase.id)
        this.currentKnowledgeBase = normalizeKnowledgeBase(this.extractResponseData(res) || this.currentKnowledgeBase)
        this.knowledgeBases = this.knowledgeBases.map(item =>
          item && item.id === this.currentKnowledgeBase.id ? { ...item, status: this.currentKnowledgeBase.status } : item
        )
        this.$message.success(this.extractResponseMessage(res, '知识库已冻结'))
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '冻结知识库失败'))
      } finally {
        this.loading.governanceAction = false
      }
    },

    async archiveCurrentKnowledgeBase() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }
      try {
        await this.$confirm(
          `确认归档知识库“${this.currentKnowledgeBase.name || this.currentKnowledgeBase.id}”吗？归档后将保留数据，但状态会标记为已归档。`,
          '归档知识库',
          { type: 'warning' }
        )
      } catch (error) {
        return
      }

      this.loading.governanceAction = true
      try {
        const res = await adminKnowledgeGovernanceService.archiveKnowledgeBase(this.currentKnowledgeBase.id)
        this.currentKnowledgeBase = normalizeKnowledgeBase(this.extractResponseData(res) || this.currentKnowledgeBase)
        this.knowledgeBases = this.knowledgeBases.map(item =>
          item && item.id === this.currentKnowledgeBase.id ? { ...item, status: this.currentKnowledgeBase.status } : item
        )
        this.$message.success(this.extractResponseMessage(res, '知识库已归档'))
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '归档知识库失败'))
      } finally {
        this.loading.governanceAction = false
      }
    },

    async deleteCurrentKnowledgeBase() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }
      const targetId = this.currentKnowledgeBase.id
      const targetName = this.currentKnowledgeBase.name || targetId
      try {
        await this.$confirm(
          `确认彻底删除知识库“${targetName}”吗？该操作会删除关联文档、索引与向量数据，且不可恢复。`,
          '删除知识库',
          { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
        )
      } catch (error) {
        return
      }

      this.loading.governanceAction = true
      try {
        const res = await adminKnowledgeGovernanceService.deleteKnowledgeBase(targetId)
        this.$message.success(this.extractResponseMessage(res, '知识库已删除'))
        if (this.currentKnowledgeBase && this.currentKnowledgeBase.id === targetId) {
          this.resetCurrentKnowledgeBase()
        }
        await this.loadKnowledgeBases()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '删除知识库失败'))
      } finally {
        this.loading.governanceAction = false
      }
    },

    async showKnowledgeBaseAuditLogs() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }

      const knowledgeBaseId = String(this.currentKnowledgeBase.id)
      this.loading.governanceAction = true
      try {
        const res = await GetOperationLogsPage({ page: 1, size: 100, module: 'ai' })
        const root = res && Object.prototype.hasOwnProperty.call(res, 'data') ? res.data : res
        const items = root && Array.isArray(root.data) ? root.data : []
        const matched = items.filter(item => {
          const action = String((item && item.action) || '')
          const details = item && item.details && typeof item.details === 'object' ? item.details : {}
          const path = String(details.path || '')
          return action.includes(`/knowledge-bases/${knowledgeBaseId}`) || path.includes(`/knowledge-bases/${knowledgeBaseId}`)
        })

        if (!matched.length) {
          this.$message.info('暂未查询到该知识库的操作审计日志')
          return
        }

        const lines = matched.slice(0, 20).map(item => {
          const details = item && item.details && typeof item.details === 'object' ? item.details : {}
          const result = details.result || item.result || '-'
          return [
            `${item.createTime || '-'}  ${item.operator || '-'}  ${item.action || '-'}`,
            `结果：${result}  IP：${item.ip || '-'}`
          ].join('\n')
        })

        const message = `<div style="max-height:420px;overflow:auto;"><pre style="white-space:pre-wrap;margin:0;font-family:monospace;">${this.escapeAuditHtml(lines.join('\n\n'))}</pre></div>`
        this.$alert(message, '操作审计', {
          dangerouslyUseHTMLString: true,
          confirmButtonText: '关闭'
        })
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '加载操作审计失败'))
      } finally {
        this.loading.governanceAction = false
      }
    },

    escapeAuditHtml(value) {
      return String(value || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
    },

    handleGovernancePlaceholder(action) {
      const label = action && action.label ? action.label : '治理动作'
      this.$message.info(`${label}，后端能力尚未接入`)
    }
  }
}
