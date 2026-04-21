<template>
  <section class="front-feed-toolbar" :class="{ 'is-dense': dense }">
    <div class="front-feed-toolbar__top">
      <div v-if="tabs.length" class="front-feed-toolbar__tabs">
        <el-tabs
          :value="activeTab"
          :stretch="stretchTabs"
          @input="handleTabInput"
          @tab-click="handleTabClick"
        >
          <el-tab-pane
            v-for="(tab, index) in tabs"
            :key="tabKey(tab, index)"
            :name="String(readField(tab, tabValueKey))"
            :disabled="!!tab.disabled"
          >
            <span slot="label" class="front-feed-toolbar__tab-label">
              {{ readField(tab, tabLabelKey) }}
              <em v-if="showTabCount && hasTabCount(tab)" class="front-feed-toolbar__tab-count">
                {{ readField(tab, tabCountKey) }}
              </em>
            </span>
          </el-tab-pane>
        </el-tabs>
      </div>

      <div class="front-feed-toolbar__controls">
        <slot name="leading" />

        <div v-if="sortOptions.length" class="front-feed-toolbar__sort">
          <span class="front-feed-toolbar__sort-label">{{ sortLabel }}</span>
          <el-radio-group
            :value="sortValue"
            size="small"
            @input="handleSortInput"
            @change="handleSortChange"
          >
            <el-radio-button
              v-for="(option, index) in sortOptions"
              :key="sortOptionKey(option, index)"
              :label="readField(option, sortValueKey)"
            >
              <i v-if="readField(option, sortIconKey)" :class="readField(option, sortIconKey)"></i>
              {{ readField(option, sortLabelKey) }}
            </el-radio-button>
          </el-radio-group>
        </div>

        <slot name="controls" />
      </div>
    </div>

    <div v-if="hasBottomRow" class="front-feed-toolbar__bottom">
      <div v-if="filters.length" class="front-feed-toolbar__filters">
        <span class="front-feed-toolbar__filter-label">{{ filterLabel }}</span>
        <el-tag
          v-for="(item, index) in filters"
          :key="filterKey(item, index)"
          size="small"
          :closable="removableFilters"
          class="front-feed-toolbar__filter-chip"
          @close="removeFilter(item, index)"
        >
          {{ formatFilterText(item) }}
        </el-tag>
        <el-button
          v-if="showClearFilters"
          type="text"
          class="front-feed-toolbar__clear-btn"
          @click="$emit('clear-filters')"
        >
          {{ clearText }}
        </el-button>
      </div>

      <div v-if="$slots.extra" class="front-feed-toolbar__extra">
        <slot name="extra" />
      </div>
    </div>
  </section>
</template>

<script>
function normalizeString(value) {
  if (value === null || value === undefined) return ''
  return String(value)
}

export default {
  name: 'FrontFeedToolbar',
  props: {
    tabs: {
      type: Array,
      default() {
        return []
      }
    },
    activeTab: {
      type: [String, Number],
      default: ''
    },
    tabLabelKey: {
      type: String,
      default: 'label'
    },
    tabValueKey: {
      type: String,
      default: 'value'
    },
    tabCountKey: {
      type: String,
      default: 'count'
    },
    tabIdKey: {
      type: String,
      default: 'id'
    },
    showTabCount: {
      type: Boolean,
      default: true
    },
    stretchTabs: {
      type: Boolean,
      default: false
    },
    sortOptions: {
      type: Array,
      default() {
        return []
      }
    },
    sortValue: {
      type: [String, Number],
      default: ''
    },
    sortLabel: {
      type: String,
      default: 'Sort'
    },
    sortLabelKey: {
      type: String,
      default: 'label'
    },
    sortValueKey: {
      type: String,
      default: 'value'
    },
    sortIconKey: {
      type: String,
      default: 'icon'
    },
    sortIdKey: {
      type: String,
      default: 'id'
    },
    filters: {
      type: Array,
      default() {
        return []
      }
    },
    filterLabel: {
      type: String,
      default: 'Filters'
    },
    filterTextKey: {
      type: String,
      default: 'text'
    },
    filterValueKey: {
      type: String,
      default: 'value'
    },
    filterIdKey: {
      type: String,
      default: 'id'
    },
    clearText: {
      type: String,
      default: 'Clear'
    },
    removableFilters: {
      type: Boolean,
      default: true
    },
    showClearFilters: {
      type: Boolean,
      default: true
    },
    dense: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    hasBottomRow() {
      return this.filters.length > 0 || !!this.$slots.extra
    }
  },
  methods: {
    readField(item, key) {
      if (item && typeof item === 'object') {
        return item[key]
      }
      return item
    },
    hasTabCount(tab) {
      const value = this.readField(tab, this.tabCountKey)
      return value !== null && value !== undefined && value !== ''
    },
    tabKey(tab, index) {
      if (tab && typeof tab === 'object' && tab[this.tabIdKey] !== undefined) {
        return tab[this.tabIdKey]
      }
      return `${index}-${normalizeString(this.readField(tab, this.tabValueKey))}`
    },
    sortOptionKey(option, index) {
      if (option && typeof option === 'object' && option[this.sortIdKey] !== undefined) {
        return option[this.sortIdKey]
      }
      return `${index}-${normalizeString(this.readField(option, this.sortValueKey))}`
    },
    filterKey(item, index) {
      if (item && typeof item === 'object' && item[this.filterIdKey] !== undefined) {
        return item[this.filterIdKey]
      }
      return `${index}-${normalizeString(this.readField(item, this.filterValueKey))}`
    },
    handleTabInput(value) {
      this.$emit('update:activeTab', value)
    },
    handleTabClick(tab) {
      this.$emit('tab-change', tab ? tab.name : undefined)
    },
    handleSortInput(value) {
      this.$emit('update:sortValue', value)
    },
    handleSortChange(value) {
      this.$emit('sort-change', value)
    },
    formatFilterText(item) {
      if (!item || typeof item !== 'object') {
        return normalizeString(item)
      }
      if (item[this.filterTextKey] !== undefined && item[this.filterTextKey] !== '') {
        return item[this.filterTextKey]
      }
      return normalizeString(item[this.filterValueKey])
    },
    removeFilter(item, index) {
      this.$emit('remove-filter', { item, index })
    }
  }
}
</script>

<style scoped>
.front-feed-toolbar {
  --front-toolbar-radius: 8px;
  --front-toolbar-border: var(--it-border, #dbe2ea);
  --front-toolbar-surface: var(--it-surface, #ffffff);
  --front-toolbar-muted: var(--it-text-muted, #64748b);
  --front-toolbar-text: var(--it-text, #0f172a);
  --front-toolbar-accent: var(--it-accent, #2563eb);
  --front-toolbar-accent-soft: var(--it-accent-soft, #e8f1ff);
  --front-toolbar-shadow: var(--it-shadow, 0 6px 18px rgba(15, 23, 42, 0.06));

  border: 1px solid var(--front-toolbar-border);
  border-radius: var(--front-toolbar-radius);
  background: var(--front-toolbar-surface);
  box-shadow: var(--front-toolbar-shadow);
  padding: 14px;
}

.front-feed-toolbar__top {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
}

.front-feed-toolbar__tabs {
  min-width: 0;
}

.front-feed-toolbar__tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.front-feed-toolbar__tab-count {
  font-style: normal;
  font-size: 11px;
  color: var(--front-toolbar-muted);
}

.front-feed-toolbar__controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.front-feed-toolbar__sort {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.front-feed-toolbar__sort-label {
  font-size: 12px;
  color: var(--front-toolbar-muted);
  white-space: nowrap;
}

.front-feed-toolbar__sort :deep(.el-radio-button__inner) {
  border-radius: 8px;
  border-color: var(--front-toolbar-border);
  background: color-mix(in srgb, var(--front-toolbar-surface) 88%, var(--front-toolbar-accent-soft));
  color: var(--front-toolbar-muted);
  box-shadow: none;
}

.front-feed-toolbar__sort :deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
  background: var(--it-primary-gradient, linear-gradient(135deg, #2563eb, #3b82f6));
  color: #ffffff;
  border-color: transparent;
}

.front-feed-toolbar__bottom {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--front-toolbar-border);
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.front-feed-toolbar__filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.front-feed-toolbar__filter-label {
  font-size: 12px;
  color: var(--front-toolbar-muted);
}

.front-feed-toolbar__filter-chip {
  border-radius: 999px;
  border-color: var(--front-toolbar-border);
  background: var(--front-toolbar-accent-soft);
  color: var(--front-toolbar-accent);
}

.front-feed-toolbar__clear-btn {
  padding: 0;
}

.front-feed-toolbar__extra {
  display: inline-flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.front-feed-toolbar.is-dense {
  padding: 10px;
}

.front-feed-toolbar :deep(.el-tabs__header) {
  margin: 0;
}

.front-feed-toolbar :deep(.el-tabs__nav-wrap::after) {
  background: var(--front-toolbar-border);
}

.front-feed-toolbar :deep(.el-tabs__item) {
  color: var(--front-toolbar-muted);
}

.front-feed-toolbar :deep(.el-tabs__item.is-active),
.front-feed-toolbar :deep(.el-tabs__item:hover) {
  color: var(--front-toolbar-accent);
}

.front-feed-toolbar :deep(.el-tabs__active-bar) {
  background: var(--front-toolbar-accent);
}

@media screen and (max-width: 1024px) {
  .front-feed-toolbar__top,
  .front-feed-toolbar__bottom {
    grid-template-columns: minmax(0, 1fr);
  }

  .front-feed-toolbar__controls,
  .front-feed-toolbar__extra {
    justify-content: flex-start;
  }
}

@media screen and (max-width: 768px) {
  .front-feed-toolbar {
    padding: 10px;
  }

  .front-feed-toolbar__sort {
    width: 100%;
    display: grid;
    gap: 8px;
  }

  .front-feed-toolbar__sort :deep(.el-radio-group) {
    width: 100%;
    display: flex;
  }

  .front-feed-toolbar__sort :deep(.el-radio-button) {
    flex: 1;
  }

  .front-feed-toolbar__sort :deep(.el-radio-button__inner) {
    width: 100%;
    text-align: center;
  }
}
</style>
