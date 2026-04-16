<template>
  <el-card shadow="never" class="conflict-sidebar-card" :class="{ 'is-active': active }">
    <div slot="header" class="sidebar-header">
      <div class="sidebar-title-group">
        <div class="sidebar-title">冲突列表</div>
        <div class="sidebar-subtitle">{{ subtitle }}</div>
      </div>
      <el-tag size="mini" effect="plain" :type="headerTagType">{{ headerTagText }}</el-tag>
    </div>

    <div v-if="loading" class="sidebar-state">
      <el-skeleton :rows="6" animated />
    </div>

    <div v-else-if="errorText" class="sidebar-state">
      <el-alert :title="errorText" type="error" :closable="false" show-icon />
    </div>

    <div v-else-if="!conflicts.length" class="sidebar-state">
      <el-empty :description="emptyText" :image-size="78" />
    </div>

    <div v-else class="sidebar-list">
      <button
        v-for="item in conflicts"
        :key="conflictKey(item)"
        type="button"
        class="sidebar-item"
        :class="[
          `tone-${item.typeTone || 'info'}`,
          { active: String(conflictKey(item)) === String(activeConflictId || '') }
        ]"
        @click="$emit('select', item)"
      >
        <div class="sidebar-item-top">
          <div class="sidebar-title-row">
            <i :class="item.typeIcon || 'el-icon-document'" class="sidebar-type-icon"></i>
            <div class="sidebar-path" :title="item.displayPath">{{ item.displayPath }}</div>
          </div>
          <el-tag size="mini" effect="plain" :type="item.typeTone">{{ item.typeLabel }}</el-tag>
        </div>

        <div class="sidebar-item-subtitle">{{ item.displaySummary }}</div>

        <div class="sidebar-item-meta">
          <el-tag
            v-if="item.basePath"
            size="mini"
            effect="plain"
          >
            base: {{ item.basePath }}
          </el-tag>
          <el-tag
            v-else-if="item.sourcePath && item.targetPath && item.sourcePath !== item.targetPath"
            size="mini"
            effect="plain"
          >
            {{ item.sourcePath }} → {{ item.targetPath }}
          </el-tag>
          <el-tag v-else-if="item.targetPath" size="mini" effect="plain">
            target: {{ item.targetPath }}
          </el-tag>
          <el-tag v-else-if="item.severity" size="mini" effect="plain">{{ item.severity }}</el-tag>
        </div>
      </button>
    </div>
  </el-card>
</template>

<script>
export default {
  name: 'ConflictSidebar',
  props: {
    conflicts: {
      type: Array,
      default: () => []
    },
    activeConflictId: {
      type: [Number, String],
      default: null
    },
    loading: {
      type: Boolean,
      default: false
    },
    errorText: {
      type: String,
      default: ''
    },
    emptyText: {
      type: String,
      default: '当前没有冲突'
    },
    subtitle: {
      type: String,
      default: '按类型查看内容冲突、路径冲突和分支过期问题'
    }
  },
  computed: {
    active() {
      return String(this.activeConflictId || '') !== ''
    },
    headerTagText() {
      return `${this.conflicts.length} 项`
    },
    headerTagType() {
      return this.conflicts.length ? 'warning' : 'info'
    }
  },
  methods: {
    conflictKey(item) {
      if (!item) return ''
      return item.conflictId || item.id || item.filePath || item.path || item.oldPath || item.newPath || ''
    }
  }
}
</script>

<style scoped>
.conflict-sidebar-card {
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid #e5ecf6;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
  height: 100%;
}

.conflict-sidebar-card.is-active {
  border-color: #bfdbfe;
}

.sidebar-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.sidebar-title-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.sidebar-subtitle {
  font-size: 12px;
  line-height: 1.7;
  color: #64748b;
}

.sidebar-state {
  min-height: 280px;
  display: flex;
  align-items: center;
}

.sidebar-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sidebar-item {
  width: 100%;
  text-align: left;
  border: 1px solid #dbe7f7;
  border-radius: 16px;
  padding: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.sidebar-item:hover,
.sidebar-item.active {
  transform: translateY(-1px);
  box-shadow: 0 12px 26px rgba(37, 99, 235, 0.08);
}

.sidebar-item.active {
  border-color: #60a5fa;
}

.sidebar-item.tone-danger.active {
  border-color: #f87171;
}

.sidebar-item.tone-warning.active {
  border-color: #fb923c;
}

.sidebar-item.tone-success.active {
  border-color: #34d399;
}

.sidebar-item.tone-info.active {
  border-color: #60a5fa;
}

.sidebar-item-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.sidebar-title-row {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex: 1;
}

.sidebar-type-icon {
  margin-top: 1px;
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  flex: 0 0 auto;
}

.sidebar-path {
  min-width: 0;
  font-size: 13px;
  line-height: 1.6;
  font-weight: 700;
  color: #1f2937;
  word-break: break-all;
}

.sidebar-item-subtitle {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: #64748b;
}

.sidebar-item-meta {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
