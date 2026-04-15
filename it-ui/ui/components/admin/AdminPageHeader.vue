<template>
  <div class="admin-page-header">
    <div class="admin-page-header__main">
      <h1 class="admin-page-header__title">{{ title }}</h1>
      <p v-if="description" class="admin-page-header__desc">{{ description }}</p>
    </div>
    <div v-if="$slots.extra" class="admin-page-header__extra">
      <slot name="extra" />
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdminPageHeader',
  props: {
    title: {
      type: String,
      default: ''
    },
    description: {
      type: String,
      default: ''
    }
  }
}
</script>

<style scoped>
.admin-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.admin-page-header__title {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.admin-page-header__desc {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}

.admin-page-header__extra {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .admin-page-header {
    flex-direction: column;
  }

  .admin-page-header__extra {
    width: 100%;
  }
}
</style>
