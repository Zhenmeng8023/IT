<template>
  <div class="kb-page">
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
          @edit="openKbDialog('edit', $event)"
          @reindex="reindexKnowledgeBase"
          @page-change="handleKbPageChange"
        />

        <div class="kb-main">
          <el-empty
            v-if="!currentKnowledgeBase"
            description="请选择左侧知识库"
            :image-size="96"
          />

          <template v-else>
            <div class="kb-main__header">
              <div>
                <div class="kb-main__title">{{ currentKnowledgeBase.name || `知识库 #${currentKnowledgeBase.id}` }}</div>
                <div class="kb-main__subtitle">
                  {{ currentKnowledgeBase.description || '这个知识库还没有描述。' }}
                </div>
              </div>

              <div class="kb-main__header-actions">
                <el-button v-if="canEditCurrentKnowledgeBase" size="small" @click="openKbDialog('edit', currentKnowledgeBase)">编辑知识库</el-button>
                <el-button size="small" @click="openKnowledgeBaseTasks">索引任务</el-button>
                <el-button v-if="canEditCurrentKnowledgeBase" type="primary" size="small" @click="reindexKnowledgeBase(currentKnowledgeBase)">重建索引</el-button>
              </div>
            </div>

            <el-descriptions :column="2" border size="small" class="kb-descriptions">
              <el-descriptions-item label="知识库 ID">{{ currentKnowledgeBase.id }}</el-descriptions-item>
              <el-descriptions-item label="拥有者">{{ currentKnowledgeBase.ownerId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="作用域">{{ currentKnowledgeBase.scopeType || '-' }}</el-descriptions-item>
              <el-descriptions-item label="项目 ID">{{ currentKnowledgeBase.projectId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="来源类型">{{ currentKnowledgeBase.sourceType || '-' }}</el-descriptions-item>
              <el-descriptions-item label="可见性">{{ currentKnowledgeBase.visibility || '-' }}</el-descriptions-item>
              <el-descriptions-item label="Embedding Provider">{{ currentKnowledgeBase.embeddingProvider || '-' }}</el-descriptions-item>
              <el-descriptions-item label="Embedding Model">{{ currentKnowledgeBase.embeddingModel || '-' }}</el-descriptions-item>
              <el-descriptions-item label="切块策略">{{ currentKnowledgeBase.chunkStrategy || '-' }}</el-descriptions-item>
              <el-descriptions-item label="默认 TopK">{{ currentKnowledgeBase.defaultTopK || '-' }}</el-descriptions-item>
              <el-descriptions-item label="默认问答模型">{{ currentKnowledgeBaseDefaultModelName }}</el-descriptions-item>
              <el-descriptions-item label="最后索引时间">{{ formatTime(currentKnowledgeBase.lastIndexedAt) }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ currentKnowledgeBase.status || '-' }}</el-descriptions-item>
            </el-descriptions>

            <div class="kb-embedding-panel">
              <div class="kb-embedding-panel__stats">
                <el-tag size="mini" type="info" effect="plain">Chunks {{ kbEmbeddingStatus.totalChunkCount || 0 }}</el-tag>
                <el-tag size="mini" type="success" effect="plain">已向量化 {{ kbEmbeddingStatus.embeddedChunkCount || 0 }}</el-tag>
                <el-tag size="mini" :type="embeddingCompletionRate >= 100 ? 'success' : 'warning'" effect="plain">
                  完成度 {{ embeddingCompletionRate }}%
                </el-tag>
              </div>
              <div class="kb-embedding-panel__actions">
                <el-button size="mini" @click="manualRefreshEmbeddingStatus">刷新向量状态</el-button>
                <el-button
                  size="mini"
                  type="primary"
                  :disabled="embeddingBackfillRunning"
                  :loading="embeddingBackfillSubmitting"
                  @click="backfillCurrentKnowledgeBaseEmbeddings"
                >
                  {{ embeddingButtonText }}
                </el-button>
              </div>
            </div>

            <div class="kb-embedding-progress">
              <el-progress
                :percentage="embeddingCompletionRate"
                :stroke-width="12"
                :status="embeddingCompletionRate >= 100 && !embeddingBackfillRunning ? 'success' : undefined"
              />
              <div class="kb-embedding-progress__hint text-muted">
                <span v-if="embeddingBackfillRunning">正在执行向量回填，页面会自动轮询进度。</span>
                <span v-else>已向量化 {{ kbEmbeddingStatus.embeddedChunkCount || 0 }} / {{ kbEmbeddingStatus.totalChunkCount || 0 }}</span>
              </div>
            </div>

            <el-tabs v-model="activeTab" class="kb-tabs">
              <el-tab-pane label="文档管理" name="documents">
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
                  @open-retrieval-by-message="openRetrievalDrawerByMessage"
                  @toggle-message-sources="toggleMessageSources"
                  @locate-source-document="locateSourceDocument"
                  @open-retrieval="openRetrievalDrawer"
                  @clear-chat="clearChat"
                  @debug-search="runChatDebugSearch"
                  @send-chat="sendChat"
                  @stop-stream="stopStream"
                />
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

    <KnowledgeBaseTaskDrawer
      :visible.sync="taskDrawerVisible"
      :title="taskDrawerTitle"
      :subtitle="taskDrawerSubtitle"
      :loading="loading.tasks"
      :tasks="indexTasks"
      :task-status-tag-type="taskStatusTagType"
      :format-time="formatTime"
    />

    <KnowledgeBaseRetrievalDrawer
      :visible.sync="retrievalDrawerVisible"
      :meta="currentRetrievalMeta"
      :loading="loading.retrievals"
      :logs="retrievalLogs"
    />
  </div>
</template>

<script>
import KnowledgeBaseListPanel from '@/components/ai/kb/panels/KnowledgeBaseListPanel.vue'
import KnowledgeBaseDocumentTab from '@/components/ai/kb/panels/KnowledgeBaseDocumentTab.vue'
import KnowledgeBaseMemberTab from '@/components/ai/kb/panels/KnowledgeBaseMemberTab.vue'
import KnowledgeBaseChatTab from '@/components/ai/kb/panels/KnowledgeBaseChatTab.vue'
import KnowledgeBaseBaseFormDialog from '@/components/ai/kb/dialogs/KnowledgeBaseBaseFormDialog.vue'
import KnowledgeBaseDocumentImportDialog from '@/components/ai/kb/dialogs/KnowledgeBaseDocumentImportDialog.vue'
import KnowledgeBaseChunkPreviewDialog from '@/components/ai/kb/dialogs/KnowledgeBaseChunkPreviewDialog.vue'
import KnowledgeBaseTaskDrawer from '@/components/ai/kb/drawers/KnowledgeBaseTaskDrawer.vue'
import KnowledgeBaseRetrievalDrawer from '@/components/ai/kb/drawers/KnowledgeBaseRetrievalDrawer.vue'
import useKnowledgeBasePage from '@/pages/ai/composables/useKnowledgeBasePage'

export default {
  name: 'KnowledgeBasePage',
  components: {
    KnowledgeBaseListPanel,
    KnowledgeBaseDocumentTab,
    KnowledgeBaseMemberTab,
    KnowledgeBaseChatTab,
    KnowledgeBaseBaseFormDialog,
    KnowledgeBaseDocumentImportDialog,
    KnowledgeBaseChunkPreviewDialog,
    KnowledgeBaseTaskDrawer,
    KnowledgeBaseRetrievalDrawer
  },
  mixins: [useKnowledgeBasePage],
  methods: {
    handleKbFormSave(form) {
      this.kbForm = { ...form }
      this.submitKbForm()
    },

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
.kb-page {
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
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 16px;
  background: #fff;
}

.kb-main__header,
.kb-main__header-actions,
.kb-embedding-panel,
.kb-embedding-panel__stats,
.kb-embedding-panel__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.kb-main__header,
.kb-embedding-panel {
  justify-content: space-between;
}

.kb-main__header {
  margin-bottom: 16px;
}

.kb-main__title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 6px;
}

.kb-main__subtitle,
.text-muted {
  color: #909399;
  font-size: 12px;
}

.kb-descriptions {
  margin-bottom: 16px;
}

.kb-embedding-panel {
  margin: 12px 0 16px;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
}

.kb-embedding-progress {
  margin-top: 8px;
}

.kb-embedding-progress__hint {
  margin-top: 6px;
}

.kb-tabs {
  min-height: 540px;
}
</style>
