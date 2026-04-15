import {
  getProjectDetail,
  getProjectContributors,
  getRelatedProjects,
  starProject,
  unstarProject,
  getProjectStarStatus,
  updateProject,
  listProjectMembers,
  listProjectTasks,
  listMyTasks,
  updateTaskStatus,
  listProjectFiles,
  listFileVersions,
  uploadProjectFile,
  uploadProjectZip,
  uploadFileNewVersion,
  setMainFile,
  deleteFile,
  previewProjectFile,
  downloadFile,
  downloadProjectFiles
} from '@/api/project'
import { getProjectRepository, initProjectRepository } from '@/api/projectRepository'
import { stageWorkspaceDelete } from '@/api/projectWorkspace'
import { aiSummarizeProject, aiSplitProjectTasks, normalizeProjectSummaryPayload, normalizeProjectTaskPayload } from '@/api/aiAssistant'
import { listEnabledAiModels, pageAiModels } from '@/api/aiAdmin'
import { getProjectPrimaryReadme, getProjectDoc, listProjectDocs } from '@/api/projectDoc'
import request from '@/utils/request'

export function submitTaskReopenRequest(taskId, payload) {
  return request({
    url: `/project/task/${taskId}/reopen-requests`,
    method: 'post',
    data: payload
  })
}

export function uploadBatchFiles(formData) {
  return request({
    url: '/project/file/upload/batch',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function downloadBatchFiles(projectId, fileIds = []) {
  return request({
    url: '/project/file/download/batch',
    method: 'post',
    data: { projectId, fileIds },
    responseType: 'blob'
  })
}

export const projectDetailService = {
  getProjectDetail,
  getProjectContributors,
  getRelatedProjects,
  starProject,
  unstarProject,
  getProjectStarStatus,
  updateProject,
  listProjectMembers,
  listProjectTasks,
  listMyTasks,
  updateTaskStatus,
  listProjectFiles,
  listFileVersions,
  uploadProjectFile,
  uploadProjectZip,
  uploadFileNewVersion,
  setMainFile,
  deleteFile,
  previewProjectFile,
  downloadFile,
  downloadProjectFiles,
  getProjectRepository,
  initProjectRepository,
  stageWorkspaceDelete,
  aiSummarizeProject,
  aiSplitProjectTasks,
  normalizeProjectSummaryPayload,
  normalizeProjectTaskPayload,
  listEnabledAiModels,
  pageAiModels,
  getProjectPrimaryReadme,
  getProjectDoc,
  listProjectDocs,
  submitTaskReopenRequest,
  uploadBatchFiles,
  downloadBatchFiles
}
