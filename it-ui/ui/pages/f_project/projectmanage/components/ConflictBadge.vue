<template>
  <button
    type="button"
    class="conflict-badge"
    :class="[`is-${displayType}`, { 'is-clickable': clickable, 'is-compact': compact }]"
    :disabled="!clickable"
    @click="$emit('click')"
  >
    <i :class="displayIcon" class="conflict-badge__icon"></i>
    <span class="conflict-badge__text">{{ displayLabel }}</span>
    <span v-if="showCount" class="conflict-badge__count">{{ displayCount }}</span>
  </button>
</template>

<script>
export default {
  name: 'ConflictBadge',
  props: {
    hasConflict: {
      type: Boolean,
      default: false
    },
    conflictCount: {
      type: [Number, String],
      default: 0
    },
    state: {
      type: String,
      default: 'known'
    },
    clickable: {
      type: Boolean,
      default: true
    },
    compact: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    normalizedCount() {
      const count = Number(this.conflictCount)
      return Number.isFinite(count) && count > 0 ? Math.floor(count) : 0
    },
    showCount() {
      return this.hasConflict && this.normalizedCount > 0
    },
    displayLabel() {
      if (this.state === 'unknown') return '待确认'
      return this.hasConflict ? '有冲突' : '可合并'
    },
    displayCount() {
      return this.normalizedCount > 99 ? '99+' : String(this.normalizedCount)
    },
    displayType() {
      if (this.state === 'unknown') return 'warning'
      return this.hasConflict ? 'danger' : 'success'
    },
    displayIcon() {
      if (this.state === 'unknown') return 'el-icon-question'
      return this.hasConflict ? 'el-icon-warning-outline' : 'el-icon-circle-check'
    }
  }
}
</script>

<style scoped>
.conflict-badge {
  appearance: none;
  border: 1px solid transparent;
  border-radius: 999px;
  padding: 4px 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  line-height: 1;
  cursor: default;
  transition: all .18s ease;
}

.conflict-badge.is-clickable {
  cursor: pointer;
}

.conflict-badge.is-clickable:hover {
  transform: translateY(-1px);
}

.conflict-badge.is-danger {
  background: #fff2f0;
  color: #cf1322;
  border-color: #ffccc7;
}

.conflict-badge.is-success {
  background: #f6ffed;
  color: #389e0d;
  border-color: #d9f7be;
}

.conflict-badge.is-warning {
  background: #fff7e6;
  color: #d48806;
  border-color: #ffe58f;
}

.conflict-badge.is-compact {
  padding: 3px 8px;
}

.conflict-badge__icon {
  font-size: 12px;
}

.conflict-badge__count {
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, .08);
  font-size: 11px;
}
</style>
