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

      <div v-if="blockingCheckLines.length" class="gate-checks">
        <div class="gate-blockers-title">Failed Checks</div>
        <div class="gate-check-lines">
          <div v-for="(item, index) in blockingCheckLines" :key="`failed-${index}`" class="gate-check-line">
            {{ item }}
          </div>
        </div>
        <div class="gate-check-actions">建议：重新运行检查；新增同类型 success 覆盖失败；或修复 CI 后再合并。</div>
      </div>

      <div v-if="diagnosticCheckLines.length" class="gate-checks">
        <div class="gate-blockers-title">System Diagnostics</div>
        <div class="gate-check-lines">
          <div v-for="(item, index) in diagnosticCheckLines" :key="`diag-${index}`" class="gate-check-line">
            {{ item }}
          </div>
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
          查看门禁明细
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
          合并 MR
        </el-button>
      </div>

      <div class="gate-footnote">
        门禁明细用于确认当前阻塞项；处理完阻塞后再执行合并 MR。
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
    effectiveChecks: {
      type: Array,
      default: () => []
    },
    blockingChecks: {
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
      default: '查看门禁明细可确认当前阻塞项和可合并状态'
    }
  },
  computed: {
    active() {
      return !!this.mergeRequestId
    },
    statusTagType() {
      return this.statusType || 'info'
    },
    normalizedBlockingChecks() {
      const fromProp = Array.isArray(this.blockingChecks) ? this.blockingChecks : []
      if (fromProp.length) return fromProp
      return (Array.isArray(this.effectiveChecks) ? this.effectiveChecks : [])
        .filter(item => item && item.checkStatus === 'failed' && item.blockingMerge)
    },
    diagnosticChecks() {
      return (Array.isArray(this.effectiveChecks) ? this.effectiveChecks : [])
        .filter(item => item && item.checkStatus === 'failed' && item.systemInternal)
    },
    blockingCheckLines() {
      return this.normalizedBlockingChecks.slice(0, 4).map(this.formatCheckLine)
    },
    diagnosticCheckLines() {
      return this.diagnosticChecks.slice(0, 4).map(this.formatCheckLine)
    }
  },
  methods: {
    formatCheckLine(check) {
      if (!check || typeof check !== 'object') return ''
      const type = String(check.checkType || 'custom').trim().toLowerCase() || 'custom'
      const summary = String(check.summary || '').trim()
      return summary ? `${type}: ${summary}` : `${type}: failed`
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

.gate-checks {
  border: 1px solid #f1f5f9;
  background: #f8fafc;
  border-radius: 10px;
  padding: 10px;
}

.gate-check-lines {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.gate-check-line {
  font-size: 12px;
  line-height: 1.7;
  color: #334155;
  word-break: break-word;
}

.gate-check-actions {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: #475569;
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
