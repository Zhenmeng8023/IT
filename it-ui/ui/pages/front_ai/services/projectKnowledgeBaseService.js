import { frontKnowledgeBaseService } from '@/pages/front_ai/services/frontKnowledgeBaseService'

function normalizeProjectPayload(projectId, payload = {}) {
  return {
    ...payload,
    scopeType: 'PROJECT',
    projectId: projectId || payload.projectId || null
  }
}

export const projectKnowledgeBaseService = {
  ...frontKnowledgeBaseService,

  fetchKnowledgeBases({ projectId, page, size } = {}) {
    return frontKnowledgeBaseService.fetchKnowledgeBases({
      listMode: 'project',
      projectId,
      page,
      size
    })
  },

  saveKnowledgeBase(mode, payload, id, projectId) {
    return frontKnowledgeBaseService.saveKnowledgeBase(
      mode,
      normalizeProjectPayload(projectId, payload),
      id
    )
  }
}

export default projectKnowledgeBaseService
