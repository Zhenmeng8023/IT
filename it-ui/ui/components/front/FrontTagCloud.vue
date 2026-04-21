<template>
  <section class="front-tag-cloud" :class="{ 'is-compact': compact }">
    <header v-if="$slots.header || title" class="front-tag-cloud__header">
      <slot name="header">
        <h3 class="front-tag-cloud__title">{{ title }}</h3>
      </slot>
    </header>

    <div v-if="visibleTags.length" class="front-tag-cloud__list">
      <button
        v-for="(item, index) in visibleTags"
        :key="tagKey(item, index)"
        type="button"
        class="front-tag-cloud__item"
        :class="{ 'is-active': isActive(item) }"
        @click="handleTagClick(item, index)"
      >
        <span class="front-tag-cloud__text">{{ tagLabel(item) }}</span>
        <em v-if="showCount && hasCount(item)" class="front-tag-cloud__count">{{ tagCount(item) }}</em>
      </button>
    </div>

    <p v-else class="front-tag-cloud__empty">{{ emptyText }}</p>
  </section>
</template>

<script>
function normalizeValue(value) {
  if (value === null || value === undefined) return ''
  return String(value)
}

export default {
  name: 'FrontTagCloud',
  props: {
    title: {
      type: String,
      default: ''
    },
    tags: {
      type: Array,
      default() {
        return []
      }
    },
    labelKey: {
      type: String,
      default: 'label'
    },
    valueKey: {
      type: String,
      default: 'value'
    },
    countKey: {
      type: String,
      default: 'count'
    },
    idKey: {
      type: String,
      default: 'id'
    },
    activeValues: {
      type: Array,
      default() {
        return []
      }
    },
    multiple: {
      type: Boolean,
      default: true
    },
    interactive: {
      type: Boolean,
      default: true
    },
    showCount: {
      type: Boolean,
      default: true
    },
    maxItems: {
      type: Number,
      default: 0
    },
    emptyText: {
      type: String,
      default: 'No tags'
    },
    compact: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    activeSet() {
      return new Set((this.activeValues || []).map(normalizeValue))
    },
    visibleTags() {
      const source = Array.isArray(this.tags) ? this.tags : []
      if (this.maxItems > 0) {
        return source.slice(0, this.maxItems)
      }
      return source
    }
  },
  methods: {
    tagLabel(item) {
      if (item && typeof item === 'object') {
        return item[this.labelKey] || item[this.valueKey] || ''
      }
      return normalizeValue(item)
    },
    tagValue(item) {
      if (item && typeof item === 'object') {
        const value = item[this.valueKey]
        if (value !== undefined && value !== null && value !== '') {
          return normalizeValue(value)
        }
        return normalizeValue(item[this.labelKey])
      }
      return normalizeValue(item)
    },
    tagCount(item) {
      if (item && typeof item === 'object') {
        return item[this.countKey]
      }
      return ''
    },
    hasCount(item) {
      const value = this.tagCount(item)
      return value !== null && value !== undefined && value !== ''
    },
    tagKey(item, index) {
      if (item && typeof item === 'object' && item[this.idKey] !== undefined) {
        return item[this.idKey]
      }
      return `${index}-${this.tagValue(item)}`
    },
    isActive(item) {
      return this.activeSet.has(this.tagValue(item))
    },
    handleTagClick(item, index) {
      const value = this.tagValue(item)
      this.$emit('tag-click', { item, index, value })

      if (!this.interactive) {
        return
      }

      if (this.multiple) {
        const next = new Set(this.activeSet)
        if (next.has(value)) next.delete(value)
        else next.add(value)
        const values = Array.from(next)
        this.$emit('update:activeValues', values)
        this.$emit('change', values)
        return
      }

      const values = this.activeSet.has(value) ? [] : [value]
      this.$emit('update:activeValues', values)
      this.$emit('change', values)
    }
  }
}
</script>

<style scoped>
.front-tag-cloud {
  --front-tag-radius: 8px;
  --front-tag-border: var(--it-border, #dbe2ea);
  --front-tag-surface: var(--it-surface, #ffffff);
  --front-tag-muted: var(--it-text-muted, #64748b);
  --front-tag-text: var(--it-text, #0f172a);
  --front-tag-accent: var(--it-accent, #2563eb);
  --front-tag-accent-soft: var(--it-accent-soft, #e8f1ff);
  --front-tag-shadow: var(--it-shadow, 0 6px 18px rgba(15, 23, 42, 0.06));

  border: 1px solid var(--front-tag-border);
  border-radius: var(--front-tag-radius);
  background: var(--front-tag-surface);
  box-shadow: var(--front-tag-shadow);
  padding: 12px;
}

.front-tag-cloud__header {
  margin-bottom: 10px;
}

.front-tag-cloud__title {
  margin: 0;
  font-size: 14px;
  color: var(--front-tag-text);
}

.front-tag-cloud__list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.front-tag-cloud__item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  border-radius: 999px;
  border: 1px solid var(--front-tag-border);
  background: color-mix(in srgb, var(--front-tag-surface) 86%, var(--front-tag-accent-soft));
  color: var(--front-tag-muted);
  padding: 0 12px;
  cursor: pointer;
  font-size: 12px;
  transition: border-color 0.2s ease, color 0.2s ease, background-color 0.2s ease;
}

.front-tag-cloud__item:hover {
  color: var(--front-tag-accent);
  border-color: color-mix(in srgb, var(--front-tag-accent) 28%, var(--front-tag-border));
}

.front-tag-cloud__item.is-active {
  color: #ffffff;
  border-color: transparent;
  background: var(--it-primary-gradient, linear-gradient(135deg, #2563eb, #3b82f6));
}

.front-tag-cloud__count {
  font-style: normal;
  font-size: 11px;
  opacity: 0.88;
}

.front-tag-cloud__empty {
  margin: 0;
  font-size: 12px;
  color: var(--front-tag-muted);
}

.front-tag-cloud.is-compact {
  padding: 10px;
}

@media screen and (max-width: 768px) {
  .front-tag-cloud {
    padding: 10px;
  }
}
</style>
