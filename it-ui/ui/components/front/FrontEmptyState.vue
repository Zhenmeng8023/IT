<template>
  <section class="front-empty-state" :class="[sizeClass, { 'has-border': bordered }]">
    <div class="front-empty-state__icon">
      <slot name="icon">
        <i :class="icon"></i>
      </slot>
    </div>

    <h3 class="front-empty-state__title">{{ title }}</h3>
    <p v-if="description" class="front-empty-state__description">{{ description }}</p>

    <div v-if="$slots.default || actionText" class="front-empty-state__actions">
      <slot>
        <el-button :type="actionType" size="small" @click="$emit('action')">
          {{ actionText }}
        </el-button>
      </slot>
    </div>
  </section>
</template>

<script>
export default {
  name: 'FrontEmptyState',
  props: {
    icon: {
      type: String,
      default: 'el-icon-box'
    },
    title: {
      type: String,
      default: 'No content yet'
    },
    description: {
      type: String,
      default: ''
    },
    actionText: {
      type: String,
      default: ''
    },
    actionType: {
      type: String,
      default: 'primary'
    },
    size: {
      type: String,
      default: 'md',
      validator(value) {
        return ['sm', 'md', 'lg'].includes(value)
      }
    },
    bordered: {
      type: Boolean,
      default: true
    }
  },
  computed: {
    sizeClass() {
      return `is-${this.size}`
    }
  }
}
</script>

<style scoped>
.front-empty-state {
  --front-empty-radius: 8px;
  --front-empty-border: var(--it-border, #dbe2ea);
  --front-empty-surface: var(--it-surface, #ffffff);
  --front-empty-text: var(--it-text, #0f172a);
  --front-empty-muted: var(--it-text-muted, #64748b);
  --front-empty-accent: var(--it-accent, #2563eb);
  --front-empty-shadow: var(--it-shadow, 0 6px 18px rgba(15, 23, 42, 0.06));

  display: grid;
  justify-items: center;
  text-align: center;
  gap: 8px;
  border-radius: var(--front-empty-radius);
  background: var(--front-empty-surface);
  padding: 24px 16px;
}

.front-empty-state.has-border {
  border: 1px solid var(--front-empty-border);
  box-shadow: var(--front-empty-shadow);
}

.front-empty-state__icon {
  width: 52px;
  height: 52px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--front-empty-accent);
  background: var(--it-accent-soft, #e8f1ff);
  font-size: 22px;
}

.front-empty-state__title {
  margin: 2px 0 0;
  font-size: 18px;
  color: var(--front-empty-text);
}

.front-empty-state__description {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--front-empty-muted);
  max-width: 520px;
}

.front-empty-state__actions {
  margin-top: 6px;
}

.front-empty-state.is-sm {
  padding: 16px 12px;
}

.front-empty-state.is-sm .front-empty-state__icon {
  width: 44px;
  height: 44px;
  font-size: 18px;
}

.front-empty-state.is-sm .front-empty-state__title {
  font-size: 16px;
}

.front-empty-state.is-lg {
  padding: 34px 18px;
}

.front-empty-state.is-lg .front-empty-state__icon {
  width: 64px;
  height: 64px;
  font-size: 28px;
}
</style>
