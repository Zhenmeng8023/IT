<template>
  <div class="project-detail-container">
    <ProjectDetailHeader
      :project="project"
      :page-access-resolved="pageAccessResolved"
      :current-user-id="resolvedCurrentUserId"
      :can-see-task-collaboration="resolvedCanSeeTaskCollaboration"
      :project-id="projectId"
      :can-manage-project="resolvedCanManageProject"
      :ai-summary-loading="aiSummaryLoading"
      :ai-task-loading="aiTaskLoading"
      :star-loading="starLoading"
      :handle-project-social-changed="handleProjectSocialChanged"
      :handle-project-manage-click="handleProjectManageClick"
      :handle-ai-summarize-project="handleAiSummarizeProject"
      :handle-ai-split-project-tasks="handleAiSplitProjectTasks"
      :toggle-star="toggleStar"
      :download-main-file="downloadMainFile"
    />

    <ProjectOverviewCard
      :project="project"
      :category-label="categoryLabel"
      :status-tag-type="statusTagType"
      :status-label="statusLabel"
      :visibility-label="visibilityLabel"
      :tag-list="tagList"
      :format-time="formatTime"
    />

    <ProjectFeatureEntryPanel
      v-if="projectId"
      :project-id="projectId"
      @open-manage="goToProjectManage($event)"
    />

    <ProjectTaskBoardPreview
      :page-access-resolved="pageAccessResolved"
      :can-see-task-collaboration="resolvedCanSeeTaskCollaboration"
      :task-board-loading="taskBoardLoading"
      :task-summary="taskSummary"
      :recent-tasks="recentTasks"
      :overdue-tasks="overdueTasks"
      :task-quick-updating-id="taskQuickUpdatingId"
      :fetch-project-tasks="fetchProjectTasks"
      :handle-task-manage-click="handleTaskManageClick"
      :is-task-overdue="isTaskOverdue"
      :get-task-priority-type="getTaskPriorityType"
      :get-task-priority-text="getTaskPriorityText"
      :get-task-status-type="getTaskStatusType"
      :get-task-status-text="getTaskStatusText"
      :get-task-assignee-name="getTaskAssigneeName"
      :get-task-time-label="getTaskTimeLabel"
      :get-task-due-label="getTaskDueLabel"
      :handle-quick-task-status-change="handleQuickTaskStatusChange"
      :open-task-collab-drawer="openTaskCollabDrawer"
    />

    <div class="content-layout">
      <div class="content-main">
        <ProjectDetailMainTabs
          :project="project"
          :can-manage-project="resolvedCanManageProject"
          :project-docs-loading="projectDocsLoading"
          :project-docs="projectDocs"
          :primary-project-doc="primaryProjectDoc"
          :active-project-doc="activeProjectDoc"
          :primary-project-doc-html="primaryProjectDocHtml"
          :active-project-doc-html="activeProjectDocHtml"
          :project-doc-fallback-html="projectDocFallbackHtml"
          :project-doc-fallback-has-content="projectDocFallbackHasContent"
          :readme-lead-text="readmeLeadText"
          :project-doc-drawer-visible.sync="projectDocDrawerVisible"
          :handle-open-doc-manage="handleOpenDocManage"
          :handle-select-project-doc="handleSelectProjectDoc"
          :handle-refresh-project-docs="handleRefreshProjectDocs"
          :has-ai-result="hasAiResult"
          :last-ai-model-label="lastAiModelLabel"
          :current-ai-model-label="currentAiModelLabel"
          :clear-ai-result="clearAiResult"
          :ai-active-tab.sync="aiActiveTab"
          :ai-summary-card="aiSummaryCard"
          :ai-task-card="aiTaskCard"
        />
        <el-card shadow="never" class="section-card">
          <div slot="header" class="section-header section-header-flex">
            <span>项目文件</span>
            <div class="file-header-actions">
              <el-button
                size="mini"
                icon="el-icon-download"
                :disabled="!selectedFileIds.length"
                @click="handleBatchDownload"
              >
                批量下载{{ selectedFileIds.length ? `（${selectedFileIds.length}）` : '' }}
              </el-button>
              <el-button v-if="resolvedCanManageProject" size="mini" icon="el-icon-upload2" :loading="uploadLoading" @click="openUploadDialog(false)">
                上传进工作区
              </el-button>
              <el-button
                v-if="resolvedCanManageProject"
                size="mini"
                icon="el-icon-top"
                :disabled="!currentFile.id"
                @click="openUploadDialog(true)"
              >
                提交变更
              </el-button>
            </div>
          </div>
          <div class="workspace-flow-banner">
            <div class="workspace-flow-copy">
              <div class="workspace-flow-title">工作区式文件流</div>
              <div class="workspace-flow-desc">
                这里的上传入口已经改成“先进入 workspace，再通过 Commit / MR 进入主线”，不会再直接改动正式版本。
              </div>
            </div>
            <div class="workspace-flow-actions">
              <el-tag size="mini" effect="plain">Upload</el-tag>
              <el-tag size="mini" type="warning" effect="plain">Workspace</el-tag>
              <el-tag size="mini" type="success" effect="plain">Commit</el-tag>
              <el-tag size="mini" type="danger" effect="plain">MR</el-tag>
              <el-button
                v-if="resolvedCanManageProject || resolvedCanSeeTaskCollaboration"
                size="mini"
                type="primary"
                plain
                @click="handleProjectManageClick"
              >
                去仓库工作台
              </el-button>
            </div>
          </div>
          <div ref="fileBrowserRef" class="file-browser">
            <div class="file-tree-panel" :style="treePanelStyle">
              <el-input
                v-model="treeFilterText"
                size="small"
                clearable
                prefix-icon="el-icon-search"
                  placeholder="搜索文件"
              />
              <div class="tree-selection-bar">
                <span>已选 {{ selectedFileIds.length }} / {{ totalFileCount }} 个</span>
                <div class="tree-selection-actions">
                  <el-button size="mini" type="text" @click="toggleSelectAllFiles">
                    {{ isAllFilesSelected ? '取消全选' : '全选文件' }}
                  </el-button>
                  <el-button
                    v-if="resolvedCanManageProject"
                    size="mini"
                    type="text"
                    class="danger-text-btn"
                    :disabled="!selectedFileIds.length"
                    @click="handleBatchDeleteSelectedFiles"
                  >
                    加入工作区删除{{ selectedFileIds.length ? `（${selectedFileIds.length}）` : '' }}
                  </el-button>
                  <el-button size="mini" type="text" :disabled="!selectedFileIds.length" @click="clearSelectedFiles">
                    清空
                  </el-button>
                </div>
              </div>
              <div class="tree-wrap">
                <el-tree
                  ref="fileTreeRef"
                  :data="fileTree"
                  :props="treeProps"
                  node-key="path"
                  default-expand-all
                  highlight-current
                  :filter-node-method="filterNode"
                  @node-click="handleFileClick"
                >
                  <span slot-scope="{ data }" class="tree-node">
                    <el-checkbox
                      v-if="data.type === 'file'"
                      class="tree-node-checkbox"
                      :value="isFileChecked(data.id)"
                      @click.stop.native
                      @change="toggleFileSelection(data, $event)"
                    />
                    <i :class="getTreeIcon(data)"></i>
                    <span class="tree-node-name" :title="data.path || data.name">{{ data.name }}</span>
                    <span v-if="data.type === 'file' && data.isMain" class="main-file-badge">主文件</span>
                  </span>
                </el-tree>
              </div>
            </div>
            <div
              class="file-browser-splitter"
              title="拖动调整目录树宽度"
              @mousedown="startTreeResize"
            ></div>
            <div class="file-preview-panel">
              <div class="file-preview-toolbar">
                <div class="file-preview-title-group">
                  <div class="file-preview-title">{{ currentFile.name || '请选择文件' }}</div>
                  <div v-if="currentFile.path" class="file-preview-subtitle">{{ currentFile.path }}</div>
                </div>
                <div class="file-preview-actions">
                  <el-button size="mini" :disabled="!currentFile.id" @click="downloadCurrentFile">下载</el-button>
                  <el-button v-if="resolvedCanManageProject" size="mini" :disabled="!currentFile.id" @click="markMainFile">设为主文件</el-button>
                  <el-button v-if="resolvedCanManageProject" size="mini" type="danger" :disabled="!currentFile.id" @click="removeCurrentFile">加入工作区删除</el-button>
                </div>
              </div>
              <div v-if="currentFile.id" class="file-preview-meta">
                <div class="preview-meta-left">
                  <span class="meta-pill meta-pill-lang">{{ currentFileLanguageLabel }}</span>
                  <span v-if="currentPreviewMetricLabel" class="meta-pill">{{ currentPreviewMetricLabel }}</span>
                  <span class="meta-pill">{{ formatFileSize(currentFile.size) || '-' }}</span>
                  <span class="meta-pill">{{ currentFile.versions.length }} 个版本</span>
                </div>
                <div class="preview-meta-right">
                  <div v-if="currentFile.id && canAdjustPreview" class="preview-view-tools">
                    <span class="preview-font-indicator">{{ previewFontSize }}px</span>
                    <el-button size="mini" plain @click="decreasePreviewFont">A-</el-button>
                    <el-button size="mini" plain @click="resetPreviewFont">重置</el-button>
                    <el-button size="mini" plain @click="increasePreviewFont">A+</el-button>
                    <el-button size="mini" :type="previewWrap ? 'primary' : 'default'" plain @click="togglePreviewWrap">
                      {{ previewWrapButtonText }}
                    </el-button>
                  </div>
                  <el-button
                    size="mini"
                    plain
                    icon="el-icon-document-copy"
                    :disabled="!canCopyCurrentFileContent"
                    @click="copyCurrentFileContent"
                  >
                    复制内容
                  </el-button>
                </div>
              </div>
              <div v-if="currentFile.id && currentFile.previewError" class="preview-warning-banner">
                <i class="el-icon-warning-outline"></i>
                <span>{{ currentFile.previewError }}</span>
              </div>
              <div v-if="currentFile.id && currentFile.previewLoading" class="empty-preview preview-loading-state">
                <i class="el-icon-loading"></i>
                <span>正在解析文件预览...</span>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'code'" class="code-preview-shell" :style="previewFontStyle">
                <div class="code-preview-header">
                  <div class="code-header-left">
                    <span class="code-dot code-dot-red"></span>
                    <span class="code-dot code-dot-yellow"></span>
                    <span class="code-dot code-dot-green"></span>
                    <span class="code-file-name">{{ currentFile.name || '未命名文件' }}</span>
                  </div>
                  <div class="code-header-right">
                    <span class="code-extension-chip">{{ currentFile.extension ? '.' + currentFile.extension : '.txt' }}</span>
                  </div>
                </div>
                <div class="code-container">
                  <div ref="lineNumbersRef" class="line-numbers">
                    <div v-for="i in currentFileLineCount" :key="i" class="line-number">{{ i }}</div>
                  </div>
                  <pre ref="codeContentRef" class="code-content" :class="{ 'is-wrap': previewWrap }" :style="previewFontStyle" @scroll="syncCodeScroll"><code class="hljs" :data-language="currentFileHighlightLanguage" v-html="highlightedCurrentFileHtml"></code></pre>
                </div>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'markdown'" class="rich-preview-shell">
                <div class="rich-preview-body ai-rich-content" :style="previewFontStyle" v-html="currentMarkdownHtml"></div>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'text'" class="text-preview-shell">
                <pre class="plain-text-preview" :class="{ 'is-wrap': previewWrap }" :style="previewFontStyle">{{ currentFile.content }}</pre>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'image'" class="media-preview-shell image-preview-shell">
                <img :src="currentFile.blobUrl" :alt="currentFile.name" class="image-preview-element">
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'pdf'" class="media-preview-shell pdf-preview-shell">
                <iframe :src="currentFile.blobUrl" class="pdf-preview-frame" frameborder="0"></iframe>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'audio'" class="media-preview-shell audio-preview-shell">
                <div class="media-preview-title">音频预览</div>
                <audio :src="currentFile.blobUrl" controls class="audio-preview-element"></audio>
                <div class="media-preview-tip">支持在线播放音频，并可继续下载原文件。</div>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'video'" class="media-preview-shell video-preview-shell">
                <video :src="currentFile.blobUrl" controls class="video-preview-element"></video>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'table'" class="table-preview-shell">
                <div v-if="currentFile.tablePreview && currentFile.tablePreview.headers.length" class="table-preview-wrap">
                  <table class="preview-table">
                    <thead>
                      <tr>
                        <th v-for="(header, index) in currentFile.tablePreview.headers" :key="'head-' + index">{{ header || ('列' + (index + 1)) }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, rowIndex) in currentFile.tablePreview.rows" :key="'row-' + rowIndex">
                        <td v-for="(cell, cellIndex) in row" :key="rowIndex + '-' + cellIndex">{{ cell }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <div v-else class="empty-preview">表格内容为空或解析失败</div>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'docx'" class="office-preview-shell">
                <div class="office-preview-head">
                  <span class="office-preview-badge">Word</span>
                  <span class="office-preview-subtitle">提取文档正文进行在线预览</span>
                </div>
                <div class="office-document-body">
                  <p v-for="(paragraph, index) in currentFile.officePreview.paragraphs" :key="'docx-' + index">{{ paragraph }}</p>
                  <div v-if="!currentFile.officePreview.paragraphs.length" class="empty-preview">文档内容为空或暂时无法解析</div>
                </div>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'spreadsheet'" class="office-preview-shell spreadsheet-preview-shell">
                <div class="office-preview-head">
                  <span class="office-preview-badge">Excel</span>
                  <span class="office-preview-subtitle">已解析工作表内容，可滚动查看</span>
                </div>
                <div v-if="currentFile.officePreview.sheets && currentFile.officePreview.sheets.length" class="sheet-preview-list">
                  <div v-for="(sheet, index) in currentFile.officePreview.sheets" :key="'sheet-' + index" class="sheet-preview-card">
                    <div class="sheet-preview-name">{{ sheet.name || ('Sheet ' + (index + 1)) }}</div>
                    <div class="table-preview-wrap">
                      <table class="preview-table">
                        <thead>
                          <tr>
                            <th v-for="(header, headerIndex) in sheet.headers" :key="'sheet-head-' + index + '-' + headerIndex">{{ header || ('列' + (headerIndex + 1)) }}</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="(row, rowIndex) in sheet.rows" :key="'sheet-row-' + index + '-' + rowIndex">
                            <td v-for="(cell, cellIndex) in row" :key="'sheet-cell-' + index + '-' + rowIndex + '-' + cellIndex">{{ cell }}</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
                <div v-else class="empty-preview">工作表内容为空或暂时无法解析</div>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'presentation'" class="office-preview-shell ppt-preview-shell">
                <div class="office-preview-head">
                  <span class="office-preview-badge">PPT</span>
                  <span class="office-preview-subtitle">已提取幻灯片文本内容进行结构化展示</span>
                </div>
                <div v-if="currentFile.officePreview.slides && currentFile.officePreview.slides.length" class="ppt-slide-list">
                  <div v-for="slide in currentFile.officePreview.slides" :key="'slide-' + slide.index" class="ppt-slide-card">
                    <div class="ppt-slide-title">第 {{ slide.index }} 页</div>
                    <div class="ppt-slide-lines">
                      <p v-for="(line, lineIndex) in slide.lines" :key="'slide-line-' + slide.index + '-' + lineIndex">{{ line }}</p>
                    </div>
                  </div>
                </div>
                <div v-else class="empty-preview">幻灯片内容为空或暂时无法解析</div>
              </div>
              <div v-else-if="currentFile.id" class="empty-preview unsupported-preview">
                <div class="unsupported-title">当前文件暂不支持在线预览</div>
                <div class="unsupported-desc">{{ currentFile.previewError || unsupportedPreviewMessage }}</div>
              </div>
              <div v-else class="empty-preview">点击左侧文件查看内容</div>
            </div>
          </div>
          <div v-if="currentFile.versions.length" class="version-box">
            <div class="sub-title">版本记录</div>
            <el-timeline>
              <el-timeline-item
                v-for="version in currentFile.versions"
                :key="version.id || version.version"
                :timestamp="formatTime(version.createdAt || version.uploadedAt)"
              >
                <div class="version-item">
                  <span class="version-name">{{ version.version || '未命名版本' }}</span>
                  <span class="version-desc">{{ version.commitMessage || version.remark || '无说明' }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </div>

      <ProjectDetailSidebar
        :activity-timeline-key="activityTimelineKey"
        :project-id="projectId"
        :selected-ai-model-id.sync="selectedAiModelId"
        :ai-models-loading="aiModelsLoading"
        :ai-models="aiModels"
        :format-ai-model-option="formatAiModelOption"
        :handle-ai-model-change="handleAiModelChange"
        :current-ai-model-label="currentAiModelLabel"
        :current-ai-provider-label="currentAiProviderLabel"
        :ai-summary-loading="aiSummaryLoading"
        :handle-ai-summarize-project="handleAiSummarizeProject"
        :ai-task-loading="aiTaskLoading"
        :handle-ai-split-project-tasks="handleAiSplitProjectTasks"
        :project="project"
        :status-label="statusLabel"
        :category-label="categoryLabel"
        :format-time="formatTime"
        :visibility-label="visibilityLabel"
        :page-access-resolved="pageAccessResolved"
        :can-see-task-collaboration="resolvedCanSeeTaskCollaboration"
        :my-todo-tasks="myTodoTasks"
        :my-tasks-loading="myTasksLoading"
        :fetch-my-tasks="fetchMyTasks"
        :my-todo-pending-count="myTodoPendingCount"
        :is-task-overdue="isTaskOverdue"
        :get-task-priority-type="getTaskPriorityType"
        :get-task-priority-text="getTaskPriorityText"
        :get-task-due-label="getTaskDueLabel"
        :task-quick-updating-id="taskQuickUpdatingId"
        :handle-quick-task-status-change="handleQuickTaskStatusChange"
        :open-task-collab-drawer="openTaskCollabDrawer"
        :today-due-tasks="todayDueTasks"
        :task-board-loading="taskBoardLoading"
        :fetch-project-tasks="fetchProjectTasks"
        :get-task-assignee-name="getTaskAssigneeName"
        :format-task-due-clock="formatTaskDueClock"
        :contributors="contributors"
        :role-label="roleLabel"
        :related-projects="relatedProjects"
        :go-to-detail="goToDetail"
        :map-category="mapCategory"
      />
    </div>

    <ProjectDetailTaskCollabDrawer
      :task-collab-drawer-visible.sync="taskCollabDrawerVisible"
      :selected-task-for-collab="selectedTaskForCollab"
      :project-id="projectId"
      :current-user-id="resolvedCurrentUserId"
      :current-member-record="currentMemberRecord"
      :can-manage-project="resolvedCanManageProject"
      :task-collab-active-tab.sync="taskCollabActiveTab"
      :task-collab-refresh-seed="taskCollabRefreshSeed"
      :handle-task-collab-changed="handleTaskCollabChanged"
      :handle-task-collab-status-updated="handleTaskCollabStatusUpdated"
      :handle-task-collab-drawer-closed="handleTaskCollabDrawerClosed"
    />

    <ProjectEditDialog
      :show-edit-dialog.sync="showEditDialog"
      :edit-form="editForm"
      :edit-rules="editRules"
      :category-options="categoryOptions"
      :status-options="statusOptions"
      :save-loading="saveLoading"
      @submit="submitEdit"
    />

    <ProjectUploadDialog
      :upload-dialog="uploadDialog"
      :upload-loading="uploadLoading"
      :handle-picked-file="handlePickedFile"
      :close-upload-dialog="closeUploadDialog"
      :submit-upload="submitUpload"
    />
  </div>
</template>

<script>
import hljs from 'highlight.js/lib/common'
import 'highlight.js/styles/atom-one-dark.css'
import { getToken } from '@/utils/auth'
import ProjectFeatureEntryPanel from './components/ProjectFeatureEntryPanel.vue'
import ProjectDetailHeader from './components/ProjectDetailHeader.vue'
import ProjectOverviewCard from './components/ProjectOverviewCard.vue'
import ProjectTaskBoardPreview from './components/ProjectTaskBoardPreview.vue'
import ProjectDetailMainTabs from './components/ProjectDetailMainTabs.vue'
import ProjectDetailSidebar from './components/ProjectDetailSidebar.vue'
import ProjectEditDialog from './dialogs/ProjectEditDialog.vue'
import ProjectUploadDialog from './dialogs/ProjectUploadDialog.vue'
import ProjectDetailTaskCollabDrawer from './drawers/ProjectDetailTaskCollabDrawer.vue'
import { projectDetailService } from './services/projectDetailService'
import {
  CATEGORY_MAP,
  STATUS_MAP,
  STATUS_TAG_MAP,
  ROLE_MAP,
  getHighlightLanguage,
  detectPreviewType,
  createEmptyPreviewState,
  normalizeLineBreaks,
  safeReadBlobText,
  blobLooksLikeZip,
  parseDelimitedText,
  parseDocxFile,
  parseXlsxFile,
  parsePptxFile,
  parseTags,
  toBackendDateTime,
  extractApiData,
  normalizeAiModel,
  buildProjectAiContent,
  decodeJwtPayload,
  pickUserIdFromObject,
  escapeHtmlValue,
  countReadmeReadableUnits,
  countMarkdownHeadings,
  countMarkdownCodeBlocks,
  buildReadmeLeadText,
  formatEstimatedReadTime,
  renderMarkdownToHtml
} from './composables/useProjectDetail'

const {
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
  previewProjectFile,
  downloadFile,
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
  submitTaskReopenRequest: submitTaskReopenRequestApi,
  uploadBatchFiles: uploadBatchFilesApi,
  downloadBatchFiles: downloadBatchFilesApi
} = projectDetailService

export default {
  layout: 'project',
  components: {
    ProjectDetailHeader,
    ProjectOverviewCard,
    ProjectTaskBoardPreview,
    ProjectDetailMainTabs,
    ProjectDetailSidebar,
    ProjectDetailTaskCollabDrawer,
    ProjectEditDialog,
    ProjectUploadDialog,
    ProjectFeatureEntryPanel,
  },

  data() {
    return {
      projectId: null,
      clientHydrated: false,
      loading: false,
      pageAccessResolved: false,
      starLoading: false,
      saveLoading: false,
      uploadLoading: false,
      activityTimelineKey: 0,
      aiModelsLoading: false,
      aiSummaryLoading: false,
      aiTaskLoading: false,
      aiModels: [],
      activeAiModel: null,
      selectedAiModelId: null,
      lastAiModelLabel: '',
      aiActiveTab: 'summary',
      aiProjectSummary: '',
      aiProjectTasks: '',
      aiSummaryCard: {
        overview: '',
        scenarios: [],
        features: [],
        risks: [],
        nextActions: [],
        rawText: ''
      },
      aiTaskCard: {
        phases: [],
        executionOrder: [],
        risks: [],
        rawText: ''
      },
      taskCollabDrawerVisible: false,
      taskCollabActiveTab: 'overview',
      taskCollabRefreshSeed: 0,
      selectedTaskForCollab: null,
      taskCollabRouteRestoring: false,
      taskCollabRouteApplying: false,
      projectDocs: [],
      projectDocsLoading: false,
      projectDocDrawerVisible: false,
      activeProjectDoc: null,
      taskBoardLoading: false,
      myTasksLoading: false,
      taskQuickUpdatingId: null,
      taskList: [],
      myTaskList: [],
      memberListLoaded: false,
      memberList: [],
      treeFilterText: '',
      selectedFileIds: [],
      previewWrap: false,
      previewFontSize: 14,
      treePanelWidth: 360,
      treeResizeActive: false,
      treeResizeMinWidth: 280,
      treeResizeMaxWidth: 640,
      repositoryInfo: null,
      project: {
        id: null,
        name: '',
        description: '',
        category: '',
        status: '',
        visibility: 'public',
        tags: '',
        stars: 0,
        downloads: 0,
        views: 0,
        starred: false,
        authorId: null,
        authorName: '',
        authorAvatar: '',
        createdAt: '',
        updatedAt: '',
        members: [],
        tasks: [],
        files: [],
        contributors: [],
        relatedProjects: []
      },
      contributors: [],
      relatedProjects: [],
      fileTree: [],
      treeProps: {
        children: 'children',
        label: 'name'
      },
      currentFile: createEmptyPreviewState(),
      showEditDialog: false,
      editForm: {
        name: '',
        description: '',
        category: '',
        status: '',
        visibility: 'public',
        tagsText: ''
      },
      editRules: {
        name: [{ required: true, message: 'Please enter project name', trigger: 'blur' }],
        category: [{ required: true, message: 'Please select project category', trigger: 'change' }],
        status: [{ required: true, message: 'Please select project status', trigger: 'change' }]
      },
      uploadDialog: {
        visible: false,
        isVersion: false,
        file: null,
        files: []
      },
      categoryOptions: Object.keys(CATEGORY_MAP).map(key => ({ value: key, label: CATEGORY_MAP[key] })),
      statusOptions: Object.keys(STATUS_MAP).map(key => ({ value: key, label: STATUS_MAP[key] }))
    }
  },

  computed: {
    renderedReadme() {
      return this.renderMarkdownContent(this.project.readme, '暂无 README 文档')
    },
    primaryProjectDoc() {
      if (!Array.isArray(this.projectDocs) || !this.projectDocs.length) return null
      if (this.project.readmeDocId) {
        return this.projectDocs.find(item => Number(item.id) === Number(this.project.readmeDocId)) || this.projectDocs[0]
      }
      return this.projectDocs[0] || null
    },
    primaryProjectDocHtml() {
      return this.primaryProjectDoc && this.primaryProjectDoc.content
        ? this.renderMarkdownContent(this.primaryProjectDoc.content, '暂无项目文档')
        : ''
    },
    activeProjectDocHtml() {
      return this.activeProjectDoc && this.activeProjectDoc.content
        ? this.renderMarkdownContent(this.activeProjectDoc.content, '暂无项目文档')
        : ''
    },
    projectDocFallbackHtml() {
      return this.renderMarkdownContent(this.project.readme, '暂无 README 文档')
    },
    projectDocFallbackHasContent() {
      return !!String(this.project.readme || '').trim()
    },
    recentProjectDocs() {
      return Array.isArray(this.projectDocs) ? this.projectDocs.slice(0, 4) : []
    },
    readmeSourceText() {
      return String(this.project.readme || '')
    },
    readmeHasContent() {
      return !!this.readmeSourceText.trim()
    },
    readmeReadableUnits() {
      return countReadmeReadableUnits(this.readmeSourceText)
    },
    readmeHeadingCount() {
      return countMarkdownHeadings(this.readmeSourceText)
    },
    readmeCodeBlockCount() {
      return countMarkdownCodeBlocks(this.readmeSourceText)
    },
    readmeReadTimeText() {
      return formatEstimatedReadTime(this.readmeSourceText)
    },
    readmeLeadText() {
      return buildReadmeLeadText(this.readmeSourceText, this.project.description)
    },
    renderedAiProjectSummary() {
      return this.renderMarkdownContent(this.aiProjectSummary)
    },
    renderedAiProjectTasks() {
      return this.renderMarkdownContent(this.aiProjectTasks)
    },
    tagList() {
      return parseTags(this.project.tags)
    },
    categoryLabel() {
      return this.mapCategory(this.project.category)
    },
    statusLabel() {
      return STATUS_MAP[this.project.status] || this.project.status || '未知状态'
    },
    statusTagType() {
      return STATUS_TAG_MAP[this.project.status] || 'info'
    },
    visibilityLabel() {
      return this.project.visibility === 'private' ? '私有' : '公开'
    },
    currentUserId() {
      if (!this.clientHydrated) return null
      return this.getCurrentAiUserId()
    },
    resolvedCurrentUserId() {
      return this.pageAccessResolved ? this.currentUserId : null
    },
    isProjectOwner() {
      return this.currentUserId !== null && this.currentUserId !== undefined && Number(this.project.authorId) === Number(this.currentUserId)
    },
    currentMemberRecord() {
      if (this.currentUserId === null || this.currentUserId === undefined || this.currentUserId === '') return null
      const sources = []
      if (Array.isArray(this.memberList) && this.memberList.length) sources.push(...this.memberList)
      if (Array.isArray(this.project.members) && this.project.members.length) sources.push(...this.project.members)
      return sources.find(item => {
        if (!item) return false
        const targetUserId = item.userId ?? item.id
        if (targetUserId === null || targetUserId === undefined || targetUserId === '') return false
        const status = String(item.status || item.memberStatus || 'active').toLowerCase()
        return Number(targetUserId) === Number(this.currentUserId) && status !== 'inactive' && status !== 'removed'
      }) || null
    },
    currentProjectRole() {
      if (this.isProjectOwner) return 'owner'
      return this.currentMemberRecord && this.currentMemberRecord.role ? String(this.currentMemberRecord.role).toLowerCase() : ''
    },
    canManageProject() {
      return this.currentProjectRole === 'owner' || this.currentProjectRole === 'admin'
    },
    resolvedCanManageProject() {
      return this.pageAccessResolved && this.canManageProject
    },
    canSeeTaskCollaboration() {
      if (this.currentUserId === null || this.currentUserId === undefined || this.currentUserId === '') return false
      if (this.isProjectOwner) return true
      return !!this.currentMemberRecord
    },
    resolvedCanSeeTaskCollaboration() {
      return this.pageAccessResolved && this.canSeeTaskCollaboration
    },
    hasAiResult() {
      return !!(this.aiSummaryCard.overview || this.aiTaskCard.phases.length || this.aiProjectSummary || this.aiProjectTasks)
    },
    taskSummary() {
      return {
        total: this.taskList.length,
        todo: this.taskList.filter(item => item.status === 'todo').length,
        inProgress: this.taskList.filter(item => item.status === 'in_progress').length,
        done: this.taskList.filter(item => item.status === 'done').length,
        overdue: this.taskList.filter(item => this.isTaskOverdue(item)).length
      }
    },
    recentTasks() {
      return [...this.taskList]
        .sort((a, b) => this.getTaskSortTime(b) - this.getTaskSortTime(a))
        .slice(0, 5)
    },
    overdueTasks() {
      return this.taskList
        .filter(item => this.isTaskOverdue(item))
        .sort((a, b) => this.getTaskDueTimestamp(a) - this.getTaskDueTimestamp(b))
        .slice(0, 5)
    },
    myTodoTasks() {
      return this.myTaskList
        .filter(item => item.status !== 'done')
        .sort((a, b) => {
          const dueDiff = this.getTaskDueTimestamp(a) - this.getTaskDueTimestamp(b)
          if (dueDiff !== 0) return dueDiff
          return this.getTaskSortTime(b) - this.getTaskSortTime(a)
        })
        .slice(0, 4)
    },
    myTodoPendingCount() {
      return this.myTaskList.filter(item => item.status !== 'done').length
    },
    todayDueTasks() {
      return this.taskList
        .filter(item => this.isTaskDueToday(item) && item.status !== 'done')
        .sort((a, b) => this.getTaskDueTimestamp(a) - this.getTaskDueTimestamp(b))
        .slice(0, 4)
    },
    selectedAiModel() {
      const targetId = this.selectedAiModelId
      if (targetId === null || targetId === undefined || targetId === '') return this.activeAiModel
      return this.aiModels.find(item => String(item.id) === String(targetId)) || this.activeAiModel
    },
    currentAiModelLabel() {
      const model = this.selectedAiModel || this.activeAiModel
      return model && model.modelName ? model.modelName : '默认激活模型'
    },
    currentAiProviderLabel() {
      const model = this.selectedAiModel || this.activeAiModel
      return model && model.providerCode ? model.providerCode : ''
    },
    totalFileCount() {
      return this.flattenFileTree(this.fileTree).length
    },
    isAllFilesSelected() {
      return this.totalFileCount > 0 && this.selectedFileIds.length === this.totalFileCount
    },
    currentPreviewType() {
      return this.currentFile.previewType || detectPreviewType(this.currentFile.actualType || this.currentFile.extension)
    },
    currentFileLineCount() {
      const normalized = normalizeLineBreaks(this.currentFile.content || '')
      return normalized ? normalized.split('\n').length : 0
    },
    currentPreviewMetricLabel() {
      if (!this.currentFile.id) return ''
      if (this.currentPreviewType === 'code' || this.currentPreviewType === 'markdown' || this.currentPreviewType === 'text') return `${this.currentFileLineCount || 0} 行`
      if (this.currentPreviewType === 'table') {
        const table = this.currentFile.tablePreview || { rows: [], headers: [] }
        return `${table.rows.length} 行 / ${table.headers.length} 列`
      }
      if (this.currentPreviewType === 'docx') return `${(this.currentFile.officePreview.paragraphs || []).length} 段`
      if (this.currentPreviewType === 'spreadsheet') return `${(this.currentFile.officePreview.sheets || []).length} 个工作表`
      if (this.currentPreviewType === 'presentation') return `${(this.currentFile.officePreview.slides || []).length} 页幻灯片`
      return '在线预览'
    },
    canAdjustPreview() {
      return ['code', 'text', 'markdown'].includes(this.currentPreviewType)
    },
    previewWrapButtonText() {
      return this.previewWrap ? '关闭换行' : '自动换行'
    },
    previewFontStyle() {
      return { '--preview-font-size': `${this.previewFontSize}px`, fontSize: `${this.previewFontSize}px` }
    },
    treePanelStyle() {
      return { width: `${this.treePanelWidth}px` }
    },
    currentMarkdownHtml() {
      return this.currentFile.markdownHtml || this.renderMarkdownContent(this.currentFile.content, '暂无 Markdown 内容')
    },
    canCopyCurrentFileContent() {
      return !!String(this.currentFile.content || '').trim()
    },
    unsupportedPreviewMessage() {
      if (this.currentPreviewType === 'office-legacy') return '当前仅支持 docx / xlsx / pptx 在线预览，旧版 Office 文件请下载后查看。'
      return '该文件类型暂不支持在线预览，请下载后查看。'
    },
    currentFileLanguageLabel() {
      const extension = String(this.currentFile.actualType || this.currentFile.extension || '').toLowerCase()
      const map = {
        js: 'JavaScript', ts: 'TypeScript', vue: 'Vue', java: 'Java', py: 'Python', go: 'Go', rs: 'Rust', cpp: 'C++', c: 'C', cs: 'C#', php: 'PHP',
        json: 'JSON', yml: 'YAML', yaml: 'YAML', xml: 'XML', html: 'HTML', css: 'CSS', scss: 'SCSS', less: 'LESS', md: 'Markdown', sql: 'SQL', sh: 'Shell',
        txt: 'Text', csv: 'CSV', tsv: 'TSV', pdf: 'PDF', png: '图片', jpg: '图片', jpeg: '图片', gif: '图片', webp: '图片', svg: 'SVG',
        mp3: '音频', wav: '音频', mp4: '视频', webm: '视频', docx: 'Word', xlsx: 'Excel', pptx: 'PPT'
      }
      return map[extension] || (extension ? extension.toUpperCase() : 'TEXT')
    },
    currentFileHighlightLanguage() {
      return getHighlightLanguage(this.currentFile.actualType || this.currentFile.extension)
    },
    highlightedCurrentFileHtml() {
      const content = String(this.currentFile.content || '')
      if (!content) return ''
      const language = this.currentFileHighlightLanguage
      try {
        if (language && language !== 'plaintext' && hljs.getLanguage(language)) return hljs.highlight(content, { language, ignoreIllegals: true }).value
        return hljs.highlight(content, { language: 'plaintext', ignoreIllegals: true }).value
      } catch (error) {
        return escapeHtmlValue(content)
      }
    }
  },

  watch: {
    treeFilterText(val) {
      if (this.$refs.fileTreeRef) {
        this.$refs.fileTreeRef.filter(val)
      }
    },
    // 监听路由变化，点击相关项目时重新加载数据
    '$route': {
      async handler(route) {
        const newProjectId = route.query.projectId || route.params.id
        const nextTaskId = route.query.taskId
        const nextTaskTab = route.query.taskTab || 'overview'

        if (newProjectId && String(newProjectId) !== String(this.projectId)) {
          this.projectId = newProjectId
          await this.initPage()
          await this.restoreTaskCollabFromRoute()
          return
        }

        if (!this.pageAccessResolved) {
          return
        }

        if (nextTaskId) {
          if (
            !this.taskCollabDrawerVisible ||
            !this.selectedTaskForCollab ||
            String(this.selectedTaskForCollab.id) !== String(nextTaskId) ||
            String(this.taskCollabActiveTab || '') !== String(nextTaskTab || '')
          ) {
            await this.restoreTaskCollabFromRoute()
          }
          return
        }

        if (this.taskCollabDrawerVisible && !this.taskCollabRouteApplying && !this.taskCollabRouteRestoring) {
          this.taskCollabDrawerVisible = false
          this.taskCollabActiveTab = 'overview'
          this.selectedTaskForCollab = null
        }
      },
      deep: true
    },
  },

  async mounted() {
    this.clientHydrated = true
    this.projectId = this.$route.query.projectId || this.$route.params.id
    if (!this.projectId) {
      this.$message.error('缺少项目 ID')
      return
    }
    await this.initPage()
    await this.restoreTaskCollabFromRoute()
  },

  beforeDestroy() {
    this.clearPreviewBlobUrl()
    this.stopTreeResize()
  },

  methods: {
    togglePreviewWrap() {
      this.previewWrap = !this.previewWrap
    },
    increasePreviewFont() {
      this.previewFontSize = Math.min(18, this.previewFontSize + 1)
    },
    decreasePreviewFont() {
      this.previewFontSize = Math.max(12, this.previewFontSize - 1)
    },
    resetPreviewFont() {
      this.previewFontSize = 14
    },
    startTreeResize(event) {
      if (event && typeof event.preventDefault === 'function') event.preventDefault()
      this.treeResizeActive = true
      document.addEventListener('mousemove', this.handleTreeResize)
      document.addEventListener('mouseup', this.stopTreeResize)
    },
    handleTreeResize(event) {
      if (!this.treeResizeActive) return
      const browser = this.$refs.fileBrowserRef
      if (!browser || !event) return
      const rect = browser.getBoundingClientRect()
      const maxWidth = Math.min(this.treeResizeMaxWidth, Math.max(this.treeResizeMinWidth + 40, rect.width - 360))
      const nextWidth = Math.min(maxWidth, Math.max(this.treeResizeMinWidth, event.clientX - rect.left))
      this.treePanelWidth = nextWidth
    },
    stopTreeResize() {
      this.treeResizeActive = false
      document.removeEventListener('mousemove', this.handleTreeResize)
      document.removeEventListener('mouseup', this.stopTreeResize)
    },
    syncCodeScroll(event) {
      const lineBox = this.$refs.lineNumbersRef
      if (lineBox && event && event.target) {
        lineBox.scrollTop = event.target.scrollTop
      }
    },
    buildEmptyCurrentFile() {
      return createEmptyPreviewState()
    },

    clearPreviewBlobUrl() {
      if (process.client && this.currentFile && this.currentFile.blobUrl) {
        window.URL.revokeObjectURL(this.currentFile.blobUrl)
      }
    },

    async resolveFilePreview(node, blob) {
      const extension = String(node.extension || '').toLowerCase()
      const actualType = String(node.actualType || extension || '').toLowerCase()
      const previewType = detectPreviewType(actualType)
      const payload = {
        previewType,
        blobUrl: '',
        mimeType: blob.type || '',
        content: '',
        markdownHtml: '',
        tablePreview: { headers: [], rows: [] },
        officePreview: { paragraphs: [], sheets: [], slides: [] },
        previewError: ''
      }
      if (previewType === 'code' || previewType === 'text') {
        payload.content = await safeReadBlobText(blob)
        return payload
      }
      if (previewType === 'markdown') {
        payload.content = await safeReadBlobText(blob)
        payload.markdownHtml = this.renderMarkdownContent(payload.content, '暂无 Markdown 内容')
        return payload
      }
      if (previewType === 'table') {
        payload.content = await safeReadBlobText(blob)
        payload.tablePreview = parseDelimitedText(payload.content, actualType === 'tsv' ? '\t' : ',')
        return payload
      }
      if (previewType === 'image' || previewType === 'pdf' || previewType === 'audio' || previewType === 'video') {
        if (process.client) payload.blobUrl = window.URL.createObjectURL(blob)
        return payload
      }
      if (previewType === 'docx') {
        if (!(await blobLooksLikeZip(blob))) {
          payload.previewType = 'text'
          payload.previewError = '文件名类似 Word 文档，但内容不是标准 docx 压缩包，已按文本回退预览。'
          payload.content = await safeReadBlobText(blob)
          return payload
        }
        payload.officePreview = await parseDocxFile(blob)
        payload.content = (payload.officePreview.paragraphs || []).join('\n')
        if (!(payload.officePreview.paragraphs || []).length) {
          payload.previewError = '未提取到正文内容，可能是扫描件、模板文档，或文件内容与扩展名不一致。'
        }
        return payload
      }
      if (previewType === 'spreadsheet') {
        if (!(await blobLooksLikeZip(blob))) {
          payload.previewType = 'text'
          payload.previewError = '文件名类似 Excel 文档，但内容不是标准 xlsx 压缩包，已按文本回退预览。'
          payload.content = await safeReadBlobText(blob)
          return payload
        }
        payload.officePreview = await parseXlsxFile(blob)
        if (!(payload.officePreview.sheets || []).length) {
          payload.previewError = '未提取到工作表内容，可能是空表、受保护文件，或文件内容与扩展名不一致。'
        }
        return payload
      }
      if (previewType === 'presentation') {
        if (!(await blobLooksLikeZip(blob))) {
          payload.previewType = 'text'
          payload.previewError = '文件名类似 PPT 文档，但内容不是标准 pptx 压缩包，已按文本回退预览。'
          payload.content = await safeReadBlobText(blob)
          return payload
        }
        payload.officePreview = await parsePptxFile(blob)
        payload.content = (payload.officePreview.slides || []).map(slide => slide.lines.join('\n')).join('\n\n')
        if (!(payload.officePreview.slides || []).length) {
          payload.previewError = '未提取到幻灯片文本，可能是纯图片 PPT、受保护文件，或文件内容与扩展名不一致。'
        }
        return payload
      }
      payload.previewError = previewType === 'office-legacy'
        ? '当前支持 docx / xlsx / pptx 在线预览，旧版 Office 文件请下载后查看。'
        : '该文件类型暂不支持在线预览，请下载后查看。'
      return payload
    },

    async initPage() {
      this.loading = true
      this.pageAccessResolved = false
      try {
        const baseTasks = [
          this.fetchProjectDetail(),
          this.fetchContributors(),
          this.fetchRelatedProjects(),
          this.fetchFiles(),
          this.loadAiModels()
        ]
        const token = getToken ? getToken() : ''
        if (token) {
          baseTasks.push(this.fetchProjectStarState())
        } else {
          this.project.starred = false
          this.myTaskList = []
          this.memberList = []
          this.memberListLoaded = true
        }
        await Promise.all(baseTasks)

        if (token) {
          await this.fetchMemberSnapshot()
        }

        if (this.canSeeTaskCollaboration) {
          const taskJobs = [this.fetchProjectTasks()]
          if (token) {
            taskJobs.push(this.fetchMyTasks())
          } else {
            this.myTaskList = []
          }
          await Promise.all(taskJobs)
        } else {
          this.taskList = []
          this.myTaskList = []
        }
      } finally {
        this.pageAccessResolved = true
        this.loading = false
      }
    },

    async loadAiModels() {
      this.aiModelsLoading = true
      try {
        let enabledList = []
        try {
          const enabledRes = await listEnabledAiModels()
          const enabledData = extractApiData(enabledRes)
          enabledList = Array.isArray(enabledData) ? enabledData.map(normalizeAiModel) : []
        } catch (error) {
          console.error('加载已启用模型失败', error)
        }

        if (!enabledList.length) {
          try {
            const pageRes = await pageAiModels({ page: 0, size: 100 })
            const pagePayload = extractApiData(pageRes)
            enabledList = Array.isArray(pagePayload?.content)
              ? pagePayload.content.map(normalizeAiModel)
              : Array.isArray(pagePayload?.records)
                ? pagePayload.records.map(normalizeAiModel)
                : Array.isArray(pagePayload?.list)
                  ? pagePayload.list.map(normalizeAiModel)
                  : Array.isArray(pagePayload)
                    ? pagePayload.map(normalizeAiModel)
                    : []
          } catch (error) {
            console.error('兜底加载全部模型失败', error)
          }
        }

        this.aiModels = enabledList

        let savedModelId = null
        if (process.client) {
          savedModelId = window.localStorage.getItem('project_detail_ai_model_id')
        }

        const preferredModelId = savedModelId
          ? String(savedModelId)
          : (enabledList[0] && enabledList[0].id) || null
        this.selectedAiModelId = preferredModelId === '' ? null : preferredModelId
        this.activeAiModel = enabledList.find(item => String(item.id) === String(this.selectedAiModelId)) || enabledList[0] || null
      } catch (error) {
        console.error(error)
        this.aiModels = []
        this.activeAiModel = null
      } finally {
        this.aiModelsLoading = false
      }
    },

    handleAiModelChange(value) {
      const modelId = value === '' || value === undefined || value === null ? null : String(value)
      this.selectedAiModelId = modelId
      if (process.client) {
        if (modelId === null) {
          window.localStorage.removeItem('project_detail_ai_model_id')
        } else {
          window.localStorage.setItem('project_detail_ai_model_id', modelId)
        }
      }
    },

    formatAiModelOption(item) {
      const modelName = item.modelName || '未命名模型'
      const provider = item.providerCode ? `（${item.providerCode}）` : ''
      return `${modelName}${provider}`
    },

    getCurrentAiUserId() {
      const directCandidates = [
        this.$store && this.$store.state && this.$store.state.user && this.$store.state.user.id,
        this.$store && this.$store.state && this.$store.state.user && this.$store.state.user.userId,
        this.$store && this.$store.state && this.$store.state.login && this.$store.state.login.userInfo && this.$store.state.login.userInfo.id,
        this.$store && this.$store.state && this.$store.state.login && this.$store.state.login.userInfo && this.$store.state.login.userInfo.userId
      ].filter(value => value !== undefined && value !== null && String(value).trim() !== '')

      if (directCandidates.length) {
        const value = String(directCandidates[0]).trim()
        return /^\d+$/.test(value) ? Number(value) : value
      }

      if (process.client) {
        const storageKeys = [
          'userInfo',
          'user',
          'loginUser',
          'currentUser',
          'Admin-User',
          'auth_user',
          'authUser',
          'memberInfo'
        ]

        for (const storage of [window.localStorage, window.sessionStorage]) {
          for (const key of storageKeys) {
            try {
              const raw = storage.getItem(key)
              if (!raw) continue
              const parsed = JSON.parse(raw)
              const foundId = pickUserIdFromObject(parsed)
              if (foundId !== null && foundId !== undefined && String(foundId).trim() !== '') {
                return foundId
              }
            } catch (e) {}
          }
        }

        try {
          const nuxtState = window.__NUXT__
          const foundId = pickUserIdFromObject(nuxtState)
          if (foundId !== null && foundId !== undefined && String(foundId).trim() !== '') {
            return foundId
          }
        } catch (e) {}
      }

      const token = getToken ? getToken() : ''
      if (token) {
        const payload = decodeJwtPayload(token)
        const foundId = pickUserIdFromObject(payload)
        if (foundId !== null && foundId !== undefined && String(foundId).trim() !== '') {
          return foundId
        }
      }

      return null
    },

    hasAiLoginContext() {
      const userId = this.getCurrentAiUserId()
      if (userId !== null && userId !== undefined && String(userId).trim() !== '') {
        return true
      }
      const token = getToken ? getToken() : ''
      return !!token
    },

    buildAiProjectContent() {
      return buildProjectAiContent(this.project, this.contributors, this.currentFile)
    },

    async handleAiSummarizeProject() {
      const userId = this.getCurrentAiUserId()
      if (!this.hasAiLoginContext()) {
        this.$message.warning('请先登录后再使用 AI 功能')
        return
      }

      this.aiSummaryLoading = true
      try {
        const modelId = this.selectedAiModelId || undefined
        const result = await aiSummarizeProject({
          userId: userId || undefined,
          modelId,
          projectId: this.projectId,
          title: this.project.name,
          content: this.buildAiProjectContent(),
          project: this.project
        })
        if (!result) {
          this.$message.warning('AI 未返回项目总结')
          return
        }
        const normalized = result?.normalized || normalizeProjectSummaryPayload(result)
        this.aiSummaryCard = {
          overview: normalized.overview || '',
          scenarios: Array.isArray(normalized.scenarios) ? normalized.scenarios : [],
          features: Array.isArray(normalized.features) ? normalized.features : [],
          risks: Array.isArray(normalized.risks) ? normalized.risks : [],
          nextActions: Array.isArray(normalized.nextActions) ? normalized.nextActions : [],
          rawText: normalized.rawText || result?.text || ''
        }
        this.aiProjectSummary = normalized.displayText || result?.displayText || result?.text || ''
        this.aiActiveTab = 'summary'
        this.lastAiModelLabel = this.currentAiModelLabel
        this.$message.success('AI 项目总结生成成功')
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || error.message || 'AI 项目总结生成失败')
      } finally {
        this.aiSummaryLoading = false
      }
    },

    async handleAiSplitProjectTasks() {
      const userId = this.getCurrentAiUserId()
      if (!this.canManageProject) {
        this.$message.warning('仅项目所有者或管理员可进行 AI 拆任务')
        return
      }
      if (!this.hasAiLoginContext()) {
        this.$message.warning('请先登录后再使用 AI 功能')
        return
      }

      this.aiTaskLoading = true
      try {
        const modelId = this.selectedAiModelId || undefined
        const result = await aiSplitProjectTasks({
          userId: userId || undefined,
          modelId,
          projectId: this.projectId,
          title: this.project.name,
          content: this.buildAiProjectContent(),
          project: this.project
        })
        if (!result) {
          this.$message.warning('AI 未返回任务拆解结果')
          return
        }
        const normalized = result?.normalized || normalizeProjectTaskPayload(result)
        this.aiTaskCard = {
          phases: Array.isArray(normalized.phases) ? normalized.phases : [],
          executionOrder: Array.isArray(normalized.executionOrder) ? normalized.executionOrder : [],
          risks: Array.isArray(normalized.risks) ? normalized.risks : [],
          rawText: normalized.rawText || result?.text || ''
        }
        this.aiProjectTasks = normalized.displayText || result?.displayText || result?.text || ''
        this.aiActiveTab = 'tasks'
        this.lastAiModelLabel = this.currentAiModelLabel
        this.$message.success('AI 任务拆解生成成功')
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || error.message || 'AI 任务拆解生成失败')
      } finally {
        this.aiTaskLoading = false
      }
    },

    clearAiResult() {
      this.aiProjectSummary = ''
      this.aiProjectTasks = ''
      this.aiSummaryCard = {
        overview: '',
        scenarios: [],
        features: [],
        risks: [],
        nextActions: [],
        rawText: ''
      }
      this.aiTaskCard = {
        phases: [],
        executionOrder: [],
        risks: [],
        rawText: ''
      }
      this.aiActiveTab = 'summary'
      this.lastAiModelLabel = ''
    },

    async fetchProjectStarState() {
      const token = getToken ? getToken() : ''
      if (!token) {
        this.project.starred = false
        return
      }
      try {
        const res = await getProjectStarStatus(this.projectId)
        const data = res?.data || res || {}
        this.project.starred = !!data.starred
        if (data.stars !== undefined && data.stars !== null && !Number.isNaN(Number(data.stars))) {
          this.project.stars = Number(data.stars)
        }
      } catch (error) {
        console.error(error)
      }
    },

    normalizeTaskItem(item = {}) {
      return {
        ...item,
        assigneeName: item.assigneeName || '',
        assigneeAvatar: item.assigneeAvatar || '',
        creatorName: item.creatorName || '',
        status: item.status || 'todo',
        priority: item.priority || 'medium'
      }
    },

    async fetchMemberSnapshot() {
      const token = getToken ? getToken() : ''
      if (!token) {
        this.memberList = []
        this.memberListLoaded = true
        return
      }
      try {
        const res = await listProjectMembers(this.projectId)
        const list = Array.isArray(res.data) ? res.data : []
        this.memberList = list.map(item => ({ ...item }))
      } catch (error) {
        this.memberList = []
      } finally {
        this.memberListLoaded = true
      }
    },

    showEditProjectDialog() {
      if (!this.canManageProject) {
        this.$message.warning('仅项目所有者或管理员可编辑项目信息')
        return
      }
      this.showEditDialog = true
    },

    async fetchProjectTasks() {
      if (!this.canSeeTaskCollaboration) {
        this.taskList = []
        return
      }
      this.taskBoardLoading = true
      try {
        const res = await listProjectTasks(this.projectId)
        const list = Array.isArray(extractApiData(res)) ? extractApiData(res) : []
        this.taskList = list.map(this.normalizeTaskItem)
      } catch (error) {
        console.error(error)
        this.taskList = []
      } finally {
        this.taskBoardLoading = false
      }
    },

    async fetchMyTasks() {
      const token = getToken ? getToken() : ''
      if (!token || !this.canSeeTaskCollaboration) {
        this.myTaskList = []
        return
      }
      this.myTasksLoading = true
      try {
        const res = await listMyTasks(this.projectId)
        const list = Array.isArray(extractApiData(res)) ? extractApiData(res) : []
        this.myTaskList = list.map(this.normalizeTaskItem)
      } catch (error) {
        console.error(error)
        this.myTaskList = []
      } finally {
        this.myTasksLoading = false
      }
    },

    syncTaskCollections(updatedTask = {}) {
      const normalized = this.normalizeTaskItem(updatedTask)
      const targetId = String(normalized.id || '')
      if (!targetId) return
      const replaceItem = item => String(item.id) === targetId ? { ...item, ...normalized } : item
      this.taskList = this.taskList.map(replaceItem)
      this.myTaskList = this.myTaskList.map(replaceItem)
      if (this.selectedTaskForCollab && String(this.selectedTaskForCollab.id) === targetId) {
        this.selectedTaskForCollab = { ...this.selectedTaskForCollab, ...normalized }
      }
    },

    findTaskFromCollections(taskId) {
      const targetId = String(taskId || '')
      if (!targetId) return null
      const merged = [...this.taskList, ...this.myTaskList]
      return merged.find(item => String(item && item.id) === targetId) || null
    },

    getTaskCollabRouteQuery(taskId, tab = 'overview') {
      const query = { ...this.$route.query }
      query.projectId = this.projectId
      query.taskId = taskId
      query.taskTab = tab || 'overview'
      return query
    },

    async syncTaskCollabRoute(taskId, tab = 'overview') {
      const currentTaskId = this.$route.query.taskId
      const currentTaskTab = this.$route.query.taskTab || 'overview'
      if (String(currentTaskId || '') === String(taskId || '') && String(currentTaskTab || '') === String(tab || 'overview')) {
        return
      }
      this.taskCollabRouteApplying = true
      try {
        await this.$router.replace({
          path: this.$route.path,
          query: this.getTaskCollabRouteQuery(taskId, tab)
        })
      } catch (e) {
      } finally {
        this.$nextTick(() => {
          this.taskCollabRouteApplying = false
        })
      }
    },

    async clearTaskCollabRoute() {
      const query = { ...this.$route.query }
      delete query.taskId
      delete query.taskTab
      this.taskCollabRouteApplying = true
      try {
        await this.$router.replace({
          path: this.$route.path,
          query
        })
      } catch (e) {
      } finally {
        this.$nextTick(() => {
          this.taskCollabRouteApplying = false
        })
      }
    },

    async restoreTaskCollabFromRoute() {
      if (this.taskCollabRouteApplying || this.taskCollabRouteRestoring) return
      if (!this.pageAccessResolved || !this.canSeeTaskCollaboration) return

      const taskId = this.$route.query.taskId
      const tab = this.$route.query.taskTab || 'overview'
      if (!taskId) return

      this.taskCollabRouteRestoring = true
      try {
        if (!this.taskList.length) {
          await this.fetchProjectTasks()
        }
        if (!this.myTaskList.length && getToken && getToken()) {
          await this.fetchMyTasks()
        }

        const matched = this.findTaskFromCollections(taskId)
        if (!matched) return

        this.selectedTaskForCollab = { ...matched }
        this.taskCollabActiveTab = tab || 'overview'
        this.taskCollabDrawerVisible = true
        this.taskCollabRefreshSeed += 1
      } finally {
        this.taskCollabRouteRestoring = false
      }
    },

    taskCollabPanelKey(name) {
      const taskId = this.selectedTaskForCollab && this.selectedTaskForCollab.id ? this.selectedTaskForCollab.id : 'empty'
      return `${name}-${taskId}-${this.taskCollabRefreshSeed}`
    },

    async openTaskCollabDrawer(task, tab = 'overview') {
      if (!task || !task.id) return
      const latestTask = this.findTaskFromCollections(task.id) || task
      this.selectedTaskForCollab = { ...latestTask }
      this.taskCollabActiveTab = tab || 'overview'
      this.taskCollabDrawerVisible = true
      this.taskCollabRefreshSeed += 1
      await this.syncTaskCollabRoute(task.id, this.taskCollabActiveTab)
    },

    async handleTaskCollabChanged(payload = {}) {
      const currentTaskId = this.selectedTaskForCollab && this.selectedTaskForCollab.id

      if (payload && payload.partial && currentTaskId) {
        const latestTask = this.findTaskFromCollections(currentTaskId)
        if (latestTask) {
          this.selectedTaskForCollab = { ...latestTask }
        }
        this.taskCollabRefreshSeed += 1
        await this.syncTaskCollabRoute(currentTaskId, this.taskCollabActiveTab || 'overview')
        return
      }

      const jobs = [this.fetchProjectTasks()]
      const token = getToken ? getToken() : ''
      if (token) {
        jobs.push(this.fetchMyTasks())
      }
      try {
        await Promise.all(jobs)
        const latestTask = this.findTaskFromCollections(currentTaskId)
        if (latestTask) {
          this.selectedTaskForCollab = { ...latestTask }
          await this.syncTaskCollabRoute(latestTask.id, this.taskCollabActiveTab || 'overview')
        }
        this.taskCollabRefreshSeed += 1
      } catch (error) {
        console.error(error)
      }
    },

    async handleTaskCollabDrawerClosed() {
      this.taskCollabActiveTab = 'overview'
      this.selectedTaskForCollab = null
      await this.clearTaskCollabRoute()
    },

    handleTaskCollabStatusUpdated(payload = {}) {
      const taskId = payload && payload.taskId
      const status = payload && payload.status
      if (!taskId || !status) return

      const applyUpdate = (list = []) => list.map(item => {
        if (!item || String(item.id) !== String(taskId)) return item
        return { ...item, status }
      })

      this.taskList = applyUpdate(this.taskList)
      this.myTaskList = applyUpdate(this.myTaskList)

      if (this.selectedTaskForCollab && String(this.selectedTaskForCollab.id) === String(taskId)) {
        this.selectedTaskForCollab = {
          ...this.selectedTaskForCollab,
          status
        }
      }

      this.taskCollabRefreshSeed += 1
    },

    getComparableTaskCycleTime(value) {
      if (!value) return 0
      const time = new Date(value).getTime()
      return Number.isNaN(time) ? 0 : time
    },

    isHistoricalDoneTaskForCurrentUser(task) {
      if (!task || task.status !== 'done' || this.canManageProject) return false
      if (this.currentUserId === null || this.currentUserId === undefined || this.currentUserId === '') return false
      if (Number(task.assigneeId) !== Number(this.currentUserId)) return false
      const completedCycle = this.getComparableTaskCycleTime(task.completedMemberJoinedAt)
      const currentCycle = this.getComparableTaskCycleTime(this.currentMemberRecord && this.currentMemberRecord.joinedAt)
      if (!completedCycle || !currentCycle) return false
      return completedCycle !== currentCycle
    },

    async submitTaskReopenRequest(task, targetStatus) {
      if (!task || !task.id) return
      try {
        const { value } = await this.$prompt('请填写重开原因，管理员或项目所有者确认后才会将任务改回未完成。', '提交重开申请', {
          confirmButtonText: '提交申请',
          cancelButtonText: '取消',
          inputType: 'textarea',
          inputPlaceholder: '例如：验收发现遗漏、联调失败、完成结论需要撤回等',
          inputValidator: (inputValue) => {
            if (!String(inputValue || '').trim()) {
              return '请填写重开原因'
            }
            if (String(inputValue || '').trim().length < 2) {
              return '重开原因至少 2 个字'
            }
            return true
          }
        })
        await submitTaskReopenRequestApi(task.id, {
          targetStatus,
          reason: String(value || '').trim()
        })
        this.$message.success('已提交重开申请，请等待管理员或项目所有者确认')
        this.taskCollabRefreshSeed += 1
      } catch (error) {
        if (error === 'cancel' || error === 'close') return
        console.error(error)
        this.$message.error(error.response?.data?.message || '提交重开申请失败')
      }
    },

    async handleQuickTaskStatusChange(task, status) {
      if (!task || !task.id || !status || task.status === status) return
      if (!this.canSeeTaskCollaboration) {
        this.$message.warning('加入项目后才可参与任务协作')
        return
      }
      if (!this.canManageProject && task.status === 'done' && status !== 'done') {
        if (this.isHistoricalDoneTaskForCurrentUser(task)) {
          this.$message.error('你不能修改上一个入组周期已完成的任务，请联系项目管理员或所有者处理')
          return
        }
        if (Number(task.assigneeId) !== Number(this.currentUserId)) {
          this.$message.error('\u5f53\u524d\u4efb\u52a1\u5df2\u5b8c\u6210\uff0c\u53ea\u6709\u8d1f\u8d23\u4eba\u672c\u4eba\u53ef\u4ee5\u63d0\u4ea4\u91cd\u5f00\u7533\u8bf7')
          return
        }
        await this.submitTaskReopenRequest(task, status)
        return
      }
      this.taskQuickUpdatingId = task.id
      try {
        const res = await updateTaskStatus(task.id, { status })
        const updatedTask = extractApiData(res) || { ...task, status }
        this.syncTaskCollections({ ...task, ...updatedTask, status })
        this.$message.success('\u4efb\u52a1\u72b6\u6001\u5df2\u66f4\u65b0')
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '更新任务状态失败')
      } finally {
        this.taskQuickUpdatingId = null
      }
    },

    getTaskSortTime(task) {
      return this.parseDateTime(task && (task.updatedAt || task.createdAt || task.dueDate))
    },

    getTaskDueTimestamp(task) {
      return this.parseDateTime(task && task.dueDate)
    },

    parseDateTime(value) {
      if (!value) return 0
      const time = new Date(value).getTime()
      return Number.isNaN(time) ? 0 : time
    },

    isTaskOverdue(task) {
      if (!task || task.status === 'done' || !task.dueDate) return false
      const dueTime = this.parseDateTime(task.dueDate)
      if (!dueTime) return false
      return dueTime < Date.now()
    },

    isTaskDueToday(task) {
      if (!task || !task.dueDate) return false
      const date = new Date(task.dueDate)
      if (Number.isNaN(date.getTime())) return false
      const now = new Date()
      return date.getFullYear() === now.getFullYear() &&
        date.getMonth() === now.getMonth() &&
        date.getDate() === now.getDate()
    },

    getTaskAssigneeName(task) {
      return (task && task.assigneeName) || '未分配'
    },

    getTaskTimeLabel(task) {
      if (task && task.updatedAt) return `更新于 ${this.formatTaskShortTime(task.updatedAt)}`
      if (task && task.createdAt) return `创建于 ${this.formatTaskShortTime(task.createdAt)}`
      return '时间未知'
    },

    getTaskDueLabel(task) {
      if (task && task.dueDate) return `截止 ${this.formatTaskShortTime(task.dueDate)}`
      return '未设置截止时间'
    },

    formatTaskShortTime(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return `${date.toLocaleDateString('zh-CN')} ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
    },

    formatTaskDueClock(value) {
      if (!value) return '未设置时间'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return `今天 ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
    },

    getTaskStatusType(status) {
      return {
        todo: 'info',
        in_progress: 'warning',
        done: 'success'
      }[status] || 'info'
    },

    getTaskStatusText(status) {
      return {
        todo: '待处理',
        in_progress: '进行中',
        done: '已完成'
      }[status] || status || '未知状态'
    },

    getTaskPriorityType(priority) {
      return {
        low: 'info',
        medium: '',
        high: 'warning',
        urgent: 'danger'
      }[priority] || 'info'
    },

    getTaskPriorityText(priority) {
      return {
        low: '低',
        medium: '中',
        high: '高',
        urgent: '紧急'
      }[priority] || priority || '中'
    },

    async fetchProjectDetail() {
      try {
        const res = await getProjectDetail(this.projectId)
        const data = res.data || {}
        this.project = {
          ...this.project,
          ...data,
          name: data.name || '',
          description: data.description || '',
          category: data.category || '',
          status: data.status || 'draft',
          visibility: data.visibility || 'public',
          tags: data.tags || '',
          stars: data.stars || 0,
          downloads: data.downloads || 0,
          views: data.views || 0,
          starred: !!data.starred,
          authorId: data.authorId || null,
          authorName: data.authorName || '',
          authorAvatar: data.authorAvatar || '',
          createdAt: data.createdAt || '',
          updatedAt: data.updatedAt || '',
          readme: '',
          members: data.members || [],
          tasks: data.tasks || [],
          files: data.files || [],
          contributors: data.contributors || [],
          relatedProjects: data.relatedProjects || []
        }
        this.editForm = {
          name: this.project.name,
          description: this.project.description,
          category: this.project.category,
          status: this.project.status,
          visibility: this.project.visibility || 'public',
          tagsText: parseTags(this.project.tags).join(', ')
        }
      } catch (e) {
        console.error('getProjectDetail error:', e?.response?.data || e)
        this.$message.error(
          e?.response?.data?.message ||
          e?.response?.data?.msg ||
          '项目详情加载失败'
        )
      }
    },

    async fetchContributors() {
      try {
        const res = await getProjectContributors(this.projectId)
        const list = Array.isArray(res.data) ? res.data : []
        this.contributors = list.map(item => ({
          ...item,
          displayName: item.nickname || item.username || `用户${item.userId || ''}`
        }))
      } catch (error) {
        console.error(error)
        this.contributors = []
      }
    },

    async fetchRelatedProjects() {
      try {
        const res = await getRelatedProjects(this.projectId, { size: 6 })
        this.relatedProjects = Array.isArray(res.data) ? res.data : []
      } catch (error) {
        console.error(error)
        this.relatedProjects = []
      }
    },

    async fetchFiles() {
      let files = []
      try {
        const res = await listProjectFiles(this.projectId)
        files = Array.isArray(extractApiData(res)) ? extractApiData(res) : []
        this.project.files = files
        this.fileTree = this.buildFileTree(files)
        this.syncSelectedFileIds()
      } catch (error) {
        console.error('list project files error:', error)
        this.project.files = []
        this.fileTree = []
        this.$message.error(error.response?.data?.message || '获取项目文件失败')
      }

      try {
        await this.loadReadme(files)
      } catch (error) {
        console.error('load readme error:', error)
        this.project.readme = ''
        this.project.readmeTitle = ''
        this.project.readmeSource = ''
        this.project.readmeDocId = null
      }
    },

    buildFileTree(files) {
      const root = []
      const nodeMap = {}

      const normalizePath = (value) => String(value || '')
        .replace(/\\/g, '/')
        .replace(/^\/+/, '')
        .replace(/\/+/g, '/')
        .replace(/\/$/, '')
        .trim()

      const stripKnownStoragePrefix = (value) => {
        let v = String(value || '').replace(/\\/g, '/').trim()
        v = v.replace(/^[A-Za-z]:\/+/, '')
        v = v.replace(/^\/+/, '')
        v = v.replace(/^.*?\/runtime\/template-generated\/project-\d+\//i, '')
        v = v.replace(/^runtime\/template-generated\/project-\d+\//i, '')
        v = v.replace(/^uploads\/project\/\d+\/(main|version)\/\d{4}-\d{2}-\d{2}\//i, '')
        v = v.replace(/^uploads\/project\/\d+\/(main|version)\//i, '')
        return normalizePath(v)
      }

      const isFolderRecord = (file) => String(file.fileType || file.type || '').toLowerCase() === 'folder'

      const looksLikeStoredGeneratedName = (value) => {
        const text = String(value || '').trim().toLowerCase()
        return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(\.[a-z0-9_-]+)?$/.test(text)
      }

      const resolveRelativePath = (file) => {
        const explicitRelativePath = normalizePath(file.relativePath || file.relative_file_path || '')
        if (explicitRelativePath) return explicitRelativePath

        const fileName = normalizePath(file.fileName || file.name || '')
        const filePath = stripKnownStoragePrefix(file.path || file.filePath || '')

        if (fileName && fileName.includes('/')) {
          return fileName
        }

        if (filePath && (!fileName || looksLikeStoredGeneratedName(fileName))) {
          return filePath
        }

        if (fileName) {
          return fileName
        }

        if (filePath) {
          return filePath
        }

        return `file-${file.id || 'unknown'}`
      }

      const ensureFolderNode = (folderPath) => {
        const normalized = normalizePath(folderPath)
        if (!normalized) return null
        if (nodeMap[normalized]) return nodeMap[normalized]

        const segments = normalized.split('/').filter(Boolean)
        let currentPath = ''
        let currentChildren = root
        let currentNode = null

        segments.forEach((segment) => {
          currentPath = currentPath ? `${currentPath}/${segment}` : segment
          if (!nodeMap[currentPath]) {
            const folderNode = {
              name: segment,
              path: currentPath,
              type: 'folder',
              children: []
            }
            nodeMap[currentPath] = folderNode
            currentChildren.push(folderNode)
          }
          currentNode = nodeMap[currentPath]
          currentChildren = currentNode.children
        })

        return currentNode
      }

      ;(files || []).forEach((file) => {
        const relativePath = resolveRelativePath(file)
        if (!relativePath) return

        if (isFolderRecord(file)) {
          ensureFolderNode(relativePath)
          return
        }

        const segments = relativePath.split('/').filter(Boolean)
        const fileName = segments[segments.length - 1] || `文件${file.id}`
        const folderPath = segments.slice(0, -1).join('/')
        const extFromName = fileName.includes('.') ? fileName.split('.').pop().toLowerCase() : ''
        const actualType = String(file.fileType || '').trim().toLowerCase() || extFromName

        let children = root
        if (folderPath) {
          const folderNode = ensureFolderNode(folderPath)
          children = folderNode ? folderNode.children : root
        }

        children.push({
          id: file.id,
          name: fileName,
          path: relativePath,
          type: 'file',
          size: file.fileSizeBytes || 0,
          extension: extFromName,
          actualType,
          isMain: !!file.isMain,
          raw: file,
          children: []
        })
      })

      const sortNodes = (nodes = []) => {
        nodes.sort((a, b) => {
          if (a.type !== b.type) {
            return a.type === 'folder' ? -1 : 1
          }
          return String(a.name || '').localeCompare(String(b.name || ''), 'zh-CN')
        })
        nodes.forEach(item => {
          if (Array.isArray(item.children) && item.children.length) {
            sortNodes(item.children)
          }
        })
      }

      sortNodes(root)
      return root
    },

    normalizeProjectDoc(item = {}) {
      const content = item.content !== undefined
        ? item.content
        : (item.currentContent !== undefined ? item.currentContent : item.current_content)

      return {
        ...item,
        id: item.id ?? item.docId ?? null,
        projectId: item.projectId ?? item.project_id ?? this.projectId ?? null,
        title: item.title || '',
        docType: item.docType || item.doc_type || 'wiki',
        status: item.status || 'draft',
        visibility: item.visibility || 'project',
        content: content == null ? '' : String(content),
        currentVersion: item.currentVersion ?? item.current_version ?? 1,
        creatorId: item.creatorId ?? item.creator_id ?? null,
        editorId: item.editorId ?? item.editor_id ?? null,
        createdAt: item.createdAt || item.created_at || '',
        updatedAt: item.updatedAt || item.updated_at || '',
        readmeCandidate: !!item.readmeCandidate,
        readmePriority: item.readmePriority ?? item.readme_priority ?? 0
      }
    },

    sortProjectDocs(list = []) {
      return [...list].sort((a, b) => {
        const pa = Number(a.readmePriority || 0)
        const pb = Number(b.readmePriority || 0)
        if (pa !== pb) return pb - pa
        const ta = new Date(a.updatedAt || a.createdAt || 0).getTime()
        const tb = new Date(b.updatedAt || b.createdAt || 0).getTime()
        return tb - ta
      })
    },

    mergeProjectDocCache(detail) {
      if (!detail || !detail.id) return
      const normalized = this.normalizeProjectDoc(detail)
      const list = Array.isArray(this.projectDocs) ? [...this.projectDocs] : []
      const index = list.findIndex(item => Number(item.id) === Number(normalized.id))
      if (index >= 0) {
        list.splice(index, 1, { ...list[index], ...normalized })
      } else {
        list.unshift(normalized)
      }
      this.projectDocs = this.sortProjectDocs(list)
    },

    async fetchProjectDocsFromApi(params = {}) {
      return listProjectDocs(this.projectId, params)
    },

    async fetchProjectDocDetailFromApi(docId) {
      return getProjectDoc(docId)
    },

    async ensureProjectDocsLoaded(force = false) {
      if (!force && Array.isArray(this.projectDocs) && this.projectDocs.length) {
        return this.projectDocs
      }
      if (this.projectId === null || this.projectId === undefined || String(this.projectId) === '') {
        this.projectDocs = []
        return []
      }
      try {
        this.projectDocsLoading = true
        const res = await this.fetchProjectDocsFromApi()
        const rows = Array.isArray(extractApiData(res)) ? extractApiData(res) : []
        const normalized = this.sortProjectDocs(rows.map(item => this.normalizeProjectDoc(item)))
        this.projectDocs = normalized
        return normalized
      } catch (error) {
        console.error('list project docs error:', error?.response?.data || error)
        if (force) {
          this.projectDocs = []
        }
        return Array.isArray(this.projectDocs) ? this.projectDocs : []
      } finally {
        this.projectDocsLoading = false
      }
    },

    async selectProjectDoc(item, openDrawer = false) {
      if (!item || !item.id) {
        if (openDrawer) this.projectDocDrawerVisible = true
        return null
      }
      try {
        const res = await this.fetchProjectDocDetailFromApi(item.id)
        const detail = this.normalizeProjectDoc(extractApiData(res) || item)
        this.activeProjectDoc = detail
        this.mergeProjectDocCache(detail)
        if (openDrawer) this.projectDocDrawerVisible = true
        return detail
      } catch (error) {
        console.error('get project doc error:', error?.response?.data || error)
        const normalized = this.normalizeProjectDoc(item)
        this.activeProjectDoc = normalized
        this.mergeProjectDocCache(normalized)
        if (openDrawer) this.projectDocDrawerVisible = true
        return normalized
      }
    },

    async openProjectDocDrawer(targetDoc = null) {
      await this.ensureProjectDocsLoaded()
      if (targetDoc) {
        await this.selectProjectDoc(targetDoc, true)
        return
      }
      if (this.activeProjectDoc && this.activeProjectDoc.id) {
        this.projectDocDrawerVisible = true
        return
      }
      if (this.project.readmeDocId) {
        const matched = this.projectDocs.find(item => Number(item.id) === Number(this.project.readmeDocId))
        if (matched) {
          await this.selectProjectDoc(matched, true)
          return
        }
      }
      if (this.projectDocs.length) {
        await this.selectProjectDoc(this.projectDocs[0], true)
        return
      }
      this.projectDocDrawerVisible = true
    },

    async refreshProjectDocs() {
      const docs = await this.ensureProjectDocsLoaded(true)
      if (!docs.length) {
        this.activeProjectDoc = null
        return
      }
      const currentId = this.activeProjectDoc && this.activeProjectDoc.id
      const current = currentId ? docs.find(item => Number(item.id) === Number(currentId)) : null
      const primary = current || docs[0]
      if (primary) {
        await this.selectProjectDoc(primary, false)
      }
    },

    handleProjectDocKeywordInput() {
      const list = this.filteredProjectDocs
      if (!list.length) return
      const currentId = this.activeProjectDoc && this.activeProjectDoc.id
      const exists = list.some(item => Number(item.id) === Number(currentId))
      if (!exists) {
        this.selectProjectDoc(list[0], false)
      }
    },

    handleProjectDocManageClick() {
      if (!this.canManageProject) {
        this.openProjectDocDrawer()
        return
      }
      this.goToProjectManage('doc-manage')
    },

    handleOpenDocManage(payload = {}) {
      if (!this.canManageProject) {
        this.openProjectDocDrawer(payload && payload.docId ? { id: payload.docId } : null)
        return
      }
      this.goToProjectManage(payload && typeof payload === 'object' ? { tab: 'doc-manage', ...payload } : 'doc-manage')
    },

    async handleSelectProjectDoc(doc) {
      await this.selectProjectDoc(doc, true)
    },

    async handleRefreshProjectDocs() {
      await this.refreshProjectDocs()
      if (this.project.readmeSource === 'doc' || this.project.readmeDocId) {
        await this.loadReadmeFromPrimaryDoc()
      }
    },

    getProjectDocTypeText(value) {
      const map = {
        wiki: '说明文档',
        spec: '需求规格',
        meeting_note: '会议纪要',
        design: '设计文档',
        manual: '使用手册',
        other: '其他'
      }
      return map[value] || value || '-'
    },

    getProjectDocStatusText(value) {
      const map = {
        draft: '草稿',
        published: '已发布',
        archived: '已归档'
      }
      return map[value] || value || '-'
    },

    getProjectDocVisibilityText(value) {
      const map = {
        project: '项目内',
        team: '团队',
        private: '仅自己'
      }
      return map[value] || value || '-'
    },

    async loadReadme(files) {
      const docLoaded = await this.loadReadmeFromPrimaryDoc()
      if (docLoaded) {
        return
      }
      await this.loadReadmeFromProjectFiles(files)
    },

    async loadReadmeFromProjectFiles(files) {
      const normalizePath = (value) => String(value || '')
        .replace(/\\/g, '/')
        .replace(/^\/+/, '')
        .replace(/\/+/g, '/')
        .trim()

      const resolveReadmePath = (file) => {
        const relativePath = normalizePath(file.relativePath || file.relative_file_path || '')
        if (relativePath) return relativePath

        const fileName = normalizePath(file.fileName || file.file_name || file.name || '')
        if (fileName) return fileName

        const filePath = normalizePath(file.path || file.filePath || file.file_path || '')
        if (!filePath) return ''

        return filePath
      }

      const list = (files || [])
        .map(file => {
          const rawPath = resolveReadmePath(file)
          const lowerPath = rawPath.toLowerCase()
          return {
            file,
            rawPath,
            lowerPath,
            depth: rawPath ? rawPath.split('/').length - 1 : 0
          }
        })
        .filter(item => {
          return item.lowerPath === 'readme'
            || item.lowerPath === 'readme.md'
            || item.lowerPath === 'readme.txt'
            || item.lowerPath === 'readme.markdown'
            || item.lowerPath.endsWith('/readme')
            || item.lowerPath.endsWith('/readme.md')
            || item.lowerPath.endsWith('/readme.txt')
            || item.lowerPath.endsWith('/readme.markdown')
        })
        .sort((a, b) => {
          const aRoot = /^readme(\.(md|txt|markdown))?$/.test(a.lowerPath) ? 0 : 1
          const bRoot = /^readme(\.(md|txt|markdown))?$/.test(b.lowerPath) ? 0 : 1
          if (aRoot !== bRoot) return aRoot - bRoot
          if (a.depth !== b.depth) return a.depth - b.depth
          return a.rawPath.localeCompare(b.rawPath, 'zh-CN')
        })

      const readmeFile = list.length ? list[0].file : null

      if (!readmeFile) {
        this.project.readme = ''
        this.project.readmeTitle = ''
        this.project.readmeSource = ''
        this.project.readmeDocId = null
        return false
      }

      try {
        const blob = await previewProjectFile(readmeFile.id)
        this.project.readme = await safeReadBlobText(blob)
        this.project.readmeTitle = readmeFile.fileName || readmeFile.file_name || readmeFile.name || 'README'
        this.project.readmeSource = 'file'
        this.project.readmeDocId = null
        return true
      } catch (error) {
        console.error(error)
        this.project.readme = ''
        this.project.readmeTitle = ''
        this.project.readmeSource = ''
        this.project.readmeDocId = null
        return false
      }
    },

    async loadReadmeFromPrimaryDoc() {
      try {
        const res = await getProjectPrimaryReadme(this.projectId)
        const data = extractApiData(res)
        if (!data) {
          return false
        }
        const detail = this.normalizeProjectDoc(data)
        const content = String(detail.content || '').trim()
        if (!content) {
          return false
        }

        this.project.readme = detail.content || ''
        this.project.readmeTitle = detail.title || `${this.project.name || '未命名项目'} README`
        this.project.readmeSource = 'doc'
        this.project.readmeDocId = detail.id || null

        this.activeProjectDoc = detail
        this.mergeProjectDocCache(detail)
        return true
      } catch (error) {
        console.error('load readme from primary doc error:', error?.response?.data || error)
        return false
      }
    },

    async handleDownloadCurrentFile() {
      if (!this.currentFile || !this.currentFile.id) return
      const blob = await downloadFile(this.currentFile.id)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = this.currentFile.fileName || 'file'
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    },

    async handleFileClick(node) {
      if (!node) return

      if (node.type === 'folder') {
        const firstFile = this.findFirstFileNode(Array.isArray(node.children) ? node.children : [])
        if (firstFile) {
          await this.handleFileClick(firstFile)
        }
        return
      }

      if (!node.id) return

      try {
        this.fileLoading = true
        this.clearPreviewBlobUrl()

        const [blob, versionRes] = await Promise.all([
          previewProjectFile(node.id),
          listFileVersions(node.id).catch(() => ({ data: [] }))
        ])

        const versionData = extractApiData(versionRes)
        const versions = Array.isArray(versionData) ? versionData : []

        this.currentFile = {
          ...this.buildEmptyCurrentFile(),
          ...node,
          versions,
          previewLoading: true
        }

        const previewPayload = await this.resolveFilePreview(this.currentFile, blob)

        this.currentFile = {
          ...this.buildEmptyCurrentFile(),
          ...node,
          versions,
          blob,
          previewLoading: false,
          ...previewPayload
        }
      } catch (error) {
        console.error(error)
        this.currentFile = {
          ...this.buildEmptyCurrentFile(),
          ...node,
          previewLoading: false,
          previewError: error?.message || '读取文件失败'
        }
        this.$message.error(error?.response?.data?.message || error?.message || '读取文件失败')
      } finally {
        this.fileLoading = false
      }
    },

    async selectFile(node) {
      if (!node) return
      if (node.type === 'file' && node.id) {
        await this.handleFileClick(node)
        return
      }

      const firstFile = this.findFirstFileNode(Array.isArray(node.children) ? node.children : [])
      if (firstFile) {
        await this.handleFileClick(firstFile)
      }
    },

    findFirstFileNode(nodes = []) {
      for (const item of nodes) {
        if (!item) continue
        if (item.type === 'file' && item.id) {
          return item
        }
        if (Array.isArray(item.children) && item.children.length) {
          const matched = this.findFirstFileNode(item.children)
          if (matched) {
            return matched
          }
        }
      }
      return null
    },

    async toggleStar() {
      this.starLoading = true
      try {
        const res = this.project.starred
          ? await unstarProject(this.projectId)
          : await starProject(this.projectId)
        const data = res.data || {}
        this.project.starred = !!data.starred
        this.project.stars = data.stars != null ? data.stars : this.project.stars
        this.$message.success(this.project.starred ? '收藏成功' : '已取消收藏')
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '操作失败')
      } finally {
        this.starLoading = false
      }
    },

    async submitEdit() {
      if (!this.canManageProject) {
        this.$message.warning('仅项目所有者或管理员可编辑项目信息')
        return
      }
      this.saveLoading = true
      try {
        const payload = {
          name: this.editForm.name,
          description: this.editForm.description,
          category: this.editForm.category,
          status: this.editForm.status,
          visibility: this.editForm.visibility,
          tags: JSON.stringify(parseTags(this.editForm.tagsText)),
          updatedAt: toBackendDateTime(new Date())
        }
        await updateProject(this.projectId, payload)
        this.$message.success('项目更新成功')
        this.showEditDialog = false
        await this.fetchProjectDetail()
        await this.fetchRelatedProjects()
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '更新项目失败')
      } finally {
        this.saveLoading = false
      }
    },

    openUploadDialog(isVersion) {
      if (!this.canManageProject) {
        this.$message.warning('仅项目所有者或管理员可管理项目文件')
        return
      }
      if (isVersion && !this.currentFile.id) {
        this.$message.warning('请先选择一个文件')
        return
      }
      this.uploadDialog = {
        visible: true,
        isVersion,
        file: null,
        files: []
      }
      this.$nextTick(() => {
        if (this.$refs.uploadInput) this.$refs.uploadInput.value = ''
      })
    },

    getNextAvailableVersion() {
      const current = String(this.currentFile.version || '').trim()
      const existing = new Set(
        (Array.isArray(this.currentFile.versions) ? this.currentFile.versions : [])
          .map(item => String(item && item.version ? item.version : '').trim())
          .filter(Boolean)
      )

      let candidate = this.incrementSemanticVersion(current || '1.0.0')
      let guard = 0
      while (existing.has(candidate) && guard < 100) {
        candidate = this.incrementSemanticVersion(candidate)
        guard += 1
      }
      return candidate
    },

    incrementSemanticVersion(version) {
      const source = String(version || '').trim()
      if (!source) return '1.0.0'
      const parts = source.split('.')
      const last = Number(parts[parts.length - 1])
      if (Number.isNaN(last)) {
        return `${source}.1`
      }
      parts[parts.length - 1] = String(last + 1)
      return parts.join('.')
    },

    closeUploadDialog() {
      this.uploadDialog.visible = false
      this.uploadDialog.file = null
      this.uploadDialog.files = []
      if (this.$refs.uploadInput) this.$refs.uploadInput.value = ''
    },

    handlePickedFile(event) {
      const files = Array.from((event.target && event.target.files) || [])
        .filter(Boolean)
        .map(file => (file && file.raw ? file.raw : file))
        .filter(file => file instanceof File || file instanceof Blob)

      this.uploadDialog.files = files
      this.uploadDialog.file = files[0] || null
    },

    async submitUpload() {
      const pickedFiles = Array.isArray(this.uploadDialog.files) ? this.uploadDialog.files : []

      const getRawFile = (file) => {
        const raw = file && file.raw ? file.raw : file
        return raw instanceof File || raw instanceof Blob ? raw : null
      }

      if (this.uploadDialog.isVersion) {
        if (!getRawFile(this.uploadDialog.file)) {
          this.$message.warning('请选择文件')
          return
        }
      } else if (!pickedFiles.length) {
        this.$message.warning('请选择文件')
        return
      }

      this.uploadLoading = true
      const previousCurrentFileId = this.currentFile.id

      try {
        if (this.uploadDialog.isVersion) {
          const rawFile = getRawFile(this.uploadDialog.file)
          const formData = new FormData()
          formData.append('file', rawFile)
          await uploadFileNewVersion(this.currentFile.id, formData)
        } else if (pickedFiles.length === 1) {
          const rawFile = getRawFile(pickedFiles[0])
          if (!rawFile) {
            this.$message.error('所选文件无效，请重新选择')
            return
          }

          const isZipFile = /\.zip$/i.test(rawFile.name || '')

          if (isZipFile) {
            await uploadProjectZip(this.projectId, rawFile)
          } else {
            const formData = new FormData()
            formData.append('projectId', String(this.projectId))
            formData.append('file', rawFile)
            await uploadProjectFile(this.projectId, formData)
          }
        } else {
          const normalizedFiles = pickedFiles
            .map(getRawFile)
            .filter(Boolean)

          if (!normalizedFiles.length) {
            this.$message.error('所选文件无效，请重新选择')
            return
          }

          const formData = new FormData()
          formData.append('projectId', String(this.projectId))
          normalizedFiles.forEach(file => formData.append('files', file))
          await this.uploadBatchFiles(formData)
          this.selectedFileIds = []
        }

        this.$message.success(this.uploadDialog.isVersion ? '变更已加入工作区，正在前往仓库工作台' : '文件已加入工作区，正在前往仓库工作台')
        this.closeUploadDialog()
        await this.$router.push({
          path: '/projectmanage',
          query: {
            projectId: String(this.projectId),
            tab: 'repo-workbench',
            ...(previousCurrentFileId ? { fileId: String(previousCurrentFileId) } : {})
          }
        })
      } catch (e) {
        const message = e?.response?.data?.message || e?.response?.data?.msg || e?.message || '加入工作区失败'
        this.$message.error(message)
      } finally {
        this.uploadLoading = false
      }
    },

    async uploadBatchFiles(formData) {
      return uploadBatchFilesApi(formData)
    },

    async downloadBatchFiles(projectId, fileIds = []) {
      return downloadBatchFilesApi(projectId, fileIds)
    },



    async markMainFile() {
      if (!this.canManageProject) {
        this.$message.warning('仅项目所有者或管理员可设置主文件')
        return
      }
      if (!this.currentFile.id) return
      try {
        await setMainFile(this.currentFile.id)
        this.$message.success('已设置为主文件')
        await this.fetchFiles()
        const flatList = this.flattenFileTree(this.fileTree)
        const selected = flatList.find(item => item.id === this.currentFile.id)
        if (selected) await this.handleFileClick(selected)
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '设置主文件失败')
      }
    },

    async removeCurrentFile() {
      if (!this.canManageProject) {
        this.$message.warning('仅项目所有者或管理员可将文件加入工作区删除')
        return
      }
      if (!this.currentFile.id) return
      try {
        await this.$confirm(`确认将文件“${this.currentFile.name}”加入工作区删除吗？正式版本会在提交后才移除。`, '提示', {
          type: 'warning'
        })
        await this.stageProjectFilesDeleteToWorkspace([this.currentFile])
        this.$message.success('删除请求已加入工作区，请继续提交到分支')
        this.goToProjectManage('repo-workbench')
      } catch (error) {
        if (error !== 'cancel') {
          console.error(error)
          this.$message.error(error.response?.data?.message || error.message || '加入工作区删除失败')
        }
      }
    },

    async downloadCurrentFile() {
      if (!this.currentFile.id) {
        this.$message.warning('请先选择文件')
        return
      }
      try {
        const blob = await downloadFile(this.currentFile.id)
        this.triggerBlobDownload(blob, this.currentFile.name)
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '下载失败')
      }
    },

    async downloadMainFile() {
      const flatList = this.flattenFileTree(this.fileTree)
      const mainFile = flatList.find(item => item.isMain) || flatList[0]
      if (!mainFile) {
        this.$message.warning('暂无可下载文件')
        return
      }
      try {
        const blob = await downloadFile(mainFile.id)
        this.triggerBlobDownload(blob, mainFile.name)
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '下载失败')
      }
    },

    triggerBlobDownload(blob, filename) {
      if (!process.client || typeof document === 'undefined') return
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename || 'download'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    },

    async copyCurrentFileContent() {
      if (!process.client || !this.currentFile.content) return
      try {
        if (navigator.clipboard && window.isSecureContext) {
          await navigator.clipboard.writeText(this.currentFile.content)
        } else {
          const textarea = document.createElement('textarea')
          textarea.value = this.currentFile.content
          textarea.style.position = 'fixed'
          textarea.style.opacity = '0'
          document.body.appendChild(textarea)
          textarea.focus()
          textarea.select()
          document.execCommand('copy')
          document.body.removeChild(textarea)
        }
        this.$message.success('文件内容已复制')
      } catch (error) {
        console.error(error)
        this.$message.error('复制失败')
      }
    },

    escapeHtml(text) {
      return escapeHtmlValue(text)
    },

    renderMarkdownContent(content, emptyText = '暂无内容') {
      return renderMarkdownToHtml(content, emptyText)
    },

    flattenFileTree(nodes = []) {
      const list = []
      nodes.forEach(node => {
        if (node.type === 'file') {
          list.push(node)
        }
        if (node.children && node.children.length) {
          list.push(...this.flattenFileTree(node.children))
        }
      })
      return list
    },

    syncSelectedFileIds() {
      const availableIds = new Set(this.flattenFileTree(this.fileTree).map(item => item.id))
      this.selectedFileIds = this.selectedFileIds.filter(id => availableIds.has(id))
    },

    isFileChecked(fileId) {
      return this.selectedFileIds.includes(fileId)
    },

    toggleFileSelection(node, checked) {
      if (!node || node.type !== 'file' || !node.id) return
      const exists = this.selectedFileIds.includes(node.id)
      if (checked && !exists) {
        this.selectedFileIds = [...this.selectedFileIds, node.id]
      } else if (!checked && exists) {
        this.selectedFileIds = this.selectedFileIds.filter(id => id !== node.id)
      }
    },

    toggleSelectAllFiles() {
      if (this.isAllFilesSelected) {
        this.selectedFileIds = []
        return
      }
      this.selectedFileIds = this.flattenFileTree(this.fileTree).map(item => item.id)
    },

    clearSelectedFiles() {
      this.selectedFileIds = []
    },

    async handleBatchDownload() {
      if (!this.selectedFileIds.length) {
        this.$message.warning('请先选择文件')
        return
      }
      try {
        const blob = await this.downloadBatchFiles(this.projectId, this.selectedFileIds)
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = 'project-' + this.projectId + '-files.zip'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      } catch (error) {
        console.error(error)
        const message = error.response?.data?.message || error.response?.data || ''
        this.$message.error(message || '批量下载失败')
      }
    },
    async handleBatchDeleteSelectedFiles() {
      if (!this.canManageProject) {
        this.$message.warning('仅项目所有者或管理员可批量加入工作区删除')
        return
      }
      if (!this.selectedFileIds.length) {
        this.$message.warning('请先选择文件')
        return
      }
      const selectedNodes = this.flattenFileTree(this.fileTree)
        .filter(item => this.selectedFileIds.includes(item.id))
      try {
        await this.$confirm('确定将选中的 ' + selectedNodes.length + ' 个文件加入工作区删除吗？正式版本会在提交后才移除。', '提示', { type: 'warning' })
        await this.stageProjectFilesDeleteToWorkspace(selectedNodes)
        this.selectedFileIds = []
        this.$message.success('已将 ' + selectedNodes.length + ' 个文件加入工作区删除，请继续提交到分支')
        this.goToProjectManage('repo-workbench')
      } catch (error) {
        if (error !== 'cancel') {
          console.error(error)
          this.$message.error(error.response?.data?.message || error.message || '批量加入工作区删除失败')
        }
      }
    },

    async ensureWorkspaceBranchId() {
      if (this.repositoryInfo && this.repositoryInfo.defaultBranchId) {
        return Number(this.repositoryInfo.defaultBranchId)
      }
      let repository = null
      try {
        repository = extractApiData(await getProjectRepository(this.projectId))
      } catch (error) {
        repository = null
      }
      if ((!repository || !repository.id) && this.canManageProject) {
        repository = extractApiData(await initProjectRepository(this.projectId))
      }
      if (!repository || !repository.defaultBranchId) {
        throw new Error('当前项目仓库尚未准备完成，暂时无法走工作区删除')
      }
      this.repositoryInfo = repository
      return Number(repository.defaultBranchId)
    },

    normalizeCanonicalPath(value) {
      const normalized = String(value || '')
        .replace(/\\/g, '/')
        .replace(/^\/+/, '')
        .replace(/\/+/g, '/')
        .trim()
      if (!normalized) {
        throw new Error('文件缺少规范路径，无法加入工作区删除')
      }
      return `/${normalized}`
    },

    async stageProjectFilesDeleteToWorkspace(nodes = []) {
      const branchId = await this.ensureWorkspaceBranchId()
      for (const node of nodes) {
        const canonicalPath = this.normalizeCanonicalPath(
          node && (node.path || node.canonicalPath || (node.raw && node.raw.canonicalPath) || node.name)
        )
        await stageWorkspaceDelete(this.projectId, branchId, canonicalPath)
      }
    },

    filterNode(value, data) {
      if (!value) return true
      return (data.name || '').toLowerCase().includes(value.toLowerCase())
    },

    getTreeIcon(data) {
      if (data.type === 'folder') return 'el-icon-folder'
      return 'el-icon-document'
    },

    roleLabel(role) {
      return ROLE_MAP[role] || role || '成员'
    },

    mapCategory(category) {
      return CATEGORY_MAP[category] || category || '未分类'
    },

    formatTime(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return date.toLocaleString('zh-CN')
    },

    formatFileSize(size) {
      const bytes = Number(size || 0)
      if (!bytes) return '0 B'
      if (bytes < 1024) return `${bytes} B`
      if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
      return `${(bytes / 1024 / 1024).toFixed(1)} MB`
    },

    handleTaskManageClick() {
      if (!this.pageAccessResolved) return
      if (!this.canSeeTaskCollaboration) {
        this.$message.closeAll()
        this.$message.warning('只有已加入项目的成员才能查看任务协作')
        return
      }
      this.goToProjectManage('task-manage')
    },

    handleMemberManageClick() {
      if (!this.pageAccessResolved) return
      if (!this.canManageProject) {
        this.$message.closeAll()
        this.$message.warning('仅项目所有者或管理员可进入管理页面')
        return
      }
      this.goToProjectManage('member-manage')
    },

    handleProjectManageClick() {
      if (!this.pageAccessResolved) return
      if (!this.canSeeTaskCollaboration) {
        this.$message.closeAll()
        this.$message.warning('加入项目后才能进入项目工作台')
        return
      }
      this.goToProjectManage('')
    },

    goToProjectManage(payload = '') {
      if (!this.pageAccessResolved) {
        return
      }
      const isObjectPayload = payload && typeof payload === 'object'
      const tab = isObjectPayload ? (payload.tab || '') : payload
      const targetTab = tab || 'overview'
      if ([
        'overview',
        'task-manage',
        'activity-manage',
        'milestone-manage',
        'sprint-manage',
        'release-manage',
        'download-manage',
        'stat-manage'
      ].includes(targetTab) && !this.canSeeTaskCollaboration) {
        this.$message.closeAll()
        this.$message.warning('加入项目后才能进入对应协作页面')
        return
      }
      if (['member-manage', 'doc-manage', 'settings'].includes(targetTab) && !this.canManageProject) {
        this.$message.closeAll()
        this.$message.warning('仅项目所有者或管理员可进入该管理页面')
        return
      }

      const query = { projectId: this.projectId }
      query.tab = targetTab
      if (isObjectPayload && payload.docId) {
        query.docId = payload.docId
      }
      if (isObjectPayload && payload.mode) {
        query.mode = payload.mode
      }

      if (this.taskCollabDrawerVisible && this.selectedTaskForCollab && this.selectedTaskForCollab.id) {
        query.taskId = this.selectedTaskForCollab.id
        query.taskTab = this.taskCollabActiveTab || 'overview'
      }

      this.$router.push({ path: '/projectmanage', query })
    },

    async handleProjectSocialChanged() {
      this.activityTimelineKey += 1

      await Promise.all([
        this.fetchProjectDetail(),
        this.fetchMemberSnapshot(),
        this.refreshProjectDocs()
      ])

      if (this.canSeeTaskCollaboration) {
        await Promise.all([
          this.fetchProjectTasks(),
          this.fetchMyTasks()
        ])
      } else {
        this.taskList = []
        this.myTaskList = []
      }
    },

    goToDetail(id) {
      this.$router.push(`/projectdetail?projectId=${id}`)
    }
  }
}
</script>

<style>
.project-detail-container {
  max-width: 1320px;
  margin: 0 auto;
  padding: 20px;
  background: var(--it-surface);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid #dfe8f5;
  background: linear-gradient(135deg, #f8fbff 0%, #eef5ff 58%, #fdfefe 100%);
  box-shadow: 0 18px 36px rgba(148, 163, 184, 0.16);
}

.breadcrumb-wrap {
  min-width: 0;
  flex: 1;
  padding: 10px 14px;
  border-radius: 14px;
  border: 1px solid rgba(206, 221, 241, 0.9);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.breadcrumb-wrap .el-breadcrumb {
  line-height: 1;
}

.breadcrumb-wrap .el-breadcrumb__item:last-child .el-breadcrumb__inner {
  color: #1e3a5f;
  font-weight: 700;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
}

.header-actions > * {
  flex-shrink: 0;
}

.header-actions .el-button {
  min-height: 38px;
  padding: 0 16px;
  border-radius: 999px;
  border-color: #d7e3f5;
  background: #ffffff;
  color: #1d3557;
  font-weight: 700;
  letter-spacing: 0.01em;
  box-shadow: 0 8px 18px rgba(148, 163, 184, 0.14);
}

.header-actions .el-button:hover,
.header-actions .el-button:focus {
  color: #16324f;
  border-color: #bfd3f2;
  background: #f8fbff;
  transform: translateY(-1px);
}

.header-actions .el-button--success,
.header-actions .el-button--success:hover,
.header-actions .el-button--success:focus {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, #2563eb 0%, #0ea5e9 100%);
}

.header-actions .el-button--warning,
.header-actions .el-button--warning:hover,
.header-actions .el-button--warning:focus {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, #f59e0b 0%, #f97316 100%);
}

.header-actions .el-button--primary,
.header-actions .el-button--primary:hover,
.header-actions .el-button--primary:focus {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, #2563eb 0%, #14b8a6 100%);
}

.project-overview-card,
.section-card {
  border-radius: 12px;
  border: 1px solid #e8edf5;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
}

.project-overview {
  display: flex;
  gap: 20px;
}

.project-overview-card {
  overflow: hidden;
  background: var(--it-surface);
}

.overview-main {
  width: 100%;
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
}

.project-title {
  margin: 0;
  font-size: 28px;
  color: #1f2d3d;
}

.title-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.project-desc {
  margin-top: 14px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}

.tag-list {
  margin-top: 14px;
}

.tag-item {
  margin-right: 8px;
  margin-bottom: 8px;
}

.meta-row {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
}

.author-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-name {
  font-weight: 600;
  color: #303133;
}

.author-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.stats-row {
  display: flex;
  gap: 16px;
}

.stat-item {
  min-width: 72px;
  text-align: center;
  background: var(--it-surface-muted);
  border-radius: 10px;
  padding: 10px 12px;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.content-layout {
  margin-top: 20px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) clamp(336px, 25vw, 388px);
  gap: 22px;
  align-items: start;
}

.content-main,
.content-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.content-side {
  gap: 16px;
}

.side-card {
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #e8eef7;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
}

::v-deep(.side-card .el-card__header) {
  padding: 16px 18px 14px;
  background: var(--it-surface);
  border-bottom: 1px solid #edf2f8;
}

::v-deep(.side-card .el-card__body) {
  padding: 18px;
}

.section-header {
  font-weight: 600;
  color: #303133;
}

.section-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.readme-section-card {
  overflow: hidden;
}

.readme-section-header {
  gap: 16px;
  align-items: flex-start;
}

.readme-section-title {
  min-width: 0;
  flex: 1;
}

.readme-title-main {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
}

.readme-title-sub {
  margin-top: 8px;
  color: #7a869a;
  font-size: 13px;
  line-height: 1.7;
  max-width: 760px;
}

.readme-header-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.readme-stats-inline {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.readme-stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--it-surface-muted);
  border: 1px solid #e8eef7;
  color: #5b6b82;
  font-size: 12px;
  white-space: nowrap;
}

.readme-stat-chip i {
  color: #409eff;
}

.readme-stat-chip em {
  font-style: normal;
  font-weight: 700;
  color: #25364d;
}

.readme-showcase {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.readme-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  padding: 22px 24px;
  border-radius: 18px;
  background: var(--it-surface);
  border: 1px solid #e4eefb;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.readme-eyebrow {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.12);
  color: #3a7bd5;
  font-size: 12px;
  font-weight: 600;
}

.readme-hero-title {
  margin-top: 12px;
  font-size: 24px;
  line-height: 1.35;
  font-weight: 700;
  color: #20324a;
}

.readme-hero-desc {
  margin-top: 10px;
  color: #5e6f86;
  line-height: 1.85;
  font-size: 14px;
  max-width: 760px;
}

.readme-hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(112px, 1fr));
  gap: 12px;
  min-width: 248px;
}

.readme-hero-stat {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(219, 231, 245, 0.95);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.05);
}

.readme-hero-stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #20324a;
  line-height: 1.2;
}

.readme-hero-stat-label {
  margin-top: 6px;
  color: #7a869a;
  font-size: 12px;
}

.readme-shell {
  position: relative;
  padding: 24px 26px;
  border-radius: 18px;
  background: var(--it-surface);
  border: 1px solid #e8eef7;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.05);
}

.readme-showcase.is-empty .readme-shell {
  min-height: 180px;
  display: flex;
  align-items: center;
}

.readme-box {
  min-height: 140px;
  line-height: 1.9;
  color: #4b5d73;
  white-space: normal;
  word-break: break-word;
  font-size: 15px;
}

.file-browser {
  display: flex;
  gap: 14px;
  align-items: stretch;
  min-height: 720px;
  height: min(82vh, 920px);
}

.workspace-flow-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid #dbe7f7;
  background: var(--it-surface);
}

.workspace-flow-copy {
  min-width: 0;
}

.workspace-flow-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.workspace-flow-desc {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.8;
  color: #64748b;
}

.workspace-flow-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.file-tree-panel,
.file-preview-panel {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: var(--it-surface);
}

.file-tree-panel {
  display: flex;
  flex-direction: column;
  flex: 0 0 auto;
  min-width: 280px;
  max-width: 640px;
  padding: 14px 14px 12px;
  border-radius: 16px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
  overflow: hidden;
}

.file-browser-splitter {
  flex: 0 0 10px;
  position: relative;
  cursor: col-resize;
  border-radius: 999px;
  background: var(--it-surface-muted);
}

.file-browser-splitter::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 4px;
  height: 52px;
  transform: translate(-50%, -50%);
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.2);
}

.file-browser-splitter:hover::before {
  background: var(--it-accent);
}

::v-deep(.file-tree-panel .el-tree) {
  background: transparent;
  width: max-content;
  min-width: 100%;
}

::v-deep(.file-tree-panel .el-tree-node__content) {
  min-height: 38px;
  padding: 0 8px 0 4px;
  border-radius: 10px;
  margin: 4px 0;
  transition: background-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

::v-deep(.file-tree-panel .el-tree-node__content:hover) {
  background: var(--it-accent-soft);
}

::v-deep(.file-tree-panel .el-tree-node.is-current > .el-tree-node__content) {
  background: var(--it-accent-soft);
  box-shadow: inset 0 0 0 1px rgba(64, 158, 255, 0.16);
}

::v-deep(.file-tree-panel .el-tree-node__expand-icon) {
  color: #7b8ba7;
  padding: 6px;
}

::v-deep(.file-tree-panel .el-tree-node__children) {
  padding-left: 10px;
}

.tree-wrap {
  margin-top: 14px;
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 6px;
  padding-bottom: 6px;
}

.tree-wrap::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.tree-wrap::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.3);
  border-radius: 999px;
}

.tree-wrap::-webkit-scrollbar-track {
  background: rgba(241, 245, 249, 0.1);
  border-radius: 999px;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: max-content;
  min-width: 100%;
  padding-right: 10px;
}

.tree-node i {
  font-size: 14px;
  color: #6d7d96;
}

.tree-node-name {
  white-space: nowrap;
  font-size: 13px;
  color: #334155;
}

.main-file-badge {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  color: #2563eb;
  background: var(--it-accent-soft);
  border: 1px solid rgba(37, 99, 235, 0.14);
}

.file-preview-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  border-radius: 16px;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.06);
  overflow: hidden;
}

.file-preview-toolbar {
  padding: 14px 16px;
  border-bottom: 1px solid #eef3f9;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  background: var(--it-surface);
}

.file-preview-title-group {
  min-width: 0;
  flex: 1;
}

.file-preview-title {
  font-weight: 700;
  font-size: 30px;
  line-height: 1.35;
  color: #1f2937;
  word-break: break-all;
}

.file-preview-subtitle {
  margin-top: 8px;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.7;
  word-break: break-all;
}

.file-preview-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  width: auto;
  min-width: 0;
}

.file-preview-meta {
  padding: 12px 16px 0;
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.preview-meta-left,
.preview-meta-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.preview-meta-right {
  justify-content: flex-end;
  flex: 1;
}

.preview-view-tools {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-right: 6px;
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--it-surface-muted);
  border: 1px solid #e8eef7;
  color: #607089;
  font-size: 12px;
  line-height: 1;
}

.meta-pill-lang {
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  color: #fff;
  border: none;
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.18);
}

.rich-preview-shell,
.text-preview-shell,
.media-preview-shell,
.table-preview-shell,
.office-preview-shell {
  margin: 12px 14px 14px;
  flex: 1;
  min-height: 0;
  max-height: calc(100% - 142px);
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #e8edf5;
  background: var(--it-surface);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.rich-preview-body {
  height: 100%;
  overflow: auto;
  padding: 22px 24px 24px;
  background: var(--it-surface);
  font-size: 15px;
  line-height: 1.9;
}

.text-preview-shell {
  background: var(--it-surface-muted);
}

.plain-text-preview {
  margin: 0;
  padding: 22px 24px 24px;
  height: 100%;
  overflow: auto;
  white-space: pre;
  word-break: normal;
  line-height: 1.8;
  color: #334155;
  font-size: var(--preview-font-size, 14px);
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', 'Courier New', monospace;
}

.plain-text-preview.is-wrap {
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.media-preview-shell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: var(--it-surface);
}

.image-preview-shell {
  overflow: auto;
}

.image-preview-element {
  display: block;
  max-width: 100%;
  max-height: 560px;
  object-fit: contain;
  border-radius: 12px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.14);
  background: var(--it-surface);
}

.pdf-preview-shell {
  padding: 0;
  background: var(--it-surface-muted);
}

.pdf-preview-frame {
  width: 100%;
  min-height: 600px;
  height: 100%;
  background: var(--it-surface);
}

.video-preview-element {
  width: 100%;
  max-height: 560px;
  border-radius: 12px;
  background: var(--it-text);
}

.audio-preview-shell {
  gap: 14px;
}

.audio-preview-element {
  width: min(620px, 100%);
}

.media-preview-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
}

.media-preview-tip {
  font-size: 13px;
  color: #7b8798;
}

.table-preview-shell,
.spreadsheet-preview-shell,
.office-preview-shell {
  display: flex;
  flex-direction: column;
}

.table-preview-wrap {
  overflow: auto;
}

.preview-table {
  width: 100%;
  min-width: 520px;
  border-collapse: collapse;
}

.preview-table th,
.preview-table td {
  border: 1px solid #e8edf5;
  padding: 10px 12px;
  text-align: left;
  vertical-align: top;
  font-size: 13px;
  color: #334155;
}

.preview-table th {
  background: var(--it-surface-muted);
  font-weight: 700;
  color: #1f2d3d;
}

.table-preview-shell .table-preview-wrap,
.spreadsheet-preview-shell .table-preview-wrap {
  padding: 16px;
}

.office-preview-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 18px;
  border-bottom: 1px solid #edf1f7;
  background: var(--it-surface);
}

.office-preview-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.office-preview-subtitle {
  font-size: 13px;
  color: #6b7280;
}

.office-document-body {
  padding: 20px 22px;
  overflow: auto;
  line-height: 1.95;
  color: #374151;
}

.office-document-body p {
  margin: 0 0 14px;
  text-indent: 2em;
}

.sheet-preview-list,
.ppt-slide-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  overflow: auto;
}

.sheet-preview-card,
.ppt-slide-card {
  border: 1px solid #e8edf5;
  border-radius: 12px;
  background: var(--it-surface);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.sheet-preview-name,
.ppt-slide-title {
  padding: 12px 14px;
  border-bottom: 1px solid #edf1f7;
  font-weight: 700;
  color: #1f2d3d;
  background: var(--it-surface-muted);
}

.ppt-slide-lines {
  padding: 14px 16px 16px;
}

.ppt-slide-lines p {
  margin: 0 0 10px;
  line-height: 1.8;
  color: #475569;
}

.unsupported-preview {
  flex-direction: column;
  gap: 10px;
  text-align: center;
  padding: 24px;
}

.unsupported-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
}

.unsupported-desc {
  max-width: 520px;
  line-height: 1.8;
  color: #6b7280;
}

.preview-loading-state {
  flex-direction: column;
  gap: 10px;
  font-size: 14px;
}

.preview-loading-state i {
  font-size: 24px;
  color: #409eff;
}

.code-preview-shell {
  margin: 12px 16px 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  max-height: calc(100% - 142px);
  height: auto;
  border-radius: 18px;
  overflow: hidden;
  background: radial-gradient(circle at top left, rgba(56, 189, 248, 0.08), transparent 28%), linear-gradient(180deg, #081120 0%, #0a1526 52%, #07101d 100%);
  box-shadow: 0 20px 48px rgba(2, 6, 23, 0.28);
  border: 1px solid rgba(96, 165, 250, 0.14);
}

.code-preview-header {
  min-height: 50px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.04) 0%, rgba(255, 255, 255, 0.02) 100%);
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

.code-header-left,
.code-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.code-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.code-dot-red {
  background: #ff5f57;
}

.code-dot-yellow {
  background: #febc2e;
}

.code-dot-green {
  background: #28c840;
}

.code-file-name {
  margin-left: 6px;
  color: #dbe7ff;
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-extension-chip {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.16);
  border: 1px solid rgba(96, 165, 250, 0.28);
  color: #bfdbfe;
  font-size: 12px;
}

.code-container {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: linear-gradient(90deg, rgba(15, 23, 42, 0.96) 0 64px, rgba(8, 17, 32, 0.96) 64px);
}

.line-numbers {
  width: 64px;
  background: rgba(2, 6, 23, 0.78);
  border-right: 1px solid rgba(148, 163, 184, 0.14);
  text-align: right;
  user-select: none;
  padding: 16px 0 24px;
  overflow: auto;
  scrollbar-width: none;
}

.line-numbers::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.line-number {
  min-height: 1.75em;
  padding: 0 14px 0 8px;
  font-size: calc(var(--preview-font-size, 14px) - 1px);
  color: #6f86a8;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', 'Courier New', monospace;
  line-height: 1.75;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.code-content {
  margin: 0;
  padding: 16px 20px 24px;
  white-space: pre;
  word-break: normal;
  overflow: auto;
  flex: 1;
  background: linear-gradient(180deg, rgba(7, 16, 29, 0.72) 0%, rgba(8, 17, 32, 0.94) 100%);
  color: #e5eefc;
  font-size: var(--preview-font-size, 14px);
  line-height: 1.75;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', 'Courier New', monospace;
  tab-size: 2;
}

.code-content.is-wrap {
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.code-content::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.code-content::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.28);
  border-radius: 999px;
}

.code-content::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.02);
}

.code-content pre {
  margin: 0;
  padding: 0;
}

.code-content code {
  display: block;
  min-width: 100%;
  font-size: inherit;
  line-height: inherit;
  background: transparent !important;
  color: inherit;
  padding: 0;
}

.code-content code.hljs {
  background: transparent !important;
}

.empty-preview {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.version-box {
  margin-top: 16px;
}

.sub-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.version-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.version-name {
  font-weight: 600;
  color: #303133;
}

.version-desc {
  color: #606266;
  font-size: 13px;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.info-label {
  color: #909399;
}

.info-value {
  color: #303133;
  text-align: right;
  word-break: break-word;
}


.side-task-header-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.side-task-card-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.side-task-summary {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--it-surface-muted);
  border: 1px solid #ebeef5;
}

.side-task-summary.danger {
  background: var(--it-danger-soft);
  border-color: #fde2e2;
}

.side-task-summary-value {
  font-size: 22px;
  line-height: 1;
  font-weight: 700;
  color: #303133;
}

.side-task-summary-label {
  font-size: 12px;
  color: #909399;
}

.side-task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.side-task-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  background: var(--it-surface);
}

.side-task-item.is-overdue,
.side-task-item-due {
  border-color: #f7c7c7;
  background: var(--it-danger-soft);
}

.side-task-main {
  flex: 1;
  min-width: 0;
}

.side-task-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  line-height: 1.5;
  word-break: break-word;
}

.side-task-meta {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #909399;
}

.side-task-status-select {
  width: 104px;
  flex-shrink: 0;
}

.side-task-status-select .el-input__inner,
.task-compact-side .el-input__inner {
  border-radius: 10px;
  border-color: #d8e3f2;
  background: #fff;
  color: #1f2937;
  font-weight: 600;
}

.contributors-list,
.related-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.contributor-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.contributor-name {
  font-weight: 600;
  color: #303133;
}

.contributor-role {
  color: #909399;
  font-size: 12px;
  margin-top: 2px;
}

.related-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  cursor: pointer;
  transition: all .2s;
}

.related-item:hover {
  border-color: #409eff;
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.12);
}

.related-title {
  font-weight: 600;
  color: #303133;
}

.related-desc {
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.related-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.ai-assistant-box {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-model-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-model-label {
  font-size: 13px;
  color: #606266;
  font-weight: 600;
}

.ai-model-tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ai-helper-text {
  font-size: 12px;
  line-height: 1.7;
  color: #909399;
}

.ai-helper-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ai-result-card {
  overflow: hidden;
}

.ai-result-header-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.ai-result-tabs {
  margin-top: -8px;
}

.ai-result-box {
  min-height: 180px;
  padding: 14px 16px;
  border-radius: 10px;
  background: var(--it-surface-muted);
  border: 1px solid #ebeef5;
  line-height: 1.9;
  color: #303133;
  white-space: normal;
  word-break: break-word;
}

.ai-rich-content p,
.ai-rich-content ul,
.ai-rich-content ol,
.ai-rich-content blockquote,
.ai-rich-content pre,
.ai-rich-content .markdown-table-wrap,
.ai-rich-content .markdown-code-block,
.ai-rich-content .markdown-image-only {
  margin: 0 0 16px;
}

.ai-rich-content p:last-child,
.ai-rich-content ul:last-child,
.ai-rich-content ol:last-child,
.ai-rich-content blockquote:last-child,
.ai-rich-content pre:last-child,
.ai-rich-content .markdown-table-wrap:last-child,
.ai-rich-content .markdown-code-block:last-child,
.ai-rich-content .markdown-image-only:last-child {
  margin-bottom: 0;
}

.ai-rich-content h1,
.ai-rich-content h2,
.ai-rich-content h3,
.ai-rich-content h4,
.ai-rich-content h5,
.ai-rich-content h6 {
  margin: 24px 0 12px;
  line-height: 1.45;
  color: #1f2d3d;
  font-weight: 700;
}

.ai-rich-content h1:first-child,
.ai-rich-content h2:first-child,
.ai-rich-content h3:first-child,
.ai-rich-content h4:first-child,
.ai-rich-content h5:first-child,
.ai-rich-content h6:first-child {
  margin-top: 0;
}

.ai-rich-content h1 { font-size: 30px; }
.ai-rich-content h2 { font-size: 26px; }
.ai-rich-content h3 { font-size: 22px; }
.ai-rich-content h4 { font-size: 19px; }
.ai-rich-content h5 { font-size: 17px; }
.ai-rich-content h6 { font-size: 15px; }

.ai-rich-content ul,
.ai-rich-content ol {
  padding-left: 24px;
}

.ai-rich-content li + li {
  margin-top: 8px;
}

.ai-rich-content hr {
  border: none;
  border-top: 1px solid #e8eef7;
  margin: 22px 0;
}

.ai-rich-content blockquote {
  padding: 14px 16px;
  border-left: 4px solid #8bbdff;
  background: var(--it-surface);
  color: #5d6f88;
  border-radius: 12px;
}

.ai-rich-content a {
  color: #1d6fdc;
  text-decoration: none;
  font-weight: 600;
  border-bottom: 1px solid rgba(29, 111, 220, 0.2);
}

.ai-rich-content a:hover {
  color: #409eff;
  border-bottom-color: rgba(64, 158, 255, 0.38);
}

.ai-rich-content code {
  padding: 2px 7px;
  border-radius: 6px;
  background: var(--it-surface-muted);
  font-size: 13px;
  color: #cc4b37;
  border: 1px solid #e6edf6;
}

.ai-rich-content del {
  color: #8b98ab;
}

.ai-rich-content .markdown-code-block {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(96, 165, 250, 0.12);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
}

.ai-rich-content .markdown-code-head {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 36px;
  padding: 0 14px;
  background: linear-gradient(180deg, #1f2a3d 0%, #162132 100%);
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

.ai-rich-content .markdown-code-lang {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--it-accent-soft);
  color: #dbeafe;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.ai-rich-content pre {
  margin: 0;
  padding: 16px 18px;
  border-radius: 0;
  background: linear-gradient(180deg, #101827 0%, #0b1320 100%);
  color: #f8fbff;
  overflow: auto;
}

.ai-rich-content pre code {
  padding: 0;
  background: transparent;
  color: inherit;
  border: none;
}

.ai-rich-content .markdown-image-only {
  display: flex;
  justify-content: center;
}

.ai-rich-content .markdown-image-wrap {
  display: inline-flex;
  max-width: 100%;
}

.ai-rich-content .markdown-image-wrap img {
  max-width: 100%;
  max-height: 520px;
  border-radius: 16px;
  border: 1px solid #e7edf5;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
  object-fit: contain;
  background: var(--it-surface);
}

.ai-rich-content .markdown-image-alt {
  color: #909399;
  font-size: 13px;
}

.ai-rich-content .markdown-task-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  list-style: none;
  margin-left: -18px;
}

.ai-rich-content .markdown-task-box {
  flex: 0 0 18px;
  width: 18px;
  height: 18px;
  margin-top: 3px;
  border-radius: 6px;
  border: 1px solid #c8d7ea;
  background: var(--it-surface);
  color: transparent;
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.ai-rich-content .markdown-task-item.is-checked .markdown-task-box {
  background: var(--it-accent);
  border-color: #409eff;
  color: #fff;
}

.ai-rich-content .markdown-table-wrap {
  width: 100%;
  overflow-x: auto;
  border: 1px solid #e8eef7;
  border-radius: 14px;
  background: var(--it-surface);
}

.ai-rich-content table {
  width: 100%;
  border-collapse: collapse;
  min-width: 520px;
}

.ai-rich-content th,
.ai-rich-content td {
  border: 1px solid #edf2f8;
  padding: 12px 14px;
  text-align: left;
  vertical-align: top;
}

.ai-rich-content th {
  background: var(--it-surface-muted);
  font-weight: 700;
  color: #20324a;
}

.ai-rich-content strong {
  font-weight: 700;
  color: #1f2d3d;
}

.ai-rich-content em {
  font-style: italic;
}


.task-collab-entry-btn,
.task-collab-entry-btn.el-button--text,
.side-task-link,
.side-task-link.el-button--text {
  margin-top: 0;
  min-height: 32px;
  min-width: 74px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  border: 1px solid #cfe0ff;
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
  color: #1d4ed8 !important;
  -webkit-text-fill-color: currentColor;
  font-weight: 700;
  font-size: 12px;
  line-height: 1.2;
  white-space: nowrap;
  text-align: center;
  box-shadow: 0 6px 14px rgba(59, 130, 246, 0.1);
}

.side-task-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
  min-width: 104px;
}

.task-collab-entry-btn.el-button--text:hover,
.task-collab-entry-btn.el-button--text:focus,
.side-task-link.el-button--text:hover,
.side-task-link.el-button--text:focus {
  color: #1e40af !important;
  border-color: #b8d2ff;
  background: linear-gradient(180deg, #f1f7ff 0%, #e4efff 100%);
}

.task-collab-drawer-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.task-collab-drawer-heading {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #8a94a6;
  font-weight: 700;
}

.task-collab-drawer-subtitle {
  font-size: 18px;
  line-height: 1.4;
  color: #1f2937;
  font-weight: 700;
  max-width: 720px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

::v-deep(.task-collab-drawer .el-drawer__header) {
  margin-bottom: 0;
  padding: 18px 22px 16px;
  border-bottom: 1px solid #ebf1f7;
  background: var(--it-surface);
}

::v-deep(.task-collab-drawer .el-drawer__body) {
  padding: 0;
  background: var(--it-surface-muted);
  overflow: auto;
}

.task-collab-drawer-shell {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-collab-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 16px;
  padding: 22px;
  border-radius: 20px;
  background: var(--it-surface);
  border: 1px solid #dfeaf8;
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.06);
}

.task-collab-eyebrow {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--it-accent-soft);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.task-collab-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}

.task-collab-title-text {
  font-size: 24px;
  line-height: 1.4;
  font-weight: 700;
  color: #1e293b;
}

.task-collab-title-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.task-collab-desc {
  margin-top: 12px;
  font-size: 14px;
  line-height: 1.85;
  color: #607089;
  white-space: pre-wrap;
}

.task-collab-meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.task-collab-meta-card {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(222, 232, 245, 0.95);
}

.task-collab-meta-label {
  font-size: 12px;
  color: #7b8ba7;
}

.task-collab-meta-value {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.6;
  color: #223248;
  font-weight: 600;
  word-break: break-word;
}

.task-collab-side-box {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(222, 232, 245, 0.95);
}

.task-collab-side-title {
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
}

.task-collab-status-select {
  width: 100%;
}

.task-collab-shortcuts {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.task-collab-tabs-card {
  border-radius: 20px;
  background: var(--it-surface);
  border: 1px solid #e8eef7;
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.05);
  overflow: hidden;
}

::v-deep(.task-collab-tabs .el-tabs__header) {
  margin: 0;
  padding: 0 18px;
  border-bottom: 1px solid #edf2f8;
  background: var(--it-surface);
}

::v-deep(.task-collab-tabs .el-tabs__nav-wrap::after) {
  display: none;
}

::v-deep(.task-collab-tabs .el-tabs__item) {
  height: 54px;
  line-height: 54px;
  font-weight: 600;
}

::v-deep(.task-collab-tabs .el-tabs__content) {
  padding: 18px;
  background: var(--it-surface);
}

.task-collab-drawer-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 320px;
}

@media (max-width: 1100px) {
  .task-board-summary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .task-board-grid {
    grid-template-columns: 1fr;
  }

  .readme-section-header,
  .readme-header-right {
    flex-direction: column;
    align-items: flex-start;
  }

  .readme-stats-inline {
    justify-content: flex-start;
  }

  .readme-hero {
    grid-template-columns: 1fr;
  }

  .readme-hero-stats {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    min-width: 0;
  }

  .content-layout {
    grid-template-columns: 1fr;
  }

  .file-browser {
    grid-template-columns: 1fr;
  }

  .content-side {
    gap: 14px;
  }
}


@media (max-width: 768px) {
  .workspace-flow-banner {
    flex-direction: column;
    align-items: flex-start;
  }

  .readme-shell {
    padding: 18px 16px;
  }

  .readme-hero {
    padding: 18px 16px;
  }

  .readme-title-main {
    flex-wrap: wrap;
  }

  .readme-hero-title {
    font-size: 20px;
  }

  .readme-hero-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-header,
  .title-row,
  .meta-row,
  .task-compact-item,
  .task-compact-top {
    flex-direction: column;
    align-items: stretch;
  }

  .task-board-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .task-compact-side .el-select,
  .task-compact-side .el-button {
    width: 100%;
  }
  
  .file-preview-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .file-preview-actions,
  .preview-meta-right {
    justify-content: flex-start;
  }

  .file-preview-title {
    font-size: 24px;
  }

  .stats-row {
    width: 100%;
    justify-content: space-between;
  }

  .ai-helper-actions {
    width: 100%;
  }

  .ai-result-header-actions {
    justify-content: space-between;
    width: 100%;
  }
}
.file-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tree-selection-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  margin-top: 10px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: var(--it-surface-muted);
  color: #606266;
  font-size: 13px;
}

.tree-selection-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.danger-text-btn {
  color: #f56c6c;
}


.tree-node-checkbox {
  margin-right: 8px;
}

.upload-picked-tip,
.upload-picked-list {
  margin-top: 10px;
  color: #606266;
  font-size: 13px;
}

.upload-workspace-alert {
  margin-bottom: 16px;
}

.upload-picked-title {
  margin-bottom: 6px;
}

.upload-picked-items {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.upload-picked-item {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--it-surface-muted);
  color: #606266;
  line-height: 1.4;
}



.task-board-card {
  margin-top: 18px;
}

.task-board-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.task-board-header-actions .el-button--text {
  min-height: 32px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid #d7e4f4;
  background: #f8fbff;
  color: #2563eb;
  font-weight: 700;
}

.task-board-header-actions .el-button--primary.is-plain {
  min-height: 34px;
  border-radius: 10px;
  border-color: #bfdbfe;
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
  color: #1d4ed8;
  font-weight: 700;
}

.task-board-summary {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.task-mini-stat {
  padding: 14px 16px;
  border-radius: 12px;
  background: var(--it-surface);
  border: 1px solid #e8eef7;
}

.task-mini-stat.danger {
  background: var(--it-danger-soft);
  border-color: #ffd8d6;
}

.task-mini-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.task-mini-stat-label {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.task-board-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.task-compact-panel {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: var(--it-surface);
  padding: 14px;
  min-height: 240px;
}

.task-compact-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
  font-weight: 600;
  color: #303133;
}

.task-compact-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.task-compact-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid #eef2f7;
  background: var(--it-surface);
}

.task-compact-item.is-overdue {
  border-color: #ffd9d4;
  background: var(--it-danger-soft);
}

.task-compact-main {
  min-width: 0;
  flex: 1;
}

.task-compact-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.task-compact-title {
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  line-height: 1.5;
  word-break: break-word;
}

.task-compact-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.task-compact-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
  color: #909399;
  font-size: 12px;
  flex-wrap: wrap;
}

.task-assignee-inline {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #606266;
}

.task-compact-side {
  flex-shrink: 0;
  min-width: 112px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.file-preview-title-group {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.file-preview-subtitle {
  font-size: 12px;
  color: #8a94a6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-warning-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 14px;
  padding: 10px 14px;
  border-radius: 12px;
  background: var(--it-warning-soft);
  border: 1px solid #ffd591;
  color: #ad6800;
  font-size: 13px;
}

.ai-struct-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.ai-hero-overview {
  padding: 16px 18px;
  border-radius: 14px;
  background: var(--it-accent-soft);
  color: #1e293b;
  font-size: 16px;
  line-height: 1.7;
  font-weight: 600;
}
.ai-struct-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}
.ai-struct-item,
.ai-phase-card {
  border: 1px solid #e5edf6;
  border-radius: 14px;
  background: #fff;
  padding: 14px 16px;
}
.ai-struct-title,
.ai-phase-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 10px;
}
.ai-struct-list {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.8;
}
.ai-ordered-list {
  padding-left: 20px;
}
.ai-struct-list-warn {
  color: #b45309;
}
.ai-task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ai-task-item {
  padding: 12px;
  border-radius: 12px;
  background: var(--it-surface-muted);
  border: 1px solid #e5edf6;
}
.ai-task-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.ai-task-name {
  font-weight: 600;
  color: #0f172a;
}
.ai-task-meta {
  color: #475569;
  line-height: 1.7;
  font-size: 13px;
}



@media (max-width: 1100px) {
  .task-collab-hero {
    grid-template-columns: 1fr;
  }

  .task-collab-meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .detail-header {
    align-items: stretch;
  }

  .breadcrumb-wrap,
  .header-actions {
    width: 100%;
  }

  .header-actions {
    justify-content: flex-start;
  }

  .task-collab-drawer-shell {
    padding: 12px;
  }

  .task-collab-title-row {
    flex-direction: column;
  }

  .task-collab-title-text {
    font-size: 20px;
  }

  .task-collab-meta-grid {
    grid-template-columns: 1fr;
  }

  .side-task-actions {
    align-items: stretch;
    width: 100%;
  }

  .side-task-status-select,
  .task-collab-status-select {
    width: 100%;
  }
}

@media (max-width: 1280px) {
  .file-browser {
    height: auto;
    min-height: 0;
    flex-direction: column;
  }

  .file-tree-panel {
    width: 100% !important;
    max-width: none;
  }

  .file-browser-splitter {
    display: none;
  }

  .file-preview-panel {
    min-height: 720px;
  }
}

</style>
