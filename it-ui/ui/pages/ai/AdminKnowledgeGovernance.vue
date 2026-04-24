<template>
  <div class="kb-governance-page">
    <el-card shadow="never" class="kb-card">
      <div class="kb-page-head">
        <div class="kb-page-head__title">知识库治理台</div>
        <div class="kb-page-head__subtitle">后台仅负责治理，不提供内容录入与普通用户编辑入口。</div>
      </div>

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
          :show-list-mode-switch="true"
          :show-owner-id-input="true"
          :show-project-id-input="true"
          :show-all-mode-option="true"
          @mode-change="handleListModeChange"
          @refresh="loadKnowledgeBases"
          @select="selectKnowledgeBase"
          @page-change="handleKbPageChange"
        />

        <div class="kb-main">
          <div v-if="!currentKnowledgeBase" class="kb-empty-state">
            <el-empty
              description="请选择左侧知识库进入治理台"
              :image-size="96"
            />
          </div>

          <template v-else>
            <div class="kb-main__header">
              <div>
                <div class="kb-main__title">{{ currentKnowledgeBase.name || `知识库 #${currentKnowledgeBase.id}` }}</div>
                <div class="kb-main__subtitle">
                  {{ currentKnowledgeBase.description || '该知识库暂无描述信息。' }}
                </div>
              </div>

              <div class="kb-main__header-actions">
                <el-button size="small" @click="loadKnowledgeBases">刷新列表</el-button>
                <el-button size="small" @click="manualRefreshEmbeddingStatus">刷新向量状态</el-button>
              </div>
            </div>

            <el-descriptions :column="2" border size="small" class="kb-descriptions">
              <el-descriptions-item label="知识库 ID">{{ currentKnowledgeBase.id }}</el-descriptions-item>
              <el-descriptions-item label="拥有者">{{ currentKnowledgeBase.ownerId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="作用域">{{ displayKnowledgeBaseScope(currentKnowledgeBase.scopeType) }}</el-descriptions-item>
              <el-descriptions-item label="项目 ID">{{ currentKnowledgeBase.projectId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="来源类型">{{ displayKnowledgeBaseSourceType(currentKnowledgeBase.sourceType) }}</el-descriptions-item>
              <el-descriptions-item label="可见性">{{ displayKnowledgeBaseVisibility(currentKnowledgeBase.visibility) }}</el-descriptions-item>
              <el-descriptions-item label="向量提供方">{{ currentKnowledgeBase.embeddingProvider || '-' }}</el-descriptions-item>
              <el-descriptions-item label="向量模型">{{ currentKnowledgeBase.embeddingModel || '-' }}</el-descriptions-item>
              <el-descriptions-item label="切块策略">{{ currentKnowledgeBase.chunkStrategy || '-' }}</el-descriptions-item>
              <el-descriptions-item label="默认 TopK">{{ currentKnowledgeBase.defaultTopK || '-' }}</el-descriptions-item>
              <el-descriptions-item label="最后索引时间">{{ formatTime(currentKnowledgeBase.lastIndexedAt) }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ displayKnowledgeBaseStatus(currentKnowledgeBase.status) }}</el-descriptions-item>
            </el-descriptions>

            <div class="kb-stat-panel">
              <el-tag size="mini" type="info" effect="plain">文档总数 {{ documentStats.totalCount }}</el-tag>
              <el-tag size="mini" type="success" effect="plain">已索引 {{ documentStats.indexedCount }}</el-tag>
              <el-tag size="mini" type="warning" effect="plain">处理中 {{ documentStats.processingCount }}</el-tag>
              <el-tag size="mini" type="danger" effect="plain">失败 {{ documentStats.failedCount }}</el-tag>
              <el-tag size="mini" type="success" effect="plain">向量完成文档 {{ documentStats.embeddedReadyCount }}</el-tag>
            </div>

            <div class="kb-embedding-panel">
              <div class="kb-embedding-panel__stats">
                <el-tag size="mini" type="info" effect="plain">切片数 {{ kbEmbeddingStatus.totalChunkCount || 0 }}</el-tag>
                <el-tag size="mini" type="success" effect="plain">已向量化 {{ kbEmbeddingStatus.embeddedChunkCount || 0 }}</el-tag>
                <el-tag size="mini" :type="embeddingCompletionRate >= 100 ? 'success' : 'warning'" effect="plain">
                  完成率 {{ embeddingCompletionRate }}%
                </el-tag>
              </div>
              <div class="kb-embedding-progress">
                <el-progress
                  :percentage="embeddingCompletionRate"
                  :stroke-width="10"
                  :status="embeddingCompletionRate >= 100 && !embeddingBackfillRunning ? 'success' : undefined"
                />
              </div>
            </div>

            <AdminKnowledgeGovernanceActions
              :debug-query.sync="debugForm.query"
              :debug-top-k.sync="debugForm.topK"
              :loading-debug-search="loading.debugSearch"
              :embedding-backfill-running="embeddingBackfillRunning"
              :embedding-backfill-submitting="embeddingBackfillSubmitting"
              :embedding-button-text="embeddingButtonText"
              :can-govern="canGovernCurrentKnowledgeBase"
              :placeholder-actions="governancePlaceholders"
              @debug-search="runDebugSearch"
              @reindex="reindexKnowledgeBase(currentKnowledgeBase)"
              @open-tasks="openKnowledgeBaseTasks"
              @backfill-embedding="backfillCurrentKnowledgeBaseEmbeddings"
              @placeholder="handleGovernancePlaceholder"
            />

            <AdminKnowledgeDocumentTable
              :loading="loading.documents"
              :documents="documents"
              :pagination="documentPagination"
              :can-govern="canGovernCurrentKnowledgeBase"
              :format-time="formatTime"
              :doc-status-tag-type="docStatusTagType"
              :display-source-type="displayDocumentSourceType"
              :display-document-status="displayDocumentStatus"
              :document-embedding-label="documentEmbeddingLabel"
              :document-embedding-tag-type="documentEmbeddingTagType"
              @refresh="loadDocuments"
              @reindex-document="reindexDocument"
              @backfill-document="backfillDocumentVector"
              @view-tasks="viewIndexTasks"
              @page-change="handleDocumentPageChange"
            />
          </template>
        </div>
      </div>
    </el-card>

    <KnowledgeBaseTaskDrawer
      :visible.sync="taskDrawerVisible"
      :title="taskDrawerTitle"
      :subtitle="taskDrawerSubtitle"
      :loading="loading.tasks"
      :tasks="indexTasks"
      :task-status-tag-type="taskStatusTagType"
      :task-type-label="displayTaskType"
      :task-status-label="displayTaskStatus"
      :format-time="formatTime"
    />

    <KnowledgeBaseRetrievalDrawer
      :visible.sync="retrievalDrawerVisible"
      :meta="currentRetrievalMeta"
      :loading="loading.retrievals || loading.debugSearch"
      :logs="retrievalLogs"
      :retrieval-method-label="displayRetrievalMethod"
    />
  </div>
</template>

<script>
import KnowledgeBaseListPanel from '@/components/ai/kb/panels/KnowledgeBaseListPanel.vue'
import KnowledgeBaseTaskDrawer from '@/components/ai/kb/drawers/KnowledgeBaseTaskDrawer.vue'
import KnowledgeBaseRetrievalDrawer from '@/components/ai/kb/drawers/KnowledgeBaseRetrievalDrawer.vue'
import AdminKnowledgeDocumentTable from '@/components/ai/kb/admin/AdminKnowledgeDocumentTable.vue'
import AdminKnowledgeGovernanceActions from '@/components/ai/kb/admin/AdminKnowledgeGovernanceActions.vue'
import useAdminKnowledgeGovernancePage from '@/pages/ai/composables/useAdminKnowledgeGovernancePage'

export default {
  name: 'AdminKnowledgeGovernancePage',
  layout: 'manage',
  components: {
    KnowledgeBaseListPanel,
    KnowledgeBaseTaskDrawer,
    KnowledgeBaseRetrievalDrawer,
    AdminKnowledgeDocumentTable,
    AdminKnowledgeGovernanceActions
  },
  mixins: [useAdminKnowledgeGovernancePage]
}
</script>

<style scoped>
.kb-governance-page {
  padding: 16px;
}

.kb-card {
  border-radius: 12px;
}

.kb-page-head {
  margin-bottom: 12px;
}

.kb-page-head__title {
  color: var(--it-text);
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}

.kb-page-head__subtitle {
  color: var(--it-text-subtle);
  font-size: 12px;
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

.kb-main__header,
.kb-main__header-actions,
.kb-stat-panel,
.kb-embedding-panel__stats {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.kb-main__header {
  justify-content: space-between;
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
  margin-bottom: 14px;
}

.kb-stat-panel {
  margin-bottom: 14px;
}

.kb-embedding-panel {
  margin: 12px 0 16px;
  padding: 12px 14px;
  border: 1px solid var(--it-border);
  border-radius: 14px;
  background: var(--it-soft-gradient);
  box-shadow: var(--it-shadow-soft);
}

.kb-embedding-progress {
  margin-top: 8px;
}
</style>
