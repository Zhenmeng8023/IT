<template>
  <el-card shadow="never" class="pre-merge-gate-card" :class="{ 'is-active': active }">
    <div slot="header" class="gate-header">
      <div class="gate-title-group">
        <div class="gate-title">合并门禁</div>
        <div class="gate-subtitle">{{ subtitle }}</div>
      </div>
      <el-tag size="mini" effect="plain" :type="statusTagType">{{ statusLabel }}</el-tag>
    </div>

    <div v-if="loading" class="gate-state">
      <el-skeleton :rows="5" animated />
    </div>

    <div v-else-if="errorText" class="gate-state">
      <el-alert :title="errorText" type="error" :closable="false" show-icon />
    </div>

    <div v-else class="gate-body">
      <div class="gate-summary-box">
        <div class="gate-summary-line">
          <span class="gate-summary-label">最新结论</span>
          <span class="gate-summary-value">{{ mergeable ? '可合并' : '受阻' }}</span>
        </div>
        <div class="gate-summary-line">
          <span class="gate-summary-label">说明</span>
          <span class="gate-summary-value">{{ reasonText }}</span>
        </div>
      </div>

      <div v-if="blockingReasons.length" class="gate-blockers">
        <div class="gate-blockers-title">阻塞项</div>
        <div class="gate-blocker-list">
          <el-tag v-for="(item, index) in blockingReasons" :key="`${item}-${index}`" size="mini" type="warning" effect="plain">
            {{ item }}
          </el-tag>
        </div>
      </div>

      <div class="gate-actions">
        <el-button
          size="small"
          type="primary"
          plain
          :disabled="preMergeLoading || mergeLoading || !mergeRequestId"
          :loading="preMergeLoading"
          @click="$emit('pre-merge-check')"
        >
          预合并检查
        </el-button>
        <el-button
          size="small"
          plain
          :disabled="recheckLoading || loading || !mergeRequestId"
          :loading="recheckLoading"
          @click="$emit('recheck')"
        >
          重新检查
        </el-button>
        <el-button
          size="small"
          type="success"
          :disabled="mergeDisabled || !mergeRequestId"
          :loading="mergeLoading"
          @click="$emit('merge')"
        >
          继续合并
        </el-button>
      </div>

      <div class="gate-footnote">
        这里先保留门禁和操作位，后续线程会在此接入更细的合并策略、确认弹窗和自动化处理。
      </div>
    </div>
  </el-card>
</template>

<script>
export default {
  name: 'PreMergeGate',
  props: {
    mergeRequestId: {
      type: [String, Number],
      default: ''
    },
    loading: {
      type: Boolean,
      default: false
    },
    errorText: {
      type: String,
      default: ''
    },
    mergeable: {
      type: Boolean,
      default: false
    },
    reasonText: {
      type: String,
      default: ''
    },
    blockingReasons: {
      type: Array,
      default: () => []
    },
    statusLabel: {
      type: String,
      default: '待检查'
    },
    statusType: {
      type: String,
      default: 'info'
    },
    mergeDisabled: {
      type: Boolean,
      default: false
    },
    preMergeLoading: {
      type: Boolean,
      default: false
    },
    recheckLoading: {
      type: Boolean,
      default: false
    },
    mergeLoading: {
      type: Boolean,
      default: false
    },
    subtitle: {
      type: String,
      default: '预合并检查会在合并前再次确认门禁条件'
    }
  },
  computed: {
    active() {
      return !!this.mergeRequestId
    },
    statusTagType() {
      return this.statusType || 'info'
    }
  }
}
</script>

<style scoped>
.pre-merge-gate-card {
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid #e5ecf6;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
  height: 100%;
}

.pre-merge-gate-card.is-active {
  border-color: #bfdbfe;
}

.gate-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.gate-title-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.gate-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.gate-subtitle {
  font-size: 12px;
  line-height: 1.7;
  color: #64748b;
}

.gate-state {
  min-height: 280px;
  display: flex;
  align-items: center;
}

.gate-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.gate-summary-box {
  padding: 14px;
  border-radius: 14px;
  background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%);
  border: 1px solid #dbe7f7;
}

.gate-summary-line {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.gate-summary-line + .gate-summary-line {
  margin-top: 10px;
}

.gate-summary-label {
  flex: 0 0 72px;
  font-size: 12px;
  color: #64748b;
}

.gate-summary-value {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  line-height: 1.7;
  color: #0f172a;
  word-break: break-word;
}

.gate-blockers-title {
  font-size: 12px;
  font-weight: 700;
  color: #1f2937;
}

.gate-blocker-list {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.gate-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.gate-footnote {
  font-size: 12px;
  line-height: 1.8;
  color: #94a3b8;
}
</style>
