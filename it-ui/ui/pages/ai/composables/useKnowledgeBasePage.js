import {
  readBrowserPermissionCodes,
  extractResponseData as readResponseData,
  extractResponseMessage as readResponseMessage,
  extractPageData as readPageData,
  extractListData as readListData,
  readKnowledgeBaseDefaultModel,
  persistKnowledgeBaseDefaultModel,
  readPersistedSessionId,
  persistSessionId,
  persistCurrentKnowledgeBaseToAssistant,
  normalizeKnowledgeBase,
  normalizeDocument,
  normalizeMember,
  normalizeChunk,
  normalizeTask,
  normalizeSession,
  normalizeSources,
  extractAnswerSources,
  normalizeMessage,
  normalizeEmbeddingStatus,
  getEmbeddingCompletionRate,
  formatTime,
  kbStatusTagType,
  docStatusTagType,
  taskStatusTagType,
  readStoredUserInfo
} from '@/pages/ai/services/knowledgeBaseDomain'
import { knowledgeBaseService } from '@/pages/ai/services/knowledgeBaseService'

function createDefaultKbForm(vm) {
  return {
    scopeType: vm.routeProjectId ? 'PROJECT' : 'PERSONAL',
    projectId: vm.routeProjectId || null,
    ownerId: vm.ownerId || null,
    defaultModelId: vm.activeModel && vm.activeModel.id ? vm.activeModel.id : null,
    name: '',
    description: '',
    sourceType: vm.routeProjectId ? 'PROJECT_DOC' : 'MANUAL',
    embeddingProvider: '',
    embeddingModel: '',
    chunkStrategy: 'PARAGRAPH',
    defaultTopK: 5,
    visibility: 'PRIVATE'
  }
}

function createDefaultDocumentForm(vm) {
  return {
    sourceType: vm.routeProjectId ? 'PROJECT_DOC' : 'MANUAL',
    sourceRefId: null,
    title: '',
    contentText: '',
    contentHash: null
  }
}

function createDefaultMemberForm() {
  return {
    userId: null,
    roleCode: 'EDITOR'
  }
}

function downloadBlob(blob, fileName) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName || 'download.bin'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
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

function parseRouteDocumentId(route) {
  if (!route) return null
  const raw =
    (route.query && route.query.documentId) ||
    (route.params && route.params.documentId) ||
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
      listMode: 'owner',
      keyword: '',
      ownerId: null,
      projectId: null,
      routeProjectId: null,

      loading: {
        kbList: false,
        saveKb: false,
        documents: false,
        saveDocument: false,
        members: false,
        saveMember: false,
        chunks: false,
        tasks: false,
        chat: false,
        streamChat: false,
        retrievals: false,
        models: false,
        embeddingStatus: false,
        debugSearch: false
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

      sessionPagination: {
        page: 0,
        size: 10,
        total: 0
      },

      knowledgeBases: [],
      currentKnowledgeBase: null,
      activeTab: 'documents',

      documents: [],
      members: [],
      chunks: [],
      indexTasks: [],
      sessions: [],

      kbDialogVisible: false,
      kbDialogMode: 'create',
      kbForm: {},

      documentDialogVisible: false,
      documentImportMode: 'manual',
      documentForm: {},
      selectedLocalFileName: '',
      fileReadError: '',

      memberDialogVisible: false,
      memberForm: {},

      chunkDialogVisible: false,
      taskDrawerVisible: false,
      taskDrawerScope: 'knowledgeBase',
      activeChunkDocument: null,
      currentTaskDocument: null,
      taskPollTimer: null,
      embeddingPollTimer: null,
      embeddingBackfillRunning: false,
      embeddingBackfillSubmitting: false,
      embeddingRunningTaskId: null,

      retrievalDrawerVisible: false,
      retrievalLogs: [],
      currentRetrievalMeta: '',
      kbEmbeddingStatus: { totalChunkCount: 0, embeddedChunkCount: 0, createdEmbeddingCount: 0 },
      documentEmbeddingStatusMap: {},

      chatForm: {
        sessionId: null,
        userId: null,
        content: '',
        modelId: null,
        promptTemplateId: null,
        requestType: 'KNOWLEDGE_QA',
        bizType: 'GENERAL',
        bizId: null,
        projectId: null,
        sceneCode: 'knowledge.base',
        memoryMode: 'SHORT'
      },

      chatMessages: [],
      selectedSessionId: null,
      streamStopper: null,
      sessionKeyword: '',
      sessionLoading: false,
      enabledModels: [],
      activeModel: null,

      kbRules: {
        name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }],
        scopeType: [{ required: true, message: '请选择作用域', trigger: 'change' }]
      },

      documentRules: {
        title: [{ required: true, message: '请输入文档标题', trigger: 'blur' }],
        contentText: [{ required: true, message: '请输入文档正文', trigger: 'blur' }]
      },

      memberRules: {
        userId: [{ required: true, message: '请输入用户 ID', trigger: 'change' }],
        roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }]
      }
    }
  },

  computed: {
    currentUserIdNumber() {
      return Number(this.ownerId || this.chatForm.userId || 0) || null
    },

    currentMemberRoleCode() {
      const uid = this.currentUserIdNumber
      if (!uid || !this.currentKnowledgeBase) return ''
      if (Number(this.currentKnowledgeBase.ownerId || 0) === uid) return 'OWNER'
      const hit = (this.members || []).find(item => Number(item.userId || 0) === uid)
      return hit ? String(hit.roleCode || '') : ''
    },

    canReadCurrentKnowledgeBase() {
      return !!this.currentKnowledgeBase && this.hasAuthority('view:knowledge-base')
    },

    canCreateKnowledgeBase() {
      return true
    },

    canEditCurrentKnowledgeBase() {
      return true
    },

    canManageCurrentMembers() {
      return true
    },

    showProjectCreateGuide() {
      return !!this.routeProjectId &&
        this.listMode === 'project' &&
        !this.loading.kbList &&
        !this.currentKnowledgeBase &&
        (!this.knowledgeBases.length || Number(this.pagination.total || 0) === 0)
    },

    filteredKnowledgeBases() {
      if (!this.keyword) return this.knowledgeBases
      const key = String(this.keyword).toLowerCase()
      return this.knowledgeBases.filter(item => {
        if (!item) return false
        return (
          String(item.name || '').toLowerCase().includes(key) ||
          String(item.description || '').toLowerCase().includes(key)
        )
      })
    },

    filteredSessions() {
      if (!this.sessionKeyword) return this.sessions
      const key = String(this.sessionKeyword).toLowerCase()
      return this.sessions.filter(item => {
        if (!item) return false
        return (
          String(item.title || '').toLowerCase().includes(key) ||
          String(item.id || '').toLowerCase().includes(key)
        )
      })
    },

    effectiveChatModelId() {
      return this.chatForm.modelId ||
        (this.currentKnowledgeBase && this.currentKnowledgeBase.defaultModelId) ||
        (this.activeModel && this.activeModel.id) ||
        null
    },

    effectiveChatModelName() {
      const model = this.findModelById(this.effectiveChatModelId)
      return model ? this.buildModelLabel(model) : '未选择'
    },

    currentKnowledgeBaseDefaultModelName() {
      const model = this.findModelById(this.currentKnowledgeBase && this.currentKnowledgeBase.defaultModelId)
      return model ? this.buildModelLabel(model) : '跟随系统当前模型'
    },

    embeddingEnabledModels() {
      return (this.enabledModels || []).filter(item => {
        if (!item) return false
        const flag = item.supportsEmbedding
        return flag === true || Number(flag) === 1 || String(flag).toLowerCase() === 'true'
      })
    },

    embeddingProviderOptions() {
      const set = new Set()
      this.embeddingEnabledModels.forEach(item => {
        const code = String(item.providerCode || '').trim()
        if (code) set.add(code)
      })
      return Array.from(set)
    },

    embeddingModelOptions() {
      const provider = String(this.kbForm.embeddingProvider || '').trim().toLowerCase()
      return this.embeddingEnabledModels
        .filter(item => {
          const code = String(item.providerCode || '').trim().toLowerCase()
          return !provider || code === provider
        })
        .map(item => ({
          value: String(item.modelName || '').trim(),
          label: this.buildModelLabel(item)
        }))
        .filter(item => item.value)
    },

    embeddingConfigured() {
      return this.isKnowledgeBaseEmbeddingConfigured(this.currentKnowledgeBase)
    },

    embeddingCompletionRate() {
      return getEmbeddingCompletionRate(this.kbEmbeddingStatus)
    },

    embeddingButtonText() {
      if (this.embeddingBackfillRunning) {
        return `回填中 ${this.embeddingCompletionRate}%`
      }
      return '向量回填'
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
    activeTab(value) {
      if (!this.currentKnowledgeBase) return
      if (value === 'documents') this.loadDocuments()
      if (value === 'members') this.loadMembers()
      if (value === 'chat') this.loadSessions()
    },

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
    this.kbForm = createDefaultKbForm(this)
    this.documentForm = createDefaultDocumentForm(this)
    this.memberForm = createDefaultMemberForm()
    this.initRouteContext()
    this.initUserId()
    this.loadModels()
    this.loadKnowledgeBases()
  },

  beforeDestroy() {
    this.stopTaskPolling()
    this.stopEmbeddingPolling()
    this.stopStream()
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
      if (!status) return '未统计'
      const total = Number(status.totalChunkCount || 0)
      const embedded = Number(status.embeddedChunkCount || 0)
      if (!total) return '无切片'
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
      return true
    },

    allowRouteKnowledgeBaseFallback() {
      return true
    },

    canSelectKnowledgeBase() {
      return true
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

    roleOfKnowledgeBase(row) {
      if (!row) return ''
      const uid = this.currentUserIdNumber
      if (!uid) return ''
      if (Number(row.ownerId || 0) === uid) return 'OWNER'
      if (this.currentKnowledgeBase && Number(this.currentKnowledgeBase.id || 0) === Number(row.id || 0)) {
        return this.currentMemberRoleCode
      }
      return ''
    },

    ensureCanCreateKnowledgeBase() {
      if (this.canCreateKnowledgeBase) return true
      this.$message.warning('你没有知识库访问权限')
      return false
    },

    ensureCanEditCurrentKnowledgeBase(action = '执行该操作') {
      if (this.canEditCurrentKnowledgeBase) return true
      this.$message.warning(`只有知识库 OWNER / EDITOR 才能${action}`)
      return false
    },

    ensureCanManageCurrentMembers(action = '管理成员') {
      if (this.canManageCurrentMembers) return true
      this.$message.warning(`只有知识库 OWNER 才能${action}`)
      return false
    },

    getDefaultKbForm() {
      return createDefaultKbForm(this)
    },

    getDefaultDocumentForm() {
      return createDefaultDocumentForm(this)
    },

    getDefaultMemberForm() {
      return createDefaultMemberForm()
    },

    readKnowledgeBaseDefaultModel(kbId) {
      return readKnowledgeBaseDefaultModel(kbId)
    },

    persistKnowledgeBaseDefaultModel(kbId, modelId) {
      persistKnowledgeBaseDefaultModel(kbId, modelId)
    },

    findModelById(id) {
      const targetId = Number(id) || null
      if (!targetId) return null
      return this.enabledModels.find(item => Number(item.id) === targetId) ||
        (this.activeModel && Number(this.activeModel.id) === targetId ? this.activeModel : null) ||
        null
    },

    buildModelLabel(model) {
      if (!model) return '未命名模型'
      const name = model.modelName || model.name || model.label || `模型 #${model.id}`
      const provider = model.providerCode || model.modelType || ''
      return provider ? `${name}（${provider}）` : name
    },

    handleEmbeddingProviderChange(value) {
      const provider = String(value || '').trim().toLowerCase()
      if (!provider) {
        this.kbForm.embeddingModel = ''
        return
      }
      const currentModel = String(this.kbForm.embeddingModel || '').trim()
      const matched = this.embeddingModelOptions.find(item => item.value === currentModel)
      if (!matched) {
        const first = this.embeddingModelOptions[0]
        this.kbForm.embeddingModel = first ? first.value : ''
      }
    },

    isKnowledgeBaseEmbeddingConfigured(row) {
      if (!row) return false
      return !!String(row.embeddingProvider || '').trim() && !!String(row.embeddingModel || '').trim()
    },

    async loadModels(showMessage = false) {
      this.loading.models = true
      try {
        const enabledRes = await knowledgeBaseService.fetchModels()
        const enabledList = this.extractListData(enabledRes)
        this.enabledModels = enabledList
          .map(item => ({ ...item, id: Number(item.id) || item.id }))
          .filter(item => item && item.id)

        this.activeModel = this.enabledModels.length ? this.enabledModels[0] : null
        if (this.kbDialogVisible && !this.kbForm.embeddingProvider && this.embeddingProviderOptions.length) {
          this.kbForm.embeddingProvider = this.embeddingProviderOptions[0]
          this.handleEmbeddingProviderChange(this.kbForm.embeddingProvider)
        }

        if (this.currentKnowledgeBase && this.currentKnowledgeBase.id) {
          const persistedDefault = this.readKnowledgeBaseDefaultModel(this.currentKnowledgeBase.id)
          if (persistedDefault && !this.currentKnowledgeBase.defaultModelId) {
            this.$set(this.currentKnowledgeBase, 'defaultModelId', persistedDefault)
          }
          this.useKnowledgeBaseDefaultModel(false)
        } else if (!this.chatForm.modelId && this.activeModel && this.activeModel.id) {
          this.chatForm.modelId = this.activeModel.id
        }

        if (showMessage) this.$message.success('模型列表已刷新')
      } catch (error) {
        this.enabledModels = []
        this.activeModel = null
        if (showMessage) {
          this.$message.error(this.extractResponseMessage(error, '加载模型列表失败'))
        }
      } finally {
        this.loading.models = false
      }
    },

    refreshModels() {
      this.loadModels(true)
    },

    useKnowledgeBaseDefaultModel(showMessage = true) {
      const modelId = (this.currentKnowledgeBase && this.currentKnowledgeBase.defaultModelId) || (this.activeModel && this.activeModel.id) || null
      this.chatForm.modelId = modelId
      if (!showMessage) return
      this.$message.success(modelId ? '已切换到当前知识库默认模型' : '当前没有可用模型')
    },

    useActiveModel(showMessage = true) {
      const modelId = this.activeModel && this.activeModel.id ? this.activeModel.id : null
      this.chatForm.modelId = modelId
      if (!showMessage) return
      this.$message.success(modelId ? '已切换到系统当前模型' : '当前没有系统默认模型')
    },

    initRouteContext() {
      const previousRouteProjectId = this.routeProjectId
      const routeProjectId = parseRouteProjectId(this.$route)
      const changed = !isSameProjectId(previousRouteProjectId, routeProjectId)
      if (!routeProjectId) {
        this.routeProjectId = null
        if (this.chatForm.projectId && isSameProjectId(this.chatForm.projectId, previousRouteProjectId)) {
          this.chatForm.projectId = null
        }
        if (this.chatForm.bizType === 'PROJECT') {
          this.chatForm.bizType = 'GENERAL'
        }
        if (this.chatForm.sceneCode === 'project.knowledge-base') {
          this.chatForm.sceneCode = 'knowledge.base'
        }

        if (
          this.listMode === 'project' &&
          previousRouteProjectId &&
          isSameProjectId(this.projectId, previousRouteProjectId)
        ) {
          this.projectId = null
          this.listMode = 'owner'
        }
        return changed
      }

      this.routeProjectId = routeProjectId
      this.projectId = routeProjectId
      this.listMode = 'project'
      this.chatForm.projectId = routeProjectId
      this.chatForm.bizType = 'PROJECT'
      this.chatForm.sceneCode = 'project.knowledge-base'
      return changed
    },

    initUserId() {
      const user = readStoredUserInfo()
      const uid = user && (user.id || user.userId)
      if (!uid) return
      this.ownerId = uid
      this.chatForm.userId = uid
      this.kbForm.ownerId = uid
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

    handleSessionPageChange(page) {
      this.sessionPagination.page = page - 1
      this.loadSessions(false)
    },

    async loadKnowledgeBases() {
      this.loading.kbList = true
      try {
        if (this.listMode === 'project' && !this.projectId) {
          this.knowledgeBases = []
          this.pagination.total = 0
          return
        }
        if (this.listMode === 'owner' && !this.ownerId) {
          this.knowledgeBases = []
          this.pagination.total = 0
          return
        }

        const res = await knowledgeBaseService.fetchKnowledgeBases({
          listMode: this.listMode,
          ownerId: this.ownerId,
          projectId: this.projectId,
          page: this.pagination.page,
          size: this.pagination.size
        })

        const pageData = this.extractPageData(res)
        this.knowledgeBases = (pageData.content || [])
          .map(item => normalizeKnowledgeBase(item))
          .filter(item => this.canSelectKnowledgeBase(item))
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
        if (!target && routeKnowledgeBaseId && this.allowRouteKnowledgeBaseFallback()) {
          try {
            const detailRes = await knowledgeBaseService.fetchKnowledgeBaseDetail(routeKnowledgeBaseId)
            const detail = normalizeKnowledgeBase(this.extractResponseData(detailRes) || {})
            target = this.canSelectKnowledgeBase(detail) ? detail : null
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

        if (!target) {
          this.resetCurrentKnowledgeBase()
          return
        }
        const selected = await this.selectKnowledgeBase(target, { reloadBase: false })
        if (!selected) {
          this.resetCurrentKnowledgeBase()
        }
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '加载知识库失败'))
      } finally {
        this.loading.kbList = false
      }
    },

    resetCurrentKnowledgeBase() {
      this.currentKnowledgeBase = null
      persistCurrentKnowledgeBaseToAssistant(null)
      this.documents = []
      this.members = []
      this.indexTasks = []
      this.sessions = []
      this.chatMessages = []
      this.selectedSessionId = null
      this.chatForm.sessionId = null
      this.documentPagination.total = 0
      this.sessionPagination.total = 0
      this.kbEmbeddingStatus = { totalChunkCount: 0, embeddedChunkCount: 0, createdEmbeddingCount: 0 }
      this.documentEmbeddingStatusMap = {}
    },

    async selectKnowledgeBase(item, options = {}) {
      if (!item || !item.id) return false
      const normalized = normalizeKnowledgeBase(item)
      if (!this.canSelectKnowledgeBase(normalized)) {
        this.$message.warning('当前项目上下文不允许访问该知识库')
        return false
      }
      const reloadBase = options.reloadBase !== false

      try {
        if (reloadBase) {
          const res = await knowledgeBaseService.fetchKnowledgeBaseDetail(item.id)
          const detail = normalizeKnowledgeBase(this.extractResponseData(res) || item)
          if (!this.canSelectKnowledgeBase(detail)) {
            this.$message.warning('当前项目上下文不允许访问该知识库')
            return false
          }
          this.currentKnowledgeBase = detail
        } else {
          this.currentKnowledgeBase = normalized
        }

        const persistedDefaultModelId = this.readKnowledgeBaseDefaultModel(this.currentKnowledgeBase.id)
        if (persistedDefaultModelId && !this.currentKnowledgeBase.defaultModelId) {
          this.$set(this.currentKnowledgeBase, 'defaultModelId', persistedDefaultModelId)
        }

        persistCurrentKnowledgeBaseToAssistant(this.currentKnowledgeBase)
        this.useKnowledgeBaseDefaultModel(false)

        this.documentPagination.page = 0
        this.sessionPagination.page = 0
        this.createNewSession(false)

        if (this.activeTab === 'documents') {
          await this.loadDocuments()
        } else if (this.activeTab === 'members') {
          await this.loadMembers()
        } else if (this.activeTab === 'chat') {
          await this.loadSessions()
        }

        await this.loadMembers(true)
        await this.loadIndexTasks('knowledgeBase', { silent: true, background: true })
        await this.refreshEmbeddingRuntimeState(false, true)
        return true
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '加载知识库详情失败'))
        return false
      }
    },

    openKbDialog(mode, row) {
      if (mode === 'create' && !this.ensureCanCreateKnowledgeBase()) return
      if (mode === 'edit' && !this.canEditKnowledgeBaseItem(row || this.currentKnowledgeBase)) {
        this.$message.warning('只有知识库 OWNER / EDITOR 才能编辑知识库')
        return
      }

      const normalized = row ? normalizeKnowledgeBase(row) : null
      this.kbDialogMode = mode
      this.kbForm = mode === 'edit' && normalized
        ? {
            ...this.getDefaultKbForm(),
            ...normalized,
            defaultModelId: normalized.defaultModelId || this.readKnowledgeBaseDefaultModel(normalized.id) || null
          }
        : this.getDefaultKbForm()
      this.kbDialogVisible = true
    },

    async submitKbForm() {
      if (this.kbDialogMode === 'edit' && !this.ensureCanEditCurrentKnowledgeBase('编辑知识库')) return
      if (this.kbDialogMode !== 'edit' && !this.ensureCanCreateKnowledgeBase()) return

      this.loading.saveKb = true
      try {
        const payload = knowledgeBaseService.normalizeKnowledgeBaseEmbeddingPayload({ ...this.kbForm })
        if ((payload.embeddingProvider && !payload.embeddingModel) || (!payload.embeddingProvider && payload.embeddingModel)) {
          this.$message.warning('Embedding Provider 和 Embedding Model 需要同时配置')
          return
        }

        const res = await knowledgeBaseService.saveKnowledgeBase(this.kbDialogMode, payload, this.kbForm.id)
        const saved = this.extractResponseData(res) || {}
        const savedId = saved.id || this.kbForm.id || null
        if (savedId) {
          this.persistKnowledgeBaseDefaultModel(savedId, payload.defaultModelId || null)
        }
        if (
          this.currentKnowledgeBase &&
          this.currentKnowledgeBase.id &&
          Number(this.currentKnowledgeBase.id) === Number(savedId || this.currentKnowledgeBase.id)
        ) {
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
        const res = await knowledgeBaseService.fetchDocuments(
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

    openProjectUploadGuide() {
      if (this.currentKnowledgeBase && this.currentKnowledgeBase.id) {
        this.openDocumentDialog()
        return
      }
      this.$message.info('请先为该项目创建知识库，再上传资料')
      this.openKbDialog('create')
    },

    openDocumentDialog() {
      if (!this.ensureCanEditCurrentKnowledgeBase('新增文档')) return
      this.documentForm = this.getDefaultDocumentForm()
      this.documentImportMode = 'manual'
      this.selectedLocalFileName = ''
      this.fileReadError = ''
      this.documentDialogVisible = true
    },

    async submitDocumentForm() {
      if (!this.ensureCanEditCurrentKnowledgeBase('保存文档')) return
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }

      this.loading.saveDocument = true
      try {
        await knowledgeBaseService.addDocument(this.currentKnowledgeBase.id, { ...this.documentForm })
        this.$message.success('文档已保存')
        this.documentDialogVisible = false
        await this.loadDocuments()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '保存文档失败'))
      } finally {
        this.loading.saveDocument = false
      }
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
        await knowledgeBaseService.uploadDocuments(this.currentKnowledgeBase.id, files)
        this.$message.success('文件上传成功')
        await this.loadDocuments()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '文件上传失败'))
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
        const res = await knowledgeBaseService.uploadZip(this.currentKnowledgeBase.id, file, {
          sourceType: 'PROJECT_DOC'
        })
        this.$message.success((res && (res.message || res.msg)) || 'ZIP 项目导入成功')
        await this.loadDocuments()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, 'ZIP 项目导入失败'))
      } finally {
        this.loading.documents = false
      }
    },

    importLocalTextFile(file) {
      if (!this.ensureCanEditCurrentKnowledgeBase('导入本地文本')) return
      if (!file) return
      this.fileReadError = ''
      const reader = new FileReader()
      reader.onload = event => {
        const content = event && event.target ? event.target.result : ''
        this.documentForm = {
          ...this.getDefaultDocumentForm(),
          title: file.name,
          contentText: typeof content === 'string' ? content : ''
        }
        this.selectedLocalFileName = file.name
        this.documentImportMode = 'manual'
        this.documentDialogVisible = true
      }
      reader.onerror = () => {
        this.fileReadError = '读取本地文本失败'
        this.$message.error('读取本地文本失败')
      }
      reader.readAsText(file, 'utf-8')
    },

    async viewChunks(row) {
      if (!row || !row.id) return
      this.activeChunkDocument = row
      this.chunkDialogVisible = true
      this.loading.chunks = true
      try {
        const res = await knowledgeBaseService.fetchDocumentChunks(row.id)
        const chunks = this.extractListData(res).map(item => normalizeChunk(item))
        if (chunks.length) {
          this.chunks = chunks
        } else {
          const previewRes = await knowledgeBaseService.previewDocumentChunks(row.id)
          this.chunks = this.extractListData(previewRes).map(item => normalizeChunk(item))
        }
      } catch (error) {
        try {
          const previewRes = await knowledgeBaseService.previewDocumentChunks(row.id)
          this.chunks = this.extractListData(previewRes).map(item => normalizeChunk(item))
        } catch (innerError) {
          this.$message.error(this.extractResponseMessage(innerError, '加载切片失败'))
        }
      } finally {
        this.loading.chunks = false
      }
    },

    async previewChunks(row) {
      if (!row || !row.id) return
      this.activeChunkDocument = row
      this.chunkDialogVisible = true
      this.loading.chunks = true
      try {
        const res = await knowledgeBaseService.previewDocumentChunks(row.id)
        this.chunks = this.extractListData(res).map(item => normalizeChunk(item))
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '切片预览失败'))
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
          const res = await knowledgeBaseService.fetchDocumentEmbeddingStatus(item.id)
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
        const result = await knowledgeBaseService.downloadDocument(row.id)
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
        this.$message.warning('当前页没有可下载文档')
        return
      }
      try {
        const ids = this.documents.map(item => item.id).filter(Boolean)
        const result = await knowledgeBaseService.downloadDocumentsZip(this.currentKnowledgeBase.id, ids)
        downloadBlob(result.blob, result.fileName || `${this.currentKnowledgeBase.name || 'knowledge-base'}.zip`)
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '打包下载失败'))
      }
    },

    seedChatQuestion(row) {
      if (!row) return
      this.activeTab = 'chat'
      this.chatForm.content = `请基于文档《${row.title || `文档 #${row.id}`}》进行总结，并给出命中文档与切片来源。`
      this.$nextTick(() => this.scrollChatToBottom())
    },

    async loadMembers(silent = false) {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      this.loading.members = true
      try {
        const res = await knowledgeBaseService.fetchMembers(this.currentKnowledgeBase.id)
        this.members = this.extractListData(res).map(item => normalizeMember(item))
      } catch (error) {
        if (!silent) {
          this.$message.error(this.extractResponseMessage(error, '加载成员失败'))
        }
      } finally {
        this.loading.members = false
      }
    },

    openMemberDialog() {
      if (!this.ensureCanManageCurrentMembers('添加成员')) return
      this.memberForm = this.getDefaultMemberForm()
      this.memberDialogVisible = true
    },

    async submitMemberForm() {
      if (!this.ensureCanManageCurrentMembers('添加成员')) return
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return false
      }

      this.loading.saveMember = true
      try {
        await knowledgeBaseService.addMember(this.currentKnowledgeBase.id, { ...this.memberForm })
        this.$message.success('成员已添加')
        this.memberDialogVisible = false
        await this.loadMembers()
        return true
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '添加成员失败'))
        return false
      } finally {
        this.loading.saveMember = false
      }
    },

    async removeMember(row) {
      if (!this.ensureCanManageCurrentMembers('移除成员')) return
      if (!row || !row.id || !this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      try {
        await this.$confirm(`确定移除成员 ${row.userId} 吗？`, '提示', { type: 'warning' })
        await knowledgeBaseService.removeMember(this.currentKnowledgeBase.id, row.id)
        this.$message.success('成员已移除')
        await this.loadMembers()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(this.extractResponseMessage(error, '移除成员失败'))
        }
      }
    },

    async reindexKnowledgeBase(row) {
      if (!this.ensureCanEditCurrentKnowledgeBase('执行索引重建')) return
      const kb = row || this.currentKnowledgeBase
      if (!kb || !kb.id) return
      try {
        await knowledgeBaseService.createIndexTask(kb.id, {})
        this.$message.success('已提交知识库索引任务')
        this.openKnowledgeBaseTasks()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '提交索引任务失败'))
      }
    },

    async reindexDocument(row) {
      if (!this.ensureCanEditCurrentKnowledgeBase('执行索引重建')) return
      if (!row || !row.id || !this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      try {
        await knowledgeBaseService.createIndexTask(this.currentKnowledgeBase.id, { documentId: row.id })
        this.$message.success('已提交文档索引任务')
        this.viewIndexTasks(row)
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '提交文档索引任务失败'))
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
          res = await knowledgeBaseService.fetchDocumentTasks(this.currentTaskDocument.id)
        } else {
          res = await knowledgeBaseService.fetchKnowledgeBaseTasks(this.currentKnowledgeBase.id)
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
        const res = await knowledgeBaseService.fetchKnowledgeBaseEmbeddingStatus(this.currentKnowledgeBase.id)
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
      if (!this.ensureCanEditCurrentKnowledgeBase('执行向量回填')) return
      if (this.embeddingBackfillRunning) {
        this.$message.warning('当前知识库正在执行向量回填，请等待完成后再重试')
        this.startEmbeddingPolling()
        return
      }
      if (!this.isKnowledgeBaseEmbeddingConfigured(this.currentKnowledgeBase)) {
        this.$message.warning('当前知识库还没有配置 Embedding Provider 和 Model')
        return
      }

      this.embeddingBackfillSubmitting = true
      this.embeddingBackfillRunning = true
      this.startEmbeddingPolling()

      try {
        await knowledgeBaseService.backfillKnowledgeBaseEmbeddings(this.currentKnowledgeBase.id, {
          provider: this.currentKnowledgeBase.embeddingProvider,
          modelName: this.currentKnowledgeBase.embeddingModel
        })
        await this.refreshEmbeddingRuntimeState(false, true)
        await this.loadDocumentEmbeddingStatuses()
        this.$message.success('知识库向量回填完成')
      } catch (error) {
        const status = error && error.response ? Number(error.response.status || 0) : 0
        if (status === 409) {
          this.$message.warning(this.extractResponseMessage(error, '当前知识库已有向量回填任务正在执行'))
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

    async backfillDocumentVector(row) {
      if (!row || !row.id) return
      if (!this.ensureCanEditCurrentKnowledgeBase('执行向量回填')) return
      if (!this.isKnowledgeBaseEmbeddingConfigured(this.currentKnowledgeBase)) {
        this.$message.warning('当前知识库还没有配置 Embedding Provider 和 Model')
        return
      }
      try {
        await knowledgeBaseService.backfillDocumentEmbeddings(row.id, {
          provider: this.currentKnowledgeBase.embeddingProvider,
          modelName: this.currentKnowledgeBase.embeddingModel
        })
        this.$message.success('文档向量回填完成')
        await this.loadKnowledgeBaseEmbeddingStatus(false)
        await this.loadDocumentEmbeddingStatuses()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '文档向量回填失败'))
      }
    },

    createNewSession(showMessage = true) {
      this.stopStream()
      this.selectedSessionId = null
      this.chatForm.sessionId = null
      this.chatMessages = []
      persistSessionId(this.currentKnowledgeBase && this.currentKnowledgeBase.id, this.chatForm.userId || this.ownerId, null)
      if (showMessage) {
        this.$message.success('已切换为新会话')
      }
    },

    async loadSessions(autoRestore = true) {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      this.sessionLoading = true
      try {
        const res = await knowledgeBaseService.fetchSessions({
          page: this.sessionPagination.page,
          size: this.sessionPagination.size,
          userId: this.chatForm.userId || this.ownerId,
          bizType: this.chatForm.bizType || undefined,
          knowledgeBaseId: this.currentKnowledgeBase.id,
          status: 'ACTIVE'
        })
        const pageData = this.extractPageData(res)
        this.sessions = (pageData.content || [])
          .map(item => normalizeSession(item))
          .filter(item => item && item.id && !item.archived)
        this.sessionPagination.total = pageData.total || 0

        if (!autoRestore) return

        const rememberedId = readPersistedSessionId(this.currentKnowledgeBase.id, this.chatForm.userId || this.ownerId)
        const targetId = this.selectedSessionId || rememberedId
        if (!targetId) return

        const hit = this.sessions.find(item => item.id === targetId)
        if (hit) {
          await this.selectSession(hit, true)
        } else if (this.selectedSessionId === targetId) {
          this.createNewSession(false)
        }
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '加载会话失败'))
      } finally {
        this.sessionLoading = false
      }
    },

    async selectSession(item, silent = false) {
      if (!item || !item.id) return
      this.selectedSessionId = item.id
      this.chatForm.sessionId = item.id
      persistSessionId(this.currentKnowledgeBase && this.currentKnowledgeBase.id, this.chatForm.userId || this.ownerId, item.id)
      await this.loadSessionMessages(item.id)
      if (!silent) {
        this.$message.success(`已打开会话 #${item.id}`)
      }
    },

    async loadSessionMessages(sessionId) {
      if (!sessionId) return
      try {
        const res = await knowledgeBaseService.fetchSessionMessages(sessionId)
        const pageData = this.extractPageData(res)
        this.chatMessages = (pageData.content || []).map(item => normalizeMessage(item))
        this.scrollChatToBottom()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '加载会话消息失败'))
      }
    },

    async bindCurrentSessionKnowledgeBase(sessionId) {
      if (!sessionId || !this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      try {
        await knowledgeBaseService.bindSessionKnowledgeBases(sessionId, [this.currentKnowledgeBase.id])
      } catch (error) {
      }
    },

    buildChatPayload() {
      const content = String(this.chatForm.content || '').trim()
      return {
        sessionId: this.selectedSessionId || this.chatForm.sessionId || null,
        userId: this.chatForm.userId || this.ownerId,
        content,
        modelId: this.effectiveChatModelId || null,
        promptTemplateId: this.chatForm.promptTemplateId || null,
        requestType: this.chatForm.requestType,
        knowledgeBaseIds: this.currentKnowledgeBase && this.currentKnowledgeBase.id ? [this.currentKnowledgeBase.id] : [],
        bizType: this.chatForm.bizType || 'GENERAL',
        bizId: this.chatForm.bizId || null,
        projectId: this.chatForm.projectId || this.routeProjectId || null,
        sceneCode: this.chatForm.sceneCode || 'knowledge.base',
        memoryMode: this.chatForm.memoryMode || 'SHORT',
        defaultKnowledgeBaseId: this.currentKnowledgeBase ? this.currentKnowledgeBase.id : null,
        sessionTitle: content.slice(0, 30)
      }
    },

    buildUserMessage(content) {
      return {
        id: `u-${Date.now()}`,
        role: 'user',
        content,
        totalTokens: null,
        latencyMs: null,
        modelName: '',
        callLogId: null,
        sources: [],
        sourceOpen: false
      }
    },

    buildAssistantMessage(data = {}) {
      return {
        id: data.assistantMessageId || `a-${Date.now()}`,
        role: 'assistant',
        content: data.content || '',
        totalTokens: data.totalTokens || null,
        latencyMs: data.latencyMs || null,
        modelName: data.modelName || '',
        callLogId: data.callLogId || null,
        finishReason: data.finishReason || '',
        sources: extractAnswerSources(data, { callLogId: data.callLogId || null }),
        sourceOpen: true
      }
    },

    async sendChat(isStream) {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }
      const content = String(this.chatForm.content || '').trim()
      if (!content) {
        this.$message.warning('请输入问题')
        return
      }
      if (!this.effectiveChatModelId) {
        this.$message.warning('请先选择问答模型')
        return
      }
      if (this.loading.chat || this.loading.streamChat) return

      const payload = this.buildChatPayload()
      this.chatMessages.push(this.buildUserMessage(content))
      this.chatForm.content = ''
      this.scrollChatToBottom()

      if (isStream) {
        const assistant = this.buildAssistantMessage({})
        this.chatMessages.push(assistant)
        this.loading.streamChat = true

        const streamTask = knowledgeBaseService.streamChat({
          body: payload,
          onMessage: chunk => {
            const data = this.extractResponseData(chunk) || chunk || {}
            if (typeof data === 'string') {
              assistant.content += data
              this.scrollChatToBottom()
              return
            }
            if (data.sessionId) {
              this.selectedSessionId = data.sessionId
              this.chatForm.sessionId = data.sessionId
              persistSessionId(this.currentKnowledgeBase && this.currentKnowledgeBase.id, this.chatForm.userId || this.ownerId, data.sessionId)
            }
            if (data.delta) assistant.content += data.delta
            if (data.content && !data.delta) assistant.content = data.content
            if (data.modelName) assistant.modelName = data.modelName
            if (data.totalTokens) assistant.totalTokens = data.totalTokens
            if (data.latencyMs) assistant.latencyMs = data.latencyMs
            if (data.callLogId) assistant.callLogId = data.callLogId
            if (data.finishReason) assistant.finishReason = data.finishReason
            if (data.sources || data.citations || data.references || data.retrievals || data.retrievedChunks) {
              assistant.sources = extractAnswerSources(data, { callLogId: data.callLogId || assistant.callLogId || null })
            }
            this.scrollChatToBottom()
          },
          onError: error => {
            this.$message.error(this.extractResponseMessage(error, '流式发送失败'))
          }
        })

        this.streamStopper = streamTask && streamTask.abort ? streamTask.abort : null

        try {
          await streamTask
          if (this.selectedSessionId) {
            await this.bindCurrentSessionKnowledgeBase(this.selectedSessionId)
            await this.loadSessions(false)
          }
        } catch (error) {
          this.$message.error(this.extractResponseMessage(error, '流式发送失败'))
        } finally {
          this.loading.streamChat = false
          this.streamStopper = null
        }
        return
      }

      this.loading.chat = true
      try {
        const res = await knowledgeBaseService.sendChat(payload)
        const data = this.extractResponseData(res) || {}
        if (data.sessionId) {
          this.selectedSessionId = data.sessionId
          this.chatForm.sessionId = data.sessionId
          persistSessionId(this.currentKnowledgeBase && this.currentKnowledgeBase.id, this.chatForm.userId || this.ownerId, data.sessionId)
        }
        this.chatMessages.push(this.buildAssistantMessage(data))
        if (this.selectedSessionId) {
          await this.bindCurrentSessionKnowledgeBase(this.selectedSessionId)
        }
        await this.loadSessions(false)
        this.scrollChatToBottom()
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '发送失败'))
      } finally {
        this.loading.chat = false
      }
    },

    clearChat() {
      this.chatMessages = []
      this.chatForm.content = ''
    },

    stopStream() {
      if (typeof this.streamStopper === 'function') {
        this.streamStopper()
      }
      this.streamStopper = null
      this.loading.streamChat = false
    },

    async archiveSession(item) {
      if (!item || !item.id) return
      try {
        await this.$confirm(`确定归档会话 #${item.id} 吗？`, '提示', { type: 'warning' })
        await knowledgeBaseService.archiveSession(item.id)
        this.$message.success('会话已归档')
        if (this.selectedSessionId === item.id) {
          this.createNewSession(false)
        }
        await this.loadSessions(false)
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(this.extractResponseMessage(error, '归档会话失败'))
        }
      }
    },

    async removeSession(item) {
      if (!item || !item.id) return
      try {
        await this.$confirm(`确定删除会话 #${item.id} 吗？`, '提示', { type: 'warning' })
        await knowledgeBaseService.removeSession(item.id)
        this.$message.success('会话已删除')
        if (this.selectedSessionId === item.id) {
          this.createNewSession(false)
        }
        await this.loadSessions(false)
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(this.extractResponseMessage(error, '删除会话失败'))
        }
      }
    },

    toggleMessageSources(index) {
      const msg = this.chatMessages[index]
      if (!msg) return
      this.$set(msg, 'sourceOpen', !msg.sourceOpen)
    },

    locateSourceDocument(source) {
      if (!source || !source.documentId) return
      const hit = this.documents.find(item => item.id === source.documentId)
      if (hit) {
        this.viewChunks(hit)
        return
      }
      this.$message.info(`文档 ID: ${source.documentId}`)
    },

    locateRouteDocument() {
      const documentId = parseRouteDocumentId(this.$route)
      if (!documentId || !Array.isArray(this.documents) || !this.documents.length) return
      const hit = this.documents.find(item => String(item.id) === String(documentId))
      if (!hit) return
      this.activeTab = 'documents'
      this.$nextTick(() => this.viewChunks(hit))
    },

    async openRetrievalDrawerByMessage(msg) {
      if (msg && msg.callLogId) {
        await this.openRetrievalDrawer(msg.callLogId, '检索日志')
        return
      }
      this.$message.warning('当前消息没有关联检索日志')
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
        const res = await knowledgeBaseService.fetchRetrievals(callLogId)
        this.retrievalLogs = normalizeSources(this.extractListData(res), { callLogId })
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '加载检索日志失败'))
      } finally {
        this.loading.retrievals = false
      }
    },

    async runChatDebugSearch() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }
      const query = String((this.chatForm && this.chatForm.content) || '').trim()
      if (!query) {
        this.$message.warning('请输入问题后再调试检索')
        return
      }

      this.retrievalDrawerVisible = true
      this.currentRetrievalMeta = `检索调试：${query}`
      this.loading.debugSearch = true
      try {
        const res = await knowledgeBaseService.debugSearch(this.currentKnowledgeBase.id, {
          query,
          topK: (this.currentKnowledgeBase && this.currentKnowledgeBase.defaultTopK) || 5
        })
        const payload = this.extractResponseData(res) || {}
        this.retrievalLogs = normalizeSources(payload.hits || [], {
          knowledgeBaseId: this.currentKnowledgeBase.id
        })
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '检索调试失败'))
      } finally {
        this.loading.debugSearch = false
      }
    },

    scrollChatToBottom() {
      this.$nextTick(() => {
        const chatTab = this.$refs.chatTabRef
        if (chatTab && typeof chatTab.scrollToBottom === 'function') {
          chatTab.scrollToBottom()
        }
      })
    }
  }
}
