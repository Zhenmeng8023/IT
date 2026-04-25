import {
  pageAllKnowledgeBases,
  getKnowledgeBase,
  freezeAdminKnowledgeBase,
  archiveAdminKnowledgeBase,
  deleteAdminKnowledgeBase,
  pageKnowledgeDocuments,
  createKnowledgeIndexTask,
  listKnowledgeBaseIndexTasks,
  listDocumentIndexTasks,
  backfillKnowledgeBaseEmbeddings,
  backfillDocumentEmbeddings,
  getKnowledgeBaseEmbeddingStatus,
  getDocumentEmbeddingStatus,
  searchKnowledgeBaseDebug,
  listCallRetrievals
} from '@/api/knowledgeBase'

export const adminKnowledgeGovernanceService = {
  fetchKnowledgeBases({ ownerId, projectId, scopeType, page, size }) {
    return pageAllKnowledgeBases({
      ownerId,
      projectId,
      scopeType,
      page,
      size
    })
  },

  fetchKnowledgeBaseDetail(id) {
    return getKnowledgeBase(id)
  },

  freezeKnowledgeBase(id) {
    return freezeAdminKnowledgeBase(id)
  },

  archiveKnowledgeBase(id) {
    return archiveAdminKnowledgeBase(id)
  },

  deleteKnowledgeBase(id) {
    return deleteAdminKnowledgeBase(id)
  },

  fetchDocuments(knowledgeBaseId, page, size) {
    return pageKnowledgeDocuments(knowledgeBaseId, { page, size })
  },

  fetchKnowledgeBaseTasks(knowledgeBaseId) {
    return listKnowledgeBaseIndexTasks(knowledgeBaseId)
  },

  fetchDocumentTasks(documentId) {
    return listDocumentIndexTasks(documentId)
  },

  createIndexTask(knowledgeBaseId, payload = {}) {
    return createKnowledgeIndexTask(knowledgeBaseId, payload)
  },

  fetchKnowledgeBaseEmbeddingStatus(knowledgeBaseId) {
    return getKnowledgeBaseEmbeddingStatus(knowledgeBaseId)
  },

  fetchDocumentEmbeddingStatus(documentId) {
    return getDocumentEmbeddingStatus(documentId)
  },

  backfillKnowledgeBaseEmbeddings(knowledgeBaseId, payload) {
    return backfillKnowledgeBaseEmbeddings(knowledgeBaseId, payload)
  },

  backfillDocumentEmbeddings(documentId, payload) {
    return backfillDocumentEmbeddings(documentId, payload)
  },

  debugSearch(knowledgeBaseId, payload) {
    return searchKnowledgeBaseDebug(knowledgeBaseId, payload)
  },

  fetchRetrievals(callLogId) {
    return listCallRetrievals(callLogId)
  }
}
