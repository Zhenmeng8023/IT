<template>
  <div class="kb-page ai-page">
    <el-card shadow="never" class="kb-card">
      <div class="kb-page-head">
        <div>
          <div class="kb-page-head__eyebrow">Platform Operations</div>
          <div class="kb-page-head__title">平台知识库</div>
          <div class="kb-page-head__subtitle">只维护平台官方知识，不承载个人库或项目库语义。</div>
        </div>
      </div>

      <div class="kb-layout">
        <KnowledgeBaseListPanel
          :keyword.sync="keyword"
          :loading="loading.kbList"
          :knowledge-bases="filteredKnowledgeBases"
          :current-knowledge-base="currentKnowledgeBase"
          :pagination="pagination"
          :can-create-knowledge-base="canCreateKnowledgeBase"
          :can-edit-knowledge-base-item="canEditKnowledgeBaseItem"
          :kb-status-tag-type="kbStatusTagType"
          :show-list-mode-switch="false"
          :show-owner-id-input="false"
          :show-project-id-input="false"
          @refresh="loadKnowledgeBases"
          @create="openKbDialog('create')"
          @select="selectKnowledgeBase"
          @edit="openKbDialog('edit', $event)"
          @reindex="reindexKnowledgeBase"
          @page-change="handleKbPageChange"
        />

        <div class="kb-main">
          <div v-if="!currentKnowledgeBase" class="kb-empty-state">
            <div class="kb-empty-card">
              <div class="kb-empty-card__icon">
                <i class="el-icon-office-building" />
              </div>
              <div class="kb-empty-card__title">请选择左侧平台知识库</div>
              <div class="kb-empty-card__subtitle">
                这里用于维护官方知识内容、索引和向量状态，不处理个人或项目资产。
              </div>
            </div>
          </div>

          <template v-else>
            <div class="kb-main__header">
              <div>
                <div class="kb-main__title">
                  {{ currentKnowledgeBase.name || `知识库 #${currentKnowledgeBase.id}` }}
                </div>
                <div class="kb-main__subtitle">
                  {{ currentKnowledgeBase.description || '这个平台知识库还没有填写描述。' }}
                </div>
              </div>

              <div class="kb-main__header-actions">
                <el-button
                  v-if="canEditCurrentKnowledgeBase"
                  size="small"
                  @click="openKbDialog('edit', currentKnowledgeBase)"
                >
                  编辑知识库
                </el-button>
                <el-button size="small" @click="openKnowledgeBaseTasks">索引任务</el-button>
                <el-button
                  v-if="canEditCurrentKnowledgeBase"
                  type="primary"
                  size="small"
                  @click="reindexKnowledgeBase(currentKnowledgeBase)"
                >
                  重建索引
                </el-button>
              </div>
            </div>

            <el-descriptions :column="2" border size="small" class="kb-descriptions">
              <el-descriptions-item label="知识库 ID">{{ currentKnowledgeBase.id }}</el-descriptions-item>
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
                <el-tag size="mini" type="info" effect="plain">
                  Chunks {{ kbEmbeddingStatus.totalChunkCount || 0 }}
                </el-tag>
                <el-tag size="mini" type="success" effect="plain">
                  已向量化 {{ kbEmbeddingStatus.embeddedChunkCount || 0 }}
                </el-tag>
                <el-tag
                  size="mini"
                  :type="embeddingCompletionRate >= 100 ? 'success' : 'warning'"
                  effect="plain"
                >
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
                <span v-else>
                  已向量化 {{ kbEmbeddingStatus.embeddedChunkCount || 0 }} / {{ kbEmbeddingStatus.totalChunkCount || 0 }}
                </span>
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
                  :show-view-chunks-action="true"
                  :show-chunk-preview-action="true"
                  :show-download-action="true"
                  :show-seed-chat-action="true"
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
      :show-owner-id-field="false"
      :show-scope-type-field="false"
      :show-project-id-field="false"
      :show-source-type-field="false"
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
import KnowledgeBaseBaseFormDialog from '@/components/ai/kb/dialogs/KnowledgeBaseBaseFormDialog.vue'
import KnowledgeBaseDocumentImportDialog from '@/components/ai/kb/dialogs/KnowledgeBaseDocumentImportDialog.vue'
import KnowledgeBaseChunkPreviewDialog from '@/components/ai/kb/dialogs/KnowledgeBaseChunkPreviewDialog.vue'
import KnowledgeBaseTaskDrawer from '@/components/ai/kb/drawers/KnowledgeBaseTaskDrawer.vue'
import KnowledgeBaseRetrievalDrawer from '@/components/ai/kb/drawers/KnowledgeBaseRetrievalDrawer.vue'
import useAdminPlatformKnowledgeBasePage from '@/pages/ai/composables/useAdminPlatformKnowledgeBasePage'

export default {
  name: 'AdminPlatformKnowledgeBasePage',
  layout: 'manage',
  components: {
    KnowledgeBaseListPanel,
    KnowledgeBaseDocumentTab,
    KnowledgeBaseBaseFormDialog,
    KnowledgeBaseDocumentImportDialog,
    KnowledgeBaseChunkPreviewDialog,
    KnowledgeBaseTaskDrawer,
    KnowledgeBaseRetrievalDrawer
  },
  mixins: [useAdminPlatformKnowledgeBasePage],
  methods: {
    handleKbFormSave(form) {
      this.kbForm = { ...form }
      this.submitKbForm()
    },

    handleDocumentFormSave(form) {
      this.documentForm = { ...form }
      this.submitDocumentForm()
    }
  }
}
</script>

<style scoped>
.kb-page {
  padding: 16px;
}

.kb-card {
  border-radius: 14px;
  border: 1px solid color-mix(in srgb, var(--it-border) 86%, rgba(255, 255, 255, 0.04));
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.015), rgba(255, 255, 255, 0)),
    var(--it-panel-bg);
  box-shadow: var(--it-shadow-soft);
}

.kb-page-head {
  margin-bottom: 14px;
  padding: 4px 2px 0;
}

.kb-page-head__eyebrow {
  margin-bottom: 8px;
  color: var(--it-accent);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.kb-page-head__title {
  color: var(--it-text);
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 6px;
}

.kb-page-head__subtitle {
  color: var(--it-text-subtle);
  font-size: 13px;
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
  background:
    radial-gradient(circle at top right, rgba(86, 201, 255, 0.08), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.02), rgba(255, 255, 255, 0)),
    var(--it-panel-bg-strong);
  box-shadow: var(--it-shadow-soft);
}

.kb-empty-state {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.kb-empty-card {
  max-width: 440px;
  padding: 30px 26px;
  border: 1px dashed color-mix(in srgb, var(--it-primary) 30%, var(--it-border));
  border-radius: 22px;
  background:
    radial-gradient(circle at top, rgba(86, 201, 255, 0.12), transparent 54%),
    color-mix(in srgb, var(--it-panel-bg) 90%, rgba(86, 201, 255, 0.06));
  text-align: center;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.kb-empty-card__icon {
  width: 58px;
  height: 58px;
  margin: 0 auto 14px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--it-primary) 18%, transparent);
  color: var(--it-primary);
  font-size: 24px;
  box-shadow: 0 10px 24px rgba(34, 211, 238, 0.12);
}

.kb-empty-card__title {
  margin-bottom: 8px;
  color: var(--it-text);
  font-size: 18px;
  font-weight: 600;
}

.kb-empty-card__subtitle {
  color: var(--it-text-subtle);
  font-size: 13px;
  line-height: 1.7;
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
  font-weight: 700;
  margin-bottom: 6px;
  color: var(--it-text);
}

.kb-main__subtitle,
.text-muted {
  color: var(--it-text-subtle);
  font-size: 13px;
  line-height: 1.7;
}

.kb-descriptions {
  margin-bottom: 16px;
}

.kb-embedding-panel {
  margin: 12px 0 16px;
  padding: 12px 14px;
  border: 1px solid color-mix(in srgb, var(--it-primary) 18%, var(--it-border));
  border-radius: 14px;
  background:
    linear-gradient(180deg, rgba(86, 201, 255, 0.08), rgba(86, 201, 255, 0.02)),
    var(--it-soft-gradient);
  box-shadow: var(--it-shadow-soft);
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

.kb-page ::v-deep .el-card__body {
  background: transparent;
}

.kb-page ::v-deep .el-descriptions {
  border-radius: 16px;
  overflow: hidden;
}

.kb-page ::v-deep .el-descriptions__body {
  background: transparent;
}

.kb-page ::v-deep .el-descriptions-item__label,
.kb-page ::v-deep .el-descriptions-item__content {
  border-color: color-mix(in srgb, var(--it-border) 88%, rgba(255, 255, 255, 0.04)) !important;
}

.kb-page ::v-deep .el-descriptions-item__label {
  background: color-mix(in srgb, var(--it-panel-bg) 88%, rgba(86, 201, 255, 0.08)) !important;
  color: var(--it-text-subtle) !important;
}

.kb-page ::v-deep .el-descriptions-item__content {
  background: color-mix(in srgb, var(--it-panel-bg-strong) 92%, rgba(255, 255, 255, 0.02)) !important;
  color: var(--it-text) !important;
}

.kb-page ::v-deep .el-tag {
  border-color: color-mix(in srgb, var(--it-primary) 18%, var(--it-border));
  background: color-mix(in srgb, var(--it-primary) 12%, transparent);
}

.kb-page ::v-deep .el-progress-bar__outer {
  background: color-mix(in srgb, var(--it-panel-bg) 88%, rgba(255, 255, 255, 0.04));
}

@media (max-width: 1200px) {
  .kb-layout {
    flex-direction: column;
    min-height: auto;
  }
}
</style>
