<template>
  <div class="block-navigator">
    <div class="navigator-head">
      <div>
        <div class="navigator-title">冲突块导航</div>
        <div class="navigator-subtitle">{{ blockSummary }}</div>
      </div>
      <el-tag size="mini" effect="plain" :type="blocks.length ? 'warning' : 'info'">
        {{ blocks.length }} 块
      </el-tag>
    </div>

    <div class="navigator-actions">
      <el-button
        size="mini"
        plain
        :disabled="disabled || !hasPrevious"
        @click="$emit('previous')"
      >
        上一块
      </el-button>
      <el-button
        size="mini"
        plain
        :disabled="disabled || !hasNext"
        @click="$emit('next')"
      >
        下一块
      </el-button>
    </div>

    <div v-if="!blocks.length" class="navigator-empty">
      未返回结构化冲突块，可继续手工编辑最终结果。
    </div>

    <div v-else class="block-list">
      <button
        v-for="(block, index) in blocks"
        :key="blockKey(block, index)"
        type="button"
        class="block-item"
        :class="{ active: index === activeIndex }"
        :disabled="disabled"
        @click="$emit('select', index)"
      >
        <span class="block-index">#{{ index + 1 }}</span>
        <span class="block-copy">
          <span class="block-title">{{ blockTitle(block, index) }}</span>
          <span class="block-meta">{{ blockMeta(block) }}</span>
          <span class="block-choice">{{ blockChoiceLabel(block) }}</span>
        </span>
      </button>
    </div>
  </div>
</template>

<script>
import { getContentBlockChoiceLabel, normalizeContentBlockChoice } from '@/utils/projectMergeConflictAdapter'

export default {
  name: 'ConflictBlockNavigator',
  props: {
    blocks: {
      type: Array,
      default: () => []
    },
    choiceMap: {
      type: Object,
      default: () => ({})
    },
    sourceBranchName: {
      type: String,
      default: ''
    },
    targetBranchName: {
      type: String,
      default: ''
    },
    activeIndex: {
      type: Number,
      default: 0
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    hasPrevious() {
      return this.blocks.length > 0 && this.activeIndex > 0
    },
    hasNext() {
      return this.blocks.length > 0 && this.activeIndex < this.blocks.length - 1
    },
    blockSummary() {
      if (!this.blocks.length) return '暂无可定位冲突块'
      return `当前 ${Math.min(this.activeIndex + 1, this.blocks.length)} / ${this.blocks.length}`
    }
  },
  methods: {
    blockKey(block, index) {
      return block && (block.id || block.blockId || block.conflictBlockId)
        ? String(block.id || block.blockId || block.conflictBlockId)
        : `block-${index}`
    },
    blockTitle(block, index) {
      const value = block && (block.title || block.summary || block.reason || block.blockType || block.type)
      return value ? String(value) : `冲突块 ${index + 1}`
    },
    blockMeta(block) {
      if (!block || typeof block !== 'object') return '位置未知'
      const sourceRange = this.rangeText(block.sourceStartLine, block.sourceEndLine)
      const targetRange = this.rangeText(block.targetStartLine, block.targetEndLine)
      const fallbackRange = this.rangeText(block.startLine, block.endLine)
      if (sourceRange || targetRange) {
        return `源 ${sourceRange || '-'} / 目标 ${targetRange || '-'}`
      }
      return fallbackRange ? `行 ${fallbackRange}` : '位置未知'
    },
    blockChoiceLabel(block) {
      const blockId = block && (block.blockId || block.id || block.conflictBlockId) ? String(block.blockId || block.id || block.conflictBlockId) : ''
      const row = blockId ? this.choiceMap[blockId] : null
      const choice = normalizeContentBlockChoice(row && row.choice ? row.choice : (block && block.defaultChoice))
      return getContentBlockChoiceLabel(choice, {
        sourceBranchName: this.sourceBranchName || 'source',
        targetBranchName: this.targetBranchName || 'target'
      })
    },
    rangeText(start, end) {
      const from = Number(start)
      const to = Number(end)
      if (!Number.isFinite(from) || from <= 0) return ''
      if (!Number.isFinite(to) || to <= 0 || to === from) return String(from)
      return `${from}-${to}`
    }
  }
}
</script>

<style scoped>
.block-navigator {
  display: flex;
  flex-direction: column;
  gap: 10px;
  border: 1px solid #dbe7f7;
  border-radius: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  padding: 12px;
}

.navigator-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.navigator-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.navigator-subtitle {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: #64748b;
}

.navigator-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.navigator-empty {
  font-size: 12px;
  line-height: 1.7;
  color: #94a3b8;
}

.block-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 220px;
  overflow: auto;
}

.block-item {
  width: 100%;
  border: 1px solid #dbe7f7;
  border-radius: 12px;
  background: #ffffff;
  padding: 10px;
  display: flex;
  gap: 10px;
  align-items: flex-start;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.block-item:hover,
.block-item.active {
  border-color: #60a5fa;
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.08);
  transform: translateY(-1px);
}

.block-item:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.block-index {
  flex: 0 0 auto;
  font-size: 12px;
  color: #2563eb;
  font-weight: 700;
}

.block-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.block-title {
  font-size: 12px;
  line-height: 1.5;
  color: #0f172a;
  font-weight: 700;
  word-break: break-word;
}

.block-meta {
  font-size: 12px;
  line-height: 1.5;
  color: #64748b;
  word-break: break-word;
}

.block-choice {
  font-size: 12px;
  line-height: 1.5;
  color: #2563eb;
  word-break: break-word;
}
</style>
