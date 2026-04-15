import {
  pageKnowledgeBasesByOwner,
  pageKnowledgeBasesByProject,
  getKnowledgeBase,
  createKnowledgeBase,
  updateKnowledgeBase,
  pageKnowledgeDocuments,
  addKnowledgeDocument,
  uploadKnowledgeDocuments,
  uploadKnowledgeDocumentsZip,
  listDocumentChunks,
  previewDocumentChunks,
  downloadKnowledgeDocument,
  downloadKnowledgeDocumentsZip,
  listKnowledgeBaseMembers,
  addKnowledgeBaseMember,
  removeKnowledgeBaseMember,
  createKnowledgeIndexTask,
  listKnowledgeBaseIndexTasks,
  listDocumentIndexTasks,
  backfillKnowledgeBaseEmbeddings,
  backfillDocumentEmbeddings,
  getKnowledgeBaseEmbeddingStatus,
  getDocumentEmbeddingStatus,
  searchKnowledgeBaseDebug,
  pageAiSessions,
  pageAiSessionMessages,
  bindSessionKnowledgeBases,
  archiveAiSession,
  deleteAiSession,
  chatWithKnowledgeBase,
  listCallRetrievals,
  streamChatWithKnowledgeBase,
  normalizeKnowledgeBaseEmbeddingPayload
} from '@/api/knowledgeBase'
import { listEnabledAiModels } from '@/api/aiAdmin'

function buildFileFormData(files = [], fieldName = 'files') {
  const formData = new FormData()
  files.forEach(file => {
    formData.append(fieldName, file)
  })
  return formData
}

export const knowledgeBaseService = {
  fetchModels() {
    return listEnabledAiModels()
  },

  fetchKnowledgeBases({ listMode, ownerId, projectId, page, size }) {
    if (listMode === 'project') {
      return pageKnowledgeBasesByProject(projectId, { page, size })
    }
    return pageKnowledgeBasesByOwner(ownerId, { page, size })
  },

  fetchKnowledgeBaseDetail(id) {
    return getKnowledgeBase(id)
  },

  saveKnowledgeBase(mode, payload, id) {
    const normalized = normalizeKnowledgeBaseEmbeddingPayload(payload)
    return mode === 'edit' && id
      ? updateKnowledgeBase(id, normalized)
      : createKnowledgeBase(normalized)
  },

  fetchDocuments(knowledgeBaseId, page, size) {
    return pageKnowledgeDocuments(knowledgeBaseId, { page, size })
  },

  addDocument(knowledgeBaseId, payload) {
    return addKnowledgeDocument(knowledgeBaseId, payload)
  },

  uploadDocuments(knowledgeBaseId, files = []) {
    return uploadKnowledgeDocuments(knowledgeBaseId, buildFileFormData(files, 'files'))
  },

  uploadZip(knowledgeBaseId, file, extra = {}) {
    const formData = new FormData()
    formData.append('file', file)
    Object.keys(extra).forEach(key => {
      if (extra[key] !== undefined && extra[key] !== null && extra[key] !== '') {
        formData.append(key, extra[key])
      }
    })
    return uploadKnowledgeDocumentsZip(knowledgeBaseId, formData)
  },

  fetchDocumentChunks(documentId) {
    return listDocumentChunks(documentId)
  },

  previewDocumentChunks(documentId, payload = {}) {
    return previewDocumentChunks(documentId, payload)
  },

  downloadDocument(documentId) {
    return downloadKnowledgeDocument(documentId)
  },

  downloadDocumentsZip(knowledgeBaseId, documentIds = []) {
    return downloadKnowledgeDocumentsZip(knowledgeBaseId, documentIds)
  },

  fetchMembers(knowledgeBaseId) {
    return listKnowledgeBaseMembers(knowledgeBaseId)
  },

  addMember(knowledgeBaseId, payload) {
    return addKnowledgeBaseMember(knowledgeBaseId, payload)
  },

  removeMember(knowledgeBaseId, memberId) {
    return removeKnowledgeBaseMember(knowledgeBaseId, memberId)
  },

  createIndexTask(knowledgeBaseId, payload = {}) {
    return createKnowledgeIndexTask(knowledgeBaseId, payload)
  },

  fetchKnowledgeBaseTasks(knowledgeBaseId) {
    return listKnowledgeBaseIndexTasks(knowledgeBaseId)
  },

  fetchDocumentTasks(documentId) {
    return listDocumentIndexTasks(documentId)
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

  fetchSessions(params) {
    return pageAiSessions(params)
  },

  fetchSessionMessages(sessionId) {
    return pageAiSessionMessages(sessionId, { page: 0, size: 200 })
  },

  bindSessionKnowledgeBases(sessionId, knowledgeBaseIds = []) {
    return bindSessionKnowledgeBases(sessionId, knowledgeBaseIds)
  },

  archiveSession(sessionId) {
    return archiveAiSession(sessionId)
  },

  removeSession(sessionId) {
    return deleteAiSession(sessionId)
  },

  sendChat(payload) {
    return chatWithKnowledgeBase(payload)
  },

  streamChat(payload) {
    return streamChatWithKnowledgeBase(payload)
  },

  fetchRetrievals(callLogId) {
    return listCallRetrievals(callLogId)
  },

  normalizeKnowledgeBaseEmbeddingPayload
}
