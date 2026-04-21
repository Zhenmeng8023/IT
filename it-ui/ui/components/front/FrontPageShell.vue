<template>
  <section
    class="front-page-shell"
    :class="shellClassList"
    :style="shellStyle"
  >
    <div class="front-page-shell__inner" :style="innerStyle">
      <div v-if="$slots.top" class="front-page-shell__top">
        <slot name="top" />
      </div>

      <div class="front-page-shell__grid" :style="gridStyle">
        <aside v-if="showLeftRail" class="front-page-shell__rail front-page-shell__rail--left">
          <slot name="left" />
        </aside>

        <main class="front-page-shell__main" :class="contentClass">
          <slot />
        </main>

        <aside v-if="showRightRail" class="front-page-shell__rail front-page-shell__rail--right">
          <slot name="right" />
        </aside>
      </div>

      <div v-if="$slots.bottom" class="front-page-shell__bottom">
        <slot name="bottom" />
      </div>
    </div>
  </section>
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
  name: 'FrontPageShell',
  props: {
    maxWidth: {
      type: [Number, String],
      default: 1240
    },
    layout: {
      type: String,
      default: 'three',
      validator(value) {
        return ['three', 'two', 'single'].includes(value)
      }
    },
    twoColumnSide: {
      type: String,
      default: 'right',
      validator(value) {
        return ['left', 'right'].includes(value)
      }
    },
    leftWidth: {
      type: [Number, String],
      default: 250
    },
    rightWidth: {
      type: [Number, String],
      default: 300
    },
    gap: {
      type: [Number, String],
      default: 20
    },
    paddingTop: {
      type: [Number, String],
      default: 20
    },
    paddingBottom: {
      type: [Number, String],
      default: 32
    },
    paddingX: {
      type: [Number, String],
      default: 0
    },
    showBackground: {
      type: Boolean,
      default: true
    },
    stackTablet: {
      type: Boolean,
      default: true
    },
    stackMobile: {
      type: Boolean,
      default: true
    },
    contentClass: {
      type: String,
      default: ''
    }
  },
  computed: {
    hasLeftSlot() {
      return !!this.$slots.left
    },
    hasRightSlot() {
      return !!this.$slots.right
    },
    effectiveTwoColumnSide() {
      if (this.layout === 'two') {
        if (this.twoColumnSide === 'left' && this.hasLeftSlot) return 'left'
        if (this.twoColumnSide === 'right' && this.hasRightSlot) return 'right'
        if (this.hasRightSlot) return 'right'
        if (this.hasLeftSlot) return 'left'
        return null
      }

      if (this.layout === 'three') {
        if (this.hasLeftSlot && !this.hasRightSlot) return 'left'
        if (this.hasRightSlot && !this.hasLeftSlot) return 'right'
      }

      return this.twoColumnSide
    },
    effectiveLayout() {
      if (this.layout === 'single') {
        return 'single'
      }

      if (this.layout === 'two') {
        return this.effectiveTwoColumnSide ? 'two' : 'single'
      }

      if (this.hasLeftSlot && this.hasRightSlot) {
        return 'three'
      }

      if (this.hasLeftSlot || this.hasRightSlot) {
        return 'two'
      }

      return 'single'
    },
    showLeftRail() {
      if (this.effectiveLayout === 'three') {
        return this.hasLeftSlot
      }
      if (this.effectiveLayout === 'two') {
        return this.effectiveTwoColumnSide === 'left' && this.hasLeftSlot
      }
      return false
    },
    showRightRail() {
      if (this.effectiveLayout === 'three') {
        return this.hasRightSlot
      }
      if (this.effectiveLayout === 'two') {
        return this.effectiveTwoColumnSide === 'right' && this.hasRightSlot
      }
      return false
    },
    shellClassList() {
      return [
        `is-layout-${this.effectiveLayout}`,
        { 'has-background': this.showBackground },
        { 'is-stack-tablet': this.stackTablet },
        { 'is-stack-mobile': this.stackMobile }
      ]
    },
    shellStyle() {
      return {
        '--front-shell-padding-top': toCssSize(this.paddingTop, '20px'),
        '--front-shell-padding-bottom': toCssSize(this.paddingBottom, '32px'),
        '--front-shell-padding-x': toCssSize(this.paddingX, '0px')
      }
    },
    innerStyle() {
      return {
        maxWidth: toCssSize(this.maxWidth, '1240px')
      }
    },
    gridStyle() {
      const gap = toCssSize(this.gap, '20px')
      const left = toCssSize(this.leftWidth, '250px')
      const right = toCssSize(this.rightWidth, '300px')

      if (this.effectiveLayout === 'three') {
        return {
          gap,
          gridTemplateColumns: `${left} minmax(0, 1fr) ${right}`
        }
      }

      if (this.effectiveLayout === 'two') {
        if (this.effectiveTwoColumnSide === 'left') {
          return {
            gap,
            gridTemplateColumns: `${left} minmax(0, 1fr)`
          }
        }
        return {
          gap,
          gridTemplateColumns: `minmax(0, 1fr) ${right}`
        }
      }

      return {
        gap,
        gridTemplateColumns: 'minmax(0, 1fr)'
      }
    }
  }
}
</script>

<style scoped>
.front-page-shell {
  --front-shell-radius: 8px;
  --front-shell-border: var(--it-border, #dbe2ea);
  --front-shell-surface: var(--it-surface, #ffffff);
  --front-shell-page-bg: var(--it-page-bg, #f5f8fc);
  --front-shell-shadow: var(--it-shadow, 0 6px 18px rgba(15, 23, 42, 0.06));

  min-height: 100%;
  padding: var(--front-shell-padding-top) var(--front-shell-padding-x) var(--front-shell-padding-bottom);
  background: transparent;
}

.front-page-shell.has-background {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.45), rgba(255, 255, 255, 0)) no-repeat,
    var(--front-shell-page-bg);
}

.front-page-shell__inner {
  width: 100%;
  margin: 0 auto;
}

.front-page-shell__top,
.front-page-shell__bottom {
  margin-bottom: 16px;
}

.front-page-shell__bottom {
  margin-top: 16px;
  margin-bottom: 0;
}

.front-page-shell__grid {
  display: grid;
  align-items: start;
}

.front-page-shell__main,
.front-page-shell__rail {
  min-width: 0;
}

.front-page-shell__main {
  background: var(--front-shell-surface);
  border: 1px solid var(--front-shell-border);
  border-radius: var(--front-shell-radius);
  box-shadow: var(--front-shell-shadow);
  padding: 16px;
}

.front-page-shell__rail {
  display: grid;
  gap: 12px;
}

@media screen and (max-width: 1024px) {
  .front-page-shell.is-stack-tablet .front-page-shell__grid {
    grid-template-columns: minmax(0, 1fr) !important;
  }

  .front-page-shell.is-stack-tablet .front-page-shell__rail {
    order: 2;
  }
}

@media screen and (max-width: 768px) {
  .front-page-shell {
    padding-top: 12px;
    padding-bottom: 20px;
  }

  .front-page-shell__main {
    padding: 12px;
  }

  .front-page-shell.is-stack-mobile .front-page-shell__grid {
    grid-template-columns: minmax(0, 1fr) !important;
  }
}
</style>
