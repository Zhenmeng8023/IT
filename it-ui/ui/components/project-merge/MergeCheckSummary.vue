<template>
  <el-card shadow="never" class="merge-check-summary-card">
    <div slot="header" class="summary-header">
      <div class="summary-title-group">
        <div class="summary-eyebrow">Latest merge-check</div>
        <div class="summary-title">冲突处理中心</div>
        <div class="summary-subtitle">
          {{ summarySubtitle }}
        </div>
      </div>

      <div class="summary-actions">
        <el-tag size="mini" effect="plain" :type="statusTagType">{{ statusLabel }}</el-tag>
        <el-button size="mini" plain :disabled="refreshDisabled" :loading="loading" @click="$emit('refresh')">
          刷新
        </el-button>
        <el-button size="mini" type="warning" plain :disabled="recheckDisabled" :loading="recheckLoading" @click="$emit('recheck')">
          重新检查
        </el-button>
      </div>
    </div>

    <div v-if="loading" class="summary-loading">
      <el-skeleton :rows="2" animated />
    </div>

    <div v-else-if="errorText" class="summary-error">
      <el-alert :title="errorText" type="error" :closable="false" show-icon />
    </div>

    <div v-else-if="!cards.length" class="summary-empty">
      <el-empty description="暂无 merge-check 摘要" :image-size="80" />
    </div>

    <div v-else class="summary-grid">
      <div
        v-for="card in cards"
        :key="card.key"
        class="summary-card"
        :class="card.tone ? `tone-${card.tone}` : ''"
      >
        <div class="summary-card-label">{{ card.label }}</div>
        <div class="summary-card-value">{{ card.value }}</div>
        <div class="summary-card-desc">{{ card.desc }}</div>
      </div>
    </div>
  </el-card>
</template>

<script>
export default {
  name: 'MergeCheckSummary',
  props: {
    cards: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    },
    errorText: {
      type: String,
      default: ''
    },
    recheckLoading: {
      type: Boolean,
      default: false
    },
    statusLabel: {
      type: String,
      default: '待检查'
    },
    statusType: {
      type: String,
      default: 'info'
    },
    summarySubtitle: {
      type: String,
      default: ''
    }
  },
  computed: {
    refreshDisabled() {
      return this.loading
    },
    recheckDisabled() {
      return this.recheckLoading || this.loading
    },
    statusTagType() {
      return this.statusType || 'info'
    }
  }
}
</script>

<style scoped>
.merge-check-summary-card {
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid #e5ecf6;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
}

.summary-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.summary-title-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.summary-eyebrow {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #2563eb;
  font-weight: 700;
}

.summary-title {
  font-size: 22px;
  line-height: 1.4;
  color: #0f172a;
  font-weight: 700;
}

.summary-subtitle {
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
  max-width: 920px;
}

.summary-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-card {
  padding: 16px;
  border-radius: 16px;
  border: 1px solid #e5ecf6;
  background: #ffffff;
}

.summary-card.tone-blue { background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%); }
.summary-card.tone-cyan { background: linear-gradient(180deg, #ecfeff 0%, #ffffff 100%); }
.summary-card.tone-success { background: linear-gradient(180deg, #f0fdf4 0%, #ffffff 100%); }
.summary-card.tone-warning { background: linear-gradient(180deg, #fff7ed 0%, #ffffff 100%); }
.summary-card.tone-danger { background: linear-gradient(180deg, #fff1f2 0%, #ffffff 100%); }

.summary-card-label {
  font-size: 12px;
  color: #64748b;
}

.summary-card-value {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  word-break: break-word;
}

.summary-card-desc {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: #94a3b8;
}

.summary-loading,
.summary-error,
.summary-empty {
  min-height: 120px;
  display: flex;
  align-items: center;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .summary-header {
    flex-direction: column;
  }

  .summary-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
