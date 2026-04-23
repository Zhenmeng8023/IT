<template>
  <div class="kb-front-page" :class="{ 'kb-front-page--embedded': embedded }">
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
          :can-create-knowledge-base="false"
          :can-edit-knowledge-base-item="canEditKnowledgeBaseItem"
          :kb-status-tag-type="kbStatusTagType"
          :show-list-mode-switch="false"
          :show-owner-id-input="false"
          :show-project-id-input="false"
          @mode-change="handleListModeChange"
          @refresh="loadKnowledgeBases"
          @select="selectKnowledgeBase"
          @page-change="handleKbPageChange"
        />

        <div class="kb-main">
          <div v-if="!currentKnowledgeBase" class="kb-empty-state">
            <el-empty description="请选择左侧我的知识库" :image-size="96" />
          </div>

          <template v-else>
            <div class="kb-main__header">
              <div>
                <div class="kb-main__title">{{ currentKnowledgeBase.name || `知识库 #${currentKnowledgeBase.id}` }}</div>
                <div class="kb-main__subtitle">
                  {{ currentKnowledgeBase.description || '这个知识库还没有描述。' }}
                </div>
              </div>
            </div>

            <el-descriptions :column="2" border size="small" class="kb-descriptions">
              <el-descriptions-item label="知识库 ID">{{ currentKnowledgeBase.id }}</el-descriptions-item>
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
                    :show-create-document-button="false"
                    :show-import-local-file-button="false"
                    :show-open-tasks-button="false"
                    :show-document-backfill-action="false"
                    :show-document-reindex-action="false"
                    :show-document-task-action="false"
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

              <el-tab-pane label="成员管理" name="members">
                <KnowledgeBaseMemberTab
                  :loading="loading.members"
                  :saving="loading.saveMember"
                  :members="members"
                  :can-manage="canManageCurrentMembers"
                  :format-time="formatTime"
                  @refresh="loadMembers"
                  @remove-member="removeMember"
                  @add-member="handleMemberSubmit"
                />
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
                    :show-debug-search-action="false"
                    :show-retrieval-actions="false"
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
import KnowledgeBaseMemberTab from '@/components/ai/kb/panels/KnowledgeBaseMemberTab.vue'
import KnowledgeBaseChatTab from '@/components/ai/kb/panels/KnowledgeBaseChatTab.vue'
import KnowledgeBaseDocumentImportDialog from '@/components/ai/kb/dialogs/KnowledgeBaseDocumentImportDialog.vue'
import KnowledgeBaseChunkPreviewDialog from '@/components/ai/kb/dialogs/KnowledgeBaseChunkPreviewDialog.vue'
import useFrontPersonalKnowledgePage from '@/pages/front_ai/composables/useFrontPersonalKnowledgePage'

export default {
  name: 'FrontPersonalKnowledgeBaseCenter',
  layout: 'default',
  props: {
    embedded: {
      type: Boolean,
      default: false
    }
  },
  components: {
    KnowledgeBaseListPanel,
    KnowledgeBaseDocumentTab,
    KnowledgeBaseMemberTab,
    KnowledgeBaseChatTab,
    KnowledgeBaseDocumentImportDialog,
    KnowledgeBaseChunkPreviewDialog
  },
  mixins: [useFrontPersonalKnowledgePage],
  methods: {
    handleDocumentFormSave(form) {
      this.documentForm = { ...form }
      this.submitDocumentForm()
    },

    async handleMemberSubmit(form, done) {
      this.memberForm = { ...form }
      const success = await this.submitMemberForm()
      if (success && typeof done === 'function') done()
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

.kb-main__header {
  display: flex;
  align-items: center;
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

.kb-front-page--embedded {
  padding: 0;
}

.kb-front-page--embedded .kb-card {
  border: 0;
  background: transparent;
  box-shadow: none;
}

.kb-front-page--embedded :deep(.el-card__body) {
  padding: 0;
}

.kb-front-page--embedded .kb-layout {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  gap: 12px;
  min-height: 0;
  align-items: start;
}

.kb-front-page--embedded .kb-main {
  padding: 14px;
  border-radius: 12px;
  box-shadow: none;
  min-height: 0;
}

.kb-front-page--embedded .kb-descriptions {
  margin-bottom: 12px;
}

.kb-front-page--embedded .kb-tabs {
  min-height: 0;
}

.kb-front-page--embedded :deep(.kb-sidebar) {
  width: auto;
  padding: 10px;
  border-radius: 12px;
  min-height: 0;
}

.kb-front-page--embedded :deep(.page-toolbar) {
  margin-bottom: 12px;
}

.kb-front-page--embedded :deep(.page-toolbar__left),
.kb-front-page--embedded :deep(.page-toolbar__right) {
  gap: 8px;
}

.kb-front-page--embedded :deep(.page-toolbar__left .el-input) {
  width: 100% !important;
  max-width: 100%;
}

.kb-front-page--embedded :deep(.kb-list-item) {
  padding: 10px;
  margin-bottom: 8px;
}

.kb-front-page--embedded :deep(.kb-list-item__desc) {
  min-height: auto;
}

@media (max-width: 1200px) {
  .kb-front-page--embedded .kb-layout {
    grid-template-columns: minmax(0, 1fr);
    min-height: auto;
  }

  .kb-front-page--embedded :deep(.kb-sidebar) {
    width: 100%;
  }
}
</style>
