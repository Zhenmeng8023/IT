<template>
  <div class="admin-action-group" :class="groupClass">
    <slot />
  </div>
</template>

<script>
export default {
  name: 'AdminActionGroup',
  props: {
    align: {
      type: String,
      default: 'center'
    },
    wrap: {
      type: Boolean,
      default: true
    }
  },
  computed: {
    groupClass() {
      return [
        `admin-action-group--${this.align}`,
        this.wrap ? 'admin-action-group--wrap' : 'admin-action-group--nowrap'
      ]
    }
  }
}
</script>

<style scoped>
.admin-action-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-action-group--wrap {
  flex-wrap: wrap;
}

.admin-action-group--nowrap {
  flex-wrap: nowrap;
}

.admin-action-group--left {
  justify-content: flex-start;
}

.admin-action-group--center {
  justify-content: center;
}

.admin-action-group--right {
  justify-content: flex-end;
}

.admin-action-group :deep(.el-button) {
  margin-left: 0;
}
</style>
