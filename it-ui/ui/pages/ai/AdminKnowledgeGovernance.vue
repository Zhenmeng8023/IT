<template>
  <div class="kb-governance-page ai-page">
    <el-card shadow="never" class="kb-card">
      <div class="kb-page-head">
        <div>
          <div class="kb-page-head__eyebrow">AI Governance</div>
          <div class="kb-page-head__title">知识库治理台</div>
          <div class="kb-page-head__subtitle">只保留全站治理与审计，不承载平台知识内容维护。</div>
        </div>
      </div>

      <div class="kb-filter-bar">
        <el-select
          :value="scopeType"
          clearable
          size="small"
          placeholder="全部范围"
          style="width: 160px"
          @change="scopeType = $event"
        >
          <el-option label="全部范围" value="" />
          <el-option label="平台" value="PLATFORM" />
          <el-option label="项目" value="PROJECT" />
          <el-option label="个人" value="PERSONAL" />
        </el-select>

        <el-input
          :value="ownerId"
          clearable
          size="small"
          placeholder="Owner ID"
          style="width: 180px"
          @input="ownerId = $event"
          @keyup.enter.native="handleFilterSearch"
        />

        <el-input
          :value="projectId"
          clearable
          size="small"
          placeholder="Project ID"
          style="width: 180px"
          @input="projectId = $event"
          @keyup.enter.native="handleFilterSearch"
        />

        <el-button type="primary" size="small" @click="handleFilterSearch">查询</el-button>
        <el-button size="small" @click="handleFilterReset">重置</el-button>
        <el-button size="small" @click="loadKnowledgeBases">刷新列表</el-button>
      </div>

      <div class="kb-layout">
        <div class="kb-sidebar">
          <div class="kb-sidebar__title">知识库列表</div>

          <div v-loading="loading.kbList" class="kb-list">
            <el-empty
              v-if="!knowledgeBases.length"
              description="暂无符合条件的知识库"
              :image-size="72"
            />

            <div
              v-for="item in knowledgeBases"
              :key="item.id"
              class="kb-list-item"
              :class="{ active: currentKnowledgeBase && String(currentKnowledgeBase.id) === String(item.id) }"
              @click="selectKnowledgeBase(item)"
            >
              <div class="kb-list-item__top">
                <div class="kb-list-item__name">{{ item.name || `知识库 #${item.id}` }}</div>
                <el-tag size="mini" :type="kbStatusTagType(item.status)">{{ item.status || 'UNKNOWN' }}</el-tag>
              </div>

              <div class="kb-list-item__desc">{{ item.description || '暂无描述' }}</div>

              <div class="kb-list-item__meta">
                <span>ID {{ item.id }}</span>
                <span>{{ displayKnowledgeBaseScope(item.scopeType) }}</span>
                <span>Owner {{ item.ownerId || '-' }}</span>
                <span>Project {{ item.projectId || '-' }}</span>
              </div>
            </div>
          </div>

          <div class="table-pagination">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="pagination.page + 1"
              :page-size="pagination.size"
              :total="pagination.total"
              @current-change="handleKbPageChange"
            />
          </div>
        </div>

        <div class="kb-main">
          <div v-if="!currentKnowledgeBase" class="kb-empty-state">
            <div class="kb-empty-card">
              <div class="kb-empty-card__icon">
                <i class="el-icon-reading" />
              </div>
              <div class="kb-empty-card__title">请选择左侧知识库进入治理</div>
              <div class="kb-empty-card__subtitle">
                当前页面只提供冻结、归档、删除与审计能力，不承担知识内容编辑。
              </div>
            </div>
          </div>

          <template v-else>
            <div class="kb-main__header">
              <div>
                <div class="kb-main__title">{{ currentKnowledgeBase.name || `知识库 #${currentKnowledgeBase.id}` }}</div>
                <div class="kb-main__subtitle">
                  {{ currentKnowledgeBase.description || '暂无描述信息' }}
                </div>
              </div>

              <div class="kb-main__header-actions">
                <el-button size="small" @click="loadKnowledgeBases">刷新列表</el-button>
              </div>
            </div>

            <el-descriptions :column="2" border size="small" class="kb-descriptions">
              <el-descriptions-item label="知识库 ID">{{ currentKnowledgeBase.id }}</el-descriptions-item>
              <el-descriptions-item label="范围类型">{{ displayKnowledgeBaseScope(currentKnowledgeBase.scopeType) }}</el-descriptions-item>
              <el-descriptions-item label="拥有者">{{ currentKnowledgeBase.ownerId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="项目 ID">{{ currentKnowledgeBase.projectId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="来源类型">{{ displayKnowledgeBaseSourceType(currentKnowledgeBase.sourceType) }}</el-descriptions-item>
              <el-descriptions-item label="可见性">{{ displayKnowledgeBaseVisibility(currentKnowledgeBase.visibility) }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ displayKnowledgeBaseStatus(currentKnowledgeBase.status) }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatTime(currentKnowledgeBase.createdAt) }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ formatTime(currentKnowledgeBase.updatedAt) }}</el-descriptions-item>
            </el-descriptions>

            <div class="kb-governance-note">
              当前治理页仅提供冻结、归档、删除与审计，不提供内容维护入口。
            </div>

            <div class="kb-governance-actions">
              <el-button :disabled="!canGovernCurrentKnowledgeBase" @click="freezeCurrentKnowledgeBase">冻结</el-button>
              <el-button :disabled="!canGovernCurrentKnowledgeBase" @click="archiveCurrentKnowledgeBase">归档</el-button>
              <el-button :disabled="!canGovernCurrentKnowledgeBase" type="danger" @click="deleteCurrentKnowledgeBase">删除</el-button>
              <el-button :disabled="!canGovernCurrentKnowledgeBase" @click="showKnowledgeBaseAuditLogs">审计</el-button>
            </div>
          </template>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import useAdminKnowledgeGovernancePage from '@/pages/ai/composables/useAdminKnowledgeGovernancePage'

export default {
  name: 'AdminKnowledgeGovernancePage',
  layout: 'manage',
  mixins: [useAdminKnowledgeGovernancePage]
}
</script>

<style scoped>
.kb-governance-page {
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

.kb-filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  padding: 12px 14px;
  border: 1px solid var(--it-border);
  border-radius: 14px;
  background:
    linear-gradient(180deg, rgba(86, 201, 255, 0.06), rgba(86, 201, 255, 0)),
    var(--it-panel-bg-strong);
}

.kb-layout {
  display: flex;
  gap: 16px;
  min-height: 720px;
}

.kb-sidebar {
  width: 360px;
  border: 1px solid var(--it-border);
  border-radius: 18px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.02), rgba(255, 255, 255, 0)),
    var(--it-panel-bg-strong);
  box-shadow: var(--it-shadow-soft);
}

.kb-sidebar__title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--it-text);
}

.kb-list {
  flex: 1;
  overflow: auto;
  min-height: 0;
  padding-right: 2px;
}

.kb-list-item {
  border: 1px solid var(--it-border);
  border-radius: 14px;
  padding: 12px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background 0.18s ease;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.025), rgba(255, 255, 255, 0)),
    var(--it-panel-bg);
}

.kb-list-item:hover,
.kb-list-item.active {
  border-color: var(--it-primary);
  background:
    linear-gradient(180deg, rgba(86, 201, 255, 0.12), rgba(86, 201, 255, 0.02)),
    color-mix(in srgb, var(--it-panel-bg) 82%, rgba(86, 201, 255, 0.18));
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.28);
  transform: translateY(-1px);
}

.kb-list-item__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.kb-list-item__name {
  font-weight: 600;
  color: var(--it-text);
}

.kb-list-item__desc,
.kb-list-item__meta {
  color: var(--it-text-subtle);
  font-size: 12px;
}

.kb-list-item__desc {
  margin: 8px 0;
  line-height: 1.6;
  min-height: 40px;
}

.kb-list-item__meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.table-pagination {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
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
.kb-governance-actions {
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
  font-weight: 700;
  margin-bottom: 6px;
  color: var(--it-text);
}

.kb-main__subtitle {
  color: var(--it-text-subtle);
  font-size: 13px;
  line-height: 1.7;
}

.kb-descriptions {
  margin-bottom: 14px;
}

.kb-governance-note {
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid color-mix(in srgb, var(--it-primary) 18%, var(--it-border));
  background:
    linear-gradient(180deg, rgba(86, 201, 255, 0.08), rgba(86, 201, 255, 0.02)),
    var(--it-soft-gradient);
  color: var(--it-text-subtle);
  font-size: 12px;
  line-height: 1.7;
}

.kb-governance-actions {
  padding-top: 4px;
}

.kb-governance-page ::v-deep .el-card__body {
  background: transparent;
}

.kb-governance-page ::v-deep .el-empty__description p {
  color: var(--it-text-subtle);
}

.kb-governance-page ::v-deep .el-descriptions {
  border-radius: 16px;
  overflow: hidden;
}

.kb-governance-page ::v-deep .el-descriptions__body {
  background: transparent;
}

.kb-governance-page ::v-deep .el-descriptions-item__label,
.kb-governance-page ::v-deep .el-descriptions-item__content {
  border-color: color-mix(in srgb, var(--it-border) 88%, rgba(255, 255, 255, 0.04)) !important;
}

.kb-governance-page ::v-deep .el-descriptions-item__label {
  background: color-mix(in srgb, var(--it-panel-bg) 88%, rgba(86, 201, 255, 0.08)) !important;
  color: var(--it-text-subtle) !important;
}

.kb-governance-page ::v-deep .el-descriptions-item__content {
  background: color-mix(in srgb, var(--it-panel-bg-strong) 92%, rgba(255, 255, 255, 0.02)) !important;
  color: var(--it-text) !important;
}

.kb-governance-page ::v-deep .el-tag {
  border-color: color-mix(in srgb, var(--it-primary) 18%, var(--it-border));
  background: color-mix(in srgb, var(--it-primary) 12%, transparent);
}

@media (max-width: 1200px) {
  .kb-layout {
    flex-direction: column;
    min-height: auto;
  }

  .kb-sidebar {
    width: 100%;
  }
}
</style>
