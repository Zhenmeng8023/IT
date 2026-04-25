import useKnowledgeBasePage from '@/pages/ai/composables/useKnowledgeBasePage'
import { adminPlatformKnowledgeBaseService } from '@/pages/ai/services/adminPlatformKnowledgeBaseService'
import {
  normalizeKnowledgeBase,
  normalizeDocument,
  normalizeChunk,
  normalizeTask,
  normalizeEmbeddingStatus,
  normalizeSources
} from '@/pages/ai/services/knowledgeBaseDomain'

const PROCESSING_DOCUMENT_STATUSES = ['UPLOADED', 'PENDING', 'UPLOADING', 'PROCESSING', 'INDEXING']
const TERMINAL_IMPORT_TASK_STATUSES = ['SUCCESS', 'FAILED', 'CANCELLED']

function createEmptyEmbeddingStatus() {
  return { totalChunkCount: 0, embeddedChunkCount: 0, createdEmbeddingCount: 0 }
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
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

function downloadBlob(blob, fileName) {
  if (typeof window === 'undefined') return
  const targetBlob = blob instanceof Blob ? blob : new Blob([blob])
  const url = URL.createObjectURL(targetBlob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName || 'download'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

export default {
  mixins: [useKnowledgeBasePage],

  computed: {
    canCreateKnowledgeBase() {
      return this.hasAdminKnowledgeAuthority() || this.hasLegacyKnowledgeAuthority()
    },

    canViewCurrentMembers() {
      return false
    }
  },

  methods: {
    initRouteContext() {
      const changed =
        this.routeProjectId !== null ||
        this.projectId !== null ||
        this.listMode !== 'platform' ||
        this.chatForm.projectId !== null ||
        this.chatForm.bizType !== 'GENERAL' ||
        this.chatForm.sceneCode !== 'knowledge.base'

      this.routeProjectId = null
      this.projectId = null
      this.listMode = 'platform'
      this.chatForm.projectId = null
      this.chatForm.bizType = 'GENERAL'
      this.chatForm.sceneCode = 'knowledge.base'
      return changed
    },

    handleListModeChange() {
      this.pagination.page = 0
      this.loadKnowledgeBases()
    },

    canSelectKnowledgeBase(row) {
      if (!row || !row.id) return false
      return String(row.scopeType || '').toUpperCase() === 'PLATFORM'
    },

    getDefaultKbForm() {
      return {
        ...useKnowledgeBasePage.methods.getDefaultKbForm.call(this),
        scopeType: 'PLATFORM',
        projectId: null,
        sourceType: 'MANUAL'
      }
    },

    async loadKnowledgeBases() {
      this.loading.kbList = true
      try {
        const res = await adminPlatformKnowledgeBaseService.fetchKnowledgeBases({
          page: this.pagination.page,
          size: this.pagination.size
        })

        const pageData = this.extractPageData(res)
        this.knowledgeBases = (pageData.content || [])
          .map(item => normalizeKnowledgeBase(item))
          .filter(item => this.canSelectKnowledgeBase(item))
        this.pagination.total = pageData.total || 0

        const currentId = this.currentKnowledgeBase && this.currentKnowledgeBase.id
        const routeKnowledgeBaseId = parseRouteKnowledgeBaseId(this.$route)
        let target = currentId
          ? this.knowledgeBases.find(item => item.id === currentId) || null
          : null

        if (!target && routeKnowledgeBaseId) {
          target = this.knowledgeBases.find(item => String(item.id) === String(routeKnowledgeBaseId)) || null
        }

        if (!target && routeKnowledgeBaseId) {
          try {
            const detailRes = await adminPlatformKnowledgeBaseService.fetchKnowledgeBaseDetail(routeKnowledgeBaseId)
            const detail = normalizeKnowledgeBase(this.extractResponseData(detailRes) || {})
            target = this.canSelectKnowledgeBase(detail) ? detail : null
          } catch (error) {
            target = null
          }
        }

        if (!target && this.knowledgeBases.length) {
          target = this.knowledgeBases[0]
        }

        if (!target) {
          this.resetCurrentKnowledgeBase()
          return
        }

        if (!this.currentKnowledgeBase || Number(this.currentKnowledgeBase.id) !== Number(target.id)) {
          await this.selectKnowledgeBase(target, { reloadBase: true })
        } else {
          this.currentKnowledgeBase = target
        }
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, 'Failed to load knowledge bases'))
      } finally {
        this.loading.kbList = false
      }
    },

    async selectKnowledgeBase(item, options = {}) {
      if (!item || !item.id) return false
      let target = normalizeKnowledgeBase(item)

      if (!this.canSelectKnowledgeBase(target)) {
        this.$message.warning('Current page only supports platform knowledge bases')
        return false
      }

      try {
        if (options.reloadBase !== false) {
          const res = await adminPlatformKnowledgeBaseService.fetchKnowledgeBaseDetail(item.id)
          target = normalizeKnowledgeBase(this.extractResponseData(res) || item)
          if (!this.canSelectKnowledgeBase(target)) {
            this.$message.warning('Current page only supports platform knowledge bases')
            return false
          }
        }

        return useKnowledgeBasePage.methods.selectKnowledgeBase.call(this, target, {
          ...options,
          reloadBase: false
        })
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, 'Failed to load knowledge base detail'))
        return false
      }
    },

    async submitKbForm() {
      if (this.kbDialogMode === 'edit' && !this.ensureCanEditCurrentKnowledgeBase('编辑知识库')) return
      if (this.kbDialogMode !== 'edit' && !this.ensureCanCreateKnowledgeBase()) return

      this.loading.saveKb = true
      try {
        const payload = adminPlatformKnowledgeBaseService.normalizeKnowledgeBaseEmbeddingPayload({
          ...this.kbForm,
          scopeType: 'PLATFORM',
          projectId: null
        })
        if ((payload.embeddingProvider && !payload.embeddingModel) || (!payload.embeddingProvider && payload.embeddingModel)) {
          this.$message.warning('Embedding Provider and Embedding Model must be configured together')
          return
        }

        const res = await adminPlatformKnowledgeBaseService.saveKnowledgeBase(this.kbDialogMode, payload, this.kbForm.id)
        const saved = normalizeKnowledgeBase(this.extractResponseData(res) || {})
        const savedId = saved.id || this.kbForm.id || null
        if (savedId) {
          this.persistKnowledgeBaseDefaultModel(savedId, payload.defaultModelId || null)
        }

        if (this.currentKnowledgeBase && Number(this.currentKnowledgeBase.id) === Number(savedId || this.currentKnowledgeBase.id)) {
          this.$set(this.currentKnowledgeBase, 'defaultModelId', payload.defaultModelId || null)
          this.useKnowledgeBaseDefaultModel(false)
        }

        this.$message.success(this.kbDialogMode === 'edit' ? 'Knowledge base updated' : 'Knowledge base created')
        this.kbDialogVisible = false
        await this.loadKnowledgeBases()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, 'Failed to save knowledge base'))
      } finally {
        this.loading.saveKb = false
      }
    },

    openProjectUploadGuide() {
      if (this.currentKnowledgeBase && this.currentKnowledgeBase.id) {
        this.openDocumentDialog()
        return
      }
      this.openKbDialog('create')
    },

    async loadDocuments() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      this.loading.documents = true
      try {
        const res = await adminPlatformKnowledgeBaseService.fetchDocuments(
          this.currentKnowledgeBase.id,
          this.documentPagination.page,
          this.documentPagination.size
        )
        const pageData = this.extractPageData(res)
        this.documents = (pageData.content || []).map(item => normalizeDocument(item))
        this.documentPagination.total = pageData.total || 0
        await this.loadDocumentEmbeddingStatuses()
        this.locateRouteDocument()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, 'Failed to load documents'))
      } finally {
        this.loading.documents = false
      }
    },

    async submitDocumentForm() {
      if (!this.ensureCanEditCurrentKnowledgeBase('保存文档')) return
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('Please select a knowledge base first')
        return
      }

      this.loading.saveDocument = true
      try {
        await adminPlatformKnowledgeBaseService.addDocument(this.currentKnowledgeBase.id, { ...this.documentForm })
        this.$message.success('Document saved')
        this.documentDialogVisible = false
        await this.loadDocuments()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, 'Failed to save document'))
      } finally {
        this.loading.saveDocument = false
      }
    },

    async pollImportTask(taskId, options = {}) {
      const attempts = Number(options.attempts || 60)
      const interval = Number(options.interval || 2000)
      if (!taskId) {
        await this.loadDocuments()
        return null
      }

      let latestTask = null
      for (let index = 0; index < attempts; index += 1) {
        const res = await adminPlatformKnowledgeBaseService.fetchImportTask(taskId)
        latestTask = this.extractResponseData(res) || {}
        const status = String((latestTask && latestTask.status) || '').toUpperCase()
        if (TERMINAL_IMPORT_TASK_STATUSES.includes(status)) {
          await this.loadDocuments()
          return latestTask
        }
        await sleep(interval)
      }

      await this.loadDocuments()
      return latestTask
    },

    async uploadSelectedDocuments(files = []) {
      if (!this.ensureCanEditCurrentKnowledgeBase('上传文件')) return
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('Please select a knowledge base first')
        return
      }
      if (!files.length) return

      this.loading.saveDocument = true
      try {
        await adminPlatformKnowledgeBaseService.uploadDocuments(this.currentKnowledgeBase.id, files)
        this.$message.success('Files accepted and indexing started')
        await this.loadDocuments()
        void this.pollDocumentIndexing()
      } catch (error) {
        this.$message.error(this.resolveForbiddenMessage('upload', error, 'Failed to upload files'))
      } finally {
        this.loading.saveDocument = false
      }
    },

    async uploadZipFile(file) {
      if (!this.ensureCanEditCurrentKnowledgeBase('导入 ZIP')) return
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('Please select a knowledge base first')
        return
      }
      if (!file) return

      const lowerName = String(file.name || '').toLowerCase()
      if (!lowerName.endsWith('.zip')) {
        this.$message.warning('Only ZIP files are supported')
        return
      }

      this.loading.documents = true
      try {
        const res = await adminPlatformKnowledgeBaseService.uploadZip(this.currentKnowledgeBase.id, file)
        const task = this.extractResponseData(res) || {}
        this.$message.success(this.extractResponseMessage(res, 'ZIP import accepted'))
        const finalTask = await this.pollImportTask(task.id)
        if (finalTask && String(finalTask.status || '').toUpperCase() === 'FAILED') {
          this.$message.warning(finalTask.errorMessage || 'ZIP import task failed')
        } else if (finalTask && String(finalTask.status || '').toUpperCase() === 'CANCELLED') {
          this.$message.warning('ZIP import task cancelled')
        } else {
          void this.pollDocumentIndexing()
        }
      } catch (error) {
        this.$message.error(this.resolveForbiddenMessage('upload', error, 'Failed to import ZIP'))
      } finally {
        this.loading.documents = false
      }
    },

    async viewChunks(row) {
      if (!row || !row.id) return
      this.activeChunkDocument = row
      this.loading.chunks = true
      this.chunks = []
      try {
        const res = await adminPlatformKnowledgeBaseService.fetchDocumentChunks(row.id)
        const chunks = this.extractListData(res).map(item => normalizeChunk(item))
        if (!chunks.length) {
          this.$message.info('No chunks are available yet, please wait for indexing')
          return
        }
        this.chunks = chunks
        this.chunkDialogVisible = true
      } catch (error) {
        this.$message.error(this.resolveForbiddenMessage('viewChunks', error, 'Failed to load chunks'))
      } finally {
        this.loading.chunks = false
      }
    },

    async previewChunks(row) {
      if (!row || !row.id) return
      this.loading.chunks = true
      try {
        const res = await adminPlatformKnowledgeBaseService.previewDocumentChunks(row.id)
        const chunks = this.extractListData(res).map(item => normalizeChunk(item))
        if (!chunks.length) {
          this.$message.info('No chunk preview available')
          return
        }
        this.activeChunkDocument = row
        this.chunks = chunks
        this.chunkDialogVisible = true
      } catch (error) {
        this.$message.error(this.resolveForbiddenMessage('previewChunks', error, 'Failed to preview chunks'))
      } finally {
        this.loading.chunks = false
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
          const res = await adminPlatformKnowledgeBaseService.fetchDocumentEmbeddingStatus(item.id)
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

    async downloadDocument(row) {
      if (!row || !row.id) return
      try {
        const result = await adminPlatformKnowledgeBaseService.downloadDocument(row.id)
        downloadBlob(result.blob, result.fileName || row.title || `document-${row.id}`)
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, 'Failed to download document'))
      }
    },

    async downloadCurrentDocumentsZip() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('Please select a knowledge base first')
        return
      }
      if (!this.documents.length) {
        this.$message.warning('No documents to download')
        return
      }
      try {
        const ids = this.documents.map(item => item.id).filter(Boolean)
        const result = await adminPlatformKnowledgeBaseService.downloadDocumentsZip(this.currentKnowledgeBase.id, ids)
        downloadBlob(result.blob, result.fileName || `${this.currentKnowledgeBase.name || 'knowledge-base'}.zip`)
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, 'Failed to download ZIP'))
      }
    },

    async loadMembers() {
      this.members = []
    },

    hasProcessingDocuments() {
      return Array.isArray(this.documents) &&
        this.documents.some(item => PROCESSING_DOCUMENT_STATUSES.includes(String((item && item.status) || '').toUpperCase()))
    },

    async reindexKnowledgeBase(row) {
      if (!this.ensureCanEditCurrentKnowledgeBase('执行索引重建')) return
      const kb = row || this.currentKnowledgeBase
      if (!kb || !kb.id) return
      try {
        await adminPlatformKnowledgeBaseService.createIndexTask(kb.id, {})
        this.$message.success('Knowledge base index task submitted')
        this.openKnowledgeBaseTasks()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, 'Failed to submit index task'))
      }
    },

    async reindexDocument(row) {
      if (!this.ensureCanEditCurrentKnowledgeBase('执行索引重建')) return
      if (!row || !row.id || !this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      try {
        await adminPlatformKnowledgeBaseService.createIndexTask(this.currentKnowledgeBase.id, { documentId: row.id })
        this.$message.success('Document index task submitted')
        this.viewIndexTasks(row)
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, 'Failed to submit document index task'))
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
          res = await adminPlatformKnowledgeBaseService.fetchDocumentTasks(this.currentTaskDocument.id)
        } else {
          res = await adminPlatformKnowledgeBaseService.fetchKnowledgeBaseTasks(this.currentKnowledgeBase.id)
        }
        this.indexTasks = this.extractListData(res).map(item => normalizeTask(item))
        if (scope !== 'document') {
          this.syncEmbeddingTaskState(this.indexTasks)
        }
      } catch (error) {
        if (!silent) {
          this.$message.error(this.extractResponseMessage(error, 'Failed to load index tasks'))
        }
      } finally {
        if (!background) {
          this.loading.tasks = false
        }
      }
    },

    async loadKnowledgeBaseEmbeddingStatus(showError = false, background = false) {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.kbEmbeddingStatus = createEmptyEmbeddingStatus()
        return
      }
      if (!background) {
        this.loading.embeddingStatus = true
      }
      try {
        const res = await adminPlatformKnowledgeBaseService.fetchKnowledgeBaseEmbeddingStatus(this.currentKnowledgeBase.id)
        this.kbEmbeddingStatus = normalizeEmbeddingStatus(this.extractResponseData(res) || {})
      } catch (error) {
        if (showError) {
          this.$message.error(this.extractResponseMessage(error, 'Failed to load embedding status'))
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

    async backfillCurrentKnowledgeBaseEmbeddings() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('Please select a knowledge base first')
        return
      }
      if (!this.ensureCanEditCurrentKnowledgeBase('执行向量回填')) return
      if (this.embeddingBackfillRunning) {
        this.$message.warning('Embedding backfill is already running')
        this.startEmbeddingPolling()
        return
      }
      if (!this.isKnowledgeBaseEmbeddingConfigured(this.currentKnowledgeBase)) {
        this.$message.warning('Please configure embedding provider and model first')
        return
      }

      this.embeddingBackfillSubmitting = true
      this.embeddingBackfillRunning = true
      this.startEmbeddingPolling()

      try {
        await adminPlatformKnowledgeBaseService.backfillKnowledgeBaseEmbeddings(this.currentKnowledgeBase.id, {
          provider: this.currentKnowledgeBase.embeddingProvider,
          modelName: this.currentKnowledgeBase.embeddingModel
        })
        await this.refreshEmbeddingRuntimeState(false, true)
        await this.loadDocumentEmbeddingStatuses()
        this.$message.success('Knowledge base embedding backfill completed')
      } catch (error) {
        const status = error && error.response ? Number(error.response.status || 0) : 0
        if (status === 409) {
          this.$message.warning(this.extractResponseMessage(error, 'Embedding backfill task is already running'))
          await this.refreshEmbeddingRuntimeState(false, true)
          return
        }
        this.embeddingBackfillRunning = false
        this.stopEmbeddingPolling()
        this.$message.error(this.extractResponseMessage(error, 'Failed to backfill embeddings'))
      } finally {
        this.embeddingBackfillSubmitting = false
        if (!this.embeddingBackfillRunning) {
          this.stopEmbeddingPolling()
        }
      }
    },

    async backfillDocumentVector(row) {
      if (!row || !row.id) return
      if (!this.ensureCanEditCurrentKnowledgeBase('执行向量回填')) return
      if (!this.isKnowledgeBaseEmbeddingConfigured(this.currentKnowledgeBase)) {
        this.$message.warning('Please configure embedding provider and model first')
        return
      }
      try {
        await adminPlatformKnowledgeBaseService.backfillDocumentEmbeddings(row.id, {
          provider: this.currentKnowledgeBase.embeddingProvider,
          modelName: this.currentKnowledgeBase.embeddingModel
        })
        this.$message.success('Document embedding backfill completed')
        await this.loadKnowledgeBaseEmbeddingStatus(false)
        await this.loadDocumentEmbeddingStatuses()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, 'Failed to backfill document embeddings'))
      }
    },

    async runChatDebugSearch() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('Please select a knowledge base first')
        return
      }
      const query = String((this.chatForm && this.chatForm.content) || '').trim()
      if (!query) {
        this.$message.warning('Please enter a question before debug search')
        return
      }

      this.retrievalDrawerVisible = true
      this.currentRetrievalMeta = `Debug Search: ${query}`
      this.loading.debugSearch = true
      try {
        const res = await adminPlatformKnowledgeBaseService.debugSearch(this.currentKnowledgeBase.id, {
          query,
          topK: (this.currentKnowledgeBase && this.currentKnowledgeBase.defaultTopK) || 5
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
    }
  }
}
