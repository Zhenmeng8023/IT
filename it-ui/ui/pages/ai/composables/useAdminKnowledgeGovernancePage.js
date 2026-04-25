import {
  extractResponseData as readResponseData,
  extractResponseMessage as readResponseMessage,
  extractPageData as readPageData,
  extractListData as readListData,
  normalizeKnowledgeBase,
  formatTime,
  kbStatusTagType
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
  DELETED: '已删除',
  DISABLED: '已冻结',
  ARCHIVED: '已归档'
}

function formatLabel(value, mapping, fallback = '-') {
  const text = String(value || '').trim()
  if (!text) return fallback
  return mapping[text.toUpperCase()] || text
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

function normalizeIdFilter(value) {
  const text = String(value || '').trim()
  return /^\d+$/.test(text) ? text : ''
}

function parseAuditDetails(raw) {
  if (!raw) return {}
  if (typeof raw === 'object') return raw
  if (typeof raw !== 'string') return {}
  try {
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch (error) {
    return {}
  }
}

function getAuditMatchText(item, details) {
  return [
    item && item.action,
    item && item.target,
    item && item.resource,
    item && item.module,
    details.path,
    details.requestUri,
    details.uri,
    details.url,
    details.endpoint,
    details.api,
    details.message
  ]
    .map(value => String(value || ''))
    .join(' ')
}

function matchKnowledgeBaseAuditRecord(item, details, knowledgeBaseId) {
  const text = getAuditMatchText(item, details)
  const directIdFields = [
    item && item.bizId,
    item && item.targetId,
    details.knowledgeBaseId,
    details.kbId,
    details.targetId,
    details.id
  ]
    .map(value => String(value || '').trim())
    .filter(Boolean)

  if (directIdFields.includes(knowledgeBaseId)) {
    return true
  }

  return [
    `/knowledge-bases/${knowledgeBaseId}`,
    `/platform-knowledge-bases/${knowledgeBaseId}`,
    `knowledgeBaseId=${knowledgeBaseId}`,
    `kbId=${knowledgeBaseId}`
  ].some(token => text.includes(token))
}

export default {
  data() {
    return {
      scopeType: '',
      ownerId: '',
      projectId: '',
      routeKnowledgeBaseId: null,

      loading: {
        kbList: false,
        governanceAction: false
      },

      pagination: {
        page: 0,
        size: 10,
        total: 0
      },

      knowledgeBases: [],
      currentKnowledgeBase: null
    }
  },

  computed: {
    canGovernCurrentKnowledgeBase() {
      return !!this.currentKnowledgeBase
    }
  },

  watch: {
    '$route.fullPath'() {
      const changed = this.initRouteContext()
      if (!changed) return
      this.pagination.page = 0
      this.loadKnowledgeBases()
    }
  },

  mounted() {
    this.initRouteContext()
    this.loadKnowledgeBases()
  },

  methods: {
    formatTime,
    kbStatusTagType,

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

    initRouteContext() {
      const previousRouteKnowledgeBaseId = this.routeKnowledgeBaseId
      this.routeKnowledgeBaseId = parseRouteKnowledgeBaseId(this.$route)
      return String(previousRouteKnowledgeBaseId || '') !== String(this.routeKnowledgeBaseId || '')
    },

    handleFilterSearch() {
      this.pagination.page = 0
      this.loadKnowledgeBases()
    },

    handleFilterReset() {
      this.scopeType = ''
      this.ownerId = ''
      this.projectId = ''
      this.pagination.page = 0
      this.loadKnowledgeBases()
    },

    handleKbPageChange(page) {
      this.pagination.page = page - 1
      this.loadKnowledgeBases()
    },

    resetCurrentKnowledgeBase() {
      this.currentKnowledgeBase = null
    },

    async loadKnowledgeBases() {
      this.loading.kbList = true
      try {
        const res = await adminKnowledgeGovernanceService.fetchKnowledgeBases({
          ownerId: normalizeIdFilter(this.ownerId) || undefined,
          projectId: normalizeIdFilter(this.projectId) || undefined,
          scopeType: String(this.scopeType || '').trim() || undefined,
          page: this.pagination.page,
          size: this.pagination.size
        })

        const pageData = this.extractPageData(res)
        this.knowledgeBases = (pageData.content || []).map(item => normalizeKnowledgeBase(item))
        this.pagination.total = pageData.total || 0

        const currentId = this.currentKnowledgeBase && this.currentKnowledgeBase.id
        let target = null

        if (currentId) {
          target = this.knowledgeBases.find(item => String(item.id) === String(currentId)) || null
        }

        if (!target && this.routeKnowledgeBaseId) {
          target = this.knowledgeBases.find(item => String(item.id) === String(this.routeKnowledgeBaseId)) || null
        }

        if (!target && this.knowledgeBases.length) {
          target = this.knowledgeBases[0]
        }

        if (target) {
          await this.selectKnowledgeBase(target, { reloadBase: true })
        } else {
          this.resetCurrentKnowledgeBase()
        }
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '加载知识库列表失败'))
      } finally {
        this.loading.kbList = false
      }
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
      } catch (error) {
        this.$message.error(this.extractResponseMessage(error, '加载知识库详情失败'))
        this.currentKnowledgeBase = normalizeKnowledgeBase(item)
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
        const updated = normalizeKnowledgeBase(this.extractResponseData(res) || this.currentKnowledgeBase)
        this.currentKnowledgeBase = updated
        this.knowledgeBases = this.knowledgeBases.map(item =>
          item && String(item.id) === String(updated.id)
            ? { ...item, status: updated.status }
            : item
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
          `确认归档知识库“${this.currentKnowledgeBase.name || this.currentKnowledgeBase.id}”吗？归档后数据保留，但状态会标记为已归档。`,
          '归档知识库',
          { type: 'warning' }
        )
      } catch (error) {
        return
      }

      this.loading.governanceAction = true
      try {
        const res = await adminKnowledgeGovernanceService.archiveKnowledgeBase(this.currentKnowledgeBase.id)
        const updated = normalizeKnowledgeBase(this.extractResponseData(res) || this.currentKnowledgeBase)
        this.currentKnowledgeBase = updated
        this.knowledgeBases = this.knowledgeBases.map(item =>
          item && String(item.id) === String(updated.id)
            ? { ...item, status: updated.status }
            : item
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
        if (this.currentKnowledgeBase && String(this.currentKnowledgeBase.id) === String(targetId)) {
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
        const res = await GetOperationLogsPage({ page: 1, size: 200 })
        const items = this.extractListData(res)
        const matched = items.filter(item => {
          const details = parseAuditDetails(item && item.details)
          return matchKnowledgeBaseAuditRecord(item, details, knowledgeBaseId)
        })

        if (!matched.length) {
          this.$message.info('未查询到该知识库的操作审计日志')
          return
        }

        const lines = matched.slice(0, 20).map(item => {
          const details = parseAuditDetails(item && item.details)
          const result = details.result || item.result || '-'
          return [
            `${item.createTime || '-'}  ${item.operator || '-'}  ${item.action || '-'}`,
            `结果: ${result}  IP: ${item.ip || '-'}`
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
    }
  }
}
