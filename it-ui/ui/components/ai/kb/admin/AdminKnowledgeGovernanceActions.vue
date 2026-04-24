<template>
  <div class="governance-panel">
    <div class="governance-panel__block">
      <div class="governance-panel__title">调试检索</div>
      <div class="governance-panel__row">
        <el-input
          :value="debugQuery"
          clearable
          placeholder="输入问题并执行检索调试"
          @input="$emit('update:debugQuery', $event)"
        />
        <el-input-number
          :value="debugTopK"
          :min="1"
          :max="50"
          controls-position="right"
          @input="$emit('update:debugTopK', $event)"
        />
        <el-button
          type="primary"
          :loading="loadingDebugSearch"
          :disabled="!canGovern"
          @click="$emit('debug-search')"
        >
          调试检索
        </el-button>
      </div>
    </div>

    <div class="governance-panel__block">
      <div class="governance-panel__title">治理动作</div>
      <div class="governance-panel__row">
        <el-button :disabled="!canGovern" @click="$emit('open-tasks')">查看任务状态</el-button>
        <el-button :disabled="!canGovern" @click="$emit('reindex')">重建知识库索引</el-button>
        <el-button
          type="primary"
          :disabled="!canGovern || embeddingBackfillRunning"
          :loading="embeddingBackfillSubmitting"
          @click="$emit('backfill-embedding')"
        >
          {{ embeddingButtonText }}
        </el-button>
      </div>
      <div class="governance-panel__row governance-panel__row--placeholder">
        <el-button :disabled="!canGovern" size="mini" @click="$emit('freeze')">冻结知识库</el-button>
        <el-button :disabled="!canGovern" size="mini" @click="$emit('archive')">归档知识库</el-button>
        <el-button :disabled="!canGovern" size="mini" type="danger" @click="$emit('delete')">删除知识库</el-button>
        <el-button :disabled="!canGovern" size="mini" @click="$emit('audit')">操作审计</el-button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdminKnowledgeGovernanceActions',
  props: {
    debugQuery: {
      type: String,
      default: ''
    },
    debugTopK: {
      type: Number,
      default: 5
    },
    loadingDebugSearch: {
      type: Boolean,
      default: false
    },
    canGovern: {
      type: Boolean,
      default: false
    },
    embeddingBackfillRunning: {
      type: Boolean,
      default: false
    },
    embeddingBackfillSubmitting: {
      type: Boolean,
      default: false
    },
    embeddingButtonText: {
      type: String,
      default: '执行向量回填'
    }
  }
}
</script>

<style scoped>
.governance-panel {
  margin-bottom: 16px;
  padding: 14px;
  border: 1px solid var(--it-border);
  border-radius: 14px;
  background: var(--it-panel-bg-strong);
}

.governance-panel__block + .governance-panel__block {
  margin-top: 14px;
}

.governance-panel__title {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--it-text);
}

.governance-panel__row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.governance-panel__row :deep(.el-input) {
  width: min(420px, 100%);
}

.governance-panel__row--placeholder {
  margin-top: 10px;
}
</style>
