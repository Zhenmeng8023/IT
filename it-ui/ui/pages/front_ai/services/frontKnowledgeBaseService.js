import {
  pageMyFrontKnowledgeBases,
  pageFrontKnowledgeBasesByProject,
  getFrontKnowledgeBase,
  createFrontKnowledgeBase,
  updateFrontKnowledgeBase,
  deleteFrontKnowledgeBase,
  pageFrontKnowledgeDocuments,
  deleteFrontKnowledgeDocument,
  addFrontKnowledgeDocument,
  uploadFrontKnowledgeDocuments,
  uploadFrontKnowledgeDocumentsZip,
  listFrontDocumentChunks,
  downloadFrontKnowledgeDocument,
  downloadFrontKnowledgeDocumentsZip,
  listFrontKnowledgeBaseMembers,
  addFrontKnowledgeBaseMember,
  removeFrontKnowledgeBaseMember,
  listFrontKnowledgeImportTasks,
  getFrontKnowledgeImportTask,
  createFrontKnowledgeIndexTask,
  listFrontKnowledgeBaseIndexTasks,
  listFrontDocumentIndexTasks,
  getFrontKnowledgeBaseEmbeddingStatus,
  getFrontDocumentEmbeddingStatus,
  normalizeKnowledgeBaseEmbeddingPayload
} from '@/api/knowledgeBase'

function buildFileFormData(files = [], fieldName = 'files') {
  const formData = new FormData()
  files.forEach(file => {
    formData.append(fieldName, file)
  })
  return formData
}

export const frontKnowledgeBaseService = {
  fetchKnowledgeBases({ listMode, projectId, page, size }) {
    if (listMode === 'project') {
      return pageFrontKnowledgeBasesByProject(projectId, { page, size })
    }
    return pageMyFrontKnowledgeBases({ page, size })
  },

  fetchKnowledgeBaseDetail(id) {
    return getFrontKnowledgeBase(id)
  },

  saveKnowledgeBase(mode, payload, id) {
    const normalized = normalizeKnowledgeBaseEmbeddingPayload(payload)
    return mode === 'edit' && id
      ? updateFrontKnowledgeBase(id, normalized)
      : createFrontKnowledgeBase(normalized)
  },

  deleteKnowledgeBase(knowledgeBaseId) {
    return deleteFrontKnowledgeBase(knowledgeBaseId)
  },

  fetchDocuments(knowledgeBaseId, page, size) {
    return pageFrontKnowledgeDocuments(knowledgeBaseId, { page, size })
  },

  deleteDocument(knowledgeBaseId, documentId) {
    return deleteFrontKnowledgeDocument(knowledgeBaseId, documentId)
  },

  addDocument(knowledgeBaseId, payload) {
    return addFrontKnowledgeDocument(knowledgeBaseId, payload)
  },

  uploadDocuments(knowledgeBaseId, files = []) {
    return uploadFrontKnowledgeDocuments(knowledgeBaseId, buildFileFormData(files, 'files'))
  },

  uploadZip(knowledgeBaseId, file, extra = {}) {
    const formData = new FormData()
    formData.append('file', file)
    Object.keys(extra).forEach(key => {
      if (extra[key] !== undefined && extra[key] !== null && extra[key] !== '') {
        formData.append(key, extra[key])
      }
    })
    return uploadFrontKnowledgeDocumentsZip(knowledgeBaseId, formData)
  },

  fetchDocumentChunks(documentId) {
    return listFrontDocumentChunks(documentId)
  },

  downloadDocument(documentId) {
    return downloadFrontKnowledgeDocument(documentId)
  },

  downloadDocumentsZip(knowledgeBaseId, documentIds = []) {
    return downloadFrontKnowledgeDocumentsZip(knowledgeBaseId, documentIds)
  },

  fetchMembers(knowledgeBaseId) {
    return listFrontKnowledgeBaseMembers(knowledgeBaseId)
  },

  addMember(knowledgeBaseId, payload) {
    return addFrontKnowledgeBaseMember(knowledgeBaseId, payload)
  },

  removeMember(knowledgeBaseId, memberId) {
    return removeFrontKnowledgeBaseMember(knowledgeBaseId, memberId)
  },

  fetchImportTasks(knowledgeBaseId) {
    return listFrontKnowledgeImportTasks(knowledgeBaseId)
  },

  fetchImportTask(taskId) {
    return getFrontKnowledgeImportTask(taskId)
  },

  fetchKnowledgeBaseTasks(knowledgeBaseId) {
    return listFrontKnowledgeBaseIndexTasks(knowledgeBaseId)
  },

  createIndexTask(knowledgeBaseId, payload = {}) {
    return createFrontKnowledgeIndexTask(knowledgeBaseId, payload)
  },

  fetchDocumentTasks(documentId) {
    return listFrontDocumentIndexTasks(documentId)
  },

  fetchKnowledgeBaseEmbeddingStatus(knowledgeBaseId) {
    return getFrontKnowledgeBaseEmbeddingStatus(knowledgeBaseId)
  },

  fetchDocumentEmbeddingStatus(documentId) {
    return getFrontDocumentEmbeddingStatus(documentId)
  },

  normalizeKnowledgeBaseEmbeddingPayload
}

export default frontKnowledgeBaseService
