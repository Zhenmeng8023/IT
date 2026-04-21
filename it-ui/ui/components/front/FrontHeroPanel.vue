<template>
  <section class="front-hero-panel" :class="{ 'is-compact': compact }">
    <div class="front-hero-panel__main">
      <div class="front-hero-panel__copy">
        <div v-if="$slots.badge || badge" class="front-hero-panel__badge">
          <slot name="badge">{{ badge }}</slot>
        </div>

        <h1 v-if="$slots.title || title" class="front-hero-panel__title">
          <slot name="title">{{ title }}</slot>
        </h1>

        <p v-if="$slots.subtitle || subtitle" class="front-hero-panel__subtitle">
          <slot name="subtitle">{{ subtitle }}</slot>
        </p>

        <div v-if="$slots.default" class="front-hero-panel__extra">
          <slot />
        </div>
      </div>

      <div v-if="hasActionSlot" class="front-hero-panel__actions" :class="`is-${actionsAlign}`">
        <slot name="actions" />
      </div>
    </div>

    <div
      v-if="hasStatsContent"
      class="front-hero-panel__stats"
      :style="{ '--front-hero-columns': String(resolvedStatsColumns) }"
    >
      <slot name="stats">
        <article
          v-for="(item, index) in normalizedStats"
          :key="statKey(item, index)"
          class="front-hero-panel__stat-card"
        >
          <p class="front-hero-panel__stat-label">{{ readField(item, statLabelKey) }}</p>
          <p class="front-hero-panel__stat-value">{{ readField(item, statValueKey) }}</p>
          <p v-if="readField(item, statHintKey)" class="front-hero-panel__stat-hint">
            {{ readField(item, statHintKey) }}
          </p>
        </article>
      </slot>
    </div>
  </section>
</template>

<script>
function normalizeFieldValue(value) {
  if (value === null || value === undefined) {
    return ''
  }
  return value
}

export default {
  name: 'FrontHeroPanel',
  props: {
    badge: {
      type: String,
      default: ''
    },
    title: {
      type: String,
      default: ''
    },
    subtitle: {
      type: String,
      default: ''
    },
    stats: {
      type: Array,
      default() {
        return []
      }
    },
    statsLimit: {
      type: Number,
      default: 6
    },
    statsColumns: {
      type: Number,
      default: 3
    },
    statLabelKey: {
      type: String,
      default: 'label'
    },
    statValueKey: {
      type: String,
      default: 'value'
    },
    statHintKey: {
      type: String,
      default: 'hint'
    },
    statIdKey: {
      type: String,
      default: 'id'
    },
    actionsAlign: {
      type: String,
      default: 'end',
      validator(value) {
        return ['start', 'center', 'end'].includes(value)
      }
    },
    compact: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    hasActionSlot() {
      return !!this.$slots.actions
    },
    hasStatsContent() {
      return !!this.$slots.stats || this.normalizedStats.length > 0
    },
    normalizedStats() {
      const source = Array.isArray(this.stats) ? this.stats : []
      if (this.statsLimit > 0) {
        return source.slice(0, this.statsLimit)
      }
      return source
    },
    resolvedStatsColumns() {
      return Math.max(1, Number(this.statsColumns) || 1)
    }
  },
  methods: {
    readField(item, key) {
      if (!item || typeof item !== 'object') {
        return normalizeFieldValue(item)
      }
      return normalizeFieldValue(item[key])
    },
    statKey(item, index) {
      if (item && typeof item === 'object' && item[this.statIdKey] !== undefined) {
        return item[this.statIdKey]
      }
      return `${index}-${this.readField(item, this.statLabelKey)}`
    }
  }
}
</script>

<style scoped>
.front-hero-panel {
  --front-hero-radius: 8px;
  --front-hero-border: var(--it-border, #dbe2ea);
  --front-hero-surface: var(--it-surface, #ffffff);
  --front-hero-muted: var(--it-text-muted, #64748b);
  --front-hero-text: var(--it-text, #0f172a);
  --front-hero-accent-soft: var(--it-accent-soft, #e8f1ff);
  --front-hero-accent: var(--it-accent, #2563eb);
  --front-hero-shadow: var(--it-shadow, 0 6px 18px rgba(15, 23, 42, 0.06));

  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid var(--front-hero-border);
  border-radius: var(--front-hero-radius);
  background: var(--front-hero-surface);
  box-shadow: var(--front-hero-shadow);
}

.front-hero-panel__main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: start;
}

.front-hero-panel__copy {
  min-width: 0;
}

.front-hero-panel__badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  margin-bottom: 10px;
  border-radius: 999px;
  border: 1px solid var(--front-hero-border);
  background: var(--front-hero-accent-soft);
  color: var(--front-hero-accent);
  font-size: 12px;
  font-weight: 700;
}

.front-hero-panel__title {
  margin: 0;
  color: var(--front-hero-text);
  font-size: clamp(24px, 3.6vw, 36px);
  line-height: 1.15;
}

.front-hero-panel__subtitle {
  margin: 10px 0 0;
  color: var(--front-hero-muted);
  line-height: 1.7;
  font-size: 14px;
}

.front-hero-panel__extra {
  margin-top: 14px;
}

.front-hero-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.front-hero-panel__actions.is-start {
  justify-content: flex-start;
}

.front-hero-panel__actions.is-center {
  justify-content: center;
}

.front-hero-panel__actions.is-end {
  justify-content: flex-end;
}

.front-hero-panel__stats {
  display: grid;
  grid-template-columns: repeat(var(--front-hero-columns), minmax(0, 1fr));
  gap: 10px;
}

.front-hero-panel__stat-card {
  border: 1px solid var(--front-hero-border);
  border-radius: var(--front-hero-radius);
  background: color-mix(in srgb, var(--front-hero-surface) 86%, var(--front-hero-accent-soft));
  padding: 12px;
  min-width: 0;
}

.front-hero-panel__stat-label {
  margin: 0;
  color: var(--front-hero-muted);
  font-size: 12px;
}

.front-hero-panel__stat-value {
  margin: 6px 0 0;
  color: var(--front-hero-text);
  font-size: 24px;
  line-height: 1.15;
  word-break: break-word;
}

.front-hero-panel__stat-hint {
  margin: 8px 0 0;
  color: var(--front-hero-muted);
  font-size: 12px;
  line-height: 1.5;
}

.front-hero-panel.is-compact {
  padding: 14px;
}

.front-hero-panel.is-compact .front-hero-panel__title {
  font-size: clamp(20px, 3vw, 30px);
}

@media screen and (max-width: 1024px) {
  .front-hero-panel__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media screen and (max-width: 768px) {
  .front-hero-panel {
    padding: 14px;
  }

  .front-hero-panel__main {
    grid-template-columns: minmax(0, 1fr);
  }

  .front-hero-panel__actions {
    justify-content: flex-start;
  }

  .front-hero-panel__stats {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
