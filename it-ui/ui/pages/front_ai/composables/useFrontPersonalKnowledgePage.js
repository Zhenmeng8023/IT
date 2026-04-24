import useKnowledgeBasePage from '@/pages/ai/composables/useKnowledgeBasePage'
import { frontKnowledgeBaseService } from '@/pages/front_ai/services/frontKnowledgeBaseService'
import {
  normalizeKnowledgeBase,
  normalizeDocument,
  normalizeMember,
  normalizeChunk,
  normalizeEmbeddingStatus
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
    showMemberTab() {
      return this.canViewCurrentMembers
    }
  },
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

    canSelectKnowledgeBase(row) {
      if (!row || !row.id) return false
      const scopeType = String(row.scopeType || '').toUpperCase()
      return scopeType === 'PERSONAL' || scopeType === 'PROJECT'
    },

    async loadKnowledgeBases() {
      this.loading.kbList = true
      try {
        const res = await frontKnowledgeBaseService.fetchKnowledgeBases({
          listMode: 'owner',
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
        this.$message.error(this.extractResponseMessage(error, '加载知识库失败'))
      } finally {
        this.loading.kbList = false
      }
    },

    async selectKnowledgeBase(item, options = {}) {
      if (!item || !item.id) return false
      let target = normalizeKnowledgeBase(item)
      if (!this.canSelectKnowledgeBase(target)) {
        this.$message.warning('当前个人中心不允许访问该知识库')
        return false
      }

      try {
        if (options.reloadBase !== false) {
          const res = await frontKnowledgeBaseService.fetchKnowledgeBaseDetail(item.id)
          target = normalizeKnowledgeBase(this.extractResponseData(res) || item)
          if (!this.canSelectKnowledgeBase(target)) {
            this.$message.warning('当前个人中心不允许访问该知识库')
            return false
          }
        }
        return useKnowledgeBasePage.methods.selectKnowledgeBase.call(this, target, {
          ...options,
          reloadBase: false
        })
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '加载知识库详情失败'))
        return false
      }
    },

    async submitKbForm() {
      if (this.kbDialogMode === 'edit' && !this.ensureCanEditCurrentKnowledgeBase('编辑知识库')) return
      if (this.kbDialogMode !== 'edit' && !this.ensureCanCreateKnowledgeBase()) return

      this.loading.saveKb = true
      try {
        const payload = frontKnowledgeBaseService.normalizeKnowledgeBaseEmbeddingPayload({ ...this.kbForm })
        if ((payload.embeddingProvider && !payload.embeddingModel) || (!payload.embeddingProvider && payload.embeddingModel)) {
          this.$message.warning('Embedding Provider 和 Embedding Model 需要同时配置')
          return
        }

        const res = await frontKnowledgeBaseService.saveKnowledgeBase(this.kbDialogMode, payload, this.kbForm.id)
        const saved = normalizeKnowledgeBase(this.extractResponseData(res) || {})
        const savedId = saved.id || this.kbForm.id || null
        if (savedId) {
          this.persistKnowledgeBaseDefaultModel(savedId, payload.defaultModelId || null)
        }
        if (this.currentKnowledgeBase && Number(this.currentKnowledgeBase.id) === Number(savedId || this.currentKnowledgeBase.id)) {
          this.$set(this.currentKnowledgeBase, 'defaultModelId', payload.defaultModelId || null)
          this.useKnowledgeBaseDefaultModel(false)
        }

        this.$message.success(this.kbDialogMode === 'edit' ? '知识库已更新' : '知识库已创建')
        this.kbDialogVisible = false
        await this.loadKnowledgeBases()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '保存知识库失败'))
      } finally {
        this.loading.saveKb = false
      }
    },

    async loadDocuments() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      this.loading.documents = true
      try {
        const res = await frontKnowledgeBaseService.fetchDocuments(
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
        this.$message.error(this.extractResponseMessage(error, '加载文档失败'))
      } finally {
        this.loading.documents = false
      }
    },

    async submitDocumentForm() {
      if (!this.ensureCanEditCurrentKnowledgeBase('保存文档')) return
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }

      this.loading.saveDocument = true
      try {
        await frontKnowledgeBaseService.addDocument(this.currentKnowledgeBase.id, { ...this.documentForm })
        this.$message.success('文档已保存')
        this.documentDialogVisible = false
        await this.loadDocuments()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '保存文档失败'))
      } finally {
        this.loading.saveDocument = false
      }
    },

    async loadIndexTasks() {
      this.indexTasks = []
      this.taskDrawerVisible = false
      this.stopTaskPolling()
    },

    async loadKnowledgeBaseEmbeddingStatus() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.kbEmbeddingStatus = createEmptyEmbeddingStatus()
        return
      }
      try {
        const res = await frontKnowledgeBaseService.fetchKnowledgeBaseEmbeddingStatus(this.currentKnowledgeBase.id)
        this.kbEmbeddingStatus = normalizeEmbeddingStatus(this.extractResponseData(res) || {})
      } catch (error) {
        this.kbEmbeddingStatus = createEmptyEmbeddingStatus()
      }
    },

    async loadMembers() {
      if (!this.canViewCurrentMembers) {
        this.members = []
        return
      }
      this.loading.members = true
      try {
        const res = await frontKnowledgeBaseService.fetchMembers(this.currentKnowledgeBase.id)
        this.members = this.extractListData(res).map(item => normalizeMember(item))
      } catch (error) {
        this.$message.error(this.resolveForbiddenMessage('membersRead', error, '加载成员失败'))
      } finally {
        this.loading.members = false
      }
    },

    async refreshEmbeddingRuntimeState() {
      this.embeddingBackfillRunning = false
      this.embeddingRunningTaskId = null
      this.stopEmbeddingPolling()
      if (!this.documentEmbeddingStatusMap || typeof this.documentEmbeddingStatusMap !== 'object') {
        this.documentEmbeddingStatusMap = {}
      }
      await this.loadKnowledgeBaseEmbeddingStatus()
    },

    async loadDocumentEmbeddingStatuses() {
      const docs = Array.isArray(this.documents) ? this.documents.slice() : []
      if (!docs.length) {
        this.documentEmbeddingStatusMap = {}
        return
      }
      const entries = await Promise.all(docs.map(async item => {
        try {
          const res = await frontKnowledgeBaseService.fetchDocumentEmbeddingStatus(item.id)
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

    hasProcessingDocuments() {
      return Array.isArray(this.documents) &&
        this.documents.some(item => PROCESSING_DOCUMENT_STATUSES.includes(String((item && item.status) || '').toUpperCase()))
    },

    async pollDocumentIndexing(options = {}) {
      const attempts = Number(options.attempts || 12)
      const interval = Number(options.interval || 2000)
      for (let index = 0; index < attempts; index += 1) {
        await this.loadDocuments()
        if (!this.hasProcessingDocuments()) {
          return
        }
        await sleep(interval)
      }
      await this.loadDocuments()
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
        const res = await frontKnowledgeBaseService.fetchImportTask(taskId)
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
        this.$message.warning('请先选择知识库')
        return
      }
      if (!files.length) return

      this.loading.saveDocument = true
      try {
        await frontKnowledgeBaseService.uploadDocuments(this.currentKnowledgeBase.id, files)
        this.$message.success('文件已接收，正在索引')
        await this.loadDocuments()
        void this.pollDocumentIndexing()
      } catch (error) {
        this.$message.error(this.resolveForbiddenMessage('upload', error, '文件上传失败'))
      } finally {
        this.loading.saveDocument = false
      }
    },

    async uploadZipFile(file) {
      if (!this.ensureCanEditCurrentKnowledgeBase('导入 ZIP')) return
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }
      if (!file) return

      const lowerName = String(file.name || '').toLowerCase()
      if (!lowerName.endsWith('.zip')) {
        this.$message.warning('只能上传 ZIP 文件')
        return
      }

      this.loading.documents = true
      try {
        const res = await frontKnowledgeBaseService.uploadZip(this.currentKnowledgeBase.id, file)
        const task = this.extractResponseData(res) || {}
        this.$message.success('ZIP 已接收，正在导入并索引')
        const finalTask = await this.pollImportTask(task.id)
        if (finalTask && String(finalTask.status || '').toUpperCase() === 'FAILED') {
          this.$message.warning(finalTask.errorMessage || 'ZIP 导入任务失败')
        } else if (finalTask && String(finalTask.status || '').toUpperCase() === 'CANCELLED') {
          this.$message.warning('ZIP 导入任务已取消')
        } else {
          void this.pollDocumentIndexing()
        }
      } catch (error) {
        this.$message.error(this.resolveForbiddenMessage('upload', error, 'ZIP 项目导入失败'))
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
        const res = await frontKnowledgeBaseService.fetchDocumentChunks(row.id)
        const chunks = this.extractListData(res).map(item => normalizeChunk(item))
        if (!chunks.length) {
          this.$message.info('当前文档还没有可用切片，请先等待索引完成')
          return
        }
        this.chunks = chunks
        this.chunkDialogVisible = true
      } catch (error) {
        this.$message.error(this.resolveForbiddenMessage('viewChunks', error, '加载切片失败'))
      } finally {
        this.loading.chunks = false
      }
    },

    async downloadDocument(row) {
      if (!row || !row.id) return
      try {
        const result = await frontKnowledgeBaseService.downloadDocument(row.id)
        downloadBlob(result.blob, result.fileName || row.title || `document-${row.id}`)
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '下载文档失败'))
      }
    },

    async downloadCurrentDocumentsZip() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }
      if (!this.documents.length) {
        this.$message.warning('当前没有可下载文档')
        return
      }
      try {
        const ids = this.documents.map(item => item.id).filter(Boolean)
        const result = await frontKnowledgeBaseService.downloadDocumentsZip(this.currentKnowledgeBase.id, ids)
        downloadBlob(result.blob, result.fileName || `${this.currentKnowledgeBase.name || 'knowledge-base'}.zip`)
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '打包下载失败'))
      }
    },

    previewChunks() {
      this.$message.info('前台个人知识库中心不提供切片预览功能')
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
