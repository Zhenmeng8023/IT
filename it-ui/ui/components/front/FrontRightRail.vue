<template>
  <aside
    class="front-right-rail"
    :class="{ 'is-sticky': sticky, 'is-compact': compact }"
    :style="railStyle"
  >
    <header v-if="$slots.header || title || subtitle" class="front-right-rail__header">
      <slot name="header">
        <h3 v-if="title" class="front-right-rail__title">{{ title }}</h3>
        <p v-if="subtitle" class="front-right-rail__subtitle">{{ subtitle }}</p>
      </slot>
    </header>

    <div class="front-right-rail__body">
      <slot />
    </div>

    <footer v-if="$slots.footer" class="front-right-rail__footer">
      <slot name="footer" />
    </footer>
  </aside>
</template>

<script>
function toCssSize(value, fallback) {
  if (value === null || value === undefined || value === '') {
    return fallback
  }
  if (typeof value === 'number') {
    return `${value}px`
  }
  return String(value)
}

export default {
  name: 'FrontRightRail',
  props: {
    title: {
      type: String,
      default: ''
    },
    subtitle: {
      type: String,
      default: ''
    },
    sticky: {
      type: Boolean,
      default: true
    },
    topOffset: {
      type: [Number, String],
      default: 20
    },
    sectionGap: {
      type: [Number, String],
      default: 12
    },
    compact: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    railStyle() {
      return {
        '--front-right-rail-top': toCssSize(this.topOffset, '20px'),
        '--front-right-rail-gap': toCssSize(this.sectionGap, '12px')
      }
    }
  }
}
</script>

<style scoped>
.front-right-rail {
  --front-rail-radius: 8px;
  --front-rail-border: var(--it-border, #dbe2ea);
  --front-rail-surface: var(--it-surface, #ffffff);
  --front-rail-muted: var(--it-text-muted, #64748b);
  --front-rail-text: var(--it-text, #0f172a);
  --front-rail-shadow: var(--it-shadow, 0 6px 18px rgba(15, 23, 42, 0.06));

  border: 1px solid var(--front-rail-border);
  border-radius: var(--front-rail-radius);
  background: var(--front-rail-surface);
  box-shadow: var(--front-rail-shadow);
  padding: 14px;
}

.front-right-rail.is-sticky {
  position: sticky;
  top: var(--front-right-rail-top);
}

.front-right-rail__header {
  margin-bottom: 12px;
}

.front-right-rail__title {
  margin: 0;
  font-size: 16px;
  color: var(--front-rail-text);
}

.front-right-rail__subtitle {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--front-rail-muted);
}

.front-right-rail__body {
  display: grid;
  gap: var(--front-right-rail-gap);
}

.front-right-rail__footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--front-rail-border);
}

.front-right-rail__body :deep(.front-right-rail__section) {
  border: 1px solid var(--front-rail-border);
  border-radius: var(--front-rail-radius);
  background: color-mix(in srgb, var(--front-rail-surface) 90%, var(--it-accent-soft, #e8f1ff));
  padding: 10px;
}

.front-right-rail.is-compact {
  padding: 10px;
}

@media screen and (max-width: 1024px) {
  .front-right-rail.is-sticky {
    position: static;
  }
}
</style>
