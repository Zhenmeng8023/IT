<template>
  <el-card shadow="never" class="manage-entry-hub">
    <div slot="header" class="hub-header">
      <div class="hub-heading">
        <div class="hub-title">{{ title }}</div>
        <div class="hub-subtitle">{{ subtitle }}</div>
      </div>
      <div v-if="projectId" class="hub-context">项目 #{{ projectId }}</div>
    </div>

    <div class="hub-entry-grid">
      <button
        v-for="entry in normalizedEntries"
        :key="entry.key"
        type="button"
        class="hub-entry-card"
        :class="[
          entry.tone ? 'tone-' + entry.tone : '',
          {
            active: entry.key === currentKey,
            disabled: entry.disabled
          }
        ]"
        :disabled="entry.disabled"
        @click="handleNavigate(entry)"
      >
        <div class="hub-entry-head">
          <div class="hub-entry-title">{{ entry.title }}</div>
          <div class="hub-entry-badge">{{ entry.badge || (entry.key === currentKey ? '当前入口' : '可切换') }}</div>
        </div>
        <div class="hub-entry-desc">{{ entry.desc }}</div>
      </button>
    </div>

    <div v-if="normalizedStatusCards.length" class="hub-status-grid">
      <div
        v-for="card in normalizedStatusCards"
        :key="card.key"
        class="hub-status-card"
        :class="card.tone ? 'tone-' + card.tone : ''"
      >
        <div class="hub-status-label">{{ card.label }}</div>
        <div class="hub-status-value">{{ card.value }}</div>
        <div class="hub-status-desc">{{ card.desc }}</div>
      </div>
    </div>
  </el-card>
</template>

<script>
export default {
  name: 'ProjectManageEntryHub',
  props: {
    currentKey: {
      type: String,
      default: ''
    },
    title: {
      type: String,
      default: '管理侧统一入口'
    },
    subtitle: {
      type: String,
      default: ''
    },
    projectId: {
      type: [String, Number],
      default: null
    },
    entries: {
      type: Array,
      default: () => []
    },
    statusCards: {
      type: Array,
      default: () => []
    }
  },
  computed: {
    normalizedEntries() {
      return Array.isArray(this.entries) ? this.entries : []
    },
    normalizedStatusCards() {
      return Array.isArray(this.statusCards) ? this.statusCards : []
    }
  },
  methods: {
    handleNavigate(entry) {
      if (!entry || entry.disabled || !entry.path) return
      if (entry.requiresProjectId && !this.projectId) {
        this.$message.warning('请先绑定项目上下文，再进入该入口')
        return
      }
      this.$router.push({
        path: entry.path,
        query: entry.query || undefined
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.manage-entry-hub {
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid #e5ecf6;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
}

.hub-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.hub-heading {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hub-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.hub-subtitle {
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
}

.hub-context {
  flex-shrink: 0;
  padding: 8px 12px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
}

.hub-entry-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.hub-entry-card {
  width: 100%;
  text-align: left;
  border: 1px solid #dbe7f7;
  border-radius: 18px;
  padding: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.hub-entry-card:hover:not(.disabled) {
  transform: translateY(-1px);
  border-color: #93c5fd;
  box-shadow: 0 12px 26px rgba(37, 99, 235, 0.08);
}

.hub-entry-card.active {
  border-color: #60a5fa;
  background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%);
}

.hub-entry-card.disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.hub-entry-card.tone-blue.active {
  border-color: #60a5fa;
}

.hub-entry-card.tone-cyan.active {
  border-color: #22d3ee;
  background: linear-gradient(180deg, #ecfeff 0%, #ffffff 100%);
}

.hub-entry-card.tone-orange.active {
  border-color: #fb923c;
  background: linear-gradient(180deg, #fff7ed 0%, #ffffff 100%);
}

.hub-entry-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.hub-entry-title {
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
}

.hub-entry-badge {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
  font-size: 11px;
  font-weight: 600;
}

.hub-entry-desc {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.7;
  color: #64748b;
}

.hub-status-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.hub-status-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid #e5ecf6;
  background: #ffffff;
}

.hub-status-card.tone-blue {
  background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%);
}

.hub-status-card.tone-cyan {
  background: linear-gradient(180deg, #ecfeff 0%, #ffffff 100%);
}

.hub-status-card.tone-purple {
  background: linear-gradient(180deg, #f5f3ff 0%, #ffffff 100%);
}

.hub-status-card.tone-orange {
  background: linear-gradient(180deg, #fff7ed 0%, #ffffff 100%);
}

.hub-status-card.tone-danger {
  background: linear-gradient(180deg, #fff1f2 0%, #ffffff 100%);
}

.hub-status-label {
  font-size: 12px;
  color: #64748b;
}

.hub-status-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.hub-status-desc {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: #94a3b8;
}

@media (max-width: 1200px) {
  .hub-entry-grid {
    grid-template-columns: 1fr;
  }

  .hub-status-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .hub-header {
    flex-direction: column;
  }

  .hub-status-grid {
    grid-template-columns: 1fr;
  }
}


.hub-title,
.hub-entry-title,
.hub-status-value { color: var(--it-text); }
.hub-subtitle,
.hub-status-label,
.hub-entry-desc { color: var(--it-text-muted); }
.manage-entry-hub,
.hub-status-card,
.hub-entry-card {
  border-color: var(--it-border);
  box-shadow: var(--it-shadow-soft);
}
.manage-entry-hub { background: var(--it-panel-bg-strong); }
.hub-context,
.hub-entry-badge {
  background: var(--it-tag-bg);
  color: var(--it-tag-text);
}
.hub-entry-card,
.hub-status-card { background: var(--it-panel-bg); }
.hub-entry-card:hover:not(.disabled),
.hub-entry-card.active { border-color: var(--it-border-strong); box-shadow: var(--it-shadow-hover); }
.hub-entry-card.active,
.hub-status-card.tone-blue { background: linear-gradient(180deg, var(--it-tone-info-soft) 0%, var(--it-panel-bg-strong) 100%); }
.hub-entry-card.tone-cyan.active,
.hub-status-card.tone-cyan { background: linear-gradient(180deg, var(--it-tone-cyan-soft) 0%, var(--it-panel-bg-strong) 100%); }
.hub-status-card.tone-purple { background: linear-gradient(180deg, var(--it-tone-purple-soft) 0%, var(--it-panel-bg-strong) 100%); }
.hub-status-card.tone-orange { background: linear-gradient(180deg, var(--it-warning-soft) 0%, var(--it-panel-bg-strong) 100%); }
.hub-status-card.tone-danger { background: linear-gradient(180deg, var(--it-danger-soft) 0%, var(--it-panel-bg-strong) 100%); }

</style>


<style scoped>
/* round11-entryhub-unify */
.manage-entry-hub,
.hub-status-card,
.hub-entry-card {
  border-color: var(--it-border) !important;
  box-shadow: var(--it-shadow-soft) !important;
}
.manage-entry-hub { background: var(--it-panel-bg-strong) !important; }
.hub-title,
.hub-entry-title,
.hub-status-value { color: var(--it-text) !important; }
.hub-subtitle,
.hub-status-label,
.hub-entry-desc,
.hub-status-desc { color: var(--it-text-muted) !important; }
.hub-context,
.hub-entry-badge {
  background: var(--it-tag-bg) !important;
  color: var(--it-tag-text) !important;
}
.hub-entry-card,
.hub-status-card { background: var(--it-panel-bg) !important; }
.hub-entry-card:hover:not(.disabled),
.hub-entry-card.active {
  border-color: var(--it-border-strong) !important;
  box-shadow: var(--it-shadow-hover) !important;
}
.hub-entry-card.active,
.hub-status-card.tone-blue { background: linear-gradient(180deg, var(--it-tone-info-soft) 0%, var(--it-panel-bg-strong) 100%) !important; }
.hub-entry-card.tone-cyan.active,
.hub-status-card.tone-cyan { background: linear-gradient(180deg, var(--it-tone-cyan-soft) 0%, var(--it-panel-bg-strong) 100%) !important; }
.hub-status-card.tone-purple { background: linear-gradient(180deg, var(--it-tone-purple-soft) 0%, var(--it-panel-bg-strong) 100%) !important; }
.hub-entry-card.tone-orange.active,
.hub-status-card.tone-orange { background: linear-gradient(180deg, var(--it-warning-soft) 0%, var(--it-panel-bg-strong) 100%) !important; }
.hub-status-card.tone-danger { background: linear-gradient(180deg, var(--it-danger-soft) 0%, var(--it-panel-bg-strong) 100%) !important; }
</style>

