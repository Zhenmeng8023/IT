<template>
  <span class="admin-code-tag" :title="displayValue">
    <slot>{{ displayValue }}</slot>
  </span>
</template>

<script>
export default {
  name: 'CodeTag',
  props: {
    value: {
      type: [String, Number],
      default: '-'
    }
  },
  computed: {
    displayValue() {
      if (this.value === null || this.value === undefined || this.value === '') {
        return '-'
      }
      return String(this.value)
    }
  }
}
</script>

<style scoped>
.admin-code-tag {
  display: inline-flex;
  max-width: 100%;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.1);
  color: #2f7df6;
  font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
