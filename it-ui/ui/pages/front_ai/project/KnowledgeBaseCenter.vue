<template>
  <div class="kb-front-page">
    <el-card shadow="never" class="kb-card">
      <div class="kb-layout">
        <KnowledgeBaseListPanel
          :list-mode.sync="listMode"
          :owner-id.sync="ownerId"
          :project-id.sync="projectId"
          :keyword.sync="keyword"
          :loading="loading.kbList"
          :knowledge-bases="filteredKnowledgeBases"
          :current-knowledge-base="currentKnowledgeBase"
          :pagination="pagination"
          :can-create-knowledge-base="canCreateKnowledgeBase"
          :can-edit-knowledge-base-item="canEditKnowledgeBaseItem"
          :kb-status-tag-type="kbStatusTagType"
          @mode-change="handleListModeChange"
          @refresh="loadKnowledgeBases"
          @create="openKbDialog('create')"
          @select="selectKnowledgeBase"
          @page-change="handleKbPageChange"
        />

        <div class="kb-main">
          <div v-if="!currentKnowledgeBase" class="kb-empty-state">
            <el-empty description="请先创建或选择当前项目知识库" :image-size="96" />
            <el-alert
              v-if="showProjectCreateGuide"
              class="kb-project-guide"
              type="info"
              :closable="false"
              show-icon
              title="当前项目还没有知识库"
              description="创建项目知识库后，可直接上传文件、ZIP 导入项目资料，并进行知识库问答。"
            />
            <div v-if="showProjectCreateGuide" class="kb-project-guide__actions">
              <el-button type="primary" size="small" :disabled="!canCreateKnowledgeBase" @click="openKbDialog('create')">
                创建项目知识库
              </el-button>
            </div>
          </div>

          <template v-else>
            <div class="kb-main__header">
              <div>
                <div class="kb-main__title">{{ currentKnowledgeBase.name || `知识库 #${currentKnowledgeBase.id}` }}</div>
                <div class="kb-main__subtitle">
                  {{ currentKnowledgeBase.description || 'This project knowledge base has no description yet.' }}
                </div>
              </div>
              <el-tag size="small" type="info" effect="plain">项目 #{{ routeProjectId || currentKnowledgeBase.projectId || '-' }}</el-tag>
            </div>

            <el-descriptions :column="2" border size="small" class="kb-descriptions">
              <el-descriptions-item label="知识库 ID">{{ currentKnowledgeBase.id }}</el-descriptions-item>
              <el-descriptions-item label="项目 ID">{{ currentKnowledgeBase.projectId || routeProjectId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="作用域">{{ currentKnowledgeBase.scopeType || '-' }}</el-descriptions-item>
              <el-descriptions-item label="可见性">{{ currentKnowledgeBase.visibility || '-' }}</el-descriptions-item>
              <el-descriptions-item label="来源类型">{{ currentKnowledgeBase.sourceType || '-' }}</el-descriptions-item>
              <el-descriptions-item label="默认问答模型">{{ currentKnowledgeBaseDefaultModelName }}</el-descriptions-item>
              <el-descriptions-item label="最后索引时间">{{ formatTime(currentKnowledgeBase.lastIndexedAt) }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ currentKnowledgeBase.status || '-' }}</el-descriptions-item>
            </el-descriptions>

            <el-tabs v-model="activeTab" class="kb-tabs">
              <el-tab-pane label="文档列表" name="documents">
                <div class="kb-front-doc-tab">
                  <KnowledgeBaseDocumentTab
                    :loading="loading.documents"
                    :documents="documents"
                    :pagination="documentPagination"
                    :can-edit="canEditCurrentKnowledgeBase"
                    :format-time="formatTime"
                    :doc-status-tag-type="docStatusTagType"
                    :document-embedding-label="documentEmbeddingLabel"
                    :document-embedding-tag-type="documentEmbeddingTagType"
                    @open-document-dialog="openDocumentDialog"
                    @refresh="loadDocuments"
                    @open-tasks="openKnowledgeBaseTasks"
                    @download-zip="downloadCurrentDocumentsZip"
                    @upload-files="uploadSelectedDocuments"
                    @upload-zip="uploadZipFile"
                    @import-local-file="importLocalTextFile"
                    @view-chunks="viewChunks"
                    @preview-chunks="previewChunks"
                    @backfill-document="backfillDocumentVector"
                    @reindex-document="reindexDocument"
                    @view-tasks="viewIndexTasks"
                    @download-document="downloadDocument"
                    @seed-chat="seedChatQuestion"
                    @page-change="handleDocumentPageChange"
                  />
                </div>
              </el-tab-pane>

              <el-tab-pane label="知识库问答" name="chat">
                <div class="kb-front-chat-tab">
                  <KnowledgeBaseChatTab
                    ref="chatTabRef"
                    :chat-form="chatForm"
                    :enabled-models="enabledModels"
                    :loading-models="loading.models"
                    :session-loading="sessionLoading"
                    :sessions="sessions"
                    :filtered-sessions="filteredSessions"
                    :session-keyword.sync="sessionKeyword"
                    :session-pagination="sessionPagination"
                    :selected-session-id="selectedSessionId"
                    :chat-messages="chatMessages"
                    :current-knowledge-base="currentKnowledgeBase"
                    :current-knowledge-base-default-model-name="currentKnowledgeBaseDefaultModelName"
                    :effective-chat-model-name="effectiveChatModelName"
                    :loading-chat="loading.chat"
                    :loading-stream-chat="loading.streamChat"
                    :loading-debug-search="loading.debugSearch"
                    :format-time="formatTime"
                    :build-model-label="buildModelLabel"
                    @update-chat-field="updateChatField"
                    @use-kb-default-model="useKnowledgeBaseDefaultModel"
                    @use-active-model="useActiveModel"
                    @refresh-models="refreshModels"
                    @new-session="createNewSession"
                    @refresh-sessions="loadSessions"
                    @session-page-change="handleSessionPageChange"
                    @select-session="selectSession"
                    @archive-session="archiveSession"
                    @remove-session="removeSession"
                    @toggle-message-sources="toggleMessageSources"
                    @locate-source-document="locateSourceDocument"
                    @clear-chat="clearChat"
                    @send-chat="sendChat"
                    @stop-stream="stopStream"
                  />
                </div>
              </el-tab-pane>
            </el-tabs>
          </template>
        </div>
      </div>
    </el-card>

    <KnowledgeBaseBaseFormDialog
      :visible.sync="kbDialogVisible"
      :mode="kbDialogMode"
      :form="kbForm"
      :rules="kbRules"
      :enabled-models="enabledModels"
      :embedding-provider-options="embeddingProviderOptions"
      :build-model-label="buildModelLabel"
      :saving="loading.saveKb"
      :disabled="kbDialogMode === 'edit' ? !canEditCurrentKnowledgeBase : !canCreateKnowledgeBase"
      @save="handleKbFormSave"
    />

    <KnowledgeBaseDocumentImportDialog
      :visible.sync="documentDialogVisible"
      :form="documentForm"
      :import-mode.sync="documentImportMode"
      :rules="documentRules"
      :selected-local-file-name="selectedLocalFileName"
      :file-read-error="fileReadError"
      :saving="loading.saveDocument"
      :disabled="!canEditCurrentKnowledgeBase"
      @save="handleDocumentFormSave"
    />

    <KnowledgeBaseChunkPreviewDialog
      :visible.sync="chunkDialogVisible"
      :loading="loading.chunks"
      :document="activeChunkDocument"
      :chunks="chunks"
    />
  </div>
</template>

<script>
import KnowledgeBaseListPanel from '@/components/ai/kb/panels/KnowledgeBaseListPanel.vue'
import KnowledgeBaseDocumentTab from '@/components/ai/kb/panels/KnowledgeBaseDocumentTab.vue'
import KnowledgeBaseChatTab from '@/components/ai/kb/panels/KnowledgeBaseChatTab.vue'
import KnowledgeBaseBaseFormDialog from '@/components/ai/kb/dialogs/KnowledgeBaseBaseFormDialog.vue'
import KnowledgeBaseDocumentImportDialog from '@/components/ai/kb/dialogs/KnowledgeBaseDocumentImportDialog.vue'
import KnowledgeBaseChunkPreviewDialog from '@/components/ai/kb/dialogs/KnowledgeBaseChunkPreviewDialog.vue'
import useFrontProjectKnowledgePage from '@/pages/front_ai/composables/useFrontProjectKnowledgePage'

export default {
  name: 'FrontProjectKnowledgeBaseCenter',
  layout: 'manage',
  components: {
    KnowledgeBaseListPanel,
    KnowledgeBaseDocumentTab,
    KnowledgeBaseChatTab,
    KnowledgeBaseBaseFormDialog,
    KnowledgeBaseDocumentImportDialog,
    KnowledgeBaseChunkPreviewDialog
  },
  mixins: [useFrontProjectKnowledgePage],
  methods: {
    handleKbFormSave(form) {
      const fixedProjectId = this.routeProjectId || this.projectId || null
      this.kbForm = {
        ...form,
        scopeType: 'PROJECT',
        sourceType: 'PROJECT_DOC',
        projectId: fixedProjectId
      }
      this.submitKbForm()
    },

    handleDocumentFormSave(form) {
      this.documentForm = { ...form, sourceType: 'PROJECT_DOC' }
      this.submitDocumentForm()
    },

    updateChatField(field, value) {
      this.chatForm = {
        ...this.chatForm,
        [field]: value
      }
    }
  }
}
</script>

<style scoped>
.kb-front-page {
  padding: 16px;
}

.kb-card {
  border-radius: 12px;
}

.kb-layout {
  display: flex;
  gap: 16px;
  min-height: 720px;
}

.kb-main {
  flex: 1;
  min-width: 0;
  border: 1px solid var(--it-border);
  border-radius: 18px;
  padding: 18px;
  background: var(--it-panel-bg-strong);
  box-shadow: var(--it-shadow-soft);
}

.kb-empty-state {
  min-height: 320px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.kb-project-guide {
  margin: 8px 0 0;
}

.kb-project-guide__actions {
  margin-top: 12px;
}

.kb-main__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.kb-main__title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--it-text);
}

.kb-main__subtitle {
  color: var(--it-text-subtle);
  font-size: 12px;
}

.kb-descriptions {
  margin-bottom: 16px;
}

.kb-tabs {
  min-height: 540px;
}

.kb-front-page ::v-deep .kb-sidebar .page-toolbar .el-radio-group,
.kb-front-page ::v-deep .kb-sidebar .page-toolbar .el-input-number {
  display: none;
}

.kb-front-doc-tab ::v-deep .tab-toolbar__left > .el-button:nth-of-type(1),
.kb-front-doc-tab ::v-deep .tab-toolbar__left > .el-button:nth-of-type(4),
.kb-front-doc-tab ::v-deep .tab-toolbar__left > .el-button:nth-of-type(6) {
  display: none;
}

.kb-front-doc-tab ::v-deep .el-table .cell > .el-button--text:nth-of-type(3),
.kb-front-doc-tab ::v-deep .el-table .cell > .el-button--text:nth-of-type(4),
.kb-front-doc-tab ::v-deep .el-table .cell > .el-button--text:nth-of-type(5) {
  display: none;
}

.kb-front-chat-tab ::v-deep .chat-actions > .el-button:nth-of-type(2),
.kb-front-chat-tab ::v-deep .chat-message__footer .el-button--text,
.kb-front-chat-tab ::v-deep .source-card__actions .el-button--text:nth-of-type(2) {
  display: none;
}
</style>
