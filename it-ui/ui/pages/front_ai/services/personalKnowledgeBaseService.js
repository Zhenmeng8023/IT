import { frontKnowledgeBaseService } from '@/pages/front_ai/services/frontKnowledgeBaseService'

function normalizePersonalPayload(payload = {}) {
  return {
    ...payload,
    scopeType: 'PERSONAL',
    projectId: null
  }
}

export const personalKnowledgeBaseService = {
  ...frontKnowledgeBaseService,

  fetchKnowledgeBases({ page, size } = {}) {
    return frontKnowledgeBaseService.fetchKnowledgeBases({
      listMode: 'owner',
      page,
      size
    })
  },

  saveKnowledgeBase(mode, payload, id) {
    return frontKnowledgeBaseService.saveKnowledgeBase(mode, normalizePersonalPayload(payload), id)
  }
}

export default personalKnowledgeBaseService
